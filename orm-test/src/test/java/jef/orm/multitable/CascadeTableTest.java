package jef.orm.multitable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Or;
import jef.database.PagingIterator;
import jef.database.QB;
import jef.database.Session;
import jef.database.Transaction;
import jef.database.meta.FBIField;
import jef.database.query.JpqlExpression;
import jef.database.query.Query;
import jef.database.query.RefField;
import jef.database.query.Selects;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.multitable.model.Person;
import jef.orm.multitable.model.School;
import jef.orm.multitable.model.Score;
import jef.script.javascript.Var;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.string.RandomData;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 级联操作相关测试
 * 
 * @author Administrator
 * 
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ 
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"), 
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"), 
	@DataSource(name = "derby", url = "jdbc:derby:./db;create=true"),
	@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	@DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
})
public class CascadeTableTest extends MultiTableTestBase {

	@BeforeClass
	public static void setUp() throws SQLException {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("jef.orm");
	}

	@DatabaseInit
	public void prepareData() throws SQLException {
		dropTable();
		createtable();
		initData();
		testInserPerson(db);
	}

	/**
	 * 针对一个带有级联关系的对象，实现指定选择列的查询
	 * 
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testAssignSelectColumn() throws SQLException {
		db.createTable(Person.class);
		Session db = this.db.startTransaction();
		Person p = RandomData.newInstance(Person.class);
		p.setGender("F");
		p.setAge(19);
		db.insert(p);
		System.out.println("===========testAssignSelectColumn begin==============");
		{
			Query<Person> t1 = QB.create(Person.class);
			// 只选择指定的列
			t1.setCascadeViaOuterJoin(false);
			Selects select = QB.selectFrom(t1);
			// select.clearSelectItems();
			select.guessColumn("schoolId");
			// select.column(School.Field.name);

			select.guessColumn("schoolName");
			select.columns(t1, "name,age,cell");
			select.column(t1, "id");
			List<Person> result = db.select(t1);
			LogUtil.show(result.get(0));
		}
		{
			Query<Person> t1 = QB.create(Person.class);
			// 只选择指定的列
			// t1.setAutoOuterJoin(false);
			Selects select = QB.selectFrom(t1);
			// select.clearSelectItems();
			select.guessColumn("schoolId");
			// select.column(School.Field.name);

			select.guessColumn("schoolName");
			select.columns(t1, "name,age,cell");
			select.column(t1, "id");
			List<Person> result = db.select(t1);
			LogUtil.show(result.get(0));
		}
		{
			Query<Person> t1 = QB.create(Person.class);
			// 只选择指定的列
			// t1.setAutoOuterJoin(false);
			Selects select = QB.selectFrom(t1);
			// select.clearSelectItems();
			select.guessColumn("schoolId");
			// select.column(School.Field.name);

			select.guessColumn("schoolName");
			select.columns(t1, "name,age,cell");
			select.column(t1, "id");
			t1.setCascade(false);
			List<Person> result = db.select(t1.getInstance());
			LogUtil.show(result.get(0));
		}
		db.close();
		System.out.println("===========result==============");
	}

	@Test
	public void testSelectWithFunction() throws SQLException {
		Query<Person> t1 = QB.create(Person.class);
		// 只选择指定的列
		t1.addCondition(new FBIField("MOD(age, 10)", t1), 2);
		t1.setCascade(false);
		List<Person> map = db.select(t1.getInstance());
	}
	
	@Test
	public void testSelectSimple() throws SQLException {
		Person person=new Person();
		person.setId(12);
		person=db.load(person);
	}

	/**
	 * 支持Distinct的查询()
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectDistinct() throws SQLException {
		Query<Person> t1 = QB.create(Person.class);
		// 只选择指定的列
		Selects select = QB.selectFrom(t1);
		select.clearSelectItems();
		select.setDistinct(true);
		select.column(Person.Field.schoolId);
		// select.guessColumn("schoolId",null);
		select.columns(t1, "name,age,cell");
		select.column(t1, "id");
		t1.setCascade(false);
		List<Person> map = db.select(t1.getInstance());
		LogUtil.show(map.get(0));
	}
	
	@Test
	public void testSelect11() throws SQLException {
		
		Person p=new Person();
		p.setId(1);
		p.getQuery().setCascade(false);
		List<Person> map = db.select(p);
		LogUtil.show(map);
	}
	

	/**
	 * 插入Person表
	 * 
	 * @throws SQLException
	 */
	private void testInserPerson(Session db) throws SQLException {
		Person p1 = new Person();
		p1.setAge(22);
		p1.setBirthDay(new Date());
		p1.setCell("135gg876");
		p1.setGender("M");
		p1.setHomeTown("BEIJING");
		p1.setLastModified(new Date());
		p1.setName("爸爸" + StringUtils.randomString());
		p1.setSchoolId(2);
		p1.setPhone("(083-2233)88778800");
		db.insert(p1);

		Person p2 = new Person();
		p2.setAge(22);
		p2.setBirthDay(new Date());
		p2.setCell("13506877");
		p2.setGender("M");
		p2.setHomeTown("BEIJING");
		p2.setLastModified(new Date());
		p2.setName("JinWang" + StringUtils.randomString());
		p2.setPhone("(083-87ss0");
		p2.setSchoolId(3);
		// p2.setPhoto(new File("c:/NTDETECT.COM"));
		db.insert(p2);

		Person p3 = new Person();
		p3.setAge(22);
		p3.setBirthDay(new Date());
		p3.setCell("1350ff876");
		p3.setGender("M");
		p3.setHomeTown("BEIJING");
		p3.setLastModified(new Date());
		p3.setName("儿子" + StringUtils.randomString());
		p3.setPhone("(083-28800)");
		p3.setSchoolId(2);
		p3.setParentId(1);
		db.insert(p3);

		assertEquals(1, p1.getId().intValue());
		assertEquals(2, p2.getId().intValue());
		assertEquals(3, p3.getId().intValue());
	}


