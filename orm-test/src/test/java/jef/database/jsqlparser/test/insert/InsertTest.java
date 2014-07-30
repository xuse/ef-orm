package jef.database.jsqlparser.test.insert;

import jef.database.jsqlparser.expression.DoubleValue;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.schema.Column;
import jef.database.jsqlparser.schema.Table;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SubSelect;
import junit.framework.TestCase;

import org.junit.Ignore;
import org.junit.Test;

public class InsertTest extends TestCase {
	
	public InsertTest(String arg0) {
		super(arg0);
	}

	@Test
	public void testRegularInsert() throws ParseException {
		String statement = "insert into mytable (col1,col2,col3) values (?1,'sadfsd',234)";
		Insert insert = (Insert) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", ((Table)insert.getTable()).getName());
		assertEquals(3, insert.getColumns().size());
		assertEquals("col1", ((Column) insert.getColumns().get(0)).getColumnName());
		assertEquals("col2", ((Column) insert.getColumns().get(1)).getColumnName());
		assertEquals("col3", ((Column) insert.getColumns().get(2)).getColumnName());
		assertEquals(3, ((ExpressionList) insert.getItemsList()).getExpressions().size());
		assertTrue (((ExpressionList) insert.getItemsList()).getExpressions().get(0) instanceof JpqlParameter);
		assertEquals("sadfsd", ((StringValue) ((ExpressionList) insert.getItemsList()).getExpressions().get(1)).getValue());
		assertEquals(234, ((LongValue) ((ExpressionList) insert.getItemsList()).getExpressions().get(2)).getValue());
		assertEquals(statement, ""+insert);

		 statement = "insert into myschema.mytable values (?1,?2,2.3)";
		 insert = (Insert) jef.database.DbUtils.parseStatement(statement);
		assertEquals("myschema.mytable", insert.getTable().getWholeTableName());
		assertEquals(3, ((ExpressionList) insert.getItemsList()).getExpressions().size());
		assertTrue (((ExpressionList) insert.getItemsList()).getExpressions().get(0) instanceof JpqlParameter);
		assertEquals(2.3, ((DoubleValue) ((ExpressionList) insert.getItemsList()).getExpressions().get(2)).getValue(), 0.0);
		assertEquals(statement, ""+insert);

	}

	@Test
	@Ignore
	public void testInsertFromSelect() throws ParseException {
		String statement = "insert into mytable (col1,col2,col3) SELECT * FROM mytable2";
		Insert insert = (Insert) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", ((Table)insert.getTable()).getName());
		assertEquals(3, insert.getColumns().size());
		assertEquals("col1", ((Column) insert.getColumns().get(0)).getColumnName());
		assertEquals("col2", ((Column) insert.getColumns().get(1)).getColumnName());
		assertEquals("col3", ((Column) insert.getColumns().get(2)).getColumnName());
		assertTrue (insert.getItemsList() instanceof SubSelect);
		assertEquals("mytable2", ((Table) ((PlainSelect) ((SubSelect)insert.getItemsList()).getSelectBody()).getFromItem()).getName());
		
		//toString uses brakets
		String statementToString = "insert into mytable (col1,col2,col3) (select * from mytable2)";
		assertEquals(statementToString, ""+insert);
	}

	@Test
	public static void main(String[] args) {
		//junit.swingui.TestRunner.run(InsertTest.class);
	}

}
