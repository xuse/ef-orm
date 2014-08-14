package jef.database.jsqlparser.test.replace;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.replace.Replace;
import jef.database.jsqlparser.statement.select.SubSelect;

import org.junit.Test;

public class ReplaceTest {
	@Test
	public void testReplaceSyntax1() throws ParseException {
		String statement = "REPLACE mytable SET col1='as', col2=?1, col3=565";
		Replace replace = (Replace) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", replace.getTable().getName());
		assertEquals(3, replace.getColumns().size());
		assertEquals("col1", ((Column) replace.getColumns().get(0)).getColumnName());
		assertEquals("col2", ((Column) replace.getColumns().get(1)).getColumnName());
		assertEquals("col3", ((Column) replace.getColumns().get(2)).getColumnName());
		assertEquals("as", ((StringValue)replace.getExpressions().get(0)).getValue());
		assertTrue(replace.getExpressions().get(1) instanceof JpqlParameter);
		assertEquals(565, ((LongValue)replace.getExpressions().get(2)).getValue().longValue());
		assertEquals(statement, ""+replace);

	}
	@Test
	public void testReplaceSyntax2() throws ParseException {
		String statement = "REPLACE mytable (col1,col2,col3) VALUES ('as',?1,565)";
		Replace replace = (Replace) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", replace.getTable().getName());
		assertEquals(3, replace.getColumns().size());
		assertEquals("col1", ((Column) replace.getColumns().get(0)).getColumnName());
		assertEquals("col2", ((Column) replace.getColumns().get(1)).getColumnName());
		assertEquals("col3", ((Column) replace.getColumns().get(2)).getColumnName());
		assertEquals("as", ((StringValue) ((ExpressionList)replace.getItemsList()).getExpressions().get(0)).getValue());
		assertTrue(((ExpressionList)replace.getItemsList()).getExpressions().get(1) instanceof JpqlParameter);
		assertEquals(565, ((LongValue) ((ExpressionList)replace.getItemsList()).getExpressions().get(2)).getValue().longValue());
		assertEquals(statement, ""+replace);
	}

	@Test
	public void testReplaceSyntax3() throws ParseException {
		String statement = "REPLACE mytable (col1, col2, col3) SELECT * FROM mytable3";
		Replace replace = (Replace) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", replace.getTable().getName());
		assertEquals(3, replace.getColumns().size());
		assertEquals("col1", ((Column) replace.getColumns().get(0)).getColumnName());
		assertEquals("col2", ((Column) replace.getColumns().get(1)).getColumnName());
		assertEquals("col3", ((Column) replace.getColumns().get(2)).getColumnName());
		assertTrue(replace.getItemsList() instanceof SubSelect);
		//TODO:
		//assertEquals(statement, ""+replace);
	}
}
