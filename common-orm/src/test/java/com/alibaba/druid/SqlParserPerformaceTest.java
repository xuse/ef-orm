package com.alibaba.druid;
import org.junit.Test;

import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;



public class SqlParserPerformaceTest {
	@Test
	public void testDruidParser(){
		String sql = "select t.rowid from (select col1,col2,col3 from person_table where age>12 and name='asss' and schoolId||'tomo'||schoolId  =? order by item) t";
		OracleStatementParser s=new OracleStatementParser(sql);
		SQLSelectStatement select=s.parseSelect();
		System.out.println(select);
	}
	@Test
	public void testDrud(){
//		String sql="SELECT '2008-12-31 23:59:59' + INTERVAL 1 SECOND from dual";
		String sql="select value from dual  where end_time < date_sub(now(), interval 5 day)";
		{
			MySqlStatementParser parser = new MySqlStatementParser(sql);
//			parser.Select();
		}
		for(int i=0;i<10;i++){
			long start=System.nanoTime();
			MySqlStatementParser parser = new MySqlStatementParser(sql);
			SQLSelectStatement select= parser.parseSelect();
			System.out.println((System.nanoTime()-start)/1000);
//			System.out.println(select);
		}
	}

}
