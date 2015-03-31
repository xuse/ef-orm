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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.DbCfg;
import jef.database.DebugUtil;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.datasource.DataSourceInfo;
import jef.database.datasource.SimpleDataSource;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.ColumnType.Varchar;
import jef.database.dialect.type.AColumnMapping;
import jef.database.exception.JDBCExceptionHelper;
import jef.database.exception.TemplatedViolatedConstraintNameExtracter;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.Scientific;
import jef.database.query.function.EmuCoalesce_Nvl;
import jef.database.query.function.EmuOracleCast;
import jef.database.query.function.EmuOracleDateAdd;
import jef.database.query.function.EmuOracleDateSub;
import jef.database.query.function.EmuOracleExtract;
import jef.database.query.function.EmuOracleTime;
import jef.database.query.function.EmuOracleTimeStampDiff;
import jef.database.query.function.EmuOracleTimestampAdd;
import jef.database.query.function.EmuOracleToDate;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;
import jef.database.query.function.TransformFunction;
import jef.database.query.function.VarArgsSQLFunction;
import jef.database.support.RDBMS;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.populator.ResultSetExtractor;
import jef.jre5support.ProcessUtil;
import jef.tools.DateFormats;
import jef.tools.DateUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.string.JefStringReader;

/**
 * 修改列名Oracle：lter table bbb rename column nnnnn to hh int;
 */
public class OracleDialect extends AbstractDialect {
	public OracleDialect() {
		features = CollectionUtil.identityHashSet();
		features.addAll(Arrays.asList(
				Feature.AUTOINCREMENT_NEED_SEQUENCE, 
				Feature.USER_AS_SCHEMA, 
				Feature.REMARK_META_FETCH, 
				Feature.BRUKETS_FOR_ALTER_TABLE, 
				Feature.SUPPORT_CONCAT,
				Feature.SUPPORT_CONNECT_BY,
				Feature.DROP_CASCADE,
				Feature.SUPPORT_SEQUENCE,
				Feature.EMPTY_CHAR_IS_NULL,
				Feature.COLUMN_DEF_ALLOW_NULL));

		super.loadKeywords("oracle_keywords.properties");
		if (JefConfiguration.getBoolean(DbCfg.DB_ENABLE_ROWID, false)) {
			features.add(Feature.SELECT_ROW_NUM);
		}
		setProperty(DbProperty.ADD_COLUMN, "ADD");
		setProperty(DbProperty.MODIFY_COLUMN, "MODIFY");
		setProperty(DbProperty.DROP_COLUMN, "DROP");
		setProperty(DbProperty.CHECK_SQL, "SELECT 1 FROM DUAL");
		setProperty(DbProperty.SEQUENCE_FETCH, "SELECT %s.NEXTVAL FROM DUAL");
		setProperty(DbProperty.SELECT_EXPRESSION, "SELECT %s FROM DUAL");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"\"");
		setProperty(DbProperty.OTHER_VERSION_SQL, "select 'USER_LANGUAGE',userenv('language') from dual");
		
		registerNative(Scientific.sinh);
		registerNative(Scientific.cosh);
		registerNative(Scientific.tanh);
		registerNative(Scientific.exp);
		registerNative(Scientific.ln);
		registerNative(new StandardSQLFunction("log"));//Oracle can assign a log param.
		registerNative(new StandardSQLFunction("stddev"));
		
		
		registerNative(new StandardSQLFunction("variance"));

		registerNative(Func.round);
		registerNative(Func.trunc);
		registerNative(Func.ceil);
		registerNative(Func.floor);