	private void checkResult1(List<Person> result) {
		assertEquals(2, result.size());
		Person p1 = result.get(0);
		assertTrue(p1.getName().startsWith("爸爸"));
		assertTrue(p1.getScores().size() == 6);
		assertEquals(80, CollectionUtil.findFirst(p1.getScores(), "subject", "英语").getScore());
		assertEquals(90, CollectionUtil.findFirst(p1.getScores(), "subject", "物理").getScore());
		assertEquals(50, CollectionUtil.findFirst(p1.getScores(), "subject", "电脑").getScore());
		assertEquals(70, CollectionUtil.findFirst(p1.getScores(), "subject", "算数").getScore());
		assertEquals(60, CollectionUtil.findFirst(p1.getScores(), "subject", "语文").getScore());
		assertEquals(100, CollectionUtil.findFirst(p1.getScores(), "subject", "化学").getScore());
		assertTrue(p1.getSchoolId() == 2);
		assertEquals("战国高校", p1.getSchoolName());
		assertEquals(p1.getSchoolName(), p1.getSchool().getName());
		assertEquals(p1.getSchoolId(), p1.getSchool().getId());
		assertEquals(2, p1.getFriends().size());
		assertEquals(p1.getFriendComment()[0], p1.getFriends().get(0).getComment());
		assertTrue(p1.getFriends().get(0).getFriend().getName().startsWith("JinWang"));
		assertTrue(p1.getFriends().get(1).getFriend().getName().startsWith("儿子"));
		p1.getParentName();
	}

