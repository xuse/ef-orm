package jef.database.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;

import javax.persistence.PersistenceException;

import jef.common.Cfg;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Blob;
import jef.database.dialect.ColumnType.Clob;
import jef.database.dialect.ColumnType.Int;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.statement.DelegatingPreparedStatement;
import jef.database.dialect.statement.DelegatingStatement;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.CastFunction;
import jef.database.query.function.EmuDateAddSubByTimesatmpadd;
import jef.database.query.function.EmuDatediffByTimestampdiff;
import jef.database.query.function.EmuDecodeWithCase;
import jef.database.query.function.EmuJDBCTimestampFunction;
import jef.database.query.function.EmuLocateOnPostgres;
import jef.database.query.function.EmuPostgreTimestampDiff;
import jef.database.query.function.EmuPostgresAddDate;
import jef.database.query.function.EmuPostgresExtract;
import jef.database.query.function.EmuPostgresSubDate;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.query.function.VarArgsSQLFunction;
import jef.database.support.RDBMS;
import jef.tools.ArrayUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.string.JefStringReader;

import org.postgresql.util.PGobject;

public class PostgreSqlDialect extends DbmsProfile {
	protected static final String DRIVER_CLASS = "org.postgresql.Driver";
	protected static final String JDBC_URL_FORMAT = "jdbc:postgresql://%1$s:%2$s/%3$s";
	protected static final int DEFAULT_PORT = 5432;

	protected static final String POSTGRESQL_PAGE = " limit %next% offset %start%";
	

	public PostgreSqlDialect() {
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(Feature.ALTER_FOR_EACH_COLUMN, Feature.COLUMN_ALTERATION_SYNTAX, Feature.SUPPORT_CONCAT, Feature.SUPPORT_BOOLEAN, Feature.SUPPORT_SEQUENCE,Feature.SUPPORT_LIMIT));
		
		boolean needSequence = JefConfiguration.getBoolean(Cfg.valueOf("postgresql.need.sequence"), false);
		if (needSequence) {
			features.add(Feature.AUTOINCREMENT_NEED_SEQUENCE);
		}

		loadKeywords("postgresql_keywords.properties");

		registerNative(Func.coalesce);
		registerAlias(Func.nvl, "coalesce");

		registerNative(Scientific.cot);
		registerNative(Scientific.exp);
		registerNative(Scientific.ln, "log");
		registerNative(new StandardSQLFunction("cbrt"));
		registerNative(Scientific.radians);
		registerNative(Scientific.degrees);

		registerNative(new StandardSQLFunction("stddev"));
		registerNative(new StandardSQLFunction("variance"));

		registerNative(new NoArgSQLFunction("random"));
		registerAlias(Scientific.rand, "random");
		registerNative(Func.cast, new CastFunction());
		registerNative(Func.mod);
		registerNative(Func.nullif);
		registerNative(Func.round);
		registerNative(Func.trunc);
		registerNative(Func.ceil);
		registerNative(Func.floor);
		registerNative(Func.translate);
		registerNative(new StandardSQLFunction("chr"));
		registerNative(Func.lower);
		registerNative(Func.upper);
		registerAlias("lcase", "lower");
		registerAlias("ucase", "upper");
		registerNative(new StandardSQLFunction("substr"));
		registerAlias(Func.substring, "substr");
		registerNative(new StandardSQLFunction("initcap"));
		registerNative(new StandardSQLFunction("to_ascii"));
		registerNative(new StandardSQLFunction("quote_ident"));
		registerNative(new StandardSQLFunction("quote_literal"));
		registerNative(new StandardSQLFunction("md5"));
		registerNative(new StandardSQLFunction("ascii"));
		registerNative(new StandardSQLFunction("char_length"));
		registerAlias(Func.length, "char_length");
		registerNative(new StandardSQLFunction("bit_length"));
		registerNative(new StandardSQLFunction("octet_length"));

