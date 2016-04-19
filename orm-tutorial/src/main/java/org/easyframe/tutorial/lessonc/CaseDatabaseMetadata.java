package org.easyframe.tutorial.lessonc;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbMetaData;
import jef.database.meta.Column;
import jef.database.meta.Function;
import jef.database.meta.Index;
import jef.database.meta.TableInfo;

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
		db = new DbClientBuilder().build();
		db.dropTable(Student.class,StudentToLesson.class,LessonInfo.class);
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
			System.out.println(c.getColumnName()+"\t"+c.getDataType() + "\t" + c.getColumnSize() + "\t"+ c.getRemarks());
		}
		//表的主键
		System.out.println("======= Table " + tableName + " Primary key ========");
		System.out.println(meta.getPrimaryKey(tableName));
		//表的索引
		Collection<Index> is = meta.getIndexes(tableName);
		System.out.println("======= Table " + tableName + " has " + is.size() + " indexes. ========");
		for (Index i : is) {
			System.out.println(i);
		}
	}
	
	/**
	 * 删除表并建表。
	 * 删除表时会主动删除相关的所有主外键、索引、约束。
	 * 建表时会同时创建表的索引。
	 * @throws SQLException
	 */
	@Test
	public void testTableDropCreate() throws SQLException{
		DbMetaData meta = db.getMetaData(null);
		//删除Student表
		meta.dropTable(Student.class);
		//创建Student表
		meta.createTable(Student.class);
	}
	
	/**
	 * 操作外键的演示
	 * @throws SQLException
	 */
	@Test
	public void testForeignKeys() throws SQLException{
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

	/**
	 * 演示如何操作索引
	 * @throws SQLException
	 */
	@Test
	public void testIndexes() throws SQLException{
		DbMetaData meta = db.getMetaData(null);
		System.out.println("=== Indexes on table Student ===");
		for(Index index: meta.getIndexes(Student.class)){
			System.out.println(index);
		}
		
		System.out.println("=== Now we try to create two indexes ===");
		//创建单字段的unique索引
		Index index1=meta.toIndexDescrption(Student.class, "name");
		index1.setUnique(true);
		meta.createIndex(index1);
		//创建复合索引
		Index index2=meta.toIndexDescrption(Student.class, "grade desc", "gender");
		meta.createIndex(index2);
		
		//打印出表上的所有索引
		System.out.println("=== Indexes on table Student (After create)===");
		for(Index index: meta.getIndexes(Student.class)){
			System.out.println(index);
		}
		
		System.out.println("=== Now we try to drop all indexes ===");
		//删除所有索引
		for(Index index: meta.getIndexes(Student.class)){
			if(index.getIndexName().startsWith("IDX")){
				meta.dropIndex(index);
			}
		}
		
		//打印出表上的所有索引
		System.out.println("=== Indexes on table Student (After drop)===");
		for(Index index: meta.getIndexes(Student.class)){
			System.out.println(index);
		}
	}
	
	/**
	 * 打印出数据库中的自定义函数和存储过程
	 * @throws SQLException
	 */
	@Test
	public void testFunctions() throws SQLException{
		DbMetaData meta = db.getMetaData(null);
		System.out.println("=== User Defined Functions ===");
		List<Function> functions=meta.getFunctions(null);
		for(Function f: functions){
			System.out.println(f);
		}
		System.out.println("=== User Defined Procedures ===");
		functions=meta.getProcedures(null);
		for(Function f: functions){
			System.out.println(f);
		}		
	}
	
	
	@AfterClass
	public static void close() {
		if (db != null) {
			db.close();
		}
	}
}
