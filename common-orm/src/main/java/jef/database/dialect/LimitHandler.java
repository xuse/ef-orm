package jef.database.dialect;

import jef.database.wrapper.clause.BindSql;

/**
 * LimitHandler用于进行结果集限制。offset,limit的控制
 * @author jiyi
 *
 */
public interface LimitHandler {
	/**
	 * 对于SQL语句进行结果集限制
	 * @param sql
	 * @param offsetLimit
	 * @return
	 */
	BindSql toPageSQL(String sql,int[] offsetLimit);
	
	/**
	 * 对于SQL语句进行结果集限制
	 * @param sql
	 * @param offsetLimit
	 * @param isUnion
	 * @return
	 */
	BindSql toPageSQL(String sql,int[] offsetLimit,boolean isUnion);
}
