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
import jef.database.OperateTarget;
import jef.database.dialect.type.AColumnMapping;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.EmuDateAddSubByTimesatmpadd;
import jef.database.query.function.EmuDatediffByTimestampdiff;
import jef.database.query.function.EmuDecodeWithDerbyCase;
import jef.database.query.function.EmuDerbyCast;
import jef.database.query.function.EmuDerbyUserFunction;
import jef.database.query.function.EmuJDBCTimestampFunction;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.query.function.VarArgsSQLFunction;
import jef.database.support.RDBMS;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.string.JefStringReader;

/**
 * Derby的dialet，用于derby的嵌入式模式
 * 
 * @author Administrator
 * 
 */
public class DerbyDialect extends AbstractDialect {
	
	public DerbyDialect() {
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(
				Feature.USER_AS_SCHEMA, 
				Feature.BATCH_GENERATED_KEY_ONLY_LAST, 
				Feature.ONE_COLUMN_IN_SINGLE_DDL, 
				Feature.SUPPORT_CONCAT,
				Feature.COLUMN_ALTERATION_SYNTAX,
				Feature.CASE_WITHOUT_SWITCH,
				Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD,
				Feature.UNION_WITH_BUCK
				));
		keywords.addAll(Arrays.asList("ASC", "ACCESS", "ADD", "ALTER", "DESC", "AUDIT", "CLUSTER", "COLUMN", "COMMENT", "COMPRESS", "CONNECT", "DATE", "DROP", "EXCLUSIVE", "FILE", "IDENTITY", "IDENTIFIED", "IMMEDIATE", "INCREMENT", "INDEX", "INITIAL", "INTERSECT", "LEVEL",
				"LOCK", "LONG", "MAXEXTENTS", "MINUS", "MODE", "NOAUDIT", "NOCOMPRESS", "NOWAIT", "NUMBER", "OFFLINE", "ONLINE", "PCTFREE", "PRIOR","KEY","ORDER"));

		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "ALTER");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "values 1");
		setProperty(DbProperty.SELECT_EXPRESSION, "values %s");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"\"");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "IDENTITY_VAL_LOCAL()");
		registerNative(Func.abs,"absval");
		registerNative(Func.mod);
		registerNative(Func.coalesce);
		registerNative(Func.locate);
		registerNative(Func.ceil, "ceiling");
		registerNative(Func.floor);
		registerNative(Func.round);
		registerNative(Func.nullif);
		registerNative(Func.length);
		registerAlias(Func.lengthb, "length");//FIXME length is a function to get unicode char length, not byte length.
		registerNative(Func.cast, new EmuDerbyCast());
		registerAlias(Func.nvl, "coalesce");
		registerCompatible(Func.str, new TemplateFunction("str", "rtrim(char(%s))"));
		
		registerNative(Scientific.cot);// 三角余切
		registerNative(Scientific.exp);
		registerNative(Scientific.ln,"log");
		registerNative(Scientific.log10);
		registerNative(Scientific.radians);
		registerNative(Scientific.degrees);
		registerNative(new NoArgSQLFunction("random"));
		registerAlias( Scientific.rand, "random");
		
		registerNative(Scientific.soundex);
		registerNative(new StandardSQLFunction("stddev"));
		registerNative(new StandardSQLFunction("variance"));// 标准方差
		registerNative(new StandardSQLFunction("nullif"));

		registerNative(new StandardSQLFunction("monthname"));
		registerNative(new StandardSQLFunction("quarter"));
		//date extract functions
		registerNative(Func.second);
		registerNative(Func.minute);
		registerNative(Func.hour);
		registerNative(Func.day);
		registerNative(Func.month);
		registerNative(Func.year);
		
		registerNative(new NoArgSQLFunction("current_time", false));
		registerAlias(Func.current_time,"current_time");
		registerNative(new NoArgSQLFunction("current_date", false));
		registerAlias(Func.current_date, "current_date");
		registerNative(new NoArgSQLFunction("current_timestamp", false));
		registerAlias(Func.now,"current_timestamp");
		registerAlias(Func.current_timestamp,"current_timestamp");
		registerAlias("sysdate","current_timestamp");
		
		registerNative(new StandardSQLFunction("dayname"));
		registerNative(new StandardSQLFunction("dayofyear"));
		registerNative(new StandardSQLFunction("days"));
		
		//derby timestamp Compatiable functions
		registerCompatible(Func.adddate, new EmuDateAddSubByTimesatmpadd(Func.adddate));
		registerCompatible(Func.subdate, new EmuDateAddSubByTimesatmpadd(Func.subdate));
		registerCompatible(Func.datediff,new EmuDatediffByTimestampdiff());
		registerCompatible(Func.timestampdiff, new EmuJDBCTimestampFunction(Func.timestampdiff,this));
		registerCompatible(Func.timestampadd, new EmuJDBCTimestampFunction(Func.timestampadd,this));
		registerCompatible(Func.replace, new EmuDerbyUserFunction("replace","replace"));
		registerCompatible(Func.lpad, new EmuDerbyUserFunction("lpad","USR_LEFTPAD"));
		registerCompatible(Func.rpad, new EmuDerbyUserFunction("rpad","USR_RIGHTPAD"));
		EmuDerbyUserFunction trunc=new EmuDerbyUserFunction("trunc","USR_TRUNC");
		trunc.setPadParam(2, LongValue.L0);
		registerCompatible(Func.trunc, trunc);
		registerCompatible(Func.translate, new EmuDerbyUserFunction("translate","USR_TRANSLATE"));	
		
		//cast functions
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
		registerNative(new StandardSQLFunction("integer"),"int");
		registerNative(new StandardSQLFunction("smallint"));

		registerNative(new StandardSQLFunction("digits"));
		registerNative(new StandardSQLFunction("chr"));
		
		//string functions
		registerNative(Func.upper,"ucase");
		registerNative(Func.lower,"lcase");
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Func.trim);
		registerNative(new StandardSQLFunction("substr"));
		registerAlias(Func.substring,"substr");
		
		
		
		registerCompatible(Func.concat,new VarArgsSQLFunction("", "||", "")); //Derby是没有concat函数的，要改写为相加
		registerCompatible(Func.decode,new EmuDecodeWithDerbyCase());
		
		registerCompatible(null,new TemplateFunction("power", "exp(%2$s * ln(%1$s))"),"power");//power(b, x) = exp(x * ln(b)) 
		registerCompatible(Func.add_months, new TemplateFunction("add_months","{fn timestampadd(SQL_TSI_MONTH,%2$s,%1$s)}"));
		
		// Derby是从10.7版本才开始支持boolean类型的
		// registerColumnType( Types.BLOB, "blob" );
		// determineDriverVersion();
		// if ( driverVersionMajor > 10 || ( driverVersionMajor == 10 &&
		// driverVersionMinor >= 7 ) ) {
		// registerColumnType( Types.BOOLEAN, "boolean" );
		// }
		
		typeNames.put(Types.BOOLEAN, "boolean", 0);
	}

	@Override
	public String getDefaultSchema() {
		return "APP";
	}

	public String getDriverClass(String url) {
		if(url!=null && url.startsWith("jdbc:derby://")){
			return "org.apache.derby.jdbc.ClientDriver";			
		}else{
			return "org.apache.derby.jdbc.EmbeddedDriver";
		}
	}


	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		if(StringUtils.isEmpty(host)){
			return super.generateUrl(host, port, pathOrName)+";create=true";
		}
		if(port<=0)port=1527;
		String result= "jdbc:derby://"+host+":"+port+"/"+pathOrName+";create=true";
		return result;
	}

	public RDBMS getName() {
		return RDBMS.derby;
	}

	@Override
	public String getObjectNameToUse(String name) {
		return StringUtils.upperCase(name);
	}

	@Override
	public String getColumnNameToUse(String name) {
		return StringUtils.upperCase(name);
	}
	
	@Override
	public String getColumnNameToUse(AColumnMapping name) {
		return name.upperColumnName();
	}
	
	@Override
	public void init(OperateTarget db) {
		super.init(db);
		try {
			ensureUserFunction(this.functions.get("trunc"), db);
		} catch (SQLException e) {
			LogUtil.exception("Initlize user function error.",e);
		}
	}

	/**
	 * {@inheritDoc}
	 * Like 
	 * <ul>
	 * 	<li>jdbc:derby://localhost:1527/databaseName;create=true</li>
	 *  <li>jdbc:derby:./db1;create=true</li>
	 *  <li>jdbc:derby://localhost:1527/ij_cmd_test_db</li>
	 *  </ul>
	 */
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ', ';');
		reader.consume("jdbc:derby:");
		if (reader.matchNext("//") == -1) {// 本地
			String path = reader.readToken(';');
			path.replace('\\', '/');
			String dbname = StringUtils.substringAfterLast(path, "/");
			connectInfo.setDbname(dbname);
		} else {// 网
			reader.omitChars('/');
			connectInfo.setHost(reader.readToken('/', ' '));
			connectInfo.setDbname(reader.readToken(';'));
		}
	}

	private final LimitHandler limit=new DerbyLimitHandler();
	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}
}
