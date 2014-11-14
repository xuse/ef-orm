package com.alibaba.druid;

import jef.database.dialect.DerbyLimitHandler;
import jef.database.dialect.LimitHandler;
import jef.database.dialect.LimitOffsetLimitHandler;
import jef.database.dialect.MySqlLimitHandler;
import jef.database.dialect.SQL2000LimitHandler;
import jef.database.dialect.SQL2005LimitHandler;

import org.junit.Test;

public class LimitHandlerTest {
	String[] sqls = { "SELECT  \n" + "T.NAME AS PNAME, T1.NAME FROM 	parent T, 	child T1 WHERE	T.ID = T1.PARENTID order by t1.name",
			"(select * from child t where t.code like 'code%') union all (select rootid as parentid,code,id,name from parent) union all (select * from child t) order by name " };

	int[] pageParam = new int[] { 70, 10 };

	@Test
	public void testSql2000Impl() {
		LimitHandler lh = new SQL2000LimitHandler();
		for (String sql : sqls) {
			System.out.println(lh.toPageSQL(sql, pageParam));
		}
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
		System.out.println(System.currentTimeMillis() - start);
	}

}
