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
import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.ORMConfig;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.meta.Column;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
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
import jef.tools.ArrayUtils;
import jef.tools.collection.CollectionUtils;
import jef.tools.string.JefStringReader;

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
 */
public class MySqlDialect extends AbstractDialect {
	public MySqlDialect() {
		// 在MYSQL中 ||是逻辑运算符
		features = CollectionUtils.identityHashSet();
		features.addAll(Arrays.asList(Feature.DBNAME_AS_SCHEMA, Feature.ALTER_FOR_EACH_COLUMN, Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD, Feature.SUPPORT_LIMIT, Feature.COLUMN_DEF_ALLOW_NULL));
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
		int generateType = 0;
		if (column instanceof SqlTypeDateTimeGenerated) {
			// 1 创建时生成为sysdate 2更新时生成为sysdate 3创建时设置为为java系统时间  4为更新时设置为java系统时间
			generateType = ((SqlTypeDateTimeGenerated) column).getGenerateType();
			Object defaultValue = column.defaultValue;
			if (generateType == 0 && (defaultValue == Func.current_date || defaultValue == Func.current_time || defaultValue == Func.now)) {
				generateType = 1;
			}
			if(generateType==0 && defaultValue!=null){
				String dStr = defaultValue.toString().toLowerCase();
				if (dStr.startsWith("current") || dStr.startsWith("sys")) {
					generateType = 1;	
				}
			}
		}
		if(generateType==1){
			return "datetime not null";
		}else if(generateType==2){
			return "timestamp not null default current_timestamp on update current_timestamp";
		}
		return super.getCreationComment(column, flag);
	}
	
	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("INT UNSIGNED");
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
		}
		sb.append(" AUTO_INCREMENT");
		return sb.toString();
	}

	@Override
	public boolean containKeyword(String name) {
		return keywords.contains(name.toLowerCase());
	}

	/**
	 * MYSQL的时间日期类型有三种，date datetime，timestamp
	 * 
	 * 其中 date time都只能设置默认值为常量，不能使用函数。 第一个timestamp则默认会变为not null default
	 * current_timestamp on update current_timestamp
	 */


	/**
	 * MYSQL中，表名是全转小写的，列名才是保持大小写的，先做小写处理，如果有处理列名的场合，改为调用
	 * {@link #getColumnNameToUse(String)}
	 */
	@Override
	public String getObjectNameToUse(String name) {
		if(name==null || name.length()==0)return null;
		if(name.charAt(0)=='`')return name;
		return name.toLowerCase();
	}
	
	

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
	
}
