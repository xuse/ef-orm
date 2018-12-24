package org.easyframe.tutorial.lesson1;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.easyframe.tutorial.lesson1.entity.Foo;
import org.junit.Assert;
import org.junit.Test;

import jef.database.QB;

public class Case1 {
	/**
	 * At the very begining. 在最初的开始——
	 * 
	 * 我们现在创建一个名为Foo的实体 {@link Foo}。然后用API为这个实体建表、并实现增删改查操作。
	 * 大家平时操作ORM框架都是使用了一个Dao对象的，因此这里也使用了一个通用的DAO对象。
	 */
	@Test
	public void simpleTest() throws SQLException {
		// 模拟Spring的初始化
		SessionFactoryBean sessionFactory = new SessionFactoryBean();
		sessionFactory.setDataSource("jdbc:derby:./db;create=true", null, null);
		sessionFactory.setPackagesToScan(new String[] {"org.easyframe.tutorial.lesson1.entity"});
		sessionFactory.afterPropertiesSet();
		CommonDao dao = new CommonDaoImpl(sessionFactory.getObject());

		// 创建表
		dao.getNoTransactionSession().dropTable(Foo.class);
		dao.getNoTransactionSession().createTable(Foo.class);

		// 增加记录
		Foo foo = new Foo();
		foo.setId(1);
		foo.setName("Hello,World!");
		//此处应该以设置的值为准。。。。
		foo.setCreated(new Date());
		dao.insert(foo); // 插入一条记录

		// 从数据库查询这条记录
		Foo loaded = dao.loadByField(Foo.class, "id", foo.getId(), true);
		System.out.println(loaded.getName());

		//FIXME  字段显示为null
		loaded.setName("EF-ORM is very simple.");
		dao.update(loaded);

		// 删除这条记录
		dao.removeByField(Foo.class, "id", foo.getId());
		
		//FIXME 查找  应该无条件
//		List<Foo> allrecords = dao.find(new Foo());
		
		
		List<Foo> allrecords = dao.find(QB.create(Foo.class));
		
		Assert.assertTrue(allrecords.isEmpty());

		// 删除表
		dao.getNoTransactionSession().dropTable(Foo.class);
	}
}
