package com.github.geequery.test.database.publicfield;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.Condition;
import jef.database.DbClient;
import jef.database.QB;

import org.junit.Test;

import com.github.geequery.test.database.boss.DeviceSrc;
import com.github.geequery.test.database.boss.PipeMappingSrc;

public class TestCase {
	DbClient db = new DbClient();

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
