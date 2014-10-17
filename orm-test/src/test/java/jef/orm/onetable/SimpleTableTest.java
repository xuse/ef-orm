package jef.orm.onetable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Not;
import jef.database.IConditionField.Or;
import jef.database.NativeQuery;
import jef.database.ORMConfig;
import jef.database.PagingIterator;
import jef.database.QB;
import jef.database.Sequence;
import jef.database.Transaction;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Func;
import jef.database.query.JpqlExpression;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.support.RDBMS;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.populator.Transformer;
import jef.orm.multitable.model.Person;
import jef.orm.onetable.model.CaAsset;
import jef.orm.onetable.model.TestEntity;
import jef.orm.onetable.model.TestEntitySon;
import jef.tools.DateUtils;
import jef.tools.ThreadUtils;
import jef.tools.string.RandomData;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"), 
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
	@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	@DataSource(name = "derby", url = "jdbc:derby:./db;create=true"), 
	@DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"), 
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class SimpleTableTest extends org.junit.Assert {
	private DbClient db;

	@BeforeClass
	public static void setUp() {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("jef.orm.onetable.model,");
	}

	@DatabaseInit
	public void cleanRecords() {
		try {
			// 清除缓存，否则测试案例清理不完整
			if (db.getProfile(null).getName() == RDBMS.oracle) {
				ITableMetadata meta = MetaHolder.getMeta(TestEntity.class);
				Sequence holder = db.getSqlTemplate(null).getSequence(meta.getFirstAutoincrementDef());
				holder.clear();

				meta = MetaHolder.getMeta(CaAsset.class);
				holder = db.getSqlTemplate(null).getSequence(meta.getFirstAutoincrementDef());
				holder.clear();
			}
			db.dropTable(TestEntity.class, CaAsset.class); // 删除表
			db.refreshTable(TestEntity.class); // 创建表
			db.refreshTable(CaAsset.class);
			CaAsset t1 = new CaAsset();
			t1.setNormal("asfc");
			t1.setAssetType(1);
			db.insert(t1);
			ThreadUtils.doSleep(500);
		} catch (SQLException e) {
			LogUtil.exception(e);
		}
	}

	/**
	 * 直接用对象构造出最简单的update语句
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testDirectUpdate() throws SQLException {
		long last = insert3Records();

		TestEntity e = new TestEntity();
		e.setLongField(last);
		e.setIntFiled(1);
		e.setLongField2(2L);
		int count = db.update(e);
		assertTrue(count > 0);
	}

	@Test
	public void testCase1XX() throws SQLException {
		db.refreshTable(TestEntity.class);

	}

	/**
	 * 使用普通方式插入
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase1() throws SQLException {
		db.delete(QB.create(TestEntity.class));

		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		TestEntity t2 = RandomData.newInstance(TestEntity.class);
		TestEntity t3 = RandomData.newInstance(TestEntity.class);
		TestEntity t4 = RandomData.newInstance(TestEntity.class);
		t1.startUpdate();
		t2.startUpdate();
		t3.startUpdate();
		t4.startUpdate();
		t1.setDateField(null);
		db.insert(t1);
		long base = t1.getLongField() - 1;
		assertEquals(base + 1L, t1.getLongField());

		db.insert(t2);
		assertEquals(base + 2L, t2.getLongField());

		db.insert(t3);
		assertEquals(base + 3L, t3.getLongField());

		t4.setField2("field2 of t4");
		db.insert(t4);
		assertEquals(base + 4L, t4.getLongField());

		System.out.println("rowid:" + t1.rowid());
		System.out.println("rowid:" + t2.rowid());
		System.out.println("rowid:" + t3.rowid());
		System.out.println("rowid:" + t4.rowid());

		t1 = db.load(t1);
		t2 = db.load(t2);
		t3 = db.load(t3);
		TestEntity t4_ = db.load(t4);

		System.out.println("rowid:" + t1.rowid());
		System.out.println("rowid:" + t2.rowid());
		System.out.println("rowid:" + t3.rowid());
		System.out.println("rowid:" + t4_.rowid());

		assertNotNull(t1);
		assertNotNull(t2);
		assertNotNull(t3);
		assertNotNull(t4_);
		System.out.println(t1.getDateField());
		assertNull(t1.getDateField());
		assertNotNull(t2.getDateField());
		assertEquals("field2 of t4", t4_.getField2());
	}

	@Test
	@IgnoreOn({ "oracle", "mysql", "derby", "hsqldb" })
	public void testCaseForPG() throws SQLException {
		db.delete(QB.create(TestEntity.class));
		CaAsset t1 = RandomData.newInstance(CaAsset.class);
		db.insert(t1);
		Transaction session = db.startTransaction();
		CaAsset t2 = RandomData.newInstance(CaAsset.class);
		t2.setAssetId(t1.getAssetId());
		try {
			session.insert(t2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("===" + session.getExpressionValue(session.func(Func.current_timestamp).toString(), Object.class));
		session.commit(true);
	}

	@Test
	public void testForChenfy() throws SQLException {

		TestEntity t1 = new TestEntity();
		t1.setField1("aaaaa");
		t1.setField2("萨芬的男生方式飞上的风格 ");
		t1.setBinaryData("afdsfgsdgfdgdfgfd0".getBytes());
		db.insert(t1);
		System.out.println("自增主键值：" + t1.getLongField());

		{
			if (!ORMConfig.getInstance().isEnableLazyLoad()) {
				ORMConfig.getInstance().setEnableLazyLoad(true);
			}
			// 测试OB延迟加载功能
			TestEntity q = new TestEntity();
			q.setLongField(t1.getLongField());
			t1 = db.load(q);
			System.out.println(t1.getField2());
			assertEquals("萨芬的男生方式飞上的风格 ", t1.getField2());
		}
	}

	/**
	 * 使用Between条件查询
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectBetween() throws SQLException {
		db.delete(QB.create(TestEntity.class));
		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		t1.setDateField(DateUtils.getDate(2012, 5, 1, 16, 24, 30));
		db.insert(t1);

		Query<TestEntity> query = QB.create(TestEntity.class);
		java.util.Date d = DateUtils.getDate(2012, 5, 1);
		query.addCondition(QB.between(TestEntity.Field.dateField, DateUtils.dayBegin(d), DateUtils.dayEnd(d)));
		System.out.println(db.count(query));
		List<TestEntity> list = db.select(query);
		assertEquals(1, list.size());
	}

	@Test
	public void testaBatch1() throws SQLException{
		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		t1.setLongField(0);
		db.batchInsert(Arrays.asList(t1));
		long n = t1.getLongField();
		System.out.println(t1.getLongField());
	}
	
	
	/**
	 * @测试功能 批量插入
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase2() throws SQLException {
		System.out.println("=========== testCase2 ===========");
		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		TestEntity t2 = RandomData.newInstance(TestEntity.class);
		TestEntity t3 = RandomData.newInstance(TestEntity.class);
		TestEntity t4 = RandomData.newInstance(TestEntity.class);
		t1.setLongField(0);
		t2.setLongField(2);
		t3.setLongField(3);
		t4.setLongField(9);

		db.batchInsert(Arrays.asList(t1, t2, t3, t4));
		long n = t1.getLongField();
		assertEquals(n, t1.getLongField());
		assertEquals(n + 1, t2.getLongField());
		assertEquals(n + 2, t3.getLongField());
		assertEquals(n + 3, t4.getLongField());
	}

	/**
	 * @测试功能 Batch update 更新包括BLOB在内的多个字段，更新为表达式，当前日期等多种值
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase3() throws SQLException {
		insert3Records();

		// 测试
		{
			List<TestEntity> list = db.selectAll(TestEntity.class);
			for (TestEntity t1 : list) {
				t1.setBoolField(false);
				t1.setField1("update!" + t1.getField1());
				t1.setBinaryData("updated!".getBytes());// 简单的更新，直接在对象赋值
				// 下面是集中复杂的更新赋值
				t1.prepareUpdate(TestEntity.Field.doubleField, TestEntity.Field.doubleField2);// 更新为另外一个字段的值
				t1.prepareUpdate(TestEntity.Field.dateField, db.func(Func.now)); // 更新为数据库的当前时间
				t1.prepareUpdate(TestEntity.Field.intFiled, new JpqlExpression("intFiled + intField2"));// 更新为当前字段值加上另一个字段的值

			}
			db.batchUpdate(list);
		}

		// 开始检查数据
		{
			List<TestEntity> list = db.selectAll(TestEntity.class);
			for (TestEntity t1 : list) {
				assertEquals(t1.isBoolField(), false);
				assertTrue(t1.getField1().startsWith("update!"));
				assertEquals("updated!", new String(t1.getBinaryData()));
			}
		}

	}

	/**
	 * 测试单条语句更新记录
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testUpdate() throws SQLException {
		long lastId = insert3Records();
		{
			TestEntity t1 = new TestEntity();
			t1.setLongField(lastId - 2);
			t1 = db.load(t1);
			t1.setField2(null);
			t1.prepareUpdate(TestEntity.Field.dateField, db.func(Func.current_timestamp));
			t1.setBinaryData("人间".getBytes());
			int i = db.update(t1);
			assertEquals(1, i);	
		}
	}

	@Test
	@IgnoreOn("sqlite")  //SQLite由于当前时区的问题，该案例不过
	public void testUpdate2() throws SQLException {
		db.delete(QB.create(TestEntity.class));
		long lastId = insert3Records();
		//增加案例，支持
		{
			TestEntity t1 = new TestEntity();//>>>
			t1.getQuery().addCondition(new FBIField("date(createTime)"), DateUtils.sqlToday());
			t1.setField2("uuuuuuuuuuuuuu");
			int i=db.update(t1);
			assertEquals(3, i);
			
		}
		{
			//这个案例测试时要注意，在Oracle中，即便传入java.sql.Date对象，Oracle仍然会将其当作date类型（带时分秒进行判断）
			TestEntity t1 = new TestEntity();
			t1.getQuery().addCondition(new FBIField("date(createTime)"), DateUtils.sqlToday());
			int i=db.delete(t1);
			assertEquals(3, i);
		}
	 }
	/**
	 * 测试遍历模式的查询
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCasex() throws SQLException {
		// 准备数据
		db.delete(QB.create(TestEntity.class));
		TestEntity t0 = RandomData.newInstance(TestEntity.class);
		db.insert(t0);
		db.insert(t0);
		db.insert(t0);
		db.insert(t0);
		db.insert(t0);

		List<TestEntity> list = db.selectAll(TestEntity.class);
		// 开始测试
		int n = 0;
		ResultIterator<TestEntity> iter = db.iteratedSelect(QB.create(TestEntity.class), null);
		try {
			for (; iter.hasNext();) {
				iter.next();
				n++;
			}
		} finally {
			iter.close();
		}
		assertEquals(list.size(), n);
	}

	@Test
	public void testIterated() throws SQLException {
		insert3Records();

		// 准备数据

		List<TestEntity> list = db.selectAll(TestEntity.class);
		// 开始测试
		int n = 0;
		ResultIterator<TestEntity> iter = db.getSqlTemplate(null).iteratorBySql("select * from test_entity", new Transformer(TestEntity.class), 0, 0);
		try {
			for (; iter.hasNext();) {
				iter.next();
				n++;
			}
		} finally {
			iter.close();
		}
		assertEquals(list.size(), n);
	}

	@Test
	public void testCasex1() throws SQLException {
		// 准备数据
		db.delete(QB.create(TestEntity.class));
		TestEntity t0 = RandomData.newInstance(TestEntity.class);
		db.insert(t0);
		db.insert(t0);
		db.insert(t0);
		db.insert(t0);
		db.insert(t0);

		NativeQuery<TestEntity> query = db.createNativeQuery("select * from test_entity", TestEntity.class);
		ResultIterator<TestEntity> iter = query.getResultIterator();
		int n = 0;
		try {
			for (; iter.hasNext();) {
				TestEntity entity = iter.next();
				n++;
			}
		} finally {
			iter.close();
		}
		assertEquals(5, n);
	}

	/**
	 * 测试删除记录
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testRemove() throws SQLException {
		long lastId = insert3Records();
		TestEntity t1 = new TestEntity();
		t1.setLongField(lastId - 1);
		t1 = db.load(t1);
		t1.setBinaryData("人间".getBytes());
		t1.getQuery().addCondition(TestEntity.Field.dateField, db.func(Func.current_timestamp));
		t1.clearQuery();
		int deleted = db.delete(t1);// 清除了刚刚设置进去的条件
		assertEquals(1, deleted);
	}

	/**
	 * 测试分页查找
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testPaging() throws SQLException {
		insert3Records();
		insert3Records();
		System.out.println("=========== testPaging  Begin ==========");
		Query<TestEntity> q = QB.create(TestEntity.class);
		Selects select=QB.selectFrom(q);
		select.column(TestEntity.Field.longField).as("lf1").toField("longField");
		
		q.addCondition(TestEntity.Field.boolField, true);
		q.orderByAsc(TestEntity.Field.longField);
		PagingIterator<TestEntity> page = db.pageSelect(q, 5);
		System.out.println("Total Page:" + page.getTotalPage());
		for (; page.hasNext();) {
			List<TestEntity> list = page.next();
		}
		System.out.println("=========== testPaging  End ==========");
	}

	/**
	 * 测试批量删除
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testBatchRemove() throws SQLException {
		db.delete(QB.create(TestEntity.class));
		insert3Records();
		long lastId = insert3Records();
		int num = db.count(QB.create(TestEntity.class));
		assertEquals(6, num);

		System.out.println("=========== testBatchRemove  Begin ==========");
		TestEntity t1 = new TestEntity();
		TestEntity t3 = new TestEntity();
		TestEntity t5 = new TestEntity();
		t1.setLongField(lastId - 4);
		t3.setLongField(lastId - 2);
		t5.setLongField(lastId);

		List<TestEntity> batch = new ArrayList<TestEntity>();
		batch.add(t1);
		batch.add(t3);
		batch.add(t5);
		db.batchDelete(batch);
		num = db.count(QB.create(TestEntity.class));
		assertEquals(3, num);
		System.out.println("=========== testBatchRemove  Begin ==========");
	}

	/**
	 * 测试用Field=Field这样的表达式作为条件
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testFieldToField() throws SQLException {
		java.util.Date date = new java.util.Date();
		TestEntity data = RandomData.newInstance(TestEntity.class);
		data.setBoolField(true);
		data.setBoolField2(true);
		data.setField2("hello world!!");
		data.setDateField(date);
		db.insert(data);

		TestEntity e = new TestEntity();
		e.setBoolField(false);
		e.getQuery().addCondition(TestEntity.Field.boolField, TestEntity.Field.boolField2);
		e.getQuery().addCondition(TestEntity.Field.dateField, date);
		int count = db.update(e);
		assertEquals(1, count);
	}

	/**
	 * 测试使用NativeQuery来查询数据
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNativeQueryLike() throws SQLException {
		db.truncate(TestEntity.class);

		TestEntity data = RandomData.newInstance(TestEntity.class);
		data.setField2("hello aa world!!");
		db.insert(data);

		NativeQuery<TestEntity> q = db.createNativeQuery("select * from test_entity where field_2 like :likestr<$string$> ", TestEntity.class);
		q.setParameter("likestr", "aa");

		// 查询并检查数据
		assertEquals(1, q.getResultCount());
		List<TestEntity> result = q.getResultList();
		assertEquals(1, result.size());
	}

	private long insert3Records() throws SQLException {
//		ORMConfig.getInstance().setDebugMode(false);
		TestEntity t1 = RandomData.newInstance(TestEntity.class);
		t1.setLongField(1);
		db.insert(t1);
		TestEntity t2 = RandomData.newInstance(TestEntity.class);
		t1.setLongField(2);
		db.insert(t2);
		TestEntity t3 = RandomData.newInstance(TestEntity.class);
		t1.setLongField(3);
		db.insert(t3);
		ORMConfig.getInstance().setDebugMode(true);
		return t3.getLongField();
	}

	/**
	 * Normal Insert And load
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCassetInsertLoad() throws SQLException {
		Query<?> q = QB.create(CaAsset.class);
		QB.selectFrom(q).column(CaAsset.Field.acctId).max();
		q.getResultTransformer().setResultType(Integer.class);
		Integer max = db.load(q);
		if (max == null)
			max = 0;
		System.out.println("=========== test1 ========");

		CaAsset t2 = new CaAsset();
		t2.setAssetId(max + 1);
		t2.setAssetType(1);
		t2.setNormal("aa");
		db.insert(t2);
		CaAsset t3 = new CaAsset();
		t3.setAssetId(1);
		t3.setNormal("bb");
		db.insert(t3);
		CaAsset t4 = new CaAsset();
		t4.setNormal("cc");
		db.insert(t4);

		t2 = db.load(t2);
		t3 = db.load(t3);
	}

	@Test
	public void testAssignPK() throws SQLException {
		System.out.println("=========== testAssignPK ========");
		CaAsset t1 = new CaAsset();
		t1.setAssetId(500);
		t1.setAssetType(1);
		db.insert(t1);
	}

	/**
	 * Batch Insert
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCaAssetBatchInsert() throws SQLException {
		System.out.println("=========== test2 ========");
		CaAsset t1 = new CaAsset();
		t1.setNormal("aaa123");
		CaAsset t2 = new CaAsset();
		t2.setNormal("aaa124");
		CaAsset t3 = new CaAsset();
		t3.setNormal("aaa125");
		CaAsset t4 = new CaAsset();
		t4.setNormal("aaa126");
		db.batchInsert(Arrays.asList(t1, t2, t3, t4));

		long n = t1.getAssetId();

		assertEquals(n, t1.getAssetId());
		assertEquals(n + 1, t2.getAssetId());
		assertEquals(n + 2, t3.getAssetId());
		assertEquals(n + 3, t4.getAssetId());

		CaAsset t1_1 = db.load(t1);
		assertEquals("aaa123", t1_1.getNormal());

	}

	/**
	 * Batch update
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCaAssetBatchUpdate() throws SQLException {
		System.out.println("=========== test3 ========");
		List<CaAsset> list = db.selectAll(CaAsset.class);

		CaAsset t1 = list.get(0);
		t1.prepareUpdate(CaAsset.Field.assetType, 1, true);

		db.batchUpdate(list);

		// Batch<CaAsset> batch = db.startBatchUpdate(t1);
		// for (CaAsset t : list) {
		// batch.add(t);
		// }
		// batch.commit();
	}

	@Test
	public void testCase4() throws SQLException {
		// 准备数据
		db.delete(QB.create(CaAsset.class));
		int n = 4;
		CaAsset t1 = new CaAsset();
		t1.setAssetId(1);
		t1.setThedate(new Date());
		CaAsset t2 = new CaAsset();
		t2.setThedate(new Date());
		t2.setAssetId(2);
		CaAsset t3 = new CaAsset();
		t3.setThedate(new Date());
		t3.setAssetId(3);
		CaAsset t4 = new CaAsset();
		t4.setThedate(new Date());
		t4.setAssetId(4);
		db.batchInsert(Arrays.asList(t1, t2, t3, t4));

		// 开始测试
		Query<CaAsset> q = QB.create(CaAsset.class);
		q.addCondition(CaAsset.Field.thedate, DateUtils.dayBegin(new Date()));
		q.getInstance().prepareUpdate(CaAsset.Field.assetType, 3);
		db.update(q.getInstance());
	}

	@Test
	public void testCaAssetUpdate() throws SQLException {
		CaAsset data = RandomData.newInstance(CaAsset.class);
		data.setAcctId(123L);
		data.setNormal("hello world!");
		data.setThedate(new Date());
		db.insert(data);
		System.out.println("=========== testCaAssetUpdate ========");

		CaAsset t1 = new CaAsset();
		t1.setAssetId(data.getAssetId());
		t1 = db.load(t1);
		t1.prepareUpdate(CaAsset.Field.assetType, 3);
		t1.prepareUpdate(CaAsset.Field.thedate, new Date());
		t1.setNormal("dsdsdfsdf");
		int count = db.update(t1);
		assertEquals(1, count);

	}

	@Test
	public void testCaAssetRemove() throws SQLException {
		int max = db.loadBySql("select max(asset_id) from ca_asset", Integer.class);

		CaAsset t1 = new CaAsset();
		t1.setAssetId(max);
		t1 = db.load(t1);
		int count = db.delete(t1);
		assertEquals(1, count);
	}

	@Test
	public void testCaAssetPaging() throws SQLException {
		Query<CaAsset> q = QB.create(CaAsset.class);
		q.addCondition(CaAsset.Field.assetId, Operator.IN, Arrays.asList(new Long[] { 4L, 3L }));
		PagingIterator<CaAsset> page = db.pageSelect(q, 5);
		System.out.println("Total Page:" + page.getTotalPage());
		for (; page.hasNext();) {
			List<CaAsset> list = page.next();
		}
	}

	@Test
	public void testBatchInsertTrans() throws SQLException {
		// 清除全部数据
		db.delete(QB.create(CaAsset.class));
		assertEquals(0, db.selectAll(CaAsset.class).size());
		// 创建事务
		Transaction t = db.startTransaction();
		List<CaAsset> batch1 = new ArrayList<CaAsset>();
		batch1.add(new CaAsset("t1"));
		batch1.add(new CaAsset("t2"));
		batch1.add(new CaAsset("t3"));
		batch1.add(new CaAsset("t4"));
		t.batchInsert(batch1);// 批提交
		assertEquals(4, t.selectAll(CaAsset.class).size());// 从事务中查询到已插入的四条数据。

		// assertEquals(0,db.selectAll(CaAsset.class).size());//从事务外查询不到这四条数据(Oracle事务是隔离的，这里直接返回0。但是在derby上测试，由于表被锁，这里的查询无法返回结果，死锁)。

		t.rollback(true);// 回滚
		assertEquals(0, db.selectAll(CaAsset.class).size());// 还是查不到这四条数据
	}

	/**
	 * 测试能否把一个复杂SQL语句正确的转换为Count语句并执行分页查询
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testComplexPageSQL() throws SQLException {
		String sql = "SELECT t2.*  FROM ca_asset t2, (SELECT t.field_1, MAX(t.longfield) AS wo_run_id FROM test_entity t " +
	"GROUP BY t.field_1) t3 WHERE t2.normal = t3.field_1  AND t2.asset_type = t3.wo_run_id";
		PagingIterator<Person> pp = db.pageSelect(sql, Person.class, 10);
		assertEquals(0, pp.getTotal());
	}

	/**
	 * 使用Max函数的三种办法
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testMax() throws SQLException {
		insert3Records();
		// 方法1
		Query<TestEntity> query = QB.create(TestEntity.class);
		Selects s = QB.selectFrom(query);
		s.column(TestEntity.Field.longField).max().as("max_count");
		int max1 = db.selectAs(query, Integer.class).get(0);

		// 方法2
		int max2 = db.createNativeQuery("select max(longfield) from test_entity", Integer.class).getSingleResult();

		// 方法3 不支持schema重定向
		int max3 = (int) db.loadBySql("select max(longfield) from test_entity", Integer.class);

		assertEquals(max1, max2);
		assertEquals(max2, max3);
	}

	/**
	 * @throws SQLException 
	 * @测试目的 添加orderby条件时使用父类entity中的field发生异常的BUG是否已被修复(see Trac#78601)
	 * @预期结果 查询操作能正常解析并执行
	 */
	@Test
	public void testOrderByUsingParentField() throws SQLException {
		TestEntitySon entity = new TestEntitySon();
		db.dropTable(entity);
		db.createTable(entity);
		entity.getQuery().setAllRecordsCondition();
		entity.getQuery().addOrderBy(true, TestEntity.Field.field1);
		db.select(entity);
	}

	
	/**
	 * 
	 * And Or互相嵌套，再加上like语句的转义等混合场景下的操作.
	 * 
	 * Test And\or\Like(escape)
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCaseAndOr_Like() throws SQLException {
		TestEntity t = new TestEntity();
		t.setField1("Here insert field value");
		t.setField2("asa1bbb");
		t.setIntField2(1);
		t.setBoolField(true);
		db.insert(t); // 插入记录

		{
			System.out.println("======= 1 ========");

			t.setField1("value is updated!"); // 设置要更新的数据

			// (int_field_2=? and boolField=?)
			Condition and = QB.and(
				QB.eq(TestEntity.Field.intField2, 3),
				QB.eq(TestEntity.Field.boolField, Boolean.FALSE)
			);
			// ((int_field_2=? and boolField=?) or field_2 like ? escape '/' )
			Condition or = QB.or(
				and,
				QB.matchStart(TestEntity.Field.field2, "asa_") // 此处将自动转义
			);
			// ((int_field_2=? and boolField=?) or field_2 like ? escape '/' )
			// and int_field_2=?
			t.getQuery().addCondition(or);
			t.getQuery().addCondition(TestEntity.Field.intField2, 1);
			System.out.println("======== a =======");
			db.update(t);
		}
		{
			System.out.println("======== 2 =======");
			System.out.println(ORMConfig.getInstance().isSpecifyAllColumnName());
			TestEntity q = new TestEntity();
			And and = new And();
			and.addCondition(TestEntity.Field.dateField, new Date());
			and.addCondition(TestEntity.Field.boolField, Boolean.FALSE);

			Or or = new Or();
			or.addCondition(new Not(and));
			or.addCondition(TestEntity.Field.field2, Operator.MATCH_END, "asa");
			q.getQuery().addCondition(or);
			q.getQuery().addCondition(TestEntity.Field.intField2, 1);
			db.select(q);
			System.out.println("=========================");
		}
		db.delete(t); // 删除数据
	}
	
	@Test
	public void testBatchLoad() throws SQLException{
		CaAsset ca=db.load(CaAsset.class, 12);
		List<CaAsset> ca1=db.loadByField(CaAsset.Field.acctId, 12);
		Integer[] a=new Integer[501];
		for(int i=0;i<501;i++){
			a[i]=i+1;
		}
		
		List<CaAsset>  list1=db.batchLoad(CaAsset.class, Arrays.asList(a));
		
		List<CaAsset>  list2=db.batchLoadByField(CaAsset.Field.acctId, Arrays.asList(a));
		
	}
}
