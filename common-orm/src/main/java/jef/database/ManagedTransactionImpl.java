package jef.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import org.easyframe.enterprise.spring.TransactionMode;

import jef.database.cache.CacheDummy;
import jef.database.innerpool.AbstractJDBCConnection;
import jef.database.innerpool.IConnection;

public class ManagedTransactionImpl extends Transaction{
	
	public ManagedTransactionImpl(DbClient parent,Connection connection) {
		super();
		this.parent = parent;
		this.preProcessor=parent.preProcessor;
		this.selectp = parent.selectp;
		this.insertp = parent.insertp;
		this.updatep=parent.updatep;
		this.deletep=parent.deletep;
		
		cache = CacheDummy.getInstance();
		this.conn=new Conn(connection);
	}
	static final class Conn extends AbstractJDBCConnection implements IConnection{
		Conn(Connection conn) {
			this.conn=conn;
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
		
		public boolean isClosed() throws SQLException{
			return conn==null || conn.isClosed();
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

	public boolean isOpen() {
		try {
			return !conn.isClosed();
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
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

	@Override
	protected TransactionMode getTxType() {
		return parent.getTxType();
	}
	@Override
	protected boolean isJpaTx() {
		return false;
	}
}
