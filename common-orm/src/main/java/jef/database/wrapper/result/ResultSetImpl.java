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
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class ResultSetImpl implements IResultSet {
	protected ResultSet rs;
	protected DatabaseDialect profile;
	private ColumnMeta columns;
	private int total = -1; // -1表示尚未取得

	public ResultSetImpl(ResultSet rs, DatabaseDialect profile) {
		this.rs = rs;
		this.profile = profile;
		try {
			initMetadata();
		} catch (SQLException e) {
			LogUtil.exception(e);
		}
	}
	
	
	public void reset(){
		try {
			rs.beforeFirst();
		} catch (SQLException e) {
			LogUtil.exception(e);
		}
	}
	
	private Map<Reference, List<Condition>> filters;
	
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

	// 获取结果集元数据
	private void initMetadata() throws SQLException {
		ResultSetMetaData meta = rs.getMetaData();
		List<ColumnDescription> columnList = new ArrayList<ColumnDescription>();
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			//对于Oracle getCOlumnName和getColumnLabel是一样的（非标准JDBC实现），MySQL正确地实现了JDBC的要求，getLabel得到别名，getColumnName得到表的列名
			String name=meta.getColumnLabel(i);  
			int type=meta.getColumnType(i);
			columnList.add(new ColumnDescription(i,type,name,meta.getTableName(i),meta.getSchemaName(i)));
		}
		this.columns = new ColumnMeta(columnList);
	}

	public ColumnMeta getColumns(){
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
					for(ColumnDescription c: columns.getColumns()){
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

	public void first() throws SQLException {
		rs.first();
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


	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		return (T) rs.getObject(columnIndex);
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
}
