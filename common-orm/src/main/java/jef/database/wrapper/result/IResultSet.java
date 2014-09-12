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

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.populator.ColumnMeta;

/**
 * 这个类是JDBC ResultSet的封装，大部分方法都和JDBC ResultSet一致
 * 
 * 下一步重构，将其继承ResultSet，进一步简化代码
 * 
 * @author jiyi
 *
 */
public interface IResultSet {
	ColumnMeta getColumns();

	boolean next();

	Object getObject(String columnName)throws SQLException;
	
	Object getObject(int columnIndex)throws SQLException;
	
	<T> T getObject(int columnIndex, Class<T> type) throws SQLException;

	boolean getBoolean(String columnName)throws SQLException;
	
	boolean getBoolean(int columnIndex)throws SQLException;

	double getDouble(String columnName)throws SQLException;
	
	double getDouble(int columnIndex)throws SQLException;

	float getFloat(String columnName)throws SQLException;
	
	float getFloat(int columnIndex)throws SQLException;

	long getLong(String columnName)throws SQLException;
	
	long getLong(int columnIndex)throws SQLException;

	int getInt(String columnName)throws SQLException;
	
	int getInt(int columnIndex)throws SQLException;
	
	Clob getClob(String columnName)throws SQLException;
	
	Clob getClob(int columnIndex)throws SQLException;
	
	Blob getBlob(String columnName)throws SQLException;
	
	Blob getBlob(int columnIndex)throws SQLException;
	
	byte[] getBytes(int columnIndex)throws SQLException;

	String getString(String columnName)throws SQLException;
	
	String getString(int columnIndex)throws SQLException;

	Timestamp getTimestamp(String columnName)throws SQLException;
	
	Timestamp getTimestamp(int columnIndex)throws SQLException;

	Time getTime(String columnName)throws SQLException;
	
	Time getTime(int columnIndex)throws SQLException;

	java.sql.Date getDate(String columnName)throws SQLException;
	
	java.sql.Date getDate(int columnIndex)throws SQLException;
	
	InputStream getBinaryStream(int columnIndex) throws SQLException ;
	
	InputStream getUnicodeStream(int columnIndex) throws SQLException;
	
	InputStream getAsciiStream(int columnIndex) throws SQLException;
	
	short getShort(int columnIndex) throws SQLException;
	
	byte getByte(int columnIndex) throws SQLException;
	
	void moveToInsertRow()throws SQLException;

	void deleteRow()throws SQLException;

	void updateRow()throws SQLException;
	
	void updateClob(int columnIndex, Reader reader) throws SQLException;
	
	void updateBlob(int columnIndex, InputStream inputStream)throws SQLException;
	
	void updateNull(String columnName)throws SQLException;
	
	void updateNull(int columnIndex) throws SQLException;
	
	void updateClob(String columnLabel, Reader reader, long length) throws SQLException ;
	
	void updateClob(int columnIndex, Reader reader, long length) throws SQLException;
	
	void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException;
	
	void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException;
	
	void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException;
	
	SQLXML getSQLXML(int columnIndex) throws SQLException;
	
	boolean isClosed() throws SQLException;
	
	void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException;

	void updateObject(String columnName, Object value)throws SQLException;
	
	void updateObject(int columnIndex, Object x) throws SQLException;

	void beforeFirst()throws SQLException;

	void first()throws SQLException;

	boolean previous()throws SQLException;

	void afterLast()throws SQLException;

	void insertRow()throws SQLException;

	/**
	 * 这个方法会关闭ResultSet对象、Statement对象、以及释放连接
	 * @throws SQLException
	 */
	void close()throws SQLException;

	DatabaseDialect getProfile();
	
	RowId getRowId(int columnIndex) throws SQLException;
	
	Map<Reference, List<Condition>> getFilters();
}
