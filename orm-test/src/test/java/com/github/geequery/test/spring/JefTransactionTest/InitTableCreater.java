package com.github.geequery.test.spring.JefTransactionTest;

import com.github.geequery.test.common.TestUtils;

import jef.database.DbClient;
import jef.database.support.DbInitHandler;

/**
 * DbInitHandler是一个可以自定义数据库初始化后执行代码的接口。
 * @author jiyi
 *
 */
public class InitTableCreater implements DbInitHandler{

	@Override
	public void doDatabaseInit(DbClient db) {
		db.createTable(TestUtils.URM_SERVICE);
		db.createTable(TestUtils.URM_GROUP);
	}

}
