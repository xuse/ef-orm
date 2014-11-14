package jef.database.test.jdbc;

import java.sql.Statement;

import jef.database.jdbc.statement.DelegatingStatement;

public class DebugStatement extends DelegatingStatement{
	private DebugConnection conn;
	
	public DebugStatement(Statement st,DebugConnection conn) {
		super(st);
		this.conn=conn;
	}
	

}
