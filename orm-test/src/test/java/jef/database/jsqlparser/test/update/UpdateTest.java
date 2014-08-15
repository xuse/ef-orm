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
		assertEquals(3, update.getSets().size());
		assertEquals("col1", update.getSet(0).first.getColumnName());
		assertEquals("col2", update.getSet(1).first.getColumnName());
		assertEquals("col3", update.getSet(2).first.getColumnName());
		assertEquals("as", ((StringValue) update.getSet(0).second).getValue());
		assertTrue(update.getSet(1).second instanceof JpqlParameter);
		assertEquals(565, ((LongValue) update.getSet(2).second).getValue().longValue());

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
