package org.easyframe.tutorial.lesson6;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson4.entity.Person;
import org.easyframe.tutorial.lesson4.entity.School;
import org.easyframe.tutorial.lesson5.entity.Item;
import org.junit.BeforeClass;
import org.junit.Test;

import jef.common.wrapper.IntRange;
import jef.common.wrapper.Page;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.QB;
import jef.database.query.Join;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.tools.PageLimit;

public class Case1 extends org.junit.Assert {

	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial").build();
		db.dropTable(Person.class, Item.class, Student.class,School.class);
		db.createTable(Person.class, Item.class, Student.class,School.class);

		Person p = new Person();
		p.setGender('F');
		p.setName("张飞");
		db.insert(p);

		p = new Person();
		p.setGender('F');
		p.setName("关羽");
		db.insert(p);

		p = new Person();
		p.setGender('F');
		p.setName("刘备");
		db.insert(p);

		Item item = new Item();
		item.setName("张飞");
		item.setCatalogyId(12);
		db.insert(item);

		Student st = new Student();
		st.setName("张飞");
		st.setDateOfBirth(new Date());
		st.setGender("F");
		st.setGrade("3");
		db.insert(st);

		st = new Student();
		st.setName("关羽");
		st.setDateOfBirth(new Date());
		st.setGender("F");
		st.setGrade("2");
		db.insert(st);
	}

	/**
	 * 使用多表的Criteria API，可以自由的组合各种Query形成Join.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testMultiTable() throws SQLException {
		Query<Person> p = QB.create(Person.class);
		Query<Item> i = QB.create(Item.class);
		Join join = QB.innerJoin(p, i, QB.on(Person.Field.name, Item.Field.name));
		// 不指定返回数据的类型时，Join查询默认返回Map对象。
		Map<String, Object> o = db.load(join);
		System.out.println(o);

		// 如果指定返回“多个对象”，那么返回的Object[]中就包含了 Person对象和Item对象
		{
			Object[] objs = db.loadAs(join, Object[].class);
			Person person = (Person) objs[0];
			Item item = (Item) objs[1];

			assertEquals(person.getName(), item.getName());
			System.out.println(person);
			System.out.println(item);
		}

		// 上面的join对象中只有两张表，还可以追加新的表进去
		{
			join.innerJoin(QB.create(Student.class), QB.on(Person.Field.name, Student.Field.name));
			Object[] objs = db.loadAs(join, Object[].class);
			Person person = (Person) objs[0];
			Item item = (Item) objs[1];
			Student student = (Student) objs[2];
			assertEquals(person.getName(), item.getName());
			assertEquals(item.getName(), student.getName());
			System.out.println(student);
		}
	}

	/**
	 * 可以指定从Join中查出哪些字段
	 * @throws SQLException
	 */
	@Test
	public void testSelectFromJoin() throws SQLException {
		Query<Person> p = QB.create(Person.class);
		Query<Item> i = QB.create(Item.class);
		Join join = QB.innerJoin(p, i, QB.on(Person.Field.name, Item.Field.name));

		Selects select = QB.selectFrom(join);
		select.column(Person.Field.id).as("personId");
		select.column(Item.Field.name).as("itemName");

		List<Map<String, Object>> vars = db.select(join, null);
		for (Map<String, Object> var : vars) {
			System.out.println(var);
			// 打印出 {itemname=张飞, personid=1}
		}
	}

	/**
	 * 本案例演示Join的分页查询的两种方法
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testJoinWithPage() throws SQLException {
		Query<Person> p = QB.create(Person.class);
		Join join = QB.innerJoin(p, QB.create(Student.class), QB.on(Person.Field.gender, Student.Field.gender));
		join.orderByDesc(Person.Field.id);
		// 方法1
		{
			int count = db.count(join);
			List<Object[]> result = db.selectAs(join, Object[].class, new PageLimit(3, 5));
			System.out.println("总数:" + count);
			for (Object[] objs : result) {
				System.out.println(Arrays.toString(objs));
			}
		}
		// 方法2
		{
			// 使用分页查询
			Page<Object[]> result = db.pageSelect(join, Object[].class, 5).setOffset(3).getPageData(); // 每页五条,从第四条开始读取
			System.out.println(result.getTotalCount());
			for (Object[] objs : result.getList()) {
				System.out.println(Arrays.toString(objs));
			}
		}
	}

	/**
	 * Join中有多个Query对象，Condition和Order要怎么添加？
	 * 一种做法是将Condition和Order设置在每个Query对象自身上。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testConditionAndOrder1() throws SQLException {
		// 两个Query对象，各自设置条件和
		Query<Person> p = QB.create(Person.class);
		p.addCondition(Person.Field.gender, "M");
		p.orderByAsc(Person.Field.id);

		Query<Student> s = QB.create(Student.class);
		s.addCondition(Student.Field.dateOfBirth, Operator.IS_NOT_NULL, null);
		s.orderByDesc(Student.Field.grade);

		Join join = QB.innerJoin(p, s, QB.on(Person.Field.gender, Student.Field.gender));
		List<Map<String, Object>> result = db.select(join, null);
	}

	/**
	 *  Join中有多个Query对象，Condition和Order要怎么添加？
	 * 另外的做法是，将Condition加在参与Join的任意Query上。
	 * 或者将Order直接添加到join对象上。
	 * @throws SQLException
	 */
	@Test
	public void testConditionAndOrder2() throws SQLException {
		// 把条件集中在第一个Query上。
		Query<Person> p = QB.create(Person.class);
		p.addCondition(Person.Field.gender, "M");
		p.orderByAsc(Person.Field.id);
		p.addCondition(Student.Field.dateOfBirth, Operator.IS_NOT_NULL, null);

		Join join = QB.innerJoin(p, QB.create(Student.class), QB.on(Person.Field.gender, Student.Field.gender));
		// join上也可以直接设置排序字段。
		join.orderByDesc(Student.Field.grade);

		List<Map<String, Object>> result = db.select(join, null);
		System.out.println(result);
	}
	
	/**
	 * Union查询，增加排序条件后
	 * @throws SQLException
	 */
	@Test
	public void testUnion() throws SQLException {
		JoinElement p = QB.create(Person.class);
		p=QB.innerJoin(p, QB.create(School.class));
		
		Selects select = QB.selectFrom(p);
		select.clearSelectItems();
		select.sqlExpression("upper(name) as name");
		select.column(Person.Field.gender);
		select.sqlExpression("'1'").as("grade");
		select.column(School.Field.name).as("schoolName");
		
		Query<Student> s=QB.create(Student.class);
		select = QB.selectFrom(s);
		select.column(Student.Field.name);
		select.column(Student.Field.gender);
		select.column(Student.Field.grade);
		select.sqlExpression("'Unknown'").as("schoolName");
		
		List<Map<String,Object>> result=db.select(QB.unionAll(Map.class,p,s), null);
		System.out.println(result);
	}
	/**
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testUnion2() throws SQLException {
		Query<Person> p = QB.create(Person.class);
		p.orderByDesc(Person.Field.currentSchoolId);
		p.addCondition(Person.Field.gender.isNotNull());
		Selects select = QB.selectFrom(p);
		select.column(Person.Field.name).as("name");
		select.column(Person.Field.gender);
		
		Query<Student> s=QB.create(Student.class);
		s.orderByAsc(Student.Field.grade);
		
		select = QB.selectFrom(s);
		select.column(Student.Field.name);
		select.column(Student.Field.gender);
		List<Student> result=db.select(QB.unionAll(Student.class,p,s).orderByAsc(Student.Field.name),new PageLimit(1,5));
		for(Student st:result){
			System.out.println(st.getName()+":"+st.getGender());	
		}
	}
	@Test
	public void testExists() throws SQLException{
		Query<Person> p=QB.create(Person.class);
		
		//级联功能生效的情况下，查询依然是正确的。此处为了输出更简单的SQL语句暂时关闭级联功能。
		//您可以尝试开启级联功能进行查询
		p.setCascade(false); 
		
		p.addCondition(QB.exists(QB.create(Student.class), 
				QB.on(Person.Field.name, Student.Field.name)));
		System.out.println(db.select(p));
	}
	
	@Test
	public void testNotExists() throws SQLException{
		Query<Person> p=QB.create(Person.class);
		
		p.addCondition(QB.notExists(QB.create(Student.class), 
				QB.on(Person.Field.name, Student.Field.name)));
		System.out.println(db.select(p));
	}
}


