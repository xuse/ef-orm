package com.github.geequery.springdata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.OptimisticLockException;

import jef.database.QB;
import jef.database.RecordsHolder;
import jef.database.query.Query;

import org.easyframe.enterprise.spring.CommonDao;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.github.geequery.springdata.test.entity.Foo;
import com.github.geequery.springdata.test.entity.VersionLog;
import com.github.geequery.springdata.test.repo.FooDao;
import com.github.geequery.springdata.test.repo.FooDao2;

/**
 * 与Spring集成的示例。 本示例使用的xml作为Spring配置。参见
 * src/main/resources/spring/spring-test-case1.xml
 * 
 * @author jiyi
 *
 */
@ContextConfiguration(locations = { "classpath:spring-test-data.xml" })
public class Case1 extends AbstractJUnit4SpringContextTests implements InitializingBean {

	@javax.annotation.Resource
	private CommonDao commonDao;

	@javax.annotation.Resource
	private FooDao foodao;

	@javax.annotation.Resource
	private FooDao2 foodao2;

	// @javax.annotation.Resource
	// private ComplexFooDao complex;

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
			List<Foo> fooList = foodao.findByAgeOrderById(0);
			System.out.println(fooList);
		}
		// =========================
		{
			System.out.println("=== FindByAge Page ===");
			Page<Foo> fooPage = foodao.findByAgeOrderById(0, new PageRequest(1, 4));
			System.out.println(fooPage.getTotalElements());
			System.out.println(Arrays.toString(fooPage.getContent().toArray()));
		}
		// =========================
		{
			System.out.println("=== FindAll(page+sort) ===");
			Page<Foo> p = foodao.findAll(new PageRequest(0, 3, new Sort(new Order(Direction.DESC, "id"))));
			System.out.println(p.getTotalElements());
			System.out.println(Arrays.toString(p.getContent().toArray()));
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
			System.out.println("list=" + foos);
		}
		{
			System.out.println("=== findByNameLike ===");
			List<Foo> foo = foodao.findByNameLike("%四");
			System.out.println(foo);
		}

		{
			System.out.println("=== findByNameContainsAndAge ===");
			List<Foo> foos = foodao.findByNameContainsAndAge("李", 0);
			System.out.println(foos);
		}

		{
			// 多参数顺序测试否有问题(已修复)
			System.out.println("=== findByNameStartsWithAndAge ===");
			List<Foo> foos = foodao.findByNameStartsWithAndAge(0, "李");
			System.out.println(foos);
		}
		{
			System.out.println("=== DeleteAll() ===");
			foodao.deleteAll();
		}
	}

	@Test
	public void testFooDao2() {
		{
//			Foo foo = foodao2.findBysName("张三");
//			System.out.println(foo);
		}
		// =========================
		{
			//设置了@Param后，这是按name进行绑定的NativeQuery，只要语句中用了:xxx格式，参数顺序随便改没问题。
			List<Foo> foos = foodao2.findBySql(new Date(), "李四");
			System.out.println(foos);
		}
		// =========================
		{
			// NativeQuery多参数并按 ？1 ？2 序号操作可能就有问题
		}
	}

	// @Test
	// public void testComplexId() {
	// // 测试复合主键的情况
	// {
	// ComplexFoo cf = new ComplexFoo(1, 2);
	// cf.setMessage("test");
	// complex.save(cf);
	//
	// cf = new ComplexFoo(2, 2);
	// cf.setMessage("1222234324");
	// complex.save(cf);
	// }
	// {
	// ComplexFoo cf = complex.findOne(new int[] { 1, 2 });
	// System.out.println(cf);
	// cf.setMessage("修改消息!");
	// complex.save(cf);
	// }
	// {
	// Iterable<ComplexFoo> list = complex.findAll(Arrays.asList(new int[] { 1,
	// 2 }, new int[] { 2, 2 }));
	// for (ComplexFoo foo : list) {
	// System.out.println(foo);
	// }
	// }
	// {
	// complex.delete(new int[] { 1, 2 });
	// }
	// }

	@Override
	public void afterPropertiesSet() throws Exception {
		commonDao.getNoTransactionSession().dropTable(Foo.class);
		commonDao.getNoTransactionSession().createTable(Foo.class);
		commonDao.getNoTransactionSession().truncate(VersionLog.class);
		{
			List<Foo> list = new ArrayList<Foo>();
			list.add(new Foo("张三"));
			list.add(new Foo("李四"));
			list.add(new Foo("王五"));
			list.add(new Foo("赵柳"));
			list.add(new Foo("开发"));
			list.add(new Foo("测试"));
			list.add(new Foo("纠结"));
			list.add(new Foo("刘备"));
			list.add(new Foo("市场"));
			list.add(new Foo("渠道"));
			list.add(new Foo("销售"));
			list.add(new Foo("赵日天"));
			list.add(new Foo("叶良辰"));
			list.add(new Foo("玛丽苏"));
			list.add(new Foo("龙傲天"));
			foodao.save(list);
		}
	}

	@Test(expected = OptimisticLockException.class)
	public void testVersionUpdateAndOptLock() {
		int id;
		{
			VersionLog v = new VersionLog();
			v.setName("叶问");
			commonDao.insert(v);
			id = v.getId();
		}
		{
			VersionLog v = commonDao.load(VersionLog.class, id);
			v.setName("叶问天");
			commonDao.update(v);
		}
		{
			VersionLog v = commonDao.load(VersionLog.class, id);

			VersionLog v2 = commonDao.load(VersionLog.class, id);
			v2.setName("抢先更新");
			commonDao.update(v2);
			try {
				v.setName("啥，已经被人更新了，那我不是写不进去了！");
				commonDao.update(v);
			} catch (OptimisticLockException e) {
				e.printStackTrace();
				throw e;
			}
		}

	}

	@Test
	public void testVersionAndOptLockInBatch() {
		int id;
		{
			VersionLog v = new VersionLog();
			v.setName("我的");
			commonDao.insert(v);
			id = v.getId();
		}
		{
			VersionLog v1 = commonDao.load(VersionLog.class, id);
			VersionLog v2 = commonDao.load(VersionLog.class, id - 3);
			VersionLog v3 = commonDao.load(VersionLog.class, id - 2);
			VersionLog v4 = commonDao.load(VersionLog.class, id - 1);

			VersionLog v_ = commonDao.load(VersionLog.class, id - 2);
			v_.setName("再次抢先更新");
			commonDao.update(v_);

			v1.setName("更新1");
			v2.setName("更新2");
			v3.setName("又被抢了，还能好好做朋友吗？");
			v4.setName("更新4");
			int count = commonDao.batchUpdate(Arrays.asList(v1, v2, v3, v4));
			Assert.assertEquals(3, count); // 该条记录没有更新

			v3 = commonDao.load(v3);
			Assert.assertEquals("再次抢先更新", v3.getName()); // 该条记录没有更新
			/**
			 * 在Batch模式下，乐观锁可以阻止覆盖他人的记录（无法写入），但是无法检测出是哪一组记录因为冲突造成无法写入。
			 * 因此只能确认不覆盖其他人的记录。
			 * 
			 * 此外，非按主键更新的场合下，乐观锁也不能生效。因为非按主键更新时的更新请求并不能对应要数据库的的特定记录，无法检查
			 * 记录是否非修改。无法要求乐观锁进行干预。
			 */
		}
	}

	/**
	 * 演示悲观锁的使用
	 */
	@Test
	public void testPessimisticLock() {
		int id;
		{
			VersionLog v = new VersionLog();
			v.setName("我");
			commonDao.insert(v);
			id = v.getId();
		}
		{
			Query<VersionLog> query = QB.create(VersionLog.class).addCondition(QB.between(VersionLog.Field.id, id - 4, id));
			RecordsHolder<VersionLog> records = commonDao.selectForUpdate(query);
			try {
				for (VersionLog version : records) {
					version.setName("此时:" + version.getName());
					version.setModified(new Date());
				}
				records.commit();
			} finally {
				records.close();
			}

		}
		{
			Query<VersionLog> query = QB.create(VersionLog.class).addCondition(QB.between(VersionLog.Field.id, id - 4, id));
			List<VersionLog> records = commonDao.find(query);
			for (VersionLog version : records) {
				System.out.println(version.getName());
			}
		}

	}

	/**
	 * 特定场景下不需要加锁更新
	 */
	@Test
	public void testUpdateWithoutLock() {
		List<Foo> foos = commonDao.find(QB.create(Foo.class));
		Foo foo = foos.get(0);
		QB.fieldAdd(foo, Foo.Field.age, 100);
		foo.setName("姓名也更新了");
		commonDao.update(foo);
	}
}
