package org.easyframe.tutorial.lesson7;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.SqlTemplate;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.DefaultRoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.database.meta.MetaHolder;
import jef.database.meta.AbstractMetadata;
import junit.framework.Assert;

import org.easyframe.tutorial.lesson4.entity.Person;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * nativeQuery支持路由功能（多数据库） 
 *
 */
public class Case2 {
	
	private static DbClient db;
	
	/**
	 * 准备测试数据
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException{
		//准备多个数据源
		Map<String,DataSource> datasources=new HashMap<String,DataSource>();
		datasources.put("datasource1", new SimpleDataSource("jdbc:derby:./db;create=true",null,null));
		datasources.put("datasource2", new SimpleDataSource("jdbc:derby:./db2;create=true",null,null));
		MapDataSourceLookup lookup=new MapDataSourceLookup(datasources);
		lookup.setDefaultKey("datasource1");//指定datasource1是默认的操作数据源
		
		//构造一个多数据源的DbClient
		db=new DbClient(new DefaultRoutingDataSource(lookup));
		
		ORMConfig.getInstance().setDebugMode(true);
		db.dropTable(Person.class);
		//将Person对象的对应数据库修改为datasource2。（默认应该用注解 @BindDataSource来设置）
		((AbstractMetadata)MetaHolder.getMeta(Person.class)).setBindDsName("datasource2");
		db.dropTable(Person.class);
		db.createTable(Person.class);
		
		Person p=new Person();
		p.setName("张三");
		p.setGender('F');
		db.insert(p);
		ORMConfig.getInstance().setDebugMode(true);
	}
	
	/**
	 * 测试多数据源下，命名查询可以指定数据源
	 * @throws SQLException
	 */
	@Test(expected=PersistenceException.class)
	public void testDataSourceBind() throws SQLException{
		//Person对象所在的数据源为DataSource2		
		//由于配置中指定默认数据源为datasource2，因此可以正常查出
		List<Person> persons=db.createNamedQuery("getUserById",Person.class).setParameter("name", "张三").getResultList();
		System.out.println(persons);
		
		//这个配置未指定绑定的数据源，因此会抛出异常
		try{
			List<Person> p2=db.createNamedQuery("getUserById-not-bind-ds",Person.class).setParameter("name", "张三").getResultList();	
		}catch(RuntimeException e){
			throw e;
		}
	}
	
	@Test
	public void testSqlTemplate() throws SQLException{
		System.out.println("============================");
		//获得在datasource2上执行SQL操作的句柄
		SqlTemplate t=db.getSqlTemplate("datasource2");
		List<Person> person=t.selectBySql("select * from t_person where gender=?", Person.class, "F");
		Assert.assertEquals(1, person.size());
	}
}
