/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.wrapper.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.persistence.PersistenceException;

import jef.database.DbUtils;
import jef.database.OperateTarget;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.populator.ColumnMeta;

public final class ResultSetHolder extends AbstractResultSet{
	private Statement st;
	ResultSet rs;
	OperateTarget db;

	public OperateTarget getDb() {
		return db;
	}
	public ResultSetHolder(OperateTarget tx,Statement st,ResultSet rs) {
		this.db=tx;
		this.st=st;
		this.rs=rs;
	}
	
	/**
	 * 
	 * @param closeResultSet 是否关闭ResultSet
	 */
	public void close(boolean closeResultSet) {
		if(closeResultSet && rs!=null){
			DbUtils.close(rs);
			rs=null;
		}
		if(st!=null){
			DbUtils.close(st);
			st=null;	
		}
		if(db!=null){
			db.releaseConnection();
			//而目前设计约束凡是用户持有游标的场景，必须嵌套到一个内部的事务中去。因此实际上不会出现非当前线程的方法来释放连接的可能。
			//如果是为了持有结果集专门设计的连接，那么直接就关闭掉			
			if(db.isResultSetHolderTransaction()){
				db.commitAndClose();
			}	
			db=null;
		}
	}
	@Override
	public DatabaseDialect getProfile() {
		return db.getProfile();
	}
	@Override
	public ColumnMeta getColumns() {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean next() {
		try {
			ensureOpen();
			return rs.next();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	private void ensureOpen() throws SQLException {
		if(rs==null){
			throw new SQLException("The resultset was closed!");
		}
	}
	@Override
	public void afterLast() throws SQLException {
		ensureOpen();
		rs.afterLast();
	}
	@Override
	public void beforeFirst() throws SQLException {
		ensureOpen();
		rs.beforeFirst();
	}
	@Override
	public void close() throws SQLException {
		close(true);
	}
	@Override
	public boolean first() throws SQLException {
		ensureOpen();
		return rs.first();
	}
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		ensureOpen();
		return rs.getMetaData();
	}
	@Override
	public boolean isClosed() throws SQLException {
		return rs.isClosed();
	}
	@Override
	public boolean previous() throws SQLException {
		ensureOpen();
		return rs.previous();
	}
	@Override
	protected ResultSet get() throws SQLException {
		ensureOpen();
		return rs;
	}
}
