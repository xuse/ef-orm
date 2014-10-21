package jef.database.dialect;

import java.sql.Types;

import jef.database.ConnectInfo;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.statement.LimitHandler;
import jef.database.dialect.statement.UnionJudgement;
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

import org.apache.commons.lang.StringUtils;

/**
 * Dialect for SQL Server 2000 and before..
 * 
 * @author jiyi
 * 
 */
public class SQLServer2000Dialect extends AbstractDialect {
	public SQLServer2000Dialect() {
		super();
		features = CollectionUtil.identityHashSet();
		features.add(Feature.COLUMN_DEF_ALLOW_NULL);
		features.add(Feature.CONCAT_IS_ADD);
		features.add(Feature.NOT_SUPPORT_KEYWORD_DEFAULT);
		features.add(Feature.BATCH_GENERATED_KEY_BY_FUNCTION);
		// features.add(Feature.NO_BIND_FOR_INSERT);
		// features.add(Feature.NO_BIND_FOR_SELECT);

		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "SELECT @@IDENTITY");

		loadKeywords("sqlserver_keywords.properties");

		typeNames.put(Types.BINARY, "binary($l)", 0);
		typeNames.put(Types.BIT, "tinyint", Types.TINYINT);
		typeNames.put(Types.BIGINT, "numeric(19,0)", 0);
		typeNames.put(Types.DOUBLE, "double precision", 0);
		typeNames.put(Types.DATE, "datetime", Types.TIMESTAMP);
		typeNames.put(Types.TIME, "datetime", Types.TIMESTAMP);
		typeNames.put(Types.TIMESTAMP, "datetime", 0);
		typeNames.put(Types.VARBINARY, "varbinary($l)", 0);
		typeNames.put(Types.NUMERIC, "numeric($p,$s)", 0);
		typeNames.put(Types.BLOB, "image", 0);
		typeNames.put(Types.CLOB, "text", 0);
		typeNames.put(Types.VARBINARY, "image", 0);
		typeNames.put(Types.VARBINARY, 8000, "varbinary($l)", 0);
		typeNames.put(Types.LONGVARBINARY, "image", 0);
		typeNames.put(Types.LONGVARCHAR, "text", Types.CLOB);
		typeNames.put(Types.BOOLEAN, "bit", Types.BIT);

		registerNative(Func.coalesce);
		registerAlias(Func.nvl, "coalesce");
		registerNative(Scientific.cot);
		registerNative(Scientific.exp);
		registerNative(Scientific.ln, "log");
		registerNative(Scientific.radians);
		registerNative(Scientific.degrees);

		registerNative(new StandardSQLFunction("ceiling"));
		registerAlias(Func.ceil, "ceiling");
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

		registerNative(Func.current_timestamp, new NoArgSQLFunction("current_timestamp", false), "getutcdate", "getdate");
		registerCompatible(Func.current_date, new TemplateFunction("current_date", "cast(current_timestamp as date)"));
		registerCompatible(Func.current_time, new TemplateFunction("current_time", "cast(current_timestamp as time)"));
		registerAlias(Func.now, "current_timestamp");
		registerAlias("sysdate", "current_timestamp");

		registerNative(Func.year);
		registerNative(Func.month);
		registerNative(Func.day);
		// registerCompatible("extract",new
		// TemplateFunction("extract","datepart(%1$s,%3$s)"));
		registerCompatible(Func.hour, new TemplateFunction("hour", "datepart(hour,%s)"));
		registerCompatible(Func.minute, new TemplateFunction("minute", "datepart(minute,%s)"));
		registerCompatible(Func.second, new TemplateFunction("second", "datepart(second,%s)"));
		registerCompatible(Func.date, new TemplateFunction("date", "cast(%s as date)"));
		registerCompatible(Func.time, new TemplateFunction("time", "cast(%s as time)"));

		registerCompatible(Func.timestampadd, new EmuSQLServerTimestamp("timestampadd", "dateadd"));
		registerCompatible(Func.timestampdiff, new EmuSQLServerTimestamp("timestampdiff", "datediff"));
		registerCompatible(Func.datediff, new TemplateFunction("datediff", "datediff(day,%2$s,%1$s)"));

		registerCompatible(Func.adddate, new TemplateFunction("adddate", "dateadd(day,%2$s,%1$s)"));
		registerCompatible(Func.subdate, new TemplateFunction("subdate", "dateadd(day,-%2$s,%1$s)"));

		registerCompatible(Func.add_months, new TemplateFunction("add_months", "dateadd(month,%2$s,%1$s)"));

		registerCompatible(Func.mod, new TemplateFunction("mod", "(%1$s %% %2$s)"));
		registerCompatible(Func.decode, new EmuDecodeWithCase());
		registerCompatible(Func.concat, new VarArgsSQLFunction("", "+", "")); // 没有concat函数的，要改写为相加
		registerCompatible(Func.trim, new TemplateFunction("trim", "ltrim(rtrim(%s))")); // 没有concat函数的，要改写为相加
		registerCompatible(Func.lpad, new TemplateFunction("lpad", "(replicate(%3$s, %2$s-len(%1$s)) + %1$s)"));
		registerCompatible(Func.rpad, new TemplateFunction("rpad", "(%1$s + replicate(%3$s, %2$s-len(%1$s)))"));
		registerCompatible(Func.trunc, new EmuSQLServerTrunc());
		registerCompatible(Func.str, new TemplateFunction("str", "convert(varchar,%s,120)"));
	}

	@Override
	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("int identity(1,1)");
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	public int getPort() {
		return 1433;
	}

	public RDBMS getName() {
		return RDBMS.sqlserver;
	}

	/**
	 * SQL Server的表名列名大小写由服务端的排序规则决定。 这里根据一般性习惯统统转为小写处理
	 */
	@Override
	public String getObjectNameToUse(String name) {
		return StringUtils.lowerCase(name);
	}

	/**
	 * SQL Server的表名列名大小写由服务端的排序规则决定。 这里根据一般性习惯统统转为小写处理
	 * 
	 * 关键是看ResultSet获取有没有问题
	 */
	@Override
	public String getColumnNameToUse(String name) {
		return StringUtils.lowerCase(name);
	}

	@Override
	public String getDefaultSchema() {
		return "dbo";
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
	
	private final LimitHandler limit=generateLimitHander();

	@Override
	public final LimitHandler getLimitHandler() {
		return limit;
	}

	protected LimitHandler generateLimitHander() {
		if(UnionJudgement.isDruid()){
			return new SQL2000LimitHandler();
		}else{
			return new SQL2000LimitHandlerSlowImpl();
		}
	}

}
