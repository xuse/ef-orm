//package jef.orm.partition;
//
//import java.sql.SQLException;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import jef.codegen.EntityEnhancer;
//import jef.common.log.LogUtil;
//import jef.database.Condition.Operator;
//import jef.database.DbClient;
//import jef.database.DistributedDbContext;
//import jef.database.PagingIterator;
//import jef.database.QB;
//import jef.database.jpa.DbClientFactory;
//import jef.database.query.Query;
//import jef.http.client.support.CommentEntry;
//import jef.tools.DateUtils;
//import jef.tools.string.RandomData;
//
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
///**
// * 这个测试案例描述各种单表操作的分表计算办法 1、目前实现的： 使用
// * 
// * @author Administrator
// * 
// * 
// *         分表路由功能改进计划： 1、单表分表 (OK) 2、单表分表支持多库... 3、多表连接的分表 （多表连接情况下）
// *         4、多表连接的支持多库……
// * 
// * 
// *TODO: 目前采用的是一次SQL生成策略，因此无法支持不同数据库平台下的分表。
// *因为不同数据库下，生成的SQL语句的大小写、分页语法、自增主键语法都不一样。所以目前无法支持将一类分布式数据同时分布在多个数据库中。
// *
// *
// *  分库结果合并测试
// *  分库结果排序测试
// *  分库Page测试（排序、count）
// * 
// */
//public class PartitionSiteTest extends org.junit.Assert{
//
//	protected static DbClient db;
//	protected static DbClient db2;
//	protected static DbClient db3;
//
//	@BeforeClass
//	public static void init() throws SQLException {
//		try{
//			EntityEnhancer en = new EntityEnhancer();
//			en.enhance("jef.orm");
//			db = DbClientFactory.getDbClient();
//	
//			// DbClientFactory dbf1=new
//			// DbClientFactory(DbClientFactory.getDbClient("jdbc:oracle:thin:@ORA1201.HZ.ASIAINFO.COM:1521:ora1201",
//			// "XG", "XG"));
//			// dbf1.setName("db1-1");
//	
//			DbClientFactory dbf2 = new DbClientFactory(DbClientFactory.getDbClient("jdbc:derby:@db1-2;create=true", "AD", "AD"));
//			dbf2.setName("DB1-2");
//	
//			DbClientFactory dbf3 = new DbClientFactory(DbClientFactory.getDbClient("jdbc:derby:@db2-1;create=true", "CY", "CY"));
//			dbf3.setName("db1-3");
//	
//			DistributedDbContext context = new DistributedDbContext();
//			context.accept(db, "db1-1");
//			context.accept(dbf2);
//			context.accept(dbf3);
//			db2=dbf2.getDefault();
//			db3=dbf3.getDefault();
//			
//	
//			createTables(db);
//			createTables(dbf2.getDefault());
//			createTables(dbf3.getDefault());
//		} catch (Exception e) {
//			LogUtil.exception(e);
//		}
//	}
//
//	@AfterClass
//	public static void close() throws SQLException {
//		System.out.println("Closing database connections...");
//		db.close();
//	}
//
//	public static void createTables(DbClient db) throws SQLException {
//		db.dropTable(PartitionEntity.class);
//		db.createTable(PartitionEntity.class);
//
//		PartitionEntity p = new PartitionEntity();
//		p.setDateField(DateUtils.getDate(2012, 3, 1));
//		db.dropTable(p);
//		db.createTable(p);
//
//		p.setDateField(DateUtils.getDate(2012, 4, 1));
//		db.dropTable(p);
//		db.createTable(p);
//
//		p.setDateField(DateUtils.getDate(2012, 5, 1));
//		db.dropTable(p);
//		db.createTable(p);
//
//		p.setDateField(DateUtils.getDate(2012, 6, 1));
//		db.dropTable(p);
//		db.createTable(p);
//
//		p.setDateField(DateUtils.getDate(2012, 7, 1));
//		db.dropTable(p);
//		db.createTable(p);
//	}
//
//	/**
//	 * 测试分库后的数据库操作
//	 * @throws SQLException
//	 */
//	@Test
//	public void testInsert() throws SQLException {
//		System.out.println("================ testInsert ===================");
//		{
//			PartitionEntity p = new PartitionEntity();
//			p.getQuery().setAttribute("dbkey", "db1-1");
//			p.setIntField(RandomData.randomInteger(1, 5));
//			p.setLongField(RandomData.randomLong(1000, 1008));
//			p.setDateField(DateUtils.getDate(2012, 6, 1));
//			db.insert(p);
//		}
//		{
//			PartitionEntity p = new PartitionEntity();
//			p.getQuery().setAttribute("dbkey", "db1-1");
//			p.setIntField(RandomData.randomInteger(1, 5));
//			p.setLongField(RandomData.randomLong(1000, 1008));
//			p.setDateField(DateUtils.getDate(2012, 6, 1));
//			db.insert(p);
//		}
//		{
//			PartitionEntity p = new PartitionEntity();
//			p.getQuery().setAttribute("dbkey", "DB1-2");
//			p.setIntField(RandomData.randomInteger(1, 5));
//			p.setLongField(RandomData.randomLong(1000, 1008));
//			p.setDateField(DateUtils.getDate(2012, 6, 1));
//			db.insert(p);
//		}
//		{
//			PartitionEntity p = new PartitionEntity();
//			
//			p.getQuery().setAttribute("dbkey", "DB1-2");
//			p.setIntField(RandomData.randomInteger(1, 5));
//			p.setLongField(RandomData.randomLong(1000, 1008));
//			p.setDateField(DateUtils.getDate(2012, 7, 1));
//			db.insert(p);
//		}
//		{
//			PartitionEntity p = new PartitionEntity();
//			p.getQuery().setAttribute("dbkey", "DB1-2");
//			p.setIntField(RandomData.randomInteger(1, 5));
//			p.setLongField(RandomData.randomLong(1000, 1008));
//			p.setDateField(DateUtils.getDate(2012, 6, 1));
//			db.insert(p);
//		}
//		{
//			PartitionEntity p = new PartitionEntity();
//			p.getQuery().setAttribute("dbkey", "db1-3");
//			p.setIntField(RandomData.randomInteger(1, 5));
//			p.setLongField(RandomData.randomLong(1000, 1008));
//			p.setDateField(DateUtils.getDate(2012, 6, 1));
//			db.insert(p);
//		}
//	}
//
//	public void doCOunt(DbClient db) throws SQLException{
//		Map<String, String> result = new HashMap<String, String>();
//		for (CommentEntry table : db.getMetaData().getTables()) {
//			String sql = "select count(*) as count from " + table.getKey();
//			int n = db.countBySql(sql);
//			result.put(table.getKey(), String.valueOf(n));
//		}
//		LogUtil.show(result);
//		
//		
//	}
//	@Test
//	public void testSelectAll() throws Exception{
//		System.out.println("================ testSelectAll ===================");
//		Query<PartitionEntity> q=QB.create(PartitionEntity.class);
//		q.addOrderBy(false, PartitionEntity.Field.longField);
//		q.addOrderBy(true, PartitionEntity.Field.intField);
//		List<PartitionEntity> result=db.select(q,null);
//		LogUtil.show(result);
//	}
//	
//	
//	@Test
//	public void testPageAll() throws SQLException{
//		PartitionEntity p=new PartitionEntity();
//		p.getQuery().setAttribute("dbkey", "db1-1");
//		p.getQuery().addCondition(PartitionEntity.Field.intField,123);
//		p.getQuery().addCondition(PartitionEntity.Field.dateField, Operator.BETWEEN_L_L, new Date[]{new Date(),new Date()});
//		PagingIterator<PartitionEntity> ee=db.pageSelect(p, 10);
//		System.out.println(ee.getTotal());
//	}
//	
//	@Test
//	public void testDelete() throws Exception{
//		System.out.println("================ testDelete ===================");
//		db.delete(QB.create(PartitionEntity.class));//先删光
//		testInsert();//插入6条
//		int delete=db.delete(QB.create(PartitionEntity.class));
//		System.out.println("Total Delete:" +delete);
//		assertEquals(6,delete);
//	}
//	
//	@Test
//	public void testSelectPage() throws Exception{
//		Query<PartitionEntity> q=QB.create(PartitionEntity.class);
//		q.addOrderBy(false, PartitionEntity.Field.longField);
//		q.addOrderBy(true, PartitionEntity.Field.intField);
//		PagingIterator<PartitionEntity> pages=db.pageSelect(q, 3);
//		 
//		while(pages.hasNext()){
//			System.out.println("第"+pages.getCurrentPage()+"页");
//			LogUtil.show(pages.next());
//		}
//	}
//	@Test
//	public void testSelect2() throws Exception{
//		
//	}
//	@Test
//	public void testSelect3() throws Exception{
//		
//	}
//	@Test
//	public void testSelect4() throws Exception{
//		
//	}
//	@Test
//	public void testSelect5() throws Exception{
//		
//	}
//	@Test
//	public void testSelect6() throws Exception{
//		
//	}
//}
