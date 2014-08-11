/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.common.Entry;
import jef.common.SimpleMap;
import jef.common.log.LogUtil;
import jef.database.Condition.Operator;
import jef.database.annotation.PartitionFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;
import jef.database.innerpool.MetadataConnectionPool;
import jef.database.innerpool.MetadataService;
import jef.database.meta.Column;
import jef.database.meta.ColumnChange;
import jef.database.meta.ColumnModification;
import jef.database.meta.DdlGenerator;
import jef.database.meta.Feature;
import jef.database.meta.ForeignKey;
import jef.database.meta.Function;
import jef.database.meta.ITableMetadata;
import jef.database.meta.Index;
import jef.database.meta.MetaHolder;
import jef.database.meta.PrimaryKey;
import jef.database.query.DefaultPartitionCalculator;
import jef.database.support.MetadataEventListener;
import jef.database.wrapper.populator.ResultPopulatorImpl;
import jef.database.wrapper.populator.ResultSetTransformer;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.result.ResultSetImpl;
import jef.database.wrapper.result.ResultSets;
import jef.http.client.support.CommentEntry;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

import org.apache.commons.lang.ArrayUtils;

/*
 *  ====Oracle约束===== 约束包括： 主键、外键（引用）、唯一约束、检查约束
 * 
 * 外键必须确保在被引用的表上是唯一的。否则不能创建。 因此外键必然要么引用表的主键、要么引用表的唯一约束键。 外键必然有索引。也必然有键
 * 
 * 所有约束都可以通过disable和enable命令启用和禁用 ALTER TABLE products disable CONSTRAINT
 * fk_supplier;
 * 
 * 主键、唯一约束一定有索引。其他约束不一定有索引
 * 
 * 除了由唯一约束创建的索引，还有其他非唯一的索引。
 * 也可以创建唯一约束的索引，但是唯一约束索引虽然能起到唯一约束的作用，但是却不能作为唯一约束那样建立外键引用。
 * 
 * alter table person add constraint FK_PERSON foreign key (schoolId) references
 * School(id); alter table person add constraint FK_PERSON foreign key
 * (parentid) references person(id);
 * 
 * 判断序列是否存在
 * 
 * ==== 注释 ==== 1\Derby目前还不支持数据库对象注释 2\普通注释语法：Oracle Eg. COMMENT ON TABLE
 * EMPLOYEE IS 'Reflects first quarter 2000 reorganization' COMMENT ON COLUMN
 * mytable.primarykey IS 'Unique ID from Sequence SEQ_MASTER'
 * 
 * 3\MySql注释语法 CREATE TABLE FOO ( A COMMENT 'This col is A') COMMENT='And here
 * is the table comment'
 * 
 * 
 * 5\同一张表上也能建立外键 alter table person add constraint FK_PERSON_PARENT foreign key
 * (parentid) references person(id);
 * 
 * 6、多字段也能建议外键
 */
/**
 * 对于数据库元数据的访问封装
 * <p>
 * 通过操作JDBC的DatabaseMetadata，来访问数据库中的表、列、索引等各种对象
 * 
 * @author Jiyi
 * 
 */
public class DbMetaData extends MetadataConnectionPool {
	private MetadataService parent;
	
	private String dbkey;
	
	private ConnectInfo info;
	/**
	 * 所属shema
	 */
	private String schema;


	/**
	 * 缓存清理的间隔时间<br>
	 * 考虑到在生产环境中，一些原本没创建的表可能会动态创建，因此考虑间隔一定时间清除缓存。
	 */
	private int interval;
	/**
	 * 下次缓存过期时间
	 */
	private long nextExpireTime;
	
	/**
	 * 根据扫描得到的所有表的情况
	 */
	private final Map<String, Set<String>> subTableData = new ConcurrentHashMap<String, Set<String>>();
	
	// 运行时缓存
	private String[] tableTypes;
	/**
	 * 记录数据库是否支持恢复点
	 */
	private Boolean supportsSavepoints;

	private int jdbcVersion;
	
	private long dbTimeDelta;

