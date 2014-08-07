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

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.meta.Reference;

public abstract class AbstractResultSet implements IResultSet{
	//级联过滤条件
	protected Map<Reference, List<Condition>> filters;
	
	
	protected abstract ResultSet get();
	

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
	
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}
}
