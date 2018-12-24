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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.derby.catalog.IndexDescriptor;
import org.apache.derby.catalog.ReferencedColumns;

import com.querydsl.sql.DerbyTemplates;
import com.querydsl.sql.SQLTemplates;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.dialect.handler.DerbyLimitHandler;
import jef.database.dialect.handler.LimitHandler;
import jef.database.jdbc.result.IResultSet;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.object.Column;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.ConstraintType;
import jef.database.meta.object.ForeignKeyAction;
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
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtils;
import jef.tools.string.JefStringReader;

/**
 * Derby的dialet，用于derby的嵌入式模式
 * 
 * @author Administrator
 * 
 */
public class DerbyDialect extends AbstractDialect {

	@Override
	protected void initFeatures() {
		features = CollectionUtils.identityHashSet();
		features.addAll(
				Arrays.asList(Feature.USER_AS_SCHEMA, Feature.BATCH_GENERATED_KEY_ONLY_LAST, 
						Feature.ONE_COLUMN_IN_SINGLE_DDL, 
						Feature.SUPPORT_CONCAT, 
						Feature.COLUMN_ALTERATION_SYNTAX, 
						Feature.CASE_WITHOUT_SWITCH, 
						Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD, 
						Feature.UNION_WITH_BUCK));
		setProperty(DbProperty.ADD_COLUMN, "ADD COLUMN");
		setProperty(DbProperty.MODIFY_COLUMN, "ALTER");
		setProperty(DbProperty.DROP_COLUMN, "DROP COLUMN");
		setProperty(DbProperty.CHECK_SQL, "values 1");
		setProperty(DbProperty.SELECT_EXPRESSION, "values %s");
		setProperty(DbProperty.WRAP_FOR_KEYWORD, "\"\"");
		setProperty(DbProperty.GET_IDENTITY_FUNCTION, "IDENTITY_VAL_LOCAL()");
	}

	@Override
	protected void initKeywods() {
		loadKeywords("derby_keywords.properties");
	}

	@Override
	protected void initTypes() {
		super.initTypes();
		// Derby是从10.7版本才开始支持boolean类型的
		// registerColumnType( Types.BLOB, "blob" );
		// determineDriverVersion();
		// if ( driverVersionMajor > 10 || ( driverVersionMajor == 10 &&
		// driverVersionMinor >= 7 ) ) {
		// registerColumnType( Types.BOOLEAN, "boolean" );
		// }
		typeNames.put(Types.BOOLEAN, "boolean", 0, "bool");
		// Derby中，Float是double的同义词
		typeNames.put(Types.FLOAT, "double", Types.DOUBLE);
	}

	@Override
	protected void initFunctions() {
		super.initFunctions();
		registerNative(Func.abs, "absval");
		registerNative(Func.mod);
		registerNative(Func.coalesce);
		registerNative(Func.locate);
		registerNative(Func.ceil, "ceiling");
		registerNative(Func.floor);
		registerNative(Func.round);
		registerNative(Func.nullif);
		registerNative(Func.length);
		registerAlias(Func.lengthb, "length");// FIXME length is a function to
												// get unicode char length, not
												// byte length.
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

		registerNative(new StandardSQLFunction("dayname"));
		registerNative(new StandardSQLFunction("dayofyear"));
		registerNative(new StandardSQLFunction("days"));

		// derby timestamp Compatiable functions
		registerCompatible(Func.adddate, new EmuDateAddSubByTimesatmpadd(Func.adddate));
		registerCompatible(Func.subdate, new EmuDateAddSubByTimesatmpadd(Func.subdate));
		registerCompatible(Func.datediff, new EmuDatediffByTimestampdiff());
		registerCompatible(Func.timestampdiff, new EmuJDBCTimestampFunction(Func.timestampdiff, this));
		registerCompatible(Func.timestampadd, new EmuJDBCTimestampFunction(Func.timestampadd, this));
		registerCompatible(Func.replace, new EmuDerbyUserFunction("replace", "replace"));
		registerCompatible(Func.lpad, new EmuDerbyUserFunction("lpad", "USR_LEFTPAD"));
		registerCompatible(Func.rpad, new EmuDerbyUserFunction("rpad", "USR_RIGHTPAD"));
		EmuDerbyUserFunction trunc = new EmuDerbyUserFunction("trunc", "USR_TRUNC");
		trunc.setPadParam(2, LongValue.L0);
		registerCompatible(Func.trunc, trunc);
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

		// string functions
		registerNative(Func.upper, "ucase");
		registerNative(Func.lower, "lcase");
		registerNative(Func.ltrim);
		registerNative(Func.rtrim);
		registerNative(Func.trim);
		registerNative(new StandardSQLFunction("substr"));
		registerAlias(Func.substring, "substr");

		registerCompatible(Func.concat, new VarArgsSQLFunction("", "||", "")); // Derby是没有concat函数的，要改写为相加
		registerCompatible(Func.decode, new EmuDecodeWithDerbyCase());

		registerCompatible(null, new TemplateFunction("power", "exp(%2$s * ln(%1$s))"), "power");// power(b,
																									// x)
																									// =
																									// exp(x
																									// *
																									// ln(b))
		registerCompatible(Func.add_months, new TemplateFunction("add_months", "{fn timestampadd(SQL_TSI_MONTH,%2$s,%1$s)}"));
	}

