package org.easyframe.tutorial.lessonc;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbMetaData;
import jef.database.meta.Column;
import jef.database.meta.ForeignKey;
import jef.database.meta.Index;
import jef.database.meta.TableInfo;
import jef.tools.StringUtils;

import org.easyframe.tutorial.lesson2.entity.LessonInfo;
import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson2.entity.StudentToLesson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 关于DbMetadata的使用，DbMetadata封装了大部分数据库结构的存取方法，也提供了建表删表等DDL操作。
 * 
 * @author jiyi
 * 
 */
public class CaseDatabaseMetadata {
	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException{
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db = new DbClient();
		db.createTable(Student.class,StudentToLesson.class,LessonInfo.class);
	}
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
		System.out.println("======= Table " + tableName + " has " + cs.size() + " columns. ========");
		for (Column c : cs) {
			System.out.println(StringUtils.rightPad(c.getColumnName(), 10) + "\t" + StringUtils.rightPad(c.getDataType(), 9) + "\t" + c.getColumnSize() + "\t" + (c.isNullAble() ? "null" : "not null") + "\t" + c.getRemarks());
		}
		System.out.println("======= Table " + tableName + " Primary key ========");
		System.out.println(meta.getPrimaryKey(tableName));

		Collection<Index> is = meta.getIndexes(tableName);
		System.out.println("======= Table " + tableName + " has " + is.size() + " indexes. ========");
		for (Index i : is) {
			System.out.println(StringUtils.rightPad(i.getIndexName(), 10) + "\t" + StringUtils.rightPad(StringUtils.join(i.getColumnName(), ','), 10) + "\t" + (i.isUnique() ? "Uniqie" : "NonUnique") + (i.isOrderAsc() ? " Asc" : " Desc") + "\t" + i.getType());
		}
	}
	
	/**
	 * 操作外键的演示
	 * @throws SQLException
	 */
	@Test
	public void testKeys() throws SQLException{
		String tableName="STUDENT_TO_LESSON";
		DbMetaData meta = db.getMetaData(null);
		
		//在表上面创建两个外键，分别引用Student表和LessonInfo表。
		meta.createForeignKey(StudentToLesson.Field.studentId, Student.Field.id);
		meta.createForeignKey(StudentToLesson.Field.lessionId, LessonInfo.Field.id);
		
		//打印目前表上的外键(应该看到两个外键)
		System.out.println("=== Foreign keys on table ["+tableName+"] ===");
		LogUtil.show(meta.getForeignKey(tableName));
		
		//打印引用到表Student的外键（应该看到一个外键）
		System.out.println("=== Foreign keys on table [STUDENT] ===");
		LogUtil.show(meta.getForeignKeyReferenceTo("STUDENT"));
		
		//打印引用到表LessonInfo的外键（应该看到一个外键）
		System.out.println("=== Foreign keys on table [LESSON_INFO] ===");
		LogUtil.show(meta.getForeignKeyReferenceTo("LESSON_INFO"));
		
		//删除表上的所有外键
		System.out.println("=== Drop Foreign keys ===");
		meta.dropAllForeignKey(tableName);
		
		//打印目前表上的外键(应该没有外键)
		System.out.println("=== Foreign keys on table "+tableName+" ===");
		LogUtil.show(meta.getForeignKey(tableName));
	}

	@AfterClass
	public static void close() {
		if (db != null) {
			db.close();
		}
	}
}
