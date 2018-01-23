package jef.database.dialect;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.dialect.handler.LimitHandler;
import jef.database.dialect.handler.LimitOffsetLimitHandler;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.object.SequenceInfo;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.EmuDateAddSubByTimesatmpadd;
import jef.database.query.function.EmuDatediffByTimestampdiff;
import jef.database.query.function.EmuDerbyCast;
import jef.database.query.function.EmuDerbyUserFunction;
import jef.database.query.function.EmuJDBCTimestampFunction;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.support.RDBMS;
import jef.tools.Exceptions;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtils;
import jef.tools.reflect.ClassEx;
import jef.tools.string.JefStringReader;

import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLTemplates;

public class H2Dialect extends AbstractDialect {
	// registerFunction( "avg", new AvgWithArgumentCastFunction( "double" ) );
	//
	// // select topic, syntax from information_schema.help
	// // where section like 'Function%' order by section, topic
	// //
	// // see also -> http://www.h2database.com/html/functions.html
	//
	//
	//
	// // String Functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// registerFunction( "ascii", new StandardSQLFunction( "ascii",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "char", new StandardSQLFunction( "char",
	// StandardBasicTypes.CHARACTER ) );
	// registerFunction( "concat", new VarArgsSQLFunction(
	// StandardBasicTypes.STRING, "(", "||", ")" ) );
	// registerFunction( "difference", new StandardSQLFunction( "difference",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "insert", new StandardSQLFunction( "lower",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "left", new StandardSQLFunction( "left",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "lcase", new StandardSQLFunction( "lcase",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "ltrim", new StandardSQLFunction( "ltrim",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "octet_length", new StandardSQLFunction(
	// "octet_length", StandardBasicTypes.INTEGER ) );
	// registerFunction( "position", new StandardSQLFunction( "position",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "repeat", new StandardSQLFunction( "repeat",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "replace", new StandardSQLFunction( "replace",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "right", new StandardSQLFunction( "right",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "rtrim", new StandardSQLFunction( "rtrim",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "soundex", new StandardSQLFunction( "soundex",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "space", new StandardSQLFunction( "space",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "stringencode", new StandardSQLFunction(
	// "stringencode", StandardBasicTypes.STRING ) );
	// registerFunction( "stringdecode", new StandardSQLFunction(
	// "stringdecode", StandardBasicTypes.STRING ) );
	// registerFunction( "stringtoutf8", new StandardSQLFunction(
	// "stringtoutf8", StandardBasicTypes.BINARY ) );
	// registerFunction( "ucase", new StandardSQLFunction( "ucase",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "utf8tostring", new StandardSQLFunction(
	// "utf8tostring", StandardBasicTypes.STRING ) );
	//
	// // Time and Date Functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// registerFunction( "curdate", new NoArgSQLFunction( "curdate",
	// StandardBasicTypes.DATE ) );
	// registerFunction( "curtime", new NoArgSQLFunction( "curtime",
	// StandardBasicTypes.TIME ) );
	// registerFunction( "curtimestamp", new NoArgSQLFunction( "curtimestamp",
	// StandardBasicTypes.TIME ) );
	// registerFunction( "current_date", new NoArgSQLFunction( "current_date",
	// StandardBasicTypes.DATE ) );
	// registerFunction( "current_time", new NoArgSQLFunction( "current_time",
	// StandardBasicTypes.TIME ) );
	// registerFunction( "current_timestamp", new NoArgSQLFunction(
	// "current_timestamp", StandardBasicTypes.TIMESTAMP ) );
	// registerFunction( "datediff", new StandardSQLFunction( "datediff",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "dayname", new StandardSQLFunction( "dayname",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "monthname", new StandardSQLFunction( "monthname",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "now", new NoArgSQLFunction( "now",
	// StandardBasicTypes.TIMESTAMP ) );
	// registerFunction( "quarter", new StandardSQLFunction( "quarter",
	// StandardBasicTypes.INTEGER ) );
	// registerFunction( "week", new StandardSQLFunction( "week",
	// StandardBasicTypes.INTEGER ) );
	//
	// // System Functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// registerFunction( "database", new NoArgSQLFunction( "database",
	// StandardBasicTypes.STRING ) );
	// registerFunction( "user", new NoArgSQLFunction( "user",
	// StandardBasicTypes.STRING ) );
	//
	// getDefaultProperties().setProperty(
	// AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	// // http://code.google.com/p/h2database/issues/detail?id=235
	// getDefaultProperties().setProperty(
	// AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	// }
	//
	// @Override
	// public String getAddColumnString() {
	// return "";
	// }
	//
	// @Override
	// public String getForUpdateString() {
	// return " for update";
	// }
	//
	// @Override
	// public LimitHandler getLimitHandler() {
	// return LIMIT_HANDLER;
	// }
	//
	// @Override
	// public boolean supportsLimit() {
	// return true;
	// }
	//
	// @Override
	// public String getLimitString(String sql, boolean hasOffset) {
	// return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
	// }
	//
	// @Override
	// public boolean bindLimitParametersInReverseOrder() {
	// return true;
	// }
	//
	// @Override
	// public boolean bindLimitParametersFirst() {
	// return false;
	// }
	//
	// @Override
	// public boolean supportsIfExistsAfterTableName() {
	// return true;
	// }
	//
	// @Override
	// public boolean supportsIfExistsBeforeConstraintName() {
	// return true;
	// }
	//
	// @Override
	// public boolean supportsSequences() {
	// return true;
	// }
	//
	// @Override
	// public boolean supportsPooledSequences() {
	// return true;
	// }
	//
	// @Override
	// public String getCreateSequenceString(String sequenceName) {
	// return "create sequence " + sequenceName;
	// }
	//
	// @Override
	// public String getDropSequenceString(String sequenceName) {
	// return "drop sequence if exists " + sequenceName;
	// }
	//
	// @Override
	// public String getSelectSequenceNextValString(String sequenceName) {
	// return "next value for " + sequenceName;
	// }
	//
	// @Override
	// public String getSequenceNextValString(String sequenceName) {
	// return "call next value for " + sequenceName;
	// }
	//
	// @Override
	// public String getQuerySequencesString() {
	// return querySequenceString;
	// }
	//
	// @Override
	// public SequenceInformationExtractor getSequenceInformationExtractor() {
	// return sequenceInformationExtractor;
	// }
	//
	// @Override
	// public ViolatedConstraintNameExtracter
	// getViolatedConstraintNameExtracter() {
	// return EXTRACTER;
	// }
	//
	// private static final ViolatedConstraintNameExtracter EXTRACTER = new
	// TemplatedViolatedConstraintNameExtracter() {
	// /**
	// * Extract the name of the violated constraint from the given
	// SQLException.
	// *
	// * @param sqle The exception that was the result of the constraint
	// violation.
	// * @return The extracted constraint name.
	// */
	// @Override
	// protected String doExtractConstraintName(SQLException sqle) throws
	// NumberFormatException {
	// String constraintName = null;
	// // 23000: Check constraint violation: {0}
	// // 23001: Unique index or primary key violation: {0}
	// if ( sqle.getSQLState().startsWith( "23" ) ) {
	// final String message = sqle.getMessage();
	// final int idx = message.indexOf( "violation: " );
	// if ( idx > 0 ) {
	// constraintName = message.substring( idx + "violation: ".length() );
	// }
	// }
	// return constraintName;
	// }
	// };
	//
	// @Override
	// public SQLExceptionConversionDelegate
	// buildSQLExceptionConversionDelegate() {
	// SQLExceptionConversionDelegate delegate =
	// super.buildSQLExceptionConversionDelegate();
	// if (delegate == null) {
	// delegate = new SQLExceptionConversionDelegate() {
	// @Override
	// public JDBCException convert(SQLException sqlException, String message,
	// String sql) {
	// final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException
	// );
	//
	// if (40001 == errorCode) {
	// // DEADLOCK DETECTED
	// return new LockAcquisitionException(message, sqlException, sql);
	// }
	//
	// if (50200 == errorCode) {
	// // LOCK NOT AVAILABLE
	// return new PessimisticLockException(message, sqlException, sql);
	// }
	//
	// if ( 90006 == errorCode ) {
	// // NULL not allowed for column [90006-145]
	// final String constraintName =
	// getViolatedConstraintNameExtracter().extractConstraintName( sqlException
	// );
	// return new ConstraintViolationException( message, sqlException, sql,
	// constraintName );
	// }
	//
	// return null;
	// }
	// };
	// }
	// return delegate;
	// }
	//
	// @Override
	// public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
	// return new LocalTemporaryTableBulkIdStrategy(
	// new IdTableSupportStandardImpl() {
	// @Override
	// public String getCreateIdTableCommand() {
	// return "create cached local temporary table if not exists";
	// }
	//
	// @Override
	// public String getCreateIdTableStatementOptions() {
	// // actually 2 different options are specified here:
	// // 1) [on commit drop] - says to drop the table on transaction commit
	// // 2) [transactional] - says to not perform an implicit commit of any
	// current transaction
	// return "on commit drop transactional"; }
	// },
	// AfterUseAction.CLEAN,
	// TempTableDdlTransactionHandling.NONE
	// );
	// }
	//
	// @Override
	// public boolean supportsCurrentTimestampSelection() {
	// return true;
	// }
	//
	// @Override
	// public boolean isCurrentTimestampSelectStringCallable() {
	// return false;
	// }
	//
	// @Override
	// public String getCurrentTimestampSelectString() {
	// return "call current_timestamp()";
	// }
	//
	// @Override
	// public boolean supportsUnionAll() {
	// return true;
	// }
	//
	//
	// // Overridden informational metadata
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//
	// @Override
	// public boolean supportsLobValueChangePropogation() {
	// return false;
	// }
	//
	// @Override
	// public boolean requiresParensForTupleDistinctCounts() {
	// return true;
	// }
	//
	// @Override
	// public boolean doesReadCommittedCauseWritersToBlockReaders() {
	// // see
	// http://groups.google.com/group/h2-database/browse_thread/thread/562d8a49e2dabe99?hl=en
	// return true;
	// }
	//
	// @Override
	// public boolean supportsTuplesInSubqueries() {
	// return false;
	// }
	//
	// @Override
	// public boolean dropConstraints() {
	// // We don't need to drop constraints before dropping tables, that just
	// leads to error
	// // messages about missing tables when we don't have a schema in the
	// database
	// return false;
	// }
	//
	// @Override
	// public IdentityColumnSupport getIdentityColumnSupport() {
	// return new H2IdentityColumnSupport();
	// }
	//
	// @Override
	// public String getQueryHintString(String query, String hints) {
	// return IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	// }
	// }

