package jef.database.wrapper.result;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.ColumnMeta;

/**
 * 这个类是JDBC ResultSet的封装，大部分方法都和JDBC ResultSet一致
 * @author jiyi
 *
 */
public interface IResultSet {
	ColumnMeta getColumns();

	boolean next();

	Object getObject(String columnName)throws SQLException;
	
	Object getObject(int columnIndex)throws SQLException;

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
	
	void moveToInsertRow()throws SQLException;

	void deleteRow()throws SQLException;

	void updateRow()throws SQLException;

	void updateNull(String columnName)throws SQLException;

	void updateObject(String columnName, Object value)throws SQLException;

	void beforeFirst()throws SQLException;

	void first()throws SQLException;

	boolean previous()throws SQLException;

	void afterLast()throws SQLException;

	void insertRow()throws SQLException;

	void close()throws SQLException;

	DatabaseDialect getProfile();
	
	RowId getRowId(int columnIndex) throws SQLException;
	
	Map<Reference, List<Condition>> getFilters();
}