		registerNative(new StandardSQLFunction("chr"));
		registerNative(new StandardSQLFunction("initcap"));
		registerNative(Func.lower);
		registerNative(Func.upper);
		registerNative(Func.trim);
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Scientific.soundex);
		registerNative(new StandardSQLFunction("ascii"));
		registerNative(Func.add_months);
		registerAlias("lcase", "lower");
		registerAlias("ucase", "upper");
		

		// Oracle的to_char有两种含义，具体如何转化需要根据传入参数来确定，单参数下是类型转换，双参数下是日期格式化
		registerNative(new StandardSQLFunction("to_char"));
		registerNative(new StandardSQLFunction("to_number"));
		registerNative(new StandardSQLFunction("to_date"));
		registerAlias(Func.date,"trunc");
		registerCompatible(Func.time,new EmuOracleTime());
		
		registerNative(new NoArgSQLFunction("sysdate", false));
		registerAlias(Func.current_timestamp, "sysdate");
		registerAlias(Func.now,"sysdate");
		

		
		registerNative(new NoArgSQLFunction("systimestamp", false));
		registerCompatible(Func.current_time, new TemplateFunction("current_time", "(systimestamp-trunc(sysdate))"));
		
		registerNative(new StandardSQLFunction("last_day"));
		registerNative(new NoArgSQLFunction("uid", false));
		registerNative(new NoArgSQLFunction("user", false));

		registerNative(new NoArgSQLFunction("rowid", false));
		registerNative(new NoArgSQLFunction("rownum", false));
		registerNative(Func.length);
		registerNative(Func.lengthb);
		
		
		registerNative(new StandardSQLFunction("instr"));
		registerNative(new StandardSQLFunction("instrb"));
		registerNative(Func.lpad);
		registerNative(Func.replace);
		registerNative(Func.rpad);
		registerNative(new StandardSQLFunction("substr"));
		registerAlias(Func.substring, "substr");
		registerNative(new StandardSQLFunction("substrb"));
		registerNative(Func.translate);
		

		// Multi-param numeric dialect functions...
		registerNative(new StandardSQLFunction("atan2"));
		
		registerNative(Func.mod);
		registerNative(Func.nvl);
		registerNative(Func.nullif);
		registerNative(Func.decode);
		registerNative(new StandardSQLFunction("nvl2"));
		
		registerNative(Scientific.power);
		
		// Multi-param date dialect functions...
		registerNative(new StandardSQLFunction("add_months"));
		registerNative(new StandardSQLFunction("months_between"));
		registerNative(new StandardSQLFunction("next_day"));
		registerNative(new StandardSQLFunction("rollup"));
		registerNative(new StandardSQLFunction("cube"));
//		Oracle的GROUP BY语句除了最基本的语法外，还支持ROLLUP和CUBE语句。如果是ROLLUP(A, B, C)的话，首先会对(A、B、C)进行GROUP BY，然后对(A、B)进行GROUP BY，然后是(A)进行GROUP BY，
//		最后对全表进行GROUP BY操作。如果是GROUP BY CUBE(A, B, C)，则首先会对(A、B、C)进行GROUP BY，然后依次是(A、B)，(A、C)，(A)，(B、C)，(B)，(C)，最后对全表进行GROUP BY操作。
		
