package jef.database.jsqlparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.database.DbUtils;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.operators.arithmetic.Concat;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.parser.JpqlParser;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.tools.IOUtils;
import jef.tools.reflect.CloneUtils;

import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import com.alibaba.druid.sql.dialect.postgresql.parser.PGSQLStatementParser;
import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerStatementParser;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;

public class ComplexSqlParseTest extends org.junit.Assert {
	@Test
	@Ignore
	public void testParseAndPrint() throws IOException, ParseException {
		String sql = IOUtils.asString(new File("d:/aaa.sql"), "US-ASCII");
		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);
		System.out.println(st);
	}

	@Test
	public void main1() throws ParseException {
		String source = "select to_char(t.acct_id) name1,to_char(t.name) name2,to_char(t.account_status) name3,\nto_char( t.org_id) name4,to_char(t.so_nbr) name5,to_char(t.create_date) name6 from ca_account t where 1=1  or t.create_date =:operateTime or "
				+ "\n:selectType<sql> = :selectValue";
		Statement re = jef.database.DbUtils.parseStatement(source);
	}

	@Test
	public void main2() throws ParseException {
		Select ex = DbUtils.parseSelect("select * from D where not 1=2");
		System.out.println(ex);
	}

	/**
	 * 测试解析器能否解析srart with 的查询
	 * 
	 * @throws SQLException
	 * @throws ParseException
	 */
	@Test
	public void testConnect() throws SQLException, ParseException {
		String s = "select * from ad.ca_account_rel where relationship_type = 1 and sysdate between valid_date and expire_date start with acct_id = ?1 connect by prior rel_acct_id = acct_id";
		String result = "select * from ad.ca_account_rel where relationship_type = 1 AND sysdate BETWEEN valid_date AND expire_date START WITH acct_id = ?1 CONNECT BY PRIOR rel_acct_id = acct_id";
		Select select = DbUtils.parseSelect(s);
		System.out.println(select);
		System.out.println(result);
		assertEquals(result, select.toString());
		System.out.println(select);
	}

	@Test
	public void asdasdas() throws ParseException {
		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement("  select :column from root where id in (:id<int>)  and id2=:id2 and name like :name and id3=:id3 and id4=:id4 order by :orderBy");
		System.out.println(st);
	}

	@Test
	public void testMySQLDate() throws ParseException {
		// String sql="SELECT INTERVAL 1 DAY + '2008-12-31' from dual";
		String sql = "select value from dual  where end_time < date_sub(now(), interval 5 day)";
		{
			JpqlParser parser = new JpqlParser(new StringReader(sql));
			parser.Select();
		}
		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			JpqlParser parser = new JpqlParser(new StringReader(sql));
			Select select = parser.Select();
			System.out.println((System.nanoTime() - start) / 1000);
		}
	}

	@Test(expected = ParseException.class)
	public void parseJpqlParams() throws ParseException {
		String sql = "select * from t where id=:top";
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		parser.Statement();

		sql = "update t set name=:desc";
		parser = new JpqlParser(new StringReader(sql));
		parser.Statement();

	}

	@Test
	public void parseFunctionOnSQLServer() throws ParseException {
//		
		String s = "select @@LANGUAGE from dual";
		{
			StSqlParser parser = new StSqlParser(new StringReader(s));
			Select select = parser.Select();
			System.out.println(select);
		}
		{
			SQLServerStatementParser parser = new SQLServerStatementParser(s);
			StringBuilder out = new StringBuilder();
			List<SQLStatement> statementList = parser.parseStatementList();
			SQLServerOutputVisitor visitor = new SQLServerOutputVisitor(out);
			statementList.get(0).accept(visitor);
			System.out.println(out);
			
		}
	}
	
	
	@Test
	public void parseMatch() throws ParseException {
		String s = "select * from team where team.search_column @@ to_tsquery('new & york & yankees')";
		{
			SQLStatementParser parser = new PGSQLStatementParser(s);
			StringBuilder out = new StringBuilder();
			List<SQLStatement> statementList = parser.parseStatementList();
//			SQLASTOutputVisitor visitor = new PGSQLOutputVisitor(out);
//			statementList.get(0).accept(visitor);
			System.out.println(out);
			
		}
	}

	/**
	 * 解析右侧的SQL表达式
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testMySelf() throws ParseException {
		String sql = "select * from tablea where 1=1 and :aaa<sql>";
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		Select select = parser.Select();
		System.out.println(select);

	}

	@Test
	public void testPgInterval() throws ParseException {
		String sql = "select extract(day FROM current_timestamp) from dual";
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		Select select = parser.Select();
		System.out.println(select);

	}

	@Test
	public void testParse2() throws ParseException {
		String sql = "select :column<sql> from root where id1 in (:id<int>)  and the_id=:id2 and name like :name and id3=:id3 and id4=:id4 order by :orderBy<sql>";
		System.out.println(sql);
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		Select select = parser.Select();
		System.out.println(select);
		// deparseOrderBy(orderBy);
	}

	@Test
	public void testSelectaItem() throws ParseException {
		String sql = "trunc(dob) as pname,aa";
		StSqlParser parser = new StSqlParser(new StringReader(sql));
		List select = parser.SelectItemsList();
		System.out.println(select);
	}

	@Test
	public void aaa() throws ParseException {
		// String sql =
		// "select t.rowid from (select :col from person_table where age>:ss1<string> and name=?1<int> and nvl(aa,translate(fastbean,'abc123','ccccc'))||schoolId||'tomo'||schoolId  =:name2 order by :orderBy) t";

		String sql = "select t.* from rm_camera_info t where t.treenodeindexcode=? and t.typecode=?";
		Select st = DbUtils.parseNativeSelect(sql);
		System.out.println(sql);
		st.accept(new VisitorAdapter() {
			@Override
			public void visit(Concat concat) {
				List<Expression> el = new ArrayList<Expression>();
				recursion(concat, el);
				Function func = new Function();
				func.setName("concat");
				func.setParameters(new ExpressionList(el));
				super.visit(concat);
			}

			private void recursion(Concat concat, List<Expression> el) {
				Expression left = concat.getLeftExpression();
				if (left instanceof Concat) {
					recursion((Concat) left, el);
				} else {
					el.add(left);
				}
				Expression right = concat.getRightExpression();
				el.add(right);
			}
		});

		System.out.println(st.toString());
	}

	@Test
	public void ccc() throws ParseException {
		String sql = "select * from sd.SO_STEP_RELATION start with step_id=2179 and type='dashArrow' connect by prior step_id=depend_step_id \n";
		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);

		System.out.println(st.toString());
	}

	@Test
	public void aaa2() throws ParseException {
		String sql = "select * from sys_resource rs start with rs.resource_id in (:value<int>) connect by PRIOR rs.resource_id = rs.parent_id";
		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);
		// st.accept(new VisitorAdapter() {
		// @Override
		// public void visit(JpqlParameter param) {
		// System.out.println(param);
		// }
		//
		// @Override
		// public void visit(Column tableColumn) {
		// // TODO Auto-generated method stub
		// super.visit(tableColumn);
		// System.out.println(tableColumn);
		// }
		//
		// @Override
		// public void visit(OrderByElement orderBy) {
		// System.out.println("orderBy:----" + orderBy);
		// }
		//
		// });

		System.out.println(st.toString());
	}

	@Test
	public void aaax() throws ParseException {
		String s = "select decode(ID,1,'壹',2,'贰',3,'叁',4,'肆',5,'伍',6,'陆',7,'柒',8,'捌',9,'玖',str(ID)) as C from foo t1";
		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(s);

		jef.database.jsqlparser.visitor.Statement st1 = DbUtils.parseNativeSelect(s);
	}

	@Test
	@Ignore
	// TODO: 无法支持Oracle分析函数的解析
	public void aaa3() throws ParseException {
		String sql = "select a.* from (select l.vm_Id,row_number() over (partition by l.vm_id order by l.update_time desc) time from dbm2.rdc_vm_state_record l where l.vm_id = 1185) a where time < 2";
		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);
		System.out.println(st.toString());
	}

	@Test
	// 支持Oracle的DBlink
	public void aaa4() throws ParseException {
		String sql = "SELECT A.M_ROW$$, A.TYPE_ID, A.BIZ_TYPE, A.SP_ID, A.SP_SERVICE_ID, A.STATUS, A.VALID_DATE, A.EXPIRE_DATE, A.NOTES, A.RATE, A.SERV_TYPE, A.OPERATOR_NAME, A.BILL_FLAG, A.IN_PROP, A.OUT_PROP, A.COUNT, A.DCONFIRM_FLAG, A.DEDUCT_CLUE, A.QUERY_TD, A.EXT1, A.EXT2\n"
				+ " FROM DSMP.DSMP_BIZSCOPE_DEF@sh_dev_link A WHERE SUBSTR(A.SP_ID, 1, 1) BETWEEN 5 AND 9   AND A.EXPIRE_DATE > SYSDATE   AND NOT EXISTS (SELECT B.*          FROM bd.RS_ISMG_RATE B         WHERE (B.VALID_DATE = A.VALID_DATE OR B.VALID_DATE < SYSDATE) AND B.EXPIRE_DATE > SYSDATE AND B.SP_CODE = A.SP_ID AND B.OPERATOR_CODE = A.SP_SERVICE_ID AND B.RATE = A.RATE AND (B.SP_CODE LIKE '909%' OR B.SP_CODE LIKE '809%' OR B.SP_CODE LIKE '509%'))";

		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);
		System.out.println(st.toString());
	}

	// 支持Oracle多表更新操作
	// TODO 依赖特定环境，故先ignore
	@Ignore
	@Test
	public void aaa5() throws ParseException {
		String sql = "update (select a.start_time astarttime,b.job_ins_start_time bstarttime,a.status astatus,b.job_ins_status bstatus"
				+ "from sd.so_job_ins_result a, test10.rdc_job_ins b where a.status not in (2, 4, -1, -2) and a.job_ins_id = b.job_ins_id  and a.job_ins_sequence = b.job_ins_sequence) " + "set astarttime = bstarttime, astatus = bstatus";

		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);
		System.out.println(st.toString());
	}

	@Test
	public void aaa6() throws ParseException {
		String sql = "delete t1 from dbm2.dbm_warn_log as t1 where not exists \n (select 1 from dbm2.dbm_warn_dealed as t2 where t2.log_id = t1.log_id)";

		jef.database.jsqlparser.visitor.Statement st = DbUtils.parseStatement(sql);
		System.out.println(st.toString());
	}

	@Test
	public void aaa7() throws ParseException, SQLException {
		String sql = "DELETE FROM DBM2.dbm_warn_log WHERE warn_time < subdate(now(), :DAYS)   AND NOT EXISTS (select 1 from DBM2.dbm_warn_dealed where log_id = DBM2.dbm_warn_log.log_id)";
		Statement st = DbUtils.parseStatement(sql);
		System.out.println(st);

		// DbClient db = new DbClient();
		// NativeQuery<?> q = db.createNativeQuery(sql);
		// q.setParameter("DAYS", "aa");
		// System.out.println(q);
	}

	@Test
	public void testOrder() throws ParseException {
		String sql = " where t.a1='aa' order by t.a2";
		StSqlParser parser = new StSqlParser(new StringReader(sql));
		Expression exp = parser.WhereClause();
		System.out.println(exp);
	}

	@Test
	public void testWhereMod() throws ParseException {
		String sql = "where (t.age / 10)=?1";
		JpqlParser parser = new JpqlParser(new StringReader(sql));
		Expression exp = parser.WhereClause();
	}

	/**
	 * DBM在开发使用中发现的BUG，当时这句语句解析会出错。目前已修正
	 * 
	 * @throws SQLException
	 * @throws ParseException
	 */
	@Test
	public void testDistinctAndStartWith() throws SQLException, ParseException {
		String strSql = "select DISTINCT * " + "	  from xg.sys_region rs" + "	 start with rs.region_code in (:privIDs<Long>)" + "	connect by PRIOR rs.priv_id = rs.parent_id";

		jef.database.jsqlparser.statement.select.Select select = DbUtils.parseSelect(strSql);
		select.accept(new VisitorAdapter() {
			// 计算绑定变量
			@Override
			public void visit(JpqlParameter param) {
				System.out.println(param);
			}
		});
	}

	@Test
	public void testExpression() throws SQLException, ParseException {
		String sql = "select int(year(current_date)/100)+1 as aa from dual";
		jef.database.jsqlparser.statement.select.Select select = DbUtils.parseSelect(sql);
		System.out.println(select);
	}

	@Test
	public void testComplexSql() throws SQLException, ParseException, IOException {
		doParseFile("complex-sqls.txt", 0);
	}

	@Test
	public void testComplexSqlDruidOracle() throws SQLException, ParseException, IOException {
		doParseFile("complex-sqls-oracle.txt", 2);
	}

	@Test
	public void testComplexSqlDruidMySQL() throws SQLException, ParseException, IOException {
		doParseFile("complex-sqls-mysql.txt", 3);
	}

	@Test
	public void testComplexE_Sql() throws SQLException, ParseException, IOException {
		doParseFile("complex-e-sqls.txt", 1);
	}

	private void doParseFile(String filename, int eSql) throws ParseException, IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = IOUtils.getReader(this.getClass().getResource(filename), "UTF-8");
		String line;
		boolean comment = false;
		int total = 0;
		while ((line = reader.readLine()) != null) {
			if (comment) {
				if (line.endsWith("*/")) {
					comment = false;
				}
				System.out.println(line);
				continue;
			}
			if (line.startsWith("/*")) {
				comment = true;
				System.out.println(line);
				continue;
			}
			if (line.length() == 0 || line.startsWith("--"))
				continue;
			if (sb.length() > 0)
				sb.append('\n');
			sb.append(line);
			if (endsWith(line, ';')) {
				sb.setLength(sb.length() - 1);
				String sql = sb.toString();
				sb.setLength(0);
				if (StringUtils.isNotBlank(sql)) {
					parseTest(sql, eSql);
					total++;
				}
			}
		}
		if (sb.length() > 0) {
			parseTest(sb.toString(), eSql);
			total++;
		}
		System.out.println("测试完成，共计解析了" + total + "句SQL语句");
	}

	@Test
	public void testComplexSqlPerformances() throws SQLException, ParseException, IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			BufferedReader reader = IOUtils.getReader(this.getClass().getResource("complex-sqls.txt"), "UTF-8");
			String line;
			boolean comment = false;
			List<Long> cost = new ArrayList<Long>();// 记录每句SQL的解析时间
			while ((line = reader.readLine()) != null) {
				if (comment) {
					if (line.endsWith("*/")) {
						comment = false;
					}
					continue;
				}
				if (line.startsWith("/*")) {
					comment = true;
					continue;
				}
				if (line.length() == 0 || line.startsWith("--"))
					continue;
				if (sb.length() > 0)
					sb.append('\n');
				sb.append(line);
				if (endsWith(line, ';')) {
					sb.setLength(sb.length() - 1);
					String sql = sb.toString();
					sb.setLength(0);
					if (StringUtils.isNotBlank(sql)) {
						cost.add(countParseStSql(sql));
					}
				}
			}
			if (sb.length() > 0) {
				cost.add(countParseStSql(sb.toString()));
			}

			long total = 0;
			for (long l : cost) {
				total += l;
			}
			System.out.println("测试完成，共计解析了" + cost.size() + "句SQL语句，总耗时" + total / 1000 + "us，各句耗时分别为——");
			for (long l : cost) {
				System.out.println(l / 1000 + "us");
			}
			sb.setLength(0);
		}

	}

	@Test
	public void testComplexSqlPerformances2() throws SQLException, ParseException, IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {

			BufferedReader reader = IOUtils.getReader(this.getClass().getResource("complex-e-sqls.txt"), "UTF-8");
			String line;
			boolean comment = false;
			List<Long> cost = new ArrayList<Long>();// 记录每句SQL的解析时间
			while ((line = reader.readLine()) != null) {
				if (comment) {
					if (line.endsWith("*/")) {
						comment = false;
					}
					continue;
				}
				if (line.startsWith("/*")) {
					comment = true;
					continue;
				}
				if (line.length() == 0 || line.startsWith("--"))
					continue;
				if (sb.length() > 0)
					sb.append('\n');
				sb.append(line);
				if (endsWith(line, ';')) {
					sb.setLength(sb.length() - 1);
					String sql = sb.toString();
					sb.setLength(0);
					try {
						if (StringUtils.isNotBlank(sql)) {
							cost.add(countParseJpql(sql));
						}
					} catch (Exception e) {
						System.out.println(sql);
					}

				}
			}
			if (sb.length() > 0) {
				cost.add(countParseStSql(sb.toString()));
			}

			long total = 0;
			for (long l : cost) {
				total += l;
			}
			System.out.println("测试完成，共计解析了" + cost.size() + "句SQL语句，总耗时" + total / 1000 + "us，各句耗时分别为——");
			for (long l : cost) {
				System.out.println(l / 1000 + "us");
			}
		}
	}

	// 0 StSQL 1 Jpql 2 Druid Oracle 3 Druid MySQL
	private void parseTest(String sql, int type) throws ParseException {
		System.out.println("===================== [RAW]  ==================");
		System.out.println(sql);
		System.out.println("-------------------- [PARSE] ------------------");
		Object st;
		switch (type) {
		case 0: {
			StSqlParser parser = new StSqlParser(new StringReader(sql));
			st = parser.Statement();
			System.out.println(st);
			break;
		}
		case 1: {
			JpqlParser parser = new JpqlParser(new StringReader(sql));
			st = parser.Statement();
			System.out.println(st);
			break;
		}
		case 2: {
			OracleStatementParser parser = new OracleStatementParser(sql);
			List<SQLStatement> statementList = parser.parseStatementList();
			StringBuilder out = new StringBuilder();
			OracleOutputVisitor visitor = new OracleOutputVisitor(out);
			statementList.get(0).accept(visitor);
			System.out.println(out);
			break;
		}
		case 3: {
			MySqlStatementParser parser = new MySqlStatementParser(sql);
			List<SQLStatement> statementList = parser.parseStatementList();
			st = statementList.get(0);
			StringBuilder out = new StringBuilder();
			MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
			statementList.get(0).accept(visitor);
			System.out.println(out);
			break;
		}
		default:
			throw new IllegalArgumentException();
		}

	}

	/**
	 * 返回用标准解析器解析的耗时（纳秒）
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	private long countParseStSql(String sql) throws ParseException {
		long start = System.nanoTime();
		Statement st = new StSqlParser(new StringReader(sql)).Statement();
		long cost = System.nanoTime() - start;
		start = System.nanoTime();
		Statement st1 = (Statement) CloneUtils.clone(st);
		assertEquals(st.toString(), st1.toString());
		System.out.println("拷贝耗时" + (System.nanoTime() - start) / 1000 + "us");
		return cost;
	}

	/**
	 * 返回用JPQL解析器解析的耗时（纳秒）
	 * 
	 * @param sql
	 * @return
	 * @throws ParseException
	 */
	private long countParseJpql(String sql) throws ParseException {
		long start = System.nanoTime();
		new JpqlParser(new StringReader(sql)).Statement();
		return System.nanoTime() - start;
	}

	private boolean endsWith(String line, char key) {
		if (line.length() == 0)
			return false;
		int len = line.length();
		for (int i = 1; i <= len; i++) {
			char c = line.charAt(len - i);
			if (c == ' ')
				continue;
			return c == key;
		}
		return false;
	}

}
