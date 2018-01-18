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
package jef.database.dialect;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.ORMConfig;
import jef.database.annotation.DateGenerateType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.handler.LimitHandler;
import jef.database.dialect.handler.MySqlLimitHandler;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.jdbc.result.IResultSet;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.object.Column;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.ConstraintType;
import jef.database.meta.object.ForeignKeyAction;
import jef.database.meta.object.ForeignKeyMatchType;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.CastFunction;
import jef.database.query.function.EmuDecodeWithIf;
import jef.database.query.function.EmuTranslateByReplace;
import jef.database.query.function.MySQLTruncate;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.support.RDBMS;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtils;
import jef.tools.string.JefStringReader;

import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLTemplates;

/**
 * MySQL 特性
 * <p>
 * <ol>
 * <li>自增特性 AUTO_INCREMENT 表示自增 SELECT LAST_INSERT_ID();获取自增量值 SELECT @@IDENTITY
 * 获取新分配的自增量值</li>
 * 
 * <li>支持无符号数、支持BLOB，支持TEXT。数据类型较多。但是考虑兼容性，很多都不在本框架使用</li>
 * 
 * <li>MYSQL VARCHAR长度 最大长度65535，在utf8编码时最大65535/3=21785，GBK则是65535/2.
 * 但是用varchar做主键的长度是受MYSQL索引限制的。 MYSQL索引只能索引768个字节。（过去某个版本好像是1000字节）
 * 当数据库默认使用GBK编码时。这个长度是383. UTF8这个是255，latin5是767. （建表时可以在尾部加上charset=latin5;
 * 来指定表的语言。） <br>
 * 因此在MYSQL中不能将长度超过这个限制的字段设置为主键。一般来说设置varchar也就到255，这是比较安全的。</li>
 * 
 * <li>MY SQL中对不同大小的文有多种类型
 * <ul>
 * <li>TINYTEXT，最大长度为255，占用空间也是(实际长度+1)；</li>
 * <li>TEXT，最大长度65535，占用空间是(实际长度+2)；</li>
 * <li>MEDIUMTEXT，最大长度16777215，占用空间是(实际长度+3)；</li>
 * <li>LONGTEXT，最大长度4294967295，占用空间是(实际长度+4)。</li>
 * </ul>
 * 
 * <li>BLOB和TEXT 作为主键、或者建立索引时，需要指定索引长度，限制见上。</li>
 * 
 * <li>日期DATE、TIME、DATETIME、TIMESTAMP和YEAR等。</li>
 * </ol>
 * 
 * <p>
 * 常用命令
 * <ol>
 * <li>查看表的所有信息：show create table 表名;</li>
 * <li>添加主键约束：alter table 表名 add constraint 主键 （形如：PK_表名） primary key 表名(主键字段);
 * <li>添加外键约束：alter table 从表 add constraint 外键（形如：FK_从表_主表） foreign key 从表(外键字段)
 * references 主表(主键字段);
 * <li>删除主键约束：alter table 表名 drop primary key;</li>
 * <li>删除外键约束：alter table 表名 drop foreign key 外键（区分大小写）;</li>
 * <li>修改表名： alter table t_book rename to bbb;</li>
 * <li>添加列： alter table 表名 add column 列名 varchar(30);</li>
 * <li>删除列： alter table 表名 drop column 列名;</li>
 * <li>修改列名： alter table bbb change nnnnn hh int;</li>
 * <li>修改列属性：alter table t_book modify name varchar(22);</li>
 * </ol>
 * 
 * MySQL的四种BLOB类型 类型 大小(单位：字节) TinyBlob 最大 255 Blob 最大 65K MediumBlob 最大 16M
 * LongBlob 最大 4G
 * 
 * 
 * 遗留问题：MySQL能不能做成表名大小写不敏感的。目前是敏感的。这造成大小写不一致会认为是两张表。
 */
