package org.easyframe.tutorial.lessond;

import java.sql.SQLException;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbMetaData;

import org.junit.BeforeClass;
import org.junit.Test;

public class MetadataCase {
	static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial").build();
	}
	
	@Test
	public void testDbMeta() throws SQLException{
//		db.dropTable(TableMaster.class);
//		db.createTable(TableMaster.class);
	}
	
	@Test
	public void testDbMeta3() throws SQLException{
		DbMetaData meta=db.getMetaData(null);
		System.out.println(meta.getSupportDataType());
	}
	@Test
	public void testDbMeta2() throws SQLException{
		
		db.dropTable(TableMaster.class);
		db.createTable(TableMaster.class);
		DbMetaData meta=db.getMetaData(null);
		System.out.println(meta.getColumns("TableMaster"));
		System.out.println(meta.getPrimaryKey("TableMaster"));
		System.out.println(meta.getForeignKey("TableMaster"));
		System.out.println(meta.getForeignKeyReferenceTo("TableMaster"));
		System.out.println(meta.getIndexes("TableMaster"));
		
		meta.dropIndex("TableMaster","name");
//		meta.executeSql("alter table TableMaster drop DROP INDEX name");
		//ORACLE
//		meta.executeSql("alter table TableMaster drop UNIQUE(NAME)");
		//
	}
	
	

}
