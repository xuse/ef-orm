package com.github.geequery.test.spring.JefTransactionTest;

import com.github.geequery.test.common.TestUtils;

import jef.database.DbClient;
import jef.database.support.DbInitHandler;

public class InitTableCreater implements DbInitHandler{

	@Override
	public void doDatabaseInit(DbClient db) {
		db.createTable(TestUtils.URM_SERVICE);
		db.createTable(TestUtils.URM_GROUP);
	}

}
