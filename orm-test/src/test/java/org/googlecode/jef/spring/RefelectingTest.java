package org.googlecode.jef.spring;

import java.sql.Connection;
import java.sql.SQLException;

import jef.database.jdbc.statement.DelegatingStatement;
import jef.database.test.generator.TestObject;

import org.junit.Test;

public class RefelectingTest {

	@Test
	public void testObjects() {
		try {
			doMethod1();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void doMethod1() throws SQLException {
		Connection conn = Mocks.getMockConnection();
		{
			jef.database.jdbc.statement.DelegatingStatement st = new DelegatingStatement(conn.createStatement());
			TestObject.invokeObject(st);
		}
		{
			jef.database.jdbc.statement.DelegatingPreparedStatement st = new jef.database.jdbc.statement.DelegatingPreparedStatement(conn.prepareStatement(""));
			TestObject.invokeObject(st);
		}

	}

}
