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
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

@Deprecated
public class JdbcResultSetAdapter implements ResultSet{
	private IResultSet rs;

	public JdbcResultSetAdapter(IResultSet rs){
		this.rs=rs;
		
	}
	public RowId getRowId(int columnIndex) throws SQLException {
		return rs.getRowId(columnIndex);
	}

	public Clob getClob(int columnIndex) throws SQLException {
		return rs.getClob(columnIndex);
	}

	public Blob getBlob(int columnIndex) throws SQLException {
		return rs.getBlob(columnIndex);
	}

	public boolean getBoolean(String columnName) throws SQLException {
		return rs.getBoolean(columnName);
	}

	public double getDouble(String columnName) throws SQLException {
		return rs.getDouble(columnName);
	}

	public float getFloat(String columnName) throws SQLException {
		return rs.getFloat(columnName);
	}

	public long getLong(String columnName) throws SQLException {
		return rs.getLong(columnName);
	}

	public int getInt(String columnName) throws SQLException {
		return rs.getInt(columnName);
	}

	public Clob getClob(String columnName) throws SQLException {
		return rs.getClob(columnName);
	}

	public Blob getBlob(String columnName) throws SQLException {
		return rs.getBlob(columnName);
	}

	public String getString(String columnName) throws SQLException {
		return rs.getString(columnName);
	}

	public Timestamp getTimestamp(String columnName) throws SQLException {
		return rs.getTimestamp(columnName);
	}

	public Time getTime(String columnName) throws SQLException {
		return rs.getTime(columnName);
	}

	public Date getDate(String columnName) throws SQLException {
		return rs.getDate(columnName);
	}

	public Object getObject(String columnName) throws SQLException {
		return rs.getObject(columnName);
	}

	public boolean getBoolean(int i) throws SQLException {
		return rs.getBoolean(i);
	}

	public double getDouble(int i) throws SQLException {
		return rs.getDouble(i);
	}

	public float getFloat(int i) throws SQLException {
		return rs.getFloat(i);
	}

	public long getLong(int i) throws SQLException {
		return rs.getLong(i);
	}

	public int getInt(int i) throws SQLException {
		return rs.getInt(i);
	}

	public String getString(int i) throws SQLException {
		return rs.getString(i);
	}

	public Timestamp getTimestamp(int i) throws SQLException {
		return rs.getTimestamp(i);
	}

	public Time getTime(int i) throws SQLException {
		return rs.getTime(i);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return rs.getBytes(columnIndex);
	}

	public Date getDate(int i) throws SQLException {
		return rs.getDate(i);
	}

	public void moveToInsertRow() throws SQLException {
		rs.moveToInsertRow();
	}

	public void deleteRow() throws SQLException {
		rs.deleteRow();
	}

	public void updateRow() throws SQLException {
		rs.updateRow();
	}

	public void updateNull(String columnName) throws SQLException {
		rs.updateNull(columnName);
	}

	public void updateObject(String columnName, Object value) throws SQLException {
		rs.updateObject(columnName, value);
	}

	public void beforeFirst() throws SQLException {
		rs.beforeFirst();
	}

	public boolean first() throws SQLException {
		rs.first();
		return true;
	}

	public boolean previous() throws SQLException {
		return rs.previous();
	}

	public void afterLast() throws SQLException {
		rs.afterLast();
	}

	public void insertRow() throws SQLException {
		rs.insertRow();
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if(iface==IResultSet.class){
			return (T) rs;
		}
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface==IResultSet.class;
	}

	public boolean next() throws SQLException {
		return rs.next();
	}

	public void close() throws SQLException {
		rs.close();
	}

	public boolean wasNull() throws SQLException {
		throw new UnsupportedOperationException(); 
	}

	public byte getByte(int columnIndex) throws SQLException {
		return rs.getByte(columnIndex);
	}

	public short getShort(int columnIndex) throws SQLException {
		return rs.getShort(columnIndex);
	}

	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return rs.getAsciiStream(columnIndex);
	}

	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return rs.getUnicodeStream(columnIndex);
	}

	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return rs.getBinaryStream(columnIndex);
	}

	public byte getByte(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public short getShort(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public byte[] getBytes(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getCursorName() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return rs.getMetaData();
	}

	public Object getObject(int columnIndex) throws SQLException {
		return rs.getObject(columnIndex);
	}

	public int findColumn(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return rs.getCharacterStream(columnIndex);
	}

	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return rs.getCharacterStream(columnLabel);
	}

	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isBeforeFirst() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isAfterLast() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isFirst() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isLast() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean last() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean absolute(int row) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean relative(int rows) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setFetchDirection(int direction) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getFetchDirection() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setFetchSize(int rows) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getFetchSize() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getType() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getConcurrency() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean rowUpdated() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean rowInserted() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean rowDeleted() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNull(int columnIndex) throws SQLException {
		rs.updateNull(columnIndex);
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateShort(int columnIndex, short x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateInt(int columnIndex, int x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateLong(int columnIndex, long x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateString(int columnIndex, String x) throws SQLException {
		throw new UnsupportedOperationException();		
	}

	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject(int columnIndex, Object x) throws SQLException {
		rs.updateObject(columnIndex, x);
	}

	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateByte(String columnLabel, byte x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateShort(String columnLabel, short x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateInt(String columnLabel, int x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateLong(String columnLabel, long x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateFloat(String columnLabel, float x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDouble(String columnLabel, double x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateString(String columnLabel, String x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDate(String columnLabel, Date x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTime(String columnLabel, Time x) throws SQLException {
		throw new UnsupportedOperationException();		
	}

	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void refreshRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void cancelRowUpdates() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void moveToCurrentRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Ref getRef(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Array getArray(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Ref getRef(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Array getArray(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public URL getURL(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public URL getURL(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw new UnsupportedOperationException();		
	}

	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateArray(String columnLabel, Array x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public RowId getRowId(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getHoldability() throws SQLException {
		return 0;
	}

	public boolean isClosed() throws SQLException {
		return rs.isClosed();
	}

	public void updateNString(int columnIndex, String nString) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNString(String columnLabel, String nString) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public NClob getNClob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public NClob getNClob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return rs.getSQLXML(columnIndex);
	}

	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		rs.updateSQLXML(columnIndex, xmlObject);
	}

	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		rs.updateSQLXML(columnLabel, xmlObject);
		
	}

	public String getNString(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getNString(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		rs.updateBlob(columnIndex, inputStream, length);
	}

	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		rs.updateBlob(columnLabel, inputStream, length);
	}

	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		rs.updateClob(columnIndex, reader, length);
	}

	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		rs.updateClob(columnLabel, reader, length);
	}

	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		rs.updateBlob(columnIndex, inputStream);
	}

	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		rs.updateClob(columnIndex, reader);
	}

	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();
	}
}
