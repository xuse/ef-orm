package jef.database.dialect.statement;

import jef.database.wrapper.clause.BindSql;

/**
 * LimitHandler用于进行结果集限制。offset,limit的控制
 * LimitHandler
 * 1 首先是通过改写SQL语句实现，
 * 2 其次是通过对结果集ResultSet进行处理来实现。
 * 复杂一些的情况是两者结合。
 * 比如SQLServer
 * 
 * 
 * 
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
