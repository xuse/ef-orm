package jef.database.jsqlparser.test.truncate;

import jef.database.jsqlparser.statement.truncate.Truncate;
import junit.framework.TestCase;

public class TruncateTest extends TestCase {

	public TruncateTest(String arg0) {
		super(arg0);
	}

	public void testTruncate() throws Exception {
		String statement = "TRUncATE TABLE myschema.mytab";
		Truncate truncate = (Truncate) jef.database.DbUtils.parseStatement(statement);
		assertEquals("myschema", truncate.getTable().getSchemaName());
		assertEquals("myschema.mytab", truncate.getTable().getWholeTableName());
		assertEquals(statement.toUpperCase(), truncate.toString().toUpperCase());	
		
		statement = "TRUncATE   TABLE    mytab";
		String toStringStatement = "TRUncATE TABLE mytab";
		truncate = (Truncate) jef.database.DbUtils.parseStatement(statement);
		assertEquals("mytab", truncate.getTable().getName());
		assertEquals(toStringStatement.toUpperCase(), truncate.toString().toUpperCase());
	}

	public static void main(String[] args) {
		//junit.swingui.TestRunner.run(TruncateTest.class);
	}

}
