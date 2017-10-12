package jef.database.meta;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.text.StyledEditorKit.ItalicAction;

import jef.database.DbMetaData;
import jef.database.dialect.ColumnType;
import jef.database.meta.def.IndexDef;


public interface DdlGenerator {
	// ///////////////////////////////////////////////////////////////
	/**
	 * 转为建表语句
	 * 
	 * @param obj
	 * @param tablename
	 * @return
	 */
	TableCreateStatement toTableCreateClause(ITableMetadata obj, String tablename);

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
	 * 生成删除约束的语句
	 * @return
	 */
	String getDropConstraintSql(String tableName,String contraintName);

    String deleteIndex(Index index);

    /**
     * 
     * @param def
     * @param meta
     * @param tableName
     * @param tableSchema
     * @return
     */
    String addIndex(IndexDef def,ITableMetadata meta, String tableName);
}
