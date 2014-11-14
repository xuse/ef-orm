package jef.database.test.jdbc;

import java.sql.PreparedStatement;

import jef.database.jdbc.statement.DelegatingPreparedStatement;

public class DebugPreparedStatement extends DelegatingPreparedStatement{
	private DebugConnection conn;
	
	public DebugPreparedStatement(PreparedStatement s,DebugConnection conn) {
		super(s);
		this.conn=conn;
	}

}
