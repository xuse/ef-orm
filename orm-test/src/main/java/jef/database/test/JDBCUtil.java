package jef.database.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.AbstractDialect;


public class JDBCUtil {
	public static boolean existTable(Connection conn,String table) throws SQLException{
		DatabaseMetaData meta=conn.getMetaData();
		String name=meta.getDatabaseProductName();
		DatabaseDialect profile=AbstractDialect.getProfile(name);
		int n = table.indexOf('.');
		String schema=null;
		if (n > -1) {
			schema = table.substring(0, n);
			table = table.substring(n + 1);
		}
		
		schema=profile.getObjectNameToUse(schema);
		table = profile.getObjectNameToUse(table);
		
		ResultSet rs = meta.getTables(profile.getCatlog(schema), profile.getSchema(schema), table, new String[]{"TABLE"});
		try {
			boolean flag = rs.next();
			return flag;
		} finally {
			DbUtils.close(rs);
		}
	}
	
	public static int execute(Connection conn,String string) throws SQLException {
		PreparedStatement st=conn.prepareStatement(string);
		try{
			return st.executeUpdate();
		}finally{
			st.close();
		}
	}
}
