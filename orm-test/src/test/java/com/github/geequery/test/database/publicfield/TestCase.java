package com.github.geequery.test.database.publicfield;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import com.github.geequery.test.database.boss.DeviceSrc;
import com.github.geequery.test.database.boss.PipeMappingSrc;

import jef.codegen.EntityEnhancer;
import jef.database.Condition;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.QB;
import jef.database.query.Query;
import jef.tools.string.RandomData;

public class TestCase {
	DbClient db = new DbClientBuilder().setPackagesToScan(new String[] {"com.github.geequery.test.database.boss"}).build();

	@Test
	public void mainTest() throws SQLException {
		int upCascadeId = 999;

		Condition upCasIdCon = UnitShareInfo.Field.upCascadeId.eq(upCascadeId);
		Condition downCasIdCon = UnitShareInfo.Field.downCascadeId.eq(0);
		UnitShareInfo unitShareInfo = new UnitShareInfo(); // 查询条件
		unitShareInfo.getQuery().addCondition(upCasIdCon);
		unitShareInfo.getQuery().addCondition(downCasIdCon);

		LocalUnitInfo unit = new LocalUnitInfo();
		unit.getQuery().addCondition(LocalUnitInfo.Field.parentId.eq(100L));
		unit.getQuery().addCondition(LocalUnitInfo.Field.unitType.eq(1));
		unit.getQuery().addCondition(LocalUnitInfo.Field.controlUnitId.eq(0));
		unit.getQuery().addCondition(QB.notExists(unitShareInfo.getQuery(), QB.on(UnitShareInfo.Field.indexCode, LocalUnitInfo.Field.indexCode)));
		
		db.select(unit);

	}
	
	@Test
	public void testM() throws SQLException {
		DeviceSrc s=RandomData.newInstance(DeviceSrc.class);
		db.insert(s);
		Query<DeviceSrc> deviceExample = QB.create(DeviceSrc.class);
		deviceExample.addCondition(DeviceSrc.Field.manufacturer.in(new String[] {"DS/1"}));
		deviceExample.addCondition(DeviceSrc.Field.protocol.in(new String[] {"HTTP"}));
		// 名称模糊（忽略大小写）
		deviceExample.addCondition(QB.sqlExpression("t.c_name ilike ?5"));
		deviceExample.setAttribute("5", "%sssss%");
		List<DeviceSrc> page = db.pageSelect(deviceExample, DeviceSrc.class,10).next();
		System.out.println(page);
	}

	@Test
	public void testSelectExists() throws SQLException {
		new EntityEnhancer().enhance("com.github.geequery.test.database.boss");
		db.createTable(DeviceSrc.class);
		db.createTable(PipeMappingSrc.class);
		// select count(*) from device_src t where exists(select 1 from
		// pipe_mapping_src et where et.c_pipe_code=? and
		// et.c_device_code=t.c_index_code)
		DeviceSrc deviceExample = new DeviceSrc();
		deviceExample.getQuery().addCondition(QB.exists(QB.create(PipeMappingSrc.class).addCondition(PipeMappingSrc.Field.pipeCode.eq(1000)), QB.on(PipeMappingSrc.Field.deviceCode, DeviceSrc.Field.indexCode)));

		db.select(deviceExample);

	}
}
