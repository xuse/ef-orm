package jef.database.jsqlparser.test.update;

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.expression.operators.relational.GreaterThanEquals;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.update.Update;
import junit.framework.TestCase;

public class UpdateTest extends TestCase {

	public UpdateTest(String arg0) {
		super(arg0);
	}
	public void testUpdate() throws ParseException {
		String statement = "UPDATE mytable set col1='as', col2=?1, col3=565 Where o >= 3";
		Update update = (Update) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", ((Table)update.getTable()).getName());
		assertEquals(3, update.getColumns().size());
		assertEquals("col1", ((Column) update.getColumns().get(0)).getColumnName());
		assertEquals("col2", ((Column) update.getColumns().get(1)).getColumnName());
		assertEquals("col3", ((Column) update.getColumns().get(2)).getColumnName());
		assertEquals("as", ((StringValue) update.getExpressions().get(0)).getValue());
		assertTrue(update.getExpressions().get(1) instanceof JpqlParameter);
		assertEquals(565, ((LongValue) update.getExpressions().get(2)).getValue());

		assertTrue(update.getWhere() instanceof GreaterThanEquals);
	}

	public void testUpdateWAlias() throws ParseException {
		String statement = "UPDATE table1 A SET A.column = 'XXX' WHERE A.cod_table = 'YYY'";
		Update update = (Update) jef.database.DbUtils.parseStatement(statement);
	}

	public static void main(String[] args) {
		//junit.swingui.TestRunner.run(UpdateTest.class);
	}

}
