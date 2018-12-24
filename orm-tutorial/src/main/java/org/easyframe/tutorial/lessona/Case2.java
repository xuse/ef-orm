package org.easyframe.tutorial.lessona;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.easyframe.tutorial.lessona.entity.Customer;
import org.easyframe.tutorial.lessona.entity.Device;
import org.easyframe.tutorial.lessona.entity.OperateLog;
import org.easyframe.tutorial.lessona.entity.Person2;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.common.wrapper.Page;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.NativeQuery;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.DefaultRoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.query.SqlExpression;
import jef.database.wrapper.ResultIterator;
import jef.tools.DateUtils;
import jef.tools.PageLimit;
import jef.tools.ThreadUtils;
import jef.tools.string.RandomData;

/**
 * 
 * 分库分表后的对象操作演示（基本）
 * 
 * @author jiyi
 * 
 */
@SuppressWarnings("rawtypes")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Case2 extends org.junit.Assert {
	private static DbClient db;
	private static boolean doinit = true;

	/**
	 * 准备测试数据
	 * 
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException {
		// 准备多个数据源
		Map<String, DataSource> datasources = new HashMap<String, DataSource>();
		// 创建三个数据库。。。
		datasources.put("datasource1", new SimpleDataSource("jdbc:derby:./db;create=true", null, null));
		datasources.put("datasource2", new SimpleDataSource("jdbc:derby:./db2;create=true", null, null));
		datasources.put("datasource3", new SimpleDataSource("jdbc:derby:./db3;create=true", null, null));
		MapDataSourceLookup lookup = new MapDataSourceLookup(datasources);
		lookup.setDefaultKey("datasource1");// 指定datasource1是默认的操作数据源
		// 构造一个带数据路由功能的DbClient
		db = new DbClient(new DefaultRoutingDataSource(lookup));
		
		if (doinit) {
			//现在删表，删表时会自动扫描目前存在的分表。
			db.dropTable(Customer.class, Device.class, OperateLog.class, Person2.class);

			 System.err.println("现在开始为Customer对象建表。Customer对象按“年_月”方式分表，并且按customerNo除以3的余数分库");
			 db.createTable(Customer.class);
			 System.out.println();

			System.err.println("现在开始为Device对象建表。Device对象按“IndexCode的头两位数字”分表，当头两位数字介于10~20时分布于ds1；21~32时分布于ds2；33~76时分布于ds3；其他情形时分布于默认数据源");
			db.createTable(Device.class);
			System.out.println();
			
			 System.err.println("现在开始为OperateLog对象建表。OperateLog对象按“年_月_日”方式分表，不分库");
			 db.createTable(OperateLog.class);
			 System.out.println();
			
			 System.err.println("现在开始为Person2对象建表。Person2对象是垂直拆分，因此所有数据都位于datasource2上。不分表");
			 db.createTable(Person2.class);
			 System.out.println();

			System.err.println("======= 建表操作完成，对于分区表只创建了可以预测到的若干表，实际操作中需要用到的表会自动按需创建=========");
		}
	}
	
	@Test
	public void ddlTest() throws SQLException{
		Customer customer=new Customer();
		customer.setCustomerNo(1234);
		customer.setCreateDate(DateUtils.getDate(2016,12,10));
		db.createTableByInstance(customer);
		db.dropTableByInstance(customer);
		
		
		Device d=new Device();
		d.setIndexcode("123456");
		db.createTableByInstance(d);
		db.dropTableByInstance(d);
	}
	
	/**
	 * 当分表结果计算后，对比数据库中实际存在的表。不存在的表不会参与查询。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testTableFilter() throws SQLException {
		Device d=new Device();
		d.setIndexcode("665565");
		db.createTableByInstance(d);
		ThreadUtils.doSleep(1000);
		System.out.println("第一次查询，表Device_6存在，故会查询此表");
		Query<Device> query = QB.create(Device.class);
		List<Device> device=db.select(query);
		
		db.dropTableByInstance(d);
		ThreadUtils.doSleep(500);
		System.out.println("第二次查询，由于表Device_6被删除，因此不会查此表");
		device=db.select(query);
		
	}
	

	/**
	 * 当分表结果计算后，发现没有需要查询的表的时候，会直接返回
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNoMatchTables() throws SQLException {
		Query<Device> d = QB.create(Device.class);
		//当Device_9表还不存在时，这个查询直接返回空
		if(!db.getMetaData(null).existTable("DEVICE_9")){
			d.addCondition(Device.Field.indexcode, "9999999");
			List<Device> empty=db.select(d);
			assertTrue(empty.isEmpty());
		}
	}

	/**
	 * 当采用垂直拆分时， Person2的所有操作都集中在DB2上。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void test1() throws SQLException {
		// 插入
		Person2 p2 = new Person2();
		p2.setName("Jhon smith");
		db.insert(p2);

		// 查询
		Person2 loaded = db.load(Person2.class, p2.getId());

		// 更新
		loaded.setName("Kingstone");
		assertEquals(1, db.update(loaded));

		// 查询数量
		assertEquals(1, db.count(QB.create(Person2.class)));

		// 删除
		assertEquals(1, db.delete(loaded));
		// 删除后
		assertEquals(0, db.count(QB.create(Person2.class)));
		
		System.out.println("=============== In Native Query ============");
		{
			String sql="insert into person2(name,data_desc,created,modified) values(:name,:data,sysdate,sysdate)";
			NativeQuery query=db.createNativeQuery(sql, Person2.class).withRouting();	
			query.setParameter("name", "测试111");
			query.setParameter("data", "备注信息");
			query.executeUpdate();
		}
		{
			String sql="select * from person2";
			NativeQuery<Person2> query=db.createNativeQuery(sql, Person2.class).withRouting();
			query.getResultCount();
			
			query.getResultList();	
		}
		{
			String sql="update person2 set name=:name where id=:id";
			NativeQuery query=db.createNativeQuery(sql).withRouting();
			query.setParameter("id", 1);
			query.setParameter("name", "测试222");
			query.executeUpdate();
		}
		{
			String sql="delete person2  where id=:id";
			NativeQuery query=db.createNativeQuery(sql).withRouting();
			query.setParameter("id", 2);
			query.executeUpdate();
		}
		{
			String sql="select * from person2 order by id limit 1,3";
			NativeQuery query=db.createNativeQuery(sql).withRouting();
			query.getResultList();
		}
		{
			String sql="select * from person2 order by id limit 3 offset 1";
			NativeQuery query=db.createNativeQuery(sql).withRouting();
			query.getResultList();
		}
		
	}

	/**
	 * Customer对象按创建记录created字段时的“年_月”方式分表，并且按customerNo除以3的余数分库
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCustomer() throws SQLException {
		// 插入
		Customer c = new Customer();
		c.setLastName("Hession");
		c.setFirstName("Obama");
		c.setDOB(DateUtils.getDate(1904, 3, 4));
		c.setDOD(DateUtils.getDate(1991, 7, 12));
		c.setEmail("obama@hotmail.com");
		db.insert(c);
		/*
		 * 这个案例中，用作分库分表的customerNo和created这两个字段都没赋值，但是记录却成功的插入了。
		 * 因为customerNo是自增值，created是系统自动维护的当前时间值。系统自动维护了这两个字段，从而使得分库分表的路由条件充分了。
		 * 此外，注意自增值的生成方式变化了。在本例中，使用TABLE来实现自增值。因为分库分表后，表的自增值已经无法保证唯一了,
		 * 而Derby不支持Sequence，所以采用TABLE方式生成自增值。(DERBY实际上支持，但兼容性有些问题，故关闭了)
		 */

		System.out.println("====将会按需建表====");
		Customer c2 = new Customer();
		c2.setLastName("Joe");
		c2.setFirstName("Lei");
		c2.setDOB(DateUtils.getDate(1981, 3, 4));
		c2.setEmail("joe@hotmail.com");
		c2.setCreateDate(DateUtils.getDate(2013, 5, 1));
		db.insert(c2);
		/*
		 * 这里指定记录创建时间为2013-5-1，也就是说该条记录需要插到表 CUSTOMER_201305这张表中。
		 * 这张表虽然现在不存在，但是EF-ORM会在需要用到时，自动创建这张表。
		 */

		// 路由维度充分时的查询
		System.out.println("当分表条件充分时，只需要查一张表即可...");
		Customer loaded1 = db.load(c);
		
		// 查询。
		System.out.println("当分表条件不完整时，需要查询同一个库上的好几张表。");
		Customer loaded = db.load(Customer.class, c.getCustomerNo());

		// 更新
		loaded.setLastName("King");
		assertEquals(1, db.update(loaded));

		// 查询数量，由于有没分库分表条件，意味着要查询所有库上，一定时间范围内的所有表
		assertEquals(1, db.count(QB.create(Customer.class)));

		// 删除
		assertEquals(1, db.delete(loaded));
		// 删除后
		assertEquals(0, db.count(QB.create(Customer.class)));
	}

	/**
	 * 演示Batch操作在分库分表后发生的变化
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCustomerBatch() throws SQLException {
		System.out.println("testCustomerBatch");
		db.truncate(Customer.class);
		List<Customer> list = new ArrayList<Customer>();
		// 批量插入
		Customer c = new Customer();
		c.setLastName("Hession");
		c.setFirstName("Obama");
		c.setDOB(DateUtils.getDate(1904, 3, 4));
		c.setDOD(DateUtils.getDate(1991, 7, 12));
		c.setEmail("obama@hotmail.com");
		list.add(c);

		c = new Customer();
		c.setLastName("Joe");
		c.setFirstName("Lei");
		c.setDOB(DateUtils.getDate(1981, 3, 4));
		c.setEmail("joe@hotmail.com");
		c.setCreateDate(DateUtils.getDate(2013, 5, 1));
		list.add(c);

		c = new Customer();
		c.setCustomerNo(4);
		c.setLastName("Yang");
		c.setFirstName("Fei");
		c.setDOB(DateUtils.getDate(1976, 12, 15));
		c.setEmail("fei@hotmail.com");
		c.setCreateDate(DateUtils.getDate(2013, 5, 1));
		list.add(c);
		
		System.out.println("当Batch操作遇到分库分表——\n所有记录将会重新归类后，相同的表的划为一批，进行批量插入。");
		db.batchInsert(list);

		// 跨表跨库的搜索
		Query<Customer> query = QB.create(Customer.class);
		query.addCondition(QB.between(Customer.Field.createDate, DateUtils.getDate(2013, 4, 30), DateUtils.getDate(2018, 12, 31)));

		// 艰难的查找——
		// 由于条件只有一个时间范围，因此会搜索所有可能出现记录的表。包括——三个数据库上所有时段在13年4月到14年12月的表。理论上所有可能的组合。
		// 但由于大部分表不存在。EF-ORM只会查找实际存在的表，实际上不存在的表不会被查找。
		list = db.select(query);
		assertEquals(3, list.size());

		// 批量更新
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setEmail("mail" + i + "@hotmail.com");
		}
		// 虽然是Batch更新，但实际上所有记录分散在不同的库的不同的表中，重新分组后，每张表构成一个批来更新
		db.batchUpdate(list);

		// 批量删除
		db.executeBatchDeletion(list);
	}

	/**
	 * 演示若干复杂的跨库Cirteria操作。这些操作虽然会涉及多个数据库的多张表，但在框架封装下，几乎对开发者做到了完全透明。
	 * 
	 * 案例说明：<br>
	 * 1、用批模式插入50条记录，这些记录将会分布在三个数据库的10张表中。由于分库维度和分表维度一致，因此实际情况为，表1,8,9位于DB，表2,3位于DB2，表4,5,6,7位于DB3。
	 * 2、跨库查询总数                        (看点:跨库记录数汇总)
	 * 3、查询index 为4开头的记录              （看点:条件解析与精确路由）
	 * 4、查询indexcode含0的记录，并按创建日期排序 (看点: 跨库排序)
	 * 5、查询indexcode含0的记录，并按创建日期排序，每页10条，显示第二页(看点: 跨库排序+分页)
	 * 6、所有记录,按type字段group by，并计算总数。（看点：跨库+聚合计算）
	 * 7、所有记录,按type字段group by，并计算总数。并按总数排序（看点：跨库+聚合计算+排序）
	 * 8、查询所有记录的type字段，并Distinct。  (看点：跨库 distinct)
	 * 9、跨数据库查询所有记录，并排序分页     (看点：跨库+分页)
	 * 10、跨数据库查询所有记录,按type分组后计算总数，通过having条件过滤掉总数在9以上的记录，最后排序 (看点：跨库+聚合+Having+排序)
	 * 11、跨数据库查询所有记录,按type分组后计算总数，通过having条件过滤掉总数在9以上的记录，最后排序，取结果的第3~5条。 (看点：跨库+聚合+Having+排序+分页)
	 * @throws SQLException
	 */
	@Test
	public void testDeviceSelect() throws SQLException {
		List<Device> list = generateDevice(50);
		ORMConfig.getInstance().setMaxBatchLog(2);
		db.batchInsert(list);
		System.err.println("=====1. 数据准备：插入50条记录完成=====");
		
		System.err.println("=====2. 跨库查询总数=====");
		System.out.println("当前总数是:" + db.count(QB.create(Device.class)));
		
		
		{
			System.err.println("=====3. 查询indexCode 4开头的记录完成=====");
			Query<Device> query = QB.create(Device.class);
			query.addCondition(QB.matchStart(Device.Field.indexcode, "4"));
			List<Device> results = db.select(query);
			LogUtil.show(results);
		}
		{
			System.err.println("=====4. 查询indexcode含0的记录，并按创建日期排序=====");
			// 分库分表后的难点之一——跨库查询并且并且排序
			Query<Device> query = QB.create(Device.class);
			query.addCondition(QB.matchAny(Device.Field.indexcode, "0"));// 显然这个查询意味着要调动好几个数据库上的好几张表
			query.orderByDesc(Device.Field.createDate); // 更麻烦的是，要对这些数据按日期进行排序。
			List<Device> results = db.select(query);
			for (Device ss : results) {
				System.out.println(ss);
			}

			System.err.println("=====5. 查询indexcode含0的记录，并按创建日期排序，每页10条，显示第二页=====");
			// 更麻烦一点——[跨库查询]并且并且[排序]还要[分页]——每页10条，从第二页开始显示
			Page<Device> page = db.pageSelect(query, Device.class, 10).setOffset(10).getPageData();
			System.out.println("总数:" + page.getTotalCount() + " 每页:" + page.getPageSize());
			LogUtil.show(page.getList());
		}
		{

			System.out.println("=====6. 跨数据库 聚合查询=====");
			// 分库分表后的难点之二——聚合查询
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct");
			select.column(Device.Field.indexcode).min().as("mins");
			select.column(Device.Field.indexcode).max().as("maxs");
			List<String[]> strs = db.selectAs(query, String[].class);
			for (String[] ss : strs) {
				System.out.println(Arrays.toString(ss));
			}
		}
		{
			System.out.println("=====7. 跨数据库 聚合+重新排序 查询=====");
			// 分库分表后的难点之二——聚合查询+再加上排序
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct");
			query.orderByDesc(new SqlExpression("ct")); // 除了聚合以外，再添加聚合后排序

			List<String[]> strs = db.selectAs(query, String[].class);
			for (String[] ss : strs) {
				System.out.println(Arrays.toString(ss));
			}
		}
		{

			System.out.println("=====8. 跨数据库 Distinct 查询=====");
			// 分库分表后的难点之三——Distinct操作
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type);
			select.setDistinct(true);
			
			
			List<Device> results = db.select(query);
			for (Device ss : results) {
				System.out.println(ss.getType());
			}
			assertTrue(results.size() > 0);
			assertNotNull(results.get(0).getType());
		}
		{
			System.out.println("=====9. 跨数据库查询并带排序分页=====");
			Query<Device> query = QB.create(Device.class);
			query.orderByDesc(Device.Field.indexcode);
			List<Device> results = db.select(query, new PageLimit(10, 10));
			for (Device ss : results) {
				System.out.println(ss);
			}
			assertTrue(results.size() > 0);
			assertNotNull(results.get(0).getType());
		}
		{
			System.out.println("=====10. 跨数据库 聚合+Having +重新排序=====");
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct").having(Operator.LESS_EQUALS, 9);
			query.orderByDesc(new SqlExpression("ct")); // 除了聚合以外，再添加聚合后排序

			List<String[]> strs = db.selectAs(query, String[].class);
			for (String[] ss : strs) {
				System.out.println(Arrays.toString(ss));
			}
		}
		{
			System.out.println("=====11. 跨数据库 聚合+Having +重新排序 + 分页=====");
			//反正已经够复杂了，分页也一起上吧！
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct").having(Operator.LESS_EQUALS, 12);
			query.orderByDesc(new SqlExpression("ct")); // 除了聚合以外，再添加聚合后排序
			List<Map> strs = db.selectAs(query, Map.class, new IntRange(3,5));
			for (Map ss : strs) {
				System.out.println(ss);
			}
		}
	}
	
	
	
	/**
	 * 不仅仅是Criteria API支持分库分表，SQL语句也可以自动分库分表
	 * 案例说明：<br>
	 * 1、用批模式插入50条记录，这些记录将会分布在三个数据库的10张表中。由于分库维度和分表维度一致，因此实际情况为，表1,8,9位于DB，表2,3位于DB2，表4,5,6,7位于DB3。
	 * 2、用SQL插入记录 x2
	 * 3、用SQL更新记录 x3
	 * 4、用SQL删除记录 x2
	 * 5、用SQL查询记录 x4
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNativeQuery() throws SQLException{
		List<Device> list = generateDevice(50);
		ORMConfig.getInstance().setDebugMode(false);
		db.batchInsert(list);
		ORMConfig.getInstance().setDebugMode(true);
		System.err.println("=====数据准备：插入50条记录完成=====");
		
		/**
		 * 使用SQL语句插入记录，根据传入的indexcode分布到不同数据库上
		 */
		{
			NativeQuery nq=db.createNativeQuery("insert into DeVice(indexcode,name,type,createDate) values(:indexcode, :name, :type, sysdate)").withRouting();;
			nq.setParameter("indexcode", "122346");
			nq.setParameter("name", "测试插入数据");
			nq.setParameter("type", "办公用品");
			nq.executeUpdate();
			
			nq.setParameter("indexcode", "7822346");
			nq.setParameter("name", "非官方的得到");
			nq.setParameter("type", "大家电");
			nq.executeUpdate();
			
			nq.setParameter("indexcode", "452346");
			nq.setParameter("name", "萨菲是方式飞");
			nq.setParameter("type", "日用品");
			nq.executeUpdate();			
		}
		/**
		 * 使用SQL语句更新记录，使用Between条件，这意味着indexcode在1000xxx到6000xxx段之间的所有表会参与更新操作。
		 */
		{
			System.out.println("===Between条件中携带的路由条件,正确操作表: 1,2,3,4,5,6");
			NativeQuery nq=db.createNativeQuery("update DeVice xx set xx.name='ID:'||indexcode,createDate=sysdate where indexcode between :s1 and :s2").withRouting();;
			nq.setParameter("s1", "1000");
			nq.setParameter("s2", "6000");
			nq.executeUpdate();
		}
		
		/**
		 * 使用SQL语句更新记录，使用In条件，这将精确定位到这些记录所在的表上。
		 */
		{
			System.out.println("===用IN条件更新==");
			NativeQuery nq=db.createNativeQuery("update Device set createDate=sysdate, name=:name where indexcode in (:codes)").withRouting();;
			nq.setParameter("name", "Updated value");
			nq.setParameter("codes", new String[]{"6000123","567232",list.get(0).getIndexcode(),list.get(1).getIndexcode(),list.get(2).getIndexcode()});
			nq.executeUpdate();
		}
		/**
		 * 使用SQL语句更新记录。由于where条件中没有任何用来缩小记录范围的条件，因此所有的表上都将执行更新操作
		 */
		{
			System.out.println("===正确操作表： 2==");
			NativeQuery nq=db.createNativeQuery("update Device set createDate=sysdate where type='办公用品' or indexcode='2002345'").withRouting();;
			nq.executeUpdate();
		}
		
		
		/**
		 * 删除记录。根据indexcode可以准确定位到三张表上。
		 */
		{
			System.out.println("===正确操作表： 1,6,5==");
			NativeQuery nq=db.createNativeQuery("delete Device where indexcode in (:codes)").withRouting();;
			nq.setParameter("codes", new String[]{"6000123","567232","110000"});
			nq.executeUpdate();
		}
		/**
		 * 删除记录，indexCode上的大于和小于条件以及OR关系勾勒出了这次查询的表,将会是DEVICE_2,DEVICE_3,DEVICE_4、DEVICE_7、DEVICE_8。(5张表)
		 */
		{
			System.out.println("===正确操作表： 2,3,4,5,7,8==");
			NativeQuery nq=db.createNativeQuery("delete Device where indexcode >'200000' and indexcode<'5' or indexcode >'700000' and indexcode <'8'").withRouting();;
			nq.executeUpdate();
		}
		
		/**
		 * 查询，所有表，跨库排序
		 */
		{
			System.out.println("查询，所有表，跨库排序");
			String sql="select t.* from device t where createDate is not null order by createDate";
			NativeQuery<Device> query=db.createNativeQuery(sql,Device.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预计查询结果总数Count:"+ total);
			List<Device> devices=query.getResultList();
			assertEquals(total, devices.size());
			int n=0;
			for(Device d: devices){
				System.out.println(d);
				if(n++>10)break;
			}
		}
		{
			System.out.println("查询，所有表，跨库排序并分页");
			String sql="select t.* from device t where createDate is not null order by createDate limit 5,6";
			NativeQuery<Device> query=db.createNativeQuery(sql,Device.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预计查询结果总数Count:"+ total);
			List<Device> devices=query.getResultList();
			assertEquals(total, devices.size());
			int n=0;
			for(Device d: devices){
				System.out.println(d);
				if(n++>10)break;
			}
		}
		/**
		 * 查询两个条件时，
		 */
		{
			System.out.println("======查询，Like条件，后一个条件无参数被省略，正确操作表 3==");
			String sql="select t.* from device t where createDate is not null and (t.indexcode like '3%' or t.indexcode in (:codes)) order by createDate";
			NativeQuery<Device> query=db.createNativeQuery(sql,Device.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预期查询总数Count:"+ total);
			int count=0;
			//使用迭代器方式查询
			ResultIterator<Device> devices=query.getResultIterator();
			for (;devices.hasNext();) {
				System.out.println(devices.next());
				count++;
			}
			devices.close();
			assertEquals(count, total);
			
			query.setParameter("codes", new String[]{"123456","8823478","98765"});
			System.out.println("======查询，Like条件 OR IN条件，正确操作表:1,3,8,9==");
			total=query.getResultCount();
			System.out.println("预期查询总数Count:"+ total);
			List<Device> rst=query.getResultList();
			assertEquals(total, rst.size());
			int n=0;
			for(Device d: rst){
				System.out.println(d);
				if(n++>10)break;
			}
		}
		
		/**
		 * 查询记录、如果SQL语句中带有Group条件...
		 * 跨库条件下的Group实现尤为复杂
		 */
		{
			System.out.println("查询——分组查询，正确操作表：4,1,6");
			String sql="select type,count(*) as count,max(indexcode) max_id from Device tx where indexcode like '4%' or indexcode like '1123%' or indexcode like '6%' group by type ";
			NativeQuery<Map> query=db.createNativeQuery(sql,Map.class).withRouting();;
			System.out.println("预期查询总数Count:"+ query.getResultCount());
			List<Map> devices=query.getResultList();
			for (Map ss : devices) {
				System.out.println(ss);
			}
		}
		
		/**
		 * 查询,SQL语句中带有distinct条件
		 */
		{
			
			String sql="select distinct type from device";
			NativeQuery<String> query=db.createNativeQuery(sql,String.class).withRouting();;
			long total=query.getResultCount();
			System.out.println("预期查询总数Count:"+ total);
			List<String> devices=query.getResultList();
			for (String ss : devices) {
				System.out.println(ss);
			}
			assertEquals(6, total);
			assertEquals(6, devices.size());
			
		}
	}
	
	/*
	 * 生成一些随机的数据
	 */
	private List<Device> generateDevice(int i) {
		List<Device> result = Arrays.asList(RandomData.newArrayInstance(Device.class, i));
		String[] types = { "耗材", "大家电", "办公用品", "日用品", "电脑配件", "图书" };
		for (Device device : result) {
			device.setIndexcode(String.valueOf(RandomData.randomInteger(100000, 990000)));
			device.setCreateDate(RandomData.randomDate(DateUtils.getDate(2000, 1, 1), DateUtils.getDate(2014, 12, 31)));
			device.setType(types[RandomData.randomInteger(0, 6)]);
		}
		return result;
	}
}
