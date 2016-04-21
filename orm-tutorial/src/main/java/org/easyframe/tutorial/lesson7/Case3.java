package org.easyframe.tutorial.lesson7;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbMetaData;
import jef.database.NativeCall;
import jef.database.ORMConfig;
import jef.database.SqlTemplate;

import org.easyframe.tutorial.lesson7.entity.Employee;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * ！！！！！！！！！！！！！！！！！！！！！！！！！！！
 * 注意：这个案例演示存储过程的使用，针对MySQL数据库设计。Derby不支持。
 * @author jiyi
 *
 */
public class Case3 {
	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		//****************************************************
		//*  请先将此处的URL配置成一个可以连接的MYSQL数据库地址 *
		//****************************************************
		db = new DbClientBuilder("jdbc:mysql://localhost:3306/test","root","admin").build();
		ORMConfig.getInstance().setDebugMode(false);
		db.dropTable(Employee.class);
		db.createTable(Employee.class);
		Employee e=new Employee();
		e.setId("100000");
		e.setName("刘备");
		e.setSalary(10000.0);
		db.insert(e);
		
		e=new Employee();
		e.setId("100001");
		e.setName("关羽");
		e.setSalary(8000.0);
		db.insert(e);
		
		e=new Employee();
		e.setId("100002");
		e.setName("张飞");
		e.setSalary(7000.0);
		db.insert(e);
		ORMConfig.getInstance().setDebugMode(true);
		DbMetaData meta=db.getMetaData(null);
		//如果存储过程不存在，就创建存储过程
		if(!meta.existsProcdure(null, "update_salary")){
			meta.executeScriptFile(Case3.class.getResource("/update_salary.sql"),"@");
		}
	}
	
	@Test
	public void testProducre() throws SQLException{
		System.out.println("调整工资前——");
		System.out.println(db.selectAll(Employee.class));
		
		NativeCall nc=db.createNativeCall("update_salary", String.class,int.class);
		nc.execute("100002",2);
		nc.execute("100001", 1);
		System.out.println("调整工资后——");
		System.out.println(db.selectAll(Employee.class));
	}
	
	@Test
	public void test123() throws SQLException{
		SqlTemplate t=db.getSqlTemplate(null);
		String s="'今天是'||str(cast(year(current_date)/100+1 as int))||'世纪'";
		String trunced=t.getExpressionValue(s, String.class);
		System.out.println(trunced);
	}
}