public class MySqlDialect extends AbstractDialect {
	public MySqlDialect() {
		// 在MYSQL中 ||是逻辑运算符
		features = CollectionUtils.identityHashSet();
		features.addAll(Arrays.asList(Feature.DBNAME_AS_SCHEMA, Feature.SUPPORT_INLINE_COMMENT,Feature.ALTER_FOR_EACH_COLUMN, Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD, Feature.SUPPORT_LIMIT, Feature.COLUMN_DEF_ALLOW_NULL));
		setProperty(DbProperty.ADD_COLUMN, "ADD");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");
		setProperty(DbProperty.SELECT_EXPRESSION, "select %s");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "``");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "SELECT LAST_INSERT_ID()");
		setProperty(DbProperty.INDEX_LENGTH_LIMIT, "767");
		setProperty(DbProperty.INDEX_LENGTH_LIMIT_FIX, "255");
		setProperty(DbProperty.INDEX_LENGTH_CHARESET_FIX, "charset=latin5");
		setProperty(DbProperty.DROP_INDEX_TABLE_PATTERN, "%1$s ON %2$s");
		setProperty(DbProperty.DROP_FK_PATTERN, "alter table %1$s drop foreign key %2$s");
		
		loadKeywords("mysql_keywords.properties");
		registerNative(new StandardSQLFunction("ascii"));
		registerNative(new StandardSQLFunction("bin"));
		registerNative(new StandardSQLFunction("char_length"), "character_length");
		registerNative(new StandardSQLFunction("length"));
		registerAlias(Func.lengthb, "length");
		registerAlias(Func.length, "char_length");
		registerNative(Func.lower, "lcase");
		registerNative(Func.upper, "ucase");
		registerNative(Func.locate);
		registerNative(new StandardSQLFunction("uuid"));
		registerNative(new StandardSQLFunction("ord"));
		registerNative(new StandardSQLFunction("quote"));
		registerNative(new StandardSQLFunction("reverse"));
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Func.mod);
		registerNative(Func.coalesce);
		registerNative(Func.nullif);
		registerNative(Func.cast, new CastFunction());
		registerNative(Scientific.soundex);
		registerNative(new StandardSQLFunction("space"));
		registerNative(new StandardSQLFunction("unhex"));
		registerNative(new StandardSQLFunction("truncate"));
		registerCompatible(Func.trunc, new MySQLTruncate());// MYSQL的truncate函数因为是必须双参数的，其他数据库的允许单参数

		registerNative(Scientific.cot);
		registerNative(new StandardSQLFunction("crc32"));
		registerNative(Scientific.exp);
		registerNative(Scientific.ln, "log");

		registerNative(new StandardSQLFunction("log2"));
		registerNative(Scientific.log10);
		registerNative(new NoArgSQLFunction("pi"));
		registerNative(new NoArgSQLFunction("rand"));
		registerAlias(Scientific.rand, "rand");
		registerNative(Func.substring, "substr");

		registerNative(Scientific.radians);
		registerNative(Scientific.degrees);

		registerNative(Func.ceil, "ceiling");
		registerNative(Func.floor);
		registerNative(Func.round);

		registerNative(Func.datediff);
		registerNative(new StandardSQLFunction("timediff"));
		registerNative(new StandardSQLFunction("date_format"));
		registerNative(new StandardSQLFunction("ifnull"));
		registerAlias(Func.nvl, "ifnull");

		registerNative(Func.adddate);
		registerNative(Func.subdate);
		registerNative(new StandardSQLFunction("date_add"));
		registerNative(new StandardSQLFunction("date_sub"));

		
		registerNative(Func.current_date, new NoArgSQLFunction("current_date", false), "curdate");
		registerNative(Func.current_time, new NoArgSQLFunction("current_time", false), "curtime");

		registerNative(Func.current_timestamp, new NoArgSQLFunction("current_timestamp", false));
		registerAlias(Func.now, "current_timestamp");
		registerAlias("sysdate", "current_timestamp");

		registerNative(Func.date);

		registerNative(new StandardSQLFunction("timestampdiff"));
		registerNative(new StandardSQLFunction("timestampadd"));

		registerNative(Func.day, "dayofmonth");
		registerNative(new StandardSQLFunction("dayname"));
		registerNative(new StandardSQLFunction("dayofweek"));
		registerNative(new StandardSQLFunction("dayofyear"));
		registerNative(new StandardSQLFunction("from_days"));
		registerNative(new StandardSQLFunction("from_unixtime"));
		registerNative(Func.hour);
		registerNative(new NoArgSQLFunction("localtime"));
		registerNative(new NoArgSQLFunction("localtimestamp"));
		registerNative(new StandardSQLFunction("microseconds"));
		registerNative(Func.minute);
		registerNative(Func.month);
		registerNative(new StandardSQLFunction("monthname"));
		registerNative(new StandardSQLFunction("quarter"));
		registerNative(Func.second);
		registerNative(new StandardSQLFunction("sec_to_time"));// 秒数转为time对象
		registerNative(Func.time);
		registerNative(new StandardSQLFunction("timestamp"));
		registerNative(new StandardSQLFunction("time_to_sec"));
		registerNative(new StandardSQLFunction("to_days"));
		registerNative(new StandardSQLFunction("unix_timestamp"));
		registerNative(new NoArgSQLFunction("utc_date"));
		registerNative(new NoArgSQLFunction("utc_time"));
		registerNative(new NoArgSQLFunction("utc_timestamp"));
		registerNative(new StandardSQLFunction("week"), "weekofyear"); // 返回日期属于当年的第几周
		registerNative(new StandardSQLFunction("weekday"));
		registerNative(Func.year);
		registerNative(new StandardSQLFunction("yearweek"));
		registerNative(new StandardSQLFunction("hex"));
		registerNative(new StandardSQLFunction("oct"));

		registerNative(new StandardSQLFunction("octet_length"));
		registerNative(new StandardSQLFunction("bit_length"));

		registerNative(new StandardSQLFunction("bit_count"));
		registerNative(new StandardSQLFunction("encrypt"));
		registerNative(new StandardSQLFunction("md5"));
		registerNative(new StandardSQLFunction("sha1"));
		registerNative(new StandardSQLFunction("sha"));
		registerNative(Func.trim);
		registerNative(Func.concat);
		registerNative(Func.replace);
		registerNative(Func.lpad);
		registerNative(Func.rpad);
		registerNative(Func.timestampdiff);
		registerNative(Func.timestampadd);
		registerCompatible(Func.add_months, new TemplateFunction("add_months", "timestampadd(MONTH,%2$s,%1$s)"));
		registerCompatible(Func.decode, new EmuDecodeWithIf());
		registerCompatible(Func.translate, new EmuTranslateByReplace());
		registerCompatible(Func.str, new TemplateFunction("str", "cast(%s as char)"));
		typeNames.put(Types.BOOLEAN,"BIT(1)", 0);
		typeNames.put(Types.BLOB,"mediumblob", 0);
		typeNames.put(Types.BLOB, 255, "tinyblob", 0);
		typeNames.put(Types.BLOB, 65535, "blob", 0);
		typeNames.put(Types.BLOB, 1024 * 1024 * 16, "mediumblob", 0);
		typeNames.put(Types.BLOB, 1024 * 1024 * 1024 * 4, "longblob", 0);
		typeNames.put(Types.CLOB, "text", 0);

		typeNames.put(Types.VARCHAR, 21785, "varchar($l)", 0);
		typeNames.put(Types.VARCHAR, 65535, "text", Types.CLOB);
		typeNames.put(Types.VARCHAR, 1024 * 1024 * 16, "mediumtext", Types.CLOB);
		// MYSQL中的Timestamp含义有些特殊，默认还是用datetime记录
		typeNames.put(Types.TIMESTAMP, 1024 * 1024 * 16, "datetime", 0);
		typeNames.put(Types.TIMESTAMP, "datetime", 0);
	}

	@Override
	public String getCreationComment(ColumnType column, boolean flag) {
		DateGenerateType generateType = null;
		if (column instanceof SqlTypeDateTimeGenerated) {
			generateType = ((SqlTypeDateTimeGenerated) column).getGenerateType();
			Object defaultValue = column.defaultValue;
			/*
			 * 根据用户设置的defaultValue进行修正
			 */
			if (generateType == null && (defaultValue == Func.current_date || defaultValue == Func.current_time || defaultValue == Func.now)) {
				generateType = DateGenerateType.created;
			}
			if(generateType== null && defaultValue!=null){
				String dStr = defaultValue.toString().toLowerCase();
				if (dStr.startsWith("current") || dStr.startsWith("sys")) {
					generateType = DateGenerateType.created;	
				}
			}
		}
		if(generateType==DateGenerateType.created){
			return "datetime NOT NULL";
		}else if(generateType==DateGenerateType.modified){
			return "timestamp NOT NULL DEFAULT current_timestamp ON UPDATE current_timestamp";
		}
		return super.getCreationComment(column, flag);
	}
	
	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		//sb.append("INT UNSIGNED");
		//2016-4-19日从 int unsigned改为int，因为当一个表主键被另一个表作为外键引用时，双方类型必须完全一样。
		//实际测试发现，由于一般建表时普通int字段不会处理为 int unsigned，造成外键创建失败。所以此处暂时为int
		sb.append("INT ");
		
		
		if (flag) {
			if (!column.nullable)
				sb.append(" NOT NULL");
		}
		sb.append(" AUTO_INCREMENT");
		return sb.toString();
	}

	@Override
	public boolean containKeyword(String name) {
		return keywords.contains(name.toLowerCase());
	}

	@Override
    public String toDefaultString(Object defaultValue, int sqlType, int changeTo) {
	    String def=String.valueOf(defaultValue);
	    if(sqlType==Types.BIT && def.startsWith("b'") && def.length()>2){
	        char c=def.charAt(2);
	        return String.valueOf(c);
	    }
        return super.toDefaultString(defaultValue, sqlType, changeTo);
    }

    /**
	 * MYSQL的时间日期类型有三种，date datetime，timestamp
	 * 
	 * 其中 date time都只能设置默认值为常量，不能使用函数。 第一个timestamp则默认会变为not null default
	 * current_timestamp on update current_timestamp
	 */
	

	@Override
	public String getCatlog(String schema) {
		return schema;
	}

	@Override
	public String getSchema(String schema) {
		return null;
	}

	/*
	 * 
	 * 关于MySQL的Auto_increament访问是比较复杂的 SELECT `AUTO_INCREMENT` FROM
	 * INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'DatabaseName' AND
	 * TABLE_NAME = 'TableName';
	 * 
	 * 
	 * $result = mysql_query("SHOW TABLE STATUS LIKE 'table_name'"); $row =
	 * mysql_fetch_array($result); $nextId = $row['Auto_increment'];
	 * mysql_free_result($result);
	 * 
	 * 
	 * 
	 * down vote accepted Use this:
	 * 
	 * ALTER TABLE users AUTO_INCREMENT = 1001;or if you haven't already id
	 * column, also add it
	 * 
	 * ALTER TABLE users ADD id INT UNSIGNED NOT NULL AUTO_INCREMENT, ADD INDEX
	 * (id);
	 */

	public String getDriverClass(String url) {
		return "com.mysql.jdbc.Driver";
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		// jdbc:mysql://localhost:3306/allandb
		// ??useUnicode=true&characterEncoding=UTF-8
		  //新版本MySQL对0000-00-00加强了管理，要求增加参数
	    //zeroDateTimeBehavior=convertToNull
	    //zeroDateTimeBehavior=noDatetimeStringSync
	    //zeroDateTimeBehavior=exception
	    //zeroDateTimeBehavior=round
		sb.append("mysql:");
		sb.append("//").append(host).append(":").append(port <= 0 ? 3306 : port);
		sb.append("/").append(pathOrName).append("?useUnicode=true&characterEncoding=UTF-8");//
		String url = sb.toString();
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(url);
		}
		return url;
	}

	@Override
	public ColumnType getProprtMetaFromDbType(Column column) {
		if ("DECIMAL".equals(column.getDataType())) {
			if (column.getDecimalDigit() > 0) {// 小数
				return new ColumnType.Double(column.getColumnSize(), column.getDecimalDigit());
			} else {// 整数
				if (column.getColumnDef() != null && column.getColumnDef().startsWith("GENERATED")) {
					return new ColumnType.AutoIncrement(column.getColumnSize());
				} else {
					return new ColumnType.Int(column.getColumnSize());
				}
			}
		} else {
			return super.getProprtMetaFromDbType(column);
		}
	}

	public RDBMS getName() {
		return RDBMS.mysql;
	}


	// " jdbc:mysql://localhost:3306/allandb?useUnicode=true&characterEncoding=UTF-8"
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consume("jdbc:mysql:");
		reader.omitChars('/');
		String host = reader.readToken(':', '/');
		reader.omitUntillChar('/');// 忽略端口，直到db开始
		reader.omit(1);
		String dbname = reader.readToken(new char[] { '?', ' ', ';' });
		connectInfo.setHost(host);
		connectInfo.setDbname(dbname);
		reader.close();
	}

	private final static int[] IO_ERROR_CODE = { 1158, 1159, 1160, 1161, 2001, 2002, 2003, 2004, 2006, 2013, 2024, 2025, 2026 };

	@Override
	public boolean isIOError(SQLException se) {
		if (se.getSQLState() != null) { // per Mark Matthews at MySQL
			if (se.getSQLState().startsWith("08")) {// 08s01 网络错误
				return true;
			}
		}
		int code = se.getErrorCode();
		if (ArrayUtils.contains(IO_ERROR_CODE, code)) {
			return true;
		} else if (se.getCause() != null && "NetException".equals(se.getCause().getClass().getSimpleName())) {
			return true;
		} else {
			LogUtil.info("MySQL non-io Err:{}: {}", se.getErrorCode(), se.getMessage());
			return false;
		}
	}

	@Override
	public void processIntervalExpression(BinaryExpression parent, Interval interval) {
		interval.toMySqlMode();
	}

	@Override
	public void processIntervalExpression(Function func, Interval interval) {
		interval.toMySqlMode();
	}
	
	private final LimitHandler limit=new MySqlLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}
	
	private static ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter(){
		@Override
		public String extractConstraintName(SQLException sqle) {
			if("MySQLIntegrityConstraintViolationException".equals(sqle.getClass().getSimpleName())){
				return sqle.getMessage();
			}
			return null;
		}
		
	};
	
	private final SQLTemplates queryDslDialect = new MySQLTemplates();

    @Override
    public SQLTemplates getQueryDslDialect() {
        return queryDslDialect;
    }

	@Override
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String constraintName)
			throws SQLException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("    SELECT kcu.*, tc.constraint_type, rc.update_rule, rc.delete_rule, rc.match_option");
		sb.append("      FROM information_schema.key_column_usage kcu ");
		sb.append("INNER JOIN information_schema.table_constraints tc");
		sb.append("        ON tc.constraint_schema = kcu.constraint_schema");
		sb.append("       AND tc.constraint_name = kcu.constraint_name");
		sb.append("       AND tc.table_schema = kcu.constraint_schema");
		sb.append("       AND tc.table_name = kcu.table_name");
		sb.append(" LEFT JOIN information_schema.referential_constraints rc");
		sb.append("        ON kcu.constraint_schema = rc.constraint_schema");
		sb.append("       AND kcu.constraint_name = rc.constraint_name");
		sb.append("       AND kcu.table_schema = rc.constraint_schema");
		sb.append("       AND kcu.table_name = rc.table_name");
		sb.append("     WHERE kcu.constraint_schema like ? and kcu.constraint_name like ?");
		sb.append("  ORDER BY kcu.constraint_schema, kcu.table_name, kcu.constraint_name, kcu.ordinal_position");
		schema = StringUtils.isBlank(schema) ? "%" : schema.toLowerCase();
		constraintName = StringUtils.isBlank(constraintName) ? "%" : constraintName.toLowerCase();
		
		List<Constraint> constraints = conn.selectBySql(sb.toString(), new AbstractResultSetTransformer<List<Constraint>>(){
			
			@Override
			public List<Constraint> transformer(IResultSet rs) throws SQLException {
				
				List<Constraint> constraints = new ArrayList<Constraint>();
				List<Column> columns = new ArrayList<Column>();
				List<Column> refColumns = new ArrayList<Column>();
				Constraint preCon = new Constraint(); // 上一条记录
				
				while(rs.next()){
					
					if(constraints.size() > 0){
						preCon = constraints.get(constraints.size() - 1);
					}
					
					boolean isSameConstraint = rs.getString("constraint_schema").equals(preCon.getSchema())
							&& rs.getString("constraint_name").equals(preCon.getName())
							&& rs.getString("table_schema").equals(preCon.getTableSchema())
							&& rs.getString("table_name").equals(preCon.getTableName());

					if(!isSameConstraint){

						columns = new ArrayList<Column>();
						refColumns = new ArrayList<Column>();

						Constraint c = new Constraint();
						c.setCatalog(rs.getString("constraint_catalog"));
						c.setSchema(rs.getString("constraint_schema"));
						c.setName(rs.getString("constraint_name"));
						c.setType(ConstraintType.parseFullName(rs.getString("constraint_type")));
						c.setDeferrable(false);
						c.setInitiallyDeferrable(false);
						c.setTableCatalog(rs.getString("table_catalog"));
						c.setTableSchema(rs.getString("table_schema"));
						c.setTableName(rs.getString("table_name"));
						c.setMatchType(ForeignKeyMatchType.parseName(rs.getString("match_option")));
						c.setRefTableName(rs.getString("referenced_table_name"));
						c.setUpdateRule(ForeignKeyAction.parseName(rs.getString("update_rule")));
						c.setDeleteRule(ForeignKeyAction.parseName(rs.getString("delete_rule")));
						c.setEnabled(true);
						c.setColumns(columns);
						c.setRefColumns(refColumns);
						constraints.add(c);
					}
					
					// 有指定列的约束则添加到列表
					if(StringUtils.isNotBlank(rs.getString("column_name"))){
						Column column = new Column();
						column.setColumnName(rs.getString("column_name"));
						columns.add(column);
					}
					
					// 是外键约束则添加到参照列表
					if(StringUtils.isNotBlank(rs.getString("referenced_column_name"))){
						Column column = new Column();
						column.setColumnName(rs.getString("referenced_column_name"));
						refColumns.add(column);
					}
				}
				
				return constraints;
			}
			
		}, Arrays.asList(schema, constraintName));
		
    	return constraints;
	}
}
