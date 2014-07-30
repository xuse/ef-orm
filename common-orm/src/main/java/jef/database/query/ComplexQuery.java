package jef.database.query;

import java.sql.SQLException;

import jef.database.SelectProcessor;
import jef.database.wrapper.BindSql;
import jef.database.wrapper.CountSqlResult;

public interface ComplexQuery extends ConditionQuery{
	/**
	 * 转换为查询语句无绑定
	 * @param processor
	 * @return
	 */
	String toQuerySql(SelectProcessor processor);
	
	/**
	 * 转换为查询语句(绑定)
	 * @param processor
	 * @param context
	 * @return
	 */
	BindSql toPrepareQuerySql(SelectProcessor processor, SqlContext context);

	/**
	 * 转换为count语句（无绑定）
	 * @param processor
	 * @return
	 * @throws SQLException
	 */
	CountSqlResult toCountSql(SelectProcessor processor) throws SQLException ;
	
	/**
	 * 转换为count语句（绑定变量）
	 * @param processor
	 * @param context
	 * @return
	 * @throws SQLException
	 */
	CountSqlResult toPrepareCountSql(SelectProcessor processor,SqlContext context) throws SQLException ;
	
	/**
	 * 准备上下文
	 * @return
	 */
	SqlContext prepare();

	
}
