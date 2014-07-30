package jef.database.jsqlparser.test.create;

import static junit.framework.Assert.assertEquals;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.create.table.ColumnDefinition;
import jef.database.jsqlparser.statement.create.table.CreateTable;
import jef.database.jsqlparser.statement.create.table.Index;

import org.junit.Test;
public class CreateTableTest {
	@Test
	public void testCreateTable() throws ParseException {
		String statement = "CREATE TABLE mytab (mycol a(10,20) c nm g,mycol2 mypar1 mypar2 (23,323,3) asdf ('23','123') dasd, " + "PRIMARY  KEY (mycol2,mycol)) type = myisam";
		CreateTable createTable = (CreateTable) jef.database.DbUtils.parseStatement(statement);
		assertEquals(2, createTable.getColumnDefinitions().size());
		assertEquals("mycol", ((ColumnDefinition) createTable.getColumnDefinitions().get(0)).getColumnName());
		assertEquals("mycol2", ((ColumnDefinition) createTable.getColumnDefinitions().get(1)).getColumnName());
		assertEquals("PRIMARY KEY", ((Index) createTable.getIndexes().get(0)).getType());
		assertEquals("mycol", ((Index) createTable.getIndexes().get(0)).getColumnsNames().get(1));
		
		assertEquals(statement.replace("PRIMARY  KEY", "PRIMARY KEY"), "" + createTable);
	}
}
