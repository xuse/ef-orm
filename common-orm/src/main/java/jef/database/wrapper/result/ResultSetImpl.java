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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.sql.rowset.CachedRowSet;

import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

/**
 * IResultSet的最简实现
 * @author jiyi
 *
 */
public class ResultSetImpl implements IResultSet {
	protected ResultSet rs;
	protected DatabaseDialect profile;
	private ColumnMeta columns;
	private int total = -1; // -1表示尚未取得
	private Map<Reference, List<Condition>> filters;
	
	public ResultSetImpl(ResultSet rs, DatabaseDialect profile) {
		this.rs = rs;
		this.profile = profile;
	}
	
	
	public void reset(){
		try {
			rs.beforeFirst();
		} catch (SQLException e) {
			LogUtil.exception(e);
		}
	}
	
	
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	public void setFilters(Map<Reference, List<Condition>> filters) {
		this.filters = filters;
	}

	ResultSetImpl(ResultSet rs, ColumnMeta columns,DatabaseDialect dialect) {
		this.rs=rs;
		this.profile=dialect;
		this.columns=columns;
	}

	public ColumnMeta getColumns(){
		if(columns==null){
			try {
				this.columns = new ColumnMeta(getMetaData());
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
		return columns;
	}
	
	public boolean next() {
		try {
			return (rs == null) ? false : rs.next();
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public Object getObject(int columnIndex) throws SQLException {
		return rs.getObject(columnIndex);
	}

	public DatabaseDialect getProfile() {
		return profile;
	}

	public void close() throws SQLException {
		DbUtils.close(rs);
	}
	

	// 输出数据为文本
	public int write(Writer writer) {
		return write(writer, 0);
	}

	/**
	 * 输出数据为文本
	 * 
	 * @param writer
	 * @param maxReturn
	 * @throws SQLException
	 * @throws IOException
	 * @return 返回显示的实际结果数
	 */
	public int write(Writer writer, int maxReturn) {
		int count = 0;
		int shown = 0;
		try {
			while (rs.next()) {
				count++;
				if (count <= maxReturn || maxReturn == 0) {
					List<Object> row = new ArrayList<Object>();
					for(ColumnDescription c: getColumns().getColumns()){
						switch(c.getType()){
						case Types.LONGNVARCHAR:
						case Types.LONGVARCHAR:
						case Types.NVARCHAR:
						case Types.NCHAR:
						case Types.VARCHAR:
						case Types.CHAR:
							row.add(StringUtils.removeChars(rs.getString(c.getN()), '\r','\n'));
							break;
						case Types.BIGINT:
						case Types.INTEGER:
						case Types.SMALLINT:
						case Types.TINYINT:
							
						case Types.NUMERIC:
						case Types.DECIMAL:
						case Types.REAL:	
						case Types.DOUBLE:
						case Types.FLOAT:	
							row.add(rs.getBigDecimal(c.getN()));
							break;
						case Types.BLOB:
							row.add("[blob]");
							break;
						case Types.CLOB:
						case Types.NCLOB:
							Clob lob=rs.getClob(c.getN());
							row.add(IOUtils.asString(lob.getCharacterStream()));
							lob.free();
							break;
						case Types.BOOLEAN:
							row.add(rs.getBoolean(c.getN()));
							break;
						case Types.DATE:
							row.add(rs.getDate(c.getN()));
							break;
						case Types.TIME:
							row.add(rs.getTime(c.getN()));
							break;
						case Types.TIMESTAMP:
							row.add(rs.getTimestamp(c.getN()));
							break;
						case Types.ROWID:
							byte[] bytes=rs.getRowId(c.getN()).getBytes();
							row.add(new String(bytes,"US-ASCII"));
							break;
						default:
							row.add(rs.getObject(c.getN()));
							break;		
						}
					}
					String msg=row.toString();
					writer.write(msg);
					writer.write('\n');
					shown++;
				} else {// 如果要求的数量已经达到到，并且记录总数也已得到，则可以不用再遍历结果集
					if (total > -1)
						break;
				}
			}
		} catch (SQLException e) {
			LogUtil.exception(e);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		if (total == -1)
			total = count;
		return shown;
	}
	
	/**
	 * 获取结果集总数
	 * @return
	 */
	public int getTotal() {
		if(total==-1){
			try {// 如果结果是一次性返回的，则获取结果的数量
				if(rs instanceof CachedRowSet){
					total=((CachedRowSet)rs).size();
				}else if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {//尝试跳到最后一个结果来计算长度
					if (rs.last()) {
						total = rs.getRow();
						rs.beforeFirst();
					} else {
						total = 0;
					}
				}
			} catch (SQLException e) {
				LogUtil.exception(e);
				total=-2;//不再计算
			}
		}
		return total;
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
		return rs.first();
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

	public boolean isClosed() throws SQLException {
		return rs == null || rs.isClosed();
	}

	public ResultSet get() {
		return rs;
	}


	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return rs.getBinaryStream(columnIndex);
	}


	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return rs.getUnicodeStream(columnIndex);
	}


	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return rs.getAsciiStream(columnIndex);
	}


	public short getShort(int columnIndex) throws SQLException {
		return rs.getShort(columnIndex);
	}


	public byte getByte(int columnIndex) throws SQLException {
		return rs.getByte(columnIndex);
	}


	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		rs.updateClob(columnIndex, reader);
	}



	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		rs.updateBlob(columnIndex, inputStream);
	}


	public void updateNull(int columnIndex) throws SQLException {
		rs.updateNull(columnIndex);
	}


	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		rs.updateClob(columnLabel, reader, length);
	}


	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		rs.updateClob(columnIndex, reader, length);
	}


	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		rs.updateBlob(columnLabel, inputStream,length);
	}


	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		rs.updateSQLXML(columnLabel, xmlObject);
		
	}


	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		rs.updateSQLXML(columnIndex, xmlObject);
	}


	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return rs.getSQLXML(columnIndex);
	}


	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		rs.updateBlob(columnIndex, inputStream, length);
	}


	public void updateObject(int columnIndex, Object x) throws SQLException {
		rs.updateObject(columnIndex, x);
	}
	
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return rs.getMetaData();
	}


	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return rs.getCharacterStream(columnIndex);
	}


	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return rs.getCharacterStream(columnLabel);
	}


	@Override
	public boolean wasNull() throws SQLException {
		return rs.wasNull();
	}

	@SuppressWarnings("deprecation")
	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return rs.getBigDecimal(columnIndex,scale);
	}


	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return rs.getByte(columnLabel);
	}


	@Override
	public short getShort(String columnLabel) throws SQLException {
		return rs.getShort(columnLabel);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return rs.getBigDecimal(columnLabel,scale);
	}


	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return rs.getBytes(columnLabel);
	}


	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return rs.getAsciiStream(columnLabel);
	}


	@SuppressWarnings("deprecation")
	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return rs.getUnicodeStream(columnLabel);
	}


	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return rs.getBinaryStream(columnLabel);
	}


	@Override
	public SQLWarning getWarnings() throws SQLException {
		return rs.getWarnings();
	}


	@Override
	public void clearWarnings() throws SQLException {
		rs.clearWarnings();
	}


	@Override
	public String getCursorName() throws SQLException {
		return rs.getCursorName();
	}


	@Override
	public int findColumn(String columnLabel) throws SQLException {
		return rs.findColumn(columnLabel);
	}


	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return rs.getBigDecimal(columnIndex);
	}


	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return rs.getBigDecimal(columnLabel);
	}


	@Override
	public boolean isBeforeFirst() throws SQLException {
		return rs.isBeforeFirst();
	}


	@Override
	public boolean isAfterLast() throws SQLException {
		return rs.isAfterLast();
	}


	@Override
	public boolean isFirst() throws SQLException {
		return rs.isFirst();
	}


	@Override
	public boolean isLast() throws SQLException {
		return rs.isLast();
	}


	@Override
	public boolean last() throws SQLException {
		return rs.last();
	}


	@Override
	public int getRow() throws SQLException {
		return rs.getRow();
	}


	@Override
	public boolean absolute(int row) throws SQLException {
		return rs.absolute(row);
	}


	@Override
	public boolean relative(int rows) throws SQLException {
		return rs.relative(rows);
	}


	@Override
	public void setFetchDirection(int direction) throws SQLException {
		rs.setFetchDirection(direction);
	}


	@Override
	public int getFetchDirection() throws SQLException {
		return rs.getFetchDirection();
	}


	@Override
	public void setFetchSize(int rows) throws SQLException {
		rs.setFetchSize(rows);
	}


	@Override
	public int getFetchSize() throws SQLException {
		return rs.getFetchSize();
	}


	@Override
	public int getType() throws SQLException {
		return rs.getType();
	}


	@Override
	public int getConcurrency() throws SQLException {
		return rs.getConcurrency();
	}


	@Override
	public boolean rowUpdated() throws SQLException {
		return rs.rowUpdated();
	}


	@Override
	public boolean rowInserted() throws SQLException {
		return rs.rowInserted();
	}


	@Override
	public boolean rowDeleted() throws SQLException {
		return rs.rowDeleted();
	}


	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		rs.updateBoolean(columnIndex, x);
	}


	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		rs.updateByte(columnIndex, x);
	}


	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		rs.updateShort(columnIndex, x);
		
	}


	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		rs.updateInt(columnIndex, x);
	}


	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		rs.updateLong(columnIndex, x);
	}


	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		rs.updateFloat(columnIndex, x);
	}


	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		rs.updateDouble(columnIndex, x);
	}


	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		rs.updateBigDecimal(columnIndex, x);
	}


	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		rs.updateString(columnIndex, x);
	}


	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		rs.updateBytes(columnIndex, x);
	}


	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		rs.updateDate(columnIndex, x);
		
	}


	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		rs.updateTime(columnIndex, x);
		
	}


	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		rs.updateTimestamp(columnIndex, x);
		
	}


	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		rs.updateAsciiStream(columnIndex, x);
	}


	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		rs.updateAsciiStream(columnIndex, x);
	}


	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		rs.updateCharacterStream(columnIndex, x);
	}


	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		rs.updateObject(columnIndex, x);
	}


	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		rs.updateBoolean(columnLabel, x);
	}


	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		rs.updateByte(columnLabel, x);
	}


	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		rs.updateShort(columnLabel, x);
	}


	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		rs.updateInt(columnLabel, x);
	}


	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		rs.updateLong(columnLabel, x);
	}


	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		rs.updateFloat(columnLabel, x);
	}


	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		rs.updateDouble(columnLabel, x);
	}


	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		rs.updateBigDecimal(columnLabel, x);
	}


	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		rs.updateString(columnLabel, x);
	}


	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		rs.updateBytes(columnLabel, x);
	}


	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		rs.updateDate(columnLabel, x);
	}


	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		rs.updateTime(columnLabel, x);
	}


	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		rs.updateTimestamp(columnLabel, x);
	}


	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		rs.updateAsciiStream(columnLabel, x,length);
	}


	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		rs.updateBinaryStream(columnLabel, x,length);
	}


	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		rs.updateCharacterStream(columnLabel,reader,length);
	}


	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		rs.updateObject(columnLabel, x,scaleOrLength);
	}


	@Override
	public void refreshRow() throws SQLException {
		rs.refreshRow();
		
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		rs.cancelRowUpdates();
	}


	@Override
	public void moveToCurrentRow() throws SQLException {
		rs.moveToCurrentRow();
	}


	@Override
	public Statement getStatement() throws SQLException {
		return rs.getStatement();
	}


	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		return rs.getObject(columnIndex, map);
	}


	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		return rs.getRef(columnIndex);
	}


	@Override
	public Array getArray(int columnIndex) throws SQLException {
		return rs.getArray(columnIndex);
	}


	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return rs.getObject(columnLabel,map);
	}


	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return rs.getRef(columnLabel);
	}


	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return rs.getArray(columnLabel);
	}


	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return rs.getDate(columnIndex,cal);
	}


	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return rs.getDate(columnLabel,cal);
	}


	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return rs.getTime(columnIndex,cal);
	}


	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return rs.getTime(columnLabel,cal);
	}


	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return rs.getTimestamp(columnIndex,cal);
	}


	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return rs.getTimestamp(columnLabel,cal);
	}


	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return rs.getURL(columnIndex);
	}


	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return rs.getURL(columnLabel);
	}


	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		rs.updateRef(columnIndex,x);
	}


	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		rs.updateRef(columnLabel,x);
	}


	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		rs.updateBlob(columnIndex,x);
	}


	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		rs.updateBlob(columnLabel,x);
	}


	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		rs.updateClob(columnIndex,x);
	}


	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		rs.updateClob(columnLabel,x);
	}


	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		rs.updateArray(columnIndex,x);
	}


	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		rs.updateArray(columnLabel,x);
	}


	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return rs.getRowId(columnLabel);
	}


	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		rs.updateRowId(columnIndex,x);
	}


	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		rs.updateRowId(columnLabel,x);
	}

	@Override
	public int getHoldability() throws SQLException {
		return rs.getHoldability();
	}


	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		rs.updateNString(columnIndex, nString);
	}


	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		rs.updateNString(columnLabel, nString);
	}


	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		rs.updateNClob(columnIndex, nClob);
	}


	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		rs.updateNClob(columnLabel, nClob);
	}


	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		return rs.getNClob(columnIndex);
	}


	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return rs.getNClob(columnLabel);
	}


	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return rs.getSQLXML(columnLabel);
	}


	@Override
	public String getNString(int columnIndex) throws SQLException {
		return rs.getNString(columnIndex);
	}


	@Override
	public String getNString(String columnLabel) throws SQLException {
		return rs.getNString(columnLabel);
	}


	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return rs.getNCharacterStream(columnIndex);
	}


	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return rs.getNCharacterStream(columnLabel);
	}


	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		rs.updateNCharacterStream(columnIndex, x, length);
	}


	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		rs.updateNCharacterStream(columnLabel, reader,length);
	}


	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		rs.updateAsciiStream(columnIndex, x, length);
	}


	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		rs.updateBinaryStream(columnIndex, x, length);
	}


	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		rs.updateCharacterStream(columnIndex, x, length);
	}


	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		rs.updateAsciiStream(columnLabel, x, length);
	}


	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		rs.updateBinaryStream(columnLabel, x, length);
	}


	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		rs.updateCharacterStream(columnLabel, reader, length);
	}


	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		rs.updateNClob(columnIndex, reader, length);
	}


	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		rs.updateNClob(columnLabel, reader, length);
	}


	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		rs.updateNClob(columnIndex, x);
	}


	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		rs.updateNCharacterStream(columnLabel, reader);
	}


	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		rs.updateAsciiStream(columnIndex, x);
	}


	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		rs.updateBinaryStream(columnIndex, x);
	}


	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		rs.updateCharacterStream(columnIndex, x);
	}


	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		rs.updateAsciiStream(columnLabel, x);
	}


	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		rs.updateBinaryStream(columnLabel, x);
	}


	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		rs.updateCharacterStream(columnLabel, reader);
	}


	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		rs.updateBlob(columnLabel, inputStream);
	}


	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		rs.updateClob(columnLabel, reader);
	}


	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		rs.updateNClob(columnIndex, reader);
	}


	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		rs.updateNClob(columnLabel, reader);
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
}
