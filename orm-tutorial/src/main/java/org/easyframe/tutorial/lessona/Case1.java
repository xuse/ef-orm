package org.easyframe.tutorial.lessona;

import java.sql.SQLException;
import java.util.Date;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DataObject;
import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.ORMConfig;
import jef.database.annotation.PartitionResult;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.DateUtils;

import org.easyframe.tutorial.lessona.entity.Customer;
import org.easyframe.tutorial.lessona.entity.Device;
import org.easyframe.tutorial.lessona.entity.OperateLog;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 分库分表相关演示案例。
 * @author jiyi
 *
 */
public class Case1 extends org.junit.Assert {

	static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial.lessona");
		db = new DbClient();
		db.dropTable("OPERATELOG_20100302");
	}

	/**
	 * 测试在分库后的表中插入一条记录
	 * @throws SQLException
	 */
	@Test
	public void createTest() throws SQLException {
		OperateLog log = new OperateLog();
		log.setCreated(DateUtils.getDate(2010, 3, 2));
		log.setMessage("测试！！");
		db.insert(log);
	}

	/**
	 * 分表结果计算器的计算测试
	 */
	@Test
	public void testPartition() {
		ITableMetadata meta = MetaHolder.getMeta(Customer.class);
		PartitionResult[] result=DbUtils.toTableNames(meta, DebugUtil.getPartitionSupport(db), 1);
		assertEquals(3,result.length);
		assertEquals(3,result[0].tableSize());
		assertEquals(3,result[1].tableSize());
		assertEquals(3,result[2].tableSize());
		
		System.out.println("================");
		meta = MetaHolder.getMeta(OperateLog.class);
		result=DbUtils.toTableNames(meta, DebugUtil.getPartitionSupport(db), 2);
		assertEquals(1,result.length);
		assertEquals(4,result[0].tableSize());
		
		
		System.out.println("================");
		meta = MetaHolder.getMeta(Device.class);
		result=DbUtils.toTableNames(meta, DebugUtil.getPartitionSupport(db), 2);
		assertEquals(1,result.length);
		assertEquals(1,result[0].tableSize());
	}

	@Test
	public void testPartition2() {
		PartitionSupport s = DebugUtil.getPartitionSupport(db);
		ORMConfig.getInstance().setFilterAbsentTables(false);
		Customer c = new Customer();
		System.out.println("列出CUSTOMER表所有可能的表");
		LogUtil.show(toTableName(c, s));
		System.out.println("================");
		System.out.println("当customNo=11时。根据分库规则，11 mod 3=2。因此将只落于datasource2上。");
		c.setCustomerNo(11);
		LogUtil.show(toTableName(c, s));
		System.out.println("================");
		System.out.println("当customNo=11时。根据分库规则，11 mod 3=2。因此将只落于datasource2上。createDate=今天，故可以定位到具体的一张表上");
		c.setCreateDate(new Date());
		LogUtil.show(toTableName(c, s));
	}

	@Test
	public void testPartition3() {
		PartitionSupport s = DebugUtil.getPartitionSupport(db);
		
		LogUtil.show(DbUtils.toTableNames(MetaHolder.getMeta(Device.class), s, 2));
		
		ORMConfig.getInstance().setFilterAbsentTables(false);
		Device c = new Device();
		// LogUtil.show(
		// DbUtils.toTableNames(c, null, null, s)
		// );
		System.out.println("================");
		c.setIndexcode("130001");
		LogUtil.show(toTableName(c,s));
		System.out.println("================");
		c.setIndexcode("160011");
		c.setCreateDate(new Date());
		LogUtil.show(toTableName(c, s));
		System.out.println("================");
		c.setIndexcode("835592");
		c.setCreateDate(new Date());
		LogUtil.show(toTableName(c, s));
		
		System.out.println("================");
		LogUtil.show(toTableName(new Device(), s));
	}

	private PartitionResult[] toTableName(DataObject c, PartitionSupport s) {
		long nano=System.nanoTime();
		PartitionResult[] xx= DbUtils.toTableNames(c, null, null, s);
		System.out.println((System.nanoTime()-nano)/1000+"us");
		return xx;
	}

}
