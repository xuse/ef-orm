package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.jdbc.result.IResultSet;

/**
 * ResultSet访问者 用于描述某个字段的值从结果集中的获取办法
 * @author jiyi
 * 
 */
public interface ResultSetAccessor {
	Object getProperObject(IResultSet rs, int n) throws SQLException;

	boolean applyFor(int type);
}
