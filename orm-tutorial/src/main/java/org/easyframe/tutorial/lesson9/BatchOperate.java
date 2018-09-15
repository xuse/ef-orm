package org.easyframe.tutorial.lesson9;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.Batch;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.QB;
import jef.database.query.Func;
import jef.tools.DateUtils;
import jef.tools.string.RandomData;

import org.easyframe.tutorial.lesson4.entity.DataDict;
import org.easyframe.tutorial.lesson4.entity.Person;
import org.easyframe.tutorial.lesson4.entity.School;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 批量操作，是在操作大量数据时提高性能的有效方法
 * 
 * @author jiyi
 *
 */
public class BatchOperate extends org.junit.Assert {

	private static DbClient db;

	/**
	 * 测试数据准备
	 * 
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial").build();

		db.dropTable(Person.class, School.class, DataDict.class);
		db.createTable(Person.class, School.class, DataDict.class);
	}

	/**
	 * 批量操作接口
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testBatchOperates() throws SQLException {
		System.out.println("testBatchOperates==");
		db.truncate(Person.class);
		List<Person> persons = new ArrayList<Person>();
		for (int i = 0; i < 5; i++) {
			Person p = new Person();
			RandomData.fill(p); // 填充一些随机值
			persons.add(p);
		}

		{ // 批量插入
			db.batchInsert(persons);

			// 批量操作下，从数据库获得的自增键值依然会写回到对象中。
			for (int i = 0; i < 5; i++) {
				assertEquals(Integer.valueOf(i + 1), persons.get(i).getId());
			}
		}
		{ // 批量更新(按主键)
			for (int i = 0; i < 5; i++) {
				persons.get(i).setGender(i % 2 == 0 ? 'M' : 'F');
			}
			db.batchUpdate(persons);
		}
		{// 批量更新 (按模板)
			for (int i = 0; i < 5; i++) {
				Person p = persons.get(i);
				p.getQuery().clearQuery();
				p.getQuery().addCondition(Person.Field.name, p.getName());
				p.setName("第" + (i + 1) + "人");
			}
			db.batchUpdate(persons);
		}

		{// 按主键批量删除记录
			db.batchDelete(persons);
		}

	}

	/**
	 * 设置多组更新数据。 注意、后面的更新请求必须和第一个请求具有完全相同的更新字段和条件。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testBatchUpdate() throws SQLException {
		doInsert(5);
		Person p1 = new Person();
		p1.getQuery().addCondition(Person.Field.name.matchAny("a"));
		p1.prepareUpdate(Person.Field.created, db.func(Func.current_timestamp));

		Person p2 = QB.create(Person.class).addCondition(Person.Field.name.matchAny("b")).getInstance();

		Person p3 = QB.create(Person.class).addCondition(Person.Field.name.matchAny("cc")).getInstance();

		db.batchUpdate(Arrays.asList(p1, p2, p3));
	}

	@Test
	public void testBatchUpdate2() throws SQLException {
		doInsert(5);
		Person query = new Person();
		query.getQuery().addCondition(Person.Field.created.gt(DateUtils.get(2000, 1, 1)));

		List<Person> persons = db.select(query);
		for (Person person : persons) {
			person.setCreated(new Date());
		}
		Batch<Person> batch = db.startBatchUpdate(persons.get(0), true);
		batch.execute(persons);
		persons = db.select(QB.create(Person.class));
		batch.execute(persons);

	}

	private void doInsert(int max) throws SQLException {
		List<Person> persons = new ArrayList<Person>(max);
		for (int i = 0; i < max; i++) {
			Person p = new Person();
			RandomData.fill(p); // 填充一些随机值
			persons.add(p);
		}
		;
		db.batchInsert(persons);
	}

	private void doExtremeInsert(int max) throws SQLException {
		List<Person> persons = new ArrayList<Person>(max);
		for (int i = 0; i < max; i++) {
			Person p = new Person();
			RandomData.fill(p); // 填充一些随机值
			persons.add(p);
		}
		;
		db.extremeInsert(persons, false);
	}

	@Test
	public void reuseBatchObject() throws SQLException {
		Batch<Person> batch = db.startBatchInsert(new Person(), false);

		List<Person> persons = new ArrayList<Person>(5);
		for (int i = 0; i < 5; i++) {
			Person p = new Person();
			RandomData.fill(p); // 填充一些随机值
			persons.add(p);
		}
		batch.execute(persons);
		// 再来5个
		persons = new ArrayList<Person>(5);
		for (int i = 0; i < 5; i++) {
			Person p = new Person();
			RandomData.fill(p); // 填充一些随机值
			persons.add(p);
		}
		batch.execute(persons);
	}

}