		registerNative(new StandardSQLFunction("age"));// 单参数时计算当前时间与指定时间的差，双参数时计算第一个减去第二个时间
		registerNative(new NoArgSQLFunction("current_date", false));
		registerAlias(Func.current_date, "current_date");

		registerNative(new NoArgSQLFunction("current_time", false));
		registerAlias(Func.current_time, "current_time");

		registerNative(new NoArgSQLFunction("current_timestamp", false), "now");
		registerAlias(Func.current_timestamp, "current_timestamp");
		registerAlias(Func.now, "current_timestamp");
		registerAlias("sysdate", "current_timestamp");

		registerNative(new StandardSQLFunction("date_trunc"));
		registerNative(new NoArgSQLFunction("localtime", false));
		registerNative(new NoArgSQLFunction("localtimestamp", false));

		registerNative(new NoArgSQLFunction("timeofday"));
		registerNative(new StandardSQLFunction("isfinite"));
		registerNative(Func.date);
		registerCompatible(Func.time, new TemplateFunction("time", "cast(%s as time)"));

		registerNative(new NoArgSQLFunction("current_user", false));
		registerNative(new NoArgSQLFunction("session_user", false));
		registerNative(new NoArgSQLFunction("user", false));
		registerNative(new NoArgSQLFunction("current_database", true));
		registerNative(new NoArgSQLFunction("current_schema", true));

		registerNative(new StandardSQLFunction("to_char"));
		registerNative(new StandardSQLFunction("to_date"));
		registerNative(new StandardSQLFunction("to_timestamp"));
		registerNative(new StandardSQLFunction("to_number"));

