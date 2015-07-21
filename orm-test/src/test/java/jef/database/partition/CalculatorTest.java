package jef.database.partition;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbUtils;
import jef.database.annotation.PartitionKeyImpl;
import jef.database.annotation.PartitionTable;
import jef.database.annotation.PartitionTableImpl;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping.GenerationResolution;
import jef.database.innerpool.PartitionSupport;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.TableMetadata;
import jef.database.meta.TupleMetadata;
import jef.database.query.DefaultPartitionCalculator;
import jef.database.query.PartitionCalculator;
import jef.database.routing.PartitionResult;
import jef.database.routing.function.KeyFunction;
import jef.database.routing.sql.SqlAnalyzer;
import jef.database.support.MultipleDatabaseOperateException;
import jef.orm.onetable.model.TestEntity;
import jef.orm.partition.PartitionEntity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

public class CalculatorTest extends org.junit.Assert{
	static PartitionTableImpl config = new PartitionTableImpl();
	private PartitionCalculator calc = new DefaultPartitionCalculator();

	private TupleMetadata tuple = new TupleMetadata("TestPartitionTable");
	private TableMetadata meta = (TableMetadata) MetaHolder.getMeta(TestEntity.class);

	static Method inv;

	@BeforeClass
	public static void setup() throws SecurityException, NoSuchMethodException {
		new EntityEnhancer().enhance("jef.orm");
		config.setAppender("_");
		config.setKeySeparator("_");
		PartitionKeyImpl[] keys = new PartitionKeyImpl[] { new PartitionKeyImpl("id", 1) };
		// keys[0].length=2;
		config.setKey(keys);

		inv = TableMetadata.class.getDeclaredMethod("setPartition", PartitionTable.class);
		inv.setAccessible(true);
	}

	private PartitionSupport supportor = new PartitionSupport() {
		public Collection<String> getSubTableNames(String dbName, ITableMetadata pTable) throws SQLException {
			String name = pTable.getTableName(true);
			List<String> result = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				result.add(name + "0" + i);
			}
			return result;
		}

		public Collection<String> getDdcNames() {
			return Arrays.asList("Database1", "Database2", "Database3", "Database4", "Database5");
		}

		public DatabaseDialect getProfile(String dbkey) {
			return AbstractDialect.getProfile("oracle");
		}

		public void ensureTableExists(String db,String table,ITableMetadata meta) {
		}

		public PartitionResult[] getSubTableNames(ITableMetadata meta) {
			PartitionResult r=new PartitionResult(meta.getTableName(true)+"_MM");
			return new PartitionResult[]{r};
		}

		public boolean isExist(String dbName, String table, ITableMetadata meta) {
			return true;
		}
	};

	@Test
	public void testTableResults() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		DatabaseDialect profile = AbstractDialect.getProfile("mariadb");
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.RAW;
			((PartitionKeyImpl) config.key()[0]).defaultWhenFieldIsNull = "AA, BB, CC";
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,2);
			System.out.println(Arrays.asList(result));
			assertEquals("[TEST_ENTITY_AA,TEST_ENTITY_BB,TEST_ENTITY_CC,TEST_ENTITY]", Arrays.toString(result));
			assertEquals(GenerationResolution.TABLE, meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.MODULUS;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,1);
			System.out.println(Arrays.asList(result));
			assertEquals("[TEST_ENTITY_0,TEST_ENTITY_1,TEST_ENTITY_2,TEST_ENTITY_3,TEST_ENTITY_4,TEST_ENTITY_5,TEST_ENTITY_6,TEST_ENTITY_7,TEST_ENTITY_8,TEST_ENTITY_9]", Arrays.toString(result));
			assertEquals(GenerationResolution.TABLE, meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.MONTH;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,2);
			System.out.println(Arrays.asList(result));
			assertEquals("[TEST_ENTITY_1,TEST_ENTITY_2,TEST_ENTITY_3,TEST_ENTITY_4,TEST_ENTITY_5,TEST_ENTITY_6,TEST_ENTITY_7,TEST_ENTITY_8,TEST_ENTITY_9,TEST_ENTITY_10,TEST_ENTITY_11,TEST_ENTITY_12,TEST_ENTITY]", Arrays.toString(result));
			assertEquals(GenerationResolution.TABLE, meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.YEAR;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,2);
			System.out.println(Arrays.asList(result));
			assertEquals("[TEST_ENTITY_2015,TEST_ENTITY]",Arrays.toString(result));
			assertEquals(GenerationResolution.TABLE, meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			System.out.println("==============MAP函数=================");
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.MAPPING;
			((PartitionKeyImpl) config.key()[0]).funcParams=new String[]{"00-24:1,25-49:2,50-74:3,75-99:4"};
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,1);
			System.out.println(Arrays.asList(result));
			assertEquals("[TEST_ENTITY_1,TEST_ENTITY_2,TEST_ENTITY_3,TEST_ENTITY_4]", Arrays.toString(result));
			assertEquals(GenerationResolution.TABLE, meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
	
		{
			System.out.println("==============双维度下，一个维度无法枚举，因此收缩=================");
			PartitionKeyImpl[] keys = new PartitionKeyImpl[] { new PartitionKeyImpl("createTime", 1), new PartitionKeyImpl("id", 1) };
			keys[0].function = KeyFunction.MONTH;
			config.setKey(keys);
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,2);
			assertEquals("[TEST_ENTITY]", Arrays.toString(result));
			System.out.println(Arrays.asList(result));
		}
		
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.YEAR;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,3);
			Assert.notNull(result);
			System.out.println(Arrays.asList(result));
			assertEquals("[test_entity_MM]",Arrays.toString(result));
			assertEquals(GenerationResolution.TABLE, meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
	}

	@Test
	public void testCreaterTable() {
		PartitionEntity pe = new PartitionEntity();
		PartitionResult[] results = DbUtils.toTableNames(pe, null, null, supportor);
		assertEquals(5, results.length);
		for(PartitionResult r: results){
			assertEquals(12, r.tableSize());
			assertEquals(18, r.getTables().get(0).length());
		}
	}

	@Test(expected=MultipleDatabaseOperateException.class)
	public void testCreaterTable2() {
		PartitionEntity pe = new PartitionEntity();
		PartitionResult results = DbUtils.toTableName(pe, null, null, supportor);
		System.out.println(results);
	}
	
	@Test
	public void testCreaterTable3() {
		ITableMetadata meta=MetaHolder.getMeta(PartitionEntity.class);
		PartitionResult[] results = DbUtils.toTableNames(meta,  supportor,2);
		for (PartitionResult r : results) {
			System.out.println(r);
		}
	}
	@AfterClass
	public static void afterClz(){
		MetaHolder.getCachedDynamicModels().clear();
		MetaHolder.getCachedModels().clear();
	}

