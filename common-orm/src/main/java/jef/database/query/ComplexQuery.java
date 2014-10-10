package jef.database.query;

import java.sql.SQLException;

import jef.database.SelectProcessor;
import jef.database.wrapper.clause.CountClause;

public interface ComplexQuery extends ConditionQuery{
	/**
	 * 转换为count语句（绑定变量）
	 * @param processor
	 * @param context
	 * @return
	 * @throws SQLException
	 */
	CountClause toPrepareCountSql(SelectProcessor processor,SqlContext context) throws SQLException ;
	
	/**
	 * 转换为count语句（无绑定）
	 * @param processor
	 * @return
	 * @throws SQLException
	 */
	CountClause toCountSql(SelectProcessor processor) throws SQLException ;
}
