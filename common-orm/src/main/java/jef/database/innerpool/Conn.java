package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

final class Conn extends AbstractJDBCConnection implements IConnection {
	Conn(Connection conn) {
		super(conn);
	}

	@Override
	public void setKey(String key) {
	}

	@Override
	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
}
