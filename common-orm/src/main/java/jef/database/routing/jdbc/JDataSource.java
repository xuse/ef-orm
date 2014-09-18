package jef.database.routing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.DbClient;
import jef.database.datasource.AbstractDataSource;
import jef.database.datasource.DataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.innerpool.JConnection;

public class JDataSource extends AbstractDataSource{
	private DbClient db;
	
	public JDataSource(){
	}
	
	/**
	 * 构造
	 * @param lookup
	 */
	public JDataSource(DataSourceLookup lookup){
		this.db=new DbClient(new RoutingDataSource(lookup),0);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return new JConnection(db);
	}
	
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}
	
	public DbClient getDbClient(){
		return db;
	}
}
