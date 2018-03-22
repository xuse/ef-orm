package com.github.geequery.test.database.complexjoin;

import java.sql.SQLException;
import java.util.List;

import javax.persistence.PersistenceException;

import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.QB;
import jef.database.query.Join;
import jef.database.query.Query;

import org.junit.Test;

public class Case {
	DbClient db = new DbClient();

	@Test(expected=PersistenceException.class)
	public void test1() throws SQLException {
//		db.createTable(OrgAreaRelation.class);
//		db.createTable(OrgResourceRelation.class);
//		db.createTable(Org.class);
//		db.batchInsert(Arrays.asList(RandomData.newArrayInstance(OrgAreaRelation.class, 10)));
//		db.batchInsert(Arrays.asList(RandomData.newArrayInstance(OrgResourceRelation.class, 10)));
//		db.batchInsert(Arrays.asList(RandomData.newArrayInstance(Org.class, 10)));
		getByOrgByAreaAndResource("2","2",OrgType.a);
	}

	public List<?> getByOrgByAreaAndResource(String areaId, String resId, OrgType orgType) {
		Query<Org> orgQuery = QB.create(Org.class);
		Query<OrgAreaRelation> areaQuery = QB.create(OrgAreaRelation.class);
		Query<OrgResourceRelation> resQuery = QB.create(OrgResourceRelation.class);
		areaQuery.addCondition(OrgAreaRelation.Field.areaId, areaId);
		resQuery.addCondition(OrgResourceRelation.Field.resourceId, resId);
		orgQuery.addCondition(Org.Field.dataValid, true);
		if (orgType != null) {
			orgQuery.addCondition(Org.Field.type, orgType);
		}
		Join join = QB.innerJoin(orgQuery, areaQuery, QB.on(Org.Field.id, OrgAreaRelation.Field.orgId)).innerJoin(resQuery,
				QB.on(OrgResourceRelation.Field.orgId, OrgAreaRelation.Field.orgId));
		List<?> orgs = null;
		try {
//			orgs = db.selectAs(join, Object[].class);
			orgs = db.selectAs(join, Org.class);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
		return orgs;
	}
}
