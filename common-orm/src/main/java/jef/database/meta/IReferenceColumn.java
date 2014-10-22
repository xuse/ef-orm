package jef.database.meta;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.query.SqlContext;

public interface IReferenceColumn extends ISelectProvider{
	// //////////////////////////////单列模式
	/**
	 * 得到选择语句中的列名(表达式甚至case when子句，甚至子查询)(仅单列模式下，全对象模式返回null)
	 * 
	 * @param profile
	 * @param tableAlias
	 * @return
	 */
	String getSelectItem(DatabaseDialect profile, String tableAlias,SqlContext context);

	/**
	 * 得到该列的转义后的别名(仅单列模式下，全对象模式返回null)
	 * 
	 * @param tableAlias
	 * @param profile
	 * @param isSelecting  当生成SQL时候为true,当拼结果的时候为false
	 * @return
	 */
	String getSelectedAlias(String tableAlias,DatabaseDialect profile);//
	
	/**
	 * 获得列别名，当访问结果集时使用。要求返回大写字符
	 * @return
	 */
	String getResultAlias(String tableAlias,DatabaseDialect profile);
	
	/**
	 * 目标列的meta
	 * @return
	 */
	ColumnMapping<?> getTargetColumnType();
}
