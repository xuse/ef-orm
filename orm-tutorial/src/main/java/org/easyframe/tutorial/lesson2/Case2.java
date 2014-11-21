package org.easyframe.tutorial.lesson2;

import java.sql.SQLException;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.ORMConfig;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Case2 {
	private static DbClient db;
	
	@BeforeClass
	public static void setup() throws SQLException{
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db=new DbClient();
		db.createTable(Student.class);
	}
	
	/**
	 * 按顺序查出全部学生,错误的用法.
	 * @throws SQLException
	 */
	@Test()
	public void testAllRecords_error() throws SQLException{
		//查出全部学生
		List<Student> allStudents=db.selectAll(Student.class);

		ORMConfig.getInstance().setAllowEmptyQuery(true);
		//按学号顺序查出全部学生
		Student st=new Student();
		st.getQuery().orderByAsc(Student.Field.id);
		try{
			List<Student> all=db.select(st);
			System.out.println("共有学生"+all.size());
		}catch(NullPointerException e){
			e.printStackTrace();
			throw e;
		}
		
	}
	/**
	 * 按顺序查出全部学生, 正确的用法
	 * @throws SQLException
	 */
	@Test
	public void testAllRecords() throws SQLException{
		//按学号顺序查出全部学生
		Student st=new Student();
		st.getQuery().setAllRecordsCondition();
		st.getQuery().orderByAsc(Student.Field.id);
		List<Student> all=db.select(st);
		System.out.println("共有学生"+all.size());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	@AfterClass
	public static void close(){
		if(db!=null)
			db.close();
	}

}
