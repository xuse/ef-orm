package org.easyframe.tutorial.lesson3;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jef.codegen.EntityEnhancer;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.ORMConfig;
import jef.database.query.Func;
import jef.database.query.JpqlExpression;
import jef.tools.string.RandomData;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson2.entity.UserBalance;
import org.junit.AfterClass;
import org.junit.Test;

public class CaseUpdate {
	static DbClient db;

	public CaseUpdate() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial").build();
		
		// 准备数据时关闭调试，减少控制台信息
		ORMConfig.getInstance().setDebugMode(false);
		db.dropTable(Student.class, UserBalance.class);
		db.createTable(Student.class, UserBalance.class);
		prepareData(15);
		ORMConfig.getInstance().setDebugMode(true);
	}

	@Test
	public void testUpdate_Basic() throws SQLException {
		{
			// 查出一条对象,更新该条记录.
			Student st = db.load(Student.class, 1);
			st.setGender("M");
			int updated = db.update(st);
			// SQL: update STUDENT set GENDER = 'M' where ID=1
		}
		{
			// 按主键随意更新一条记录
			Student st = new Student();
			st.setId(3);
			st.setGrade("M");
			int updated = db.update(st);
			// SQL: update STUDENT set GRADE = 'M' where ID=3

			System.out.println("更新" + updated + "条记录。");
		}

		{

			// 按条件更新多条记录
			Student st = new Student();
			st.getQuery().addCondition(Student.Field.name, Operator.MATCH_ANY, "张");
			st.setGender("M");
			int updated = db.update(st);
			// SQL: update STUDENT set GENDER = 'M' where NAME like '%张%' escape
			// '/'

			System.out.println("更新" + updated + "条记录。");
		}
	}

	@Test
	public void testUpdate_QueryAndUpdateMap() throws SQLException {
		Student st = new Student();
		st.setId(1);
		st.setGender("M");

		Map<Field, Object> updateMap = st.getUpdateValueMap();
		System.out.println(updateMap);
		updateMap.clear();
		int updated = db.update(st); // no update here
	}

	@Test
	public void testUpdate_concurrent_error() throws SQLException {
		// 准备
		UserBalance u = new UserBalance();
		u.setAmout(100); // 一开始用户账上有100元钱
		db.insert(u);

		// 开始
		UserBalance ub = db.load(UserBalance.class, 1);// 第一步,先查出用户账上有多少钱

		// ... do some thing.
		ub.setAmout(ub.getAmout() - 50);// 扣款50元
		db.update(ub);// 将数值更新为扣款后的数值。
	}

	// 案例：乐观锁的update
	@Test
	public void testUpdate_Cas1() throws SQLException {
		// 准备
		UserBalance u = new UserBalance();
		u.setAmout(100); // 一开始用户账上有100元钱
		db.insert(u);
		System.out.println(u.getId());
		// 开始
		int updated;
		do {
			UserBalance ub = db.load(UserBalance.class, 1);// 第一步,先查出用户账上有多少钱
			ub.getQuery().addCondition(UserBalance.Field.id, ub.getId());
			ub.getQuery().addCondition(UserBalance.Field.amout, ub.getAmout());
			ub.setAmout(ub.getAmout() - 50);// 扣款50元
			updated = db.update(ub);
		} while (updated == 0);
	}

	// 案例:原子操作update
	@Test
	public void testUpdate_atom() throws SQLException {
		UserBalance ub = new UserBalance();
		ub.setId(1);
		ub.prepareUpdate(UserBalance.Field.amout, new JpqlExpression("amout-50"));
		int updated = db.update(ub);
	}

	// 案例：prepareUpdate的用法
	@Test
	public void testUpdate_MoreValues() throws SQLException {
		UserBalance ub = new UserBalance();
		ub.getQuery().setAllRecordsCondition();

		// 将一个字段更新为另一个字段的值
		ub.prepareUpdate(UserBalance.Field.todayAmount, UserBalance.Field.amout);
		// 将updateTime更新为现在的值
		ub.prepareUpdate(UserBalance.Field.updateTime, db.func(Func.now));
		// 更新为另外两个字段相加
		ub.prepareUpdate(UserBalance.Field.totalAmount, new JpqlExpression("todayAmount + amout"));
		
		db.update(ub);
	}

	// 案例：更新值回写
	@Test
	public void testUpdate_Writeback() throws SQLException {
		Student st = db.load(Student.class, 1);
		st.prepareUpdate(Student.Field.id, 199);
		int oldId = st.getId();
		db.update(st);
		System.out.println("Student的id从" + oldId + " 更新为" + st.getId());
	}

	// 案例:比较更新Map 3
	@Test
	public void testUpdate_Compare1() throws SQLException {
		// 获得新的st对象
		Student newSt = new Student();
		newSt.setId(1);
		newSt.setDateOfBirth(new Date());
		newSt.setGender("M");
		newSt.setName("王五");
		// 从数据库中获得旧的st对象
		Student oldSt = db.load(Student.class, 1);

		// 把修改过的值记录到oldSt的updateValueMap中,相等的值不记入
		DbUtils.compareToUpdateMap(newSt, oldSt);

		// 如果需要记录字段修改记录，可以直接获取oldSt.getUpdateValueMap()来记录。
		db.update(oldSt); // 只有数值不同的字段被更新。
	}

	// 案例:比较更新Map 2
	@Test
	public void testUpdate_Compare2() throws SQLException {
		// 获得新的st对象
		Student newSt = new Student();
		newSt.setId(1);
		newSt.setDateOfBirth(new Date());
		newSt.setGender("M");
		newSt.setName("李四");

		// 从数据库中获得旧的st对象
		Student oldSt = db.load(Student.class, 1);

		DbUtils.compareToNewUpdateMap(newSt, oldSt);

		db.update(newSt); // 只有数值不同的字段被更新。
	}

	@Test
	public void testUpdate_fillValues() throws SQLException {
		Student newSt = new Student();
		newSt.stopUpdate();//不记录赋值操作
		newSt.setId(1);
		newSt.setDateOfBirth(new Date());
		newSt.setGender("F");
		newSt.setName("张三");
		
		db.update(newSt);//由于未记录赋值变更，此处update操作无效。
		
		
		DbUtils.fillUpdateMap(newSt);//将主键以外所有字段都记录为变更。
		db.update(newSt);//update有效
	}

	private void prepareData(int num) throws SQLException {
		List<Student> data = new ArrayList<Student>();
		Date old = new Date(System.currentTimeMillis() - 864000000000L);
		for (int i = 0; i < num; i++) {
			// 用随机数生成一些学生信息
			Student st = new Student();
			st.setGender(i % 2 == 0 ? "M" : "F");
			st.setName(RandomData.randomChineseName());
			st.setDateOfBirth(RandomData.randomDate(old, new Date()));
			st.setGrade(String.valueOf(RandomData.randomInteger(1, 6)));
			data.add(st);
		}
		db.batchInsert(data);
	}
	@AfterClass
	public static void close(){
		db.close();
	}
}
