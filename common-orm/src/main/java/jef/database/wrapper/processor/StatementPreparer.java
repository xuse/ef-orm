package jef.database.wrapper.processor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface StatementPreparer {
	
	/**
	 * 对PreparedStatement进行准备
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	PreparedStatement doPrepareStatement(Connection conn,String sql) throws SQLException;
}
