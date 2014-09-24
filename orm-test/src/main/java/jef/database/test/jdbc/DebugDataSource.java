package jef.database.test.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.datasource.AbstractDataSource;

public class DebugDataSource extends AbstractDataSource{
	private DataSource ds;

	@Override
	public Connection getConnection() throws SQLException {
		Connection conn=ds.getConnection();
		printStack("new DebugConnection() " + conn);
		return new DebugConnection(conn);
	}

	public static void printStack(String message) {
		StackTraceElement[] stacks=new Throwable().getStackTrace();
		System.out.println("==== "+message +" ====");
		System.out.println(stacks[2]+"\n"+stacks[3]);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Connection conn=ds.getConnection(username,password);
		printStack("new DebugConnection() " + conn);
		return new DebugConnection(conn);
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}

	public DataSource getDs() {
		return ds;
	}

	public void setDs(DataSource ds) {
		this.ds = ds;
	}
	
}
