package jef.database.dialect;

import jef.common.wrapper.IntRange;
import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.EmuDecodeWithCase;
import jef.database.query.function.EmuSQLServerTimestamp;
import jef.database.query.function.EmuSQLServerTrunc;
import jef.database.query.function.EmuTranslateByReplace;
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
		features.add(Feature.CONCAT_IS_ADD);
		
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
		registerNative(Scientific.radians);
		registerNative(Scientific.degrees);
		
		
		registerNative(new StandardSQLFunction("ceiling"));
		registerAlias(Func.ceil,"ceiling");
		registerNative(Func.floor);
		registerNative(Func.round);
		registerNative(Func.nullif);
		registerNative(Func.replace);
		registerNative(Func.substring);
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Func.lower);
		registerNative(Func.upper);
		registerNative(Func.cast);
		registerNative(Func.replace);
		registerCompatible(Func.translate, new EmuTranslateByReplace());

		registerNative(new NoArgSQLFunction("@@datefirst", false));
		registerNative(new NoArgSQLFunction("@@options", false));
		registerNative(new NoArgSQLFunction("@@dbts", false));
		registerNative(new NoArgSQLFunction("@@remserver", false));
		registerNative(new NoArgSQLFunction("@@langid", false));
		registerNative(new NoArgSQLFunction("@@servername", false));
		registerNative(new NoArgSQLFunction("@@language", false));
		registerNative(new NoArgSQLFunction("@@options", false));
		registerNative(new NoArgSQLFunction("@@servicename", false));
		registerNative(new NoArgSQLFunction("@@lock_timeout", false));
		registerNative(new NoArgSQLFunction("@@spid", false));
		registerNative(new NoArgSQLFunction("@@textsize", false));
		registerNative(new NoArgSQLFunction("@@max_precision", false));
		registerNative(new NoArgSQLFunction("@@max_connections", false));
		registerNative(new NoArgSQLFunction("@@version", false));
		registerNative(new NoArgSQLFunction("@@nestlevel", false));
		
		
		registerNative(new StandardSQLFunction("char"));
		registerNative(new StandardSQLFunction("ascii"));
		registerNative(new StandardSQLFunction("space"));
		registerNative(new StandardSQLFunction("patindex"));
		registerNative(new StandardSQLFunction("charindex"));
		registerNative(new StandardSQLFunction("stuff"));
		registerNative(new StandardSQLFunction("replicate"));
		registerNative(new StandardSQLFunction("reverse"));
		registerNative(new StandardSQLFunction("dateadd"));
		registerNative(new StandardSQLFunction("current_user"));
		registerNative(new StandardSQLFunction("datename"));
		registerNative(new StandardSQLFunction("len"));
		
		
		registerAlias(Func.length, "len");
		registerNative(new StandardSQLFunction("datalength"));
		registerAlias(Func.lengthb, "datalength");
		registerAlias(Func.locate, "charindex");

		registerNative(Func.current_timestamp,"getutcdate","getdate");
		registerCompatible(Func.current_date,new TemplateFunction("current_date","cast(current_timestamp as date)"));
		registerCompatible(Func.current_time,new TemplateFunction("current_time","cast(current_timestamp as time)"));
		registerAlias(Func.now, "current_timestamp");
		registerAlias("sysdate","current_timestamp");
		
		registerNative(Func.year);
		registerNative(Func.month);
		registerNative(Func.day);
		registerCompatible(Func.hour,new TemplateFunction("hour","datepart(hour,%s)"));
		registerCompatible(Func.minute,new TemplateFunction("minute","datepart(minute,%s)"));
		registerCompatible(Func.second,new TemplateFunction("second","datepart(second,%s)"));
		registerCompatible(Func.date,new TemplateFunction("date","cast(%s as date)"));
		registerCompatible(Func.time,new TemplateFunction("time","cast(%s as time)"));
		
		registerCompatible(Func.timestampadd, new EmuSQLServerTimestamp("timestampadd","dateadd"));
		registerCompatible(Func.timestampdiff, new EmuSQLServerTimestamp("timestampdiff","datediff"));
		registerCompatible(Func.datediff,new TemplateFunction("datediff", "datediff(day,%2$s,%1$s)"));

		registerCompatible(Func.adddate, new TemplateFunction("adddate", "dateadd(day,%2$s,%1$s)"));
		registerCompatible(Func.subdate, new TemplateFunction("subdate", "dateadd(day,-%2$s,%1$s)"));
		
		registerCompatible(Func.add_months, new TemplateFunction("add_months", "dateadd(month,%2$s,%1$s)"));
		
		registerCompatible(Func.mod, new TemplateFunction("mod","(%1$s %% %2$s)"));
		registerCompatible(Func.decode, new EmuDecodeWithCase());
		registerCompatible(Func.concat,new VarArgsSQLFunction("", "+", "")); //没有concat函数的，要改写为相加
		registerCompatible(Func.trim,new TemplateFunction("trim", "ltrim(rtrim(%s))")); //没有concat函数的，要改写为相加
		registerCompatible(Func.lpad,new TemplateFunction("lpad", "(replicate(%3$s, %2$s-len(%1$s)) + %1$s)"));
		registerCompatible(Func.rpad,new TemplateFunction("rpad", "(%1$s + replicate(%3$s, %2$s-len(%1$s)))"));
		registerCompatible(Func.trunc,new EmuSQLServerTrunc());
		registerCompatible(Func.str,new TemplateFunction("str","convert(varchar,%s,120)"));
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
