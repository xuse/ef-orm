package jef.database.dialect;

import jef.database.ConnectInfo;
import jef.tools.string.JefStringReader;

/**
 * Dialect for SQL Server 2000 and before..
 * @author jiyi
 *
 */
public class SQLServer2000Dialect extends SQLServer2005Dialect {
	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		StringBuilder sb = new StringBuilder("jdbc:");
		// jdbc:microsoft:sqlserver:@localhost:1433; DatabaseName =allandb
		sb.append("microsoft:sqlserver:");
		sb.append("//").append(host).append(":").append(port <= 0 ? 1433 : port);
		sb.append("; DatabaseName=").append(pathOrName);
		String url = sb.toString();
		return url;
	}

	public String getDriverClass(String url) {
		return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
	}
	
	public void parseDbInfo(ConnectInfo connectInfo) {
		JefStringReader reader = new JefStringReader(connectInfo.getUrl());
		reader.setIgnoreChars(' ');
		reader.consumeIgnoreCase("jdbc:microsoft:sqlserver:");
		reader.consumeChars('@', '/');
		String host = reader.readToken(':', '/');
		connectInfo.setHost(host);
		if (reader.omitAfterKeyIgnoreCase("databasename=", ' ') != -1) {
			String dbname = reader.readToken(' ', ';', ':');
			connectInfo.setDbname(dbname);
		}
//		if (reader.omitAfterKeyIgnoreCase("user=", ' ') != -1) {
//			String user = reader.readToken(' ', ';', ':');
//			connectInfo.setUser(user);
//		}
//		if (reader.omitAfterKeyIgnoreCase("password=", ' ') != -1) {
//			String password = reader.readToken(' ', ';', ':');
//			connectInfo.setPassword(password);
//		}
	}
}
