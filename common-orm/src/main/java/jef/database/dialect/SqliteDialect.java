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

import java.io.File;
import java.sql.SQLException;

import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.CastFunction;
import jef.database.query.function.EmuDecodeWithCase;
import jef.database.query.function.EmuJDBCTimestampFunction;
import jef.database.query.function.EmuLRpadOnSqlite;
import jef.database.query.function.EmuTranslateByReplace;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.query.function.TransformFunction;
import jef.database.query.function.VarArgsSQLFunction;
import jef.database.support.RDBMS;
import jef.tools.collection.CollectionUtils;


/**
 * SQLite驱动方言，部分特性需要在驱动字符串后加上
 * ?date_string_format=yyyy-MM-dd HH:mm:ss
 * 
 * @author jiyi
 *
 */
public class SqliteDialect extends AbstractDialect {
	public SqliteDialect() {
		features = CollectionUtils.identityHashSet();
		features.add(Feature.SUPPORT_CONCAT);
		features.add(Feature.AUTOINCREMENT_MUSTBE_PK);
		features.add(Feature.TYPE_FORWARD_ONLY);
		features.add(Feature.BATCH_GENERATED_KEY_ONLY_LAST);

		features.add(Feature.NOT_SUPPORT_TRUNCATE);
		features.add(Feature.NOT_SUPPORT_FOREIGN_KEY);
		features.add(Feature.NOT_SUPPORT_USER_FUNCTION);
		features.add(Feature.NOT_SUPPORT_SET_BINARY);
		features.add(Feature.NOT_SUPPORT_INDEX_META);
		features.add(Feature.NOT_SUPPORT_KEYWORD_DEFAULT);
		features.add(Feature.NOT_SUPPORT_ALTER_DROP_COLUMN);
		features.add(Feature.ONE_COLUMN_IN_SINGLE_DDL);

		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY COLUMN");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "select 1");
		setProperty(DbProperty.SELECT_EXPRESSION, "select %s");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"\"");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "select last_insert_rowid()");

		registerCompatible(Func.concat, new VarArgsSQLFunction("", "||", ""));

		registerNative(Scientific.soundex);
		registerNative(Func.coalesce);
		registerNative(Func.locate);
		registerNative(Func.ceil);
		registerNative(Func.floor);
		registerNative(Func.round);

		registerNative(Func.length);
		registerNative(Func.lower);
		registerNative(Func.upper);
		registerNative(Func.trim);
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Func.nullif);
		registerNative(Func.replace);
		registerAlias("lcase", "lower");
		registerAlias("ucase", "upper");

		registerNative(new StandardSQLFunction("substr"));
		registerAlias(Func.substring, "substr");
		loadKeywords("mysql_keywords.properties");
		/*
		 * 1) 关于SQLite取当前时区的问题
		 * 是SQLITE查询当前时间返回的不是本地时间而是GMT时间，因此使用current_timestamp或者now()查出的时间不正确。
		 * 遂将current_timestamp函数重写为"datetime('now','localtime'),测试案例能通过。
		 * 然而很快发现，当创建表的时候， default值用函数表达式则建表出错。目前还没想好怎么解决，此处先不改。 
		 * 	2014-7-21
		 * 
		 * 2) 今天用Sqlite驱动3.8.11.2测试，发现原来的default不支持是误解，只要在函数两边加上括号()，就可以在default值中使用函数。
		 * 但是随后发现新的问题。由于使用datetime('now','localtime')会将日期格式化成 yyyy-MM-dd HH:mm:ss。
		 * 但默认的Sqlite日期格式为 yyyy-MM-dd HH:mm:ss.SSS。这造成该张表的数据无法读出（从ResultSet中get时获取抛出异常），
		 * 目前解决办法是——在jdbc url中增加   ?date_string_format=yyyy-MM-dd HH:mm:ss。
		 *  a)该方法仅对 3.8以后的版本有效
		 *  TODO b)Document it into FAQ.
		 * 2016-1-12
		 * 
		 */
		registerCompatible(Func.current_timestamp, new NoArgSQLFunction("datetime('now','localtime')", false), "now", "sysdate");
		registerAlias(Func.now, "current_timestamp");
		registerNative(Func.current_time, new NoArgSQLFunction("current_time", false));
		registerNative(Func.current_date, new NoArgSQLFunction("current_date", false));
		registerCompatible(Func.mod, new TemplateFunction("mod", "(%1$s %% %2$s)"));
		registerCompatible(Func.locate, new TransformFunction("locate", "instr", new int[] { 2, 1 }));// 用instr来模拟locate参数相反
		registerNative(new StandardSQLFunction("instr"));
		registerNative(new StandardSQLFunction("ifnull"));
		registerAlias(Func.nvl, "ifnull");
		registerNative(new StandardSQLFunction("hex"));
		registerNative(new StandardSQLFunction("like"));
		registerNative(new StandardSQLFunction("likelihood"));
		registerNative(new StandardSQLFunction("load_extension"));
		registerNative(new StandardSQLFunction("quote"));
		registerNative(new StandardSQLFunction("random"));
		registerNative(new StandardSQLFunction("randomblob"));
		registerNative(new StandardSQLFunction("zeroblob"));
		registerNative(new StandardSQLFunction("sqlite_compileoption_get"));
		registerNative(new StandardSQLFunction("sqlite_compileoption_used"));
		registerNative(new StandardSQLFunction("sqlite_source_id"));
		registerNative(new StandardSQLFunction("sqlite_version"));
		registerNative(new StandardSQLFunction("typeof"));
		registerNative(new StandardSQLFunction("unlikely"));
		registerNative(new StandardSQLFunction("unicode"));
		registerNative(new StandardSQLFunction("total"));
		registerNative(new StandardSQLFunction("total_changes"));
		registerNative(new StandardSQLFunction("datetime"));
		registerNative(Func.date);
		registerNative(Func.time);

		/**
		 * Functions define in extension-function.c
		 */
		registerNative(new StandardSQLFunction("padl"));
		registerNative(new StandardSQLFunction("padr"));
		registerNative(new StandardSQLFunction("padc"));
		registerNative(new StandardSQLFunction("strfilter"));
		registerNative(Func.cast, new CastFunction());

		registerCompatible(Func.str, new TemplateFunction("str", "cast(%s as char)"));
		registerCompatible(Func.lpad, new EmuLRpadOnSqlite(true));
		registerCompatible(Func.rpad, new EmuLRpadOnSqlite(false));

		registerCompatible(Func.year, new TemplateFunction("year", "strftime('%%Y',%s)"));
		registerCompatible(Func.month, new TemplateFunction("year", "strftime('%%m',%s)"));
		registerCompatible(Func.day, new TemplateFunction("year", "strftime('%%d',%s)"));
		registerCompatible(Func.hour, new TemplateFunction("year", "strftime('%%H',%s)"));
		registerCompatible(Func.minute, new TemplateFunction("year", "strftime('%%M',%s)"));
		registerCompatible(Func.second, new TemplateFunction("year", "strftime('%%S',%s)"));

		registerCompatible(Func.datediff, new TemplateFunction("datediff", "cast(julianday(%1$s) - julianday(%2$s) as integer)"));
		registerCompatible(Func.adddate, new TemplateFunction("adddate", "datetime(%1$s,'localtime','%2$s day')"));
		registerCompatible(Func.subdate, new TemplateFunction("subdate", "datetime(%1$s,'localtime','-%2$s day')"));
		registerCompatible(Func.add_months, new TemplateFunction("add_months", "datetime(%1$s,'localtime','%2$s month')"));

		registerCompatible(Func.timestampdiff, new EmuJDBCTimestampFunction(Func.timestampdiff, this));
		registerCompatible(Func.timestampadd, new EmuJDBCTimestampFunction(Func.timestampadd, this));
		registerCompatible(Func.trunc, new TemplateFunction("trunc", "cast(%s as integer)"));//FIXME: the minus value -10.5 will be floor to -11.

		registerCompatible(Func.lengthb, new TemplateFunction("rpad", "length(hex(%s))/2"));
		registerCompatible(Func.translate, new EmuTranslateByReplace());
		registerCompatible(Func.decode, new EmuDecodeWithCase());
	}

	@Override
	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("integer primary key autoincrement");
		if (flag) {
			if (!column.nullable)
				sb.append(" not null");
		}
		return sb.toString();
	}

	public String getDriverClass(String url) {
		return "org.sqlite.JDBC";
	}

	public int getPort() {
		return 0;
	}

	public RDBMS getName() {
		return RDBMS.sqlite;
	}

	public void parseDbInfo(ConnectInfo connectInfo) {
		String url = connectInfo.getUrl();
		if (!url.startsWith("jdbc:sqlite:")) {
			throw new IllegalArgumentException(url);
		}
		String dbpath = url.substring(12);
		File file = new File(dbpath);
		connectInfo.setDbname(file.getName());
		connectInfo.setHost("");
	}

	private final LimitHandler limit = new LimitOffsetLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter() {
		@Override
		public String extractConstraintName(SQLException sqle) {
			String message = sqle.getMessage();
			if (message.startsWith("[SQLITE_CONSTRAINT]")) {
				return message;
			} else if ("PRIMARY KEY must be unique".equals(message)) {
				return message;
			}
			return null;
		}
	};

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	@Override
	public String toDefaultString(Object defaultValue, int sqlType, int changeTo) {
		if (defaultValue instanceof DbFunction) {
			return "("+this.getFunction((DbFunction) defaultValue)+")";
		}
		return super.toDefaultString(defaultValue, sqlType, changeTo);
	}

}
