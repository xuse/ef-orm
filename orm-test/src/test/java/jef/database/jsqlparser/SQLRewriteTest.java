package jef.database.jsqlparser;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import jef.database.DbUtils;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.support.RDBMS;

/**
 * 测试在不同数据库下的兼容性
 * 
 * @author jiyi
 * 
 */
public class SQLRewriteTest extends org.junit.Assert {
	private DatabaseDialect oracle = AbstractDialect.getProfile("oracle");
	private DatabaseDialect derby = AbstractDialect.getProfile("derby");
	private DatabaseDialect mysql = AbstractDialect.getProfile("mysql");
	private DatabaseDialect postgres = AbstractDialect.getProfile("postgresql");
	private DatabaseDialect hsql = AbstractDialect.getProfile(RDBMS.hsqldb.name());

	@Test
	public void test1() throws ParseException {
		Select ex=DbUtils.parseSelect("select -id from dual");
		System.out.println(ex);
		
	}
	/**
	 * 先来个最简单的
	 * @throws ParseException
	 */
	@Test
	public void testDateTimeOper() throws ParseException {
		String[] sqls={
				"select year(now()) from dual",
				"select month(now()) from dual",
				"select day(now()) from dual",
				"select hour(current_timestamp) from dual",
				"select minute(now()) from dual",
				"select second(current_timestamp) from dual"
		};
		
		rewrite(sqls);
		//检查Oracle重写
		assertEquals(Arrays.asList(
				"select extract(year from sysdate) from dual",
				"select extract(month from sysdate) from dual",
				"select extract(day from sysdate) from dual",
				"select extract(hour from systimestamp) from dual",
				"select extract(minute from systimestamp) from dual",
				"select extract(second from systimestamp) from dual"
		),toOracle(sqls));
	}
	
	/**
	 * 然后是复杂一点的
	 * @param sqls
	 * @throws ParseException
	 */
	@Test
	public void testDateTimeOper2() throws ParseException{
		String[] sqls={
				"select year(now())||'ABC' from dual",
				"select current_timestamp from foo where modified < adddate(now(), interval 5 hour)",
				"select sysdate() from foo where modified < subdate(sysdate(),  interval 5 minute)",
				"select now() + interval '2 years' from dual",
				"select now() + interval '2 months' from dual",
		};
		rewrite(sqls);
	}
	
	/**
	 * 如果直接写成JDBC函数的样式会怎么样
	 * @throws ParseException
	 */
	@Test
	public void testDateTimeOper3() throws ParseException{
		String[] sqls={
				"select A from datetable where B < {fn timestampadd(SQL_TSI_HOUR,5,current_timestamp)}",//直接按JDBC样式写 (单位：hour)
				"select A from datetable where B < timestampadd(MINUTE,1,'2003-01-02')",             //按MYSQL样式写 (单位：minute)
				"select timestampdiff(HOUR,'2013-7-1',now()) from dual",               //按MYSQL样式写 (单位：hour)
				"select {fn timestampdiff(SQL_TSI_YEAR,timestamp('2013-7-1 01:00:00'), current_timestamp) } from dual",//按MYSQL样式写 (单位：minute)
				"select timestampdiff(DAY,'2013-7-1',now()) from dual",               				//按MYSQL样式写 (单位：quarter)
				"select {fn timestampdiff(SQL_TSI_YEAR,timestamp('2013-7-1 01:00:00'), current_timestamp) } from dual",//按MYSQL样式写 (单位：month)
				"select timestampdiff(YEAR,'2013-7-1',now()) from dual",               //按MYSQL样式写 (单位：year)
				"select {fn timestampdiff(SQL_TSI_DAY,timestamp('2013-7-1 01:00:00'), current_timestamp) } from dual" //按MYSQL样式写(单位：day)
				
		};
		rewrite(sqls);
	}
	
	@Test
	public void testDateTimeOper4() throws ParseException{
		String[] sqls={
				"select year(sysdate),month(sysdate),day(sysdate),hour(current_timestamp),minute(current_time),second(now()) from dual",
		};
		rewrite(sqls);
	}
	
	
	@Test
	public void testDateTranslate() throws ParseException{
		String[] sqls={
				"select translate('abcdefghijklmnopqrstuvwxyz','abcdefghijk','1234567890') from dual",
		};
		rewrite(sqls);
	}
	

	/*
	 * 打印出修改后的SQL语句
	 */
	private void rewrite(String... sqls) throws ParseException {
		System.out.println("================ RAW:");
		System.out.println(StringUtils.join(sqls,"\r\n"));
		{
			System.out.println("================ ORACLE:");
			System.out.println(StringUtils.join(toOracle(sqls),"\r\n"));
		}
		{
			System.out.println("================ MySQL:");
			System.out.println(StringUtils.join(toMySQL(sqls),"\r\n"));
		}
		{
			System.out.println("================ Postgres:");
			System.out.println(StringUtils.join(toPostgres(sqls),"\r\n"));
		}
		{
			System.out.println("================ Derby:");
			System.out.println(StringUtils.join(toDerby(sqls),"\r\n"));
		}
//		{
//			System.out.println("================ HSQL:");
//			System.out.println(StringUtils.join(toHsql(sqls),"\r\n"));
//		}
		
	}

	private List<String> toOracle(String... sqls) throws ParseException {
		String[] result = new String[sqls.length];
		int n = 0;
		for (String sql : sqls) {
			Statement st = DbUtils.parseStatement(sql);
			st.accept(new SqlFunctionlocalization(oracle,null));
			result[n++] = st.toString();
		}
		return Arrays.asList(result);
	}

	private List<String> toDerby(String... sqls) throws ParseException {
		String[] result = new String[sqls.length];
		int n = 0;
		for (String sql : sqls) {
			Statement st = DbUtils.parseStatement(sql);
			st.accept(new SqlFunctionlocalization(derby,null));
			result[n++] = st.toString();
		}
		return Arrays.asList(result);
	}
	
	private List<String> toHsql(String... sqls) throws ParseException {
		String[] result = new String[sqls.length];
		int n = 0;
		for (String sql : sqls) {
			Statement st = DbUtils.parseStatement(sql);
			st.accept(new SqlFunctionlocalization(hsql,null));
			result[n++] = st.toString();
		}
		return Arrays.asList(result);
	}

	private List<String> toMySQL(String... sqls) throws ParseException {
		String[] result = new String[sqls.length];
		int n = 0;
		for (String sql : sqls) {
			Statement st = DbUtils.parseStatement(sql);
			st.accept(new SqlFunctionlocalization(mysql,null));
			result[n++] = st.toString();
		}
		return Arrays.asList(result);
	}

	private List<String> toPostgres(String... sqls) throws ParseException {
		String[] result = new String[sqls.length];
		int n = 0;
		for (String sql : sqls) {
			Statement st = DbUtils.parseStatement(sql);
			st.accept(new SqlFunctionlocalization(postgres,null));
			result[n++] = st.toString();
		}
		return Arrays.asList(result);
	}
	
	@Test
	public void test123(){
		String sql="dob,dob,str(add_month(dob,12)) as pname";
//		Statement st = DbUtils.parseStatement(sql);
		
	}
	
}
