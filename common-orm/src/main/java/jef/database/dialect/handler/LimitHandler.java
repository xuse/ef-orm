package jef.database.dialect.handler;

import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;

/**
 * LimitHandler用于进行结果集限制。offset,limit的控制
 * 
 * @author jiyi
 *
 */
public interface LimitHandler {
	/**
	 * 对于SQL语句进行结果集限制
	 * 
	 * @param sql
	 * @param offsetLimit
	 * @return
	 */
	BindSql toPageSQL(String sql, PageLimit offsetLimit);

	/**
	 * 对于SQL语句进行结果集限制
	 * 
	 * @param sql
	 * @param offsetLimit
	 * @param isUnion
	 * @return
	 */
	BindSql toPageSQL(String sql, PageLimit offsetLimit, boolean isUnion);
}