	@Override
	public RDBMS getName() {
		return RDBMS.h2;
	}

	/**
	 * TODO 构造，目前尚未仔细推销，直接从Derby中复制来的，待修改
	 */
	public H2Dialect() {
		// 判定H2的版本
		try {
			final ClassEx h2ConstantsClass = ClassEx.forName("org.h2.engine.Constants");
			final int majorVersion = (Integer) h2ConstantsClass.getDeclaredField("VERSION_MAJOR").get(null);
			final int minorVersion = (Integer) h2ConstantsClass.getDeclaredField("VERSION_MINOR").get(null);
			final int buildId = (Integer) h2ConstantsClass.getDeclaredField("BUILD_ID").get(null);
		} catch (SecurityException e) {
			throw Exceptions.asIllegalArgument(e);
		} catch (NoSuchFieldException e) {
			throw Exceptions.asIllegalArgument(e);
		}
		// if ( ! ( majorVersion > 1 || minorVersion > 2 || buildId >= 139 )
		// ) {
		// LOG.unsupportedMultiTableBulkHqlJpaql( majorVersion,
		// minorVersion, buildId );
		// }

		features = CollectionUtils.identityHashSet();
		features.addAll(Arrays.asList(Feature.BATCH_GENERATED_KEY_ONLY_LAST, Feature.ONE_COLUMN_IN_SINGLE_DDL, Feature.SUPPORT_CONCAT,
				Feature.COLUMN_ALTERATION_SYNTAX, Feature.CASE_WITHOUT_SWITCH, Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD, Feature.UNION_WITH_BUCK));
		loadKeywords("derby_keywords.properties");

		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "ALTER");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "values 1");
		setProperty(DbProperty.SELECT_EXPRESSION, "values %s");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"\"");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "IDENTITY_VAL_LOCAL()");
		registerNative(Func.mod);
		registerNative(Func.coalesce);
		registerNative(Func.locate);
		registerNative(Func.ceil, "ceiling");
		registerNative(Func.floor);
		registerNative(Func.round);
		registerNative(Func.nullif);
		registerNative(Func.length);
		registerNative(Func.cast, new EmuDerbyCast());
		registerAlias(Func.nvl, "coalesce");
		registerCompatible(Func.str, new TemplateFunction("str", "rtrim(char(%s))"));

		registerNative(Scientific.cot);// 三角余切
		registerNative(Scientific.exp);
		registerNative(Scientific.ln, "log");
		registerNative(Scientific.log10);
		registerNative(Scientific.radians);
		registerNative(Scientific.degrees);
		registerNative(new NoArgSQLFunction("random"));
		registerAlias(Scientific.rand, "random");

		registerNative(Scientific.soundex);
		registerNative(new StandardSQLFunction("stddev"));
		registerNative(new StandardSQLFunction("variance"));// 标准方差
		registerNative(new StandardSQLFunction("nullif"));

		registerNative(new StandardSQLFunction("monthname"));
		registerNative(new StandardSQLFunction("quarter"));
		// date extract functions
		registerNative(Func.second);
		registerNative(Func.minute);
		registerNative(Func.hour);
		registerNative(Func.day);
		registerNative(Func.month);
		registerNative(Func.year);

		registerNative(new NoArgSQLFunction("current_time", false));
		registerAlias(Func.current_time, "current_time");
		registerNative(new NoArgSQLFunction("current_date", false));
		registerAlias(Func.current_date, "current_date");
		registerNative(new NoArgSQLFunction("current_timestamp", false));
		registerAlias(Func.now, "current_timestamp");
		registerAlias(Func.current_timestamp, "current_timestamp");
		registerAlias("sysdate", "current_timestamp");

		registerNative(Func.trunc, new StandardSQLFunction("truncate"));
		registerNative(new StandardSQLFunction("dayname"));
		registerNative(new StandardSQLFunction("dayofyear"));
		registerNative(new StandardSQLFunction("days"));

		// derby timestamp Compatiable functions
		registerCompatible(Func.adddate, new EmuDateAddSubByTimesatmpadd(Func.adddate));
		registerCompatible(Func.subdate, new EmuDateAddSubByTimesatmpadd(Func.subdate));
		registerCompatible(Func.datediff, new EmuDatediffByTimestampdiff());
		registerCompatible(Func.timestampdiff, new EmuJDBCTimestampFunction(Func.timestampdiff, this));
		registerCompatible(Func.timestampadd, new EmuJDBCTimestampFunction(Func.timestampadd, this));
		registerCompatible(Func.translate, new EmuDerbyUserFunction("translate", "USR_TRANSLATE"));

		// cast functions
		registerNative(Func.date);
		registerNative(Func.time);
		registerNative(new StandardSQLFunction("timestamp"));
		registerNative(new StandardSQLFunction("timestamp_iso"));
		registerNative(new StandardSQLFunction("week"));
		registerNative(new StandardSQLFunction("week_iso"));

		registerNative(new StandardSQLFunction("double"));
		registerNative(new StandardSQLFunction("varchar"));
		registerNative(new StandardSQLFunction("real"));
		registerNative(new StandardSQLFunction("bigint"));
		registerNative(new StandardSQLFunction("char"));
		registerNative(new StandardSQLFunction("integer"), "int");
		registerNative(new StandardSQLFunction("smallint"));

		registerNative(new StandardSQLFunction("digits"));
		registerNative(new StandardSQLFunction("chr"));
		registerCompatible(null, new TemplateFunction("power", "exp(%2$s * ln(%1$s))"), "power");// power(b,
																									// x)
																									// =
																									// exp(x
																									// *
																									// ln(b))
		registerCompatible(Func.add_months, new TemplateFunction("add_months", "{fn timestampadd(SQL_TSI_MONTH,%2$s,%1$s)}"));
		typeNames.put(Types.BOOLEAN, "boolean", 0);
		typeNames.put(Types.LONGVARCHAR, Integer.MAX_VALUE, "varchar($l)", 0);

	}

	@Override
	public String getDriverClass(String url) {
		return "org.h2.Driver";
	}

	private final LimitHandler limit = new LimitOffsetLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}

	@Override
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ', ';');
		reader.consume("jdbc:h2:");
		if (reader.matchNext("tcp://") > -1) {// 网络
			reader.consume("tcp://");
			connectInfo.setHost(reader.readToken('/', ' '));
			if("mem:".equalsIgnoreCase(reader.nextString(4))){
				reader.consume("mem:");
				connectInfo.setDbname(reader.readToken(';'));
			}else{
				reader.consume("~/");
				connectInfo.setDbname(reader.readToken(';'));	
			}
		} else {// 本地
			String path = reader.readToken(';');
			path.replace('\\', '/');
			String dbname = StringUtils.substringAfterLast(path, "/");
			connectInfo.setDbname(dbname);
		}
		reader.close();

	}

	@Override
	public String getDefaultSchema() {
		return "PUBLIC";
	}
	
	@Override
	public SQLTemplates getQueryDslDialect() {
		return new H2Templates();
	}

	@Override
	public List<SequenceInfo> getSequenceInfo(DbMetaData conn, String schema, String seqName) throws SQLException {
		return super.getSequenceInfo(conn, schema, seqName);
		// 如果JDBC未能很好实现，可以从系统表获取
		// String querySequenceString =
		// "select sequence_name from information_schema.sequences";
	}

}
