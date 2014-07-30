package jef.database.meta;

import java.util.List;
import java.util.Map;

import jef.database.dialect.ColumnType;


public interface DdlGenerator {
	// ///////////////////////////////////////////////////////////////
	/**
	 * 转为建表语句
	 * 
	 * @param obj
	 * @param tablename
	 * @return
	 */
	String[] toTableCreateClause(ITableMetadata obj, String tablename);

	/**
	 * 转为索引语句
	 * 
	 * @param obj
	 * @param tablename
	 * @return
	 */
	List<String> toIndexClause(ITableMetadata obj, String tablename);
	
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

	
}
