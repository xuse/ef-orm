package jef.database.jsqlparser.test.tablesfinder;

import static junit.framework.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import jef.database.DbUtils;
import jef.database.jsqlparser.statement.select.Select;

import org.junit.Test;

@SuppressWarnings({  "rawtypes" })
public class TablesNamesFinderTest {
	@Test
	public void testGetTableList() throws Exception {
		String sql = "select * from MY_TABLE0 where col1 like 'aaa%' union all SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 " + " WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)";
		jef.database.jsqlparser.statement.Statement statement = DbUtils.parseStatement(sql);
		// now you should use a class that implements StatementVisitor to decide
		// what to do
		// based on the kind of the statement, that is SELECT or INSERT etc. but
		// here we are only
		// interested in SELECTS
		if (statement instanceof Select) {
			Select selectStatement = (Select) statement;
			TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
			List tableList = tablesNamesFinder.getTableList(selectStatement);
			assertEquals(7, tableList.size());
			int i = 0;
			for (Iterator iter = tableList.iterator(); iter.hasNext(); i++) {
				String tableName = (String) iter.next();
				assertEquals("MY_TABLE" + i, tableName);
			}
		}
	}
}
