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
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public abstract class AbstractResultSet implements ResultSet {
	protected abstract ResultSet get() throws SQLException;

	public Object getObject(String columnName) throws SQLException {
		return get().getObject(columnName);
	}

	public boolean getBoolean(int i) throws SQLException {
		return get().getBoolean(i);
	}

	public double getDouble(int i) throws SQLException {
		return get().getDouble(i);
	}

	public float getFloat(int i) throws SQLException {
		return get().getFloat(i);
	}

	public long getLong(int i) throws SQLException {
		return get().getLong(i);
	}

	public int getInt(int i) throws SQLException {
		return get().getInt(i);
	}

	public String getString(int i) throws SQLException {
		return get().getString(i);
	}

	public java.sql.Date getDate(int i) throws SQLException {
		return get().getDate(i);
	}

	public Timestamp getTimestamp(int i) throws SQLException {
		return get().getTimestamp(i);
	}

	public Time getTime(int i) throws SQLException {
		return get().getTime(i);
	}

	public Clob getClob(int columnIndex) throws SQLException {
		return get().getClob(columnIndex);
	}

	public Blob getBlob(int columnIndex) throws SQLException {
		return get().getBlob(columnIndex);
	}

	public boolean getBoolean(String columnName) throws SQLException {
		return get().getBoolean(columnName);
	}

	public double getDouble(String columnName) throws SQLException {
		return get().getDouble(columnName);
	}

	public float getFloat(String columnName) throws SQLException {
		return get().getFloat(columnName);
	}

	public long getLong(String columnName) throws SQLException {
		return get().getLong(columnName);
	}

	public int getInt(String columnName) throws SQLException {
		return get().getInt(columnName);
	}

	public Clob getClob(String columnName) throws SQLException {
		return get().getClob(columnName);
	}

	public Blob getBlob(String columnName) throws SQLException {
		return get().getBlob(columnName);
	}

	public String getString(String columnName) throws SQLException {
		return get().getString(columnName);
	}

	public Timestamp getTimestamp(String columnName) throws SQLException {
		return get().getTimestamp(columnName);
	}

	public Time getTime(String columnName) throws SQLException {
		return get().getTime(columnName);
	}

	public Date getDate(String columnName) throws SQLException {
		return get().getDate(columnName);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return get().getBytes(columnIndex);
	}

	public RowId getRowId(int columnIndex) throws SQLException {
		return get().getRowId(columnIndex);
	}

	public Object getObject(int columnIndex) throws SQLException {
		return get().getObject(columnIndex);
	}

	public void insertRow() throws SQLException {
		get().insertRow();
	}

	public void moveToInsertRow() throws SQLException {
		get().moveToInsertRow();
	}

	public void deleteRow() throws SQLException {
		get().deleteRow();
	}

	public void updateRow() throws SQLException {
		get().updateRow();
	}

	public void updateNull(String columnName) throws SQLException {
		get().updateNull(columnName);
	}

	public void updateObject(String columnName, Object value) throws SQLException {
		get().updateObject(columnName, value);
	}

	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return get().getBinaryStream(columnIndex);
	}

	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return get().getUnicodeStream(columnIndex);
	}

	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return get().getAsciiStream(columnIndex);
	}

	public short getShort(int columnIndex) throws SQLException {
		return get().getShort(columnIndex);
	}

	public byte getByte(int columnIndex) throws SQLException {
		return get().getByte(columnIndex);
	}

	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		get().updateClob(columnIndex, reader);
	}

	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		get().updateBlob(columnIndex, inputStream);
	}

	public void updateNull(int columnIndex) throws SQLException {
		get().updateNull(columnIndex);
	}

	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		get().updateClob(columnLabel, reader, length);
	}

	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		get().updateClob(columnIndex, reader, length);
	}

	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		get().updateBlob(columnLabel, inputStream, length);
	}

	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		get().updateSQLXML(columnLabel, xmlObject);

	}

	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		get().updateSQLXML(columnIndex, xmlObject);
	}

	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return get().getSQLXML(columnIndex);
	}

	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		get().updateBlob(columnIndex, inputStream, length);
	}

	public void updateObject(int columnIndex, Object x) throws SQLException {
		get().updateObject(columnIndex, x);
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return get().getCharacterStream(columnIndex);
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return get().getCharacterStream(columnLabel);
	}

	@Override
	public boolean wasNull() throws SQLException {
		return get().wasNull();
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		get().updateBoolean(columnIndex, x);
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		get().updateByte(columnIndex, x);
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		get().updateShort(columnIndex, x);
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		get().updateInt(columnIndex, x);
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		get().updateLong(columnIndex, x);
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		get().updateFloat(columnIndex, x);
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		get().updateDouble(columnIndex, x);
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		get().updateBigDecimal(columnIndex, x);
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		get().updateString(columnIndex, x);
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		get().updateBytes(columnIndex, x);
	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		get().updateDate(columnIndex, x);
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		get().updateTime(columnIndex, x);
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		get().updateTimestamp(columnIndex, x);
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		get().updateAsciiStream(columnIndex, x);
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		get().updateBinaryStream(columnIndex, x);
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		get().updateCharacterStream(columnIndex, x);
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		get().updateObject(columnIndex, x);
		
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		get().updateBoolean(columnLabel, x);
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		get().updateByte(columnLabel, x);
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		get().updateShort(columnLabel, x);
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		get().updateInt(columnLabel, x);
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		get().updateLong(columnLabel, x);
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		get().updateFloat(columnLabel, x);
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		get().updateBigDecimal(columnLabel, x);
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		get().updateString(columnLabel, x);
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		get().updateBytes(columnLabel, x);
	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		get().updateDate(columnLabel, x);
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		get().updateTime(columnLabel, x);
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		get().updateTimestamp(columnLabel, x);
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		get().updateAsciiStream(columnLabel, x,length);
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		get().updateBinaryStream(columnLabel, x,length);
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		get().updateCharacterStream(columnLabel, reader,length);
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		get().updateObject(columnLabel, x,scaleOrLength);
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		get().updateRef(columnIndex, x);
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		get().updateRef(columnLabel, x);
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		get().updateBlob(columnIndex, x);
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		get().updateBlob(columnLabel, x);
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		get().updateClob(columnIndex, x);
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		get().updateClob(columnLabel, x);
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		get().updateArray(columnIndex, x);
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		get().updateArray(columnLabel, x);
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		get().updateRowId(columnIndex, x);
		
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		get().updateRowId(columnLabel, x);
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		get().updateNString(columnIndex, nString);
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		get().updateNString(columnLabel, nString);
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		get().updateNClob(columnIndex, nClob);
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		get().updateNClob(columnLabel, nClob);
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		get().updateNCharacterStream(columnIndex, x,length);
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		get().updateNCharacterStream(columnLabel, reader,length);
		
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		get().updateAsciiStream(columnIndex, x,length);
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		get().updateBinaryStream(columnIndex, x,length);
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		get().updateCharacterStream(columnIndex, x,length);
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		get().updateAsciiStream(columnLabel, x,length);
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		get().updateBinaryStream(columnLabel, x,length);
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		get().updateCharacterStream(columnLabel, reader,length);
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		get().updateNClob(columnIndex, reader,length);
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		get().updateNClob(columnLabel, reader,length);
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		get().updateNCharacterStream(columnIndex, x);
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		get().updateNCharacterStream(columnLabel, reader);
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		get().updateAsciiStream(columnIndex, x);
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		get().updateBinaryStream(columnIndex, x);
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		get().updateCharacterStream(columnIndex, x);
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		get().updateAsciiStream(columnLabel, x);
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		get().updateBinaryStream(columnLabel, x);
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		get().updateCharacterStream(columnLabel, reader);
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		get().updateBlob(columnLabel, inputStream);
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		get().updateClob(columnLabel, reader);
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		get().updateNClob(columnIndex, reader);
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		get().updateNClob(columnLabel, reader);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return get().getBigDecimal(columnIndex,scale);
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return get().getByte(columnLabel);
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return get().getShort(columnLabel);
	}
	@SuppressWarnings("deprecation")
	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return get().getBigDecimal(columnLabel,scale);
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return get().getBytes(columnLabel);
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return get().getAsciiStream(columnLabel);
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return get().getBinaryStream(columnLabel);
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return get().getBigDecimal(columnIndex);
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return get().getBigDecimal(columnLabel);
	}

	@Override
	public int getRow() throws SQLException {
		throw new UnsupportedOperationException("getRow");
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		throw new UnsupportedOperationException("rowUpdated");
	}

	@Override
	public boolean rowInserted() throws SQLException {
		throw new UnsupportedOperationException("rowInserted");
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		throw new UnsupportedOperationException("rowDeleted");
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		get().updateDouble(columnLabel,x);
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		return get().getObject(columnIndex,map);
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		return get().getRef(columnIndex);
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		return get().getArray(columnIndex);
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return get().getObject(columnLabel);
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return get().getRef(columnLabel);
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return get().getArray(columnLabel);
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return get().getDate(columnIndex);
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return get().getDate(columnLabel);
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return get().getTime(columnIndex);
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return get().getTime(columnLabel);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return get().getTimestamp(columnIndex);
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return get().getTimestamp(columnLabel);
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return get().getRowId(columnLabel);
	}


	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		return get().getNClob(columnIndex);
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return get().getNClob(columnLabel);
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return get().getSQLXML(columnLabel);
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		return get().getNString(columnIndex);
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		return get().getNString(columnLabel);
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return get().getNCharacterStream(columnIndex);
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return get().getNCharacterStream(columnLabel);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return (T)this;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return get().getUnicodeStream(columnLabel);
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException("getWarnings");
	}

	@Override
	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException("clearWarnings");
	}

	@Override
	public String getCursorName() throws SQLException {
		throw new UnsupportedOperationException("getCursorName");
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException("findColumn");
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		throw new UnsupportedOperationException("isBeforeFirst");
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		throw new UnsupportedOperationException("isAfterLast");
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		throw new UnsupportedOperationException("absolute");
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		throw new UnsupportedOperationException("relative");
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		throw new UnsupportedOperationException("setFetchDirection");
	}

	@Override
	public int getFetchDirection() throws SQLException {
		throw new UnsupportedOperationException("getFetchDirection");
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		throw new UnsupportedOperationException("setFetchSize");
	}

	@Override
	public int getFetchSize() throws SQLException {
		throw new UnsupportedOperationException("getFetchSize");
	}

	@Override
	public int getType() throws SQLException {
		return get().getType();
	}

	@Override
	public int getConcurrency() throws SQLException {
		throw new UnsupportedOperationException("getConcurrency");
	}

	@Override
	public void refreshRow() throws SQLException {
		throw new UnsupportedOperationException("refreshRow");
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		throw new UnsupportedOperationException("cancelRowUpdates");
	}

	@Override
	public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException("getStatement");
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return get().getURL(columnIndex);
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return get().getURL(columnLabel);
	}

	@Override
	public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException("getHoldability");
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		throw new UnsupportedOperationException("moveToCurrentRow");
	}
}

