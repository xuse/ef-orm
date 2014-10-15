package jef.database.dialect;

import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbFunction;
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
import jef.tools.collection.CollectionUtil;
import jef.tools.string.JefStringReader;

/**
 * Dialect for SQL Server 2000 and before..
 * @author jiyi
 *
 */
public class SQLServer2000Dialect extends AbstractDialect {
	public SQLServer2000Dialect() {
		super();
		features = CollectionUtil.identityHashSet();
		features.add(Feature.COLUMN_DEF_ALLOW_NULL);
		
		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "SELECT @@IDENTITY");
		
		loadKeywords("sqlserver_keywords.properties");
		
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
	}



	public int getPort() {
		return 1433;
	}

	public RDBMS getName() {
		return RDBMS.sqlserver;
	}



	public String getFunction(DbFunction function, Object... params) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getDefaultSchema() {
		return "dbo";
	}

	public String toPageSQL(String sql, IntRange range) {
		throw new UnsupportedOperationException();
	}
	

	
	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		// jdbc:microsoft:sqlserver:@localhost:1433; DatabaseName =allandb
		sb.append("microsoft:sqlserver:");
		sb.append("//").append(host).append(":").append(port <= 0 ? 1433 : port);
		sb.append("; DatabaseName=").append(pathOrName);
		String url = sb.toString();
		return url;
	}

	public String getDriverClass(String url) {
		return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
	}
	
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consumeIgnoreCase("jdbc:microsoft:sqlserver:");
		reader.consumeChars('@', '/');
		String host = reader.readToken(':', '/');
		connectInfo.setHost(host);
		if (reader.omitAfterKeyIgnoreCase("databasename=", ' ') != -1) {
			String dbname = reader.readToken(' ', ';', ':');
			connectInfo.setDbname(dbname);
		}
	}
	
}
