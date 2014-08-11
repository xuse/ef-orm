package jef.database.jsqlparser.test.select;

import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.DoubleValue;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.TimeValue;
import jef.database.jsqlparser.expression.TimestampValue;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.jsqlparser.expression.operators.relational.EqualsTo;
import jef.database.jsqlparser.expression.operators.relational.GreaterThan;
import jef.database.jsqlparser.expression.operators.relational.InExpression;
import jef.database.jsqlparser.expression.operators.relational.LikeExpression;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.schema.Column;
import jef.database.jsqlparser.schema.Table;
import jef.database.jsqlparser.statement.Statement;
import jef.database.jsqlparser.statement.select.AllTableColumns;
import jef.database.jsqlparser.statement.select.Join;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.util.deparser.ExpressionDeParser;
import jef.database.jsqlparser.util.deparser.SelectDeParser;
import jef.database.jsqlparser.util.deparser.StatementDeParser;
import junit.framework.TestCase;

import org.junit.Test;

public class SelectTest extends TestCase {
	public SelectTest(String arg0) {
		super(arg0);
	}

	public void testLimit() throws ParseException {
		String statement = "select * from mytable where mytable.col = 9 LIMIT 3, ?1";

		Select select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(3, ((PlainSelect) select.getSelectBody()).getLimit().getOffset());
		assertTrue(((PlainSelect) select.getSelectBody()).getLimit().getOffsetJdbcParameter()==null);
		assertTrue(((PlainSelect) select.getSelectBody()).getLimit().getRowCountJdbcParameter()!=null);
		assertFalse(((PlainSelect) select.getSelectBody()).getLimit().isLimitAll());

		// toString uses standard syntax
		statement = "select * from mytable where mytable.col = 9 LIMIT ?1 OFFSET 3";
		assertEquals(statement, ""+select);

		statement = "select * from mytable where mytable.col = 9 LIMIT ?1";
		select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(0, ((PlainSelect) select.getSelectBody()).getLimit().getRowCount());
 		assertFalse(((PlainSelect) select.getSelectBody()).getLimit().isLimitAll());
		assertEquals(statement, select.toString());

		statement =
			"(select * from mytable WHERE mytable.col = 9 LIMIT 10 OFFSET ?1) UNION "
				+ "(select * from mytable2 WHERE mytable2.col = 9 LIMIT 10, ?1) LIMIT 3, 4";
		select = (Select) jef.database.DbUtils.parseStatement(statement);
		Union union = (Union) select.getSelectBody();
		assertEquals(3, union.getLimit().getOffset());
		assertEquals(4, union.getLimit().getRowCount());

		// toString uses standard syntax
		statement =
			"(select * from mytable where mytable.col = 9 LIMIT 10 OFFSET ?1)\n UNION "
				+ "(select * from mytable2 where mytable2.col = 9 LIMIT ?1 OFFSET 10) LIMIT 4 OFFSET 3";
		assertEquals(statement, ""+select);

		statement ="(select * from t1 where t1.c1 = 9 LIMIT 4 OFFSET 1)\n UNION ALL"+
			" (select * from t1 where t1.c2 = 9 LIMIT 3 OFFSET 1)\n UNION ALL"+
			" (select * from t1 where t1.c1 = 9 LIMIT 10 OFFSET 1) LIMIT 4 OFFSET 3";
		select = (Select) jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+select);

	
	}
	
	@Test
	public void testLimit2() throws ParseException {
		String statement = "select * from mytable where mytable.col = 9 LIMIT ?1 , :name";

		Select select = (Select) jef.database.DbUtils.parseStatement(statement);
		System.out.println(select);
		
		statement = "select * from mytable where mytable.col = 9 LIMIT 1";
		select = (Select) jef.database.DbUtils.parseStatement(statement);
		System.out.println(select);
	}

	public void testTop() throws ParseException {
		String statement = "select TOP 3 * from mytable where mytable.col = 9";

		Select select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(3, ((PlainSelect) select.getSelectBody()).getTop().getRowCount());
		
		statement = "select top 5 foo from bar";
		select = (Select) jef.database.DbUtils.parseStatement(statement);
		assertEquals(5, ((PlainSelect) select.getSelectBody()).getTop().getRowCount());
		

	}

	
	public void testSelectItems() throws ParseException {
		String statement =
			"select myid AS MYID,mycol,tab.*,schema.tab.*,mytab.mycol2,mytab.mycol,mytab.* from mytable where mytable.col = 9";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();

		assertEquals("MYID", ((SelectExpressionItem) plainSelect.getSelectItems().get(0)).getAlias());
		assertEquals("mycol", ((Column) ((SelectExpressionItem) plainSelect.getSelectItems().get(1)).getExpression()).getColumnName());
		assertEquals("tab", ((AllTableColumns) plainSelect.getSelectItems().get(2)).getTable().getName());
		assertEquals("schema", ((AllTableColumns) plainSelect.getSelectItems().get(3)).getTable().getSchemaName());
		assertEquals("schema.tab", ((AllTableColumns) plainSelect.getSelectItems().get(3)).getTable().getWholeTableName());
		assertEquals(
			"mytab.mycol2",
			((Column) ((SelectExpressionItem) plainSelect.getSelectItems().get(4)).getExpression()).getWholeColumnName());
		assertEquals(
			"mytab.mycol",
			((Column) ((SelectExpressionItem) plainSelect.getSelectItems().get(5)).getExpression()).getWholeColumnName());
		assertEquals("mytab", ((AllTableColumns) plainSelect.getSelectItems().get(6)).getTable().getWholeTableName());
		assertEquals(statement, ""+plainSelect);

		statement = "select myid AS MYID,(select MAX(ID) AS myid2 from mytable2) AS myalias from mytable where mytable.col = 9";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("myalias", ((SelectExpressionItem) plainSelect.getSelectItems().get(1)).getAlias());
		assertEquals(statement, ""+plainSelect);

		statement = "select (myid + myid2) AS MYID from mytable where mytable.col = 9";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("MYID", ((SelectExpressionItem) plainSelect.getSelectItems().get(0)).getAlias());
		assertEquals(statement, ""+plainSelect);
	}

