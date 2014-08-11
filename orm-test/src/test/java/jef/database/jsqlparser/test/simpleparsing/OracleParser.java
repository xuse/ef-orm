package jef.database.jsqlparser.test.simpleparsing;

import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.visitor.Statement;

import org.junit.Test;

public class OracleParser {
	@Test
	public void testOracleStart() throws ParseException{
		String sql="SELECT T1.ITEM_GROUP AS ITEMID, T1.PARENT_GROUP_ITEM, LEVEL NODELEVEL"+
 " FROM PM_CONF_ITEM_GROUP_DTL T1 START WITH T1.ITEM_GROUP IN (5001400) CONNECT BY T1.ITEM_GROUP =  PRIOR T1.PARENT_GROUP_ITEM";
		System.out.println(sql);
		Statement st=(Statement) jef.database.DbUtils.parseStatement(sql);
		System.out.println(st.toString());
	}
}
