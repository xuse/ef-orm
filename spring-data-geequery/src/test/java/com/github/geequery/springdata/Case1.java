package com.github.geequery.springdata;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.OptimisticLockException;

import  org.apache.commons.lang3.builder.ToStringBuilder;
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

import com.github.geequery.entity.Entities;
import com.github.geequery.extension.querydsl.QueryDSLTables;
import com.github.geequery.extension.querydsl.SQLQueryEx;
import com.github.geequery.extension.querydsl.SQLRelationalPath;
import com.github.geequery.springdata.test.entity.Account;
import com.github.geequery.springdata.test.entity.AccountRoleRelation;
import com.github.geequery.springdata.test.entity.ComplexFoo;
import com.github.geequery.springdata.test.entity.Foo;
import com.github.geequery.springdata.test.entity.Role;
import com.github.geequery.springdata.test.entity.Role.RoleType;
import com.github.geequery.springdata.test.entity.VersionLog;
import com.github.geequery.springdata.test.repo.ComplexFooDao;
import com.github.geequery.springdata.test.repo.FooDao;
import com.github.geequery.springdata.test.repo.FooEntityDao;
import com.querydsl.codegen.BeanSerializer;
import com.querydsl.core.Tuple;
import com.querydsl.sql.codegen.MetaDataExporter;

import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.datasource.SimpleDataSource;
import jef.database.query.Query;
import jef.tools.DateUtils;
import jef.tools.string.RandomData;

