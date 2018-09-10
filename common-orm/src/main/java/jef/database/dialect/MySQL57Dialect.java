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
import java.util.Set;

import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLTemplates;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.ORMConfig;
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

public class MySQL57Dialect extends AbstractDialect {

	public MySQL57Dialect() {
	}

	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		// sb.append("INT UNSIGNED");
		// 2016-4-19日从 int unsigned改为int，因为当一个表主键被另一个表作为外键引用时，双方类型必须完全一样。
		// 实际测试发现，由于一般建表时普通int字段不会处理为 int unsigned，造成外键创建失败。所以此处暂时为int
		sb.append("INT ");
		if (flag) {
			if (!column.nullable)
				sb.append(" NOT NULL");
		}
		sb.append(" AUTO_INCREMENT");
		return sb.toString();
	}

	protected void initFeatures() {
		Set<Feature> features = CollectionUtils.identityHashSet();
		// 在MYSQL中 ||是逻辑运算符
		features.addAll(Arrays.asList(Feature.DBNAME_AS_SCHEMA, Feature.SUPPORT_INLINE_COMMENT, Feature.ALTER_FOR_EACH_COLUMN, Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD, Feature.SUPPORT_LIMIT, Feature.COLUMN_DEF_ALLOW_NULL));
		this.features = features;
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
	}

	protected void initKeywods() {
		loadKeywords("mysql_keywords.properties");
	}

	protected void initTypes() {
		super.initTypes();
		typeNames.put(Types.BOOLEAN, "BIT(1)", 0);
		typeNames.put(Types.BLOB, "mediumblob", 0);
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

	protected void initFunctions() {
		super.initFunctions();
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
	}

	@Override
	public boolean containKeyword(String name) {
		return keywords.contains(name.toLowerCase());
	}

	@Override
	public String toDefaultString(Object defaultValue, int sqlType, int changeTo) {
		String def = String.valueOf(defaultValue);
		if (sqlType == Types.BIT && def.startsWith("b'") && def.length() > 2) {
			char c = def.charAt(2);
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

	public String getDriverClass(String url) {
		return "com.mysql.jdbc.Driver";
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		// jdbc:mysql://localhost:3306/allandb
		// ??useUnicode=true&characterEncoding=UTF-8
		// 新版本MySQL对0000-00-00加强了管理，要求增加参数
		// zeroDateTimeBehavior=convertToNull
		// zeroDateTimeBehavior=noDatetimeStringSync
		// zeroDateTimeBehavior=exception
		// zeroDateTimeBehavior=round
		sb.append("mysql:");
		sb.append("//").append(host).append(":").append(port <= 0 ? 3306 : port);
		sb.append("/").append(pathOrName).append("?useUnicode=true&characterEncoding=UTF-8");//
		String url = sb.toString();
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.info(url);
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

	// "
	// jdbc:mysql://localhost:3306/allandb?useUnicode=true&characterEncoding=UTF-8"
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

	private final LimitHandler limit = new MySqlLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter() {
		@Override
		public String extractConstraintName(SQLException sqle) {
			if ("MySQLIntegrityConstraintViolationException".equals(sqle.getClass().getSimpleName())) {
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
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String tablename, String constraintName) throws SQLException {
		String sql = "SELECT kcu.*, tc.constraint_type, rc.update_rule, rc.delete_rule, rc.match_option" + " FROM information_schema.key_column_usage kcu " + "INNER JOIN information_schema.table_constraints tc" + "        ON tc.constraint_schema = kcu.constraint_schema"
				+ "       AND tc.constraint_name = kcu.constraint_name" + "       AND tc.table_schema = kcu.constraint_schema" + "       AND tc.table_name = kcu.table_name" + " LEFT JOIN information_schema.referential_constraints rc"
				+ "        ON kcu.constraint_schema = rc.constraint_schema" + "       AND kcu.constraint_name = rc.constraint_name" + "       AND kcu.table_schema = rc.constraint_schema" + "       AND kcu.table_name = rc.table_name"
				+ "     WHERE kcu.constraint_schema like ? and kcu.table_name like ? and kcu.constraint_name like ?" + "  ORDER BY kcu.constraint_schema, kcu.table_name, kcu.constraint_name, kcu.ordinal_position";
		schema = StringUtils.isBlank(schema) ? "%" : schema.toLowerCase();
		tablename = StringUtils.isBlank(tablename) ? "%" : tablename.toLowerCase();
		constraintName = StringUtils.isBlank(constraintName) ? "%" : constraintName.toLowerCase();

		List<Constraint> constraints = conn.selectBySql(sql, new AbstractResultSetTransformer<List<Constraint>>() {

			@Override
			public List<Constraint> transformer(IResultSet rs) throws SQLException {
				List<Constraint> constraints = new ArrayList<Constraint>();
				List<String> columns = new ArrayList<String>();
				List<String> refColumns = new ArrayList<String>();
				Constraint preCon = new Constraint(); // 上一条记录
				while (rs.next()) {
					if (constraints.size() > 0) {
						preCon = constraints.get(constraints.size() - 1);
					}

					boolean isSameConstraint = rs.getString("constraint_schema").equals(preCon.getSchema()) && rs.getString("constraint_name").equals(preCon.getName()) && rs.getString("table_schema").equals(preCon.getTableSchema())
							&& rs.getString("table_name").equals(preCon.getTableName());

					if (!isSameConstraint) {
						columns = new ArrayList<String>();
						refColumns = new ArrayList<String>();
						Constraint c = new Constraint();
						c.setCatalog(rs.getString("constraint_catalog"));
						c.setSchema(rs.getString("constraint_schema"));
						c.setName(rs.getString("constraint_name"));
						c.setType(ConstraintType.parseFullName(rs.getString("constraint_type")));
						c.setDeferrable(false);
						c.setInitiallyDeferred(false);
						c.setTableCatalog(rs.getString("table_catalog"));
						c.setTableSchema(rs.getString("table_schema"));
						c.setTableName(rs.getString("table_name"));
						c.setMatchType(ForeignKeyMatchType.parseName(rs.getString("match_option")));
						c.setRefTableSchema(rs.getString("referenced_table_schema"));
						c.setRefTableName(rs.getString("referenced_table_name"));
						c.setUpdateRule(ForeignKeyAction.parseName(rs.getString("update_rule")));
						c.setDeleteRule(ForeignKeyAction.parseName(rs.getString("delete_rule")));
						c.setEnabled(true);
						c.setColumns(columns);
						c.setRefColumns(refColumns);
						constraints.add(c);
					}

					// 有指定列的约束则添加到列表
					if (StringUtils.isNotBlank(rs.getString("column_name"))) {
						columns.add(rs.getString("column_name"));
					}

					// 是外键约束则添加到参照列表
					if (StringUtils.isNotBlank(rs.getString("referenced_column_name"))) {
						refColumns.add(rs.getString("referenced_column_name"));
					}
				}
				return constraints;
			}

		}, Arrays.asList(schema, tablename, constraintName), false);

		return constraints;
	}
}
