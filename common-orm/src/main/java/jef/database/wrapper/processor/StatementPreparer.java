package jef.database.wrapper.processor;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.OperateTarget;

public interface StatementPreparer {
	
	/**
	 * 对PreparedStatement进行准备
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	PreparedStatement doPrepareStatement(OperateTarget conn,String sql) throws SQLException;
}
