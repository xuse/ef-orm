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
	 * 按顺序查出全部学生,错误的用法
	 * 
	 * 从前面看到，在实体中可以注入各种“条件”，应用于查询、更新和删除操作中。
	 * 那么如果我们要对表的全部数据操作，不注入任何条件就可以了吗？
	 * 
	 * 下面的例子，会抛出一个“Illegal usage of Query object”的异常，这是因为如果仅仅创建一个实体而不添加条件，
	 * 因为这不是一个合法的Query对象，需要用  query.setAllRecordsCondition() 方法来指定这是一个查全表的请求。
	 * 这是为了防止用户在一系列的if语句中忘记为查询设定条件。
	 * 
	 * 然而如果我们在全局设置中，设置为“允许空查询”
	 * <pre><code>
	 * 		ORMConfig.getInstance().setAllowEmptyQuery(true);
	 * </code></pre>
	 * 那么，未设置条件的查询，又是可以使用的。
	 * 
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
	 * 
	 * 如果调用过“setAllRecordsCondition()”，那么就可以查全表数据了。
	 * @throws SQLException
	 */
	@Test
	public void testAllRecords() throws SQLException{
		//按学号顺序查出全部学生
		Student st=new Student();
		st.getQuery().setAllRecordsCondition().orderByAsc(Student.Field.id);
		List<Student> all=db.select(st);
		System.out.println("共有学生"+all.size());
	}
	
	
	
	
	
	
	
	@AfterClass
	public static void close(){
		if(db!=null)
			db.close();
	}

}