//	@Ignore
//	public void testUnion() throws ParseException {
//		String statement =
//			"select * from mytable where mytable.col = 9 UNION "
//				+ "select * from mytable3 where mytable3.col = ?1 UNION "
//				+ "select * from mytable2 LIMIT 3,4";
//		
//		Union union = (Union) ((Select) jef.database.DbUtils.parseStatement(statement))).getSelectBody();
//		assertEquals(3, union.getPlainSelects().size());
//		assertEquals("mytable", ((Table) ((PlainSelect) union.getPlainSelects().get(0)).getFromItem()).getName());
//		assertEquals("mytable3", ((Table) ((PlainSelect) union.getPlainSelects().get(1)).getFromItem()).getName());
//		assertEquals("mytable2", ((Table) ((PlainSelect) union.getPlainSelects().get(2)).getFromItem()).getName());
//		assertEquals(3, ((PlainSelect) union.getPlainSelects().get(2)).getLimit().getOffset());
//		
//		//use brakets for toString
//		//use standard limit syntax
//		String statementToString =
//			"(select * from mytable where mytable.col = 9) UNION "
//				+ "(select * from mytable3 where mytable3.col = ?1) UNION "
//				+ "(select * from mytable2 LIMIT 4 OFFSET 3)";
//		assertEquals(statementToString, ""+union);
//	}

	public void testDistinct() throws ParseException {
		String statement = "select DISTINCT ON (myid) myid,mycol from mytable WHERE mytable.col = 9";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(
			"myid",
			((Column) ((SelectExpressionItem) plainSelect.getDistinct().getOnSelectItems().get(0)).getExpression()).getColumnName());
		assertEquals("mycol", ((Column) ((SelectExpressionItem) plainSelect.getSelectItems().get(1)).getExpression()).getColumnName());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());
	}

	public void testFrom() throws ParseException {
		String statement =
			"select * from mytable as mytable0, mytable1 alias_tab1, mytable2 as alias_tab2, (select * from mytable3) AS mytable4 where mytable.col = 9";
		String statementToString =
			"select * from mytable mytable0, mytable1 alias_tab1, mytable2 alias_tab2, (select * from mytable3) mytable4 where mytable.col = 9";

		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(3, plainSelect.getJoins().size());
		assertEquals("mytable0", ((Table) plainSelect.getFromItem()).getAlias());
		assertEquals("alias_tab1", ((Join) plainSelect.getJoins().get(0)).getRightItem().getAlias());
		assertEquals("alias_tab2", ((Join) plainSelect.getJoins().get(1)).getRightItem().getAlias());
		assertEquals("mytable4", ((Join) plainSelect.getJoins().get(2)).getRightItem().getAlias());
		assertEquals(statementToString.toUpperCase(), plainSelect.toString().toUpperCase());
		
	}

	public void testJoin() throws ParseException {
		String statement = "select * from tab1 LEFT outer JOIN tab2 ON tab1.id = tab2.id";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(1, plainSelect.getJoins().size());
		assertEquals("tab2", ((Table) ((Join) plainSelect.getJoins().get(0)).getRightItem()).getWholeTableName());
		assertEquals(
			"tab1.id",
			((Column) ((EqualsTo) ((Join) plainSelect.getJoins().get(0)).getOnExpression()).getLeftExpression()).getWholeColumnName());
		assertTrue(((Join) plainSelect.getJoins().get(0)).isOuter());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement = "select * from tab1 LEFT outer JOIN tab2 ON tab1.id = tab2.id INNER JOIN tab3";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(2, plainSelect.getJoins().size());
		assertEquals("tab3", ((Table) ((Join) plainSelect.getJoins().get(1)).getRightItem()).getWholeTableName());
		assertFalse(((Join) plainSelect.getJoins().get(1)).isOuter());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement = "select * from tab1 LEFT outer JOIN tab2 ON tab1.id = tab2.id JOIN tab3";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(2, plainSelect.getJoins().size());
		assertEquals("tab3", ((Table) ((Join) plainSelect.getJoins().get(1)).getRightItem()).getWholeTableName());
		assertFalse(((Join) plainSelect.getJoins().get(1)).isOuter());
		
		// implicit INNER 
		statement = "select * from tab1 LEFT outer JOIN tab2 ON tab1.id = tab2.id INNER JOIN tab3";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement= "select * from TA2 LEFT outer JOIN O USING (col1,col2) where D.OasSD = 'asdf' And (kj >= 4 OR l < 'sdf')";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());
		
		statement = "select * from tab1 INNER JOIN tab2 USING (id,id2)";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(1, plainSelect.getJoins().size());
		assertEquals("tab2", ((Table) ((Join) plainSelect.getJoins().get(0)).getRightItem()).getWholeTableName());
		assertFalse(((Join) plainSelect.getJoins().get(0)).isOuter());
		assertEquals(2, ((Join) plainSelect.getJoins().get(0)).getUsingColumns().size());
		assertEquals("id2", ((Column) ((Join) plainSelect.getJoins().get(0)).getUsingColumns().get(1)).getWholeColumnName());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement = "select * from tab1 RIGHT OUTER JOIN tab2 USING (id,id2)";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement = "select * from foo f LEFT INNER JOIN (bar b RIGHT OUTER JOIN baz z ON f.id = z.id) ON f.id = b.id";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

	}

	public void testFunctions() throws ParseException {
		String statement = "select MAX(id) as max from mytable WHERE mytable.col = 9";
		PlainSelect select = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("max", ((SelectExpressionItem) select.getSelectItems().get(0)).getAlias());
		assertEquals(statement.toUpperCase(), select.toString().toUpperCase());

		statement = "select MAX(id),AVG(pro) as myavg from mytable WHERE mytable.col = 9 GROUP BY pro";
		select = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("myavg", ((SelectExpressionItem) select.getSelectItems().get(1)).getAlias());
		assertEquals(statement.toUpperCase(), select.toString().toUpperCase());

		statement = "select MAX(a,b,c),COUNT(*),D from tab1 GROUP BY D";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		Function fun = (Function) ((SelectExpressionItem) plainSelect.getSelectItems().get(0)).getExpression();
		assertEquals("MAX", fun.getName());
		assertEquals("b", ((Column)fun.getParameters().getExpressions().get(1)).getWholeColumnName());
		assertTrue(((Function) ((SelectExpressionItem) plainSelect.getSelectItems().get(1)).getExpression()).isAllColumns());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement = "select {fn MAX(a,b,c)},COUNT(*),D from tab1 GROUP BY D";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		fun = (Function) ((SelectExpressionItem) plainSelect.getSelectItems().get(0)).getExpression();
		assertTrue(fun.isEscaped());
		assertEquals("MAX", fun.getName());
		assertEquals("b", ((Column)fun.getParameters().getExpressions().get(1)).getWholeColumnName());
		assertTrue(((Function) ((SelectExpressionItem) plainSelect.getSelectItems().get(1)).getExpression()).isAllColumns());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

		statement = "select ab.MAX(a,b,c),cd.COUNT(*),D from tab1 GROUP BY D";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		fun = (Function) ((SelectExpressionItem) plainSelect.getSelectItems().get(0)).getExpression();
		assertEquals("ab.MAX", fun.getName());
		assertEquals("b", ((Column)fun.getParameters().getExpressions().get(1)).getWholeColumnName());
		fun = (Function) ((SelectExpressionItem) plainSelect.getSelectItems().get(1)).getExpression();
		assertEquals("cd.COUNT", fun.getName());
		assertTrue(fun.isAllColumns());
		assertEquals(statement.toUpperCase(), plainSelect.toString().toUpperCase());

	}

	public void testWhere() throws ParseException {

		String statement = "select * from tab1 where ";
		String whereToString = "(a + b + c / d + e * f) * (a / b * (a + b)) > ?1";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement + whereToString)).getSelectBody();
		assertTrue(plainSelect.getWhere() instanceof GreaterThan);
		assertTrue(((GreaterThan) plainSelect.getWhere()).getLeftExpression() instanceof Multiplication);
		assertEquals(statement+whereToString, ""+plainSelect);

		ExpressionDeParser expressionDeParser = new ExpressionDeParser();
		StringBuilder StringBuilder = new StringBuilder();
		expressionDeParser.setBuffer(StringBuilder);
		plainSelect.getWhere().accept(expressionDeParser);
		assertEquals(whereToString, StringBuilder.toString());

		whereToString = "(7 * s + 9 / 3) NOT BETWEEN 3 AND ?1";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement + whereToString)).getSelectBody();

		StringBuilder = new StringBuilder();
		expressionDeParser.setBuffer(StringBuilder);
		plainSelect.getWhere().accept(expressionDeParser);

		assertEquals(whereToString, StringBuilder.toString());
		assertEquals(statement+whereToString, ""+plainSelect);

		whereToString = "a / b NOT IN (?1,'s''adf',234.2)";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement + whereToString)).getSelectBody();

		StringBuilder = new StringBuilder();
		expressionDeParser.setBuffer(StringBuilder);
		plainSelect.getWhere().accept(expressionDeParser);

		assertEquals(whereToString, StringBuilder.toString());
		assertEquals(statement+whereToString, ""+plainSelect);

		whereToString = "NOT 0 = 0";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement + whereToString)).getSelectBody();

		String where = " NOT (0 = 0)";
		whereToString = "NOT (0 = 0)";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement + whereToString)).getSelectBody();

		StringBuilder = new StringBuilder();
		expressionDeParser.setBuffer(StringBuilder);
		plainSelect.getWhere().accept(expressionDeParser);

		assertEquals(where, StringBuilder.toString());
		assertEquals(statement+whereToString, ""+plainSelect);
	}

	public void testGroupBy() throws ParseException {
		String statement = "select * from tab1 where a > 34 group by tab1.b";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(1, plainSelect.getGroupByColumnReferences().size());
		assertEquals("tab1.b", ((Column) plainSelect.getGroupByColumnReferences().get(0)).getWholeColumnName());
		assertEquals(statement, ""+plainSelect);

		statement = "select * from tab1 where a > 34 group by 2,3";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(2, plainSelect.getGroupByColumnReferences().size());
		assertEquals(2, ((LongValue) plainSelect.getGroupByColumnReferences().get(0)).getValue());
		assertEquals(3, ((LongValue) plainSelect.getGroupByColumnReferences().get(1)).getValue());
		assertEquals(statement, ""+plainSelect);
	}

	public void testHaving() throws ParseException {
		String statement = "select MAX(tab1.b) from tab1 where a > 34 group by tab1.b having MAX(tab1.b) > 56";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertTrue(plainSelect.getHaving() instanceof GreaterThan);
		assertEquals(statement, ""+plainSelect);

		statement = "select MAX(tab1.b) from tab1 where a > 34 having MAX(tab1.b) IN (56,32,3,?1)";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertTrue(plainSelect.getHaving() instanceof InExpression);
		assertEquals(statement, ""+plainSelect);
	}

	public void testExists() throws ParseException {
		String statement = "select * from tab1 where";
		String where = " EXISTS (select * from tab2)";
		statement += where;
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		PlainSelect plainSelect =	(PlainSelect) ((Select) parsed).getSelectBody();
		ExpressionDeParser expressionDeParser = new ExpressionDeParser();
		StringBuilder StringBuilder = new StringBuilder();
		expressionDeParser.setBuffer(StringBuilder);
		SelectDeParser deParser = new SelectDeParser(expressionDeParser, StringBuilder);
		expressionDeParser.setSelectVisitor(deParser);
		plainSelect.getWhere().accept(expressionDeParser);
		assertEquals(where, StringBuilder.toString());

	}

	public void testOrderBy() throws ParseException {
		//TODO: should there be a DESC marker in the OrderByElement class?
		String statement = "select * from tab1 where a > 34 group by tab1.b order by tab1.a DESC,tab1.b ASC";
		String statementToString = "select * from tab1 where a > 34 group by tab1.b order by tab1.a DESC,tab1.b";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(2, plainSelect.getOrderBy().getOrderByElements().size());
		assertEquals("tab1.a", ((Column) ((OrderByElement) plainSelect.getOrderBy().getOrderByElements().get(0)).getExpression()).getWholeColumnName());
		assertEquals("b", ((Column) ((OrderByElement) plainSelect.getOrderBy().getOrderByElements().get(1)).getExpression()).getColumnName());
		assertTrue(((OrderByElement) plainSelect.getOrderBy().getOrderByElements().get(1)).isAsc());
		assertFalse(((OrderByElement) plainSelect.getOrderBy().getOrderByElements().get(0)).isAsc());
		assertEquals(statementToString, ""+plainSelect);
		
		ExpressionDeParser expressionDeParser = new ExpressionDeParser();
		StringBuilder StringBuilder = new StringBuilder();
		SelectDeParser deParser = new SelectDeParser(expressionDeParser, StringBuilder);
		expressionDeParser.setSelectVisitor(deParser);
		expressionDeParser.setBuffer(StringBuilder);
		plainSelect.accept(deParser);
		assertEquals(statement, StringBuilder.toString());
		
		statement = "select * from tab1 where a > 34 group by tab1.b order by tab1.a,2";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals(2, plainSelect.getOrderBy().getOrderByElements().size());
		assertEquals("a", ((Column) ((OrderByElement) plainSelect.getOrderBy().getOrderByElements().get(0)).getExpression()).getColumnName());
		assertEquals(2, ((LongValue) ((OrderByElement) plainSelect.getOrderBy().getOrderByElements().get(1)).getExpression()).getValue());
		assertEquals(statement, ""+plainSelect);
	}

	public void testTimestamp() throws ParseException {
		String statement = "select * from tab1 where a > {ts '2004-04-30 04:05:34.56'}";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("2004-04-30 04:05:34.56", ((TimestampValue)((GreaterThan) plainSelect.getWhere()).getRightExpression()).getValue().toString());
		assertEquals(statement, ""+plainSelect);
	}

	public void testTime() throws ParseException {
		String statement = "select * from tab1 where a > {t '04:05:34'}";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("04:05:34", (((TimeValue)((GreaterThan) plainSelect.getWhere()).getRightExpression()).getValue()).toString());
		assertEquals(statement, ""+plainSelect);
	}

	public void testCase() throws ParseException {
		String statement = "select a,CASE b WHEN 1 THEN 2 END from tab1";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		statement = "select a,(CASE WHEN (a > 2) THEN 3 END) AS b from tab1";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		statement = "select a,(CASE WHEN a > 2 THEN 3 ELSE 4 END) AS b from tab1";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		
		statement = "select a,(CASE b WHEN 1 THEN 2 WHEN 3 THEN 4 ELSE 5 END) from tab1";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);
		
		statement = "select a,(CASE " +
				"WHEN b > 1 THEN 'BBB' " +
				"WHEN a = 3 THEN 'AAA' " +
				"END) from tab1";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);
		
		statement = "select a,(CASE " +
		"WHEN b > 1 THEN 'BBB' " +
		"WHEN a = 3 THEN 'AAA' " +
		"END) from tab1 " +
		"where c = (CASE " +
		"WHEN d <> 3 THEN 5 " +
		"ELSE 10 " +
		"END)";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		statement = "select a,CASE a " +
		"WHEN 'b' THEN 'BBB' " +
		"WHEN 'a' THEN 'AAA' " +
		"END AS b from tab1";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		statement = "select a from tab1 where CASE b WHEN 1 THEN 2 WHEN 3 THEN 4 ELSE 5 END > 34";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		statement = "select a from tab1 where CASE b WHEN 1 THEN 2 + 3 ELSE 4 END > 34";
		parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);

		
		
		statement = "select a,(CASE " +
		"WHEN (CASE a WHEN 1 THEN 10 ELSE 20 END) > 15 THEN 'BBB' " +
