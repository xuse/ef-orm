package org.easyframe.tutorial.lesson3;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.Condition;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.meta.FBIField;
import jef.database.query.JpqlExpression;
import jef.database.query.QueryBuilder;
import jef.database.query.SqlExpression;
import jef.tools.DateUtils;
import jef.tools.string.RandomData;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.junit.Test;

/**
 * 这个案例演示单表查询Criteria的用法，请对照执行后控制台上的打印出的SQL语句来验证。
 * 
 * @author jiyi
 * 
 */
public class Case1 extends org.junit.Assert {
	DbClient db;

	public Case1() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db = new DbClient();
		// 准备数据时关闭调试，减少控制台信息
		ORMConfig.getInstance().setDebugMode(false);
		db.dropTable(Student.class);
		db.createTable(Student.class);
		prepareData(5);
		ORMConfig.getInstance().setDebugMode(true);
	}

	@Test
	public void testSelect_example() throws SQLException {
		Student st = new Student();
		st.setGender("M");
		st.setGrade("2");
		List<Student> students = db.select(st);

		System.out.println("2年纪的男生：" + students.size());

		// 用改写成count语句的方式再查一遍，检查两遍是否相等。
		assertEquals(db.count(st.getQuery()), students.size());
	}

	@Test
	public void testSelect_Id() throws SQLException {
		Student st = new Student();
		st.setId(1);
		st.setGender("M");
		List<Student> students = db.select(st);

		System.out.println("ID为1的学生：" + students.size());

		// 用改写成count语句的方式再查一遍，检查两遍是否相等。
		assertEquals(db.count(st.getQuery()), students.size());
	}

	@Test
	public void testSelect_In() throws SQLException {
		Student st = new Student();

		st.getQuery().addCondition(QueryBuilder.in(Student.Field.id, new int[] { 1, 3, 5, 7, 9 }) // 查询学号为
																									// 1,3,5,7,9的学生
				);

		List<Student> students = db.select(st);

		System.out.println("学号为1、3、5、7、9的学生:" + students.size());

		// 用改写成count语句的方式再查一遍，检查两遍是否相等。
		assertEquals(db.count(st.getQuery()), students.size());
	}

	@Test
	public void testSelect_Between() throws SQLException {
		Student st = new Student();
		// 查询出生日期在，1999-1-1到 2003-12-31之间的学生。
		st.getQuery().addCondition(QueryBuilder.between(Student.Field.dateOfBirth, DateUtils.getDate(1999, 1, 1), DateUtils.getDate(2003, 12, 31)));
		// gender='M'的（男生）
		st.getQuery().addCondition(Student.Field.gender, "M");

		List<Student> students = db.select(st);
		System.out.println("出生日期在范围内的男生:" + students.size());

		assertEquals(db.count(st.getQuery()), students.size());
	}

	@Test
	public void testSelect_AndOrNot() throws SQLException {
		Student st = new Student();
		
		//三个Like条件，或
		Condition isLastNam_ZhaoQianSun =QueryBuilder.or(
				QueryBuilder.matchStart(Student.Field.name, "赵"),
				QueryBuilder.matchStart(Student.Field.name, "钱"),
				QueryBuilder.matchStart(Student.Field.name, "孙")
		);
		
		//或条件前面加上 NOT。
		Condition isNot_ZhaoQianSun = QueryBuilder.not(isLastNam_ZhaoQianSun);
		
		//最终条件: 不姓'赵钱孙' 的女生。
		st.getQuery().addCondition(QueryBuilder.and(
				isNot_ZhaoQianSun,
				QueryBuilder.eq(Student.Field.gender, "F")
			)
		);
		
		List<Student> students=db.select(st);
		System.out.println("不姓'赵钱孙'三姓的女生:" + students.size());
		
		assertEquals(db.count(st.getQuery()), students.size());
	}

	@Test
	public void testSelect_Function() throws SQLException {
		Student st = new Student();
		st.getQuery().addCondition(new FBIField("concat(lower(gender) , grade)"),"f2");
		List<Student> students=db.select(st);
		
		assertEquals(db.count(st.getQuery()), students.size());
		
	}
	
	@Test
	public void testSelect_JpqlExpression() throws SQLException {
		Student st = new Student();
		{
			//案例一
			st.getQuery().addCondition(Student.Field.gender, new JpqlExpression("upper(nvl('f','m'))"));
			List<Student> students=db.select(st);
			assertEquals(db.count(st.getQuery()), students.size());	
		}
		st.getQuery().clearQuery(); //清除上一个查询条件
		{
			//案例二: 查出出生日期最晚的学生
			st.getQuery().addCondition(Student.Field.dateOfBirth, new JpqlExpression("(select max(dateOfBirth) from student)").bind(st.getQuery()));
			List<Student> students=db.select(st);
			assertEquals(db.count(st.getQuery()), students.size());	
		}
	}
	
	@Test
	public void testSelect_SqlExpression() throws SQLException {
		Student st = new Student();
		//案例: 查出出生日期最晚的学生
		st.getQuery().addCondition(Student.Field.dateOfBirth, new SqlExpression("(select max(date_of_birth) from student)"));
		List<Student> students=db.select(st);
		
		assertEquals(db.count(st.getQuery()), students.size());	
	}
	
	
	@Test
	public void testSelect_SqlExpression2() throws SQLException {
		Student st = new Student();
		st.getQuery().addCondition(
				new SqlExpression("{fn timestampdiff(SQL_TSI_DAY,date_of_birth,current_timestamp)} > 100")
				
		);
		List<Student> students=db.select(st);
		
		assertEquals(db.count(st.getQuery()), students.size());	
	}
	
	
	@Test
	public void testUpdate_SqlExpression() throws SQLException {
		Student st = new Student();
		st.getQuery().addCondition(new FBIField("concat(lower(gender) , grade)"),"f2");
		List<Student> students=db.select(st);
		
		assertEquals(db.count(st.getQuery()), students.size());
	}
	
	
	private void prepareData(int num) throws SQLException {
		List<Student> data = new ArrayList<Student>();
		Date old=new Date(System.currentTimeMillis() - 864000000000L);
		for (int i = 0; i < num; i++) {
			// 用随机数生成一些学生信息
			Student st = new Student();
			st.setGender(i % 2 == 0 ? "M" : "F");
			st.setName(RandomData.randomChineseName());
			st.setDateOfBirth(RandomData.randomDate(old, new Date()));
			st.setGrade("2");
			data.add(st);
		}
		db.batchInsert(data);
	}
}
