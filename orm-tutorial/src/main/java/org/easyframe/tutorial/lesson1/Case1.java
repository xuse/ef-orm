package org.easyframe.tutorial.lesson1;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jef.database.DbClient;
import jef.database.jpa.JefEntityManagerFactory;
import jef.tools.reflect.BeanUtils;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.easyframe.tutorial.lesson1.entity.Foo;
import org.junit.Assert;
import org.junit.Test;

public class Case1 {
	@Test
	public void simpleTest() throws SQLException{
		DbClient db=new DbClient();
		JefEntityManagerFactory emf=new JefEntityManagerFactory(db);
		CommonDao dao=new CommonDaoImpl();
		 //模拟Spring自动注入
		BeanUtils.setFieldValue(dao, "entityManagerFactory", emf);
		//创建表
		dao.getNoTransactionSession().dropTable(Foo.class);
		dao.getNoTransactionSession().createTable(Foo.class); 
		
		
		Foo foo=new Foo();
		foo.setId(1);
		foo.setName("Hello,World!");
		foo.setCreated(new Date());
		dao.insert(foo);  //插入一条记录
		
		//从数据库查询这条记录
		Foo loaded=dao.loadByKey(Foo.class, "id", foo.getId());
		System.out.println(loaded.getName());
		
		//更新这条记录
		loaded.setName("EF-ORM is very simple.");
		dao.update(loaded);
		
		
		//删除这条记录
		dao.removeByKey(Foo.class, "id", foo.getId());
		List<Foo> allrecords=dao.find(new Foo());
		Assert.assertTrue(allrecords.isEmpty());
		
		//删除表
		dao.getNoTransactionSession().dropTable(Foo.class);
	}
}
