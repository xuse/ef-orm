package org.easyframe.tutorial.lessona;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.DateUtils;

import org.easyframe.tutorial.lessona.entity.Customer;
import org.easyframe.tutorial.lessona.entity.Device;
import org.easyframe.tutorial.lessona.entity.OperateLog;
import org.junit.BeforeClass;
import org.junit.Test;

public class Case1 extends org.junit.Assert {
	
	static DbClient db;
	
	@BeforeClass
	public static void setup() throws SQLException{
		new EntityEnhancer().enhance("org.easyframe.tutorial.lessona");
		long start=System.currentTimeMillis();
		db=new DbClient();
		System.out.println(System.currentTimeMillis()-start);
		db.createTable(OperateLog.class);
	}
	
	//问题1，一次性创建了3年的表，太多。 如果是按天分表岂不是一次性创建几百张表啊？(测试)
	//问题2，不能按需建表(已经支持！)   (Pass)
	//支持特性2，分表后，不能采用自增主键，Derby又不支持Sequence，因此会自动切换为Table模式自增。 (Pass)
	@Test
	public void createTest() throws SQLException{
		OperateLog log=new OperateLog();
		log.setCreated(DateUtils.getDate(2010, 3, 2));
		log.setMessage("测试！！");
		db.insert(log);
		
	}
	
	
	@Test
	public void testPartition(){
		ITableMetadata meta=MetaHolder.getMeta(Customer.class);
		LogUtil.show(
		DbUtils.toTableNames(meta, DebugUtil.getPartitionSupport(db), 2)
				);
		System.out.println("================");
		meta=MetaHolder.getMeta(OperateLog.class);
		LogUtil.show(
				DbUtils.toTableNames(meta, DebugUtil.getPartitionSupport(db),2)
		);
		System.out.println("================");
		meta=MetaHolder.getMeta(Device.class);
		LogUtil.show(
				DbUtils.toTableNames(meta, DebugUtil.getPartitionSupport(db),2)
		);
		
	}

}