//		"WHEN (select c from tab2 WHERE d = 2) = 3 THEN 'AAA' " +
		"END) from tab1";
		parsed = jef.database.DbUtils.parseStatement(statement);
		//System.out.println(""+statement);
		//System.out.println(""+parsed);
		assertEquals(statement, ""+parsed);
		
		
	}

	public void testReplaceAsFunction() throws ParseException {
		String statement = "select REPLACE(a,'b',c) from tab1";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);
	}

	public void testLike() throws ParseException {
		String statement = "select * from tab1 where a LIKE 'test'";
		PlainSelect plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("test", (((StringValue)((LikeExpression) plainSelect.getWhere()).getRightExpression()).getValue()).toString());
		assertEquals(statement, ""+plainSelect);

		statement = "select * from tab1 where a LIKE 'test' ESCAPE 'test2'";
		plainSelect = (PlainSelect) ((Select) jef.database.DbUtils.parseStatement(statement)).getSelectBody();
		assertEquals("test", (((StringValue)((LikeExpression) plainSelect.getWhere()).getRightExpression()).getValue()).toString());
		assertEquals("test2", (((LikeExpression) plainSelect.getWhere()).getEscape()));
		assertEquals(statement, ""+plainSelect);
	}

	public void testSelectOrderHaving() throws ParseException {
		String statement = "select units,count(units) AS num from currency group by units having count(units) > 1 order by num";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);
	}

	public void testDouble() throws ParseException {
		String statement = "select 1e2, * from mytable WHERE mytable.col = 9";
		Select select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(1e2, ((DoubleValue)((SelectExpressionItem)((PlainSelect) select.getSelectBody()).getSelectItems().get(0)).getExpression()).getValue(), 0);

		statement = "select * from mytable WHERE mytable.col = 1.e2";
		select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(1e2, ((DoubleValue)((BinaryExpression)((PlainSelect) select.getSelectBody()).getWhere()).getRightExpression()).getValue(), 0);

		statement = "select * from mytable WHERE mytable.col = 1.2e2";
		select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(1.2e2, ((DoubleValue)((BinaryExpression)((PlainSelect) select.getSelectBody()).getWhere()).getRightExpression()).getValue(), 0);

		statement = "select * from mytable WHERE mytable.col = 2e2";
		select = (Select) jef.database.DbUtils.parseStatement(statement);

		assertEquals(2e2, ((DoubleValue)((BinaryExpression)((PlainSelect) select.getSelectBody()).getWhere()).getRightExpression()).getValue(), 0);
	}


	public void testWith() throws ParseException {
		String statement = "WITH DINFO (DEPTNO,AVGSALARY,EMPCOUNT) AS " + 
							"(select OTHERS.WORKDEPT,AVG(OTHERS.SALARY),COUNT(*) from EMPLOYEE OTHERS " +
							"group by OTHERS.WORKDEPT), DINFOMAX AS (select MAX(AVGSALARY) AS AVGMAX from DINFO) " +
							"select THIS_EMP.EMPNO,THIS_EMP.SALARY,DINFO.AVGSALARY,DINFO.EMPCOUNT,DINFOMAX.AVGMAX " +
							"from EMPLOYEE THIS_EMP INNER JOIN DINFO INNER JOIN DINFOMAX " +
							"where THIS_EMP.JOB = 'SALESREP' AND THIS_EMP.WORKDEPT = DINFO.DEPTNO";
		Select select = (Select) jef.database.DbUtils.parseStatement(statement);
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);
	}
	
	public void testSelectAliasInQuotes() throws ParseException {
		String statement = "select mycolumn AS \"My Column Name\" from mytable";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		assertEquals(statement, ""+parsed);
	}
	
	
	public void testSelectJoinWithComma() throws ParseException {
	String statement = "select cb.Genus,cb.Species from Coleccion_de_Briofitas cb, unigeoestados es " +
    				"where es.nombre = \"Tamaulipas\" AND cb.the_geom = es.geom";
	Statement parsed = jef.database.DbUtils.parseStatement(statement);
	assertEquals(statement, ""+parsed);
	}
	
	public void testDeparser() throws ParseException {
		String statement = "select a.OWNERLASTNAME,a.OWNERFIRSTNAME "
							+"from ANTIQUEOWNERS a, ANTIQUES b "
							+"where b.BUYERID = a.OWNERID AND b.ITEM = 'Chair'";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		StatementDeParser deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		
		assertEquals(statement, deParser.getBuffer().toString());
		
		statement = "select count(DISTINCT f + 4) from a";
		parsed = jef.database.DbUtils.parseStatement(statement);
		deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());

		statement = "select count(DISTINCT f,g,h) from a";
		parsed = jef.database.DbUtils.parseStatement(statement);
		deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());
	}
	
	public void testMysqlQuote() throws ParseException {
		String statement = "select `a.OWNERLASTNAME`,`OWNERFIRSTNAME` "
							+"from `ANTIQUEOWNERS` a, ANTIQUES b "
							+"where b.BUYERID = a.OWNERID AND b.ITEM = 'Chair'";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		StatementDeParser deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());
	}

	public void testConcat() throws ParseException {
		String statement = "select a || b || c + 4 from t";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		StatementDeParser deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());
	}

	public void testMatches() throws ParseException {
		String statement = "select * from team where team.search_column @@ to_tsquery('new & york & yankees')";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		StatementDeParser deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());
	}

	public void testGroupByExpression() throws ParseException {
		String statement = 
		"select col1,col2,col1 + col2,sum(col8)" +
		" from table1 " +
		"group by col1,col2,col1 + col2";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		StatementDeParser deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());
	}

	public void testBitwise() throws ParseException {
		String statement = 
		"select col1 & 32,col2 ^ col1,col1 | col2" +
		" from table1";
		Statement parsed = jef.database.DbUtils.parseStatement(statement);
		StatementDeParser deParser=new StatementDeParser(new StringBuilder());
		parsed.accept(deParser);
		
		assertEquals(statement, parsed.toString());
		assertEquals(statement, deParser.getBuffer().toString());
	}

	
	public static void main(String[] args) {
		//junit.swingui.TestRunner.run(SelectTest.class);
	}

}
