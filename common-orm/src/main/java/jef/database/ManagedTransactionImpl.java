package jef.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.cache.CacheDummy;
import jef.database.innerpool.AbstractJDBCConnection;
import jef.database.innerpool.IConnection;

public class ManagedTransactionImpl extends Transaction{
	
	public ManagedTransactionImpl(DbClient parent,Connection connection) {
		super();
		this.parent = parent;
		rProcessor = parent.rProcessor;
		selectp = parent.selectp;
		p = parent.p;
		cache = CacheDummy.getInstance();
		insertp = parent.insertp;
		batchinsertp = parent.batchinsertp;
		this.conn=new Conn(connection);
	}
	static final class Conn extends AbstractJDBCConnection implements IConnection{
		private Connection conn;
		public Conn(Connection conn2) {
			this.conn=conn2;
		}

		@Override
		public void closePhysical() {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}

		@Override
		public void setKey(String key) {
		}

		@Override
		public void ensureOpen() throws SQLException {
		}

		@Override
		public void close(){
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return conn.toString();
		}
		
		
	}

	@Override
	IConnection getConnection() throws SQLException {
		return conn;
	}

	@Override
	public void close() {
	}

	@Override
	public void commit(boolean flag) {
	}

	@Override
	public void rollback(boolean flag) {
	}

	@Override
	public void setRollbackOnly(boolean b) {
	}

	@Override
	public boolean isRollbackOnly() {
		return false;
	}

	@Override
	public TransactionFlag getTransactionFlag() {
		return TransactionFlag.Managed;
	}

	@Override
	public void setReadonly(boolean flag) {
	}

	@Override
	public boolean isReadonly() {
		try {
			return conn.isReadOnly();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}


	@Override
	public int getIsolationLevel() {
		return ISOLATION_DEFAULT;
	}


	@Override
	public void setIsolationLevel(int isolationLevel) {
	}


	@Override
	public Transaction setAutoCommit(boolean autoCommit) {
		return this;
	}


	@Override
	public boolean isAutoCommit() {
		try {
			return conn.getAutoCommit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
}
