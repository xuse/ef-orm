package jef.database.routing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import jef.database.DbClient;
import jef.database.datasource.DataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.innerpool.JConnection;

public class JDataSource extends RoutingDataSource{
	private DbClient db;
	
	public JDataSource(){
	}
	
	/**
	 * 构造
	 * @param lookup
	 */
	public JDataSource(DataSourceLookup lookup){
		this.dataSourceLookup=lookup;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return new JConnection(db);
	}
	
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}
}
