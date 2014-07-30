package jef.database.jsqlparser.test.drop;

import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.drop.Drop;
import junit.framework.TestCase;

public class DropTest extends TestCase {
	public DropTest(String arg0) {
		super(arg0);
	}

	public void testDrop() throws ParseException {
		String statement =
			"DROP TABLE mytab";
		Drop drop = (Drop) jef.database.DbUtils.parseStatement(statement);
		assertEquals("TABLE", drop.getType());
		assertEquals("mytab", drop.getName());
		assertEquals(statement, ""+drop);
		
		statement =
					"DROP INDEX myindex CASCADE";
		drop = (Drop) jef.database.DbUtils.parseStatement(statement);
		assertEquals("INDEX", drop.getType());
		assertEquals("myindex", drop.getName());
		assertEquals("CASCADE", drop.getParameters().get(0));
		assertEquals(statement, ""+drop);
	}

	public static void main(String[] args) {
		//junit.swingui.TestRunner.run(DropTest.class);
	}

}
