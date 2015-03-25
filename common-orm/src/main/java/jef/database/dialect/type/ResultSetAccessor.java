package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.jdbc.result.IResultSet;

/**
 * ResultSet访问者 用于描述某个字段的值从结果集中的获取办法
 * @author jiyi
 * 
 */
public interface ResultSetAccessor {
	
	/**
	 * 从ResultSet中得到合适的值
	 * @param rs ResultSet 
	 * @param n 序号
	 * @return 合适的值。可能取到null
	 * @throws SQLException
	 */
	Object jdbcGet(IResultSet rs, int n) throws SQLException;

	/**
	 * 用于检查当前的结果集访问器获得的数据类型是否能适应指定的Type 
	 * @param type value of java.sql.Types 
	 * @return
	 * @see java.sql.Types
	 */
	boolean applyFor(int type);
}
