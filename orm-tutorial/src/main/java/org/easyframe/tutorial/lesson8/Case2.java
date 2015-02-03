package org.easyframe.tutorial.lesson8;

import java.sql.SQLException;
import java.util.Arrays;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.query.Query;
import jef.database.wrapper.ResultIterator;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson4.entity.DataDict;
import org.easyframe.tutorial.lesson4.entity.Person;
import org.easyframe.tutorial.lesson4.entity.School;
import org.easyframe.tutorial.lesson5.entity.Item;
import org.junit.BeforeClass;
import org.junit.Test;

public class Case2 extends org.junit.Assert {
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
		ORMConfig.getInstance().setDebugMode(true);
	}
	
	/**
	 * 当需要查询的数据量非常庞大时，如数据导出等，可以使用Iterator模式 流式处理。
	 * @throws SQLException
	 */
	@Test
	public void  testIteratedSelect() throws SQLException{
		Query<Person> p=QB.create(Person.class);
		p.setFetchSize(100);
		ResultIterator<Person> results=db.iteratedSelect(p, null);
		try{
			for(;results.hasNext();){
				Person person=results.next();
				
				//执行业务处理
				System.out.println(person);
			}	
		}finally{
			results.close();
		}
	}
}
