package jef.database.meta;

import java.util.List;
import java.util.Map;

import jef.database.dialect.ColumnType;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.Index;


public interface DdlGenerator {
	// ///////////////////////////////////////////////////////////////
	/**
	 * 转为建表语句
	 * 生成语句包括键表语句和索引语句还有约束生成语句。
	 * 
	 * @param obj
	 * @param tablename
	 * @return
	 */
	TableCreateSQLs toTableCreateClause(ITableMetadata obj, String tablename);

	/**
	 * 生成Alter table 语句
	 * @return
	 */
	List<String> toTableModifyClause(ITableMetadata meta,String tableName, Map<String, ColumnType> insert, List<ColumnModification> changed, List<String> delete);
	
	/**
	 * 生成 create view语句
	 * @return
	 */
	List<String> toViewCreateClause();
	
	/**
	 * 生成删除约束的语句(包括外键)
	 * @return
	 */
	String getDropConstraintSql(String tableName,String contraintName);

	/**
	 * 生成删除索引的语句
	 * @param index
	 * @return
	 */
    String deleteIndex(Index index);
    
    /**
     * 生成删除约束的语句
     * @param con 约束对象
     * @return SQL语句
     */
    String deleteConstraint(Constraint con);
    
    /**
     * 生成创建约束的语句
     * @param con 约束对象
     * @return SQL语句
     */
    String addConstraint(Constraint con);
    
    /**
     * 生成修改主键的语句(先删后加一个语句)
     * @param conBefore 修改前约束对象
     * @param conAfter 修改后约束对象
     * @return SQL语句
     */
    String modifyPrimaryKey(Constraint conBefore, Constraint conAfter);
}
