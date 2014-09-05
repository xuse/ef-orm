package org.easyframe.tutorial.lesson8;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;

import jef.codegen.EntityEnhancer;
import jef.common.Entry;
import jef.common.wrapper.Holder;
import jef.database.DbClient;
import jef.database.NativeQuery;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.Session.PopulateStrategy;
import jef.database.query.Join;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.wrapper.populator.Mapper;
import jef.database.wrapper.populator.Mappers;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.result.IResultSet;
import jef.tools.DateUtils;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson4.entity.DataDict;
import org.easyframe.tutorial.lesson4.entity.Person;
import org.easyframe.tutorial.lesson4.entity.School;
import org.easyframe.tutorial.lesson5.entity.Item;
import org.junit.BeforeClass;
import org.junit.Test;

public class Case1 extends org.junit.Assert {
	private static DbClient db;

	/**
	 * 测试数据准备
	 * 
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db = new DbClient();
		ORMConfig.getInstance().setDebugMode(false);
		db.dropTable(Person.class, Item.class, Student.class, School.class, DataDict.class);
		db.createTable(Person.class, Item.class, Student.class, School.class, DataDict.class);
		DataDict dict1 = new DataDict("USER.GENDER", "M", "男人");
		DataDict dict2 = new DataDict("USER.GENDER", "F", "女人");
		db.batchInsert(Arrays.asList(dict1, dict2));

		Person p = new Person();
		p.setGender('M');
		p.setName("张飞");
		p.setCurrentSchool(new School("成都大学"));
		db.insertCascade(p);

		p = new Person();
		p.setGender('M');
		p.setName("关羽");
		p.setCurrentSchool(new School("襄阳大学"));
		db.insertCascade(p);

		p = new Person();
		p.setGender('M');
		p.setName("刘备");
		p.setCurrentSchoolId(1);
		db.insert(p);

		Item item = new Item();
		item.setName("张飞");
		item.setCatalogyId(12);
		db.insert(item);

		Student st = new Student();
		st.setName("张飞");
		st.setDateOfBirth(DateUtils.getDate(1984, 7, 1));
		st.setGender("F");
		st.setGrade("3");
		db.insert(st);

		st = new Student();
		st.setName("关羽");
		st.setDateOfBirth(DateUtils.getDate(1980, 2, 1));
		st.setGender("F");
		st.setGrade("2");
		db.insert(st);
		ORMConfig.getInstance().setDebugMode(true);
	}

	/**
	 * 演示特性：8.1.1 返回简单对象 返回简单类型的结果
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_returnSimpleType() throws SQLException {
		// 返回String
		Query<Person> query = QB.create(Person.class);
		Selects select = QB.selectFrom(query);
		select.column(Person.Field.gender);
		List<String> result = db.selectAs(query, String.class);
		System.out.println(result);

		// 返回data
		NativeQuery<Date> nq = db.createNativeQuery("select created from t_person", Date.class);
		List<Date> results = nq.getResultList();
		System.out.println(results);

		// 返回数字
		List<Integer> result2 = db.selectBySql("select count(*) from t_person group by gender", Integer.class);
		System.out.println(result2);
	}

	/**
	 * 演示特性 8.1.2 返回和查询表匹配的对象
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_returnTableObject() throws SQLException {
		List<Person> persons = db.select(QB.create(Person.class));
		assertEquals(3, persons.size());
	}

	/**
	 * 演示特性 8.1.3 返回任意容器对象
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_otherDataObject1() throws SQLException {
		// 普通查询，查询结果改用自定义的类PersonResult来包装.
		Query<Person> q1 = QB.create(Person.class);
		List<PersonResult> result = db.selectAs(q1, PersonResult.class);
		PersonResult first = result.get(0);
		// Person表中查出的current_school_id字段无用处，被丢弃。
		// PersonResult中的字段birthday不存在，不赋值
		System.out.println(ToStringBuilder.reflectionToString(first));

		// 用NativeQuery来查询Person表，查询结果包装为PersonResult
		String sql = "select t.*,sysdate as birthday from t_person t";
		NativeQuery<PersonResult> q = db.createNativeQuery(sql, PersonResult.class);
		PersonResult p = q.getSingleResult();
		System.out.println(ToStringBuilder.reflectionToString(p));
	}

	public static class PersonResult {
		@Column(name = "person_name")
		private String personName;
		private String id;
		private String gender;
		private Date birthday;
		private int age;

		public String getPersonName() {
			return personName;
		}

		public void setPersonName(String personName) {
			this.personName = personName;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public Date getBirthday() {
			return birthday;
		}

		public void setBirthday(Date birthday) {
			this.birthday = birthday;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	/**
	 * 演示特性 8.1.3 返回任意容器对象
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_otherDataObject2() throws SQLException {
		// 用Person表查出Student对象。
		{
			Query<Person> query = QB.create(Person.class);
			Selects select = QB.selectFrom(query);
			select.columns("id, name as name, gender, '3' as grade, created as dateOfBirth");
			List<Student> result = db.selectAs(query, Student.class);
			Student first = result.get(0);
			assertNotNull(first.getGender());
			assertNotNull(first.getGrade());
			assertNotNull(first.getName());
			assertNotNull(first.getDateOfBirth());
			assertNotNull(first.getId());
		}
		// 用Person表查出Student对象。(这种写法虽然啰嗦，但是可以利用java编译器检查字段的正确性)
		{
			Query<Person> query = QB.create(Person.class);
			Selects select = QB.selectFrom(query);
			select.column(Person.Field.id);
			select.column(Person.Field.name).as("name");
			select.column(Person.Field.gender);
			select.sqlExpression("'3'").as("grade");
			select.column(Person.Field.created).as("dateOfBirth");
			List<Student> result = db.selectAs(query, Student.class);
			System.out.println(result);
			Student first = result.get(0);
			assertNotNull(first.getGender());
			assertNotNull(first.getGrade());
			assertNotNull(first.getName());
			assertNotNull(first.getDateOfBirth());
			assertNotNull(first.getId());
		}
		// 用NativeSQL做到上一点
		{
			String sql = "select id,person_name as name, gender, '3' as grade, created as date_of_birth from t_person";
			NativeQuery<Student> nq = db.createNativeQuery(sql, Student.class);
			List<Student> result = nq.getResultList();
			Student first = result.get(0);
			assertNotNull(first.getGender());
			assertNotNull(first.getGrade());
			assertNotNull(first.getName());
			assertNotNull(first.getDateOfBirth());
			assertNotNull(first.getId());
			System.out.println(result);
		}
	}

	/**
	 * 演示特性 8.1.3 返回任意容器对象
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_otherDataObject3() throws SQLException {
		String sql = "select gender as \"key\",count(*) as value from t_person group by gender";
		List<Entry> results = db.selectBySql(sql, jef.common.Entry.class);
		System.out.println(results);
	}

	/**
	 * 特性演示 8.1.6 多个列以数组形式返回
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSlelectSimpleValueArray() throws SQLException {
		NativeQuery<Object[]> query = db.createNativeQuery("select 'Asa' as  a ,'B' as b,1+1 as c, current_timestamp as D from student", Object[].class);
		Object[] result = query.getSingleResult();
		assertTrue(result[1].getClass() == String.class);
		assertTrue(Number.class.isAssignableFrom(result[2].getClass()));
		assertTrue(result[3].getClass() == Timestamp.class);
	}

	/**
	 * 特性演示 8.1.6 多个列以数组形式返回 用数组返回结果。每个元素代表一列的值
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_columnAsArray() throws SQLException {
		String sql = "select t1.id,person_name,gender,t2.name from t_person t1," + "school t2 where t1.current_school_id=t2.id";
		{
			NativeQuery<String[]> q1 = db.createNativeQuery(sql, String[].class);
			// 每个列的值被转换为String，每行记录变为一个String[]。
			for (String[] array : q1.getResultList()) {
				System.out.println(Arrays.toString(array));
			}
		}
		{
			NativeQuery<Object[]> q2 = db.createNativeQuery(sql, Object[].class);
			for (Object[] array : q2.getResultList()) {
				// 每个列的值被保留了其原始类型，每行记录变为一个Object[]。
				System.out.println(Arrays.toString(array));
			}
		}
		{
			// 单表查询API也是可以的
			Query<Person> q = QB.create(Person.class);
			Selects sel = QB.selectFrom(q);
			sel.columns(Person.Field.id, Person.Field.name, Person.Field.gender);
			List<Object[]> persons = db.selectAs(q, Object[].class);
			for (Object[] array : persons) {
				System.out.println(Arrays.toString(array));
			}
		}
	}

	/**
	 * 章节 8.1.1 直接指定返回类型 使用ResultTransformer来指定返回类型，
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResult_transformer1() throws SQLException {
		{
			Query<Person> q = QB.create(Person.class);
			Selects sel = QB.selectFrom(q);
			sel.columns(Person.Field.id, Person.Field.name, Person.Field.gender);
			q.getResultTransformer().setResultType(String[].class);
			String[] result = db.load(q);
			System.out.println(Arrays.toString(result));
		}
		{
			Query<Person> query = QB.create(Person.class);
			Selects select = QB.selectFrom(query);
			select.column(Person.Field.id);
			select.column(Person.Field.name).as("name");
			select.column(Person.Field.gender);
			select.sqlExpression("'3'").as("grade");
			select.column(Person.Field.created).as("dateOfBirth");
			query.getResultTransformer().setResultType(Student.class);
			Student st = db.load(query);
		}
	}

	/**
	 * 特性演示 8.2.2 区分两种返回数据的规则
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_columnAsArray2() throws SQLException {
		// 多表查询的时候,Object[]的返回类型默认是一张表的多个列拼成的对象作为数组元素，
		// 而不是每个列作为数组元素……
		Query<Person> q = QB.create(Person.class);
		Join join = QB.innerJoin(q, QB.create(School.class));
		List<Object[]> persons = db.selectAs(join, Object[].class);
		for (Object[] array : persons) {
			System.out.println("[" + array[0].getClass() + "," + array[1].getClass() + "]");
		}
		// 可以这样写
		join.getResultTransformer().setStrategy(PopulateStrategy.COLUMN_TO_ARRAY);
		persons = db.selectAs(join, Object[].class);
		for (Object[] array : persons) {
			System.out.println(Arrays.toString(array));
		}
		// 这样,多表查询也可以以String[]形式返回值了
		join.getResultTransformer().setStrategy(PopulateStrategy.COLUMN_TO_ARRAY);
		List<String[]> stringColumns = db.selectAs(join, String[].class);
		for (String[] array : stringColumns) {
			System.out.println(Arrays.toString(array));
		}
	}

	/**
	 * 特性演示 8.2.3 忽略@Column注解
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testResultType_ignoreColumnAnnotation() throws SQLException {
		String sql = "select id as id, name as name, gender as gender from student";
		// 将student表中查出的数据映射为Person对象。
		NativeQuery<Person> nq = db.createNativeQuery(sql, Person.class);
		// 提示结果转换器，忽略@Column注解。
		nq.getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);

		Person person = nq.getSingleResult();
		assertNotNull(person.getName());
	}
	
	
	
	
	/**
	 * 特性演示 8.2.4 自定义返回结果
	 */
	@Test
	public void testResultMapper_1() throws SQLException{
		Query<Student> q=QB.create(Student.class);
		q.getResultTransformer().addMapper(new Mapper<PersonResult>(){
			@Override
			protected void transform(PersonResult obj, IResultSet rs) throws SQLException {
				obj.setBirthday(rs.getDate("DATE_OF_BIRTH"));
				obj.setPersonName(rs.getString("NAME"));
				if(obj.getBirthday()!=null){
					//计算并设置年龄
					int year=DateUtils.getYear(new Date());
					obj.setAge(year-DateUtils.getYear(obj.getBirthday()));
				}
			}
		});
		PersonResult result = db.loadAs(q,PersonResult.class);
		System.out.println(result.getPersonName()+"出生于"+result.getBirthday()+" 今年"+result.getAge()+"岁");
	}
	
	
	/**
	 * 特性演示 8.2.4 自定义返回结果2
	 */
	@Test
	public void testResultMapper_2() throws SQLException{
		String sql="select t1.* , t2.* from t_person t1,school t2 " +
				"where t1.current_school_id=t2.id ";
		NativeQuery<Person> query = db.createNativeQuery(sql,Person.class);
		query.getResultTransformer().addMapper(Mappers.toResultProperty("currentSchool", School.class));
		
		List<Person> result=query.getResultList();
		for(Person person: result){
			System.out.println(person.toString()+" ->"+person.getCurrentSchool());
		}
	}
	
	
	/**
	 * 特性演示 8.2.4 自定义返回结果
	 */
	@Test
	public void testResultMapper_2_1() throws SQLException{
		
		//由于两张表都有id列，这里必须重命名 
		String sql="select t1.* , t2.id as schoolid, t2.name from t_person t1,school t2 " +
				"where t1.current_school_id=t2.id ";
		NativeQuery<Person> query = db.createNativeQuery(sql,Person.class);
		
		//Mappers可以提供一些符合框架默认行为的映射器。
		//因为schooldid是重命名的，所以不是框架的默认行为，于是要再adjust一下
		query.getResultTransformer().addMapper(
				Mappers.toResultProperty("currentSchool", School.class).adjust("id", "schoolid"));
		List<Person> result=query.getResultList();
		for(Person person: result){
			System.out.println(person.toString()+" ->"+person.getCurrentSchool());
		}
		
		Person zhangfei=result.get(0);
		Person liubei=result.get(2);
		
		assertEquals(zhangfei.getCurrentSchoolId(),liubei.getCurrentSchoolId());
		assertNotSame(zhangfei.getId(), liubei.getId());
		assertEquals(zhangfei.getCurrentSchool().getId(),liubei.getCurrentSchool().getId());
		assertEquals(zhangfei.getCurrentSchool().getName(),liubei.getCurrentSchool().getName());
	}
	
	
	/**
	 * 特性演示 8.2.4 自定义返回结果
	 */
	@Test
	public void testResultMapper_3() throws SQLException{
		Query<Person> t1 = QB.create(Person.class);
		Query<Student> t2 = QB.create(Student.class);

		Join join = QB.innerJoin(t1, t2, QB.on(Person.Field.name, Student.Field.name));
		Transformer transformer=join.getResultTransformer();
		
		//因为是两表查询，默认返回的数组长度为2，为了增加一个返回对象需要将数组长度调整为3
		transformer.setResultTypeAsObjectArray(3);
		transformer.addMapper(Mappers.toArrayElement(0, Student.class, "T2"));
		transformer.addMapper(Mappers.toArrayElement(1, Person.class, "T1"));
		
		//增加一个自定义的映射。
		transformer.addMapper(new Mapper<Object[]>() {
			@Override
			protected void transform(Object[] obj, IResultSet rs) throws SQLException {
				PersonResult result=new PersonResult();
				result.setPersonName(rs.getString("T1__NAME"));
				result.setBirthday(rs.getDate("T1__CREATED"));
				obj[2]=result;
			}
		});
		List<Object[]> result = db.select(join, null);
		assertNotNull(result.get(0)[2]);
		
		
		//第二段案例: 清除映射器、忽略默认的映射规则
		{
			Transformer t=join.getResultTransformer();
			t.setResultType(Holder.class);
			
			//清除之前定义的映射器，因为之前已经在Transformer中添加了映射器。
			t.clearMapper();
			//忽略默认的映射规则
			t.ignoreAll(); 
			t.addMapper(new Mapper<Holder<PersonResult>>(){
				@Override
				protected void transform(Holder<PersonResult> obj, IResultSet rs) throws SQLException {
					PersonResult result=new PersonResult();
					result.setPersonName(rs.getString("T1__NAME"));
					result.setBirthday(rs.getDate("T1__CREATED"));
					obj.set(result);
				}
			});
			List<Holder<PersonResult>> holders=db.select(join,null);
			for(Holder<PersonResult> h: holders){
				System.out.println(h.get());
			}
		}
	}
}