		registerNative(new StandardSQLFunction("bool_and"));
		registerNative(new StandardSQLFunction("bool_or"));
		registerNative(new StandardSQLFunction("bit_and"));
		registerNative(new StandardSQLFunction("bit_or"));
		registerNative(new StandardSQLFunction("extract"));
		registerNative(Func.replace);
		registerNative(Func.trim);
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Func.lpad);
		registerNative(Func.rpad);

		registerCompatible(Func.concat, new VarArgsSQLFunction("", "||", "")); // Derby是没有concat函数的，要改写为相加
		registerCompatible(Func.locate, new EmuLocateOnPostgres());
		registerCompatible(Func.year, new EmuPostgresExtract("year"));
		registerCompatible(Func.month, new EmuPostgresExtract("month"));
		registerCompatible(Func.day, new EmuPostgresExtract("day"));
		registerCompatible(Func.hour, new EmuPostgresExtract("hour"));
		registerCompatible(Func.minute, new EmuPostgresExtract("minute"));
		registerCompatible(Func.second, new EmuPostgresExtract("second"));
		registerCompatible(Func.adddate, new EmuPostgresAddDate());
		registerCompatible(Func.subdate, new EmuPostgresSubDate());
		registerCompatible(Func.add_months, new TemplateFunction("add_months", "{fn timestampadd(SQL_TSI_MONTH,%2$s,%1$s)}"));
		registerCompatible(null, new TemplateFunction("timestamp", "%1$s::TIMESTAMP"), "timestamp");

		registerCompatible(Func.timestampdiff, new EmuPostgreTimestampDiff());// 等PG的驱动完善了，可以改为EmuJDBCTimestampFunction
		registerCompatible(Func.timestampadd, new EmuJDBCTimestampFunction(Func.timestampadd, this));

		registerCompatible(Func.datediff, new EmuDatediffByTimestampdiff());// 等PG的驱动完善了，可以改为EmuJDBCTimestampFunction
		registerCompatible(Func.adddate, new EmuDateAddSubByTimesatmpadd(Func.adddate));
		registerCompatible(Func.subdate, new EmuDateAddSubByTimesatmpadd(Func.subdate));

		registerCompatible(Func.decode, new EmuDecodeWithCase());
		registerCompatible(Func.lengthb, new TemplateFunction("lengthb", "bit_length(%s)/8"));
		registerCompatible(Func.str, new CastFunction("str", "varchar"));

		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "ALTER COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");
		setProperty(DbProperty.SEQUENCE_FETCH, "select nextval('%s')");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"");
	}

	public RDBMS getName() {
		return RDBMS.postgresql;
	}
	
	@Override
	public void init(OperateTarget db) {
		super.init(db);
		try {
			ensureUserFunction(this.functions.get("timestampdiff"), db);
		} catch (SQLException e) {
			LogUtil.exception("Initlize user function error.",e);
		}
	}

	/**
	 * 获取该数据库的返回已生成的主键的函数(暂时没用到，用到时需修改)
	 * <p>
	 * PostgreSQL: SELECT currval('表名_列名_seq')
	 * </p>
	 */
	public String getGeneratedFetchFunction() {
		return "SELECT currval('%tableName%_%columnName%_seq')";
	}

	public String getDriverClass(String url) {
		return DRIVER_CLASS;
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		String url = String.format(JDBC_URL_FORMAT, host, (port <= 0 ? DEFAULT_PORT : port), pathOrName);
		if(ORMConfig.getInstance().isDebugMode()){
			LogUtil.show(url);
		}
		return url;
	}

	public String toPageSQL(String sql, IntRange range) {
		boolean isUnion = false;
		try {
			Select select = DbUtils.parseNativeSelect(sql);
			if (select.getSelectBody() instanceof Union) {
				isUnion = true;
			}
			select.getSelectBody();
		} catch (ParseException e) {
			LogUtil.exception("SqlParse Error:", e);
		}

		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(POSTGRESQL_PAGE, new String[] { "%start%", "%next%" }, new String[] { start, next });
		return isUnion ? StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit);
	}

	@Override
	public String toPageSQL(String sql, IntRange range, boolean isUnion) {
		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(POSTGRESQL_PAGE, new String[] { "%start%", "%next%" }, new String[] { start, next });
		return isUnion ? StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit);
	}

	/**
	 * PostgreSQL 无论建表SQL中的表名是大写还是小写，最终DB中的表名都是小写； 而列名是区分大小写的； <br>
	 * 为了避免大小写引起的问题，一律转成小写。
	 * 
	 * @param name
	 */
	@Override
	public String getObjectNameIfUppercase(String name) {
		return name == null ? null : name.toLowerCase();
	}

	@Override
	public String getColumnNameIncase(String name) {
		return name == null ? null : name.toLowerCase();
	}

	@Override
	public Object getJavaValue(ColumnType column, Object object) {
		if (object == null)
			return null;

		// boolean类型的解析，优先处理 column type符合的情况
		if (column instanceof ColumnType.Boolean) {
			return ArrayUtils.containsIgnoreCase(new String[] { "t", "true", "y", "yes", "on", "1" }, object.toString());
		}
		// bit, bit[n]
		else if (object instanceof java.lang.Boolean) {
			return ((java.lang.Boolean) object).booleanValue() ? "1" : "0";
		}
		// varbit, cidr, inet, maccaddr, interval, tsvector, tsquery, xml,
		// box, circle, line, lseg, path, point, polygon
		else if (object instanceof PGobject) {
			return ((PGobject) object).getValue();
		} else if (object instanceof String || object instanceof Number || column instanceof ColumnType.Date || column instanceof ColumnType.TimeStamp || column instanceof ColumnType.Blob) {
			return object;
		} else {
			System.err.println("Unknown javaField Type: " + object.getClass().getName());
		}
		return object;
	}

	@Override
	protected String getComment(Blob column, boolean flag) {
		return buildComment(column, PostgreSqlColumnTypes.bytea.name(), flag);
	}

	private String buildComment(ColumnType column, String nativeColumnType, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append(nativeColumnType);
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
			if (column.defaultValue != null)
				sb.append(" default ").append(toDefaultString(column.defaultValue));
		}
		return sb.toString();
	}

	@Override
	protected String getComment(Clob column, boolean flag) {
		return buildComment(column, PostgreSqlColumnTypes.text.name(), flag);
	}

	@Override
	protected String getComment(ColumnType.Boolean column, boolean flag) {
		return buildComment(column, PostgreSqlColumnTypes.BOOLEAN.name().toLowerCase(), flag);
	}

	@Override
	protected String getComment(Int column, boolean flag) {
		return column.precision > 10 ? buildComment(column, PostgreSqlColumnTypes.int8.name(), flag) : buildComment(column, PostgreSqlColumnTypes.int4.name(), flag);
	}

	@Override
	protected String getComment(ColumnType.Double column, boolean flag) {
		return (column.precision + column.scale) > 16 ? buildComment(column, PostgreSqlColumnTypes.float8.name(), flag) : buildComment(column, PostgreSqlColumnTypes.float4.name(), flag);
	}

	@Override
	protected String getComment(AutoIncrement column, boolean flag) {
		/*
		 * PG的自增主键后台其实是用类似于Oracle的实现完成的，后台会自动创建名为“： 表名_列命_seq这样一个sequence
		 * 支持类似于Oracle的currval和nextval语法，具体写法如下：select
		 * nextval('ca_asset_asset_id_seq'); select
		 * currval('ca_asset_asset_id_seq'); 这为我们操作PG的Sequence提供了方便。
		 * 要注意这里的currval含义用法和Oracle一样
		 * ，不是获取sequence当前的值，而是返回当前sesssion中上一次获取过的seq值。
		 */
		return column.getLength() > 10 ? buildComment(column, PostgreSqlColumnTypes.serial8.name(), flag) : buildComment(column, PostgreSqlColumnTypes.serial4.name(), flag);
	}

	protected String getComment(ColumnType.GUID column, boolean flag) {
		return buildComment(column, PostgreSqlColumnTypes.uuid.name(), flag);
	}

	public ColumnType getProprtMetaFromDbType(jef.database.meta.Column column) {
		if ("text".equals(column.getDataType())) {
			return new Clob();
		} else if ("money".equals(column.getDataType())) {
			return new Varchar(column.getColumnSize() + 2);
		} else {
			return super.getProprtMetaFromDbType(column);
		}
	}

	/**
	 * Arrays, composite types, custom types are not yet supported.
	 */
	protected enum PostgreSqlColumnTypes {
		// below column types will mapping to integer type
		bigserial, serial8, serial4, serial, int8, int4, int2, INT, integer, bigint, smallint,

		// below column types will mapping to real type
		float8, float4, DOUBLE, numeric, decimal, real,

		// below column types will mapping to varchar type
		varbit, varchar, cidr, inet, macaddr, uuid, money, tsquery, tsvector, txid_snapshot, interval,

		// below column types will mapping to varchar type too
		box, circle, line, lseg, path, point, polygon,

		// below column types will mapping to char type
		// "bpchar" 是当 column 名称为 name 时，由db metadata得到的类型。
		bit, character, CHAR, bpchar,

		// below column types will mapping to boolean type
		BOOLEAN, bool,

		// below column types will mapping to date type
		date,

		// below column types will mapping to timestamp type
		time, timetz, timestamp, timestamptz,

		// below column types will mapping to blob type
		bytea,

		// below column types will mapping to clob type
		text, xml;
	}

	// ,"jdbc:postgresql://localhost/soft"
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consume("jdbc:postgresql:");
		reader.omitChars('/');
		String host = reader.readToken('/');
		String name = reader.readToken(';', '?', '/');
		connectInfo.setHost(host);
		connectInfo.setDbname(name);
	}

	@Override
	public void processIntervalExpression(BinaryExpression parent, Interval interval) {
		interval.toPostgresMode();
	}

	@Override
	public void processIntervalExpression(Function func, Interval interval) {
		interval.toPostgresMode();
	}

	@Override
	public int getBlobDataType() {
		return Types.VARBINARY;
	}

	@Override
	public long getColumnAutoIncreamentValue(AutoIncrementMapping<?> mapping, OperateTarget db) {
		String tableName = mapping.getMeta().getTableName(false).toLowerCase();
		String seqname = tableName + "_" + mapping.columnName().toLowerCase() + "_seq";
		String sql = String.format("select nextval('%s')", seqname);
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(sql + " | " + db.getTransactionId());
		}
		try {
			Statement st = db.createStatement();
			ResultSet rs = null;
			try {
				rs = st.executeQuery(sql);
				rs.next();
				return rs.getLong(1);
			} finally {
				DbUtils.close(rs);
				DbUtils.close(st);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public Statement wrap(Statement stmt, boolean isInJpaTx) throws SQLException {
		if (isInJpaTx && ORMConfig.getInstance().isKeepTxForPG()) {
			return new PGTxStatement(stmt);
		} else {
			return stmt;
		}
	}

	@Override
	public PreparedStatement wrap(PreparedStatement stmt, boolean isInJpaTx) throws SQLException {
		if (isInJpaTx && ORMConfig.getInstance().isKeepTxForPG()) {
			return new PGTxPreparedStatement(stmt);
		} else {
			return stmt;
		}

	}

	@Override
	public String getDefaultSchema() {
		return "public";
	}

	@Override
	public String getSchema(String schema) {
		return schema != null ? schema.toLowerCase() : schema;
	}

	/**
	 * 如果确定不需要这个特性支持，可以使用 db.keep.tx.for.postgresql=false来关闭
	 * 
	 * @author jiyi
	 * 
	 */
	private static final class PGTxPreparedStatement extends DelegatingPreparedStatement {
		private Connection conn;

		public PGTxPreparedStatement(PreparedStatement s) throws SQLException {
			super(s);
			this.conn = s.getConnection();
		}

		@Override
		public ResultSet executeQuery() throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return ((PreparedStatement) _stmt).executeQuery();
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public int[] executeBatch() throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeBatch();
			} catch (SQLException e) {
				conn.rollback(sp);
				//PG在Batch模式下抛出的顶层错误是难以理解的。直接抛出nextException即可。
				throw e.getNextException()==null?e:e.getNextException();
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public int executeUpdate() throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return ((PreparedStatement) _stmt).executeUpdate();
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public boolean execute() throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return ((PreparedStatement) _stmt).execute();
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}
	}

	private static final class PGTxStatement extends DelegatingStatement {
		private Connection conn;

		public PGTxStatement(Statement s) throws SQLException {
			super(s);
			this.conn = s.getConnection();

		}

		@Override
		public int[] executeBatch() throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeBatch();
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public int executeUpdate(String sql) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeUpdate(sql);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public boolean execute(String sql) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.execute(sql);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeUpdate(sql, autoGeneratedKeys);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeUpdate(sql, columnIndexes);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public int executeUpdate(String sql, String[] columnNames) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeUpdate(sql, columnNames);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.execute(sql, autoGeneratedKeys);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public boolean execute(String sql, int[] columnIndexes) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.execute(sql, columnIndexes);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public boolean execute(String sql, String[] columnNames) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.execute(sql, columnNames);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}

		@Override
		public ResultSet executeQuery(String sql) throws SQLException {
			Savepoint sp = conn.setSavepoint();
			try {
				return _stmt.executeQuery(sql);
			} catch (SQLException e) {
				conn.rollback(sp);
				throw e;
			} finally {
				conn.releaseSavepoint(sp);
			}
		}
	}

	@Override
	public boolean containKeyword(String name) {
		return keywords.contains(StringUtils.lowerCase(name));
	}
}