	/**
	 * 构造
	 * 
	 * @param ds
	 *            数据连接
	 * @param parent
	 *            元数据服务
	 * @param dbkey
	 *            当前元数据所属的数据源名称
	 */
	public DbMetaData(DataSource ds, MetadataService parent, String dbkey) {
		super(ds);
		this.interval = JefConfiguration.getInt(DbCfg.DB_PARTITION_REFRESH, 3600) * 1000;
		this.dbkey = dbkey;
		this.nextExpireTime = System.currentTimeMillis() + interval;
		this.parent = parent;

		info = DbUtils.tryAnalyzeInfo(ds, false);
		try {
			if (info == null) {
				Connection con = getConnection();
				try{
					con.setAutoCommit(true);
					info = DbUtils.tryAnalyzeInfo(con);	
				}finally{
					releaseConnection(con);
				}
			}
			DatabaseDialect profile = info.profile;
			Assert.notNull(profile);
			if (profile.has(Feature.USER_AS_SCHEMA)) {
				this.schema = profile.getObjectNameIfUppercase(StringUtils.trimToNull(info.getUser()));
			} else if (profile.has(Feature.DBNAME_AS_SCHEMA)) {
				this.schema = profile.getObjectNameIfUppercase(StringUtils.trimToNull(info.getDbname()));
			}
			if (this.schema == null)
				schema = profile.getDefaultSchema();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * 得到数据库中的表
	 * 
	 * @param names
	 *            要查找的表名，仅第一个参数有效
	 * @return 表的信息
	 */
	public List<TableInfo> getTable(String name) throws SQLException {
		return getDatabaseObject(ObjectType.TABLE, this.schema, name, null);
	}

	/**
	 * 返回数据库中所有的表(当前schema下)
	 * 
	 * @return 表信息
	 * @throws SQLException
	 */
	public List<TableInfo> getTables() throws SQLException {
		return getDatabaseObject(ObjectType.TABLE, this.schema, null, null);
	}
	

	public void setDbTimeDelta(long dbTimeDelta) {
		this.dbTimeDelta = dbTimeDelta;
	}

	/**
	 * 得到当前数据库的时间，这一运算不是通过到数据库查询而得，而是和数据库每次心跳时都会刷新当前系统时间和数据库时间的差值，从而得到数据库时间。
	 * 当数据库心跳正时，这一方式可以较为轻量的得到系统时间。
	 * @return 当前数据库时间
	 */
	public Date getCurrentTime(){
		return new Date(System.currentTimeMillis()+dbTimeDelta);
		
	}
	/**
	 * 查询数据库中的视图
	 * 
	 * @param name
	 *            视图名称
	 * @return 视图信息
	 */
	public List<TableInfo> getView(String name) throws SQLException {
		return getDatabaseObject(ObjectType.VIEW, this.schema, name, null);
	}

	/**
	 * 得到数据库中所有视图
	 * 
	 * @return 视图信息
	 * @throws SQLException
	 */
	public List<TableInfo> getViews() throws SQLException {
		return getDatabaseObject(ObjectType.VIEW, this.schema, null, null);
	}

	/**
	 * 得到数据库中的序列
	 * 
	 * @return Sequence信息，其中 <li>{@link CommentEntry#getKey()} 返回sequence名称</li>
	 *         <li>{@link CommentEntry#getValue()} 返回sequence备注</li>
	 */
	public List<CommentEntry> getSequence(String name) throws SQLException {
		List<CommentEntry> result = new ArrayList<CommentEntry>();
		for (TableInfo table : getDatabaseObject(ObjectType.SEQUENCE, this.schema, name, null)) {
			CommentEntry e = new CommentEntry();
			e.setKey(table.getName());
			e.setValue(table.getRemarks());
		}
		return result;
	}

	/**
	 * @param type
	 *            要查询的对象类型 "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
	 *            "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
	 * @param schema
	 *            Schema
	 * @param matchName
	 *            匹配名称
	 * @param oper
	 *            操作符，可以为null，为null时表示等于条件
	 * @return 表/视图等数据库对象的信息
	 * @throws SQLException
	 * @see Operator
	 */
	public List<TableInfo> getDatabaseObject(ObjectType type, String schema, String matchName, Operator oper) throws SQLException {
		if (schema == null)
			schema = this.schema;
		if (matchName != null) {
			int n = matchName.indexOf('.');
			if (n > -1) {
				schema = matchName.substring(0, n);
				matchName = matchName.substring(n + 1);
			}
		}
		if (oper != null && oper != Operator.EQUALS) {
			if (StringUtils.isEmpty(matchName)) {
				matchName = "%";
			} else if (oper == Operator.MATCH_ANY) {
				matchName = "%" + matchName + "%";
			} else if (oper == Operator.MATCH_END) {
				matchName = "%" + matchName;
			} else if (oper == Operator.MATCH_START) {
				matchName = matchName + "%";
			}
		}
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		DatabaseDialect trans = info.profile;
		ResultSet rs =null;
		try {
			rs = databaseMetaData.getTables(trans.getCatlog(schema), trans.getSchema(schema), matchName, new String[] { type.name() });
			List<TableInfo> result = new ArrayList<TableInfo>();
			while (rs.next()) {
				TableInfo info = new TableInfo();
				info.catalog = rs.getString("TABLE_CAT");
				info.schema = rs.getString("TABLE_SCHEM");
				info.name = rs.getString("TABLE_NAME");
				info.type = rs.getString("TABLE_TYPE");// "TABLE","VIEW",
														// "SYSTEM TABLE",
														// "GLOBAL TEMPORARY","LOCAL TEMPORARY",
														// "ALIAS", "SYNONYM".
				info.remarks = rs.getString("REMARKS");
				result.add(info);
			}
			return result;
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 返回当前schema下的所有数据库表名
	 * 
	 * @param types
	 *            取以下参数{@link ObjectType}。可以省略，省略的情况下取Table
	 * @return 所有表名
	 * @throws SQLException
	 */
	public List<String> getTableNames(ObjectType... types) throws SQLException {
		if (types == null || types.length == 0) {
			types = new ObjectType[] { ObjectType.TABLE };
		}
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		DatabaseDialect trans = info.profile;
		String[] ts = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			ts[i] = types[i].name();
		}
		ResultSet rs = databaseMetaData.getTables(trans.getCatlog(schema), trans.getSchema(schema), null, ts);
		try {
			List<String> result = new ArrayList<String>();
			while (rs.next()) {
				result.add(rs.getString("TABLE_NAME"));
			}
			return result;
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 判断一张表是否存在
	 * 
	 * @param tableName
	 *            表名
	 * @return 表存在返回true, 否则false
	 * @throws SQLException
	 */
	public boolean existTable(String tableName) throws SQLException {
		return exists(ObjectType.TABLE, tableName);
	}

	/**
	 * 获取数据库中所有的catalog
	 * 
	 * @return 所有catalog
	 * @throws SQLException
	 */
	public String[] getCatalogs() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = databaseMetaData.getCatalogs();
		try {
			List<String> list = ResultSets.toStringList(rs, "TABLE_CAT", 9999, this.getProfile());
			return list.toArray(new String[list.size()]);
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 获得当前用户的Schema
	 * 
	 * @return 当前用户的Schema
	 */
	public String getCurrentSchema() {
		return schema;
	}

	/**
	 * 获得所有的Schema
	 * 
	 * @return 所有的Schema
	 * @throws SQLException
	 */
	public String[] getSchemas() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = databaseMetaData.getSchemas();
		try {
			List<String> list = ResultSets.toStringList(rs, "TABLE_SCHEM", 9999, this.getProfile());
			return list.toArray(new String[list.size()]);
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 判断对象是否存在
	 * 
	 * @param type
	 *            要查找的对象{@linkplain ObjectType 类型}
	 * @param objectName
	 *            对象名称
	 * @return true如果对象存在。否则false
	 * @throws SQLException
	 * @see ObjectType
	 */
	public boolean exists(ObjectType type, String objectName) throws SQLException {
		String schema = null;
		int n = objectName.indexOf('.');
		if (n > -1) {
			schema = objectName.substring(0, n);
			objectName = objectName.substring(n + 1);
		}
		return innerExists(type, schema, objectName);
	}

	/**
	 * 判断对象是否存在于指定的schema下
	 * 
	 * @param type
	 *            要查找的对象{@linkplain ObjectType 类型}
	 * @param schema
	 *            所属schema
	 * @param objectName
	 *            对象名称
	 * @return true如果对象存在。否则false
	 * @throws SQLException
	 * @see ObjectType
	 */
	public boolean existsInSchema(ObjectType type, String schema, String objectName) throws SQLException {
		if (schema == null) {
			int n = objectName.indexOf('.');
			if (n > -1) {
				schema = objectName.substring(0, n);
				objectName = objectName.substring(n + 1);
			}
		}
		return innerExists(type, schema, objectName);
	}

	/**
	 * 得到指定表的所有列
	 * 
	 * @param tableName
	 *            表名
	 * @return 表中的列
	 * @throws SQLException
	 * @see Column
	 */
	public List<Column> getColumns(String tableName) throws SQLException {
		tableName = info.profile.getObjectNameIfUppercase(tableName);
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();

		String schema = this.schema;
		int n = tableName.indexOf('.');
		if (n > 0) {// 尝试从表名中计算schema
			schema = tableName.substring(0, n);
			tableName = tableName.substring(n + 1);
		}
		ResultSet rs = null;
		List<Column> list = new ArrayList<Column>();
		try {
			rs = databaseMetaData.getColumns(null, schema, tableName, "%");
			while (rs.next()) {
				Column column = new Column();
				/*
				 * Notice: Oracle非常变态，当调用rs.getString("COLUMN_DEF")会经常抛出
				 * "Stream is already closed" Exception。
				 * 百思不得其解，google了半天有人提供了回避这个问题的办法
				 * （https://issues.apache.org/jira/browse/DDLUTILS-29），
				 * 就是将getString("COLUMN_DEF")作为第一个获取的字段， 非常神奇的就好了。叹息啊。。。
				 */
				String defaultVal = rs.getString("COLUMN_DEF");
				column.setColumnDef(StringUtils.trimToNull(defaultVal));// Oracle会在后面加上换行等怪字符。
				column.setColumnName(rs.getString("COLUMN_NAME"));
				column.setColumnSize(rs.getInt("COLUMN_SIZE"));
				column.setDecimalDigit(rs.getInt("DECIMAL_DIGITS"));
				column.setDataType(rs.getString("TYPE_NAME"));
				column.setDataTypeCode(rs.getInt("DATA_TYPE"));
				column.setNullAble(rs.getString("IS_NULLABLE").equalsIgnoreCase("YES"));
				column.setRemarks(rs.getString("REMARKS"));// 这个操作容易出问题，一定要最后操作
				column.setTableName(tableName);
				list.add(column);
			}
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
		return list;
	}

	/**
	 * 得到指定表的所有索引
	 * 
	 * @param tableName
	 *            表名
	 * @return 索引信息
	 * @see Index
	 */
	public Collection<Index> getIndexes(String tableName) throws SQLException {
		tableName = info.profile.getObjectNameIfUppercase(tableName);
		if (info.profile.has(Feature.NOT_SUPPORT_INDEX_META))
			return new ArrayList<Index>();
		Connection conn = getConnection();
		ResultSet rs = null;
		try {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			rs = databaseMetaData.getIndexInfo(null, schema, tableName, false, false);
			Map<String, Index> map = new HashMap<String, Index>();
			while (rs.next()) {
				String indexName = rs.getString("INDEX_NAME");
				String cName = rs.getString("COLUMN_NAME");
				if (indexName == null || cName == null)
					continue;
				Index index = map.get(indexName);
				if (index == null) {
					index = new Index();
					index.setTableName(tableName);
					index.setIndexName(indexName);
					index.setUnique(!rs.getBoolean("NON_UNIQUE"));
					String asc = rs.getString("ASC_OR_DESC");
					if (asc != null)
						index.setOrderAsc(asc.startsWith("A"));
					index.setType(rs.getInt("TYPE"));
					map.put(indexName, index);
				}
				index.addColumnName(cName);
			}
			return map.values();
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 得到数据库的JDBC驱动程序的版本
	 * 
	 * @return 驱动版本(注意：不是JDBC规范版本)
	 * @throws SQLException
	 */
	public String getDriverVersion() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		releaseConnection(conn);
		return databaseMetaData.getDriverVersion();
	}

	/**
	 * 获取数据库版本信息
	 * 
	 * @return 数据库版本
	 * @throws SQLException
	 */
	public String getDatabaseVersion() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		String version=databaseMetaData.getDatabaseProductVersion();
		releaseConnection(conn);
		return version;
	}

	/**
	 * 得到数据库和驱动的版本信息
	 * 
	 * @return Map<String,String> [key] is
	 *         <ul>
	 *         <li>DatabaseProductName</li>
	 *         <li>DatabaseProductVersion</li>
	 *         <li>DriverName</li>
	 *         <li>DriverVersion</li>
	 *         </ul>
	 */
	public Map<String, String> getDbVersion() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		Map<String, String> map = new SimpleMap<String, String>();
		map.put("DriverName", databaseMetaData.getDriverName());
		map.put("DriverVersion", databaseMetaData.getDriverVersion() + " " + databaseMetaData.getDatabaseMinorVersion());
		map.put("DatabaseProductName", databaseMetaData.getDatabaseProductName());
		map.put("DatabaseProductVersion", databaseMetaData.getDatabaseProductVersion() + " " + databaseMetaData.getDatabaseMinorVersion());
		for (String sql : info.profile.getOtherVersionSql()) {
			if (StringUtils.isBlank(sql))
				continue;
			Statement st = conn.createStatement();
			ResultSet rs = null;
			try {
				rs = st.executeQuery(sql);
				while (rs.next()) {
					map.put(rs.getString(1), rs.getString(2));
				}
			} finally {
				DbUtils.close(rs);
				DbUtils.close(st);
			}
		}
		releaseConnection(conn);
		return map;
	}

	/**
	 * 得到当前数据库用户名
	 * 
	 * @return Username
	 */
	public String getUserName() throws SQLException {
		return info.user;
	}

	/**
	 * 得到数据库支持的所有数据类型
	 * 
	 * @return List<String> 数据类型
	 */
	public List<String> getSupportDataType() throws SQLException {
		Connection conn = getConnection();
		ResultSet rs = null;
		try {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			List<String> list = new ArrayList<String>();
			rs = databaseMetaData.getTypeInfo();
			while (rs.next()) {
				String typeName = rs.getString("TYPE_NAME");
				list.add(typeName);
			}
			return list;
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 得到表的主键
	 * 
	 * @param tableName
	 *            表名
	 * @return Map<String,String> key=列名 value=主键名
	 */
	public PrimaryKey getPrimaryKey(String tableName) throws SQLException {
		tableName = MetaHolder.toSchemaAdjustedName(tableName);
		tableName = info.profile.getObjectNameIfUppercase(tableName);
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = null;
		try {
			rs = databaseMetaData.getPrimaryKeys(null, schema, tableName);
			PrimaryKey pk = null;
			List<String> pkColumns = new ArrayList<String>();
			while (rs.next()) {
				String pkName = rs.getString("PK_NAME");
				String col = rs.getString("COLUMN_NAME");

				if (pk == null) {
					pk = new PrimaryKey(pkName);
				} else {
					if (!StringUtils.equals(pk.getName(), pkName)) {
						throw new SQLException("There is more than one primary key on table " + tableName + "?!" + pk.getName() + " vs " + pkName);
					}
				}
				pkColumns.add(col);
			}
			if (pk == null)
				return pk;
			pk.setColumns(pkColumns.toArray(new String[pkColumns.size()]));
			return pk;
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 获得外键（引用其他表的键）
	 * 
	 * @param tableName
	 *            外键所在的表
	 * @return 外键列表
	 * @throws SQLException
	 */
	public ForeignKey[] getForeignKey(String tableName) throws SQLException {
		tableName = info.profile.getObjectNameIfUppercase(tableName);
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = null;
		try {
			rs = databaseMetaData.getImportedKeys(null, schema, tableName);
			List<ForeignKey> fks = ResultPopulatorImpl.instance.toPlainJavaObject(new ResultSetImpl(rs, this.getProfile()), FK_TRANSFORMER);
			return fks.toArray(new ForeignKey[fks.size()]);
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 获取指定表中被其他表引用的外键
	 * 
	 * @param tableName
	 *            被引用外键的表
	 * @return 外键列表
	 * @throws SQLException
	 */
	public ForeignKey[] getForeignKeyReferenceBy(String tableName) throws SQLException {
		tableName = info.profile.getObjectNameIfUppercase(tableName);
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = null;
		try {
			rs = databaseMetaData.getExportedKeys(null, schema, tableName);
			List<ForeignKey> fks = ResultPopulatorImpl.instance.toPlainJavaObject(new ResultSetImpl(rs, getProfile()), FK_TRANSFORMER);
			return fks.toArray(new ForeignKey[fks.size()]);
		} catch (RuntimeException e) {
			// JDBC驱动会抛出不当的错误。
			LogUtil.exception(e);
			return new ForeignKey[0];
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	/**
	 * 得到数据库支持的关键字和函数名
	 * 
	 * @return Map<String,String> [key] will be..
	 *         <ul>
	 *         <li>SQLKeywords</li>
	 *         <li>TimeDateFunctions</li>
	 *         <li>NumericFunctions</li>
	 *         <li>StringFunctions</li>
	 *         <li>SystemFunctions</li>
	 *         </ul>
	 */
	public List<String> getAllBuildInFunctions() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		List<String> result = new ArrayList<String>();
		result.addAll(Arrays.asList(StringUtils.split(databaseMetaData.getTimeDateFunctions(), ',')));
		result.addAll(Arrays.asList(StringUtils.split(databaseMetaData.getNumericFunctions(), ',')));
		result.addAll(Arrays.asList(StringUtils.split(databaseMetaData.getStringFunctions(), ',')));
		result.addAll(Arrays.asList(StringUtils.split(databaseMetaData.getSystemFunctions(), ',')));
		releaseConnection(conn);
		return result;
	}

	/**
	 * 得到内建日期函数名
	 * 
	 * @return 数据库支持的日期函数（逗号分隔）
	 * @throws SQLException
	 */
	public String getTimeDateFunctions() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		String result = databaseMetaData.getTimeDateFunctions();
		releaseConnection(conn);
		return result;
	}

	/**
	 * 得到内建数值函数名
	 * 
	 * @return 数据库支持的数值函数（逗号分隔）
	 * @throws SQLException
	 */
	public String getNumericFunctions() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		String result = databaseMetaData.getNumericFunctions();
		releaseConnection(conn);
		return result;
	}

	/**
	 * 得到内建字符串函数名
	 * 
	 * @return 内建字符串函数（逗号分隔）
	 * @throws SQLException
	 */
	public String getStringFunctions() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		String result = databaseMetaData.getStringFunctions();
		releaseConnection(conn);
		return result;
	}

	/**
	 * 得到内建其他函数名
	 * 
	 * @return 其他函数名（逗号分隔）
	 * @throws SQLException
	 */
	public String getSystemFunctions() throws SQLException {
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		String result = databaseMetaData.getSystemFunctions();
		releaseConnection(conn);
		return result;
	}

	/**
	 * 检查是否存在指定的存储过程
	 * 
	 * @param schema
	 *            所在schema
	 * @param objectName
	 *            存储过程名
	 * @return 存储过程存在返回true，否则false
	 * @throws SQLException
	 */
	public boolean existsProcdure(String schema, String objectName) throws SQLException {
		List<Function> func = this.innerGetProcedures(schema, objectName);
		return !func.isEmpty();
	}

	/**
	 * 返回指定名称的函数是否存在(需要支持JDBC 4.0的驱动才可执行)
	 * 
	 * @param schema
	 *            所在schema
	 * @param name
	 *            函数名
	 * @return 函数存在返回true,否则false
	 * @throws SQLException
	 *             检测用户函数功能在 JDBC 4.0 (JDK 6)中定义，很多旧版本驱动都不支持，会抛出此异常
	 * @since 1.7.1
	 * @author Jiyi
	 */
	public boolean existsFunction(String schema, String name) throws SQLException {
		List<Function> func = innerGetFunctions(schema, name);
		return !func.isEmpty();
	}

	/**
	 * 得到数据库中的当前用户的存储过程
	 * 
	 * @param schema
	 *            数据库schema，传入null表示当前schema
	 * @return 存储过程
	 */
	public List<Function> getProcedures(String schema) throws SQLException {
		return innerGetProcedures(schema, null);
	}

	/**
	 * 返回所有自定义数据库函数
	 * 
	 * @param schema
	 *            数据库schema，传入null表示当前schema
	 * @return 自定义函数
	 * @throws SQLException
	 */
	public List<Function> getFunctions(String schema) throws SQLException {
		return innerGetFunctions(schema, null);
	}

	/**
	 * 得到由JDBC驱动提供的所有数据库关键字
	 * 
	 * @return 关键字列表
	 * @throws SQLException
	 */
	public String[] getSQLKeywords() throws SQLException {
		Connection conn = getConnection();
		try {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			return org.apache.commons.lang.StringUtils.split(databaseMetaData.getSQLKeywords(), ',');
		} finally {
			releaseConnection(conn);
		}
	}

	/**
	 * 计算序列的起始值(根据表中最大的键值来计算Sequence的合理起点)
	 * 
	 * @param schema
	 *            schema名称
	 * @param tableName
	 *            不带schema的表名
	 * @param sequenceColumnName
	 *            使用序列值的字段名称
	 * @param stReuse
	 *            可重用的{@code Statement}对象
	 * @return 计算得到的Sequence起始值，使用该起始值一般不会造成sequence和数据表中的已有记录冲突。
	 * @throws SQLException
	 */
	public long getSequenceStartValue(String schema, String tableName, String sequenceColumnName) throws SQLException {
		if (!existTable(tableName)) {
			return 1;
		}
		String getMaxValueSql = createGetMaxSequenceColumnValueStatement(schema, tableName, sequenceColumnName);
		Connection conn = getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			if (st == null) {
				st = conn.createStatement();
			}
			if (ORMConfig.getInstance().isDebugMode()) {
				LogUtil.show(getMaxValueSql + "|" + info.getDbname());
			}
			rs = st.executeQuery(getMaxValueSql);
			long maxValue = 0;
			if (rs.next()) {
				maxValue = rs.getLong(1);
			}
			return maxValue;
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, getMaxValueSql);
			throw e;
		} finally {
			DbUtils.close(rs);
			DbUtils.close(st);
			releaseConnection(conn);
		}
	}

	/**
	 * Return whether the JDBC 3.0 Savepoint feature was supported. Caches the
	 * flag for the lifetime of this Metadata.
	 * 
	 * @return true if current Database and the JDBC Driver supports savepoints.
	 * @throws SQLException
	 *             if thrown by the JDBC driver
	 */
	public boolean supportsSavepoints() throws SQLException {
		if (supportsSavepoints == null) {
			Connection conn = getConnection();
			supportsSavepoints = conn.getMetaData().supportsSavepoints();
			releaseConnection(conn);
		}
		return supportsSavepoints.booleanValue();
	}

	/**
	 * 判断，是否支持指定的事务隔离级别
	 * 
	 * @param level
	 * @return
	 * @throws SQLException
	 */
	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		Connection conn = getConnection();
		return conn.getMetaData().supportsTransactionIsolationLevel(level);
	}

	/**
	 * 返回当前此数据库的驱动是否支持JDBC4的方法。需要再连接池初始化的时候检测
	 * 
	 * @return 返回1234的大版本号，如果无法获知返回-1
	 */
	public int getJdbcVersion() {
		if (jdbcVersion == 0) {
			try {
				jdbcVersion = caclJdbcVersion();
			} catch (SQLException e) {
				jdbcVersion = -1;
			}
		}
		return jdbcVersion;
	}

	private int caclJdbcVersion() throws SQLException {
		Connection conn = this.getConnection();
		try {
			if (testJdbc4(conn))
				return 4;
			if (testJdbc3(conn))
				return 3;
			if (testJdbc2(conn))
				return 2;
			return 1;
		} catch (Exception e) {
			LogUtil.exception(e);
		} finally {
			releaseConnection(conn);
		}
		return -1;
	}

	private boolean testJdbc2(Connection conn) {
		try {
			Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			DbUtils.close(st);
		} catch (SQLException e) {
			LogUtil.exception(e);
		} catch (AbstractMethodError e) {
			return false;
		}
		return true;
	}

	private boolean testJdbc3(Connection conn) {
		Statement st = null;
		try {
			st = conn.createStatement();
			st.getGeneratedKeys();
		} catch (SQLFeatureNotSupportedException e) {
			return false;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
		} finally {
			DbUtils.close(st);
		}
		return true;
	}

	private boolean testJdbc4(Connection conn) {
		try {
			conn.isValid(1);
			return true;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
		}
		try {
			Statement st = conn.createStatement();
			DbUtils.close(st);
			st.isClosed();
			return true;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
		}
		try {
			conn.createBlob();
			return true;
		} catch (AbstractMethodError e) {
			return false;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * 得到数据库支持的表的类型
	 * 
	 * @return 表类型
	 * @throws SQLException
	 */
	public String[] getTableTypes() throws SQLException {
		if (tableTypes == null) {
			List<String> type = new ArrayList<String>();
			Connection conn = getConnection();
			ResultSet rs = null;
			try{
				rs=conn.getMetaData().getTableTypes();
				while (rs.next()) {
					type.add(rs.getString(1));
				}	
			}finally{
				DbUtils.close(rs);
			}
			
			this.tableTypes = type.toArray(new String[type.size()]);
		}
		return tableTypes;
	}

	/**
	 * 得到数据库连接基本信息
	 * 
	 * @return 基本信息
	 * @see ConnectInfo
	 */
	public ConnectInfo getInfo() {
		return info;
	}

	/**
	 * 获取数据库名
	 * 
	 * @return 数据库名
	 */
	public String getDbName() {
		return info.dbname;
	}

	/**
	 * 获取数据库方言
	 * 
	 * @return 方言
	 */
	public DatabaseDialect getProfile() {
		return info.profile;
	}

	/**
	 * 根据基础表,查找目前数据库中已有的分表。 <h3>场景</h3>
	 * 在金融、电信大型关系型数据领域经常会有将一张表的数据拆成多张表存储。（比如按天分表，按月分表）。 比如一张日志表，可能会创建成以下表
	 * <ul>
	 * <li>USER_LOG<br>
	 * --基表，定义数据结构，但无实际数据</li>
	 * <li>USER_LOG_201204<br>
	 * --2012年4月的日志</li>
	 * <li>USER_LOG_201205<br>
	 * --2012年5月的日志</li>
	 * <li>USER_LOG_201206<br>
	 * --2012年6月的日志</li>
	 * <li>USER_LOG_201207<br>
	 * --2012年7月的日志</li>
	 * <li>.....</li>
	 * </ul>
	 * <p>
	 * 因此，根据规划，分表的名称一般都通过基表增加后缀来表示。
	 * 
	 * <h3>用法</h3>
	 * 传入基表的元数据模型，查询数据库中同个schema下所有的表，检查名称是否匹配分表规则，检查字段是否和基表一致。 符合条件的表作为结果返回。
	 * <h3>缓存</h3>
	 * 因为表查找的开销较大，所以计算结果会缓存。在jef.properties中配置<br>
	 * {@code db.partition.refresh=n}<br>
	 * n是刷新间隔秒数。默认3600秒，即分表查找结果默认缓存1小时。
	 * <p>
	 * 
	 * @param tableMetadata
	 *            数据库表的模型
	 * @return 所有分表的名称(全大写)。如果这张表没有分表，那么返回空列表。
	 * 
	 * @throws SQLException
	 */
	public Collection<String> getSubTableNames(ITableMetadata tableMetadata) throws SQLException {
		Assert.notNull(tableMetadata);
		boolean isDefault = DbUtils.partitionUtil instanceof DefaultPartitionCalculator;
		if (isDefault) {// 如果是JEF默认的分表计算器，那么可以优化计算，直接跳出
			if (tableMetadata.getPartition() == null)
				return Collections.emptySet();
		}

		String tableName = getProfile().getObjectNameIfUppercase(tableMetadata.getTableName(true));
		Set<String> result = null;
		// 缓存有效性判断
		if (System.currentTimeMillis() > nextExpireTime) {
			subTableData.clear();
			nextExpireTime = System.currentTimeMillis() + interval;
		} else {
			result = subTableData.get(tableName);
		}
		// 缓存中得不到，到数据库中计算
		if (result == null) {
			result = calculateSubTables(tableName, tableMetadata, isDefault);
			subTableData.put(tableName, result);
		}
		return result;
	}

	/**
	 * 更新表。此操作核对输入的元模型和数据库中表的差异，并且通过create table或alter table等语句尽可能将其修改得和元模型一直。
	 * 
	 * <h3>注意</h3> 由于ALTER TABLE有很多限制，因此这个方法执行有很多可能会抛出错误。
	 * <p>
	 * 此外，由于ALTER TABLE语句是DDL，因此多个DDL执行中出现错误时，已经执行过的语句将不会被回滚。所以请尽可能通过
	 * {@linkplain MetadataEventListener 监听器} 的监听事件来把握表变更的进度情况。
	 * 
	 * @param meta
	 *            元模型
	 * @param tablename
	 *            表名
	 * @param event
	 *            事件监听器，可以捕捉表对比、SQL语句执行前后等事件
	 * @throws SQLException
	 *             修改表失败时抛出
	 * @see MetadataEventListener 变更监听器
	 */
	public void refreshTable(ITableMetadata meta, String tablename, MetadataEventListener event,boolean allowCreateTable) throws SQLException {
		DatabaseDialect profile = getProfile();
		tablename = profile.getObjectNameIfUppercase(tablename);
		boolean supportChangeDelete = profile.notHas(Feature.NOT_SUPPORT_ALTER_DROP_COLUMN);
		if (!supportChangeDelete) {
			LogUtil.warn("Current database [{}] doesn't support alter table column.", profile.getName());
		}

		List<Column> columns = this.getColumns(tablename);
		if (columns.isEmpty()) {// 表不存在
			if(allowCreateTable){
				boolean created = false;
				if (event == null || event.onTableCreate(meta, tablename)) {
					created = this.createTable(meta, tablename);
				}
				if (created && event != null) {
					event.onTableFinished(meta, tablename);
				}	
			}
			return;
		}
		// 新增列
		Map<Field, ColumnType> defined = getColumnMap(meta);

		// 在对比之前判断
		if (event != null) {
			boolean isContinue = event.onCompareColumns(tablename, columns, defined);
			if (!isContinue) {
				return;
			}
		}

		// 删除的列
		List<String> delete = new ArrayList<String>();
		// 更新的列
		List<ColumnModification> changed = new ArrayList<ColumnModification>();

		// 比较差异
		for (Column c : columns) {
			Field field = meta.getFieldByLowerColumn(c.getColumnName().toLowerCase());
			if (field == null) {
				if (supportChangeDelete) {
					delete.add(c.getColumnName());
				}
				continue;
			}
			ColumnType type = defined.remove(field);// from the metadata find
													// the column defined
			Assert.notNull(type);// 不应该发生
			if (supportChangeDelete) {
				List<ColumnChange> changes = type.isEqualTo(c, getProfile());
				if (!changes.isEmpty()) {
					changed.add(new ColumnModification(c, changes, type));
				}
			}
		}
		Map<String, ColumnType> insert = new HashMap<String, ColumnType>();
		for (Map.Entry<Field, ColumnType> e : defined.entrySet()) {
			insert.put(meta.getColumnName(e.getKey(), getProfile(), true), e.getValue());
		}
		// 比较完成后，只剩下三类变更的列数据
		if (event != null && event.onColumnsCompared(tablename, meta, insert, changed, delete) == false) {
			return;
		}

		DdlGenerator ddl = parent.getDdlGenerator(dbkey);
		List<String> altertables = ddl.toTableModifyClause(meta, tablename, insert, changed, delete);
		boolean debug = ORMConfig.getInstance().isDebugMode();
		Connection conn = null;
		Statement st = null;
		try {
			conn = getConnection();
			st = conn.createStatement();
			st.setQueryTimeout(180);// 最多执行3分钟
			String id = this.getTransactionId();
			if (event != null) {
				event.beforeAlterTable(tablename, meta, conn, altertables);
			}
			int n = 0;
			for (String s : altertables) {
				long start = System.currentTimeMillis();
				if (debug) {
					LogUtil.show(s + " |" + id);
				}
				boolean success = true;
				try {
					st.executeUpdate(s);
				} catch (SQLException e) {
					success = false;
					if (event == null || !event.onSqlExecuteError(e, tablename, s, Collections.unmodifiableList(altertables), n)) {
						throw e;
					}
				}
				if (success) {
					long cost = System.currentTimeMillis() - start;
					if (event != null) {
						event.onAlterSqlFinished(tablename, s, Collections.unmodifiableList(altertables), n, cost);
					}
					if (debug) {
						LogUtil.show("DDL Executed: cost " + (cost) + " ms. |" + id);
					}
				}
				n++;
			}
		} finally {
			DbUtils.close(st);
			this.releaseConnection(conn);
		}
		if (event != null) {
			event.onTableFinished(meta, tablename);
		}
	}

	/**
	 * 清除表中的所有数据。truncate是DDL不能回滚。
	 * 
	 * @param meta
	 *            要清除的表的元数据
	 * @param tablename
	 *            表名
	 * @throws SQLException
	 */
	public void truncate(ITableMetadata meta, List<String> tablename) throws SQLException {
		Connection conn = getConnection();
		Statement st = conn.createStatement();
		String sql = null;
		boolean debug = ORMConfig.getInstance().isDebugMode();
		try {
			if (getProfile().has(Feature.NOT_SUPPORT_TRUNCATE)) {
				for (String table : tablename) {
					sql = "delete from " + table;
					if (debug) {
						LogUtil.show(sql + "  |" + getTransactionId());
					}
					st.executeUpdate(sql);
				}
			} else {
				for (String table : tablename) {
					sql = "truncate table " + table;
					if (debug) {
						LogUtil.show(sql + "  |" + getTransactionId());
					}
					st.executeUpdate(sql);
				}
			}
		} finally {
			DbUtils.close(st);
			releaseConnection(conn);
		}
	}

	/**
	 * 创建表
	 * 
	 * @param clz
	 *            建表的CLass
	 * @return true建表成功，false表已存在
	 * @throws SQLException
	 */
	public <T extends IQueryableEntity> boolean createTable(Class<T> clz) throws SQLException {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		return createTable(meta, meta.getTableName(true));
	}

	/**
	 * 创建表
	 * 
	 * @param meta
	 *            表结构描述
	 * @param tablename
	 *            表明
	 * @return 如果表被创建，返回true。如果表已存在，返回false
	 * @throws SQLException
	 * @see {@link ITableMetadata}
	 */
	public boolean createTable(ITableMetadata meta, String tablename) throws SQLException {
		DdlGenerator ddl = parent.getDdlGenerator(dbkey);
		if (tablename == null) {
			tablename = meta.getTableName(true);
		}
		if (existTable(tablename))
			return false;
		String[] sqls = ddl.toTableCreateClause(meta, tablename);
		String sql = "create table " + tablename + "(\n" + sqls[0] + "\n)";
		if (sqls[2] != null) {
			sql += sqls[2];
		}
		Connection conn = getConnection();
		Statement st = null;
		try {
			st = conn.createStatement();
			execute(st, sql);
			// create sequence
			if (sqls[1] != null) {
				createSequence(null, sqls[1], 1, StringUtils.toLong(sqls[3], Long.MAX_VALUE));
			}
			// create indexes
			List<String> idx = ddl.toIndexClause(meta, tablename);
			for (String idxSql : idx) {
				try {
					execute(st, idxSql);
				} catch (SQLException e) {
					LogUtil.exception(e);
				}
			}
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, tablename);
			throw e;
		} finally {
			DbUtils.close(st);
			this.releaseConnection(conn);
		}
		return true;
	}

	/**
	 * 创建Sequence
	 * 
	 * @param schema
	 *            数据库schema
	 * @param sequenceName
	 *            不含schema的Sequence名称
	 * @param start
	 *            Sequence起点
	 * @param max
	 *            Sequence最大值
	 * @throws SQLException
	 */
	public void createSequence(String schema, String sequenceName, long start, Long max) throws SQLException {
		DatabaseDialect profile = this.getProfile();
		sequenceName = profile.getObjectNameIfUppercase(sequenceName);
		if (innerExists(ObjectType.SEQUENCE, schema, sequenceName))
			return;
		if (max == null)
			max = 9999999999L;
		long min=1;
		if(min>start)min=start;
		
		if (schema != null) {
			sequenceName = schema + "." + sequenceName;
		}
		String sequenceSql = StringUtils.concat("create sequence ", sequenceName, " minvalue "+min+" maxvalue ", String.valueOf(max), " start with ", String.valueOf(start), " increment by 1");
		Connection conn = getConnection();
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(sequenceSql + "|" + getTransactionId());
		}
		Statement st = conn.createStatement();
		try {
			st.execute(sequenceSql);
		} finally {
			DbUtils.close(st);
			releaseConnection(conn);
		}

	}

	/**
	 * 指定一个SQL脚本文件运行
	 * 
	 * @param url
	 *            the script file.
	 * @throws SQLException
	 */
	public void executeScriptFile(URL url) throws SQLException {
		executeScriptFile(url,";/");
	}

	/**
	 * 指定一个SQL脚本文件运行
	 * 
	 * @param url
	 *            the script file.
	 * @param endChars 命令结束字符
	 * @throws SQLException
	 */
	public void executeScriptFile(URL url, String endChars) throws SQLException {
		Assert.notNull(url);
		char[] ends = endChars.toCharArray();
		Connection conn = getConnection();
		Statement st = conn.createStatement();
		try {
			StringBuilder sb = new StringBuilder();
			BufferedReader reader = IOUtils.getReader(url, null);
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("--")) {
					continue;
				}
				char end = line.charAt(line.length() - 1);
				if (ArrayUtils.contains(ends, end)) {
					sb.append(line.substring(0, line.length() - 1));
					String sql = sb.toString();
					sb.setLength(0);
					execute(st, sql);
				} else {
					sb.append(line).append('\n');
				}
			}
			if (sb.length() > 0) {
				execute(st, sb.toString());
			}
		} catch (IOException e) {
			LogUtil.exception(e);
		} finally {
			DbUtils.close(st);
			releaseConnection(conn);
		}
	}

	/**
	 * 执行指定的SQL语句<br>
	 * Execute the sql
	 * 
	 * @param sql
	 *            SQL语句
	 * @param ps
	 *            绑定变量条件
	 * @return 影响的记录条数
	 * @throws SQLException
	 */
	public final int executeSql(String sql, List<?> ps) throws SQLException {
		long start = System.currentTimeMillis();
		PreparedStatement st = null;
		int total;
		long dbAccess;
		StringBuilder sb = null;
		boolean debug = ORMConfig.getInstance().isDebugMode();
		if (debug)
			sb = new StringBuilder(sql).append("\t|").append(this.getTransactionId());
		Connection conn = getConnection();
		try {
			st = conn.prepareStatement(sql);
			if (ps != null && !ps.isEmpty()) {
				BindVariableContext context = new BindVariableContext(st, getProfile(), sb);
				BindVariableTool.setVariables(context, ps);
			}
			total = st.executeUpdate();
			dbAccess = System.currentTimeMillis();
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			throw e;
		} finally {
			if (debug)
				LogUtil.show(sb);
			DbUtils.close(st);
			releaseConnection(conn);
		}
		if (debug)
			LogUtil.show(StringUtils.concat("Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms) |", getTransactionId()));
		return total;
	}

	/**
	 * 根据指定的SQL语句查询
	 * 
	 * @param sql
	 *            SQL语句
	 * @param rst
	 *            结果转换器
	 * @param maxReturn
	 *            最多返回结果数
	 * @param objs
	 *            查询绑定变量参数
	 * @return 转换后的结果集
	 * @throws SQLException
	 */
	public final <T> T selectBySql(String sql, ResultSetTransformer<T> rst, int maxReturn, List<?> objs) throws SQLException {
		PreparedStatement st = null;
		ResultSet rs = null;
		StringBuilder sb = null;
		Connection conn = getConnection();
		DatabaseDialect profile = getProfile();
		boolean debug = ORMConfig.getInstance().isDebugMode();
		try {
			if (debug)
				sb = new StringBuilder(sql.length() + 30).append(sql).append(" | ").append(this.getTransactionId());

			st = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			if (objs != null) {
				BindVariableContext context = new BindVariableContext(st, profile, sb);
				BindVariableTool.setVariables(context, objs);
			}
			if (maxReturn > 0)
				st.setMaxRows(maxReturn);
			rs = st.executeQuery();
			return rst.transformer(rs, profile);
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			throw e;
		} finally {
			DbUtils.close(rs);
			DbUtils.close(st);
			if (debug)
				LogUtil.show(sb);
			releaseConnection(conn);
		}
	}

	/**
	 * 从databaseMetadata得到的数据表的信息（不光是表，可能包括表和视图等）
	 * 
	 * @author jiyi
	 * 
	 */
	public static class TableInfo {
		private String catalog;
		private String schema;
		private String name;
		private String remarks;
		private String type;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			// if(StringUtils.isNotEmpty(schema)){
			// sb.append(schema).append('.');
			// }
			sb.append(name);
			if (StringUtils.isNotEmpty(remarks)) {
				sb.append(':').append(remarks);
			}
			return sb.toString();
		}

		/**
		 * 数据库表所属catalog
		 * 
		 * @return catalog
		 */
		public String getCatalog() {
			return catalog;
		}

		/**
		 * 设置Catalog
		 * 
		 * @param catalog
		 */
		public void setCatalog(String catalog) {
			this.catalog = catalog;
		}

		/**
		 * 获得表所在schema
		 * 
		 * @return
		 */
		public String getSchema() {
			return schema;
		}

		/**
		 * 设置 schema
		 * 
		 * @param schema
		 */
		public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
		 * 获得表/视图(等)的名称
		 * 
		 * @return 名称
		 */
		public String getName() {
			return name;
		}

		/**
		 * 设置名称
		 * 
		 * @param name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * 获得表的备注信息
		 * 
		 * @return 备注
		 */
		public String getRemarks() {
			return remarks;
		}

		/**
		 * 设置备注
		 * 
		 * @param remarks
		 *            备注
		 */
		public void setRemarks(String remarks) {
			this.remarks = remarks;
		}

		/**
		 * 获得表的类型
		 * 
		 * @return 类型
		 */
		public String getType() {
			return type;
		}

		/**
		 * 设置表类型
		 * 
		 * @param type
		 *            类型
		 */
		public void setType(String type) {
			this.type = type;
		}
	}

	/**
	 * 删除表中的约束
	 * 
	 * @param tablename
	 *            表名，支持Schema重定向
	 * @param constraintName
	 *            约束名称
	 * @throws SQLException
	 */
	public void dropConstraint(String tablename, String constraintName) throws SQLException {
		tablename = MetaHolder.toSchemaAdjustedName(tablename);
		Connection conn = this.getConnection();
		Statement st = null;
		try {
			st = conn.createStatement();
			dropConstraint0(tablename, constraintName, st);
		} finally {
			this.releaseConnection(conn);
			if (st != null)
				st.close();
		}
	}

	/**
	 * 删除表的所有外键，无论是其他表依赖此表的，还是此表依赖其他表的
	 * 
	 * @param tablename
	 *            表名(支持schema重定向)
	 * @throws SQLException
	 */
	public void dropAllForeignKey(String tablename) throws SQLException {
		tablename = MetaHolder.toSchemaAdjustedName(tablename);
		innerDropAllForeignKey(tablename, null);
	}

	/**
	 * 删除表的所有约束
	 * 
	 * @param tablename
	 *            表名(支持schema重定向)
	 * @throws SQLException
	 */
	public void dropAllConstraint(String tablename) throws SQLException {
		dropAllForeignKey(tablename);// 删除外键
		dropPrimaryKey(tablename);
	}

	/**
	 * 删除指定的表的主键
	 * 
	 * @param tablename
	 *            支持schema重定向
	 * @return true if drop success.
	 * @throws SQLException
	 */
	public boolean dropPrimaryKey(String tablename) throws SQLException {
		PrimaryKey pk = getPrimaryKey(tablename);
		if (pk != null) {
			dropConstraint(tablename, pk.getName());
			return true;
		}
		return false;
	}

	/**
	 * 删除指定名称的表
	 * 
	 * @param table
	 *            the name of table.
	 * @return true if table dropped.
	 * @throws SQLException
	 */
	public boolean dropTable(String table) throws SQLException {
		if (existTable(table)) {
			String sql = "drop table " + table;
			if (getProfile().has(Feature.DROP_CASCADE)) {
				sql += " cascade constraints";
			} else if (getProfile().notHas(Feature.NOT_SUPPORT_FOREIGN_KEY)) {
				dropAllForeignKey(table);
			}
			executeSql(sql, null);
			nextExpireTime=0;
			return true;
		}
		return false;
	}

	/**
	 * 得到当前元数据所属的数据源名称 get the datasource name of current metadata connection.
	 * 
	 * @return name of data source
	 */
	public String getDbkey() {
		return dbkey;
	}

	/**
	 * Delete the assign sequence or do nothing if the sequence not exists..
	 * 
	 * @param sequenceName
	 *            the name of sequence.
	 * @return true if drop success.
	 * @throws SQLException
	 * 
	 */
	public boolean dropSequence(String sequenceName) throws SQLException {
		return dropSequence(null, sequenceName);
	}

	/**
	 * Delete the assign sequence or do nothing if the sequence not exists..
	 * 
	 * @param schema
	 * @param sequenceName
	 * @return
	 * @throws SQLException
	 */
	public boolean dropSequence(String schema, String sequenceName) throws SQLException {
		String seqName = (schema == null ? sequenceName : schema + "." + sequenceName);
		if (exists(ObjectType.SEQUENCE, seqName)) {
			executeSql("drop sequence " + seqName, null);
			return true;
		}
		return false;
	}

	/**
	 * 枚举数据库对象类型
	 * <ul>
	 * <li>{@link #TABLE}<br>
	 * 数据库表</li>
	 * <li>{@link #SEQUENCE}<br>
	 * 数据库序列</li>
	 * <li>{@link #VIEW}<br>
	 * 数据库视图</li>
	 * <li>{@link #FUNCTION}<br>
	 * 自定义函数</li>
	 * <li>{@link #PROCEDURE}<br>
	 * 存储过程</li>
	 * </ul>
	 * <p>
	 */
	public static enum ObjectType {
		/**
		 * 表，包含各种临时表等
		 */
		TABLE,
		/**
		 * 序列
		 */
		SEQUENCE,
		/**
		 * 视图
		 */
		VIEW,
		/**
		 * 函数
		 */
		FUNCTION,
		/**
		 * 存储过程
		 */
		PROCEDURE
	}

	@Override
	public String toString() {
		return this.getTransactionId();
	}

	private static final String DROP_CONSTRAINT_SQL = "alter table %1$s drop constraint %2$s";

	/*
	 * 计算分表 通过基表的名称，查找出分表名(全部大写)
	 */
	private Set<String> calculateSubTables(String tableName, ITableMetadata meta, boolean isDefault) throws SQLException {
		long start = System.currentTimeMillis();
		List<Column> columns = getColumns(tableName);
		int baseColumnCount=columns.size();
		if(baseColumnCount==0){
			baseColumnCount=meta.getMetaFields().size();
		}
		List<TableInfo> tables = getDatabaseObject(ObjectType.TABLE, this.schema, tableName, Operator.MATCH_START);
		String tableNameWithoutSchema = StringUtils.substringAfterIfExist(tableName, ".");
		// 正则表达式计算
		Pattern suffix;
		if (isDefault) {
			PartitionTable pt = meta.getPartition();
			StringBuilder suffixRegexp = new StringBuilder(tableNameWithoutSchema);
			suffixRegexp.append(pt.appender());
			int n = 0;
			for (@SuppressWarnings("rawtypes") Entry<PartitionKey, PartitionFunction> entry : meta.getEffectPartitionKeys()) {
				PartitionKey key = entry.getKey();
				if (key.isDbName())
					continue;
				if (n > 0) {
					suffixRegexp.append(pt.keySeparator());// 分隔字符
				}
				if (DefaultPartitionCalculator.isNumberFun(key)) {
					suffixRegexp.append("\\d");
				} else {
					suffixRegexp.append("[a-zA-Z0-9]");
				}
				if (key.length() > 0) {
					suffixRegexp.append("{" + key.length() + "}");
				} else {
					suffixRegexp.append("+");
				}
			}
			suffix = Pattern.compile(suffixRegexp.toString());
		} else {
			suffix = Pattern.compile(tableNameWithoutSchema.concat("(_?[0-9]{1,4})+"));
		}
		Set<String> result = new HashSet<String>();
		String schema = meta.getSchema();
		for (TableInfo entry : tables) {
			if (suffix.matcher(entry.getName()).matches()) {
				String fullTableName = schema == null ? entry.getName() : schema + "." + entry.getName();
				List<Column> subColumns = getColumns(fullTableName);
				if (subColumns.size() == baseColumnCount) {
					result.add(fullTableName.toUpperCase());
				} else {
					LogUtil.info("The table [" + fullTableName + "]("+subColumns.size()+") seems like a subtable of [" + tableName + "], but their columns are not match.\n"+subColumns);
				}
			}
		}
		start = System.currentTimeMillis() - start;
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.debug("Scan Partition Tables for [" + tableName + "] at " + info + ", found " + result.size() + " result. cost " + start + " ms.");
		}
		return result;
	}

	private boolean innerExists(ObjectType type, String schema, String objectName) throws SQLException {
		objectName = info.profile.getObjectNameIfUppercase(objectName);

		if (schema == null)
			schema = this.getCurrentSchema();// 如果当前schema计算不正确，会出错
		switch (type) {
		case FUNCTION:
			return existsFunction(schema, objectName);
		case PROCEDURE:
			return existsProcdure(schema, objectName);
		case SEQUENCE:
			// String[] types=this.getTableTypes();
			// if(!ArrayUtils.contains(types, "SEQUENCE")){
			// throw new
			// UnsupportedOperationException("Current database "+info.profile+" not support check sequence.");
			// }
		}

		Connection conn = getConnection();
		DatabaseDialect trans = info.profile;
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = databaseMetaData.getTables(trans.getCatlog(schema), trans.getSchema(schema), objectName, new String[] { type.name() });
		try {
			return rs.next();
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	private List<Function> innerGetFunctions(String schema, String name) throws SQLException {
		if (schema == null) {
			schema = this.schema;
		}

		Connection conn = getConnection();
		DatabaseDialect profile = getProfile();
		if (profile.has(Feature.NOT_SUPPORT_USER_FUNCTION)) {
			return Collections.emptyList();
		}
		List<Function> result = new ArrayList<Function>();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = null;
		try {
			rs = databaseMetaData.getFunctions(profile.getCatlog(schema), profile.getSchema(schema), name);
			while (rs.next()) {
				Function function = new Function();
				function.setCatalog(rs.getString(1));
				function.setSchema(rs.getString(2));
				function.setName(rs.getString(3));
				function.setRemarks(rs.getString(4));
				function.setType(rs.getShort(5));
				function.setSpecificName(rs.getString(6));
				result.add(function);
			}
		} catch (java.sql.SQLFeatureNotSupportedException e) {
			LogUtil.warn(databaseMetaData.getDriverName() + " doesn't supprt getFunctions() defined in JDDBC 4.0.");
		} catch (AbstractMethodError e) { // Driver version is too old...
			StringBuilder sb = new StringBuilder("The driver ").append(databaseMetaData.getDriverName());
			sb.append(' ').append(databaseMetaData.getDriverVersion()).append(' ').append(databaseMetaData.getDatabaseMinorVersion());
			sb.append(" not implements JDBC 4.0, please upgrade you JDBC Driver.");
			throw new SQLException(sb.toString());
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
		return result;
	}

	private List<Function> innerGetProcedures(String schema, String procdureName) throws SQLException {
		if (schema == null) {
			schema = this.schema;
		}

		DatabaseDialect profile = getProfile();
		Connection conn = getConnection();
		DatabaseMetaData databaseMetaData = conn.getMetaData();
		ResultSet rs = null;
		try {
			List<Function> result = new ArrayList<Function>();
			rs = databaseMetaData.getProcedures(profile.getCatlog(schema), profile.getSchema(schema), procdureName);
			while (rs.next()) {
				Function function = new Function(ObjectType.PROCEDURE);
				function.setCatalog(rs.getString(1));
				function.setSchema(rs.getString(2));
				function.setName(rs.getString(3));
				function.setRemarks(rs.getString(7));
				function.setType(rs.getShort(8));
				function.setSpecificName(rs.getString(9));
				result.add(function);
			}
			return result;
		} finally {
			DbUtils.close(rs);
			releaseConnection(conn);
		}
	}

	private void execute(Statement st, String sql) throws SQLException {
		if (ORMConfig.getInstance().isDebugMode())
			LogUtil.show(sql + " |" + getTransactionId());
		try {
			st.executeUpdate(sql);
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			throw e;
		}
	}

	private Map<Field, ColumnType> getColumnMap(ITableMetadata meta) {
		Map<Field, ColumnType> map = new HashMap<Field, ColumnType>();
		for (MappingType<?> mapping : meta.getMetaFields()) {
			map.put(mapping.field(), mapping.get());
		}
		return map;
	}

	private String getTransactionId() {
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(info.profile.getName()).append(':').append(getDbName()).append('@').append(Thread.currentThread().getId()).append(']');
		return sb.toString();
	}

	private Connection getConnection() throws SQLException {
		return super.poll();
	}

	private void releaseConnection(Connection con) {
		super.offer(con);
	}

	private void dropConstraint0(String tablename, String constraintName, Statement st) throws SQLException {
		String sql = String.format(DROP_CONSTRAINT_SQL, tablename, constraintName);
		execute(st, sql);
	}

	/*
	 * 删除指定表的外键
	 * 
	 * @param tablename 表名(支持Schema重定向)
	 * 
	 * @param referenceBy true：删除此表被其他表引用的外键，false:删除此表引用其他表的外键 null 全删
	 * 
	 * @throws SQLException
	 */
	private void innerDropAllForeignKey(String tablename, Boolean referenceBy) throws SQLException {
		Connection conn = this.getConnection();
		Statement st = null;
		try {
			st = conn.createStatement();
			if (referenceBy == null || referenceBy == Boolean.TRUE) {
				for (ForeignKey fk : getForeignKeyReferenceBy(tablename)) {
					dropConstraint0(fk.getFromTable(), fk.getName(), st);
				}
			}
			if (referenceBy == null || referenceBy == Boolean.FALSE) {
				for (ForeignKey fk : getForeignKey(tablename)) {
					dropConstraint0(fk.getFromTable(), fk.getName(), st);
				}
			}
		} finally {
			this.releaseConnection(conn);
			if (st != null)
				st.close();
		}
	}

	/*
	 * 创建得到表中使用序列值的字段的最大值的SQL语句
	 * 
	 * @param schema schema名称
	 * 
	 * @param tableName 不带schema的表名
	 * 
	 * @param sequenceColumnName 使用序列值的字段名称
	 * 
	 * @return
	 */
	private static String createGetMaxSequenceColumnValueStatement(String schema, String tableName, String sequenceColumnName) {
		if (schema != null) {
			tableName = schema + "." + tableName;
		}
		return StringUtils.concat("select max(", sequenceColumnName, ") from " + tableName);
	}

	private final static Transformer FK_TRANSFORMER = new Transformer(ForeignKey.class);

	public boolean clearTableMetadataCache(ITableMetadata meta) {
		return subTableData.remove(meta)!=null;
	}

	protected boolean hasRemarkFeature() {
		if (JefConfiguration.getBoolean(DbCfg.DB_NO_REMARK_CONNECTION, false)) {
			return false;
		}
		DatabaseDialect profile;
		if(this.info==null){
			ConnectInfo info = DbUtils.tryAnalyzeInfo(ds, false);
			if (info == null) {
				Connection conn = null;
				try {
					conn = ds.getConnection();
					info = DbUtils.tryAnalyzeInfo(conn);
				} catch (SQLException e) {
					return false;
				} finally {
					DbUtils.closeConnection(conn);
				}
			}
			profile=info.getProfile();
		}else{
			profile=info.getProfile();
		}
		return profile.has(Feature.REMARK_META_FETCH);
	}

	@Override
	protected String getTestSQL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void processCheck(Connection conn2) {
		// TODO Auto-generated method stub
		
	}
}