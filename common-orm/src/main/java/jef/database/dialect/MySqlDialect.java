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
import java.util.Arrays;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Blob;
import jef.database.dialect.ColumnType.Clob;
import jef.database.dialect.ColumnType.Date;
import jef.database.dialect.ColumnType.TimeStamp;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;
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
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
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
 * <li>修改列属性：alter table t_book modify name varchar(22);</li></ol>
 * 
 * MySQL的四种BLOB类型
类型 大小(单位：字节)
TinyBlob 最大 255
Blob 最大 65K
MediumBlob 最大 16M
LongBlob 最大 4G

 */
public class MySqlDialect extends DbmsProfile {
	public MySqlDialect() {
		// 在MYSQL中 ||是逻辑运算符
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(Feature.DBNAME_AS_SCHEMA, Feature.INDEX_LENGTH_LIMIT, Feature.ALTER_FOR_EACH_COLUMN,Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD));
		setProperty(DbProperty.ADD_COLUMN, "ADD");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");
		setProperty(DbProperty.SELECT_EXPRESSION, "select %s");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "`");
		
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
		registerCompatible(Func.trunc, new MySQLTruncate());//MYSQL的truncate函数因为是必须双参数的，其他数据库的允许单参数

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

		registerNative(Func.adddate, "date_add");
		registerNative(Func.subdate, "date_sub");

		registerNative(Func.current_date, new NoArgSQLFunction("current_date", false), "curdate");
		registerNative(Func.current_time,new NoArgSQLFunction("current_time", false), "curtime");

		registerNative(Func.current_timestamp,new NoArgSQLFunction("current_timestamp", false), "now", "sysdate");
		registerAlias(Func.now, "current_timestamp");

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
		registerCompatible(Func.add_months, new TemplateFunction("add_months","timestampadd(MONTH,%2$s,%1$s)"));
		registerCompatible(Func.decode, new EmuDecodeWithIf());
		registerCompatible(Func.translate, new EmuTranslateByReplace());
		registerCompatible(Func.str, new TemplateFunction("str", "cast(%s as char)"));
		
	}

	
	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("INT UNSIGNED");
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
	protected String getComment(Blob column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		if (flag) {
			sb.append("mediumblob");
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	/**
	 * 对于CLOB, MYSQL 当作 TEXT处理。
	 */
	protected String getComment(Clob column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("TEXT");
		if (flag) {
			if (!column.nullable)
				sb.append(" NOT NULL");
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
		}
		return sb.toString();
	}

	/**
	 * 
	 */
	protected String getComment(Varchar column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		if (column.length > 21785) {// 超过UTF-8下 MYSQL VARCHAR支持的最大长度
			sb.append("MEDIUMTEXT");
		} else {
			sb.append("varchar(" + column.length + ")");
		}
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
			if (column.defaultValue != null)
				sb.append(" default ").append(super.toDefaultString(column.defaultValue));
		}
		return sb.toString();
	}

	/**
	 * MYSQL的时间日期类型有三种，date datetime，timestamp
	 * 
	 * 其中 date time都只能设置默认值为常量，不能使用函数。 第一个timestamp则默认会变为not null default
	 * current_timestamp on update current_timestamp
	 */
	@Override
	protected String getComment(Date column, boolean flag) {
		if (flag && (column.defaultValue instanceof Func)) {// 死局，MYSQL中DATE字段只能用常量作为默认值，这种情况下姑且强行将这个字段作为timestamp处理
			return getComment(column.toTimeStamp(), flag);
		}
		return super.getComment(column, flag);
	}

	@Override
	protected String getComment(TimeStamp column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		
		Object defaultValue = column.defaultValue;
		boolean isTimestamp =false;
		if(defaultValue!=null){
			isTimestamp =(column.defaultValue instanceof Func);
			if(!isTimestamp){
				String dStr=defaultValue.toString().toLowerCase();
				if(dStr.startsWith("current")||dStr.startsWith("sys")){
					isTimestamp =true;					
				}
			}
		}
		if (isTimestamp) {
			if (defaultValue == Func.current_date || defaultValue == Func.current_time || defaultValue == Func.now) {
				defaultValue = Func.current_timestamp;
			}
			sb.append("timestamp");
		} else {
			sb.append("datetime");
		}
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");

			if (defaultValue != null) {
				sb.append(" default ").append(toDefaultString(defaultValue));
			}
		}
		return sb.toString();
	}

	/**
	 * MYSQL中，表名是全转小写的，列名才是保持大小写的，先做小写处理，如果有处理列名的场合，改为调用
	 * {@link #getColumnNameIncase(String)}
	 */
	@Override
	public String getObjectNameIfUppercase(String name) {
		return StringUtils.lowerCase(name);
	}

	public boolean checkPKLength(ColumnType type) {
		if (type instanceof Varchar) {
			if (((Varchar) type).length > 767) {
				throw new IllegalArgumentException("The varchar column in MYSQL will not be indexed if length is >767.");
			} else if (((Varchar) type).length > 255) {
				return false;
			}
		}
		return true;
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
	 * 关于MySQL的Auto_increament访问是比较复杂的
	 * SELECT `AUTO_INCREMENT`
FROM  INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'DatabaseName'
AND   TABLE_NAME   = 'TableName';


$result = mysql_query("SHOW TABLE STATUS LIKE 'table_name'");
$row = mysql_fetch_array($result);
$nextId = $row['Auto_increment'];
mysql_free_result($result);


 
down vote 
accepted  Use this:

ALTER TABLE users AUTO_INCREMENT = 1001;or if you haven't already id column, also add it

ALTER TABLE users ADD id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    ADD INDEX (id); 
	 */
	public String getGeneratedFetchFunction() {
		return "SELECT LAST_INSERT_ID()";
	}

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
		if(ORMConfig.getInstance().isDebugMode()){
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

	private final static String MYSQL_PAGE = " limit %start%,%next%";

	public String toPageSQL(String sql, IntRange range) {
		boolean isUnion=false;
		try {
			Select select=DbUtils.parseNativeSelect(sql);
			if(select.getSelectBody() instanceof Union){
				isUnion=true;
			}
			select.getSelectBody();
		} catch (ParseException e) {
			LogUtil.exception("SqlParse Error:",e);
		}
		String limit = StringUtils.replaceEach(MYSQL_PAGE, new String[] { "%start%", "%next%" }, range.toStartLimit());
		return isUnion ?
				StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit);
	}

	public Select toPageSQL(Select select, IntRange range) {
		int[] span=range.toStartLimitSpan();
		Limit limit=new Limit();
		limit.setOffset(span[0]);
		limit.setRowCount(span[1]);
		select.getSelectBody().setLimit(limit);
		return select;
	}
	

	@Override
	public String toPageSQL(String sql, IntRange range,boolean isUnion) {
		String limit = StringUtils.replaceEach(MYSQL_PAGE, new String[] { "%start%", "%next%" }, range.toStartLimit());
		return isUnion ?
				StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit);
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
	
	
	private final static int[] IO_ERROR_CODE = { 1158, 1159, 1160, 1161,
			2001, 2002, 2003, 2004, 2006, 2013, 2024, 2025, 2026 };
	
	@Override
	public boolean isIOError(SQLException se) {
		if (se.getSQLState() != null){ // per Mark Matthews at MySQL
			if (se.getSQLState().startsWith("08")){//08s01 网络错误
				return true;
			}
		}
		int code = se.getErrorCode();
		if (ArrayUtils.contains(IO_ERROR_CODE, code)) {
			return true;
		} else if (se.getCause() != null
				&& "NetException".equals(se.getCause().getClass().getSimpleName())) {
			return true;
		} else {
			LogUtil.info("MySQL non-io Err:{}: {}", se.getErrorCode(),se.getMessage());
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
}
