package com.github.geequery.springdata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easyframe.enterprise.spring.CommonDao;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.github.geequery.springdata.test.entity.Foo;
import com.github.geequery.springdata.test.repo.FooDao;

/**
 * 与Spring集成的示例。 本示例使用的xml作为Spring配置。参见
 * src/main/resources/spring/spring-test-case1.xml
 * 
 * @author jiyi
 *
 */
@ContextConfiguration(locations = { "classpath:spring-test-case1.xml" })
public class Case1 extends AbstractJUnit4SpringContextTests {

	@javax.annotation.Resource
	private CommonDao commonDao;

	@javax.annotation.Resource
	private FooDao foodao;

	// @Test
	// public void test1() throws SQLException{
	// commonDao.getNoTransactionSession().dropTable(Foo.class);
	// commonDao.getNoTransactionSession().createTable(Foo.class);
	// {
	// Foo foo=new Foo();
	// foo.setName("Hello!");
	// commonDao.batchInsert(Arrays.asList(foo));
	// }
	// {
	// Foo foo=new Foo();
	// foo.setAge(3);
	// foo.setName("Hello!");
	// //update MY_FOO set age=3 where name='Hello!'
	// commonDao.updateByProperty(foo, "name");
	// }
	// {
	// Foo foo=commonDao.loadByPrimaryKey(Foo.class, 1);
	// System.out.println(foo.getName());
	// }
	// {
	// //根据ID删除
	// commonDao.removeByField(Foo.class, "id", 1);
	// }
	// }

	@Test
	public void test2() throws SQLException {
		// commonDao.getNoTransactionSession().createTable(Foo.class);
		// =========================
		{
			List<Foo> list = new ArrayList<Foo>();
			list.add(new Foo("张三"));
			list.add(new Foo("李四"));
			list.add(new Foo("王五"));
			foodao.save(list);	
		}
		
		// =========================
		{
			System.out.println("=== FindByName ===");
			Foo foo = foodao.findByName("张三");
			System.out.println(foo.getName());
			System.out.println(foo.getId());	
		}
		// =========================
		{
			System.out.println("=== FindByAgeOrderById ===");
			List<Foo> fooList = foodao.findByAgeOrderById(1);
			System.out.println(fooList);	
		}
		// =========================
		{
			System.out.println("=== FindByAge Page ===");
			Page<Foo> fooPage = foodao.findByAgeOrderById(1, new PageRequest(1, 3));
			System.out.println(fooPage.getContent());	
		}
		// =========================
		{
			System.out.println("=== FindAll(page+sort) ===");
			Page<Foo> p = foodao.findAll(new PageRequest(1, 3, new Sort(new Order(Direction.DESC, "id"))));
			System.out.println(p.getTotalElements());
			System.out.println("list=" + p.getContent());	
		}
		
		// ==============================================
		{
			System.out.println("=== FindAll(sort) ===");
			Iterable<Foo> iters = foodao.findAll(new Sort(new Order(Direction.DESC, "id")));
			System.out.println("list=" + iters);	
		}
		// =========================
		{
			System.out.println("=== FindAll(?,?,?) ===");
			List<Integer> id = Arrays.<Integer> asList(1, 3, 4, 5);
			Iterable<Foo> foos = foodao.findAll(id);
			if (foos.iterator().hasNext()) {
				System.out.println(foos.iterator().next());
			}	
		}
		
		// =========================
		{
			System.out.println("=== DeleteAll() ===");
			foodao.deleteAll();	
		}
	}
}