// 用GROUP BY GROUPING SETS来代替GROUP BY CUBE。你可以应用来指定你感兴趣的总数组合。因为它不必计算它不需要集合（也不会产生太多结果），所以对SQL引擎来说更为高效。 
//其格式为： 
//		GROUP BY GROUPING SETS ((list), (list) ... ) 

		
		registerCompatible(Func.current_date, new NoArgSQLFunction("trunc(sysdate)", false));
		
		registerCompatible(Func.concat, new VarArgsSQLFunction("", "||", ""));
		registerCompatible(Func.coalesce, new EmuCoalesce_Nvl());
		registerCompatible(Func.locate, new TransformFunction("locate", "instr",new int[]{2,1}));// 用instr来模拟locate参数相反
		registerCompatible(Func.year, new EmuOracleExtract("year",false));
		registerCompatible(Func.month, new EmuOracleExtract("month",false));
		registerCompatible(Func.day, new EmuOracleExtract("day",false));
		registerCompatible(Func.hour, new EmuOracleExtract("hour",true));
		registerCompatible(Func.minute, new EmuOracleExtract("minute",true));
		registerCompatible(Func.second, new EmuOracleExtract("second",true));
		
		registerCompatible(null,EmuOracleToDate.getInstance(),"timestamp");
		registerCompatible(Func.adddate, new EmuOracleDateAdd());
		registerCompatible(Func.subdate, new EmuOracleDateSub());
		registerCompatible(Func.timestampdiff, new EmuOracleTimeStampDiff());
		registerCompatible(Func.timestampadd, new EmuOracleTimestampAdd());
		registerCompatible(Func.cast, new EmuOracleCast());
		
		registerCompatible(Func.datediff, new TemplateFunction("datediff","trunc(%1$s-%2$s)"));
		registerAlias(Func.str, "to_char");
		
		typeNames.put(Types.VARCHAR, 4000,"varchar2($l)", 0);
		typeNames.put(Types.VARCHAR, 1024*1024*1024*4,"clob", Types.CLOB);
		
		typeNames.put(Types.FLOAT, "number($p,$s)", 0);
		typeNames.put(Types.DOUBLE, "number($p,$s)", 0);
		typeNames.put(Types.NUMERIC, "number($p,$s)", 0);
		typeNames.put(Types.TINYINT, "number($p)", 0);
		typeNames.put(Types.SMALLINT, "number($p)", 0);
		typeNames.put(Types.INTEGER, "number($p)", 0);
		typeNames.put(Types.BIGINT, "number($p)", 0);
		
		typeNames.put(Types.TIMESTAMP, "date", 0);
		typeNames.put(Types.TIME, "date", 0);
	}

	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append("number(" + column.precision + ")");
		if (flag) {
			if (column.nullable){
				sb.append(" null");
			}else{
				sb.append(" not null");
			}
		}
		return sb.toString();
	}

	public CachedRowSet newCacheRowSetInstance() throws SQLException {
		return new oracle.jdbc.rowset.OracleCachedRowSet();
	}

	public ColumnType getProprtMetaFromDbType(jef.database.meta.Column column) {
		if ("NUMBER".equals(column.getDataType())) {
			if (column.getDecimalDigit() > 0) {// 小数
				return new ColumnType.Double(column.getColumnSize(), column.getDecimalDigit());
			} else {// 整数
				return new ColumnType.Int(column.getColumnSize());
			}
		} else if ("NVARCHAR2".equals(column.getDataType())) {
			if ("GUID".equals(column.getColumnDef())) {
				return new ColumnType.GUID();
			} else {
				return new Varchar(column.getColumnSize());
			}
		} else if ("DATE".equals(column.getDataType())) {
			return new ColumnType.Date();
		} else if (column.getDataType().toUpperCase().startsWith("TIMESTAMP")) { // Oracle
																					// 有
																					// TimeStamp(6)这种类型
			return new ColumnType.TimeStamp();
		} else {
			return super.getProprtMetaFromDbType(column);
		}
	}

	public String getDriverClass(String url) {
		return "oracle.jdbc.driver.OracleDriver";
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		// jdbc:oracle:thin:@192.168.253.249:1521:ora10
		sb.append(getName()).append(":thin:");
		sb.append("@").append(host).append(":").append(port <= 0 ? 1521 : port);
		sb.append(":").append(pathOrName);

		String url = sb.toString();
		if(ORMConfig.getInstance().isDebugMode()){
			LogUtil.show(url);
		}
		return url;
	}

	public RDBMS getName() {
		return RDBMS.oracle;
	}

	@Override
	public String getObjectNameToUse(String name) {
		if(name==null)return null;
		if(name.charAt(1)=='"')return name;
		return name.toUpperCase();
	}

	@Override
	public String getColumnNameToUse(String name) {
		if(name==null)return null;
		if(name.charAt(1)=='"')return name;
		return name.toUpperCase();
	}
	@Override
	public String getColumnNameToUse(AColumnMapping name) {
		return name.upperColumnName();
	}
	/**
	 * 由于暂不考虑支持Oracle TIMESTAMP到毫秒这个特性，因此在查询时需要对TIMESTAMP进行truncate处理，
	 * 以避免因多了几个毫秒而导致查不到数据的问题。
	 */
	@Override
	public java.sql.Timestamp toTimestampSqlParam(Date timestamp) {
		Calendar gval = Calendar.getInstance();
		gval.setTime(timestamp);
		int mills=gval.get(Calendar.MILLISECOND);
		return new java.sql.Timestamp(timestamp.getTime()-mills);
	}

	/**
	 * 判断SEQUENCE是否存在时，若schema为小写会存在误判情况（将存在判断为不存在）， <br>
	 * 为了避免大小写引起的问题，一律转成大写。
	 */
	@Override
	public String getSchema(String schema) {
		return StringUtils.upperCase(schema);
	}

	@Override
	public int calcSequenceStep(OperateTarget conn, String schema, String seqName, int defaultValue) {
		if (defaultValue > 0)
			return defaultValue;
		if (StringUtils.isBlank(seqName)) {
			LogUtil.warn("Return defaultValue because sequence name is blank on calculating SequenceStep.");
			return defaultValue;
		}
		
		String sql = "select increment_by from all_sequences where sequence_owner like ? and sequence_name = ?";
		
		schema=StringUtils.isBlank(schema) ? "%" : schema.toUpperCase();
		seqName=seqName.toUpperCase();
		try {
			int value=conn.getMetaData().selectBySql(sql, ResultSetExtractor.GET_FIRST_INT, 1, Arrays.asList(schema,seqName));
			LogUtil.info("Tring to access ALL_SEQUENCE to get the increament of sequence [" + seqName+"], the step="+value);
			return value;
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			LogUtil.exception(e);
		}
		return defaultValue;
	}

	@Override
	public boolean isIOError(SQLException se) {
		int code = se.getErrorCode();
		if (code == 12007 || code == 17410 || code == 17008 || code == 17002 || code == 28 || (code > 1087 && code < 1093)) {
			return true;
		} else if (se.getCause() != null && "NetException".equals(se.getCause().getClass().getSimpleName())) {
			return true;
		} else {
			// 为了跟踪所有Oracle出现网络异常时的错误码，将错误码和异常信息打印出来。
//			LogUtil.info("Oracle non-io Err:" + se.getErrorCode() + ":" + se.getMessage());
			return false;
		}
	}

	public Connection createConnection(DataSource ds) throws SQLException {
		// 设置连接属性
		Properties prop = new Properties();
		prop.put("v$session.program", "JefOrm@".concat(ProcessUtil.getHostname()));
		prop.put("v$session.process", String.valueOf(ProcessUtil.getPid()));
		prop.put(oracle.net.ns.SQLnetDef.TCP_CONNTIMEOUT_STR, "4000");
		prop.put(oracle.net.ns.SQLnetDef.TCP_READTIMEOUT_STR, "30000");
		if (ds instanceof SimpleDataSource) {
			return ((SimpleDataSource) ds).getConnectionFromDriver(prop);
		} else {
			return ds.getConnection();
		}
	}

	@Override
	public void processConnectProperties(DataSourceInfo dsw) {
		dsw.putProperty("v$session.program", "JefOrm@".concat(ProcessUtil.getHostname()));
		dsw.putProperty("v$session.process", String.valueOf(ProcessUtil.getPid()));
		dsw.putProperty(oracle.net.ns.SQLnetDef.TCP_CONNTIMEOUT_STR, "4000");
		dsw.putProperty(oracle.net.ns.SQLnetDef.TCP_READTIMEOUT_STR, "30000");
	}

	static final char[] CHAR_OF_END = new char[] { ':' };
	static final char[] CHAR_OF_END2 = new char[] { ':', ' ' };

	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consume("jdbc:", ' ');
		reader.consume("oracle:", ' ');
		if (reader.matchNext("oci:", ' ') > -1) {// 按OCI处理
			reader.consume("oci:@", ' ');
			reader.omitChars('/');
			String token = reader.readToken(CHAR_OF_END2);
			if (reader.eof()) {
				connectInfo.setDbname(token);
				connectInfo.setHost(token);
			} else {
				String port = reader.readToken(CHAR_OF_END);
				if (port.length() > 0) {
					token = token + ":" + port;
				}
				connectInfo.setHost(token);
				String dbname = reader.readToken(CHAR_OF_END2);
				connectInfo.setDbname(dbname);
			}
		} else {// 按thin处理
			reader.consume("thin:@", ' ');// RAC连接串
			if (reader.matchNextIgnoreCase("(DESCRIPTION") > -1) {//
				reader.omitAfterKeyIgnoreCase("(HOST=", ' ');
				String host = reader.readToken('"', ')');
				reader.omitAfterKeyIgnoreCase("(SERVICE_NAME=", ' ');
				String dbName = reader.readToken('"', ')');
				connectInfo.setHost(host.trim());
				connectInfo.setDbname(dbName.trim());
			} else {// 通常串
				reader.omitChars('/');
				String host = reader.readToken(CHAR_OF_END);
				String port = reader.readToken(CHAR_OF_END);
				String dbname = reader.readToken(CHAR_OF_END2);
				if (port.length() > 0)
					host = host + ":" + port;
				connectInfo.setHost(host);
				connectInfo.setDbname(dbname);
			}
		}
	}

	@Override
	public void processIntervalExpression(BinaryExpression parent, Interval interval) {
		if(interval.isPostgreMode()){
			interval.toMySqlMode();
		}
		String unit=interval.getUnit().toLowerCase();
		interval.toMySqlMode();
		Expression value=interval.getValue();
		if("day".equals(unit)){
			replace(parent,interval,value);
		}else if("hour".equals(unit)){
			value=new Division(value,new LongValue(24));
			value=new Parenthesis(value);
			replace(parent,interval,value);
		}else if("minute".equals(unit)){
			value=new Division(value,new LongValue(1440));
			value=new Parenthesis(value);
			replace(parent,interval,value);
		}else if("second".equals(unit)){
			value=new Division(value,new LongValue(86400));
			value=new Parenthesis(value);
			replace(parent,interval,value);
		}else if("month".equals(unit)){
			if(parent.getLeftExpression()==interval){
				parent.swap();//交换到右边
			}
			Function func=new Function("add_months",parent.getLeftExpression(),interval.getValue());
			parent.rewrite=func;
		}else if("year".equals(unit)){
			if(parent.getLeftExpression()==interval){
				parent.swap();//交换到右边
			}
			Expression right=new Multiplication(interval.getValue(),new LongValue(12));
			Function func=new Function("add_months",parent.getLeftExpression(),right);
			parent.rewrite=func;
		}else{
			throw new UnsupportedOperationException("The Oracle Dialect can't handle datetime unit ["+unit+"] for now.");
		}
	}

	private void replace(BinaryExpression parent, Interval interval, Expression value) {
		if(parent.getLeftExpression()==interval){
			parent.setLeftExpression(value);
		}else{
			parent.setRightExpression(value);
		}
	}
	@Override
	public String getSqlDateExpression(Date value) {
		return "to_date(" + QUOT + DateUtils.formatDate(value) + QUOT + ",'YYYY-MM-DD')";
	}

	@Override
	public String getSqlTimeExpression(Date value) {
		return "to_date(" + QUOT + DateFormats.TIME_ONLY.get().format(value) + QUOT + ",'HH24:MI:SS')";
	}

	@Override
	public String getSqlTimestampExpression(Date value) {
		return "to_date(" + QUOT + DateUtils.formatDateTime(value) + QUOT + ",'YYYY-MM-dd HH24:MI:SS')";
	}

	public void addKeyword(String... keys) {
		for(String s:keys){
			this.keywords.add(s.toUpperCase());
		}
	}

	@Override
	public void toExtremeInsert(InsertSqlClause sql) {
//		alter table xxx nologging
		sql.setInsert("insert /*+ APPEND */ into ");
	}
	
	private LimitHandler limit=new OracleLimitHander();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}
	private static ViolatedConstraintNameExtracter EXTRACTER_8 = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			int errorCode = JDBCExceptionHelper.extractErrorCode(sqle);
			if ( errorCode == 1 || errorCode == 2291 || errorCode == 2292 ) {
				return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
			}
			else if ( errorCode == 1400 ) {
				// simple nullability constraint
				return null;
			}
			else {
				return null;
			}
		}
	};
	

	private static ViolatedConstraintNameExtracter EXTRACTER_9 = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			int errorCode = JDBCExceptionHelper.extractErrorCode(sqle);
			if ( errorCode == 1 || errorCode == 2291 || errorCode == 2292 ) {
				return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
			}
			else if ( errorCode == 1400 ) {
				// simple nullability constraint
				return null;
			}
			else {
				return null;
			}
		}

	};

}
