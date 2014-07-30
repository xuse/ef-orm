package org.easyframe.tutorial.lesson1;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;

import org.easyframe.tutorial.lesson1.entity.Foo2;
import org.junit.Assert;
import org.junit.Test;

public class Case2 {
	@Test
	public void simpleTest() throws SQLException{
		DbClient db=new DbClient();
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		
		//创建表
		db.createTable(Foo2.class); 
		
		
		Foo2 foo=new Foo2();
		foo.setId(1);
		foo.setName("Hello,World!");
		foo.setCreated(new Date());
		db.insert(foo);  //插入一条记录
		
		//从数据库查询这条记录
		Foo2 loaded=db.load(foo);
		System.out.println(loaded.getName());
		
		//更新这条记录
		loaded.setName("EF-ORM is very simple.");
		db.update(loaded);
		
		
		//删除这条记录
		db.delete(loaded);
		List<Foo2> allrecords=db.selectAll(Foo2.class);
		Assert.assertTrue(allrecords.isEmpty());
		
		//删除表
		db.dropTable(Foo2.class);
	}
}
