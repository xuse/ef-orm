package jef.database.cache;

import jef.common.PairSO;
import jef.database.dialect.AbstractDialect;

import org.junit.Test;

public class WhereParserTest {

	@Test
	public void test1() {
		System.out.println("复杂Druid");
		PairSO ss=new WhereParser.DruidImpl().parseTables("root_cus T1 left join ENUMATIONTABLE T2 ON T1.CODE=T2.CODE and T2.TYPE='1' right join tt3 t3 on t2.cid=t3.type", AbstractDialect.getDialect("oracle"));
		System.out.println(ss.first);
		System.out.println(ss.second);
	}
	
	@Test
	public void test1_() {
		System.out.println("复杂Jef");
		PairSO ss=new WhereParser.NativeImpl().parseTables("root_cus T1 left join ENUMATIONTABLE T2 ON T1.CODE=T2.CODE and T2.TYPE='1' right join tt3 t3 on t2.cid=t3.type", AbstractDialect.getDialect("oracle"));
		System.out.println(ss.first);
		System.out.println(ss.second);
	}


	@Test
	public void test2() {
		System.out.println("简单Druid");
		PairSO ss=new WhereParser.DruidImpl().parseTables("root_cus T1", AbstractDialect.getDialect("oracle"));
		System.out.println(ss.first);
		System.out.println(ss.second);
	}
	
	@Test
	public void test2_() {
		System.out.println("简单Jef");
		PairSO ss=new WhereParser.NativeImpl().parseTables("root_cus T1", AbstractDialect.getDialect("oracle"));
		System.out.println(ss.first);
		System.out.println(ss.second);
	}
	
}
