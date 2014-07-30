package org.easyframe.tutorial.lesson3;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.query.SqlExpression;
import jef.tools.DateUtils;
import jef.tools.string.RandomData;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson2.entity.UserBalance;
import org.junit.Test;

public class CaseDelete {
	DbClient db;

	public CaseDelete() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db = new DbClient();
		// 准备数据时关闭调试，减少控制台信息
		ORMConfig.getInstance().setDebugMode(false);
		db.dropTable(Student.class, UserBalance.class);
		db.createTable(Student.class, UserBalance.class);
		prepareData(15);
		ORMConfig.getInstance().setDebugMode(true);
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

	@Test
	public void testDelete_Basic() throws SQLException {
		{// Case1. 删除从数据库加载出来的对象
			Student st = db.load(Student.class, 1);
			db.delete(st);
			//SQL: delete from USERBALANCE where AMOUT between -100 and 0 
		} 

		{ // Case2. 删除所有女生
			Student st = new Student();
			st.setGender("F");
			db.delete(st);
			//SQL: delete from USERBALANCE where TODAYAMOUNT=TOTALAMOUNT
		}
		{//Case3. 删除所有1980年以前出生的学生
			Student st = new Student();
			st.getQuery().addCondition(Student.Field.dateOfBirth,Operator.LESS,DateUtils.getDate(1980, 1, 1));
			db.delete(st);
			//SQL: delete from USERBALANCE where todayAmount + 100< totalAmount
		}
	}
	
	@Test
	public void testDelete_Basic2() throws SQLException {
		{// Case1. Between条件,删除账户余额amout在-100到0之间的所有记录。
			UserBalance ub=new UserBalance();
			ub.getQuery().addCondition(QB.between(UserBalance.Field.amout, -100, 0));
			db.delete(ub);
		}

		{ // Case2. 两个字段比较，删除todayAmount和 totalAmout相等的记录
			UserBalance ub=new UserBalance();
			ub.getQuery().addCondition(UserBalance.Field.todayAmount, UserBalance.Field.totalAmount);
			db.delete(ub);
		}
		{//Case3. 删除按表达式条件删除
			UserBalance ub=new UserBalance();
			ub.getQuery().addCondition(new SqlExpression("todayAmount + 100< totalAmount"));
			db.delete(ub);
		}
	}
}