/**
 * 与Spring集成的示例。 本示例使用的xml作为Spring配置。参见
 * src/main/resources/spring/spring-test-case1.xml
 * 
 * 1\save 报错 (OK) 2、排序无效(OK) 3、自定义列名无效(OK，应当想办法支持Native)
 * 
 * 4、MySQL下的时间精度造成版本无效 5、考虑使用毫秒数 6 考虑使用GUID作为版本
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
	private FooEntityDao foodao2;

	@javax.annotation.Resource
	private ComplexFooDao complex;

	@Test
	public void testCase1() {
		Foo foo = new Foo();
		foo.setAge(1);
		foo.setName("咋呼呼");
		foodao.save(foo);
	}

	@Test
	public void testFooDAOAnnotation() throws SQLException {
		{
			foodao.findByWhat("张", 100, "a", DateUtils.today(), "", DateUtils.yesterday(), "hello");
		}
	}

	/**
	 * 使用方法名来定义查询的案例.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testFooDAO() throws SQLException {
		// =============== 单字段查找 ==========
		{
			System.out.println("=== FindByName ===");
			Foo foo = foodao.findByName("张三");
			System.out.println(foo.getName());
			System.out.println(foo.getId());
		}
		// =============查找，带排序============
		{
			System.out.println("=== FindByAgeOrderById ===");
			List<Foo> fooList = foodao.findByAgeOrderById(0);
			System.out.println(fooList);

			int id = fooList.get(0).getId();
		}
		// ==============使用分页，固定排序===========
		{
			System.out.println("=== FindByAge Page ===");
			Page<Foo> fooPage = foodao.findByAgeOrderById(0, PageRequest.of(1, 4));
			System.out.println(fooPage.getTotalElements());
			System.out.println(Arrays.toString(fooPage.getContent().toArray()));
		}
		// ==============分页+传入排序参数===========
		{
			System.out.println("=== FindAll(page+sort) ===");
			Page<Foo> p = foodao.findAll(PageRequest.of(0, 3, Sort.by(new Order(Direction.DESC, "age"))));
			System.out.println(p.getTotalElements());
			System.out.println(Arrays.toString(p.getContent().toArray()));
		}

		// ===================不分页，传入排序参数===========================
		{
			System.out.println("=== FindAll(sort) ===");
			Iterable<Foo> iters = foodao.findAll(Sort.by(new Order(Direction.DESC, "id")));
			System.out.println("list=" + iters);
		}
		// ===========查询多个ID的记录==============
		{
			System.out.println("=== FindAll(?,?,?) ===");
			List<Integer> id = Arrays.<Integer> asList(1, 3, 4, 5);
			Iterable<Foo> foos = foodao.findAllById(id);
			System.out.println("list=" + foos);
		}
		{
			// =========== 在方法中携带运算符 like ===========
			System.out.println("=== countByNameLike ===");
			System.out.println(foodao.countByNameLike("%四"));
			System.out.println("=== findByNameLike ===");
			List<Foo> foo = foodao.findByNameLike("%四");
			System.out.println(foo);
		}

		{
			// 在方法中携带运算符 contains，并加上另一个条件
			// 如果入参顺序和方法名中的一致，可以不加注解
			System.out.println("=== findByNameContainsAndAge ===");
			List<Foo> foos = foodao.findByNameContainsAndAge("李", 0);
			System.out.println(foos);
		}

		{
			// 多参数顺序测试否有问题
			System.out.println("=== findByNameStartsWithAndAge ===");
			List<Foo> foos = foodao.findByNameStartsWithAndAge(0, "李");
			System.out.println(foos);
		}
		{
			// 删除全部
			System.out.println("=== DeleteAll() ===");
			foodao.deleteAll();
		}
	}

	@Test
	public void queryDSLExport() throws SQLException {
		SimpleDataSource source = new SimpleDataSource("jdbc:derby:./db;create=true", null, null);
		Connection conn = source.getConnection();
		MetaDataExporter exporter = new MetaDataExporter();
		exporter.setPackageName("com.myproject.mydomain");
		exporter.setTargetFolder(new File("target/generated-sources"));
		exporter.setBeanSerializer(new BeanSerializer());
		exporter.export(conn.getMetaData());
		conn.close();
	}

	@Test
	public void testFooDAOQueryDSL() throws SQLException {
		// =============== 单字段查找 ==========
		{
			System.out.println("=== FindByName ===");
			SQLRelationalPath<Foo> qf = QueryDSLTables.table(Foo.class);
			Iterable<Foo> foo = foodao.findAll(qf.string(Foo.Field.name).eq("张三"));
			foo.forEach(e -> System.out.println(e.getName()));
		}

		{
			// System.out.println("=== FindByName2 ===");
			// QFoo qf = QFoo.foo;
			// Iterable<com.myproject.mydomain.Foo> foo =
			// foodao.findAll(qf.nameA.eq("张三"));
			// foo.forEach(e -> System.out.println(e.getName()));

		}
		// =============查找，带排序============
		// {
		// System.out.println("=== FindByAgeOrderById ===");
		// List<Foo> fooList = foodao.findByAgeOrderById(0);
		// System.out.println(fooList);
		//
		// int id = fooList.get(0).getId();
		// // =============== 使用悲观锁更新 ==================
		// boolean updated = foodao.lockItAndUpdate(id, new Update<Foo>() {
		// @Override
		// public void setValue(Foo value) {
		// value.setName("李四");
		// value.setRemark("悲观锁定");
		//
		// }
		// });
		// if (updated) {
		// Foo u = foodao.getOne(id);
		// Assert.assertEquals("悲观锁定", u.getRemark());
		// }
		// }
		// // ==============使用分页，固定排序===========
		// {
		// System.out.println("=== FindByAge Page ===");
		// Page<Foo> fooPage = foodao.findByAgeOrderById(0, new PageRequest(1,
		// 4));
		// System.out.println(fooPage.getTotalElements());
		// System.out.println(Arrays.toString(fooPage.getContent().toArray()));
		// }
		// // ==============分页+传入排序参数===========
		// {
		// System.out.println("=== FindAll(page+sort) ===");
		// Page<Foo> p = foodao.findAll(new PageRequest(0, 3, new Sort(new
		// Order(Direction.DESC, "age"))));
		// System.out.println(p.getTotalElements());
		// System.out.println(Arrays.toString(p.getContent().toArray()));
		// }
		//
		// // ===================不分页，传入排序参数===========================
		// {
		// System.out.println("=== FindAll(sort) ===");
		// Iterable<Foo> iters = foodao.findAll(new Sort(new
		// Order(Direction.DESC, "id")));
		// System.out.println("list=" + iters);
		// }
		// // ===========查询多个ID的记录==============
		// {
		// System.out.println("=== FindAll(?,?,?) ===");
		// List<Integer> id = Arrays.<Integer> asList(1, 3, 4, 5);
		// Iterable<Foo> foos = foodao.findAllById(id);
		// System.out.println("list=" + foos);
		// }
		// {
		// // =========== 在方法中携带运算符 like ===========
		// System.out.println("=== countByNameLike ===");
		// System.out.println(foodao.countByNameLike("%四"));
		// System.out.println("=== findByNameLike ===");
		// List<Foo> foo = foodao.findByNameLike("%四");
		// System.out.println(foo);
		// }
		//
		// {
		// // 在方法中携带运算符 contains，并加上另一个条件
		// // 如果入参顺序和方法名中的一致，可以不加注解
		// System.out.println("=== findByNameContainsAndAge ===");
		// List<Foo> foos = foodao.findByNameContainsAndAge("李", 0);
		// System.out.println(foos);
		// }
		//
		// {
		// // 多参数顺序测试否有问题
		// System.out.println("=== findByNameStartsWithAndAge ===");
		// List<Foo> foos = foodao.findByNameStartsWithAndAge(0, "李");
		// System.out.println(foos);
		// }
		// {
		// // 删除全部
		// System.out.println("=== DeleteAll() ===");
		// foodao.deleteAll();
		// }
	}

	@Test
	public void testAbc() {
		{

			/**
			 * 最基本的@Query查询，注意需要@Param来绑定参数
			 */
			Foo foo = foodao2.findBysName("张三");
			System.out.println(foo);
		}
	}

	@Test
	public void testFooDao2() {
		{
			/**
			 * 最基本的@Query查询，注意需要@Param来绑定参数
			 */
			Foo foo = foodao2.findBysName("张三");
			System.out.println(foo);
		}
		{
			/**
			 * Like查询，不使用@Param来绑定
			 */
			Foo foo = foodao2.findByusername("张");
			System.out.println(foo);
		}
		// =========================
		{
			/**
			 * 1 @Query(name='xxx')可以从预定义的命名查询中获得一个配置好的查询语句 2 如果使用 @Param 对应
			 * :name，那么方法的参数先后顺序可以随意修改。反之，如果是 ?1 ?2方式进行参数绑定，则方法参数顺序有要求。
			 */
			//
			List<Foo> foos = foodao2.findBySql(new Date(), "李四").collect(Collectors.toList());
			System.out.println(foos);
		}
		// =========================
		{
			/**
			 * 没有设置@param参数时， ?1 ?2 来分别表示第一个和第二个参数。此时方法参数顺序有要求。
			 */
			List<Foo> foos = foodao2.findBySql2("李四", new Date());
			System.out.println(foos);

		}
		{
			/**
			 * 单纯的Like运算符不会在查询子条件 李四上增加通配符。因此需要自己传入通配符 %李%
			 */
			System.out.println("=== findBySql3() ====");
			Foo foo = foodao2.findBySql3("李", 0);
			System.out.println(foo);
		}
		{
			/**
			 * 用?1 ?2绑定时，顺序要注意。 如果在SQL语句中指定LIKE的查询方式是 ‘匹配头部’，那么查询就能符合期望
			 */
			System.out.println("=== findBySql4() ====");
			Foo foo = foodao2.findBySql4(0, "李");
			System.out.println(foo);
		}
		{
			/**
			 * 1、SQL语句查询支持分页功能 2、传入null可以表示忽略对应的查询条件吗（动态SQL）? 默认情况下是不可以的，但是使用
			 * 
			 * @IgnoreIf()注解可以化不可能为可能
			 */
			Page<Foo> page = (Page<Foo>) foodao2.findBySql5(0, null, PageRequest.of(1, 4));
			System.out.println(page.getTotalElements());
			System.out.println(page.getContent());
		}
		{
			/**
			 * 1、like <$string$>的用法 2、顺序不能用Spring-data的方法传入并使用。 但是可以换一种方法
			 */
			System.out.println("=== findBySql6() ====");
			List<Foo> result = foodao2.findBySql6(0, "张", Sort.by(new Order(Direction.DESC, "id")));
			System.out.println(result);

			System.out.println("=== findBySql6-2() ====");
			result = foodao2.findBySql62(0, "张", "id desc");
			System.out.println(result);
		}
		{
			/**
			 * 1、条件自动省略 2、如果条件中带有 % _等特殊符号，会自动转义
			 */
			Page<Foo> page = foodao2.findBySql7(0, "李%", PageRequest.of(3, 5));
			System.out.println(page.getContent());

			page = foodao2.findBySql7(0, "", PageRequest.of(3, 5));
			System.out.println(page.getContent());
		}
		{
			/**
			 * 使用SQL语句插入记录
			 */
			int ii = foodao2.insertInto("六河", 333, "测试", new Date());
			System.out.println(ii);
			ii = foodao2.insertInto2("狂四", 555, "测试", new Date());
			System.out.println(ii);
		}
		{
			/**
			 * 使用SQL语句来update
			 */
			int ii = foodao2.updateFooSetAgeByAgeAndId(new Date(), 12, 2);
			System.out.println(ii);
		}
	}

	/**
	 * 当有复合主键时
	 */
	@Test
	public void testComplexId() {
		// 测试复合主键的情况
		{
			ComplexFoo cf = new ComplexFoo(1, 2);
			cf.setMessage("test");
			complex.save(cf);

			cf = new ComplexFoo(2, 2);
			cf.setMessage("1222234324");
			complex.save(cf);
		}
		{
			ComplexFoo cf = complex.getOne(new int[] { 1, 2 });
			System.out.println(cf);
			cf.setMessage("修改消息!");
			complex.save(cf);
		}
		{
			Iterable<ComplexFoo> list = complex.findAllById(Arrays.asList(new int[] { 1, 2 }, new int[] { 2, 2 }));
			for (ComplexFoo foo : list) {
				System.out.println(foo);
			}
		}
		{
			complex.deleteById(new int[] { 1, 2 });
		}
	}

	/**
	 * 锁案例1-1： 乐观锁
	 */
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
			commonDao.updateByPK(v);
		}
		{
			VersionLog v = commonDao.load(VersionLog.class, id);

			VersionLog v2 = commonDao.load(VersionLog.class, id);
			v2.setName("抢先更新");
			commonDao.updateByPK(v2);
			try {
				v.setName("啥，已经被人更新了，那我不是写不进去了！");
				commonDao.updateByPK(v);
			} catch (OptimisticLockException e) {
				e.printStackTrace();
				throw e;
			}
		}

	}

	/**
	 * 锁案例1-2：批量更新模式下的乐观锁
	 */
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
			commonDao.updateByPK(v_);

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
	 * 锁案例2：悲观锁的使用
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
			List<VersionLog> records = commonDao.find(query);
			for (VersionLog version : records) {
				System.out.println(version.getName());
			}
		}

	}

	/**
	 * 锁案例3：特定场景下不需要加锁更新
	 */
	@Test
	public void testUpdateWithoutLock() {
		List<Foo> foos = commonDao.find(QB.create(Foo.class));
		Foo foo = foos.get(0);
		Query<Foo> query=Entities.asQuery(foo);
		QB.fieldAdd(query, Foo.Field.age, 100);
		foo.setName("姓名也更新了");
		commonDao.update(query);
	}

	/**
	 * 使用自行实现的扩展方法
	 */
	@Test
	public void testCustom() {
		ComplexFoo cf = new ComplexFoo(1, 2);
		complex.someCustomMethod(cf);
	}

	/**
	 * 在使用Spring-data的同时，传统的commondao/Session等方式操作依然可以正常使用
	 * 
	 * @throws SQLException
	 */
	@Test
	public void test1() throws SQLException {
		commonDao.getNoTransactionSession().dropTable(Foo.class);
		commonDao.getNoTransactionSession().createTable(Foo.class);
		{
			Foo foo = new Foo();
			foo.setName("Hello!");
			commonDao.batchInsert(Arrays.asList(foo));
		}
		{
			Foo foo = new Foo();
			foo.setAge(3);
			foo.setName("Hello!");
			// update MY_FOO set age=3 where name='Hello!'
			commonDao.updateByProperty(foo, "name");
		}
		{
			Foo foo = commonDao.loadByPrimaryKey(Foo.class, 1);
			System.out.println(foo.getName());
		}
		{
			// 根据ID删除
			commonDao.removeByField(Foo.class, "id", 1);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ORMConfig.getInstance().setDebugMode(false);
		commonDao.getNoTransactionSession().dropTable(Foo.class, VersionLog.class);
		commonDao.getNoTransactionSession().createTable(Foo.class, VersionLog.class);
		{
			List<Foo> list = new ArrayList<Foo>();
			list.add(new Foo("张三"));
			list.add(new Foo("李四"));
			list.add(new Foo("王五"));
			list.add(new Foo("张昕"));
			list.add(new Foo("张鑫"));
			list.add(new Foo("测试"));
			list.add(new Foo("张三丰"));
			list.add(new Foo("李元吉"));
			list.add(new Foo("李渊"));
			list.add(new Foo("李建成"));
			list.add(new Foo("李世民"));
			list.add(new Foo("赵日天"));
			list.add(new Foo("叶良辰"));
			list.add(new Foo("玛丽苏"));
			list.add(new Foo("龙傲天"));
			foodao.saveAll(list);
		}
		{
			VersionLog v1 = new VersionLog();
			v1.setName("一见钟情");
			VersionLog v2 = new VersionLog();
			v2.setName("两两相依");
			VersionLog v3 = new VersionLog();
			v3.setName("三生三世");
			VersionLog v4 = new VersionLog();
			v4.setName("四海为家");
			commonDao.batchInsert(Arrays.asList(v1, v2, v3, v4));
		}
		ORMConfig.getInstance().setDebugMode(true);
		System.out.println("============= 下面案例正式开始 ===========");
	}

	@Test
	public void useQueryDSLPrepare() throws SQLException {

		 commonDao.getNoTransactionSession().dropTable(Account.class);
		 commonDao.getNoTransactionSession().dropTable(Role.class);
		 commonDao.getNoTransactionSession().dropTable(AccountRoleRelation.class);
		commonDao.getNoTransactionSession().createTable(Account.class);
		commonDao.getNoTransactionSession().createTable(Role.class);
		commonDao.getNoTransactionSession().createTable(AccountRoleRelation.class);

		Role r1 = new Role();
		r1.setId("admin");
		r1.setRoleCode("1");
		r1.setRoleName("admin");
		r1.setCreated(new Date());
		r1.setType(RoleType.Custom);
		Role r2 = new Role();
		r2.setId("guest");
		r2.setRoleCode("2");
		r2.setRoleName("guest");
		r2.setCreated(new Date());
		r2.setType(RoleType.Custom);
		Role r3 = new Role();
		r3.setId("anonymous");
		r3.setRoleCode("3");
		r3.setRoleName("anonymous");
		r3.setType(RoleType.Custom);
		r3.setCreated(new Date());
		commonDao.batchInsert(Arrays.asList(r1, r2, r3));

		Account a = RandomData.newInstance(Account.class);
		a.setAccountName("jiyi");
		a.setDataValid(true);
		commonDao.insert(a);

		a = RandomData.newInstance(Account.class);
		a.setAccountName("Kim");
		a.setDataValid(true);
		commonDao.insert(a);

		AccountRoleRelation r = new AccountRoleRelation();
		r.setAccount("jiyi");
		r.setRoleId("admin");
		commonDao.insert(r);

		r = new AccountRoleRelation();
		r.setAccount("Kim");
		r.setRoleId("anonymous");
		commonDao.insert(r);

	}

	@Test
	public void useQueryDSL() {

		SQLRelationalPath<Account> qaccount = QueryDSLTables.table(Account.class);
		SQLRelationalPath<Role> qrole = QueryDSLTables.table(Role.class);
		SQLRelationalPath<AccountRoleRelation> qAccountRoleRelation = QueryDSLTables.table(AccountRoleRelation.class);
		String[] roleCodes = new String[] { "admin", "guest", "anonymous" };

		// SQLQuery<Tuple> query = commonDao.getSession().sqlFactory().query();

		SQLQueryEx<Tuple> query = commonDao.getSession().sqlFactoryEx()
				.select(qaccount.column(Account.Field.accountName), qaccount.column(Account.Field.userId), qrole.column(Role.Field.roleName)).from(qaccount)
				.join(qAccountRoleRelation).on(qaccount.string(Account.Field.accountName).eq(qAccountRoleRelation.string(AccountRoleRelation.Field.account)))
				.join(qrole).on(qAccountRoleRelation.column(AccountRoleRelation.Field.roleId).eq(qrole.column(Role.Field.id)))
				.where(qaccount.column(Account.Field.dataValid).eq(true).and(qrole.string(Role.Field.roleName).in(roleCodes)));

		List<AccountRoleCodeVO> result = query.fetchAs(AccountRoleCodeVO.class);
		System.out.println(result);

		System.out.println(query.count());
	}

	public static class AccountRoleCodeVO {
		@Column(name = "account_name")
		private String accountName;

		@Column(name = "user_id")
		private String userId;

		@Column(name = "role_name")
		private String roleName;

		public String getAccountName() {
			return accountName;
		}

		public void setAccountName(String accountName) {
			this.accountName = accountName;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getRoleName() {
			return roleName;
		}

		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

	}
}
