package com.github.geequery.test.database.publicfield;

import java.sql.SQLException;

import jef.database.Condition;
import jef.database.DbClient;
import jef.database.QB;

import org.junit.Test;

public class TestCase {
	DbClient db=new DbClient();
	@Test
	public void mainTest() throws SQLException {
		int upCascadeId =999;

		Condition upCasIdCon = QB.eq(UnitShareInfo.Field.upCascadeId, upCascadeId);
		Condition downCasIdCon = QB.eq(UnitShareInfo.Field.downCascadeId, 0);
		UnitShareInfo unitShareInfo = new UnitShareInfo(); // 查询条件
		unitShareInfo.getQuery().addCondition(upCasIdCon);
		unitShareInfo.getQuery().addCondition(downCasIdCon);

		LocalUnitInfo unit = new LocalUnitInfo();
		unit.getQuery().addCondition(QB.eq(LocalUnitInfo.Field.parentId, 100L));
		unit.getQuery().addCondition(QB.eq(LocalUnitInfo.Field.unitType, 1));
		unit.getQuery().addCondition(QB.eq(LocalUnitInfo.Field.controlUnitId, 0));
		unit.getQuery().addCondition(QB.notExists(unitShareInfo.getQuery(), QB.on(UnitShareInfo.Field.indexCode, LocalUnitInfo.Field.indexCode)));
		db.select(unit);
		
	}
}
