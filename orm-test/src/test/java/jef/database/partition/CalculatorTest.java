package jef.database.partition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jef.database.DbUtils;
import jef.database.KeyFunction;
import jef.database.annotation.PartitionKeyImpl;
import jef.database.annotation.PartitionResult;
import jef.database.annotation.PartitionTable;
import jef.database.annotation.PartitionTableImpl;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.DbmsProfile;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.TableMetadata;
import jef.database.meta.TupleMetadata;
import jef.database.query.DefaultPartitionCalculator;
import jef.database.query.PartitionCalculator;
import jef.orm.onetable.model.TestEntity;
import jef.orm.partition.PartitionEntity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CalculatorTest {
	static PartitionTableImpl config = new PartitionTableImpl();
	private PartitionCalculator calc = new DefaultPartitionCalculator();

	private TupleMetadata tuple = new TupleMetadata("TestPartitionTable");
	private TableMetadata meta = (TableMetadata) MetaHolder.getMeta(TestEntity.class);

	static Method inv;

	@BeforeClass
	public static void setup() throws SecurityException, NoSuchMethodException {
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

		public boolean isSingleSite() {
			return false;
		}

		public DatabaseDialect getProfile(String dbkey) {
			return DbmsProfile.getProfile("oracle");
		}
	};

	@Test
	public void testTableResults() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		DatabaseDialect profile = DbmsProfile.getProfile("mariadb");
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.RAW;
			((PartitionKeyImpl) config.key()[0]).defaultWhenFieldIsNull = "AA, BB, CC";
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,true);
			System.out.println(Arrays.asList(result));
			System.out.println(meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.MODULUS;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,true);
			System.out.println(Arrays.asList(result));
			System.out.println(meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.MONTH;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,true);
			System.out.println(Arrays.asList(result));
			System.out.println(meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			((PartitionKeyImpl) config.key()[0]).function = KeyFunction.YEAR;
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,true);
			System.out.println(Arrays.asList(result));
			System.out.println(meta.getFirstAutoincrementDef().getGenerationType(profile));
		}
		{
			System.out.println("==============双维度下，一个维度无法枚举，因此收缩=================");
			PartitionKeyImpl[] keys = new PartitionKeyImpl[] { new PartitionKeyImpl("createTime", 1), new PartitionKeyImpl("id", 1) };
			keys[0].function = KeyFunction.MONTH;
			config.setKey(keys);
			inv.invoke(meta, config);
			PartitionResult[] result = calc.toTableNames(meta, supportor,true);
			System.out.println(Arrays.asList(result));
		}
	}

	@Test
	public void testCreaterTable() {
		PartitionEntity pe = new PartitionEntity();
		PartitionResult[] results = DbUtils.toTableNames(pe, null, null, supportor);
		for (PartitionResult r : results) {
			System.out.println(r);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreaterTable2() {
		PartitionEntity pe = new PartitionEntity();
		PartitionResult results = DbUtils.toTableName(pe, null, null, supportor);
		System.out.println(results);
	}
	
	@Test
	public void testCreaterTable3() {
		ITableMetadata meta=MetaHolder.getMeta(PartitionEntity.class);
		PartitionResult[] results = DbUtils.toTableNames(meta,  supportor,true);
		for (PartitionResult r : results) {
			System.out.println(r);
		}
	}
	@AfterClass
	public static void afterClz(){
		MetaHolder.getCachedDynamicModels().clear();
		MetaHolder.getCachedModels().clear();
	}


}
