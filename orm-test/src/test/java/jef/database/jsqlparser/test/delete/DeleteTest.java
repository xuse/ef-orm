package jef.database.jsqlparser.test.delete;

import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.delete.Delete;
import junit.framework.TestCase;

public class DeleteTest extends TestCase {

	public DeleteTest(String arg0) {
		super(arg0);
	}

	public void testDelete() throws ParseException {
		String statement = "delete from mytable where mytable.col = 9";

		Delete delete = (Delete) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytable", ((Table)delete.getTable()).getName());
		assertEquals(statement, ""+delete);
	}

	public static void main(String[] args) {
		//junit.swingui.TestRunner.run(DeleteTest.class);
	}

}
