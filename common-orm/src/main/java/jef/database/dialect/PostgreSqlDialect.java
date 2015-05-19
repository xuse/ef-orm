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

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Clob;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.type.AColumnMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.exception.JDBCExceptionHelper;
import jef.database.exception.TemplatedViolatedConstraintNameExtracter;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.jdbc.JDBCTarget;
import jef.database.jdbc.statement.DelegatingPreparedStatement;
import jef.database.jdbc.statement.DelegatingStatement;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
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
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.string.JefStringReader;

public class PostgreSqlDialect extends AbstractDialect {
	protected static final String JDBC_URL_FORMAT = "jdbc:postgresql://%1$s:%2$s/%3$s";
	protected static final int DEFAULT_PORT = 5432;

	public PostgreSqlDialect() {
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(Feature.ALTER_FOR_EACH_COLUMN, Feature.COLUMN_ALTERATION_SYNTAX, Feature.SUPPORT_CONCAT, Feature.SUPPORT_SEQUENCE, Feature.SUPPORT_LIMIT, Feature.AI_TO_SEQUENCE_WITHOUT_DEFAULT));

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
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"\"");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "SELECT currval('%tableName%_%columnName%_seq')");

		typeNames.put(Types.BLOB, "bytea", Types.VARBINARY);
		typeNames.put(Types.CLOB, "text", 0);
		typeNames.put(Types.BOOLEAN, "boolean", 0);
		typeNames.put(Types.TINYINT, "int2", 0);
		typeNames.put(Types.SMALLINT, "int2", 0);
		typeNames.put(Types.INTEGER, "int4", 0);
		typeNames.put(Types.BIGINT, "int8", 0);
		typeNames.put(Types.FLOAT, "float4", 0);
		typeNames.put(Types.DOUBLE, "float8", 0);
		typeNames.put(Types.NUMERIC, "numeric($p, $s)", 0);
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
			LogUtil.exception("Initlize user function error.", e);
		}
	}

	public String getDriverClass(String url) {
		return "org.postgresql.Driver";
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		String url = String.format(JDBC_URL_FORMAT, host, (port <= 0 ? DEFAULT_PORT : port), pathOrName);
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.show(url);
		}
		return url;
	}

	/**
	 * PostgreSQL 无论建表SQL中的表名是大写还是小写，最终DB中的表名都是小写； 而列名是区分大小写的； <br>
	 * 为了避免大小写引起的问题，一律转成小写。
	 * 
	 * @param name
	 */
	@Override
	public String getObjectNameToUse(String name) {
		if (name == null || name.length()==0)
			return null;
		if (name.charAt(0) == '"')
			return name;
		return name.toLowerCase();
	}

	@Override
	public String getColumnNameToUse(String name) {
		if (name == null || name.length()==0)
			return null;
		if (name.charAt(0) == '"')
			return name;
		return name.toLowerCase();
	}

	@Override
	public String getColumnNameToUse(AColumnMapping name) {
		return name.lowerColumnName();
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
		// if(sequenceMode){
		// if(column.getSqlType()==Types.BIGINT){
		// return flag?"int8 not null":"int8";
		// }else{
		// return flag?"int4 not null":"int4";
		// }
		// }else{
		if (column.getSqlType() == Types.BIGINT) {
			return flag ? "serial8 not null" : "serial8";
		} else {
			return flag ? "serial4 not null" : "serial4";
		}
		// }
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
	public long getColumnAutoIncreamentValue(AutoIncrementMapping mapping, JDBCTarget db) {
		String tableName = mapping.getMeta().getTableName(false).toLowerCase();
		String seqname = tableName + "_" + mapping.lowerColumnName() + "_seq";
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
				// PG在Batch模式下抛出的顶层错误是难以理解的。直接抛出nextException即可。
				//throw e.getNextException() == null ? e : e.getNextException();
				throw e;
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

	private final LimitHandler limit = new LimitOffsetLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		public String extractConstraintName(SQLException sqle) {
			try {
				int sqlState = Integer.valueOf(JDBCExceptionHelper.extractSqlState(sqle)).intValue();
				switch (sqlState) {
				// CHECK VIOLATION
				case 23514:
					return sqle.getMessage();
					// UNIQUE VIOLATION
				case 23505:
					return sqle.getMessage();
					// FOREIGN KEY VIOLATION
				case 23503:
					return sqle.getMessage();
					// NOT NULL VIOLATION
				case 23502:
					return sqle.getMessage();
					// TODO: RESTRICT VIOLATION
				case 23001:
					return null;
					// ALL OTHER
				default:
					return null;
				}
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	};
}
