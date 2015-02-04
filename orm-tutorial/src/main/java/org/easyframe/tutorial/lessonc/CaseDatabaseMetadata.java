package org.easyframe.tutorial.lessonc;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbMetaData;
import jef.database.meta.Column;
import jef.database.meta.Index;
import jef.database.meta.TableInfo;
import jef.tools.StringUtils;

import org.junit.AfterClass;
import org.junit.Test;

/**
 * 关于DbMetadata的使用，DbMetadata封装了大部分数据库结构的存取方法，也提供了建表删表等DDL操作。
 * 
 * @author jiyi
 * 
 */
public class CaseDatabaseMetadata {
	private static DbClient db = new DbClient();

	/**
	 * 使用DbMetadata来访问数据库基本信息
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testDbInfo() throws SQLException {
		// 当有多个数据源时，需要指定数据源的名称，如果只有一个数据源，那么传入null就行了。
		DbMetaData meta = db.getMetaData(null);
		Map<String, String> version = meta.getDbVersion();
		for (String key : version.keySet()) {
			System.out.println(key + ":" + version.get(key));
		}
		System.out.println("schema:" + meta.getCurrentSchema());
		System.out.println("=== Functions ===");
		System.out.println(meta.getAllBuildInFunctions());
		System.out.println("=== DATA TYPES ===");
		System.out.println(meta.getSupportDataType());
	}
	/**
	 * 使用DbMetadata来访问表结构等信息
	 * @throws SQLException
	 */
	@Test
	public void testTableInfo() throws SQLException {
		DbMetaData meta = db.getMetaData(null);
		//查看数据库中的所有表
		List<TableInfo> tables=meta.getTables();
		System.out.println("=== There are "+ tables.size()+" tables in database. ===");
		for(TableInfo info:tables){
			System.out.println(info);
		}
		
		//查看数据库中的所有视图
		List<TableInfo> views=meta.getViews();
		System.out.println("=== There are "+ views.size()+" views in database. ===");
		for(TableInfo info:views){
			System.out.println(info);
		}
		
		//查看一张表的信息
		if(tables.isEmpty())return;
		String tableName=tables.get(0).getName();
		List<Column> cs = meta.getColumns(tableName,true);
		LogUtil.show("======= Table " + tableName + " has " + cs.size() + " columns. ========");
		for (Column c : cs) {
			LogUtil.show(StringUtils.rightPad(c.getColumnName(), 10) + "\t" + StringUtils.rightPad(c.getDataType(), 9) + "\t" + c.getColumnSize() + "\t" + (c.isNullAble() ? "null" : "not null") + "\t" + c.getRemarks());
		}
		LogUtil.show("======= Table " + tableName + " Primary key ========");
		LogUtil.show(meta.getPrimaryKey(tableName));

		Collection<Index> is = meta.getIndexes(tableName);
		LogUtil.show("======= Table " + tableName + " has " + is.size() + " indexes. ========");
		for (Index i : is) {
			LogUtil.show(StringUtils.rightPad(i.getIndexName(), 10) + "\t" + StringUtils.rightPad(StringUtils.join(i.getColumnName(), ','), 10) + "\t" + (i.isUnique() ? "Uniqie" : "NonUnique") + (i.isOrderAsc() ? " Asc" : " Desc") + "\t" + i.getType());
		}
	}
	
	@Test
	public void testKeys() throws SQLException{
		DbMetaData meta = db.getMetaData(null);
		meta.getForeignKey("Foo");
	}

	@AfterClass
	public static void close() {
		if (db != null) {
			db.close();
		}
	}
}
