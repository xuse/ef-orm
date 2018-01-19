package com.alibaba.druid;

import jef.database.dialect.handler.DerbyLimitHandler;
import jef.database.dialect.handler.LimitHandler;
import jef.database.dialect.handler.LimitOffsetLimitHandler;
import jef.database.dialect.handler.MySqlLimitHandler;
import jef.database.dialect.handler.OracleLimitHander;
import jef.database.dialect.handler.SQL2000LimitHandler;
import jef.database.dialect.handler.SQL2005LimitHandler;
import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;
import junit.framework.Assert;

import org.junit.Test;

public class LimitHandlerTest {
	String[] sqls = { "SELECT  \n" + "T.NAME AS PNAME, T1.NAME FROM 	parent T, 	child T1 WHERE	T.ID = T1.PARENTID order by t1.name",
			"(select * from child t where t.code like 'code%') union all (select rootid as parentid,code,id,name from parent) union all (select * from child t) order by name " };

	PageLimit pageParam = new PageLimit( 70, 10);

	@Test
	public void testSql2000Impl() {
		LimitHandler lh = new SQL2000LimitHandler();
		String expect="SELECT TOP 80 T.NAME AS PNAME, T1.NAME FROM parent T, child T1 WHERE T.ID = T1.PARENTID ORDER BY t1.name";
		doAssert(sqls[0],lh,expect);
		
		expect="SELECT TOP 80 * FROM(SELECT * FROM child t WHERE t.code LIKE 'code%' UNION ALL SELECT rootid AS parentid, code, id, name FROM parent UNION ALL SELECT * FROM child t ) __ef_tmp1"+
"\nORDER BY name";
		doAssert(sqls[1],lh,expect);
		doTest(lh);
	}

	@Test
	public void test2005ParserImpl() {
		LimitHandler lh = new SQL2005LimitHandler();
		for (String sql : sqls) {
			System.out.println(lh.toPageSQL(sql, pageParam));
		}
		doTest(lh);
	}

	@Test
	public void test2005DruidImpl() {
		LimitHandler lh = new SQL2005LimitHandler();
		for (String sql : sqls) {
			System.out.println("--Druid--");
			System.out.println(lh.toPageSQL(sql, pageParam));
		}
		doTest(lh);
	}

	@Test
	public void testMySQL() {
		doTest(new MySqlLimitHandler());
	}

	@Test
	public void testPostgres() {
		doTest(new LimitOffsetLimitHandler());
	}

	@Test
	public void testDerby() {
		doTest(new DerbyLimitHandler());
	}

	private void doTest(LimitHandler lh) {
		String sql = sqls[0];
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			lh.toPageSQL(sql, pageParam);
		}
		long cost=System.currentTimeMillis() - start;
		System.out.println(lh.getClass().getSimpleName()+"运行一万次，耗时"+cost+"ms.");
	}
	

	private void doAssert(String raw, LimitHandler lh, String expect) {
		BindSql sql=lh.toPageSQL(raw, pageParam);
		String actual=sql.getSql();
		System.out.println(actual);
		Assert.assertEquals(expect, actual);
	}
	
	@Test
	public void testWhenOffsetIs0(){
		LimitHandler lh=new OracleLimitHander();
		BindSql sql=lh.toPageSQL(sqls[0],new PageLimit(0,15));
		System.out.println(sql.getSql());
		
		lh=new SQL2000LimitHandler();
		sql=lh.toPageSQL(sqls[0],new PageLimit(0,15));
		System.out.println(sql.getSql());
		
		lh=new SQL2005LimitHandler();
		sql=lh.toPageSQL(sqls[0],new PageLimit(0,15));
		System.out.println(sql.getSql());
		
		lh=new DerbyLimitHandler();
		sql=lh.toPageSQL(sqls[0],new PageLimit(0,15));
		System.out.println(sql.getSql());
		
		
		lh=new LimitOffsetLimitHandler();
		sql=lh.toPageSQL(sqls[0],new PageLimit(0,15));
		System.out.println(sql.getSql());
		

		lh=new MySqlLimitHandler();
		sql=lh.toPageSQL(sqls[0],new PageLimit(0,15));
		System.out.println(sql.getSql());
		
		
	}
}