	/**
	 * 将多个对象的条件刚合并在一个Query对象中
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn(allButExcept="hsqldb")
	public void testConditionInOneObj() throws SQLException {
		System.out.println("=========== testConditionInOneObj Begin ==========");
		long count=db.getSqlTemplate(null).countBySql("select count(*) from person_table where age>16 and schoolId=2");
		
		assertEquals(2, (int) count);
				
		Person q = new Person();
		q.getQuery().addCondition(Person.Field.age, Operator.GREAT, 16);
		q.getQuery().addCondition(School.Field.id, 2);// 凡是引用一个其他表的条件要用RefField包裹
		q.getQuery().setCascadeViaOuterJoin(true);
		List<Person> result = db.select(q);
		checkResult1(result);
		System.out.println("=========== testConditionInOneObj End ==========");
	}

	/**
	 * 当分属多个对象的条件之间要实现Or,And等复合关系时：
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testOrAndConditionWithDiffQueryObj() throws SQLException {
		System.out.println("=========== testOrAndConditionWithDiffQueryObj Begin ==========");
		Person q = new Person();
		And and = new And();
		and.addCondition(Person.Field.age, Operator.GREAT, 6);
		and.addCondition(new RefField(School.Field.id), 1);

		Or or = new Or();
		or.addCondition(and);
		or.addCondition(Person.Field.age, Operator.GREAT_EQUALS, 99);

		q.getQuery().addCondition(or);

		List<Person> result = db.select(q);
		for (Person p : result) {
			printPerson(p);
		}
		System.out.println("=========== testOrAndConditionWithDiffQueryObj End ==========");
	}

	/**
	 * 测试完整的JPQL查询语句
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testJPQL() throws SQLException {
		System.out.println("=========== testJPQL Begin ==========");
		LogUtil.show(db.getSqlTemplate(null).selectByJPQL("select t.name,t.cell,pp.friendId,p2.name from person t,personFriends pp,person p2 where t.id=pp.pid and pp.friendId=p2.id",Var.class,null));
		System.out.println("=========== testJPQL End ==========");
	}

	/**
	 * 测试级联对象的插入操作
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testRefInsert() throws SQLException {
		System.out.println("=========== testRefInsert Begin ==========");
		Person p = new Person();
		p.setName("张三");
		p.setSchool(new School("浙江大学"));
		p.setAge(22);
		db.insertCascade(p);
		System.out.println(p.getId());

		Person query = new Person();
		query.setId(p.getId());
		query = db.load(query);
		assertEquals("浙江大学", query.getSchoolName());
		System.out.println("=========== testRefInsert End ==========");
	}

	/**
	 * 测试级联的更新操作
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testRefUpdate() throws SQLException {
		System.out.println("=========== testRefUpdate Begin ==========");
		Person p = new Person();
		p.setId(2);
		p = db.load(p);
		//
		p.setSchoolId(0);
		p.setSchool(new School("华南大学"));
		p.setAge(123);
		db.updateCascade(p);
		System.out.println("=========== testRefUpdate End ==========");
	}

	/**
	 * 测试JpqlExpression表达式的使用
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testExpression1() throws SQLException {
		System.out.println("=========== testExpression1 Start ==========");
		Query<Person> p = QB.create(Person.class);
		p.addCondition(new FBIField("upper(name)||str(age)", p), new JpqlExpression("upper(name)||'22'", p));
		List<Person> ps = db.select(p);
		assertTrue(ps.size() > 0);
		System.out.println("=========== testExpression1 End ==========");
	}

	/**
	 * 测试FBI Field,使用JPQL表达式
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testExpression2() throws SQLException {
		System.out.println("=========== testExpression2 Start ==========");
		Person p = new Person();
		p.getQuery().addCondition(new FBIField("upper(cell)||str(age)", p), "135GG87622");
		List<Person> ps = db.select(p);
		assertTrue(ps.size() > 0);
		System.out.println("=========== testExpression2 End ==========");
	}

	/**
	 * 在一对多的关联中使用对子表的过滤条件
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testFilterInOneToManyRef() throws SQLException {
		System.out.println("=========== testFilterInOneToManyRef Start ==========");
		// 无过滤条件的
		Person p1 = new Person();
		p1.getQuery().addCondition(Person.Field.id, 1);
		Person result = db.load(p1);
		assertEquals(6, result.getScores().size());

		// 添加过滤条件的
		Person p = new Person();
		p.getQuery().addCondition(Person.Field.id, 1);
		p.getQuery().addCascadeCondition(QB.in(Score.Field.subject, new String[] { "语文", "化学", "英语" }));

		result = db.load(p);
		assertEquals(3, result.getScores().size());
		System.out.println("=========== testFilterInOneToManyRef End ==========");
	}

	/**
	 * 测试使用复杂的子查询过滤条件
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testComplextFilterCondition() throws SQLException {
		System.out.println("=========== testComplextFilterCondition End ==========");
		//
		// 添加过滤条件的
		Person p = new Person();
		p.getQuery().addCondition(Person.Field.id, 1);

		Condition or = QB.or(QB.eq(Score.Field.subject, "语文"), QB.eq(Score.Field.subject, "化学"), QB.eq(Score.Field.subject, "英语"));
		p.getQuery().addCascadeCondition("scores", or);
		Person result = db.load(p);
		System.out.println("loaded");
		assertEquals(3, result.getScores().size());

		System.out.println("=========== testComplextFilterCondition End ==========");
	}

	/**
	 * 在复杂条件中使用REF字段，并且测试在对一情况下，FilterField自动转换为RefField.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testComplextRefCondition() throws SQLException {
		System.out.println("=========== testComplextRefCondition Start ==========");
		Person q = new Person();
		q.getQuery().addCondition(Person.Field.age, Operator.GREAT, 16);

		Or or = new Or();
		or.addCondition(new RefField(School.Field.id), 2);
		or.addCondition(new RefField(School.Field.name), Operator.MATCH_ANY, "国");
		q.getQuery().addCondition(or);// 凡是引用一个其他表的条件要用RefField包裹
		q.getQuery().addCascadeCondition(QB.eq(School.Field.name, "战国高校"));

		List<Person> result = db.select(q);
		assertEquals(2, result.size());
		System.out.println("=========== testComplextRefCondition End ==========");
	}

	/**
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect() throws SQLException {
		System.out.println("=========== testSelect Begin ==========");
		Query<Person> p = QB.create(Person.class);
		List<Person> ps = db.select(p, new IntRange(1, 2));
		assertTrue(ps.size() > 0);
		System.out.println("=========== testSelect End ==========");
	}

	/**
	 * 级联查询，驱动表的选择字段指定，其他表自动
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectColumnsInCascade() throws SQLException {
		Query<Person> t1 = QB.create(Person.class);
		// 只选择指定的列
		Selects select = QB.selectFrom(t1);
		select.column(t1, "schoolId");
		select.columns(t1, "name,age,cell");
		select.column(t1, "id");
		List<Person> map = db.select(t1.getInstance());

		Person p = map.get(0);
		assertTrue(!p.getScores().isEmpty());
		assertTrue(!p.getFriends().isEmpty());
		for (Score score : p.getScores()) {
			assertNotNull(score.getTestTime());
		}
		System.out.println("===========result:" + map.size() + "==============");
	}

	@Test
	public void testPageSql() throws SQLException, IOException {
		String sql = "select * from person_table xx where gender='M'";
		PagingIterator<Person> pagingIterator = db.getSqlTemplate(null).pageSelectBySql(sql, Person.class, 10);
		System.out.println(pagingIterator.getTotal());
	}

	/**
	 * 测试分组
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGroup() throws SQLException {
		Transaction db=this.db.startTransaction();
		Person p = RandomData.newInstance(Person.class);
		p.setGender("F");
		p.setAge(19);
		db.insert(p);
		Query<Person> t1 = QB.create(Person.class);
		
		Selects select = QB.selectFrom(t1);
		select.column(Person.Field.gender).group();
		select.column(Person.Field.id).count().as("count");
		select.column(Person.Field.age).min().as("minAge");
		select.column(Person.Field.age).max().as("maxAge").having(Operator.GREAT, 0);
		List<Map> map = db.selectAs(t1, Map.class);
		db.rollback(true);
		LogUtil.show(map);
	}
	
	
	@Test
	public void testInsertNormal() throws SQLException{
		Transaction db=this.db.startTransaction();
		List<School> schools=db.selectAll(School.class);
		School s=schools.get(0);
		Person p=new Person();
		p.setAge(12);
		p.setBirthDay(new Date());
		p.setCell("123433454");
		p.setFriendComment(new String[]{"AA"});
		p.setGender("M");
		p.setLastModified(new Date());
		p.setParentId(1);
		p.setSchool(s);
		p.setSchoolId(3);
		System.out.println(s);
		db.insert(p);
		db.rollback(true);
	}
	@Test
	public void testInsertCascade() throws SQLException{
		Transaction db=this.db.startTransaction();
		List<School> schools=db.selectAll(School.class);
		School s=schools.get(0);
		Person p=new Person();
		p.setAge(12);
		p.setBirthDay(new Date());
		p.setCell("123433454");
		p.setFriendComment(new String[]{"AA"});
		p.setGender("M");
		p.setLastModified(new Date());
		p.setParentId(1);
		p.setSchool(s);
		p.setSchoolId(3);
		db.insertCascade(p);
		System.out.println(p.getSchoolId());
		db.rollback(true);
	}
	@Test
	public void testUpdateNormal() throws SQLException{
		Transaction db=this.db.startTransaction();
		List<School> schools=db.selectAll(School.class);
		School s=schools.get(0);
		
		Person p=new Person();
		p.setId(2);
		p=db.load(p);
		System.out.println(p);
		
		System.out.println(p.getSchoolId());
		System.out.println(p.getSchool());
		p.setSchool(s);
		
		db.update(p);
		db.rollback(true);
	}
	@Test
	public void testUpdateCascade() throws SQLException{
		Transaction db=this.db.startTransaction();
		List<School> schools=db.selectAll(School.class);
		School s=schools.get(0);
		
		Person p=new Person();
		p.setId(2);
		p=db.load(p);
		System.out.println(p);
		
		System.out.println(p.getSchoolId());
		System.out.println(p.getSchool());
		p.setSchool(s);
		
		db.updateCascade(p);
		System.out.println(p);
		db.rollback(true);
	}
	@Test
	public void testDeleteNormal() throws SQLException{
		Transaction db=this.db.startTransaction();
		Person p=new Person();
		p.setId(1);
		db.delete(p);
		db.rollback(true);
	}
	@Test
	public void testDeleteCascade() throws SQLException{
		Transaction db=this.db.startTransaction();
		Person p=new Person();
		p.setId(3);
		System.out.println(p.getScores());
		db.deleteCascade(p);
		db.rollback(true);
	}
}
