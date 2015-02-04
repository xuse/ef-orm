package org.easyframe.tutorial.lessonb;

import java.sql.SQLException;

import jef.database.SqlTemplate;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.tutorial.lesson1.entity.Foo2;
import org.easyframe.tutorial.lesson2.entity.Student;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;


/**
 * JTA事务的测试/演示
 * @author jiyi
 *
 */
@ContextConfiguration(locations = { "classpath:spring/spring-test-jta.xml" })
public class CaseJTA extends AbstractJUnit4SpringContextTests {
	@Autowired
	private CommonDao dao;
	@Autowired
	private JTATestService service;

	@Before
	public void setup() throws SQLException {
		dao.getNoTransactionSession().getMetaData("ds1")
				.createTable(Foo2.class);
		dao.getNoTransactionSession().getMetaData("ds2")
				.createTable(Student.class);

		// LogUtil.show(dao.getNoTransactionSession().getMetaData("ds1").getTables());
		// LogUtil.show(dao.getNoTransactionSession().getMetaData("ds2").getTables());
	}

	/**
	 * 观察输出结果，两张表的记录数都增加了1，说明两个库的操作都被提交了。
	 * @throws SQLException
	 */
	@Test
	public void testSuccessJTA() throws SQLException {
		SqlTemplate ds1 = dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2 = dao.getSession().getSqlTemplate("ds2");

		long o1 = ds1.countBySql("select count(*) from foo2");
		long o2 = ds2.countBySql("select count(*) from student");

		service.success1();

		long n1 = ds1.countBySql("select count(*) from foo2");
		long n2 = ds2.countBySql("select count(*) from student");
		System.out.printf("Foo %d -> %d%n", o1, n1);
		System.out.printf("Student %d -> %d%n", o2, n2);
	}


	/**
	 * 观察输出结果，两张表的记录数都增加了1，说明两个库的操作都被提交了。
	 * @throws SQLException
	 */
	@Test
	public void testFailure1() throws SQLException {
		SqlTemplate ds1 = dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2 = dao.getSession().getSqlTemplate("ds2");

		long o1 = ds1.countBySql("select count(*) from foo2");
		long o2 = ds2.countBySql("select count(*) from student");

		service.failure1();

		long n1 = ds1.countBySql("select count(*) from foo2");
		long n2 = ds2.countBySql("select count(*) from student");
		System.out.printf("Foo %d -> %d%n", o1, n1);
		System.out.printf("Student %d -> %d%n", o2, n2);
	}
	
	/**
	 * 操作中出现错误，两张表都被回滚，操作前后数据不变。
	 * @throws SQLException
	 */
	@Test
	public void testFailure2() throws SQLException {
		SqlTemplate ds1 = dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2 = dao.getSession().getSqlTemplate("ds2");

		long o1 = ds1.countBySql("select count(*) from foo2");
		long o2 = ds2.countBySql("select count(*) from student");

		try{
			service.failure2();
		}catch(Exception e){
			System.out.println("method exit with exceptions");
		}

		long n1 = ds1.countBySql("select count(*) from foo2");
		long n2 = ds2.countBySql("select count(*) from student");
		System.out.printf("Foo %d -> %d%n", o1, n1);
		System.out.printf("Student %d -> %d%n", o2, n2);
	}
	
	/**
	 *  操作中出现错误，两张表都被回滚，操作前后数据不变。
	 * @throws SQLException
	 */
	@Test
	public void testFailure3() throws SQLException {
		SqlTemplate ds1 = dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2 = dao.getSession().getSqlTemplate("ds2");

		long o1 = ds1.countBySql("select count(*) from foo2");
		long o2 = ds2.countBySql("select count(*) from student");
		try{
			service.failure3();
		}catch(Exception e){
			System.out.println("method exit with exceptions");
		}

		long n1 = ds1.countBySql("select count(*) from foo2");
		long n2 = ds2.countBySql("select count(*) from student");
		System.out.printf("Foo %d -> %d%n", o1, n1);
		System.out.printf("Student %d -> %d%n", o2, n2);
	}
}
