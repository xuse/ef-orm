package jef.database.dialect.statement;

import java.sql.ResultSet;

import jef.common.wrapper.IntRange;

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
	String toPageSQL(String sql,IntRange offsetLimit);
	
	/**
	 * 对于SQL语句进行结果集限制
	 * @param sql
	 * @param offsetLimit
	 * @param isUnion
	 * @return
	 */
	String toPageSQL(String sql,IntRange offsetLimit,boolean isUnion);
	
	/**
	 * 对于结果集进行结果限定
	 * 在一部分数据库上，结果集限制必须后处理。这包括重新封装ResultSet对象等操作
	 * 
	 * @param rs
	 * @param offsetLimit
	 * @return
	 */
	ResultSet afterProcess(ResultSet rs,IntRange offsetLimit);
}
