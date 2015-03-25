package jef.orm.partition;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.PagingIterator;
import jef.database.QB;
import jef.database.Session.PopulateStrategy;
import jef.database.annotation.PartitionTable;
import jef.database.annotation.PartitionTableImpl;
import jef.database.meta.FBIField;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.database.query.RangeDimension;
import jef.database.query.Selects;
import jef.database.query.UnionQuery;
import jef.database.routing.PartitionResult;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.json.JsonUtil;
import jef.tools.DateUtils;
import jef.tools.IOUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 这个测试案例描述各种单表操作的分表计算办法
 * 1、目前实现的： 使用
 * @author Administrator
 * 
 * 
 * 分表路由功能改进计划：
 * 1、单表分表 (OK)
 * 2、单表分表支持多库...
 * 3、多表连接的分表
 * （多表连接情况下）
 * 4、多表连接的支持多库……
 * 
 *
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
 @DataSource(name = "derby", url = "jdbc:derby:./db;create=true"),
 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class PartitionTest extends org.junit.Assert {
	private DbClient db;
	
	public static class A{
		private Class clz;
	}
	
	@Test
	public void main1() {
		A a=new A();
		a.clz=int.class;
		String aa=JsonUtil.toJsonWithoutQuot(a);
		System.out.println(aa);
		A b=JsonUtil.toObject(aa,A.class);
		
		PartitionTable object=PartitionEntity.class.getAnnotation(PartitionTable.class);
		PartitionTableImpl my=PartitionTableImpl.create(object);
		String data=JsonUtil.toJsonWithoutQuot(my);
		System.out.println(data);
		
		PartitionTable rr=JsonUtil.toObject(data,PartitionTableImpl.class);
		System.out.println(rr);
	}

	@DatabaseInit
	public void createTables() throws SQLException {
		try{
			db.dropTable(PartitionEntity.class);
			db.createTable(PartitionEntity.class);
			
			PartitionEntity p = new PartitionEntity();
			p.setDateField(DateUtils.getDate(2012, 3, 1));
			p.setName("Zxa");
			db.dropTable(p);
			db.createTable(p);
	
//			p.setDateField(DateUtils.getDate(2012, 3, 2));
//			p.setName("XX");
//			db.dropTable(p);
//			db.createTable(p);
//			
//			p.setDateField(DateUtils.getDate(2012, 4, 1));
//			p.setName("a");
//			db.dropTable(p);
//			db.createTable(p);
//	
//			p.setDateField(DateUtils.getDate(2012, 5, 1));
//			p.setName("A");
//			db.dropTable(p);
//			db.createTable(p);
//	
//			p.setDateField(DateUtils.getDate(2012, 6, 1));
//			p.setName("XXX");
//			db.dropTable(p);
//			db.createTable(p);
//	
//			p.setDateField(DateUtils.getDate(2012, 7, 1));
//			p.setName(null);
//			db.dropTable(p);
//			db.createTable(p);
		} catch (Exception e) {
			LogUtil.exception(e);
		}
	}

	/**
	 * @测试对象
	 * 测试当  简单条件，between构成的Range条件时判断查询的表
	 * 
	 * @预期结果
	 * 查询03,04,05,06四张分表
	 * @throws SQLException
	 */
	@Test
	public void testBetween() throws SQLException {
		System.out.println("===================== testBetween 03,04,05,06=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.getInstance().setName("Zxa");
		q.addCondition(QB.between(PartitionEntity.Field.dateField, DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1)));
		q.addOrderBy(false, PartitionEntity.Field.name);
		db.select(q);
	}

	/**
	 * 
 	 * @测试对象
	 * 测试当 IN条件下时判断查询的表
	 * 
	 * @预期结果
	 * 查询03,06表
	 * @throws SQLException
	 */
	@Test
	public void testDimensionIn() throws SQLException {
		System.out.println("===================== testDimensionIn 03,06=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.addCondition(PartitionEntity.Field.dateField, Operator.IN, new Date[] { DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1) });
		q.addOrderBy(false, PartitionEntity.Field.name);
		db.select(q);
	}
	
	@Test
	public void testGroup() throws SQLException {
		System.out.println("===================== testDimensionIn 03,06=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		Selects items=QB.selectFrom(q);
		items.column(PartitionEntity.Field.longField).group().as("longField");
		items.column(PartitionEntity.Field.intField).count().as("intField");
		List<PartitionEntity> result=db.select(q);
		
	}

	/**
	 * @测试对象
	 * 测试当 三个普通eq条件用OR拼装 时判断查询的表
	 * 
	 * @预期结果
	 * 查询03,05,06表
	 * @throws SQLException
	 */
	@Test
	public void testDimension3ConditionOr() throws SQLException {
		System.out.println("===================== testDimension3ConditionOr 03,05,06=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.addCondition(QB.or(QB.eq(PartitionEntity.Field.dateField, DateUtils.getDate(2012, 3, 1)), QB.eq(PartitionEntity.Field.dateField, DateUtils.getDate(2012, 5, 1)), QB.eq(PartitionEntity.Field.dateField, DateUtils.getDate(2012, 6, 1))));
		q.addOrderBy(false, PartitionEntity.Field.name);
		/*
		 * select * from (select t.* from PARTITIONENTITY_05 t where (t.ID=? or
		 * t.ID=? or t.ID=?) union all select t.* from PARTITIONENTITY_06 t
		 * where (t.ID=? or t.ID=? or t.ID=?) union all select t.* from
		 * PARTITIONENTITY_03 t where (t.ID=? or t.ID=? or t.ID=?) ) t order by
		 * t.NAME DESC
		 */
		db.select(q);
	}

	/**
	 * @测试对象
	 * 测试当 对于大于和小于构成的区间 时判断查询的表
	 * 
	 * @预期结果
	 * 查询03,04,05,06表
	 * @throws SQLException
	 */
	@Test
	public void testDimensionSpan() throws SQLException {
		System.out.println("===================== testDimensionSpan 3,4,5,6=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.addCondition(PartitionEntity.Field.dateField, Operator.GREAT_EQUALS, DateUtils.getDate(2012, 3, 1));
		q.addCondition(PartitionEntity.Field.dateField, Operator.LESS, DateUtils.getDate(2012, 6,2));
		q.addOrderBy(false, PartitionEntity.Field.name);
		db.select(q);
	}

	/**
	 * @测试对象
	 * 较复杂的维度解析，一个 IN条件 AND 一个<的开区间Range条件
	 * 
	 * @预期结果
	 * 查询03表
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testDimensionInWithRange() throws SQLException {
		System.out.println("===================== testSpan 03=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.addCondition(PartitionEntity.Field.dateField, Operator.IN, new Date[] { DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1) });
		q.addCondition(PartitionEntity.Field.dateField, Operator.LESS, DateUtils.getDate(2012, 5, 1));
		q.addOrderBy(false, PartitionEntity.Field.name);
		db.select(q);
	}
	/**
	 * @测试对象
	 * 和上例一样，不同的是会将所有的有效区间都去掉，某种意义上分区条件构成的一个永远为false的表达式。
	 * 这种情况下，不存在任何能匹配用户查询条件的表。为了查询能够正确执行，我们使用基础表（即不带分表后缀的表做查询）
	 * 
	 * @预期结果
	 * 查询基础表
	 * 
	 * 错误，变化为查询全部实际存在的表……
	 * 
	 * 由于代码修改，造成基表都没有创建。。。。此时出错
	 * @throws SQLException
	 */
	@Test
	public void testDimensionInWithRange2() throws SQLException {
		System.out.println("===================== testSpan 原始表=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.addCondition(PartitionEntity.Field.dateField, Operator.IN, new Date[] { DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1) });
		q.addCondition(PartitionEntity.Field.dateField, Operator.LESS, DateUtils.getDate(2012, 3, 1));
		q.addOrderBy(false, PartitionEntity.Field.name);
		db.select(q);
	}

	
	/**
	 * 较复杂的维度解析，一个 IN条件 OR 一个Between的Range条件
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testDimensionInX() throws SQLException {
		System.out.println("===================== testSpan 03,05,06,07=======================");
		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		q.addCondition(QB.or(QB.in(PartitionEntity.Field.dateField, new Date[] { DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1) }), QB.between(PartitionEntity.Field.dateField, DateUtils.getDate(2012, 5, 1), DateUtils.getDate(2012, 7, 1))));
		q.addCondition(PartitionEntity.Field.intField,Operator.GREAT,14);
		q.addCondition(PartitionEntity.Field.intField,Operator.LESS,17);
		q.addOrderBy(false, PartitionEntity.Field.name);
		db.select(q);
	}

	/**
	 * 基础测试，测试分表维度的计算
	 */
	public void testMathRange() {
		RangeDimension<Integer> a = new RangeDimension<Integer>(3, 15);
		RangeDimension<Integer> b = new RangeDimension<Integer>(4, 16);
		RangeDimension<Integer> c = new RangeDimension<Integer>(3, 17, false, true);
		RangeDimension<Integer> d = new RangeDimension<Integer>(4, 6, false, false);
		RangeDimension<Integer> e = new RangeDimension<Integer>(null, 13);
		RangeDimension<Integer> f = new RangeDimension<Integer>(6, null);
		System.out.println(a + " AND " + b + " = " + a.mergeAnd(b));
		System.out.println(a + " AND " + c + " = " + a.mergeAnd(c));
		System.out.println(a + " AND " + d + " = " + a.mergeAnd(d));
		System.out.println(a + " AND " + e + " = " + a.mergeAnd(e));
		System.out.println(a + " AND " + f + " = " + a.mergeAnd(f));

		System.out.println(a + " OR " + b + " = " + a.mergeOr(b));
		System.out.println(a + " OR " + c + " = " + a.mergeOr(c));
		System.out.println(a + " OR " + d + " = " + a.mergeOr(d));
		System.out.println(a + " OR " + e + " = " + a.mergeOr(e));
		System.out.println(a + " OR " + f + " = " + a.mergeOr(f));

		RangeDimension<Date> g = new RangeDimension<Date>(new Date(), null);
		RangeDimension<Date> h = new RangeDimension<Date>(DateUtils.getDate(2011, 1, 1), DateUtils.getDate(2012, 5, 1), false, false);
		RangeDimension<Date> i = new RangeDimension<Date>(DateUtils.getDate(2011, 1, 1), DateUtils.getDate(2012, 5, 1), true, true);

		System.out.println(g + " AND " + h + " = " + g.mergeAnd(h));
		System.out.println(g + " AND " + i + " = " + g.mergeAnd(i));
		System.out.println(h + " AND " + i + " = " + h.mergeAnd(i));
		System.out.println(g + " OR " + h + " = " + g.mergeOr(h));
		System.out.println(g + " OR " + i + " = " + g.mergeOr(i));
		System.out.println(h + " OR " + i + " = " + h.mergeOr(i));
	}
	
	/**
	 * 当批操作时
	 * 批会对结果自动分组，然后将数据分别查到不同的表里去
	 * @throws SQLException
	 */
	@Test
	public void testPartitionBatchInsert() throws SQLException{
		System.out.println("======================testPartitionBatchInsert========================");
		db.createTable(PartitionEntity.class);
		
		List<PartitionEntity> batch=new ArrayList<PartitionEntity>();
		PartitionEntity p = new PartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 3, 1));
		p.setName("张三");
		batch.add(p);
		
		p = new PartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 5, 1));
		p.setName("王五");
		batch.add(p);
		
		p = new PartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 4, 10));
		p.setName("李四");
		batch.add(p);
		
		p = new PartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 5, 20));
		p.setName("前五");
		batch.add(p);
		
		p = new PartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 3, 8));
		p.setName("赵三");
		batch.add(p);
		
		//这个开关可以关闭分组插入