//	@Test
//	public void testSql1() throws ParseException{
//		MetaHolder.initMetadata(Device.class, null,null);
//		String sql="Delete from DEVICE where indexcode > '100000' AND indexcode < '500000'";
//		PartitionResult[] results=SqlAnalyzer.getPartitionResultOfSQL(DbUtils.parseStatement(sql), Collections.EMPTY_LIST, supportor);
//		System.out.println("------------------------------------------");
//		for (PartitionResult r : results) {
//			System.out.println(r.getDatabase()+"||"+r.getTables());
//		}
//	}
//	
//	@Test
//	public void testSql2() throws ParseException{
//		MetaHolder.initMetadata(Device.class, null,null);
//		String sql="update DEVICE xx set xx.name = 'ID:' || indexcode,createDate = current_timestamp where indexcode BETWEEN '1000' AND '6000'";
//		PartitionResult[] results=SqlAnalyzer.getPartitionResultOfSQL(DbUtils.parseStatement(sql), Collections.EMPTY_LIST, supportor);
//		System.out.println("------------------------------------------");
//		for (PartitionResult r : results) {
//			System.out.println(r.getDatabase()+"||"+r.getTables());
//		}
//	}
//	
//	@Test
//	public void testSql3() throws ParseException{
//		MetaHolder.initMetadata(Device.class, null,null);
//		String sql="delete Device where (indexcode >'200000' and indexcode<'5') or (indexcode >'700000' and indexcode <'8')";
//		PartitionResult[] results=SqlAnalyzer.getPartitionResultOfSQL(DbUtils.parseStatement(sql), Collections.EMPTY_LIST, supportor);
//		System.out.println("------------------------------------------");
//		for (PartitionResult r : results) {
//			System.out.println(r.getDatabase()+"||"+r.getTables());
//		}
////		{indexcode=(700000,8) || (200000,5)}
////		MapFunction maps=new MapFunction("1-1:datasource1,2-49999:datasource2,5-899999:datasource3,*:",1);
////		RangeDimension<String> r=RangeDimension.createCC("700000", "8");
////		 Collection<?> objs=r.toEnumationValue(Collections.<PartitionFunction>singletonList(maps));
////		for(Object o: objs){
////			System.out.println(maps.eval(String.valueOf(o)));
////		}
//	}
//
//	@Test
//	public void testSqlx() throws ParseException{
//		MetaHolder.initMetadata(Device.class, null,null);
//		String sql="delete Device where indexcode=?";
//		test(sql,"199999"); //1-1不能适应
//		test(sql,"2");      //
//		test(sql,"20");
//		test(sql,"20000");
//		test(sql,"2000001");
//		test(sql,"21");
//		test(sql,"201");
//		test(sql,"299999");
//		test(sql,"3");
//		test(sql,"300000");
//		
//	}

	private void test(String sql, Object... string) throws ParseException {
		StSqlParser parser=new StSqlParser(new StringReader(sql)); 
		PartitionResult[] results=SqlAnalyzer.getPartitionResultOfSQL(parser.Statement(), Arrays.<Object>asList(string), supportor);
		System.out.println("------------------------------------------");
		for (PartitionResult r : results) {
			System.out.println(r.getDatabase()+"||"+r.getTables());
		}
	}

}
