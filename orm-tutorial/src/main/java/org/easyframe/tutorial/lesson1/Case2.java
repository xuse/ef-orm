package org.easyframe.tutorial.lesson1;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.DbClientBuilder;

import org.easyframe.tutorial.lesson1.entity.Foo2;
import org.junit.Assert;
import org.junit.Test;

public class Case2 {
	/**
	 * 现在我们可以更进一步。刚才我们用Foo这种POJO来作为实体，其实并不是ef-orm推荐的。
	 * ef-orm希望实体按以下的要求书写
	 *   1. 继承 jef.database.DataObject
	 *   2. 在类中加入一个内部的enum类型，用于描述所有的数据库字段。
	 *   （参见 org.easyframe.tutorial.lesson1.entity.Foo2）
	 *   
	 * 一旦实现了上述要求，那么对数据库的操作可以变得更为灵活和强大(将在后面的例子中展示)
	 * 
	 * @throws SQLException
	 */
	@Test
	public void simpleTest() throws SQLException{
		DbClient db=new DbClientBuilder().build();
		
		//创建表
		db.createTable(Foo2.class); 
		
		//插入记录
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
		//查询全表
		List<Foo2> allrecords=db.selectAll(Foo2.class);
		Assert.assertTrue(allrecords.isEmpty());
		
		//删除表
		db.dropTable(Foo2.class);
	}
}
