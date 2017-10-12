package org.easyframe.tutorial.lessond;

import java.sql.SQLException;
import java.util.Collection;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbMetaData;
import jef.database.meta.Index;
import jef.database.meta.MetaHolder;
import jef.tools.collection.CollectionUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class MetadataCase {
	static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial").build();
	}

	@Test
	public void testDbMeta() throws SQLException {
		// db.dropTable(TableMaster.class);
		// db.createTable(TableMaster.class);
	}

	@Test
	public void testDbMeta3() throws SQLException {

	}

	@Test
	public void testDbMeta2() throws SQLException {

		db.dropTable(TableMaster.class);
		db.createTable(TableMaster.class);
		DbMetaData meta = db.getMetaData(null);
		// System.out.println(meta.getColumns("TableMaster"));
		// System.out.println(meta.getPrimaryKey("TableMaster"));
		// System.out.println(meta.getForeignKey("TableMaster"));
		// System.out.println(meta.getForeignKeyReferenceTo("TableMaster"));
		Collection<Index> index = meta.getUniqueConstraint(MetaHolder.getMeta(TableMaster.class));
		index = CollectionUtils.toList(index);
		LogUtil.show(index);
		System.out.println("-----------------------");
		LogUtil.show(meta.getIndexes(TableMaster.class));

		// for(Index i:index){
		// String
		// sql="ALTER TABLE TableMaster disable CONSTRAINT "+i.getIndexName();
		// try{
		// meta.executeSql(sql);
		// System.out.println(i.toString() + " 是约束");
		// sql="ALTER TABLE TableMaster enable CONSTRAINT "+i.getIndexName();
		// meta.executeSql(sql);
		// }catch(SQLException e){
		// System.out.println(e.getMessage());
		// // e.printStackTrace();
		// System.out.println(i.toString() + " 是纯索引");
		// }
		// if(i.getIndexName().startsWith("PRIMARY")||i.getIndexName().startsWith("PK_")){
		// continue;
		// }
		// meta.dropIndex(i);
	}

	//

	// meta.dropIndex("TableMaster","name");
	// meta.executeSql("alter table TableMaster drop DROP INDEX name");
	// ORACLE
	// meta.executeSql("alter table TableMaster drop UNIQUE(NAME)");

	// TABLEMASTER(ACCOUNT_ID,NAME)
	// meta.executeSql("alter table tablemaster drop UNIQUE(ACCOUNT_ID,NAME)");
	// meta.executeSql("drop index IDX_NAME_AID");
	//
}
