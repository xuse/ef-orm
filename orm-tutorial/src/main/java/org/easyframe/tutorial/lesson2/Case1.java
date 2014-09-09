package org.easyframe.tutorial.lesson2;

import java.sql.SQLException;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.query.Query;
import jef.database.query.QueryBuilder;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson2.entity.StudentToLession;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Case1 {

	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db = new DbClient();
		db.dropTable(Student.class,StudentToLession.class);
		db.createTable(Student.class,StudentToLession.class);
	}

	@AfterClass
	public static void close() {
		if (db != null)
			db.close();
	}

	@Test
	public void testCreateTable() throws SQLException {
		db.dropTable(StudentToLession.class);
		db.createTable(StudentToLession.class);
	}

	/**
	 * 自增主键的返回
	 * 
	 * @throws SQLException
	 */
	@Test
	public void studentAutoIncreament() throws SQLException {
		Student s = new Student();
		s.setName("Jhon Smith");
		s.setGender("M");
		s.setGrade("2");
		db.insert(s);

		Assert.assertTrue(s.getId() > 0);
		System.out.println("自增键值为：" + s.getId());
	}

	/**
	 * 查询示例
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testLoadAndSelect() throws SQLException {
		// 创建一个模板对象,当模板对象主键有值时，按模板对象的主键查询。
		{
			Student query = new Student();
			query.setId(3);
			Student st = db.load(query);
		}
		// 直接按主键查询,当对象为复合主键时,可传入多个键值。
		// 键值数量必须和复合主键的数量一致，顺序按field在emun Field中的出现顺序。
		{
			Student st = db.load(Student.class, 3);
			StudentToLession stl = db.load(StudentToLession.class, 3, 1);
		}
		// load方法都是查询单值的，select方法可以查询多值
		// 创建一个模板对象,当模板对象的字段有值时，按这些字段查询
		// 类似于某H框架的 findByExample()。
		{
			Student query = new Student();
			query.setGender("F");
			query.setGrade("2");
			List<Student> sts = db.select(query);// 查出所有Gender='F' and
													// grade='2'的记录。
		}
		// 如果一个对象的复合主键没有全部赋值的情况，那么也当做普通字段对待
		// 最终效果和findByExample()一样。
		{
			StudentToLession query = new StudentToLession();
			query.setLessionId(1);
			List<StudentToLession> sts = db.select(query);// 查出所有lessionId='1'的记录。

		}
		// 如果一个对象的主键都赋了值，非主键字段也赋值。那么非主键字段不会作为查询条件
		// 因为框架认为主键字段足够定位记录，所以非主键不用作查询条件。
		{
			Student query = new Student();
			query.setGrade("1");
			query.setId(12);
			List<Student> sts = db.select(query); // 查询条件为 id=12。grade不用作查询条件
		}
	}

	/**
	 * 带有Like运算符的条件
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_Like() throws SQLException {
		Student s = new Student();
		// 在Student对象中添加Like条件
		s.getQuery().addCondition(Student.Field.name, Operator.MATCH_ANY, "Jhon");
		List<Student> sts = db.select(s);

		Assert.assertEquals(sts.size(), db.count(s.getQuery()));
	}

	/**
	 * 更为复杂的查询1
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_LikeAndEtc() throws SQLException {
		Student s = new Student();
		s.getQuery()
			.addCondition(Student.Field.name, Operator.MATCH_ANY, "Jhon")
			.addCondition(Student.Field.id, Operator.LESS, 100)
			.orderByDesc(Student.Field.grade);
		List<Student> sts = db.select(s);
		Assert.assertEquals(sts.size(), db.count(s.getQuery()));
	}

	/**
	 * 更为复杂的查询2、增加一个条件：正确写法
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_LikeAndEtc2() throws SQLException {
		Student s = new Student();

		// s.setGrade("3"); //在已经使用了Query对象中的情况下，此处设值不作为查询条件

		s.getQuery().addCondition(Student.Field.grade, "3"); // 添加
																// grade='3'这个条件。当运算符为
																// =
																// 时，中间的运算符可以省略不写。

		s.getQuery().addCondition(Student.Field.name, Operator.MATCH_ANY, "Jhon");
		s.getQuery().addCondition(Student.Field.id, Operator.LESS, 100);
		s.getQuery().orderByDesc(Student.Field.grade);
		List<Student> sts = db.select(s);

		Assert.assertEquals(sts.size(), db.count(s.getQuery()));
	}

	/**
	 * 使用Like条件和in条件的更新和删除
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testUpdateAndDelete_WithLike() throws SQLException {
		Student s = new Student();
		s.setGender("F");
		s.getQuery().addCondition(Student.Field.name, Operator.MATCH_ANY, "Mary");

		db.update(s);
		// 相当于执行
		// update STUDENT set GENDER = 'F' where NAME like '%Mary%'

		s.getQuery().clearQuery();// 清除查询条件
		s.getQuery().addCondition(Student.Field.id, Operator.IN, new int[] { 2, 3, 4 });
		db.delete(s);
		// 相当于执行
		// delete from STUDENT where ID in (2, 3, 4)
	}

	/**
	 * 更新对象的主键列
	 */
	@Test
	public void testUpdatePrimaryKey() throws SQLException {
		int id=insert();
		
		Student q = new Student();
		q.setId(id);
		q = db.load(q);

		q.getQuery().addCondition(Student.Field.id, q.getId());
		q.setId(100);

		db.update(q); // 将id（主键）修改为100
		// update STUDENT set ID = 100 where ID= 1
	}

	private int insert() throws SQLException {
		Student s = new Student();
		s.setName("Jhon Smith");
		s.setGender("M");
		s.setGrade("2");
		db.insert(s);
		return s.getId();
	}

	/**
	 * 更为复杂的查询2、修改写法,使用QueryBuilder
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_LikeAndEtc3() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);

		query.addCondition(QueryBuilder.eq(Student.Field.grade, "3"));
		query.addCondition(QueryBuilder.matchAny(Student.Field.name, "Jhon"));
		query.addCondition(QueryBuilder.lt(Student.Field.id, 100));
		
		query.orderByDesc(Student.Field.grade);
		List<Student> sts = db.select(query);

		Assert.assertEquals(sts.size(), db.count(query));
	}

}