	@Override
	public String getDefaultSchema() {
		return "APP";
	}

	public String getDriverClass(String url) {
		if (url != null && url.startsWith("jdbc:derby://")) {
			return "org.apache.derby.jdbc.ClientDriver";
		} else {
			return "org.apache.derby.jdbc.EmbeddedDriver";
		}
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		if (StringUtils.isEmpty(host)) {
			return super.generateUrl(host, port, pathOrName) + ";create=true";
		}
		if (port <= 0)
			port = 1527;
		String result = "jdbc:derby://" + host + ":" + port + "/" + pathOrName + ";create=true";
		return result;
	}

	public RDBMS getName() {
		return RDBMS.derby;
	}

	@Override
	public void accept(DbMetaData db) {
		super.accept(db);
		try {
			ensureUserFunction(this.functions.get("trunc"), db);
		} catch (SQLException e) {
			LogUtil.exception("Initlize user function error.", e);
		}
	}

	/**
	 * {@inheritDoc} Like
	 * <ul>
	 * <li>jdbc:derby://localhost:1527/databaseName;create=true</li>
	 * <li>jdbc:derby:./db1;create=true</li>
	 * <li>jdbc:derby://localhost:1527/ij_cmd_test_db</li>
	 * </ul>
	 */
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ', ';');
		reader.consume("jdbc:derby:");
		if (reader.matchNext("//") == -1) {// 本地
			String path = reader.readToken(';');
			path.replace('\\', '/');
			String dbname = StringUtils.substringAfterLastIfExist(path, "/");
			connectInfo.setDbname(dbname);
		} else {// 网
			reader.omitChars('/');
			connectInfo.setHost(reader.readToken('/', ' '));
			connectInfo.setDbname(reader.readToken(';'));
		}
		reader.close();
	}

	private final LimitHandler limit = new DerbyLimitHandler();

	@Override
	public LimitHandler getLimitHandler() {
		return limit;
	}

	private final SQLTemplates queryDslDialect = new DerbyTemplates();

	@Override
	public SQLTemplates getQueryDslDialect() {
		return queryDslDialect;
	}

	@Override
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String tablename, String constraintName) throws SQLException {

		String sql = "select con.*, fk.deleterule, fk.updaterule, tab.tablename, rtab.tablename as ref_table_name, rtab.tableid as ref_table_id," + "           scm.schemaname, rscm.schemaname as ref_table_schema, conglo.descriptor, rconglo.descriptor as ref_descriptor,"
				+ "           ck.checkdefinition as check_clause,ck.referencedcolumns" + "      from sys.sysconstraints con " + " left join sys.sysforeignkeys fk" + "        on con.constraintid = fk.constraintid" + " left join sys.sysconstraints ref"
				+ "        on fk.keyconstraintid = ref.constraintid" + " left join sys.syschecks ck" + "        on con.constraintid = ck.constraintid" + " left join sys.systables tab" + "        on con.tableid = tab.tableid" + " left join sys.systables rtab"
				+ "        on ref.tableid = rtab.tableid" + " left join sys.sysschemas scm" + "        on con.schemaid = scm.schemaid" + " left join sys.sysschemas rscm" + "        on ref.schemaid = rscm.schemaid" + " left join sys.syskeys keys"
				+ "        on con.constraintid = keys.constraintid" + " left join sys.sysconglomerates conglo" + "        on keys.conglomerateid = conglo.conglomerateid" + "        or fk.conglomerateid = conglo.conglomerateid" + " left join sys.syskeys rkeys"
				+ "        on ref.constraintid = rkeys.constraintid" + " left join sys.sysconglomerates rconglo" + "        on rkeys.conglomerateid = rconglo.conglomerateid" + "     where scm.schemaname like ? and tab.tablename like ? and con.constraintname like ?";
		schema = StringUtils.isBlank(schema) ? "%" : schema;
		tablename = StringUtils.isBlank(tablename) ? "%" : tablename;
		constraintName = StringUtils.isBlank(constraintName) ? "%" : constraintName;

		List<Constraint> constraints = conn.selectBySql(sql, new AbstractResultSetTransformer<List<Constraint>>() {
			@Override
			public List<Constraint> transformer(IResultSet rs) throws SQLException {
				List<Constraint> constraints = new ArrayList<Constraint>();
				while (rs.next()) {
					Constraint c = new Constraint();
					c.setCatalog(null);
					c.setSchema(rs.getString("schemaname"));
					c.setName(rs.getString("constraintname"));
					// 'F' in derby means foreign key constraint which equals
					// 'R' in oracle
					c.setType("F".equals(rs.getString("type")) ? ConstraintType.R : ConstraintType.parseName(rs.getString("type")));
					c.setDeferrable(!"E".equals(rs.getString("state"))); // 'E'
																			// (not
																			// deferrable
																			// initially
																			// immediate)
					c.setInitiallyDeferred("e".equals(rs.getString("state"))); // 'e'
																				// (deferrable
																				// initially
																				// deferred)
					c.setTableCatalog(null);
					c.setTableSchema(rs.getString("schemaname"));
					c.setTableName(rs.getString("tablename"));
					c.setMatchType(null);
					c.setRefTableSchema(rs.getString("ref_table_schema"));
					c.setRefTableName(rs.getString("ref_table_name"));
					c.setUpdateRule(parseForeignKeyAction(rs.getString("updaterule")));
					c.setDeleteRule(parseForeignKeyAction(rs.getString("deleterule")));
					c.setEnabled(true);
					c.setCheckClause(rs.getString("check_clause"));

					if (ConstraintType.C == c.getType()) { // 检查约束的列信息需另取

						ReferencedColumns rcols = (ReferencedColumns) rs.getObject("referencedcolumns");
						if (rcols != null) {
							int[] columnIndexes = rcols.getReferencedColumnPositions();
							c.setColumns(transIntArrayToStringList(columnIndexes));
						}
					} else {
						IndexDescriptor ids = (IndexDescriptor) rs.getObject("descriptor"); // 列坐标信息
						if (ids != null) {
							int[] columnIndexes = ids.baseColumnPositions();
							c.setColumns(transIntArrayToStringList(columnIndexes));
						}
						IndexDescriptor rids = (IndexDescriptor) rs.getObject("ref_descriptor"); // 参照列坐标信息
						if (rids != null) {
							int[] refColumnIndexes = rids.baseColumnPositions();
							c.setRefColumns(transIntArrayToStringList(refColumnIndexes));
						}
					}
					constraints.add(c);
				}

				return constraints;
			}

		}, Arrays.asList(schema, tablename, constraintName), false);

		// 取得列信息
		Map<String, List<Column>> tableMap = new HashMap<>();
		for (Constraint c : constraints) {

			if (c.getColumns() != null && c.getColumns().size() > 0) {
				String fullTableName = c.getTableSchema().concat(".").concat(c.getTableName()); // 带schema的表名
				List<Column> columnList;
				if (tableMap.containsKey(fullTableName)) {
					columnList = tableMap.get(fullTableName);
				} else {
					columnList = conn.getColumns(fullTableName);
					tableMap.put(fullTableName, columnList);
				}
				List<String> columnIndexes = c.getColumns();
				for (int i = 0; i < columnIndexes.size(); i++) {
					// 重设约束对象里的列信息
					columnIndexes.set(i, columnList.get(Integer.parseInt(columnIndexes.get(i)) - 1).getColumnName());
				}
			}

			if (c.getRefColumns() != null && c.getRefColumns().size() > 0) {
				String fullTableName = c.getRefTableSchema().concat(".").concat(c.getRefTableName()); // 带schema的参照表名
				List<Column> columnList;
				if (tableMap.containsKey(fullTableName)) {
					columnList = tableMap.get(fullTableName);
				} else {
					columnList = conn.getColumns(fullTableName);
					tableMap.put(fullTableName, columnList);
				}
				List<String> columnIndexes = c.getRefColumns();
				for (int i = 0; i < columnIndexes.size(); i++) {
					columnIndexes.set(i, columnList.get(Integer.parseInt(columnIndexes.get(i)) - 1).getColumnName());
				}
			}
		}

		return constraints;
	}

	private List<String> transIntArrayToStringList(int[] array) {

		List<String> result = new ArrayList<>();
		if (array.length == 0) {
			return result;
		}

		for (int i = 0; i < array.length; i++) {
			result.add(String.valueOf(array[i]));
		}

		return result;
	}

	/**
	 * Derby的外键更新策略名称转换
	 * 
	 * @param action
	 * @return
	 */
	private ForeignKeyAction parseForeignKeyAction(String action) {

		if (StringUtils.isBlank(action)) {
			return null;
		}

		switch (action) {

		case "R":
			return ForeignKeyAction.NO_ACTION;

		case "S":
			return ForeignKeyAction.RESTRICT;

		case "U":
			return ForeignKeyAction.SET_NULL;

		case "C":
			return ForeignKeyAction.CASCADE;

		default:
			return ForeignKeyAction.NO_ACTION;
		}
	}
}
