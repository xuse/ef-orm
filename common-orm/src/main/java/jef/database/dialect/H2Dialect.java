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
		features.addAll(Arrays.asList(Feature.BATCH_GENERATED_KEY_ONLY_LAST, Feature.ONE_COLUMN_IN_SINGLE_DDL, Feature.SUPPORT_CONCAT,Feature.NOT_SUPPORT_USER_FUNCTION,
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
