package org.easyframe.tutorial.lesson7;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.ORMConfig;
import jef.database.support.RDBMS;

import org.easyframe.tutorial.lesson4.entity.Person;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CaseDialectExtend {
	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial").build();
		
		ORMConfig.getInstance().setDebugMode(false);
		db.createTable(Person.class);
		ORMConfig.getInstance().setDebugMode(true);
	}
	
	/**
	 * 本案例演示扩展方言的效用。本例中出现的ifnull和atan2函数都是内置的方言中没有用注册的函数。
	 * 通过自定义的方言覆盖内置方言，才能支持这些函数。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testExtendDialact() throws SQLException {
		if(db.getMetaData(null).getProfile().getName()==RDBMS.derby){
			String sql = "select atan2(12, 1) from t_person";
			System.out.println(db.createNativeQuery(sql).getResultList());

			sql = "select ifnull(gender, 'F') from t_person";
			System.out.println(db.createNativeQuery(sql).getResultList());	
		}
	}
	
	@AfterClass
	public static void close(){
		if(db!=null){
			db.close();
		}
	}
}