//		batch.setGroupForPartitionTable(false);
		db.batchInsert(batch);
	}
	
	/**
	 * 调用count方法，覆盖多张表的场合
	 * 当分为多张表的时候，采用多个count语句查询，然后将结果相加
	 * @throws SQLException
	 */
	@Test
	public void testCountAll() throws SQLException{
		System.out.println("======================testCountAll========================");
		db.count(QB.create(PartitionEntity.class));
	}
	
	/**
	 * 查询记录覆盖多张表的场合
	 * 直接用union All一次查询所有表得出结果
	 * @throws SQLException
	 */
	@Test
	public void testSelectAll() throws SQLException{
		System.out.println("======================testSelectAll========================");
		db.selectAll(PartitionEntity.class);
	}
	
	
	/**
	 * 分表扫描器测试。分表扫描器能自动检查数据库中的所有子表。
	 * @throws SQLException
	 */
	@Test
	public void testPartitionMetadata() throws SQLException{
		System.out.println("======================testPartitionMetadata========================");
		long start=System.nanoTime();
		PartitionResult[] result=	db.getSubTableNames(MetaHolder.getMeta(PartitionEntity.class));
		System.out.println("1."+(System.nanoTime()-start));
		LogUtil.show(result);
		assertTrue(result.length>0);
		//第二次查询分表元数据，根据缓存，在刷新期内不会再访问数据库。
		start=System.nanoTime();
		db.getSubTableNames(MetaHolder.getMeta(PartitionEntity.class));
		System.out.println("2."+(System.nanoTime()-start));
	}
	
	/**
	 * 分表环境下，对查询列指定别名的场景测试
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectUsingAlias() throws SQLException {
		ORMConfig.getInstance().setFilterAbsentTables(false);
		System.out.println("===================== testSelectUsingAlias =======================");
		testPartitionBatchInsert(); // prepare data

		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		Selects items = QB.selectFrom(q);
		items.column(PartitionEntity.Field.id).as("id");
		items.column(PartitionEntity.Field.name).as("name");
		items.column(PartitionEntity.Field.dateField).as("dateField");
		items.column(PartitionEntity.Field.longField).as("longField");
		items.column(PartitionEntity.Field.intField).as("intField");

		q.getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);
		List<PartitionEntity> list = db.select(q, null);
		for (PartitionEntity obj : list) {
			LogUtil.show(obj.toString());
			junit.framework.Assert.assertTrue(obj.getDateField() != null);
		}
		
		// 再测试下使用UnionQuery的场景
		UnionQuery<PartitionEntity> unionQuery = QB.unionAll(PartitionEntity.class, q);
		unionQuery.getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);
		List<PartitionEntity> list2 = db.select(unionQuery, null);
		for (PartitionEntity obj : list2) {
			LogUtil.show(obj.toString());
			junit.framework.Assert.assertTrue(obj.getDateField() != null);
		}
	}

	/**
	 * 分表环境下，分组查询时，对查询列指定别名的场景测试
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGroupUsingAlias() throws SQLException {
		System.out.println("===================== testGroupUsingAlias =======================");
		testPartitionBatchInsert(); // prepare data

		Query<PartitionEntity> q = QB.create(PartitionEntity.class);
		Selects items = QB.selectFrom(q);
		items.column(PartitionEntity.Field.longField).as("longField");
		items.column(PartitionEntity.Field.intField).as("intField");
		items.column(PartitionEntity.Field.longField).group().as("longField");
		items.column(PartitionEntity.Field.intField).count().as("intField");
		q.getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);
		db.select(q, null);
	}

	/**
	 * 分表环境下，分页查询时，对查询列指定别名的场景测试
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testPageUsingAlias() throws SQLException {
		System.out.println("===================== testPageUsingAlias =======================");
		testPartitionBatchInsert(); // prepare data

		PartitionEntity entity = new PartitionEntity();
		entity.getQuery().addCondition(QB.between(PartitionEntity.Field.dateField,
				DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1)));

		Selects selectItems1 = QB.selectFrom(entity.getQuery());
		selectItems1.column(PartitionEntity.Field.id).as("id");
		selectItems1.column(PartitionEntity.Field.dateField).as("dateField");
		selectItems1.column(PartitionEntity.Field.intField).as("intField");
		selectItems1.column(PartitionEntity.Field.longField).as("longField");
		entity.getQuery().getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);

		PagingIterator<PartitionEntity> page = db.pageSelect(entity, 4);

		List<PartitionEntity> list = page.next();
		for (PartitionEntity obj : list) {
			LogUtil.show(obj.toString());
			junit.framework.Assert.assertTrue(obj.getDateField() != null);
		}
	}

	/**
	 * 分表环境下，2表关联并分页查询时，对查询列指定别名的场景测试；
	 * 同时测试当查询结果需要映射到带有@Column的实体类的场景。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testUnionUsingAlias() throws SQLException {
		System.out.println("===================== testUnionUsingAlias =======================");
		testPartitionBatchInsert(); // prepare data for PartitionEntity

		// prepare data for NonPartitionEntity
		db.createTable(NonPartitionEntity.class);
		List<NonPartitionEntity> batch=new ArrayList<NonPartitionEntity>();
		NonPartitionEntity p = new NonPartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 3, 1));
		p.setName("张三");
		batch.add(p);

		p = new NonPartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 5, 1));
		p.setName("王五");
		batch.add(p);

		p = new NonPartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 4, 10));
		p.setName("李四");
		batch.add(p);

		p = new NonPartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 5, 20));
		p.setName("前五");
		batch.add(p);

		p = new NonPartitionEntity();
		p.setDateField(DateUtils.getDate(2012, 3, 8));
		p.setName("赵三");
		batch.add(p);

		// 这个开关可以关闭分组插入
		db.batchInsert(batch,false);

		// prepare data for NonPartitionEntity finished
		NonPartitionEntity entity1 = new NonPartitionEntity();
		entity1.getQuery().addCondition(NonPartitionEntity.Field.intField, 0);

		PartitionEntity entity = new PartitionEntity();
		entity.getQuery().addCondition(QB.between(PartitionEntity.Field.dateField,
				DateUtils.getDate(2012, 3, 1), DateUtils.getDate(2012, 6, 1)));

		Selects selectItems1 = QB.selectFrom(entity1.getQuery());
		selectItems1.column(NonPartitionEntity.Field.id).as("id");
		selectItems1.column(NonPartitionEntity.Field.name).as("name");
		selectItems1.column(NonPartitionEntity.Field.dateField).as("dateField");
		selectItems1.column(NonPartitionEntity.Field.intField).as("intField");
		selectItems1.column(NonPartitionEntity.Field.longField).as("longField");

		Selects selectItems2 = QB.selectFrom(entity.getQuery());
		selectItems2.column(PartitionEntity.Field.id).as("id");
		selectItems2.column(PartitionEntity.Field.name).as("name");
		selectItems2.column(PartitionEntity.Field.dateField).as("dateField");
		selectItems2.column(PartitionEntity.Field.intField).as("intField");
		selectItems2.column(PartitionEntity.Field.longField).as("longField");

		UnionQuery<NonPartitionEntity> unionQuery =
				QB.unionAll(NonPartitionEntity.class, entity1.getQuery(), entity.getQuery());
		unionQuery.addOrderBy(false, new FBIField("dateField"));
		unionQuery.getResultTransformer().setStrategy(PopulateStrategy.SKIP_COLUMN_ANNOTATION);
		PagingIterator<NonPartitionEntity> page =
				db.pageSelect(unionQuery, NonPartitionEntity.class, 4);
		
		List<NonPartitionEntity> list = page.next();
		for (NonPartitionEntity obj : list) {
			LogUtil.show(obj.toString());
			junit.framework.Assert.assertTrue(obj.getDateField() != null);
		}
		
		// 再测试下查询结果映射到不带有@Column的实体类的场景
		UnionQuery<PartitionEntityResult> unionQuery2 =
				QB.unionAll(PartitionEntityResult.class, entity1.getQuery(), entity.getQuery());
		PagingIterator<PartitionEntityResult> page2 =
				db.pageSelect(unionQuery2, PartitionEntityResult.class, 4);
		List<PartitionEntityResult> list2 = page2.next();
		for (PartitionEntityResult obj : list2) {
			LogUtil.show(obj.toString());
			junit.framework.Assert.assertTrue(obj.getDateField() != null);
		}
	}
	@Test
	@IgnoreOn({"oracle","postgresql"})
	public void jsonLoad() throws IOException {
		System.out.println("=====================================1");
		String s=IOUtils.asString(this.getClass().getResource("resource.txt"),"GBK");
		System.out.println(s);
		PartitionTableImpl table=JsonUtil.toObject(s,PartitionTableImpl.class);
		System.out.println(table.key());
		
		String s2=JsonUtil.toJson(table);
		System.out.println("=====================================");
		System.out.println(s2);
	}
}
