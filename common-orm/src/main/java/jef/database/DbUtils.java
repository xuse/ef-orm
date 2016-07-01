/*
' * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityExistsException;
import javax.persistence.FetchType;
import javax.persistence.PersistenceException;
import javax.persistence.QueryTimeoutException;
import javax.sql.DataSource;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.log.LogUtil;
import jef.database.annotation.Cascade;
import jef.database.annotation.JoinType;
import jef.database.datasource.DataSourceInfo;
import jef.database.datasource.DataSourceWrapper;
import jef.database.datasource.DataSources;
import jef.database.datasource.IRoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.innerpool.PartitionSupport;
import jef.database.jsqlparser.parser.JpqlParser;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.parser.TokenMgrError;
import jef.database.jsqlparser.statement.create.ColumnDefinition;
import jef.database.jsqlparser.statement.create.CreateTable;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.AbstractRefField;
import jef.database.meta.DbProperty;
import jef.database.meta.ITableMetadata;
import jef.database.meta.JoinKey;
import jef.database.meta.JoinPath;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.query.AbstractEntityMappingProvider;
import jef.database.query.AbstractJoinImpl;
import jef.database.query.ConditionQuery;
import jef.database.query.DefaultPartitionCalculator;
import jef.database.query.EntityMappingProvider;
import jef.database.query.JoinElement;
import jef.database.query.JoinUtil;
import jef.database.query.JpqlExpression;
import jef.database.query.OrderField;
import jef.database.query.PartitionCalculator;
import jef.database.query.Query;
import jef.database.query.RefField;
import jef.database.query.ReferenceType;
import jef.database.query.SelectsImpl;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.executor.DbTask;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.GenericUtils;
import jef.tools.security.cplus.TripleDES;
import jef.tools.string.CharUtils;

import com.google.common.base.Objects;

public final class DbUtils {

	// 在db rac的场景，在初始化数据源时需要把dbKey和racId的对应关系注册到该全局属性中，orm需要根据映射关系合并同racId的连接。
	// private static Map<String, String> dbKey2RacIds = new HashMap<String,
	// String>();
	// private static Map<String, List<String>> racId2DbKeys = new
	// HashMap<String, List<String>>();
	// private static ThreadLocal<Boolean> isRouted = new
	// ThreadLocal<Boolean>();
	public static final int NO_RAC_ID = -1;

	public static PartitionCalculator partitionUtil = new DefaultPartitionCalculator();

	/**
	 * 线程池。线程池有以下作用 1、在分库分表时使用线程 2、在JTA事务管理模式下，为了避免在JTA中执行DDL，因此不得不将代码在新的线程中执行。
	 * 
	 * 线程池策略： 1、平时最大线程数为CPU个数乘以2. 2、任务堆积最大256个
	 * 3、超出256个堆积任务后，如果线程数还不到128个，则开启新线程消除堆积。如果已经达到，则让请求线程自行完成任务
	 */
	public static ExecutorService es;

	static {
		int processorCount = Runtime.getRuntime().availableProcessors();
		es = new ThreadPoolExecutor(processorCount*2, processorCount * 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(processorCount * 4), Executors.defaultThreadFactory(), new CallerRunsPolicy());
	}

	/**
	 * 获取数据库加密的密钥,目前使用固定密钥
	 * 
	 * @return
	 * @throws IOException
	 */
	private static byte[] getEncryptKey() {
		String s = JefConfiguration.get(DbCfg.DB_ENCRYPTION_KEY);
		if (StringUtils.isEmpty(s)) {
			s = "781296-5e32-89122";
		}
		return s.getBytes();
	}

	/**
	 * 并行执行多个数据库任务
	 * 
	 * @param tasks
	 * @throws SQLException
	 */
	public static void parallelExecute(List<DbTask> tasks) throws SQLException {
		CountDownLatch latch = new CountDownLatch(tasks.size());
		Queue<SQLException> exceptions = new ConcurrentLinkedQueue<SQLException>();
		Queue<Throwable> throwables = new ConcurrentLinkedQueue<Throwable>();
		for (DbTask task : tasks) {
			task.prepare(latch, exceptions, throwables);
			DbUtils.es.execute(task);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new SQLException(e);
		}
		if (!exceptions.isEmpty()) {
			throw DbUtils.wrapExceptions(exceptions);
		}
		if (!throwables.isEmpty()) {
			throw DbUtils.toRuntimeException(throwables.peek());
		}
	}

	/*
	 * 处理SQL执行错误 <strong>注意，这个方法执行期间会调用连接，因此必须在这个方法执行完后才能释放连接</strong>
	 * 
	 * @param e
	 * 
	 * @param tablename
	 * 
	 * @param conn
	 */
	public static void processError(SQLException e, String tablename, OperateTarget conn) {
		if (conn.getProfile().isIOError(e)) {
			conn.notifyDisconnect(e);
		}
		DebugUtil.setSqlState(e, tablename);
	}

	/**
	 * 用于查找两个表之间的外键
	 * 
	 * @param class1
	 * @return 外键关系
	 */
	public static Reference findPath(ITableMetadata from, ITableMetadata target) {
		for (Reference r : from.getRefFieldsByRef().keySet()) {
			if (r.getTargetType() == target) {
				return r;
			}
		}
		return null;
	}

	/**
	 * 查找到目标类型的引用关系。只允许查找到一个到目标类型的关系。如果找到0个或者多个，那么会抛出异常。
	 * 
	 * @param class1
	 * @return 外键关系
	 * @throws IllegalArgumentException
	 */
	public static Reference findDistinctPath(ITableMetadata from, ITableMetadata target) {
		Reference ref = null;
		for (Reference reference : from.getRefFieldsByRef().keySet()) {
			if (reference.getTargetType() == target) {
				if (ref != null) {
					throw new IllegalArgumentException("There's more than one reference to [" + target.getSimpleName() + "] in type [" + from.getSimpleName() + "],please assign the reference field name.");
				}
				ref = reference;
			}
		}
		if (ref == null) {
			throw new IllegalArgumentException("Target class " + target.getSimpleName() + "of fileter-condition is not referenced by " + from.getSimpleName());
		}
		return ref;
	}

	/**
	 * 如果列名或表名碰到了数据库的关键字，那么就要增加引号一类字符进行转义
	 * 
	 * @param profile
	 * @param name
	 * @return
	 */
	public static final String escapeColumn(DatabaseDialect profile, String name) {
		if (name == null)
			return name;
		String w = profile.getProperty(DbProperty.WRAP_FOR_KEYWORD);
		if (w != null && profile.containKeyword(name)) {
			StringBuilder sb = new StringBuilder(name.length() + 2);
			sb.append(w.charAt(0)).append(name).append(w.charAt(1));
			return sb.toString();
		}
		return name;
	}

	/**
	 * 根据datasource解析连接信息
	 * 
	 * @param ds
	 * @param updateDataSourceProperties
	 *            在能解析出ds的情况下，向datasource的连接属性执行注入
	 * @return
	 * @throws SQLException
	 */
	public static ConnectInfo tryAnalyzeInfo(DataSource ds, boolean updateDataSourceProperties) {
		if (ds instanceof IRoutingDataSource) {
			IRoutingDataSource rds = (IRoutingDataSource) ds;
			Entry<String, DataSource> e = rds.getDefaultDatasource();
			if (e == null) {// 更见鬼了，没法获得缺省的DataSource。
				Collection<String> names = rds.getDataSourceNames();
				if (!names.isEmpty()) {
					String name = names.iterator().next();
					LogUtil.warn("Can not determine default datasource name. choose [" + name + "] as default datasource.");
					return tryAnalyzeInfo(rds.getDataSource(name), updateDataSourceProperties);
				}
			} else {
				return tryAnalyzeInfo(e.getValue(), updateDataSourceProperties);
			}
		}
		DataSourceWrapper dsw = DataSources.wrapFor(ds);
		if (dsw != null) {
			ConnectInfo info = new ConnectInfo();
			DbUtils.processDataSourceOfEnCrypted(dsw);

			info.url = dsw.getUrl();
			info.user = dsw.getUser();
			info.password = dsw.getPassword();
			DatabaseDialect profile = info.parse();// 解析，获得profile, 解析出数据库名等信息
			if (updateDataSourceProperties)
				profile.processConnectProperties(dsw);
			return info;// 理想情况
		}
		return null;
	}

	/**
	 * 根据已有的连接解析连接信息
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static ConnectInfo tryAnalyzeInfo(Connection conn) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		ConnectInfo info = new ConnectInfo();
		info.user = meta.getUserName();
		info.url = meta.getURL();
		info.parse();// 解析，获得profile, 解析出数据库名等信息
		return info;
	}

	/**
	 * Close the given JDBC Connection and ignore any thrown exception. This is
	 * useful for typical finally blocks in manual JDBC code.
	 * 
	 * @param con
	 *            the JDBC Connection to close (may be {@code null})
	 */
	public static void closeConnection(Connection con) {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException ex) {
				LogUtil.exception("Could not close JDBC Connection", ex);
			} catch (Throwable ex) {
				LogUtil.exception("Unexpected exception on closing JDBC Connection", ex);
			}
		}
	}

	/**
	 * 将SQL异常构成链表
	 * 
	 * @param errors
	 * @return
	 */
	public static final SQLException wrapExceptions(Collection<SQLException> errors) {
		if (errors == null || errors.isEmpty())
			return null;
		Iterator<SQLException> iter = errors.iterator();
		SQLException root = iter.next();
		SQLException last = root;
		while (iter.hasNext()) {
			SQLException current = iter.next();
			last.setNextException(current);
			last = current;
		}
		return root;
	}

	/**
	 * 数据库密码解密
	 */
	public static String decrypt(String pass) {
		TripleDES t = new TripleDES();
		String text = t.decipher2(getEncryptKey(), pass);
		return text;
	}

	/**
	 * 数据库密码解密
	 */
	public static String ecrypt(String pass) throws IOException {
		TripleDES t = new TripleDES();
		String text = t.cipher2(getEncryptKey(), pass);
		return text;
	}

	/**
	 * 处理DataSource中的密码
	 * 
	 * @param ds
	 */
	public static void processDataSourceOfEnCrypted(DataSourceInfo ds) {
		boolean flag = JefConfiguration.getBoolean(DbCfg.DB_PASSWORD_ENCRYPTED, false);
		if (!flag) {
			return;
		}
		String old = ds.getPassword();
		if (old != null && old.matches("[a-fA-F0-9]{16,}")) {
			ds.setPassword(decrypt(old));
		}
	}

	/**
	 * 解析select后的语句
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	public static List<SelectItem> parseSelectItems(String sql) throws ParseException {
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		return parser.SelectItemsList();
	}

	public static ColumnDefinition parseColumnDef(String def) throws ParseException {
		String sql = StringUtils.concat("create table A (B ", def, ")");
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		CreateTable ct = parser.CreateTable();
		return ct.getColumnDefinitions().get(0);
	}

	/**
	 * 解析select语句
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static Select parseSelect(String sql) throws ParseException {
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		return parser.Select();
	}

	/**
	 * 解析Select语句(原生SQL)
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static Select parseNativeSelect(String sql) throws ParseException {
		StSqlParser parser = new StSqlParser(new StringReader(sql));
		return parser.Select();
	}

	/**
	 * 解析表达式
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static Expression parseExpression(String sql) throws ParseException {
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		return parser.SimpleExpression();
	}

	/**
	 * 解析OrderBy元素
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static OrderBy parseOrderBy(String sql) {
		StSqlParser parser = new StSqlParser(new StringReader("ORDER BY " + sql));
		try {
			return parser.OrderByElements();
		} catch (ParseException e) {
			throw new PersistenceException(sql, e);
		}
	}

	/**
	 * 解析二元表达式
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static Expression parseBinaryExpression(String sql) throws ParseException {
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		return parser.Expression();
	}

	/**
	 * 解析语句
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	public static jef.database.jsqlparser.visitor.Statement parseStatement(String sql) throws ParseException {
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		try {
			return parser.Statement();
		} catch (ParseException e) {
			LogUtil.show("ErrorSQL:" + sql);
			throw e;
		} catch (TokenMgrError e) {
			LogUtil.show("ErrorSQL:" + sql);
			throw e;
		}
	}

	/**
	 * 对于检测基本查询是否需要展开成连接查询，如果需要就展开
	 * 
	 * @param <T>
	 * @param queryObj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IQueryableEntity> JoinElement toReferenceJoinQuery(Query<T> queryObj, List<Reference> excludeRef) {
		// 得到可以合并查询的引用关系
		Map<Reference, List<AbstractRefField>> map = queryObj.isCascadeViaOuterJoin() ? DbUtils.getMergeAsOuterJoinRef(queryObj) : Collections.EMPTY_MAP;
		Query<?>[] otherQuery = queryObj.getOtherQueryProvider();

		if (otherQuery.length == 0 && map.isEmpty()) {
			return queryObj;
		}
		// 拼装出带连接的查询请求
		AbstractJoinImpl j = DbUtils.getJoin(queryObj, map, ArrayUtils.asList(otherQuery), excludeRef);
		if (j != null) {
			j.setFetchSize(queryObj.getFetchSize());
			j.setMaxResult(queryObj.getMaxResult());
			j.setQueryTimeout(queryObj.getQueryTimeout());
			if (queryObj.getSelectItems() != null) {
				List<QueryAlias> qs = j.allElements();
				for (int i = 0; i < qs.size(); i++) {
					qs.get(i).setAlias("T" + (i + 1));
				}
				SelectsImpl select = new jef.database.query.SelectsImpl(qs);
				select.merge((AbstractEntityMappingProvider) queryObj.getSelectItems());
				j.setSelectItems(select);
			}
			//TODO 其实cacheable, Transformer, attribute都是Query在转换时需要保持不变的属性，可以设法抽取到公共类中。
			j.setResultTransformer(queryObj.getResultTransformer());
			j.setCacheable(queryObj.isCacheable());
			// FilterCondition合并
			if (queryObj.getFilterCondition() != null) {
				for (QueryAlias qa : j.allElements()) {
					if (qa.getStaticRef() != null) {
						List<Condition> con = queryObj.getFilterCondition().get(qa.getStaticRef());
						if (con != null) {
							j.addRefConditions(qa.getQuery(), con);
						}
					}
				}
			}
			return j;
		} else {
			return queryObj;
		}
	}

	/**
	 * 将带下划线的名称，转为不带下划线的名称<br>
	 * 例如： you_are_boy -> YouAreBoy
	 * 
	 * @param name
	 *            待转的名称
	 * @param capitalize
	 *            首字母是否要大写
	 * @return 转换后的名称
	 */
	public static String underlineToUpper(String name, boolean capitalize) {
		char[] r = new char[name.length()];
		int n = 0;
		boolean nextUpper = capitalize;
		for (char c : name.toCharArray()) {
			if (c == '_') {
				nextUpper = true;
			} else {
				if (nextUpper) {
					r[n] = Character.toUpperCase(c);
					nextUpper = false;
				} else {
					r[n] = Character.toLowerCase(c);
				}

				n++;
			}
		}
		return new String(r, 0, n);
	}

	/**
	 * 将带大小写的名称，转换为全小写但带下划线的名称 例如: iLoveYou -> i_love_you
	 * 
	 * @param name
	 *            待转换为名称
	 * @return 转换后的名称
	 */
	public static String upperToUnderline(String name) {
		if (name == null)
			return null;
		boolean skipUpper = true;
		StringBuilder sb = new StringBuilder();
		char[] chars = name.toCharArray();
		sb.append(chars[0]);
		for (int i = 1; i < chars.length; i++) {
			if (CharUtils.isUpperAlpha(chars[i])) {
				if (!skipUpper) {
					sb.append('_').append(chars[i]);
					skipUpper = true;
				} else {
					if (i + 2 < chars.length && CharUtils.isLowerAlpha(chars[i + 1])) {
						sb.append('_').append(chars[i]);
					} else {
						sb.append(chars[i]);
					}
				}
			} else {
				sb.append(chars[i]);
				skipUpper = false;
			}
		}
		return sb.toString().toUpperCase();
	}

	/**
	 * 设置指定的值到主键
	 * 
	 * @param data
	 *            对象
	 * @param pk
	 *            主键，可以是Map或单值
	 */
	public static void setPrimaryKeyValue(IQueryableEntity data, Object pk) throws PersistenceException {
		List<ColumnMapping> fields = MetaHolder.getMeta(data).getPKFields();
		if (fields.isEmpty())
			return;
		Assert.notNull(pk);
		// if (pk instanceof Map) {
		// Map<String, Object> pkMap = (Map<String, Object>) pk;
		// BeanWrapper wrapper = BeanWrapper.wrap(data, BeanWrapper.FAST);
		// for (Field f : fields) {
		// if (wrapper.isWritableProperty(f.name())) {
		// wrapper.setPropertyValue(f.name(), pkMap.get(f.name()));
		// }
		// }
		// } else
		if (pk.getClass().isArray()) {
			int length = Array.getLength(pk);
			int n = 0;
			Assert.isTrue(length == fields.size());
			for (ColumnMapping f : fields) {
				f.getFieldAccessor().set(data, Array.get(pk, n++));
			}
		} else {
			if (fields.size() != 1) {
				throw new PersistenceException("No Proper PK fields!");
			}
			fields.get(0).getFieldAccessor().set(data, pk);
		}
	}

	/**
	 * 提供主键的值
	 */
	public static Map<String, Object> getPrimaryKeyValueMap(IQueryableEntity data) {
		ITableMetadata meta = MetaHolder.getMeta(data);
		int len = meta.getPKFields().size();
		if (len == 0)
			return null;
		Map<String, Object> keyValMap = new HashMap<String, Object>();
		for (int i = 0; i < len; i++) {
			ColumnMapping field = meta.getPKFields().get(i);
			if (!isValidPKValue(data, meta, field)) {
				return null;
			}
			keyValMap.put(field.fieldName(), field.getFieldAccessor().get(data));
		}
		return keyValMap;
	}

	/**
	 * 提供主键的值
	 */
	public static List<Object> getPrimaryKeyValue(IQueryableEntity data) {
		ITableMetadata meta = MetaHolder.getMeta(data);
		if (meta.getPKFields().isEmpty())
			return null;

		int len = meta.getPKFields().size();
		Object[] result = new Object[len];
		for (int i = 0; i < len; i++) {
			ColumnMapping field = meta.getPKFields().get(i);
			if (!isValidPKValue(data, meta, field)) {
				return null;
			}
			result[i] = field.getFieldAccessor().get(data);
		}
		return Arrays.asList(result);
	}

	// 从实体中获取主键的值，这里的实体都必须是已经从数据库中选择出来的，因此无需校验主键值是否合法
	public static List<Serializable> getPKValueSafe(IQueryableEntity data) {
		ITableMetadata meta = MetaHolder.getMeta(data);
		if (meta.getPKFields().isEmpty())
			return null;
		int len = meta.getPKFields().size();
		Serializable[] result = new Serializable[len];
		for (int i = 0; i < len; i++) {
			ColumnMapping field = meta.getPKFields().get(i);
			result[i] = (Serializable) field.getFieldAccessor().get(data);
		}
		return Arrays.asList(result);
	}

	/**
	 * 将field转换为列名（包含表的别名）
	 * 
	 * @param field
	 * @param feature
	 * @param tableAlias
	 * @return
	 * @deprecated use
	 *             {@linkplain #toColumnName(ColumnMapping, DatabaseDialect, String)}
	 *             instead
	 */
	public static String toColumnName(Field field, DatabaseDialect feature, String tableAlias) {
		if (field instanceof MetadataContainer) {
			ITableMetadata meta = ((MetadataContainer) field).getMeta();
			return getColumnName(meta, field, tableAlias, feature);
		}
		ITableMetadata meta = getTableMeta(field);
		if (field instanceof JpqlExpression) {
			return ((JpqlExpression) field).toSqlAndBindAttribs(null, feature);
		} else {
			return getColumnName(meta, field, tableAlias, feature);
		}
	}

	/**
	 * 代替上面的方法，性能更好,也更安全
	 * 
	 * @param targetField
	 * @param profile
	 * @param tableAlias
	 * @return
	 */
	public static String toColumnName(ColumnMapping fld, DatabaseDialect profile, String alias) {
		if (alias != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(alias).append('.').append(fld.getColumnName(profile, true));
			return sb.toString();
		} else {
			return fld.getColumnName(profile, true);
		}
	}

	/**
	 * 返回某个field的数据库列名称
	 * 
	 * @param field
	 *            field
	 * @param alias
	 *            数据库表别名
	 * @param profile
	 *            当前数据库方言
	 * @return 数据库列名称
	 * 
	 * @deprecated 不够安全
	 */
	public static String getColumnName(ITableMetadata meta, Field fld, String alias, DatabaseDialect profile) {
		if (alias != null) {
			if (fld instanceof JpqlExpression) {
				throw new UnsupportedOperationException();
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(alias).append('.').append(meta.getColumnName(fld, profile, true));
				return sb.toString();
			}
		} else {
			return meta.getColumnName(fld, profile, true);
		}
	}

	/*
	 * 当存在引用列时的连接创建方式。由于连接中的表名需要转换后重映射到字段上，因此需要
	 * 
	 * @param d 查询实体
	 * 
	 * @param map 通过元数据配置的表关联
	 * 
	 * @param queryProvider : 额外的外表关联
	 * 
	 * @return
	 */
	protected static AbstractJoinImpl getJoin(Query<?> d, Map<Reference, List<AbstractRefField>> map, List<Query<?>> queryProvider, List<Reference> exclude) {
		AbstractJoinImpl join = null;
		// 处理默认需要的连接查询：该种关联只关联一级，不会递归关联。
		for (Reference r : map.keySet()) {
			if (exclude != null && exclude.contains(r))
				continue;
			Query<?> tQuery = null;
			for (Query<?> t : queryProvider) {
				if (t.getInstance().getClass() == r.getTargetType().getThisType()) {
					queryProvider.remove(t);
					tQuery = t;
					break;
				}
			}
			if (join == null) {
				join = JoinUtil.create(d, r, tQuery);
				Assert.notNull(join, "Invalid Reference:" + r.toString());
			} else {
				join = JoinUtil.create(join, r, tQuery);
				Assert.notNull(join, "Invalid Reference:" + r.toString());
			}
		}

		// 还有一些REF条件，可能隐式地指定了若干的外部查询实例，此时需要将这些隐式查询实例添加到Join上
		List<QueryAlias> qs = join == null ? Arrays.asList(new QueryAlias(null, d)) : join.allElements();
		AbstractEntityMappingProvider tmpContext = new SqlContext(-1, qs, null);
		for (Condition c : d.getConditions()) {
			checkIfThereIsExQueryInRefField(c.getField(), tmpContext, queryProvider);
		}
		for (OrderField c : d.getOrderBy()) {
			checkIfThereIsExQueryInRefField(c.getField(), tmpContext, queryProvider);
		}

		// 处理其他的额外Query……注意要设置好拼装路径
		while (queryProvider.size() > 0) {
			int left = queryProvider.size();
			for (Iterator<Query<?>> iter = queryProvider.iterator(); iter.hasNext();) {
				Query<?> tq = iter.next();
				AbstractJoinImpl ng = JoinUtil.create(join == null ? d : join, tq, null, null, false);
				if (ng != null) {
					iter.remove();
					join = ng;
				}
			}
			if (left == queryProvider.size()) {// reverse look for...
				for (Iterator<Query<?>> iter = queryProvider.iterator(); iter.hasNext();) {
					Query<?> tq = iter.next();
					AbstractJoinImpl ng = JoinUtil.create(join == null ? d : join, tq, null, null, true);
					if (ng != null) {
						iter.remove();
						join = ng;
					}
				}
			}

			if (left == queryProvider.size()) {// 用户提供的额外查询表实例无法拼装到已有的查询对象上
				LogUtil.error("There 's still " + queryProvider.size() + " query not added into join.");
				break;
			}
		}
		if (join != null)
			join.fillAttribute((Query<?>) d);
		return join;
	}

	/*
	 * 递归检查field绑定情况(如果怕field是RefField……)
	 */
	private static void checkIfThereIsExQueryInRefField(Field field, AbstractEntityMappingProvider tmpContext, List<Query<?>> qt) {
		// 为了在RefField中省略默认的查询，所以要对所有条件树中的refField进行检查，将未指定的Query实例自动补全
		if (field instanceof RefField) {
			rebindRefField((RefField) field, tmpContext, qt);
		} else if (field instanceof IConditionField) {
			IConditionField condf = (IConditionField) field;
			processConditionField(condf, tmpContext, qt);
		}
	}

	// 检查所有条件中的REFField(递归)
	private static void rebindRefField(RefField ref, AbstractEntityMappingProvider tmpContext, List<Query<?>> qt) {
		if (!ref.isBindOn(tmpContext)) {
			Query<?> refQuery = ref.getInstanceQuery(null);
			for (Query<?> extQuery : qt) {
				if (refQuery == extQuery) {
					return;
				} else if (refQuery.getType() == extQuery.getType() && refQuery.getConditions().equals(extQuery.getConditions())) {
					// ref.rebind(extQuery,null);
					return;
				}
			}
			qt.add(refQuery);
		}
	}

	// 检查所有条件中的REFField(递归)
	private static void processConditionField(IConditionField container, AbstractEntityMappingProvider tmpContext, List<Query<?>> qt) {
		for (Condition c : container.getConditions()) {
			Field field = c.getField();
			if (field instanceof IConditionField) {
				processConditionField((IConditionField) field, tmpContext, qt);
			} else if (field instanceof RefField) {
				rebindRefField((RefField) field, tmpContext, qt);
			}
		}
	}

	/**
	 * 转换为合理的集合类型容器
	 * 
	 * @param subs
	 * @param container
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static Object toProperContainerType(Collection<? extends IQueryableEntity> subs, Class<?> container, Class<?> bean, AbstractRefField config) throws SQLException {
		if (container.isAssignableFrom(subs.getClass())) {
			return subs;
		}
		if (container == Set.class) {
			HashSet set = new HashSet();
			set.addAll(subs);
			return set;
		} else if (container == List.class) {
			ArrayList list = new ArrayList();
			list.addAll(subs);
			return list;
		} else if (container == Array.class) {
			return subs.toArray();
		} else if (container == Map.class) {
			Cascade cascade = config.getCascadeInfo();
			if (cascade == null) {
				throw new SQLException("@Cascade annotation is required for Map mapping " + config.toString());
			}
			Map map = new HashMap();
			String key = cascade.keyOfMap();
			BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(bean);
			if (StringUtils.isEmpty(cascade.valueOfMap())) {
				for (IQueryableEntity e : subs) {
					map.put(ba.getProperty(e, key), e);
				}
			} else {
				String vField = cascade.valueOfMap();
				for (IQueryableEntity e : subs) {
					map.put(ba.getProperty(e, key), ba.getProperty(e, vField));
				}
			}
			return map;
		}
		throw new SQLException("the type " + container.getName() + " is not supported as a collection container.");
	}

	/**
	 * 得到外连接加载的引用。
	 * 
	 * @param data
	 * @return
	 */
	protected static Map<Reference, List<AbstractRefField>> getMergeAsOuterJoinRef(Query<?> q) {
		Map<Reference, List<AbstractRefField>> result = new HashMap<Reference, List<AbstractRefField>>(5);
		// 获得所有未配置为延迟加载的引用。
		for (Map.Entry<Reference, List<AbstractRefField>> entry : q.getMeta().getRefFieldsByRef().entrySet()) {
			Reference key = entry.getKey();
			if (key.getType().isToOne()) {
				List<AbstractRefField> value = entry.getValue();
				AbstractRefField first = value.get(0);
				if (first.getFetch() == FetchType.LAZY) {
					continue;
				}
				result.put(key, value);
			}
		}
		// 过滤掉一部分因为使用了过滤条件而不得不延迟加载的应用
		if (q.getFilterCondition() != null) {
			for (Reference ref : q.getFilterCondition().keySet()) {
				if (ref.getType().isToOne() && ref.getJoinType() != JoinType.LEFT) {
					result.remove(ref);
				}
			}
		}
		return result;
	}

	/**
	 * 得到二次加载的Ref
	 * 
	 * @param data
	 *            表对象
	 * @param excludeReference
	 *            需要排除的关联，为null表示默认方式。为空表示全部延迟加载
	 * @return
	 */
	protected static Map<Reference, List<AbstractRefField>> getLazyLoadRef(ITableMetadata data, Collection<Reference> excludeReference) {
		// ==null时，对单关联使用外连接，对多关联使用延迟加载,上个版本的形式
		// 应该逐渐淘汰的形式
		if (excludeReference == null) {
			Map<Reference, List<AbstractRefField>> result = new HashMap<Reference, List<AbstractRefField>>(5);
			for (Map.Entry<Reference, List<AbstractRefField>> entry : data.getRefFieldsByRef().entrySet()) {
				Reference key = entry.getKey();
				ReferenceType type = key.getType();
				List<AbstractRefField> value = entry.getValue();
				if (type.isToOne()) {
					if (value.get(0).getFetch() == FetchType.LAZY) {
						result.put(key, value);
					}
				} else {
					result.put(key, value);
				}
			}
			return result;
			// 对单关联和对多关联都使用延迟加载的场合
		} else if (excludeReference.isEmpty()) {
			return data.getRefFieldsByRef();
			// !!今后主流的形式,过滤掉已经合并加载的ref
		} else {
			Map<Reference, List<AbstractRefField>> result = new HashMap<Reference, List<AbstractRefField>>(data.getRefFieldsByRef());
			for (Reference ref : excludeReference) {
				result.remove(ref);
			}
			return result;
		}
	}

	/**
	 * 得到定义的class
	 * 
	 * @param field
	 * @return
	 */
	public static AbstractMetadata getTableMeta(Field field) {
		Assert.notNull(field);
		if (field instanceof MetadataContainer) {
			return (AbstractMetadata) ((MetadataContainer) field).getMeta();
		}
		if (field instanceof Enum) {
			// FIXME 这个算法对原始功能是适用的，但当动态扩展等系列功能出现后，适用上有一定问题。
			Class<?> c = field.getClass().getDeclaringClass();
			Assert.isTrue(IQueryableEntity.class.isAssignableFrom(c), field + " is not a defined in a IQueryableEntity's meta-model.");
			return MetaHolder.getMeta(c.asSubclass(IQueryableEntity.class));
		} else {
			throw new IllegalArgumentException("method 'getTableMeta' doesn't support field type of " + field.getClass());
		}
	}

	/**
	 * 得到列的完整定义
	 * 
	 * @param field
	 * @return
	 */
	public static ColumnMapping toColumnMapping(Field field) {
		if (field instanceof ColumnMapping) {
			return (ColumnMapping) field;
		} else if (field instanceof MetadataContainer) {
			return ((MetadataContainer) field).getMeta().getColumnDef(field);
		} else if (field instanceof Enum) {
			Class<?> c = field.getClass().getDeclaringClass();
			Assert.isTrue(IQueryableEntity.class.isAssignableFrom(c), field + " is not a defined in a IQueryableEntity's meta-model.");
			ITableMetadata meta = MetaHolder.getMeta(c);
			return meta.getColumnDef(field);
		}
		throw new IllegalArgumentException("method 'getTableMeta' doesn't support field type of " + field.getClass());
	}

	/**
	 * 根据引用关系字段，填充查询条件
	 * 
	 * @param bean
	 * @param rs
	 * @param query
	 * @return
	 */
	protected static boolean appendRefCondition(BeanWrapper bean, JoinPath rs, Query<?> query, List<Condition> filters) {
		query.clearQuery();
		boolean hasValue = false;
		for (JoinKey r : rs.getJoinKeys()) {
			Object value = bean.getPropertyValue(r.getLeft().name());
			query.addCondition(r.getRightAsField(), value);
			if (value != null)
				hasValue = true;
		}
		// 辅助过滤条件，不作为hasValue标记
		for (JoinKey condition : rs.getJoinExpression()) {
			Field f = condition.getLeft();
			if (f == null) {
				continue;
			}
			if (f instanceof SqlExpression) {
				query.addCondition(condition);
				continue;
			}
			if (f instanceof JpqlExpression) {
				query.addCondition(condition);
				continue;
			}
			ITableMetadata meta = DbUtils.getTableMeta(f);
			if (meta == query.getMeta()) {
				query.addCondition(condition);
			}
		}
		if (filters != null) {
			Query<?> bq = query;
			bq.getConditions().addAll(filters);
		}
		return hasValue;
	}

	/**
	 * 当请求为空时，调用此方法，将有效的主键字段到请求中
	 * 
	 * @param obj
	 * @param query
	 * @return
	 */
	protected static void fillConditionFromField(IQueryableEntity obj, Query<?> query, boolean removePkUpdate, boolean force) {
		Assert.isTrue(query.getConditions().isEmpty());
		ITableMetadata meta = query.getMeta();
		if (fillPKConditions(obj, meta, query, removePkUpdate, force)) {
			return;
		}
		populateExampleConditions(obj);
	}

	/*
	 * (nojava doc)
	 */
	private static boolean isValidPKValue(IQueryableEntity obj, ITableMetadata meta, ColumnMapping field) {
		Class<?> type = field.getFieldAccessor().getType();
		Object value = field.getFieldAccessor().get(obj);
		if (type.isPrimitive()) {
			if (field.getUnsavedValue().equals(value)) {
				if (meta.getPKFields().size() == 1 && !obj.isUsed(field.field()))
					return false;
			}
		} else {
			if (value == null) {
				return false;
			}
		}
		return true;
	}

	/*
	 * @param obj 对象
	 * 
	 * @param meta 元数据
	 * 
	 * @param wrapper 实例包装
	 * 
	 * @param query 请求
	 * 
	 * @param removePkUpdate
	 * 
	 * @param force
	 * 
	 * @return
	 */
	protected static boolean fillPKConditions(IQueryableEntity obj, ITableMetadata meta, Query<?> query, boolean removePkUpdate, boolean force) {
		if (meta.getPKFields().isEmpty())
			return false;
		if (!force) {
			for (ColumnMapping field : meta.getPKFields()) {
				if (!isValidPKValue(obj, meta, field))
					return false;
			}
		}
		for (ColumnMapping mapping : meta.getPKFields()) {
			Object value = mapping.getFieldAccessor().get(obj);
			Field field = mapping.field();
			query.addCondition(field, value);
			if (removePkUpdate && obj.getUpdateValueMap().containsKey(field)) {//
				Map<Field, Object> map = obj.getUpdateValueMap();
				Object v = map.get(field);
				if (Objects.equal(value, v)) {
					map.remove(field);
				}
			}
		}
		return true;
	}

	/**
	 * 通过比较两个对象，在旧对象中准备更新Map
	 * 
	 * @param <T>
	 * @param changedObj
	 * @param oldObj
	 * @throws SQLException
	 * @return the object who is able to update.
	 */
	public static <T extends IQueryableEntity> T compareToUpdateMap(T changedObj, T oldObj) {
		Assert.isTrue(Objects.equal(getPrimaryKeyValue(changedObj), getPKValueSafe(oldObj)), "For consistence, the two parameter must hava equally primary keys.");
		BeanWrapper bean1 = BeanWrapper.wrap(changedObj);
		ITableMetadata m = MetaHolder.getMeta(oldObj);
		boolean dynamic = ORMConfig.getInstance().isDynamicUpdate();
		for (ColumnMapping mType : m.getColumns()) {
			if (mType.isPk())
				continue;
			Field field = mType.field();
			if (dynamic && !changedObj.isUsed(field)) {// 智能更新下，发现字段未被设过值，就不予更新
				continue;
			}
			Object value1 = bean1.getPropertyValue(field.name());
			oldObj.prepareUpdate(field, value1);// 本身就会通过比较，不更新没变的字段
		}
		return oldObj;
	}

	/**
	 * 通过比较两个对象，在新对象中准备更新Map
	 * 
	 * @param <T>
	 * @param changedObj
	 * @param oldObj
	 * @throws SQLException
	 * @return the object who is able to update.
	 */
	public static <T extends IQueryableEntity> T compareToNewUpdateMap(T changedObj, T oldObj) {
		Assert.isTrue(Objects.equal(getPrimaryKeyValue(changedObj), getPKValueSafe(oldObj)), "For consistence, the two parameter must hava equally primary keys.");
		BeanWrapper beanNew = BeanWrapper.wrap(changedObj);
		BeanWrapper beanOld = BeanWrapper.wrap(oldObj);
		ITableMetadata m = MetaHolder.getMeta(oldObj);
		
		Map<Field,Object> used=null;
		boolean dynamic = ORMConfig.getInstance().isDynamicUpdate();
		if(dynamic){
			used=new HashMap<Field,Object>(changedObj.getUpdateValueMap());
		}
		changedObj.getUpdateValueMap().clear();
		for (ColumnMapping mType : m.getColumns()) {
			Field field = mType.field();
			if (mType.isPk()){
				continue;
			}
			if (dynamic && !used.containsKey(field)) {// 智能更新下，发现字段未被设过值，就不予更新
				continue;
			}
			Object valueNew = beanNew.getPropertyValue(field.name());
			Object valueOld = beanOld.getPropertyValue(field.name());
			if (!Objects.equal(valueNew, valueOld)) {
				changedObj.prepareUpdate(field, valueNew, true);
			}
		}
		return changedObj;
	}

	/**
	 * 将指定对象中除了主键以外的所有字段都作为需要update的字段。（标记为'已修改的'）
	 * <br>
	 * 这个方法实际操作时：即除了主键以外的所有字段都放置到updateMap中去
	 * 
	 * @param <T>
	 * @param prepareObj
	 */
	public static <T extends IQueryableEntity> void fillUpdateMap(T... obj) {
		if (obj == null || obj.length == 0)
			return;
		ITableMetadata m = MetaHolder.getMeta(obj[0]);
		for (T o : obj) {
			BeanWrapper bean = BeanWrapper.wrap(o);
			for (ColumnMapping mType : m.getColumns()) {
				if (mType.isPk()) {
					continue;
				}
				Field field = mType.field();
				o.prepareUpdate(field, bean.getPropertyValue(field.name()), true);
			}
		}
	}

	/**
	 * 数值处理，拼装条件Example条件
	 * 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IQueryableEntity> Query<T> populateExampleConditions(T obj, String... properties) {
		Query<T> query = (Query<T>) obj.getQuery();
		ITableMetadata meta = query.getMeta();
		BeanWrapper bw = BeanWrapper.wrap(obj, BeanWrapper.FAST);
		if (properties.length == 0) {
			for (ColumnMapping mType : meta.getColumns()) {
				Field field = mType.field();
				if (obj.isUsed(field)) {
					Object value = bw.getPropertyValue(field.name());
					query.addCondition(field, value);
				}
			}
		} else {
			for (String s : properties) {
				Field field = meta.getField(s);
				if (field == null) {
					throw new IllegalArgumentException("field [" + s + "] not found in object " + meta.getName());
				}
				Object value = bw.getPropertyValue(field.name());
				query.addCondition(field, value);
			}
		}
		return query;
	}

	/**
	 * 从查询中得到关于对象拼装的映射提示
	 * 
	 * @param queryObj
	 * @return
	 */
	protected static EntityMappingProvider getMappingProvider(ConditionQuery queryObj) {
		if (queryObj instanceof JoinElement) {
			return ((JoinElement) queryObj).getSelectItems();
		}
		return null;
	}

	/**
	 * 根据对象获得表名，支持分表，允许返回多表，主要用于查询中
	 * 
	 * @param name
	 * @param needTranslate
	 * @return
	 */
	public static PartitionResult[] toTableNames(IQueryableEntity obj, String customName, Query<?> q, PartitionSupport processor) {
		AbstractMetadata meta = q == null ? MetaHolder.getMeta(obj) : (AbstractMetadata) q.getMeta();
		if (StringUtils.isNotEmpty(customName))
			return new PartitionResult[] { new PartitionResult(customName).setDatabase(meta.getBindDsName()) };
		PartitionResult[] result = partitionUtil.toTableNames(meta, obj, q, processor, ORMConfig.getInstance().isFilterAbsentTables());
		// if(ORMConfig.getInstance().isDebugMode()){
		// LogUtil.show("Partitions:"+Arrays.toString(result));
		// }
		return result;
	}

	/**
	 * 分表和路由计算，在没有对象实例的情况下计算路由，这个计算将会返回所有可能的表名组合
	 * 
	 * 
	 * @param meta
	 *            元数据描述
	 * @param processor
	 * @param operateType
	 *            计算表名所用的操作。0基表 1 不含基表 2 分表+基表 3 数据库中的存在表（不含基表） 4所有存在的表
	 *            影响效果——建表的多寡。
	 * 
	 * 
	 * @return
	 */
	public static PartitionResult[] toTableNames(ITableMetadata meta, PartitionSupport processor, int operateType) {
		Assert.notNull(meta);
		// long start=System.nanoTime();
		// try{
		return partitionUtil.toTableNames((AbstractMetadata) meta, processor, operateType);
		// }finally{
		// System.out.println((System.nanoTime()-start)/1000+"us");
		// }
	}

	/**
	 * 根据对象获得表名，支持分表，返回单表，主要用与插入更新中
	 * 
	 * @param obj
	 * @param customName
	 * @param q
	 * @param profile
	 * @return
	 */
	public static PartitionResult toTableName(IQueryableEntity obj, String customName, Query<?> q, PartitionSupport profile) {
		AbstractMetadata meta = obj == null ? (AbstractMetadata) q.getMeta() : MetaHolder.getMeta(obj);
		if (StringUtils.isNotEmpty(customName))
			return new PartitionResult(customName).setDatabase(meta.getBindDsName());
		PartitionResult result = partitionUtil.toTableName(meta, obj, q, profile);
		Assert.notNull(result);
		return result;
	}

	// /**
	// * 判断两个dbkey指向的是否为相同的物理数据库
	// * 对于相同rac组的认为是同一物理库
	// * @param dbkey,anotherDbKey都为空时，返回true
	// * @return
	// */
	// public static boolean isSameDb(String dbkey,String anotherDbKey){
	// if(StringUtils.isEmpty(dbkey)&&StringUtils.isEmpty(anotherDbKey)){
	// return true;
	// }
	// if(StringUtils.isEmpty(dbkey))
	// return false;
	// if(dbkey.equalsIgnoreCase(anotherDbKey))
	// return true;
	// String racId = getRacId(dbkey);
	// String anotherRacId = getRacId(anotherDbKey);
	//
	// return
	// racId!=null&&!String.valueOf(NO_RAC_ID).equals(racId)&&racId.equalsIgnoreCase(anotherRacId);
	// }

	/**
	 * 安静的关闭结果集
	 * 
	 * @param rs
	 */
	public static void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
			}
		}
	}

	/**
	 * 关闭指定的Statement
	 * 
	 * @param st
	 */
	public static void close(Statement st) {
		try {
			if (st != null)
				st.close();
		} catch (SQLException e) {
		}
	}

	/**
	 * 将异常包装为RuntimeException
	 * @param e
	 * @return
	 */
	public static PersistenceException toRuntimeException(SQLException e) {
		String s = e.getSQLState();
		if (e instanceof SQLIntegrityConstraintViolationException) {
			return new EntityExistsException(e);
		}else if (e instanceof SQLTimeoutException) {
			return new QueryTimeoutException(s,e);
		}
		return new PersistenceException(s, e);
	}

	/**
	 * 将异常包装为Runtime异常
	 * 
	 * @param e
	 * @return
	 */
	public static RuntimeException toRuntimeException(Throwable e) {
		while (true) {
			if (e instanceof RuntimeException) {
				return (RuntimeException) e;
			}
			if (e instanceof InvocationTargetException) {
				e = e.getCause();
				continue;
			}
			if (e instanceof Error) {
				throw (Error) e;
			}
			if (e instanceof SQLException) {
				return toRuntimeException((SQLException)e);
			}
			return new IllegalStateException(e);
		}
	}

	private static final String DEFAULT_SEQUENCE_PATTERN = "S_%s";
	private static final int TABLE_NAME_MAX_LENGTH = 26;

	public static String calcSeqNameByTable(String schema, String tableName, String columnName) {
		String pattern = JefConfiguration.get(DbCfg.SEQUENCE_NAME_PATTERN);
		if (StringUtils.isBlank(pattern))
			pattern = DEFAULT_SEQUENCE_PATTERN;
		String tblName = tableName;
		if (tblName.length() > TABLE_NAME_MAX_LENGTH) {
			tblName = tblName.substring(0, TABLE_NAME_MAX_LENGTH);
		}
		if (schema == null) {
			return StringUtils.upperCase(String.format(pattern, tblName));
		} else {
			String name = String.format(pattern, tblName);
			return new StringBuilder(schema.length() + name.length() + 1).append(schema).append('.').append(name).toString().toUpperCase();
		}
	}

	// TODO 关于Oracle RAC模式下的URL简化问题
	// public static String getSimpleUrl(String url) {
	// if (url.toLowerCase().indexOf("service_name") > -1) {
	// StringBuilder sb=new StringBuilder();
	// StringTokenizer st=new StringTokenizer(url,"()");
	// while(st.hasMoreTokens()){
	// String str=st.nextToken();
	// String x=str.toUpperCase();
	// if(x.startsWith("SERVICE_NAME") || x.startsWith("HOST")){
	// sb.append('(').append(str).append(')');
	// }
	// }
	// url = sb.toString();
	// }
	// return url;
	// }

	/**
	 * 使用JDBC URL等构造出一个datasource对象，构造前能自动查找驱动类名称并注册
	 * 
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 */
	public static SimpleDataSource createSimpleDataSource(String url, String user, String password) {
		SimpleDataSource s = new SimpleDataSource();
		s.setUsername(user);
		s.setUrl(url);
		s.setPassword(password);
		return s;
	}
	
	/**
	 * 得到继承上级所指定的泛型类型
	 * @param subclass
	 * @param superclass
	 * @return
	 */
	public static Type[] getTypeParameters(Class<?> subclass,Class<?> superclass) {
		if(superclass==null){//在没有指定父类的情况下，默认选择第一个接口
			if(subclass.getSuperclass()==Object.class && subclass.getInterfaces().length>0){
				superclass=subclass.getInterfaces()[0];	
			}else{
				superclass=subclass.getSuperclass();	
			}
		}
		Type type= GenericUtils.getSuperType(null, subclass,superclass);
		if(type instanceof ParameterizedType){
			return ((ParameterizedType) type).getActualTypeArguments();
		}
		throw new RuntimeException("Can not get the generic param type for class:" + subclass.getName());
	}
}
