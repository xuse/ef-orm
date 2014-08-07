/*

 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jef.database.rowset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.BaseRowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetWarning;
import javax.sql.rowset.serial.SQLInputImpl;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialRef;
import javax.sql.rowset.serial.SerialStruct;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

import jef.database.ORMConfig;


/**
 * The standard implementation of the <code>CachedRowSet</code> interface.
 * 
 * See interface defintion for full behaviour and implementation requirements.
 * This reference implementation has made provision for a one-to-one write back
 * facility and it is curremtly be possible to change the peristence provider
 * during the life-time of any CachedRowSetImpl.
 * 
 * @author Jonathan Bruce, Amit Handa
 */

@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
public class CachedRowSetImpl extends BaseRowSet implements RowSet, CachedRowSet {

	/**
	 * The <code>RowSetMetaData</code> object that contains information about
	 * the columns in this <code>CachedRowSetImpl</code> object.
	 * 
	 * @serial
	 */
	private RowSetMetaDataImpl RowSetMD;

	// Properties of this RowSet

	/**
	 * An array containing the columns in this <code>CachedRowSetImpl</code>
	 * object that form a unique identifier for a row. This array is used by the
	 * writer.
	 * 
	 * @serial
	 */
	private int keyCols[];

	/**
	 * The name of the table in the underlying database to which updates should
	 * be written. This name is needed because most drivers do not return this
	 * information in a <code>ResultSetMetaData</code> object.
	 * 
	 * @serial
	 */
	private String tableName;

	/**
	 * A <code>List</code> object containing the <code>Row</code> objects that
	 * comprise this <code>CachedRowSetImpl</code> object.
	 * 
	 * @serial
	 */
	private final List<Row> rvh = new ArrayList<Row>(100);
	/**
	 * The current postion of the cursor in this <code>CachedRowSetImpl</code>
	 * object.
	 * 
	 * @serial
	 */
	private int cursorPos;

	/**
	 * The total number of rows currently in this <code>CachedRowSetImpl</code>
	 * object.
	 * 
	 * @serial
	 */
	private int numRows;

	/**
	 * A <code>boolean</code> indicating whether the last value returned was an
	 * SQL <code>NULL</code>.
	 * 
	 * @serial
	 */
	private boolean lastValueNull;


	/**
	 * The Vector holding the Match Columns
	 */
	private List iMatchColumns;

	/**
	 * The Vector that will hold the Match Column names.
	 */
	private List strMatchColumns;

	protected transient JdbcRowSetResourceBundle resBundle;

	public CachedRowSetImpl() throws SQLException {
		resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
		initContainer();
		initProperties();

	}

	/**
	 * Sets the <code>rvh</code> field to a new <code>Vector</code> object with
	 * a capacity of 100 and sets the <code>cursorPos</code> and
	 * <code>numRows</code> fields to zero.
	 */
	private void initContainer() {
		cursorPos = 0;
		numRows = 0;
	}

	/**
	 * Sets the properties for this <code>CachedRowSetImpl</code> object to
	 * their default values. This method is called internally by the default
	 * constructor.
	 */

	private void initProperties() throws SQLException {

		if (resBundle == null) {
			resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
		}
		setShowDeleted(false);
		setQueryTimeout(0);
		setMaxRows(0);
		setMaxFieldSize(0);
		setType(ResultSet.TYPE_SCROLL_INSENSITIVE);
		setConcurrency(ResultSet.CONCUR_READ_ONLY);

		if ((rvh.size() > 0) && (isReadOnly() == false))
			setReadOnly(false);
		else
			setReadOnly(true);
		setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		setEscapeProcessing(true);
		setTypeMap(null);

		iMatchColumns = new Vector(10);
		for (int i = 0; i < 10; i++) {
			iMatchColumns.add(i, new Integer(-1));
		}

		strMatchColumns = new Vector(10);
		for (int j = 0; j < 10; j++) {
			strMatchColumns.add(j, null);
		}
	}

	// ---------------------------------------------------------------------
	// Reading and writing data
	// ---------------------------------------------------------------------

	public void refresh() throws SQLException {
		numRows = rvh.size();;
		// Also rowsFetched should be equal to rvh.size()
		notifyRowSetChanged();
	}
	public void populate(ResultSet data) throws SQLException {
		Map<String, Class<?>> map = getTypeMap();
		if (data == null) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.populate").toString());
		}
		if(RowSetMD==null){
			RowSetMD = new RowSetMetaDataImpl();
			initMetaData(RowSetMD, data.getMetaData());	
		}
		
		int numCols = RowSetMD.getColumnCount();
		int count=rvh.size();
		int max=ORMConfig.getInstance().getPartitionInMemoryMaxRows();
		if(max==0)max=Integer.MAX_VALUE;
		while (data.next()) {
			if(++count>max){
				throw new SQLException("The Inmemory operate reaches it max limit of "+ max);
			}
			Row currentRow = new Row(numCols);
			for (int i = 1; i <= numCols; i++) {
				Object obj;
				if (map == null) {
					try {
						obj = data.getObject(i);
					} catch (SQLException e) {
						// PG has bug on handle type MONEY, it attempts to
						// convert money to double, even if it is a string with
						// currency unit.
						obj = data.getString(i);
					}
				} else {
					obj = data.getObject(i, map);
				}
				if (obj instanceof Struct) {
					obj = new SerialStruct((Struct) obj, map);
				} else if (obj instanceof SQLData) {
					obj = new SerialStruct((SQLData) obj, map);
				} else if (obj instanceof Blob) {
					obj = new SerialBlob((Blob) obj);
				} else if (obj instanceof Clob) {
					obj = new SerialClob((Clob) obj);
				} else if (obj instanceof java.sql.Array) {
					obj = new SerialArray((java.sql.Array) obj, map);
				}
				((Row) currentRow).initColumnObject(i, obj);
			}
			rvh.add(currentRow);
		}

		numRows = rvh.size();;
		notifyRowSetChanged();

	}

	/**
	 * Initializes the given <code>RowSetMetaData</code> object with the values
	 * in the given <code>ResultSetMetaData</code> object.
	 * 
	 * @param md
	 *            the <code>RowSetMetaData</code> object for this
	 *            <code>CachedRowSetImpl</code> object, which will be set with
	 *            values from rsmd
	 * @param rsmd
	 *            the <code>ResultSetMetaData</code> object from which new
	 *            values for md will be read
	 * @throws SQLException
	 *             if an error occurs
	 */
	private void initMetaData(RowSetMetaDataImpl md, ResultSetMetaData rsmd) throws SQLException {
		int numCols = rsmd.getColumnCount();

		md.setColumnCount(numCols);
		boolean isCaseSensitive = numCols > 0 ? rsmd.isCaseSensitive(1) : false;
		for (int col = 1; col <= numCols; col++) {
			md.setAutoIncrement(col, rsmd.isAutoIncrement(col));
			md.setCaseSensitive(col, isCaseSensitive); // fix the MySQL
														// performance
														// issue:http://blog.csdn.net/match_ding/article/details/11078553
			md.setCurrency(col, rsmd.isCurrency(col));
			md.setNullable(col, rsmd.isNullable(col));
			md.setSigned(col, rsmd.isSigned(col));
			md.setSearchable(col, rsmd.isSearchable(col));
			/*
			 * The PostgreSQL drivers sometimes return negative
			 * columnDisplaySize, which causes an exception to be thrown. Check
			 * for it.
			 */
			int size = rsmd.getColumnDisplaySize(col);
			if (size < 0) {
				size = 0;
			}
			md.setColumnDisplaySize(col, size);
			md.setColumnLabel(col, rsmd.getColumnLabel(col));
			md.setColumnName(col, rsmd.getColumnName(col));
			md.setSchemaName(col, rsmd.getSchemaName(col));
			/*
			 * Drivers return some strange values for precision, for non-numeric
			 * data, including reports of non-integer values; maybe we should
			 * check type, & set to 0 for non-numeric types.
			 */
			int precision = rsmd.getPrecision(col);
			if (precision < 0) {
				precision = 0;
			}
			md.setPrecision(col, precision);

			/*
			 * It seems, from a bug report, that a driver can sometimes return a
			 * negative value for scale. javax.sql.rowset.RowSetMetaDataImpl
			 * will throw an exception if we attempt to set a negative value. As
			 * such, we'll check for this case.
			 */
			int scale = rsmd.getScale(col);
			if (scale < 0) {
				scale = 0;
			}
			md.setScale(col, scale);
			md.setTableName(col, rsmd.getTableName(col));
			md.setCatalogName(col, rsmd.getCatalogName(col));
			md.setColumnType(col, rsmd.getColumnType(col));
			md.setColumnTypeName(col, rsmd.getColumnTypeName(col));
		}
	}

	// --------------------------------------------------------------------
	// Views
	// --------------------------------------------------------------------

	/**
	 * Returns a new <code>RowSet</code> object backed by the same data as that
	 * of this <code>CachedRowSetImpl</code> object and sharing a set of cursors
	 * with it. This allows cursors to interate over a shared set of rows,
	 * providing multiple views of the underlying data.
	 * 
	 * @return a <code>RowSet</code> object that is a copy of this
	 *         <code>CachedRowSetImpl</code> object and shares a set of cursors
	 *         with it
	 * @throws SQLException
	 *             if an error occurs or cloning is not supported
	 * @see javax.sql.RowSetEvent
	 * @see javax.sql.RowSetListener
	 */
	public RowSet createShared() throws SQLException {
		RowSet clone;
		try {
			clone = (RowSet) clone();
		} catch (CloneNotSupportedException ex) {
			throw new SQLException(ex.getMessage());
		}
		return clone;
	}

	/**
	 * Creates a <code>RowSet</code> object that is a deep copy of this
	 * <code>CachedRowSetImpl</code> object's data, including constraints.
	 * Updates made on a copy are not visible to the original rowset; a copy of
	 * a rowset is completely independent from the original.
	 * <P>
	 * Making a copy saves the cost of creating an identical rowset from first
	 * principles, which can be quite expensive. For example, it can eliminate
	 * the need to query a remote database server.
	 * 
	 * @return a new <code>CachedRowSet</code> object that is a deep copy of
	 *         this <code>CachedRowSet</code> object and is completely
	 *         independent from this <code>CachedRowSetImpl</code> object.
	 * @throws SQLException
	 *             if an error occurs in generating the copy of this of the
	 *             <code>CachedRowSetImpl</code>
	 * @see #createShared
	 * @see javax.sql.RowSetEvent
	 * @see javax.sql.RowSetListener
	 * @deprecated 无比糟糕的性能
	 */
	public CachedRowSet createCopy() throws SQLException {
		throw new UnsupportedOperationException();
		// ObjectOutputStream out;
		// ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		// try {
		// out = new ObjectOutputStream(bOut);
		// out.writeObject(this);
		// } catch (IOException ex) {
		// throw new
		// SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.clonefail").toString()
		// , ex.getMessage()));
		// }
		//
		// ObjectInputStream in;
		//
		// try {
		// ByteArrayInputStream bIn = new
		// ByteArrayInputStream(bOut.toByteArray());
		// in = new ObjectInputStream(bIn);
		// } catch (StreamCorruptedException ex) {
		// throw new
		// SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.clonefail").toString()
		// , ex.getMessage()));
		// } catch (IOException ex) {
		// throw new
		// SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.clonefail").toString()
		// , ex.getMessage()));
		// }
		//
		// try {
		// //return ((CachedRowSet)(in.readObject()));
		// CachedRowSetImpl crsTemp = (CachedRowSetImpl)in.readObject();
		// crsTemp.resBundle = this.resBundle;
		// return ((CachedRowSet)crsTemp);
		//
		// } catch (ClassNotFoundException ex) {
		// throw new
		// SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.clonefail").toString()
		// , ex.getMessage()));
		// } catch (OptionalDataException ex) {
		// throw new
		// SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.clonefail").toString()
		// , ex.getMessage()));
		// } catch (IOException ex) {
		// throw new
		// SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.clonefail").toString()
		// , ex.getMessage()));
		// }
	}

	// ---------------------------------------------------------------------
	// Reading and writing data
	// ---------------------------------------------------------------------

	/**
	 * Populates this <code>CachedRowSetImpl</code> object with data. This form
	 * of the method uses the rowset's user, password, and url or data source
	 * name properties to create a database connection. If properties that are
	 * needed have not been set, this method will throw an exception.
	 * <P>
	 * Another form of this method uses an existing JDBC <code>Connection</code>
	 * object instead of creating a new one; therefore, it ignores the
	 * properties used for establishing a new connection.
	 * <P>
	 * The query specified by the command property is executed to create a
	 * <code>ResultSet</code> object from which to retrieve data. The current
	 * contents of the rowset are discarded, and the rowset's metadata is also
	 * (re)set. If there are outstanding updates, they are also ignored.
	 * <P>
	 * The method <code>execute</code> closes any database connections that it
	 * creates.
	 * 
	 * @throws SQLException
	 *             if an error occurs or the necessary properties have not been
	 *             set
	 */
	public void execute() throws SQLException {
	}

	// -----------------------------------
	// Methods inherited from ResultSet
	// -----------------------------------

	/**
	 * Moves the cursor down one row from its current position and returns
	 * <code>true</code> if the new cursor position is a valid row. The cursor
	 * for a new <code>ResultSet</code> object is initially positioned before
	 * the first row. The first call to the method <code>next</code> moves the
	 * cursor to the first row, making it the current row; the second call makes
	 * the second row the current row, and so on.
	 * 
	 * <P>
	 * If an input stream from the previous row is open, it is implicitly
	 * closed. The <code>ResultSet</code> object's warning chain is cleared when
	 * a new row is read.
	 * 
	 * @return <code>true</code> if the new current row is valid;
	 *         <code>false</code> if there are no more rows
	 * @throws SQLException
	 *             if an error occurs or the cursor is not positioned in the
	 *             rowset, before the first row, or after the last row
	 */
	public boolean next() throws SQLException {
		return cursorPos++ < numRows;
	}

	/**
	 * Closes this <code>CachedRowSetImpl</code> objecy and releases any
	 * resources it was using.
	 * 
	 * @throws SQLException
	 *             if an error occurs when releasing any resources in use by
	 *             this <code>CachedRowSetImpl</code> object
	 */
	public void close() throws SQLException {
		// close all data structures holding
		// the disconnected rowset
		cursorPos = 0;
		numRows = 0;

		// clear the vector of it's present contents
		rvh.clear();

		// this will make it eligible for gc
		// rvh = null;
	}

	/**
	 * Reports whether the last column read was SQL <code>NULL</code>. Note that
	 * you must first call the method <code>getXXX</code> on a column to try to
	 * read its value and then call the method <code>wasNull</code> to determine
	 * whether the value was SQL <code>NULL</code>.
	 * 
	 * @return <code>true</code> if the value in the last column read was SQL
	 *         <code>NULL</code>; <code>false</code> otherwise
	 * @throws SQLException
	 *             if an error occurs
	 */
	public boolean wasNull() throws SQLException {
		return lastValueNull;
	}

	/**
	 * Sets the field <code>lastValueNull</code> to the given
	 * <code>boolean</code> value.
	 * 
	 * @param value
	 *            <code>true</code> to indicate that the value of the last
	 *            column read was SQL <code>NULL</code>; <code>false</code> to
	 *            indicate that it was not
	 */
	private void setLastValueNull(boolean value) {
		lastValueNull = value;
	}

	// Methods for accessing results by column index

	/**
	 * Checks to see whether the given index is a valid column number in this
	 * <code>CachedRowSetImpl</code> object and throws an
	 * <code>SQLException</code> if it is not. The index is out of bounds if it
	 * is less than <code>1</code> or greater than the number of columns in this
	 * rowset.
	 * <P>
	 * This method is called internally by the <code>getXXX</code> and
	 * <code>updateXXX</code> methods.
	 * 
	 * @param idx
	 *            the number of a column in this <code>CachedRowSetImpl</code>
	 *            object; must be between <code>1</code> and the number of rows
	 *            in this rowset
	 * @throws SQLException
	 *             if the given index is out of bounds
	 */
	private void checkIndex(int idx) throws SQLException {
		if (idx < 1 || idx > RowSetMD.getColumnCount()) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.invalidcol").toString());
		}
	}

	/**
	 * Checks to see whether the cursor for this <code>CachedRowSetImpl</code>
	 * object is on a row in the rowset and throws an <code>SQLException</code>
	 * if it is not.
	 * <P>
	 * This method is called internally by <code>getXXX</code> methods, by
	 * <code>updateXXX</code> methods, and by methods that update, insert, or
	 * delete a row or that cancel a row update, insert, or delete.
	 * 
	 * @throws SQLException
	 *             if the cursor for this <code>CachedRowSetImpl</code> object
	 *             is not on a valid row
	 */
	private void checkCursor() throws SQLException {
		if (isAfterLast() == true || isBeforeFirst() == true) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.invalidcp").toString());
		}
	}

	/**
	 * Returns the column number of the column with the given name in this
	 * <code>CachedRowSetImpl</code> object. This method throws an
	 * <code>SQLException</code> if the given name is not the name of one of the
	 * columns in this rowset.
	 * 
	 * @param name
	 *            a <code>String</code> object that is the name of a column in
	 *            this <code>CachedRowSetImpl</code> object
	 * @throws SQLException
	 *             if the given name does not match the name of one of the
	 *             columns in this rowset
	 */
	private int getColIdxByName(String name) throws SQLException {
		RowSetMD = (RowSetMetaDataImpl) this.getMetaData();
		int cols = RowSetMD.getColumnCount();

		for (int i = 1; i <= cols; ++i) {
			String colName = RowSetMD.getColumnName(i);
			if (colName != null)
				if (name.equalsIgnoreCase(colName))
					return (i);
				else
					continue;
		}
		throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.invalcolnm").toString());

	}

	/**
	 * Returns the insert row or the current row of this
	 * <code>CachedRowSetImpl</code>object.
	 * 
	 * @return the <code>Row</code> object on which this
	 *         <code>CachedRowSetImpl</code> objects's cursor is positioned
	 */
	protected Row getCurrentRow() {
		return (Row) (rvh.get(cursorPos - 1));
	}

	/**
	 * Removes the row on which the cursor is positioned.
	 * <p>
	 * This is a implementation only method and is not required as a standard
	 * implementation of the <code>CachedRowSet</code> interface.
	 * 
	 * @throws SQLException
	 *             if the cursor is positioned on the insert row
	 */
	protected void removeCurrentRow() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>String</code> object.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, SMALLINT, INTEGER, BIGINT, REAL,
	 * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, <b>CHAR</b>, <b>VARCHAR</b></code>
	 *             or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type.
	 */
	public String getString(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		return value.toString();
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>boolean</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value as a <code>boolean</code> in the Java progamming
	 *         language; if the value is SQL <code>NULL</code>, the result is
	 *         <code>false</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>BOOLEAN</code> value
	 * @see #getBoolean(String)
	 */
	public boolean getBoolean(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return false;
		}

		// check for Boolean...
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}

		// convert to a Double and compare to zero
		try {
			Double d = new Double(value.toString());
			if (d.compareTo(new Double((double) 0)) == 0) {
				return false;
			} else {
				return true;
			}
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.boolfail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>byte</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value as a <code>byte</code> in the Java programming
	 *         language; if the value is SQL <code>NULL</code>, the result is
	 *         <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code><b>TINYINT</b>, SMALLINT, INTEGER, BIGINT, REAL,
	 *            FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code> or
	 *             <code>LONGVARCHAR</code> value. The bold SQL type designates
	 *             the recommended return type.
	 * @see #getByte(String)
	 */
	public byte getByte(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (byte) 0;
		}
		try {
			return ((new Byte(value.toString())).byteValue());
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.bytefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>short</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, <b>SMALLINT</b>, INTEGER, BIGINT, REAL
	 * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code> or
	 *             <code>LONGVARCHAR</code> value. The bold SQL type designates
	 *             the recommended return type.
	 * @see #getShort(String)
	 */
	public short getShort(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (short) 0;
		}

		try {
			return ((new Short(value.toString().trim())).shortValue());
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.shortfail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as an <code>int</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, SMALLINT, <b>INTEGER</b>, BIGINT, REAL
	 * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code> or
	 *             <code>LONGVARCHAR</code> value. The bold SQL type designates
	 *             the recommended return type.
	 */
	public int getInt(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (int) 0;
		}

		try {
			return ((new Integer(value.toString().trim())).intValue());
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.intfail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>long</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, SMALLINT, INTEGER, <b>BIGINT</b>, REAL
	 * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code> or
	 *             <code>LONGVARCHAR</code> value. The bold SQL type designates
	 *             the recommended return type.
	 * @see #getLong(String)
	 */
	public long getLong(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (long) 0;
		}
		try {
			return ((new Long(value.toString().trim())).longValue());
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.longfail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>float</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, SMALLINT, INTEGER, BIGINT, <b>REAL</b>,
	 * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code> or
	 *             <code>LONGVARCHAR</code> value. The bold SQL type designates
	 *             the recommended return type.
	 * @see #getFloat(String)
	 */
	public float getFloat(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (float) 0;
		}
		try {
			return ((new Float(value.toString())).floatValue());
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.floatfail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>double</code> value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, SMALLINT, INTEGER, BIGINT, REAL,
	 * <b>FLOAT</b>, <b>DOUBLE</b>, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code>
	 *             or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type.
	 * @see #getDouble(String)
	 * 
	 */
	public double getDouble(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (double) 0;
		}
		try {
			return ((new Double(value.toString().trim())).doubleValue());
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.doublefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.math.BigDecimal</code> object.
	 * <P>
	 * This method is deprecated; use the version of <code>getBigDecimal</code>
	 * that does not take a scale parameter and returns a value with full
	 * precision.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @param scale
	 *            the number of digits to the right of the decimal point in the
	 *            value returned
	 * @return the column value with the specified number of digits to the right
	 *         of the decimal point; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if the given column index is out of bounds, the cursor is not
	 *             on a valid row, or this method fails
	 * @deprecated
	 */
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		Object value;
		BigDecimal bDecimal, retVal;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return (new BigDecimal(0));
		}

		bDecimal = this.getBigDecimal(columnIndex);

		retVal = bDecimal.setScale(scale);

		return retVal;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>byte</code> array value.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value as a <code>byte</code> array in the Java
	 *         programming language; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * 
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code><b>BINARY</b>, <b>VARBINARY</b> or
	 * LONGVARBINARY</code> value. The bold SQL type designates the recommended
	 *             return type.
	 * @see #getBytes(String)
	 */
	public byte[] getBytes(int columnIndex) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isBinary(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		return (byte[]) (getCurrentRow().getColumnObject(columnIndex));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Date</code>
	 * object.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value as a <code>java.sql.Data</code> object; if the
	 *         value is SQL <code>NULL</code>, the result is <code>null</code>
	 * @throws SQLException
	 *             if the given column index is out of bounds, the cursor is not
	 *             on a valid row, or this method fails
	 */
	public java.sql.Date getDate(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		/*
		 * The object coming back from the db could be a date, a timestamp, or a
		 * char field variety. If it's a date type return it, a timestamp we
		 * turn into a long and then into a date, char strings we try to parse.
		 * Yuck.
		 */
		switch (RowSetMD.getColumnType(columnIndex)) {
		case java.sql.Types.DATE: {
			long sec = ((java.sql.Date) value).getTime();
			return new java.sql.Date(sec);
		}
		case java.sql.Types.TIMESTAMP: {
			long sec = ((java.sql.Timestamp) value).getTime();
			return new java.sql.Date(sec);
		}
		case java.sql.Types.CHAR:
		case java.sql.Types.VARCHAR:
		case java.sql.Types.LONGVARCHAR: {
			try {
				DateFormat df = DateFormat.getDateInstance();
				return ((java.sql.Date) (df.parse(value.toString())));
			} catch (ParseException ex) {
				throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.datefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
			}
		}
		default: {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.datefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Time</code>
	 * object.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if the given column index is out of bounds, the cursor is not
	 *             on a valid row, or this method fails
	 */
	public java.sql.Time getTime(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		/*
		 * The object coming back from the db could be a date, a timestamp, or a
		 * char field variety. If it's a date type return it, a timestamp we
		 * turn into a long and then into a date, char strings we try to parse.
		 * Yuck.
		 */
		switch (RowSetMD.getColumnType(columnIndex)) {
		case java.sql.Types.TIME: {
			return (java.sql.Time) value;
		}
		case java.sql.Types.TIMESTAMP: {
			long sec = ((java.sql.Timestamp) value).getTime();
			return new java.sql.Time(sec);
		}
		case java.sql.Types.CHAR:
		case java.sql.Types.VARCHAR:
		case java.sql.Types.LONGVARCHAR: {
			try {
				DateFormat tf = DateFormat.getTimeInstance();
				return ((java.sql.Time) (tf.parse(value.toString())));
			} catch (ParseException ex) {
				throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.timefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
			}
		}
		default: {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.timefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Timestamp</code>
	 * object.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if the given column index is out of bounds, the cursor is not
	 *             on a valid row, or this method fails
	 */
	public java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		/*
		 * The object coming back from the db could be a date, a timestamp, or a
		 * char field variety. If it's a date type return it; a timestamp we
		 * turn into a long and then into a date; char strings we try to parse.
		 * Yuck.
		 */
		switch (RowSetMD.getColumnType(columnIndex)) {
		case java.sql.Types.TIMESTAMP: {
			return (java.sql.Timestamp) value;
		}
		case java.sql.Types.TIME: {
			long sec = ((java.sql.Time) value).getTime();
			return new java.sql.Timestamp(sec);
		}
		case java.sql.Types.INTEGER: {
			long sec = (Long) value;
			return new java.sql.Timestamp(sec);
		}
		case java.sql.Types.DATE: {
			long sec = ((java.sql.Date) value).getTime();
			return new java.sql.Timestamp(sec);
		}
		case java.sql.Types.CHAR:
		case java.sql.Types.VARCHAR:
		case java.sql.Types.LONGVARCHAR: {
			try {
				DateFormat tf = DateFormat.getTimeInstance();
				return ((java.sql.Timestamp) (tf.parse(value.toString())));
			} catch (ParseException ex) {
				throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.timefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
			}
		}
		default: {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.timefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.io.InputStream</code> object.
	 * 
	 * A column value can be retrieved as a stream of ASCII characters and then
	 * read in chunks from the stream. This method is particularly suitable for
	 * retrieving large <code>LONGVARCHAR</code> values. The JDBC driver will do
	 * any necessary conversion from the database format into ASCII.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a get method
	 * implicitly closes the stream. . Also, a stream may return <code>0</code>
	 * for <code>CachedRowSetImpl.available()</code> whether there is data
	 * available or not.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of one-byte ASCII characters. If the value is SQL
	 *         <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>CHAR, VARCHAR</code>, <code><b>LONGVARCHAR</b></code>
	 *             <code>BINARY, VARBINARY</code> or <code>LONGVARBINARY</code>
	 *             value. The bold SQL type designates the recommended return
	 *             types that this method is used to retrieve.
	 * @see #getAsciiStream(String)
	 */
	public java.io.InputStream getAsciiStream(int columnIndex) throws SQLException {
		Object value;

		// always free an old stream
		asciiStream = null;

		// sanity check
		checkIndex(columnIndex);
		// make sure the cursor is on a vlid row
		checkCursor();

		value = getCurrentRow().getColumnObject(columnIndex);
		if (value == null) {
			lastValueNull = true;
			return null;
		}

		try {
			if (isString(RowSetMD.getColumnType(columnIndex))) {
				asciiStream = new ByteArrayInputStream(((String) value).getBytes("ASCII"));
			} else {
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
			}
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new SQLException(ex.getMessage());
		}

		return (java.io.InputStream) asciiStream;
	}

	/**
	 * A column value can be retrieved as a stream of Unicode characters and
	 * then read in chunks from the stream. This method is particularly suitable
	 * for retrieving large LONGVARCHAR values. The JDBC driver will do any
	 * necessary conversion from the database format into Unicode.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a get method
	 * implicitly closes the stream. . Also, a stream may return 0 for
	 * available() whether there is data available or not.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of two byte Unicode characters. If the value is SQL NULL
	 *         then the result is null.
	 * @throws SQLException
	 *             if an error occurs
	 * @deprecated
	 */
	public java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException {
		// always free an old stream
		unicodeStream = null;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isBinary(RowSetMD.getColumnType(columnIndex)) == false && isString(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		Object value = getCurrentRow().getColumnObject(columnIndex);
		if (value == null) {
			lastValueNull = true;
			return null;
		}

		unicodeStream = new StringBufferInputStream(value.toString());

		return (java.io.InputStream) unicodeStream;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.io.InputStream</code> object.
	 * <P>
	 * A column value can be retrieved as a stream of uninterpreted bytes and
	 * then read in chunks from the stream. This method is particularly suitable
	 * for retrieving large <code>LONGVARBINARY</code> values.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a get method
	 * implicitly closes the stream. Also, a stream may return <code>0</code>
	 * for <code>CachedRowSetImpl.available()</code> whether there is data
	 * available or not.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of uninterpreted bytes. If the value is SQL
	 *         <code>NULL</code> then the result is <code>null</code>.
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>BINARY, VARBINARY</code> or
	 *             <code><b>LONGVARBINARY</b></code> The bold type indicates the
	 *             SQL type that this method is recommened to retrieve.
	 * @see #getBinaryStream(String)
	 */
	public java.io.InputStream getBinaryStream(int columnIndex) throws SQLException {

		// always free an old stream
		binaryStream = null;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isBinary(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		Object value = getCurrentRow().getColumnObject(columnIndex);
		if (value == null) {
			lastValueNull = true;
			return null;
		}

		binaryStream = new ByteArrayInputStream((byte[]) value);

		return (java.io.InputStream) binaryStream;

	}

	// Methods for accessing results by column name

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>String</code> object.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, SMALLINT, INTEGER
	 * BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, <b>CHAR</b>,
	 * <b>VARCHAR</b></code> or <code>LONGVARCHAR<</code> value. The bold SQL
	 *             type designates the recommended return type.
	 */
	public String getString(String columnName) throws SQLException {
		return getString(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>boolean</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value as a <code>boolean</code> in the Java
	 *         programming language; if the value is SQL <code>NULL</code>, the
	 *         result is <code>false</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>BOOLEAN</code> value
	 * @see #getBoolean(int)
	 */
	public boolean getBoolean(String columnName) throws SQLException {
		return getBoolean(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>byte</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value as a <code>byte</code> in the Java programming
	 *         language; if the value is SQL <code>NULL</code>, the result is
	 *         <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code><B>TINYINT</B>, SMALLINT, INTEGER,
	 * BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold type
	 *             designates the recommended return type
	 */
	public byte getByte(String columnName) throws SQLException {
		return getByte(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>short</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, <b>SMALLINT</b>, INTEGER
	 * BIGINT, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type.
	 * @see #getShort(int)
	 */
	public short getShort(String columnName) throws SQLException {
		return getShort(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * an <code>int</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL
	 *             <code>TINYINT, SMALLINT, <b>INTEGER</b>, BIGINT, REAL
	 * FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR, VARCHAR</code> or
	 *             <code>LONGVARCHAR</code> value. The bold SQL type designates
	 *             the recommended return type.
	 */
	public int getInt(String columnName) throws SQLException {
		return getInt(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>long</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, SMALLINT, INTEGER
	 * <b>BIGINT</b>, REAL, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type.
	 * @see #getLong(int)
	 */
	public long getLong(String columnName) throws SQLException {
		return getLong(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>float</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, SMALLINT, INTEGER
	 * BIGINT, <b>REAL</b>, FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT, CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type.
	 * @see #getFloat(String)
	 */
	public float getFloat(String columnName) throws SQLException {
		return getFloat(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row of
	 * this <code>CachedRowSetImpl</code> object as a <code>double</code> value.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>0</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, SMALLINT, INTEGER
	 * BIGINT, REAL, <b>FLOAT</b>, <b>DOUBLE</b>, DECIMAL, NUMERIC, BIT, CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return types.
	 * @see #getDouble(int)
	 */
	public double getDouble(String columnName) throws SQLException {
		return getDouble(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>java.math.BigDecimal</code> object.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @param scale
	 *            the number of digits to the right of the decimal point
	 * @return a java.math.BugDecimal object with <code><i>scale</i></code>
	 *         number of digits to the right of the decimal point.
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, SMALLINT, INTEGER
	 * BIGINT, REAL, FLOAT, DOUBLE, <b>DECIMAL</b>, <b>NUMERIC</b>, BIT CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type that this method is
	 *             used to retrieve.
	 * @deprecated Use the <code>getBigDecimal(String columnName)</code> method
	 *             instead
	 */
	public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
		return getBigDecimal(getColIdxByName(columnName), scale);
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>byte</code> array. The bytes represent the raw values returned by
	 * the driver.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value as a <code>byte</code> array in the Java
	 *         programming language; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code><b>BINARY</b>, <b>VARBINARY</b>
	 * </code> or <code>LONGVARBINARY</code> values The bold SQL type designates
	 *             the recommended return type.
	 * @see #getBytes(int)
	 */
	public byte[] getBytes(String columnName) throws SQLException {
		return getBytes(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>java.sql.Date</code> object.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>DATE</code> or <code>TIMESTAMP</code>
	 *             value
	 */
	public java.sql.Date getDate(String columnName) throws SQLException {
		return getDate(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>java.sql.Time</code> object.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if the given column name does not match one of this rowset's
	 *             column names or the cursor is not on one of this rowset's
	 *             rows or its insert row
	 */
	public java.sql.Time getTime(String columnName) throws SQLException {
		return getTime(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>java.sql.Timestamp</code> object.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if the given column name does not match one of this rowset's
	 *             column names or the cursor is not on one of this rowset's
	 *             rows or its insert row
	 */
	public java.sql.Timestamp getTimestamp(String columnName) throws SQLException {
		return getTimestamp(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.io.InputStream</code> object.
	 * 
	 * A column value can be retrieved as a stream of ASCII characters and then
	 * read in chunks from the stream. This method is particularly suitable for
	 * retrieving large <code>LONGVARCHAR</code> values. The
	 * <code>SyncProvider</code> will rely on the JDBC driver to do any
	 * necessary conversion from the database format into ASCII format.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a
	 * <code>getXXX</code> method implicitly closes the stream.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of one-byte ASCII characters. If the value is SQL
	 *         <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>CHAR, VARCHAR</code>,
	 *             <code><b>LONGVARCHAR</b></code>
	 *             <code>BINARY, VARBINARY</code> or <code>LONGVARBINARY</code>
	 *             value. The bold SQL type designates the recommended return
	 *             types that this method is used to retrieve.
	 * @see #getAsciiStream(int)
	 */
	public java.io.InputStream getAsciiStream(String columnName) throws SQLException {
		return getAsciiStream(getColIdxByName(columnName));

	}

	/**
	 * A column value can be retrieved as a stream of Unicode characters and
	 * then read in chunks from the stream. This method is particularly suitable
	 * for retrieving large <code>LONGVARCHAR</code> values. The JDBC driver
	 * will do any necessary conversion from the database format into Unicode.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a
	 * <code>getXXX</code> method implicitly closes the stream.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of two-byte Unicode characters. If the value is SQL
	 *         <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException
	 *             if the given column name does not match one of this rowset's
	 *             column names or the cursor is not on one of this rowset's
	 *             rows or its insert row
	 * @deprecated use the method <code>getCharacterStream</code> instead
	 */
	public java.io.InputStream getUnicodeStream(String columnName) throws SQLException {
		return getUnicodeStream(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.io.InputStream</code> object.
	 * <P>
	 * A column value can be retrieved as a stream of uninterpreted bytes and
	 * then read in chunks from the stream. This method is particularly suitable
	 * for retrieving large <code>LONGVARBINARY</code> values.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a get method
	 * implicitly closes the stream. Also, a stream may return <code>0</code>
	 * for <code>CachedRowSetImpl.available()</code> whether there is data
	 * available or not.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of uninterpreted bytes. If the value is SQL
	 *         <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException
	 *             if (1) the given column name is unknown, (2) the cursor is
	 *             not on one of this rowset's rows or its insert row, or (3)
	 *             the designated column does not store an SQL
	 *             <code>BINARY, VARBINARY</code> or
	 *             <code><b>LONGVARBINARY</b></code> The bold type indicates the
	 *             SQL type that this method is recommened to retrieve.
	 * @see #getBinaryStream(int)
	 * 
	 */
	public java.io.InputStream getBinaryStream(String columnName) throws SQLException {
		return getBinaryStream(getColIdxByName(columnName));
	}

	// Advanced features:

	/**
	 * The first warning reported by calls on this <code>CachedRowSetImpl</code>
	 * object is returned. Subsequent <code>CachedRowSetImpl</code> warnings
	 * will be chained to this <code>SQLWarning</code>.
	 * 
	 * <P>
	 * The warning chain is automatically cleared each time a new row is read.
	 * 
	 * <P>
	 * <B>Note:</B> This warning chain only covers warnings caused by
	 * <code>ResultSet</code> methods. Any warning caused by statement methods
	 * (such as reading OUT parameters) will be chained on the
	 * <code>Statement</code> object.
	 * 
	 * @return the first SQLWarning or null
	 */
	public SQLWarning getWarnings() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Clears all the warnings reporeted for the <code>CachedRowSetImpl</code>
	 * object. After a call to this method, the <code>getWarnings</code> method
	 * returns <code>null</code> until a new warning is reported for this
	 * <code>CachedRowSetImpl</code> object.
	 */
	public void clearWarnings() {
	}

	/**
	 * Retrieves the name of the SQL cursor used by this
	 * <code>CachedRowSetImpl</code> object.
	 * 
	 * <P>
	 * In SQL, a result table is retrieved through a cursor that is named. The
	 * current row of a <code>ResultSet</code> can be updated or deleted using a
	 * positioned update/delete statement that references the cursor name. To
	 * ensure that the cursor has the proper isolation level to support an
	 * update operation, the cursor's <code>SELECT</code> statement should be of
	 * the form <code>select for update</code>. If the <code>for update</code>
	 * clause is omitted, positioned updates may fail.
	 * 
	 * <P>
	 * JDBC supports this SQL feature by providing the name of the SQL cursor
	 * used by a <code>ResultSet</code> object. The current row of a result set
	 * is also the current row of this SQL cursor.
	 * 
	 * <P>
	 * <B>Note:</B> If positioned updates are not supported, an
	 * <code>SQLException</code> is thrown.
	 * 
	 * @return the SQL cursor name for this <code>CachedRowSetImpl</code> 
	 *         object's cursor
	 * @throws SQLException
	 *             if an error occurs
	 */
	public String getCursorName() throws SQLException {
		throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.posupdate").toString());
	}

	/**
	 * Retrieves a <code>ResultSetMetaData</code> object instance that contains
	 * information about the <code>CachedRowSet</code> object. However,
	 * applications should cast the returned object to a
	 * <code>RowSetMetaData</code> interface implementation. In the reference
	 * implementation, this cast can be done on the
	 * <code>RowSetMetaDataImpl</code> class.
	 * <P>
	 * For example:
	 * 
	 * <pre>
	 * CachedRowSet crs = new CachedRowSetImpl();
	 * RowSetMetaDataImpl metaData = (RowSetMetaDataImpl) crs.getMetaData();
	 * // Set the number of columns in the RowSet object for
	 * // which this RowSetMetaDataImpl object was created to the
	 * // given number.
	 * metaData.setColumnCount(3);
	 * crs.setMetaData(metaData);
	 * </pre>
	 * 
	 * @return the <code>ResultSetMetaData</code> object that describes this
	 *         <code>CachedRowSetImpl</code> object's columns
	 * @throws SQLException
	 *             if an error occurs in generating the RowSet meta data; or if
	 *             the <code>CachedRowSetImpl</code> is empty.
	 * @see javax.sql.RowSetMetaData
	 */
	public ResultSetMetaData getMetaData() throws SQLException {
		return (ResultSetMetaData) RowSetMD;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as an <code>Object</code> value.
	 * <P>
	 * The type of the <code>Object</code> will be the default Java object type
	 * corresponding to the column's SQL type, following the mapping for
	 * built-in types specified in the JDBC 3.0 specification.
	 * <P>
	 * This method may also be used to read datatabase-specific abstract data
	 * types.
	 * <P>
	 * This implementation of the method <code>getObject</code> extends its
	 * behavior so that it gets the attributes of an SQL structured type as an
	 * array of <code>Object</code> values. This method also custom maps SQL
	 * user-defined types to classes in the Java programming language. When the
	 * specified column contains a structured or distinct value, the behavior of
	 * this method is as if it were a call to the method
	 * <code>getObject(columnIndex,
	 * this.getStatement().getConnection().getTypeMap())</code>.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return a <code>java.lang.Object</code> holding the column value; if the
	 *         value is SQL <code>NULL</code>, the result is <code>null</code>
	 * @throws SQLException
	 *             if the given column index is out of bounds, the cursor is not
	 *             on a valid row, or there is a problem getting the
	 *             <code>Class</code> object for a custom mapping
	 * @see #getObject(String)
	 */
	public Object getObject(int columnIndex) throws SQLException {
		Object value;
		java.util.Map map;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}
		if (value instanceof Struct) {
			Struct s = (Struct) value;
			map = getTypeMap();
			// look up the class in the map
			Class c = (Class) map.get(s.getSQLTypeName());
			if (c != null) {
				// create new instance of the class
				SQLData obj = null;
				try {
					obj = (SQLData) c.newInstance();
				} catch (java.lang.InstantiationException ex) {
					throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.unableins").toString(), ex.getMessage()));
				} catch (java.lang.IllegalAccessException ex) {
					throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.unableins").toString(), ex.getMessage()));
				}
				// get the attributes from the struct
				Object attribs[] = s.getAttributes(map);
				// create the SQLInput "stream"
				SQLInputImpl sqlInput = new SQLInputImpl(attribs, map);
				// read the values...
				obj.readSQL(sqlInput, s.getSQLTypeName());
				return (Object) obj;
			}
		}
		return value;
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as an <code>Object</code> value.
	 * <P>
	 * The type of the <code>Object</code> will be the default Java object type
	 * corresponding to the column's SQL type, following the mapping for
	 * built-in types specified in the JDBC 3.0 specification.
	 * <P>
	 * This method may also be used to read datatabase-specific abstract data
	 * types.
	 * <P>
	 * This implementation of the method <code>getObject</code> extends its
	 * behavior so that it gets the attributes of an SQL structured type as an
	 * array of <code>Object</code> values. This method also custom maps SQL
	 * user-defined types to classes in the Java programming language. When the
	 * specified column contains a structured or distinct value, the behavior of
	 * this method is as if it were a call to the method
	 * <code>getObject(columnIndex,
	 * this.getStatement().getConnection().getTypeMap())</code>.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return a <code>java.lang.Object</code> holding the column value; if the
	 *         value is SQL <code>NULL</code>, the result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name does not match one of this
	 *             rowset's column names, (2) the cursor is not on a valid row,
	 *             or (3) there is a problem getting the <code>Class</code>
	 *             object for a custom mapping
	 * @see #getObject(int)
	 */
	public Object getObject(String columnName) throws SQLException {
		return getObject(getColIdxByName(columnName));
	}

	// ----------------------------------------------------------------

	/**
	 * Maps the given column name for one of this <code>CachedRowSetImpl</code>
	 * object's columns to its column number.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return the column index of the given column name
	 * @throws SQLException
	 *             if the given column name does not match one of this rowset's
	 *             column names
	 */
	public int findColumn(String columnName) throws SQLException {
		return getColIdxByName(columnName);
	}

	// --------------------------JDBC 2.0-----------------------------------

	// ---------------------------------------------------------------------
	// Getter's and Setter's
	// ---------------------------------------------------------------------

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>java.io.Reader</code> object.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a
	 * <code>getXXX</code> method implicitly closes the stream.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return a Java character stream that delivers the database column value
	 *         as a stream of two-byte unicode characters in a
	 *         <code>java.io.Reader</code> object. If the value is SQL
	 *         <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>CHAR, VARCHAR, <b>LONGVARCHAR</b>, BINARY, VARBINARY</code>
	 *             or <code>LONGVARBINARY</code> value. The bold SQL type
	 *             designates the recommended return type.
	 * @see #getCharacterStream(String)
	 */
	public java.io.Reader getCharacterStream(int columnIndex) throws SQLException {

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isBinary(RowSetMD.getColumnType(columnIndex))) {
			Object value = getCurrentRow().getColumnObject(columnIndex);
			if (value == null) {
				lastValueNull = true;
				return null;
			}
			charStream = new InputStreamReader(new ByteArrayInputStream((byte[]) value));
		} else if (isString(RowSetMD.getColumnType(columnIndex))) {
			Object value = getCurrentRow().getColumnObject(columnIndex);
			if (value == null) {
				lastValueNull = true;
				return null;
			}
			charStream = new StringReader(value.toString());
		} else {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		return (java.io.Reader) charStream;
	}

	/**
	 * Retrieves the value stored in the designated column of the current row as
	 * a <code>java.io.Reader</code> object.
	 * 
	 * <P>
	 * <B>Note:</B> All the data in the returned stream must be read prior to
	 * getting the value of any other column. The next call to a
	 * <code>getXXX</code> method implicitly closes the stream.
	 * 
	 * @param columnName
	 *            a <code>String</code> object giving the SQL name of a column
	 *            in this <code>CachedRowSetImpl</code> object
	 * @return a Java input stream that delivers the database column value as a
	 *         stream of two-byte Unicode characters. If the value is SQL
	 *         <code>NULL</code>, the result is <code>null</code>.
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>CHAR, VARCHAR, <b>LONGVARCHAR</b>,
	 * BINARY, VARYBINARY</code> or <code>LONGVARBINARY</code> value. The bold
	 *             SQL type designates the recommended return type.
	 */
	public java.io.Reader getCharacterStream(String columnName) throws SQLException {
		return getCharacterStream(getColIdxByName(columnName));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.math.BigDecimal</code> object.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @return a <code>java.math.BigDecimal</code> value with full precision; if
	 *         the value is SQL <code>NULL</code>, the result is
	 *         <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>TINYINT, SMALLINT, INTEGER, BIGINT, REAL,
	 * FLOAT, DOUBLE, <b>DECIMAL</b>, <b>NUMERIC</b>, BIT, CHAR, VARCHAR</code>
	 *             or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return types that this method is
	 *             used to retrieve.
	 * @see #getBigDecimal(String)
	 */
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}
		try {
			return (new BigDecimal(value.toString().trim()));
		} catch (NumberFormatException ex) {
			throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.doublefail").toString(), new Object[] { value.toString().trim(), columnIndex }));
		}
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a
	 * <code>java.math.BigDecimal</code> object.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return a <code>java.math.BigDecimal</code> value with full precision; if
	 *         the value is SQL <code>NULL</code>, the result is
	 *         <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TINYINT, SMALLINT, INTEGER
	 * BIGINT, REAL, FLOAT, DOUBLE, <b>DECIMAL</b>, <b>NUMERIC</b>, BIT CHAR,
	 * VARCHAR</code> or <code>LONGVARCHAR</code> value. The bold SQL type
	 *             designates the recommended return type that this method is
	 *             used to retrieve
	 * @see #getBigDecimal(int)
	 */
	public BigDecimal getBigDecimal(String columnName) throws SQLException {
		return getBigDecimal(getColIdxByName(columnName));
	}

	// ---------------------------------------------------------------------
	// Traversal/Positioning
	// ---------------------------------------------------------------------

	/**
	 * Returns the number of rows in this <code>CachedRowSetImpl</code> object.
	 * 
	 * @return number of rows in the rowset
	 */
	public int size() {
		return numRows;
	}

	/**
	 * Indicates whether the cursor is before the first row in this
	 * <code>CachedRowSetImpl</code> object.
	 * 
	 * @return <code>true</code> if the cursor is before the first row;
	 *         <code>false</code> otherwise or if the rowset contains no rows
	 * @throws SQLException
	 *             if an error occurs
	 */
	public boolean isBeforeFirst() throws SQLException {
		if (cursorPos == 0 && numRows > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Indicates whether the cursor is after the last row in this
	 * <code>CachedRowSetImpl</code> object.
	 * 
	 * @return <code>true</code> if the cursor is after the last row;
	 *         <code>false</code> otherwise or if the rowset contains no rows
	 * @throws SQLException
	 *             if an error occurs
	 */
	public boolean isAfterLast() throws SQLException {
		if (cursorPos == numRows + 1 && numRows > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Indicates whether the cursor is on the first row in this
	 * <code>CachedRowSetImpl</code> object.
	 * 
	 * @return <code>true</code> if the cursor is on the first row;
	 *         <code>false</code> otherwise or if the rowset contains no rows
	 * @throws SQLException
	 *             if an error occurs
	 */
	public boolean isFirst() throws SQLException {
		return cursorPos == 1;
	}

	/**
	 * Indicates whether the cursor is on the last row in this
	 * <code>CachedRowSetImpl</code> object.
	 * <P>
	 * Note: Calling the method <code>isLast</code> may be expensive because the
	 * JDBC driver might need to fetch ahead one row in order to determine
	 * whether the current row is the last row in this rowset.
	 * 
	 * @return <code>true</code> if the cursor is on the last row;
	 *         <code>false</code> otherwise or if this rowset contains no rows
	 * @throws SQLException
	 *             if an error occurs
	 */
	public boolean isLast() throws SQLException {
		return cursorPos==numRows;
	}

	/**
	 * Moves this <code>CachedRowSetImpl</code> object's cursor to the front of
	 * the rowset, just before the first row. This method has no effect if this
	 * rowset contains no rows.
	 * 
	 * @throws SQLException
	 *             if an error occurs or the type of this rowset is
	 *             <code>ResultSet.TYPE_FORWARD_ONLY</code>
	 */
	public void beforeFirst() throws SQLException {
		cursorPos=0;
	}

	/**
	 * Moves this <code>CachedRowSetImpl</code> object's cursor to the end of
	 * the rowset, just after the last row. This method has no effect if this
	 * rowset contains no rows.
	 * 
	 * @throws SQLException
	 *             if an error occurs
	 */
	public void afterLast() throws SQLException {
		cursorPos=numRows+1;
	}

	/**
	 * Moves this <code>CachedRowSetImpl</code> object's cursor to the first row
	 * and returns <code>true</code> if the operation was successful. This
	 * method also notifies registered listeners that the cursor has moved.
	 * 
	 * @return <code>true</code> if the cursor is on a valid row;
	 *         <code>false</code> otherwise or if there are no rows in this
	 *         <code>CachedRowSetImpl</code> object
	 * @throws SQLException
	 *             if the type of this rowset is
	 *             <code>ResultSet.TYPE_FORWARD_ONLY</code>
	 */
	public boolean first() throws SQLException {
		return numRows>0 && (cursorPos=1)==1;
	}

	/**
	 * Moves this <code>CachedRowSetImpl</code> object's cursor to the last row
	 * and returns <code>true</code> if the operation was successful. This
	 * method also notifies registered listeners that the cursor has moved.
	 * 
	 * @return <code>true</code> if the cursor is on a valid row;
	 *         <code>false</code> otherwise or if there are no rows in this
	 *         <code>CachedRowSetImpl</code> object
	 * @throws SQLException
	 *             if the type of this rowset is
	 *             <code>ResultSet.TYPE_FORWARD_ONLY</code>
	 */
	public boolean last() throws SQLException {
		return numRows>0 && (cursorPos=numRows)==numRows;
	}

	/**
	 * Returns the number of the current row in this
	 * <code>CachedRowSetImpl</code> object. The first row is number 1, the
	 * second number 2, and so on.
	 * 
	 * @return the number of the current row; <code>0</code> if there is no
	 *         current row
	 * @throws SQLException
	 *             if an error occurs; or if the <code>CacheRowSetImpl</code> is
	 *             empty
	 */
	public int getRow() throws SQLException {
		return cursorPos;
	}

	/**
	 * Moves this <code>CachedRowSetImpl</code> object's cursor to the row
	 * number specified.
	 * 
	 * <p>
	 * If the number is positive, the cursor moves to an absolute row with
	 * respect to the beginning of the rowset. The first row is row 1, the
	 * second is row 2, and so on. For example, the following command, in which
	 * <code>crs</code> is a <code>CachedRowSetImpl</code> object, moves the
	 * cursor to the fourth row, starting from the beginning of the rowset.
	 * 
	 * <PRE>
	 * <code>
	 * 
	 *    crs.absolute(4);
	 * 
	 * </code>
	 * </PRE>
	 * <P>
	 * If the number is negative, the cursor moves to an absolute row position
	 * with respect to the end of the rowset. For example, calling
	 * <code>absolute(-1)</code> positions the cursor on the last row,
	 * <code>absolute(-2)</code> moves it on the next-to-last row, and so on. If
	 * the <code>CachedRowSetImpl</code> object <code>crs</code> has five rows,
	 * the following command moves the cursor to the fourth-to-last row, which
	 * in the case of a rowset with five rows, is also the second row, counting
	 * from the beginning.
	 * 
	 * <PRE>
	 * <code>
	 * 
	 *    crs.absolute(-4);
	 * 
	 * </code>
	 * </PRE>
	 * 
	 * If the number specified is larger than the number of rows, the cursor
	 * will move to the position after the last row. If the number specified
	 * would move the cursor one or more rows before the first row, the cursor
	 * moves to the position before the first row.
	 * <P>
	 * Note: Calling <code>absolute(1)</code> is the same as calling the method
	 * <code>first()</code>. Calling <code>absolute(-1)</code> is the same as
	 * calling <code>last()</code>.
	 * 
	 * @param row
	 *            a positive number to indicate the row, starting row numbering
	 *            from the first row, which is <code>1</code>; a negative number
	 *            to indicate the row, starting row numbering from the last row,
	 *            which is <code>-1</code>; it must not be <code>0</code>
	 * @return <code>true</code> if the cursor is on the rowset;
	 *         <code>false</code> otherwise
	 * @throws SQLException
	 *             if the given cursor position is <code>0</code> or the type of
	 *             this rowset is <code>ResultSet.TYPE_FORWARD_ONLY</code>
	 */
	public boolean absolute(int row) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Moves the cursor the specified number of rows from the current position,
	 * with a positive number moving it forward and a negative number moving it
	 * backward.
	 * <P>
	 * If the number is positive, the cursor moves the specified number of rows
	 * toward the end of the rowset, starting at the current row. For example,
	 * the following command, in which <code>crs</code> is a
	 * <code>CachedRowSetImpl</code> object with 100 rows, moves the cursor
	 * forward four rows from the current row. If the current row is 50, the
	 * cursor would move to row 54.
	 * 
	 * <PRE>
	 * <code>
	 * 
	 *    crs.relative(4);
	 * 
	 * </code>
	 * </PRE>
	 * <P>
	 * If the number is negative, the cursor moves back toward the beginning the
	 * specified number of rows, starting at the current row. For example,
	 * calling the method <code>absolute(-1)</code> positions the cursor on the
	 * last row, <code>absolute(-2)</code> moves it on the next-to-last row, and
	 * so on. If the <code>CachedRowSetImpl</code> object <code>crs</code> has
	 * five rows, the following command moves the cursor to the fourth-to-last
	 * row, which in the case of a rowset with five rows, is also the second row
	 * from the beginning.
	 * 
	 * <PRE>
	 * <code>
	 * 
	 *    crs.absolute(-4);
	 * 
	 * </code>
	 * </PRE>
	 * 
	 * If the number specified is larger than the number of rows, the cursor
	 * will move to the position after the last row. If the number specified
	 * would move the cursor one or more rows before the first row, the cursor
	 * moves to the position before the first row. In both cases, this method
	 * throws an <code>SQLException</code>.
	 * <P>
	 * Note: Calling <code>absolute(1)</code> is the same as calling the method
	 * <code>first()</code>. Calling <code>absolute(-1)</code> is the same as
	 * calling <code>last()</code>. Calling <code>relative(0)</code> is valid,
	 * but it does not change the cursor position.
	 * 
	 * @param rows
	 *            an <code>int</code> indicating the number of rows to move the
	 *            cursor, starting at the current row; a positive number moves
	 *            the cursor forward; a negative number moves the cursor
	 *            backward; must not move the cursor past the valid rows
	 * @return <code>true</code> if the cursor is on a row in this
	 *         <code>CachedRowSetImpl</code> object; <code>false</code>
	 *         otherwise
	 * @throws SQLException
	 *             if there are no rows in this rowset, the cursor is positioned
	 *             either before the first row or after the last row, or the
	 *             rowset is type <code>ResultSet.TYPE_FORWARD_ONLY</code>
	 */
	public boolean relative(int rows) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Moves this <code>CachedRowSetImpl</code> object's cursor to the previous
	 * row and returns <code>true</code> if the cursor is on a valid row or
	 * <code>false</code> if it is not. This method also notifies all listeners
	 * registered with this <code>CachedRowSetImpl</code> object that its cursor
	 * has moved.
	 * <P>
	 * Note: calling the method <code>previous()</code> is not the same as
	 * calling the method <code>relative(-1)</code>. This is true because it is
	 * possible to call <code>previous()</code> from the insert row, from after
	 * the last row, or from the current row, whereas <code>relative</code> may
	 * only be called from the current row.
	 * <P>
	 * The method <code>previous</code> may used in a <code>while</code> loop to
	 * iterate through a rowset starting after the last row and moving toward
	 * the beginning. The loop ends when <code>previous</code> returns
	 * <code>false</code>, meaning that there are no more rows. For example, the
	 * following code fragment retrieves all the data in the
	 * <code>CachedRowSetImpl</code> object <code>crs</code>, which has three
	 * columns. Note that the cursor must initially be positioned after the last
	 * row so that the first call to the method <code>previous</code> places the
	 * cursor on the last line.
	 * 
	 * <PRE>
	 * <code>
	 * 
	 *     crs.afterLast();
	 *     while (previous()) {
	 *         String name = crs.getString(1);
	 *         int age = crs.getInt(2);
	 *         short ssn = crs.getShort(3);
	 *         System.out.println(name + "   " + age + "   " + ssn);
	 *     }
	 * 
	 * </code>
	 * </PRE>
	 * 
	 * This method throws an <code>SQLException</code> if the cursor is not on a
	 * row in the rowset, before the first row, or after the last row.
	 * 
	 * @return <code>true</code> if the cursor is on a valid row;
	 *         <code>false</code> if it is before the first row or after the
	 *         last row
	 * @throws SQLException
	 *             if the cursor is not on a valid position or the type of this
	 *             rowset is <code>ResultSet.TYPE_FORWARD_ONLY</code>
	 */
	public boolean previous() throws SQLException {
		if (getType() == ResultSet.TYPE_FORWARD_ONLY) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.last").toString());
		}
		/*
		 * make sure things look sane. The cursor must be positioned in the
		 * rowset or before first (0) or after last (numRows + 1)
		 */
		if (cursorPos < 0 || cursorPos > numRows + 1) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.invalidcp").toString());
		}
		// move and notify
		boolean ret = this.internalPrevious();
		notifyCursorMoved();

		return ret;
	}

	/**
	 * Moves the cursor to the previous row in this
	 * <code>CachedRowSetImpl</code> object, skipping past deleted rows that are
	 * not visible; returns <code>true</code> if the cursor is on a row in this
	 * rowset and <code>false</code> when the cursor goes before the first row.
	 * <P>
	 * This method is called internally by the method <code>previous</code>.
	 * <P>
	 * This is a implementation only method and is not required as a standard
	 * implementation of the <code>CachedRowSet</code> interface.
	 * 
	 * @return <code>true</code> if the cursor is on a row in this rowset;
	 *         <code>false</code> when the cursor reaches the position before
	 *         the first row
	 * @throws SQLException
	 *             if an error occurs
	 */
	protected boolean internalPrevious() throws SQLException {
		boolean ret = false;

		do {
			if (cursorPos > 1) {
				--cursorPos;
				ret = true;
			} else if (cursorPos == 1) {
				// decrement to before first
				--cursorPos;
				ret = false;
				break;
			}
		} while ((getShowDeleted() == false) && (rowDeleted() == true));
		return ret;
	}

	// ---------------------------------------------------------------------
	// Updates
	// ---------------------------------------------------------------------

	/**
	 * Indicates whether the current row of this <code>CachedRowSetImpl</code>
	 * object has been updated. The value returned depends on whether this
	 * rowset can detect updates: <code>false</code> will always be returned if
	 * it does not detect updates.
	 * 
	 * @return <code>true</code> if the row has been visibly updated by the
	 *         owner or another and updates are detected; <code>false</code>
	 *         otherwise
	 * @throws SQLException
	 *             if the cursor is on the insert row or not not on a valid row
	 * 
	 * @see DatabaseMetaData#updatesAreDetected
	 */
	public boolean rowUpdated() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Indicates whether the designated column of the current row of this
	 * <code>CachedRowSetImpl</code> object has been updated. The value returned
	 * depends on whether this rowset can detcted updates: <code>false</code>
	 * will always be returned if it does not detect updates.
	 * 
	 * @param idx
	 *            the index identifier of the column that may be have been
	 *            updated.
	 * @return <code>true</code> is the designated column has been updated and
	 *         the rowset detects updates; <code>false</code> if the rowset has
	 *         not been updated or the rowset does not detect updates
	 * @throws SQLException
	 *             if the cursor is on the insert row or not on a valid row
	 * @see DatabaseMetaData#updatesAreDetected
	 */
	public boolean columnUpdated(int idx) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Indicates whether the designated column of the current row of this
	 * <code>CachedRowSetImpl</code> object has been updated. The value returned
	 * depends on whether this rowset can detcted updates: <code>false</code>
	 * will always be returned if it does not detect updates.
	 * 
	 * @param columnName
	 *            the <code>String</code> column name column that may be have
	 *            been updated.
	 * @return <code>true</code> is the designated column has been updated and
	 *         the rowset detects updates; <code>false</code> if the rowset has
	 *         not been updated or the rowset does not detect updates
	 * @throws SQLException
	 *             if the cursor is on the insert row or not on a valid row
	 * @see DatabaseMetaData#updatesAreDetected
	 */
	public boolean columnUpdated(String columnName) throws SQLException {
		return columnUpdated(getColIdxByName(columnName));
	}

	/**
	 * Indicates whether the current row has been inserted. The value returned
	 * depends on whether or not the rowset can detect visible inserts.
	 * 
	 * @return <code>true</code> if a row has been inserted and inserts are
	 *         detected; <code>false</code> otherwise
	 * @throws SQLException
	 *             if the cursor is on the insert row or not not on a valid row
	 * 
	 * @see DatabaseMetaData#insertsAreDetected
	 */
	public boolean rowInserted() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Indicates whether the current row has been deleted. A deleted row may
	 * leave a visible "hole" in a rowset. This method can be used to detect
	 * such holes if the rowset can detect deletions. This method will always
	 * return <code>false</code> if this rowset cannot detect deletions.
	 * 
	 * @return <code>true</code> if (1)the current row is blank, indicating that
	 *         the row has been deleted, and (2)deletions are detected;
	 *         <code>false</code> otherwise
	 * @throws SQLException
	 *             if the cursor is on a valid row in this rowset
	 * @see DatabaseMetaData#deletesAreDetected
	 */
	public boolean rowDeleted() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Indicates whether the given SQL data type is a numberic type.
	 * 
	 * @param type
	 *            one of the constants from <code>java.sql.Types</code>
	 * @return <code>true</code> if the given type is <code>NUMERIC</code>,'
	 *         <code>DECIMAL</code>, <code>BIT</code>, <code>TINYINT</code>,
	 *         <code>SMALLINT</code>, <code>INTEGER</code>, <code>BIGINT</code>,
	 *         <code>REAL</code>, <code>DOUBLE</code>, or <code>FLOAT</code>;
	 *         <code>false</code> otherwise
	 */
	private boolean isNumeric(int type) {
		switch (type) {
		case java.sql.Types.NUMERIC:
		case java.sql.Types.DECIMAL:
		case java.sql.Types.BIT:
		case java.sql.Types.TINYINT:
		case java.sql.Types.SMALLINT:
		case java.sql.Types.INTEGER:
		case java.sql.Types.BIGINT:
		case java.sql.Types.REAL:
		case java.sql.Types.DOUBLE:
		case java.sql.Types.FLOAT:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Indicates whether the given SQL data type is a string type.
	 * 
	 * @param type
	 *            one of the constants from <code>java.sql.Types</code>
	 * @return <code>true</code> if the given type is <code>CHAR</code>,'
	 *         <code>VARCHAR</code>, or <code>LONGVARCHAR</code>;
	 *         <code>false</code> otherwise
	 */
	private boolean isString(int type) {
		switch (type) {
		case java.sql.Types.CHAR:
		case java.sql.Types.VARCHAR:
		case java.sql.Types.LONGVARCHAR:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Indicates whether the given SQL data type is a binary type.
	 * 
	 * @param type
	 *            one of the constants from <code>java.sql.Types</code>
	 * @return <code>true</code> if the given type is <code>BINARY</code>,'
	 *         <code>VARBINARY</code>, or <code>LONGVARBINARY</code>;
	 *         <code>false</code> otherwise
	 */
	private boolean isBinary(int type) {
		switch (type) {
		case java.sql.Types.BINARY:
		case java.sql.Types.VARBINARY:
		case java.sql.Types.LONGVARBINARY:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Indicates whether the given SQL data type is a temporal type. This method
	 * is called internally by the conversion methods
	 * <code>convertNumeric</code> and <code>convertTemporal</code>.
	 * 
	 * @param type
	 *            one of the constants from <code>java.sql.Types</code>
	 * @return <code>true</code> if the given type is <code>DATE</code>,
	 *         <code>TIME</code>, or <code>TIMESTAMP</code>; <code>false</code>
	 *         otherwise
	 */
	private boolean isTemporal(int type) {
		switch (type) {
		case java.sql.Types.DATE:
		case java.sql.Types.TIME:
		case java.sql.Types.TIMESTAMP:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Indicates whether the given SQL data type is a boolean type. This method
	 * is called internally by the conversion methods
	 * <code>convertNumeric</code> and <code>convertBoolean</code>.
	 * 
	 * @param type
	 *            one of the constants from <code>java.sql.Types</code>
	 * @return <code>true</code> if the given type is <code>BIT</code>, , or
	 *         <code>BOOLEAN</code>; <code>false</code> otherwise
	 */
	private boolean isBoolean(int type) {
		switch (type) {
		case java.sql.Types.BIT:
		case java.sql.Types.BOOLEAN:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Converts the given <code>Object</code> in the Java programming language
	 * to the standard mapping for the specified SQL target data type. The
	 * conversion must be to a string or numeric type, but there are no
	 * restrictions on the type to be converted. If the source type and target
	 * type are the same, the given object is simply returned.
	 * 
	 * @param srcObj
	 *            the <code>Object</code> in the Java programming language that
	 *            is to be converted to the target type
	 * @param srcType
	 *            the data type that is the standard mapping in SQL of the
	 *            object to be converted; must be one of the constants in
	 *            <code>java.sql.Types</code>
	 * @param trgType
	 *            the SQL data type to which to convert the given object; must
	 *            be one of the following constants in
	 *            <code>java.sql.Types</code>: <code>NUMERIC</code>,
	 *            <code>DECIMAL</code>, <code>BIT</code>, <code>TINYINT</code>,
	 *            <code>SMALLINT</code>, <code>INTEGER</code>,
	 *            <code>BIGINT</code>, <code>REAL</code>, <code>DOUBLE</code>,
	 *            <code>FLOAT</code>, <code>VARCHAR</code>,
	 *            <code>LONGVARCHAR</code>, or <code>CHAR</code>
	 * @return an <code>Object</code> value.that is the standard object mapping
	 *         for the target SQL type
	 * @throws SQLException
	 *             if the given target type is not one of the string or numeric
	 *             types in <code>java.sql.Types</code>
	 */
	private Object convertNumeric(Object srcObj, int srcType, int trgType) throws SQLException {

		if (srcType == trgType) {
			return srcObj;
		}

		if (isNumeric(trgType) == false && isString(trgType) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString() + trgType);
		}

		try {
			switch (trgType) {
			case java.sql.Types.BIT:
				Integer i = new Integer(srcObj.toString().trim());
				return i.equals(new Integer((int) 0)) ? new Boolean(false) : new Boolean(true);
			case java.sql.Types.TINYINT:
				return new Byte(srcObj.toString().trim());
			case java.sql.Types.SMALLINT:
				return new Short(srcObj.toString().trim());
			case java.sql.Types.INTEGER:
				return new Integer(srcObj.toString().trim());
			case java.sql.Types.BIGINT:
				return new Long(srcObj.toString().trim());
			case java.sql.Types.NUMERIC:
			case java.sql.Types.DECIMAL:
				return new BigDecimal(srcObj.toString().trim());
			case java.sql.Types.REAL:
			case java.sql.Types.FLOAT:
				return new Float(srcObj.toString().trim());
			case java.sql.Types.DOUBLE:
				return new Double(srcObj.toString().trim());
			case java.sql.Types.CHAR:
			case java.sql.Types.VARCHAR:
			case java.sql.Types.LONGVARCHAR:
				return new String(srcObj.toString());
			default:
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString() + trgType);
			}
		} catch (NumberFormatException ex) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString() + trgType);
		}
	}

	/**
	 * Converts the given <code>Object</code> in the Java programming language
	 * to the standard object mapping for the specified SQL target data type.
	 * The conversion must be to a string or temporal type, and there are also
	 * restrictions on the type to be converted.
	 * <P>
	 * <TABLE
	 * ALIGN="CENTER" BORDER CELLPADDING=10 BORDERCOLOR="#0000FF" <CAPTION
	 * ALIGN="CENTER"><B>Parameters and Return Values</B></CAPTION>
	 * <TR>
	 * <TD><B>Source SQL Type</B>
	 * <TD><B>Target SQL Type</B>
	 * <TD><B>Object Returned</B>
	 * </TR>
	 * <TR>
	 * <TD><code>TIMESTAMP</code>
	 * <TD><code>DATE</code>
	 * <TD><code>java.sql.Date</code>
	 * </TR>
	 * <TR>
	 * <TD><code>TIMESTAMP</code>
	 * <TD><code>TIME</code>
	 * <TD><code>java.sql.Time</code>
	 * </TR>
	 * <TR>
	 * <TD><code>TIME</code>
	 * <TD><code>TIMESTAMP</code>
	 * <TD><code>java.sql.Timestamp</code>
	 * </TR>
	 * <TR>
	 * <TD><code>DATE</code>, <code>TIME</code>, or <code>TIMESTAMP</code>
	 * <TD><code>CHAR</code>, <code>VARCHAR</code>, or <code>LONGVARCHAR</code>
	 * <TD><code>java.lang.String</code>
	 * </TR>
	 * </TABLE>
	 * <P>
	 * If the source type and target type are the same, the given object is
	 * simply returned.
	 * 
	 * @param srcObj
	 *            the <code>Object</code> in the Java programming language that
	 *            is to be converted to the target type
	 * @param srcType
	 *            the data type that is the standard mapping in SQL of the
	 *            object to be converted; must be one of the constants in
	 *            <code>java.sql.Types</code>
	 * @param trgType
	 *            the SQL data type to which to convert the given object; must
	 *            be one of the following constants in
	 *            <code>java.sql.Types</code>: <code>DATE</code>,
	 *            <code>TIME</code>, <code>TIMESTAMP</code>, <code>CHAR</code>,
	 *            <code>VARCHAR</code>, or <code>LONGVARCHAR</code>
	 * @return an <code>Object</code> value.that is the standard object mapping
	 *         for the target SQL type
	 * @throws SQLException
	 *             if the given target type is not one of the string or temporal
	 *             types in <code>java.sql.Types</code>
	 */
	private Object convertTemporal(Object srcObj, int srcType, int trgType) throws SQLException {

		if (srcType == trgType) {
			return srcObj;
		}

		if (isNumeric(trgType) == true || (isString(trgType) == false && isTemporal(trgType) == false)) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		try {
			switch (trgType) {
			case java.sql.Types.DATE:
				if (srcType == java.sql.Types.TIMESTAMP) {
					return new java.sql.Date(((java.sql.Timestamp) srcObj).getTime());
				} else {
					throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
				}
			case java.sql.Types.TIMESTAMP:
				if (srcType == java.sql.Types.TIME) {
					return new Timestamp(((java.sql.Time) srcObj).getTime());
				} else {
					return new Timestamp(((java.sql.Date) srcObj).getTime());
				}
			case java.sql.Types.TIME:
				if (srcType == java.sql.Types.TIMESTAMP) {
					return new Time(((java.sql.Timestamp) srcObj).getTime());
				} else {
					throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
				}
			case java.sql.Types.CHAR:
			case java.sql.Types.VARCHAR:
			case java.sql.Types.LONGVARCHAR:
				return new String(srcObj.toString());
			default:
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
			}
		} catch (NumberFormatException ex) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

	}

	/**
	 * Converts the given <code>Object</code> in the Java programming language
	 * to the standard mapping for the specified SQL target data type. The
	 * conversion must be to a string or numeric type, but there are no
	 * restrictions on the type to be converted. If the source type and target
	 * type are the same, the given object is simply returned.
	 * 
	 * @param srcObj
	 *            the <code>Object</code> in the Java programming language that
	 *            is to be converted to the target type
	 * @param srcType
	 *            the data type that is the standard mapping in SQL of the
	 *            object to be converted; must be one of the constants in
	 *            <code>java.sql.Types</code>
	 * @param trgType
	 *            the SQL data type to which to convert the given object; must
	 *            be one of the following constants in
	 *            <code>java.sql.Types</code>: <code>BIT</code>, or
	 *            <code>BOOLEAN</code>
	 * @return an <code>Object</code> value.that is the standard object mapping
	 *         for the target SQL type
	 * @throws SQLException
	 *             if the given target type is not one of the Boolean types in
	 *             <code>java.sql.Types</code>
	 */
	private Object convertBoolean(Object srcObj, int srcType, int trgType) throws SQLException {

		if (srcType == trgType) {
			return srcObj;
		}

		if (isNumeric(trgType) == true || (isString(trgType) == false && isBoolean(trgType) == false)) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		try {
			switch (trgType) {
			case java.sql.Types.BIT:
				Integer i = new Integer(srcObj.toString().trim());
				return i.equals(new Integer((int) 0)) ? new Boolean(false) : new Boolean(true);
			case java.sql.Types.BOOLEAN:
				return new Boolean(srcObj.toString().trim());
			default:
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString() + trgType);
			}
		} catch (NumberFormatException ex) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString() + trgType);
		}
	}

	/**
	 * Sets the designated nullable column in the current row or the insert row
	 * of this <code>CachedRowSetImpl</code> object with <code>null</code>
	 * value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset; however, another method must be called to complete the
	 * update process. If the cursor is on a row in the rowset, the method
	 * {@link #updateRow} must be called to mark the row as updated and to
	 * notify listeners that the row has changed. If the cursor is on the insert
	 * row, the method {@link #insertRow} must be called to insert the new row
	 * into this rowset and to notify listeners that a row has changed.
	 * <P>
	 * In order to propagate updates in this rowset to the underlying data
	 * source, an application must call the method {@link #acceptChanges} after
	 * it calls either <code>updateRow</code> or <code>insertRow</code>.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateNull(int columnIndex) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Row row = getCurrentRow();
		row.setColumnObject(columnIndex, null);

	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>boolean</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();
		Object obj = convertBoolean(new Boolean(x), java.sql.Types.BIT, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>byte</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateByte(int columnIndex, byte x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertNumeric(new Byte(x), java.sql.Types.TINYINT, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>short</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateShort(int columnIndex, short x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertNumeric(new Short(x), java.sql.Types.SMALLINT, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given <code>int</code>
	 * value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateInt(int columnIndex, int x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();
		Object obj = convertNumeric(new Integer(x), java.sql.Types.INTEGER, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>long</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateLong(int columnIndex, long x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertNumeric(new Long(x), java.sql.Types.BIGINT, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);

	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>float</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateFloat(int columnIndex, float x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertNumeric(new Float(x), java.sql.Types.REAL, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>double</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateDouble(int columnIndex, double x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();
		Object obj = convertNumeric(new Double(x), java.sql.Types.DOUBLE, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.math.BigDecimal</code> object.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertNumeric(x, java.sql.Types.NUMERIC, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>String</code> object.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to mark the row as updated. If the cursor is on the insert row,
	 * the method {@link #insertRow} must be called to insert the new row into
	 * this rowset and mark it as inserted. Both of these methods must be called
	 * before the cursor moves to another row.
	 * <P>
	 * The method <code>acceptChanges</code> must be called if the updated
	 * values are to be written back to the underlying database.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateString(int columnIndex, String x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		getCurrentRow().setColumnObject(columnIndex, x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>byte</code> array.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBytes(int columnIndex, byte x[]) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isBinary(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		getCurrentRow().setColumnObject(columnIndex, x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Date</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             (3) the type of the designated column is not an SQL
	 *             <code>DATE</code> or <code>TIMESTAMP</code>, or (4) this
	 *             rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertTemporal(x, java.sql.Types.DATE, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Time</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             (3) the type of the designated column is not an SQL
	 *             <code>TIME</code> or <code>TIMESTAMP</code>, or (4) this
	 *             rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateTime(int columnIndex, java.sql.Time x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertTemporal(x, java.sql.Types.TIME, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Timestamp</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             (3) the type of the designated column is not an SQL
	 *             <code>DATE</code>, <code>TIME</code>, or
	 *             <code>TIMESTAMP</code>, or (4) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateTimestamp(int columnIndex, java.sql.Timestamp x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		Object obj = convertTemporal(x, java.sql.Types.TIMESTAMP, RowSetMD.getColumnType(columnIndex));

		getCurrentRow().setColumnObject(columnIndex, obj);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given ASCII stream
	 * value.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @param length
	 *            the number of one-byte ASCII characters in the stream
	 * @throws SQLException
	 *             if this method is invoked
	 */
	public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {
		// sanity Check
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isString(RowSetMD.getColumnType(columnIndex)) == false && isBinary(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		byte buf[] = new byte[length];
		try {
			int charsRead = 0;
			do {
				charsRead += x.read(buf, charsRead, length - charsRead);
			} while (charsRead != length);
			// Changed the condition check to check for length instead of -1
		} catch (java.io.IOException ex) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.asciistream").toString());
		}
		String str = new String(buf);

		getCurrentRow().setColumnObject(columnIndex, str);

	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.io.InputStream</code> object.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value; must be a
	 *            <code>java.io.InputStream</code> containing
	 *            <code>BINARY</code>, <code>VARBINARY</code>, or
	 *            <code>LONGVARBINARY</code> data
	 * @param length
	 *            the length of the stream in bytes
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             (3) the data in the stream is not binary, or (4) this rowset
	 *             is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {
		// sanity Check
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isBinary(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		byte buf[] = new byte[length];
		try {
			int bytesRead = 0;
			do {
				bytesRead += x.read(buf, bytesRead, length - bytesRead);
			} while (bytesRead != -1);
		} catch (java.io.IOException ex) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.binstream").toString());
		}

		getCurrentRow().setColumnObject(columnIndex, buf);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.io.Reader</code> object.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value; must be a <code>java.io.Reader</code>
	 *            containing <code>BINARY</code>, <code>VARBINARY</code>,
	 *            <code>LONGVARBINARY</code>, <code>CHAR</code>,
	 *            <code>VARCHAR</code>, or <code>LONGVARCHAR</code> data
	 * @param length
	 *            the length of the stream in characters
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             (3) the data in the stream is not a binary or character type,
	 *             or (4) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws SQLException {
		// sanity Check
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (isString(RowSetMD.getColumnType(columnIndex)) == false && isBinary(RowSetMD.getColumnType(columnIndex)) == false) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		char buf[] = new char[length];
		try {
			int charsRead = 0;
			do {
				charsRead += x.read(buf, charsRead, length - charsRead);
			} while (charsRead != length);
			// Changed the condition checking to check for length instead of -1
		} catch (java.io.IOException ex) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.binstream").toString());
		}
		String str = new String(buf);

		getCurrentRow().setColumnObject(columnIndex, str);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Object</code> value. The <code>scale</code> parameter indicates the
	 * number of digits to the right of the decimal point and is ignored if the
	 * new column value is not a type that will be mapped to an SQL
	 * <code>DECIMAL</code> or <code>NUMERIC</code> value.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @param scale
	 *            the number of digits to the right of the decimal point (for
	 *            <code>DECIMAL</code> and <code>NUMERIC</code> types only)
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		int type = RowSetMD.getColumnType(columnIndex);
		if (type == Types.DECIMAL || type == Types.NUMERIC) {
			((java.math.BigDecimal) x).setScale(scale);
		}
		getCurrentRow().setColumnObject(columnIndex, x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Object</code> value.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateObject(int columnIndex, Object x) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		getCurrentRow().setColumnObject(columnIndex, x);
	}

	/**
	 * Sets the designated nullable column in the current row or the insert row
	 * of this <code>CachedRowSetImpl</code> object with <code>null</code>
	 * value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateNull(String columnName) throws SQLException {
		updateNull(getColIdxByName(columnName));
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>boolean</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBoolean(String columnName, boolean x) throws SQLException {
		updateBoolean(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>byte</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateByte(String columnName, byte x) throws SQLException {
		updateByte(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>short</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateShort(String columnName, short x) throws SQLException {
		updateShort(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given <code>int</code>
	 * value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateInt(String columnName, int x) throws SQLException {
		updateInt(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>long</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateLong(String columnName, long x) throws SQLException {
		updateLong(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>float</code> value.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateFloat(String columnName, float x) throws SQLException {
		updateFloat(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>double</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateDouble(String columnName, double x) throws SQLException {
		updateDouble(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.math.BigDecimal</code> object.
	 * <P>
	 * This method updates a column value in the current row or the insert row
	 * of this rowset, but it does not update the database. If the cursor is on
	 * a row in the rowset, the method {@link #updateRow} must be called to
	 * update the database. If the cursor is on the insert row, the method
	 * {@link #insertRow} must be called, which will insert the new row into
	 * both this rowset and the database. Both of these methods must be called
	 * before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
		updateBigDecimal(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>String</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateString(String columnName, String x) throws SQLException {
		updateString(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>byte</code> array.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBytes(String columnName, byte x[]) throws SQLException {
		updateBytes(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Date</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, (3) the type of the
	 *             designated column is not an SQL <code>DATE</code> or
	 *             <code>TIMESTAMP</code>, or (4) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateDate(String columnName, java.sql.Date x) throws SQLException {
		updateDate(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Time</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, (3) the type of the
	 *             designated column is not an SQL <code>TIME</code> or
	 *             <code>TIMESTAMP</code>, or (4) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateTime(String columnName, java.sql.Time x) throws SQLException {
		updateTime(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Timestamp</code> object.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if the given column index is out of bounds or the cursor is
	 *             not on one of this rowset's rows or its insert row
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, (3) the type of the
	 *             designated column is not an SQL <code>DATE</code>,
	 *             <code>TIME</code>, or <code>TIMESTAMP</code>, or (4) this
	 *             rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateTimestamp(String columnName, java.sql.Timestamp x) throws SQLException {
		updateTimestamp(getColIdxByName(columnName), x);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given ASCII stream
	 * value.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @param length
	 *            the number of one-byte ASCII characters in the stream
	 */
	public void updateAsciiStream(String columnName, java.io.InputStream x, int length) throws SQLException {
		updateAsciiStream(getColIdxByName(columnName), x, length);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.io.InputStream</code> object.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value; must be a
	 *            <code>java.io.InputStream</code> containing
	 *            <code>BINARY</code>, <code>VARBINARY</code>, or
	 *            <code>LONGVARBINARY</code> data
	 * @param length
	 *            the length of the stream in bytes
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, (3) the data in the stream
	 *             is not binary, or (4) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBinaryStream(String columnName, java.io.InputStream x, int length) throws SQLException {
		updateBinaryStream(getColIdxByName(columnName), x, length);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.io.Reader</code> object.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param reader
	 *            the new column value; must be a <code>java.io.Reader</code>
	 *            containing <code>BINARY</code>, <code>VARBINARY</code>,
	 *            <code>LONGVARBINARY</code>, <code>CHAR</code>,
	 *            <code>VARCHAR</code>, or <code>LONGVARCHAR</code> data
	 * @param length
	 *            the length of the stream in characters
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, (3) the data in the stream
	 *             is not a binary or character type, or (4) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateCharacterStream(String columnName, java.io.Reader reader, int length) throws SQLException {
		updateCharacterStream(getColIdxByName(columnName), reader, length);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Object</code> value. The <code>scale</code> parameter indicates the
	 * number of digits to the right of the decimal point and is ignored if the
	 * new column value is not a type that will be mapped to an SQL
	 * <code>DECIMAL</code> or <code>NUMERIC</code> value.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @param scale
	 *            the number of digits to the right of the decimal point (for
	 *            <code>DECIMAL</code> and <code>NUMERIC</code> types only)
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateObject(String columnName, Object x, int scale) throws SQLException {
		updateObject(getColIdxByName(columnName), x, scale);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>Object</code> value.
	 * <P>
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param x
	 *            the new column value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateObject(String columnName, Object x) throws SQLException {
		updateObject(getColIdxByName(columnName), x);
	}

	/**
	 * Inserts the contents of this <code>CachedRowSetImpl</code> object's
	 * insert row into this rowset immediately following the current row. If the
	 * current row is the position after the last row or before the first row,
	 * the new row will be inserted at the end of the rowset. This method also
	 * notifies listeners registered with this rowset that the row has changed.
	 * <P>
	 * The cursor must be on the insert row when this method is called.
	 * 
	 * @throws SQLException
	 *             if (1) the cursor is not on the insert row, (2) one or more
	 *             of the non-nullable columns in the insert row has not been
	 *             given a value, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void insertRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Marks the current row of this <code>CachedRowSetImpl</code> object as
	 * updated and notifies listeners registered with this rowset that the row
	 * has changed.
	 * <P>
	 * This method cannot be called when the cursor is on the insert row, and it
	 * should be called before the cursor moves to another row. If it is called
	 * after the cursor moves to another row, this method has no effect, and the
	 * updates made before the cursor moved will be lost.
	 * 
	 * @throws SQLException
	 *             if the cursor is on the insert row or this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Deletes the current row from this <code>CachedRowSetImpl</code> object
	 * and notifies listeners registered with this rowset that a row has
	 * changed. This method cannot be called when the cursor is on the insert
	 * row.
	 * <P>
	 * This method marks the current row as deleted, but it does not delete the
	 * row from the underlying data source. The method
	 * <code>acceptChanges</code> must be called to delete the row in the data
	 * source.
	 * 
	 * @throws SQLException
	 *             if (1) this method is called when the cursor is on the insert
	 *             row, before the first row, or after the last row or (2) this
	 *             rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void deleteRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the current row with its original value and marks the row as not
	 * updated, thus undoing any changes made to the row since the last call to
	 * the methods <code>updateRow</code> or <code>deleteRow</code>. This method
	 * should be called only when the cursor is on a row in this rowset.
	 * 
	 * @throws SQLException
	 *             if the cursor is on the insert row, before the first row, or
	 *             after the last row
	 */
	public void refreshRow() throws SQLException {
		// make sure we are on a row
		checkCursor();
	}

	/**
	 * Rolls back any updates made to the current row of this
	 * <code>CachedRowSetImpl</code> object and notifies listeners that a row
	 * has changed. To have an effect, this method must be called after an
	 * <code>updateXXX</code> method has been called and before the method
	 * <code>updateRow</code> has been called. If no updates have been made or
	 * the method <code>updateRow</code> has already been called, this method
	 * has no effect.
	 * 
	 * @throws SQLException
	 *             if the cursor is on the insert row, before the first row, or
	 *             after the last row
	 */
	public void cancelRowUpdates() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Moves the cursor for this <code>CachedRowSetImpl</code> object to the
	 * insert row. The current row in the rowset is remembered while the cursor
	 * is on the insert row.
	 * <P>
	 * The insert row is a special row associated with an updatable rowset. It
	 * is essentially a buffer where a new row may be constructed by calling the
	 * appropriate <code>updateXXX</code> methods to assign a value to each
	 * column in the row. A complete row must be constructed; that is, every
	 * column that is not nullable must be assigned a value. In order for the
	 * new row to become part of this rowset, the method <code>insertRow</code>
	 * must be called before the cursor is moved back to the rowset.
	 * <P>
	 * Only certain methods may be invoked while the cursor is on the insert
	 * row; many methods throw an exception if they are called while the cursor
	 * is there. In addition to the <code>updateXXX</code> and
	 * <code>insertRow</code> methods, only the <code>getXXX</code> methods may
	 * be called when the cursor is on the insert row. A <code>getXXX</code>
	 * method should be called on a column only after an <code>updateXXX</code>
	 * method has been called on that column; otherwise, the value returned is
	 * undetermined.
	 * 
	 * @throws SQLException
	 *             if this <code>CachedRowSetImpl</code> object is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void moveToInsertRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Moves the cursor for this <code>CachedRowSetImpl</code> object to the
	 * current row. The current row is the row the cursor was on when the method
	 * <code>moveToInsertRow</code> was called.
	 * <P>
	 * Calling this method has no effect unless it is called while the cursor is
	 * on the insert row.
	 * 
	 * @throws SQLException
	 *             if an error occurs
	 */
	public void moveToCurrentRow() throws SQLException {
	}

	/**
	 * Returns <code>null</code>.
	 * 
	 * @return <code>null</code>
	 * @throws SQLException
	 *             if an error occurs
	 */
	public Statement getStatement() throws SQLException {
		return null;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as an <code>Object</code> in the
	 * Java programming language, using the given <code>java.util.Map</code>
	 * object to custom map the value if appropriate.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param map
	 *            a <code>java.util.Map</code> object showing the mapping from
	 *            SQL type names to classes in the Java programming language
	 * @return an <code>Object</code> representing the SQL value
	 * @throws SQLException
	 *             if the given column index is out of bounds or the cursor is
	 *             not on one of this rowset's rows or its insert row
	 */
	public Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}
		if (value instanceof Struct) {
			Struct s = (Struct) value;

			// look up the class in the map
			Class c = (Class) map.get(s.getSQLTypeName());
			if (c != null) {
				// create new instance of the class
				SQLData obj = null;
				try {
					obj = (SQLData) c.newInstance();
				} catch (java.lang.InstantiationException ex) {
					throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.unableins").toString(), ex.getMessage()));
				} catch (java.lang.IllegalAccessException ex) {
					throw new SQLException(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.unableins").toString(), ex.getMessage()));
				}
				// get the attributes from the struct
				Object attribs[] = s.getAttributes(map);
				// create the SQLInput "stream"
				SQLInputImpl sqlInput = new SQLInputImpl(attribs, map);
				// read the values...
				obj.readSQL(sqlInput, s.getSQLTypeName());
				return (Object) obj;
			}
		}
		return value;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>Ref</code> object in the
	 * Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @return a <code>Ref</code> object representing an SQL<code> REF</code>
	 *         value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>REF</code> value
	 * @see #getRef(String)
	 */
	public Ref getRef(int columnIndex) throws SQLException {
		Ref value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (RowSetMD.getColumnType(columnIndex) != java.sql.Types.REF) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		setLastValueNull(false);
		value = (Ref) (getCurrentRow().getColumnObject(columnIndex));

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		return value;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>Blob</code> object in the
	 * Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @return a <code>Blob</code> object representing an SQL <code>BLOB</code>
	 *         value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>BLOB</code> value
	 * @see #getBlob(String)
	 */
	public Blob getBlob(int columnIndex) throws SQLException {
		Blob value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (RowSetMD.getColumnType(columnIndex) != java.sql.Types.BLOB) {
			System.out.println(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.type").toString(), RowSetMD.getColumnType(columnIndex)));
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		setLastValueNull(false);
		value = (Blob) (getCurrentRow().getColumnObject(columnIndex));

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		return value;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>Clob</code> object in the
	 * Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @return a <code>Clob</code> object representing an SQL <code>CLOB</code>
	 *         value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>CLOB</code> value
	 * @see #getClob(String)
	 */
	public Clob getClob(int columnIndex) throws SQLException {
		Clob value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (RowSetMD.getColumnType(columnIndex) != java.sql.Types.CLOB) {
			System.out.println(MessageFormat.format(resBundle.handleGetObject("cachedrowsetimpl.type").toString(), RowSetMD.getColumnType(columnIndex)));
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		setLastValueNull(false);
		value = (Clob) (getCurrentRow().getColumnObject(columnIndex));

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		return value;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as an <code>Array</code> object in
	 * the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @return an <code>Array</code> object representing an SQL
	 *         <code>ARRAY</code> value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>ARRAY</code> value
	 * @see #getArray(String)
	 */
	public Array getArray(int columnIndex) throws SQLException {
		java.sql.Array value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (RowSetMD.getColumnType(columnIndex) != java.sql.Types.ARRAY) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		setLastValueNull(false);
		value = (java.sql.Array) (getCurrentRow().getColumnObject(columnIndex));

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		return value;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as an <code>Object</code> in the
	 * Java programming language, using the given <code>java.util.Map</code>
	 * object to custom map the value if appropriate.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param map
	 *            a <code>java.util.Map</code> object showing the mapping from
	 *            SQL type names to classes in the Java programming language
	 * @return an <code>Object</code> representing the SQL value
	 * @throws SQLException
	 *             if the given column name is not the name of a column in this
	 *             rowset or the cursor is not on one of this rowset's rows or
	 *             its insert row
	 */
	public Object getObject(String columnName, java.util.Map<String, Class<?>> map) throws SQLException {
		return getObject(getColIdxByName(columnName), map);
	}

	public <T> T getObject(int arg0, Class<T> arg1) throws SQLException {
		return (T) getObject(arg0);
	}

	public <T> T getObject(String arg0, Class<T> arg1) throws SQLException {
		return (T) getObject(arg0);
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>Ref</code> object in the
	 * Java programming language.
	 * 
	 * @param colName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return a <code>Ref</code> object representing an SQL<code> REF</code>
	 *         value
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the column value is not an SQL
	 *             <code>REF</code> value
	 * @see #getRef(int)
	 */
	public Ref getRef(String colName) throws SQLException {
		return getRef(getColIdxByName(colName));
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>Blob</code> object in the
	 * Java programming language.
	 * 
	 * @param colName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return a <code>Blob</code> object representing an SQL <code>BLOB</code>
	 *         value
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>BLOB</code> value
	 * @see #getBlob(int)
	 */
	public Blob getBlob(String colName) throws SQLException {
		return getBlob(getColIdxByName(colName));
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>Clob</code> object in the
	 * Java programming language.
	 * 
	 * @param colName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return a <code>Clob</code> object representing an SQL <code>CLOB</code>
	 *         value
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>CLOB</code> value
	 * @see #getClob(int)
	 */
	public Clob getClob(String colName) throws SQLException {
		return getClob(getColIdxByName(colName));
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as an <code>Array</code> object in
	 * the Java programming langugage.
	 * 
	 * @param colName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @return an <code>Array</code> object representing an SQL
	 *         <code>ARRAY</code> value
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>ARRAY</code> value
	 * @see #getArray(int)
	 */
	public Array getArray(String colName) throws SQLException {
		return getArray(getColIdxByName(colName));
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Date</code>
	 * object, using the given <code>Calendar</code> object to construct an
	 * appropriate millisecond value for the date.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @param cal
	 *            the <code>java.util.Calendar</code> object to use in
	 *            constructing the date
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>DATE</code> or <code>TIMESTAMP</code>
	 *             value
	 */
	public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		value = convertTemporal(value, RowSetMD.getColumnType(columnIndex), java.sql.Types.DATE);

		// create a default calendar
		Calendar defaultCal = Calendar.getInstance();
		// set this Calendar to the time we have
		defaultCal.setTime((java.util.Date) value);

		/*
		 * Now we can pull the pieces of the date out of the default calendar
		 * and put them into the user provided calendar
		 */
		cal.set(Calendar.YEAR, defaultCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, defaultCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, defaultCal.get(Calendar.DAY_OF_MONTH));

		/*
		 * This looks a little odd but it is correct - Calendar.getTime()
		 * returns a Date...
		 */
		return new java.sql.Date(cal.getTime().getTime());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Date</code>
	 * object, using the given <code>Calendar</code> object to construct an
	 * appropriate millisecond value for the date.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param cal
	 *            the <code>java.util.Calendar</code> object to use in
	 *            constructing the date
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>DATE</code> or <code>TIMESTAMP</code>
	 *             value
	 */
	public java.sql.Date getDate(String columnName, Calendar cal) throws SQLException {
		return getDate(getColIdxByName(columnName), cal);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Time</code>
	 * object, using the given <code>Calendar</code> object to construct an
	 * appropriate millisecond value for the date.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @param cal
	 *            the <code>java.util.Calendar</code> object to use in
	 *            constructing the date
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TIME</code> or <code>TIMESTAMP</code>
	 *             value
	 */
	public java.sql.Time getTime(int columnIndex, Calendar cal) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		value = convertTemporal(value, RowSetMD.getColumnType(columnIndex), java.sql.Types.TIME);

		// create a default calendar
		Calendar defaultCal = Calendar.getInstance();
		// set the time in the default calendar
		defaultCal.setTime((java.util.Date) value);

		/*
		 * Now we can pull the pieces of the date out of the default calendar
		 * and put them into the user provided calendar
		 */
		cal.set(Calendar.HOUR_OF_DAY, defaultCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, defaultCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, defaultCal.get(Calendar.SECOND));

		return new java.sql.Time(cal.getTime().getTime());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Time</code>
	 * object, using the given <code>Calendar</code> object to construct an
	 * appropriate millisecond value for the date.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param cal
	 *            the <code>java.util.Calendar</code> object to use in
	 *            constructing the date
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TIME</code> or <code>TIMESTAMP</code>
	 *             value
	 */
	public java.sql.Time getTime(String columnName, Calendar cal) throws SQLException {
		return getTime(getColIdxByName(columnName), cal);
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Timestamp</code>
	 * object, using the given <code>Calendar</code> object to construct an
	 * appropriate millisecond value for the date.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in the rowset
	 * @param cal
	 *            the <code>java.util.Calendar</code> object to use in
	 *            constructing the date
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>TIME</code> or <code>TIMESTAMP</code>
	 *             value
	 */
	public java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		Object value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		setLastValueNull(false);
		value = getCurrentRow().getColumnObject(columnIndex);

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		value = convertTemporal(value, RowSetMD.getColumnType(columnIndex), java.sql.Types.TIMESTAMP);

		// create a default calendar
		Calendar defaultCal = Calendar.getInstance();
		// set the time in the default calendar
		defaultCal.setTime((java.util.Date) value);

		/*
		 * Now we can pull the pieces of the date out of the default calendar
		 * and put them into the user provided calendar
		 */
		cal.set(Calendar.YEAR, defaultCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, defaultCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, defaultCal.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, defaultCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, defaultCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, defaultCal.get(Calendar.SECOND));

		return new java.sql.Timestamp(cal.getTime().getTime());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>CachedRowSetImpl</code> object as a <code>java.sql.Timestamp</code>
	 * object, using the given <code>Calendar</code> object to construct an
	 * appropriate millisecond value for the date.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param cal
	 *            the <code>java.util.Calendar</code> object to use in
	 *            constructing the date
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         result is <code>null</code>
	 * @throws SQLException
	 *             if (1) the given column name is not the name of a column in
	 *             this rowset, (2) the cursor is not on one of this rowset's
	 *             rows or its insert row, or (3) the designated column does not
	 *             store an SQL <code>DATE</code>, <code>TIME</code>, or
	 *             <code>TIMESTAMP</code> value
	 */
	public java.sql.Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
		return getTimestamp(getColIdxByName(columnName), cal);
	}

	/*
	 * RowSetInternal Interface
	 */

	/**
	 * Retrieves the <code>Connection</code> object passed to this
	 * <code>CachedRowSetImpl</code> object. This connection may be used to
	 * populate this rowset with data or to write data back to its underlying
	 * data source.
	 * 
	 * @return the <code>Connection</code> object passed to this rowset; may be
	 *         <code>null</code> if there is no connection
	 * @throws SQLException
	 *             if an error occurs
	 */
	public Connection getConnection() throws SQLException {
		return null;
	}

	/**
	 * Sets the metadata for this <code>CachedRowSetImpl</code> object with the
	 * given <code>RowSetMetaData</code> object.
	 * 
	 * @param md
	 *            a <code>RowSetMetaData</code> object instance containing
	 *            metadata about the columsn in the rowset
	 * @throws SQLException
	 *             if invalid meta data is supplied to the rowset
	 */
	public void setMetaData(RowSetMetaData md) throws SQLException {
		RowSetMD = (RowSetMetaDataImpl) md;
	}

	/**
	 * Returns a result set containing the original value of the rowset. The
	 * original value is the state of the <code>CachedRowSetImpl</code> after
	 * the last population or synchronization (whichever occured most recently)
	 * with the data source.
	 * <p>
	 * The cursor is positioned before the first row in the result set. Only
	 * rows contained in the result set returned by <code>getOriginal()</code>
	 * are said to have an original value.
	 * 
	 * @return the original result set of the rowset
	 * @throws SQLException
	 *             if an error occurs produce the <code>ResultSet</code> object
	 */
	public ResultSet getOriginal() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a result set containing the original value of the current row
	 * only. The original value is the state of the
	 * <code>CachedRowSetImpl</code> after the last population or
	 * synchronization (whichever occured most recently) with the data source.
	 * 
	 * @return the original result set of the row
	 * @throws SQLException
	 *             if there is no current row
	 * @see #setOriginalRow
	 */
	public ResultSet getOriginalRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Marks the current row in this rowset as being an original row.
	 * 
	 * @throws SQLException
	 *             if there is no current row
	 * @see #getOriginalRow
	 */
	public void setOriginalRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns an identifier for the object (table) that was used to create this
	 * rowset.
	 * 
	 * @return a <code>String</code> object that identifies the table from which
	 *         this <code>CachedRowSetImpl</code> object was derived
	 * @throws SQLException
	 *             if an error occurs
	 */
	public String getTableName() throws SQLException {
		return tableName;
	}

	/**
	 * Sets the identifier for the table from which this rowset was derived to
	 * the given table name.
	 * 
	 * @param tabName
	 *            a <code>String</code> object that identifies the table from
	 *            which this <code>CachedRowSetImpl</code> object was derived
	 * @throws SQLException
	 *             if an error occurs
	 */
	public void setTableName(String tabName) throws SQLException {
		if (tabName == null)
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.tablename").toString());
		else
			tableName = new String(tabName);
	}

	/**
	 * Returns the columns that make a key to uniquely identify a row in this
	 * <code>CachedRowSetImpl</code> object.
	 * 
	 * @return an array of column numbers that constitutes a primary key for
	 *         this rowset. This array should be empty if no column is
	 *         representitive of a primary key
	 * @throws SQLException
	 *             if the rowset is empty or no columns are designated as
	 *             primary keys
	 * @see #setKeyColumns
	 */
	public int[] getKeyColumns() throws SQLException {
		return keyCols;
	}

	/**
	 * Sets this <code>CachedRowSetImpl</code> object's <code>keyCols</code>
	 * field with the given array of column numbers, which forms a key for
	 * uniquely identifying a row in this rowset.
	 * 
	 * @param keys
	 *            an array of <code>int</code> indicating the columns that form
	 *            a primary key for this <code>CachedRowSetImpl</code> object;
	 *            every element in the array must be greater than <code>0</code>
	 *            and less than or equal to the number of columns in this rowset
	 * @throws SQLException
	 *             if any of the numbers in the given array is not valid for
	 *             this rowset
	 * @see #getKeyColumns
	 */
	public void setKeyColumns(int[] keys) throws SQLException {
		int numCols = 0;
		if (RowSetMD != null) {
			numCols = RowSetMD.getColumnCount();
			if (keys.length > numCols)
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.keycols").toString());
		}
		keyCols = new int[keys.length];
		for (int i = 0; i < keys.length; i++) {
			if (RowSetMD != null && (keys[i] <= 0 || keys[i] > numCols)) {
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.invalidcol").toString() + keys[i]);
			}
			keyCols[i] = keys[i];
		}
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given <code>Ref</code>
	 * value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param ref
	 *            the new column <code>java.sql.Ref</code> value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateRef(int columnIndex, java.sql.Ref ref) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		// SerialClob will help in getting the byte array and storing it.
		// We need to be checking DatabaseMetaData.locatorsUpdatorCopy()
		// or through RowSetMetaData.locatorsUpdatorCopy()
		getCurrentRow().setColumnObject(columnIndex, new SerialRef(ref));
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>double</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param ref
	 *            the new column <code>java.sql.Ref</code> value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateRef(String columnName, java.sql.Ref ref) throws SQLException {
		updateRef(getColIdxByName(columnName), ref);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>double</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param c
	 *            the new column <code>Clob</code> value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateClob(int columnIndex, Clob c) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		// SerialClob will help in getting the byte array and storing it.
		// We need to be checking DatabaseMetaData.locatorsUpdatorCopy()
		// or through RowSetMetaData.locatorsUpdatorCopy()

		getCurrentRow().setColumnObject(columnIndex, new SerialClob(c));
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>double</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param c
	 *            the new column <code>Clob</code> value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateClob(String columnName, Clob c) throws SQLException {
		updateClob(getColIdxByName(columnName), c);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.sql.Blob</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param b
	 *            the new column <code>Blob</code> value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBlob(int columnIndex, Blob b) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		// SerialBlob will help in getting the byte array and storing it.
		// We need to be checking DatabaseMetaData.locatorsUpdatorCopy()
		// or through RowSetMetaData.locatorsUpdatorCopy()

		getCurrentRow().setColumnObject(columnIndex, new SerialBlob(b));
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.sql.Blob </code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param b
	 *            the new column <code>Blob</code> value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateBlob(String columnName, Blob b) throws SQLException {
		updateBlob(getColIdxByName(columnName), b);
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.sql.Array</code> values.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnIndex
	 *            the first column is <code>1</code>, the second is
	 *            <code>2</code>, and so on; must be <code>1</code> or larger
	 *            and equal to or less than the number of columns in this rowset
	 * @param a
	 *            the new column <code>Array</code> value
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) this rowset is <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateArray(int columnIndex, Array a) throws SQLException {
		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		// SerialArray will help in getting the byte array and storing it.
		// We need to be checking DatabaseMetaData.locatorsUpdatorCopy()
		// or through RowSetMetaData.locatorsUpdatorCopy()
		getCurrentRow().setColumnObject(columnIndex, new SerialArray(a));
	}

	/**
	 * Sets the designated column in either the current row or the insert row of
	 * this <code>CachedRowSetImpl</code> object with the given
	 * <code>java.sql.Array</code> value.
	 * 
	 * This method updates a column value in either the current row or the
	 * insert row of this rowset, but it does not update the database. If the
	 * cursor is on a row in the rowset, the method {@link #updateRow} must be
	 * called to update the database. If the cursor is on the insert row, the
	 * method {@link #insertRow} must be called, which will insert the new row
	 * into both this rowset and the database. Both of these methods must be
	 * called before the cursor moves to another row.
	 * 
	 * @param columnName
	 *            a <code>String</code> object that must match the SQL name of a
	 *            column in this rowset, ignoring case
	 * @param a
	 *            the new column <code>Array</code> value
	 * @throws SQLException
	 *             if (1) the given column name does not match the name of a
	 *             column in this rowset, (2) the cursor is not on one of this
	 *             rowset's rows or its insert row, or (3) this rowset is
	 *             <code>ResultSet.CONCUR_READ_ONLY</code>
	 */
	public void updateArray(String columnName, Array a) throws SQLException {
		updateArray(getColIdxByName(columnName), a);
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>java.net.URL</code>
	 * object in the Java programming language.
	 * 
	 * @return a java.net.URL object containing the resource reference described
	 *         by the URL
	 * @throws SQLException
	 *             if (1) the given column index is out of bounds, (2) the
	 *             cursor is not on one of this rowset's rows or its insert row,
	 *             or (3) the designated column does not store an SQL
	 *             <code>DATALINK</code> value.
	 * @see #getURL(String)
	 */
	public java.net.URL getURL(int columnIndex) throws SQLException {
		// throw new SQLException("Operation not supported");

		java.net.URL value;

		// sanity check.
		checkIndex(columnIndex);
		// make sure the cursor is on a valid row
		checkCursor();

		if (RowSetMD.getColumnType(columnIndex) != java.sql.Types.DATALINK) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.dtypemismt").toString());
		}

		setLastValueNull(false);
		value = (java.net.URL) (getCurrentRow().getColumnObject(columnIndex));

		// check for SQL NULL
		if (value == null) {
			setLastValueNull(true);
			return null;
		}

		return value;
	}

	/**
	 * Retrieves the value of the designated column in this
	 * <code>CachedRowSetImpl</code> object as a <code>java.net.URL</code>
	 * object in the Java programming language.
	 * 
	 * @return a java.net.URL object containing the resource reference described
	 *         by the URL
	 * @throws SQLException
	 *             if (1) the given column name not the name of a column in this
	 *             rowset, or (2) the cursor is not on one of this rowset's rows
	 *             or its insert row, or (3) the designated column does not
	 *             store an SQL <code>DATALINK</code> value.
	 * @see #getURL(int)
	 */
	public java.net.URL getURL(String columnName) throws SQLException {
		return getURL(getColIdxByName(columnName));

	}

	/**
	 * The first warning reported by calls on this <code>CachedRowSetImpl</code>
	 * object is returned. Subsequent <code>CachedRowSetImpl</code> warnings
	 * will be chained to this <code>SQLWarning</code>. All
	 * <code>RowSetWarnings</code> warnings are generated in the disconnected
	 * environment and remain a seperate warning chain to that provided by the
	 * <code>getWarnings</code> method.
	 * 
	 * <P>
	 * The warning chain is automatically cleared each time a new row is read.
	 * 
	 * <P>
	 * <B>Note:</B> This warning chain only covers warnings caused by
	 * <code>CachedRowSet</code> (and their child interface) methods. All
	 * <code>SQLWarnings</code> can be obtained using the
	 * <code>getWarnings</code> method which tracks warnings generated by the
	 * underlying JDBC driver.
	 * 
	 * @return the first SQLWarning or null
	 * 
	 */
	public RowSetWarning getRowSetWarnings() {
		return null;
	}


	/**
	 * Unsets the designated parameter to the given int array. This was set
	 * using <code>setMatchColumn</code> as the column which will form the basis
	 * of the join.
	 * <P>
	 * The parameter value unset by this method should be same as was set.
	 * 
	 * @param columnIdxes
	 *            the index into this rowset object's internal representation of
	 *            parameter values
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds or
	 *             if the columnIdx is not the same as set using
	 *             <code>setMatchColumn(int [])</code>
	 */
	public void unsetMatchColumn(int[] columnIdxes) throws SQLException {

		int i_val;
		for (int j = 0; j < columnIdxes.length; j++) {
			i_val = (Integer.parseInt(iMatchColumns.get(j).toString()));
			if (columnIdxes[j] != i_val) {
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.matchcols").toString());
			}
		}

		for (int i = 0; i < columnIdxes.length; i++) {
			iMatchColumns.set(i, new Integer(-1));
		}
	}

	/**
	 * Unsets the designated parameter to the given String array. This was set
	 * using <code>setMatchColumn</code> as the column which will form the basis
	 * of the join.
	 * <P>
	 * The parameter value unset by this method should be same as was set.
	 * 
	 * @param columnIdxes
	 *            the index into this rowset object's internal representation of
	 *            parameter values
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds or
	 *             if the columnName is not the same as set using
	 *             <code>setMatchColumn(String [])</code>
	 */
	public void unsetMatchColumn(String[] columnIdxes) throws SQLException {

		for (int j = 0; j < columnIdxes.length; j++) {
			if (!columnIdxes[j].equals(strMatchColumns.get(j))) {
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.matchcols").toString());
			}
		}

		for (int i = 0; i < columnIdxes.length; i++) {
			strMatchColumns.set(i, null);
		}
	}

	/**
	 * Sets the designated parameter to the given int array. This forms the
	 * basis of the join for the <code>JoinRowSet</code> as the column which
	 * will form the basis of the join.
	 * <P>
	 * The parameter value set by this method is stored internally and will be
	 * supplied as the appropriate parameter in this rowset's command when the
	 * method <code>getMatchColumnIndexes</code> is called.
	 * 
	 * @param columnIdxes
	 *            the indexes into this rowset object's internal representation
	 *            of parameter values; the first parameter is 0, the second is
	 *            1, and so on; must be <code>0</code> or greater
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds
	 */
	public void setMatchColumn(int[] columnIdxes) throws SQLException {

		for (int j = 0; j < columnIdxes.length; j++) {
			if (columnIdxes[j] < 0) {
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.matchcols1").toString());
			}
		}
		for (int i = 0; i < columnIdxes.length; i++) {
			iMatchColumns.add(i, new Integer(columnIdxes[i]));
		}
	}

	/**
	 * Sets the designated parameter to the given String array. This forms the
	 * basis of the join for the <code>JoinRowSet</code> as the column which
	 * will form the basis of the join.
	 * <P>
	 * The parameter value set by this method is stored internally and will be
	 * supplied as the appropriate parameter in this rowset's command when the
	 * method <code>getMatchColumn</code> is called.
	 * 
	 * @param columnNames
	 *            the name of the column into this rowset object's internal
	 *            representation of parameter values
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds
	 */
	public void setMatchColumn(String[] columnNames) throws SQLException {

		for (int j = 0; j < columnNames.length; j++) {
			if (columnNames[j] == null || columnNames[j].equals("")) {
				throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.matchcols2").toString());
			}
		}
		for (int i = 0; i < columnNames.length; i++) {
			strMatchColumns.add(i, columnNames[i]);
		}
	}

	/**
	 * Sets the designated parameter to the given <code>int</code> object. This
	 * forms the basis of the join for the <code>JoinRowSet</code> as the column
	 * which will form the basis of the join.
	 * <P>
	 * The parameter value set by this method is stored internally and will be
	 * supplied as the appropriate parameter in this rowset's command when the
	 * method <code>getMatchColumn</code> is called.
	 * 
	 * @param columnIdx
	 *            the index into this rowset object's internal representation of
	 *            parameter values; the first parameter is 0, the second is 1,
	 *            and so on; must be <code>0</code> or greater
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds
	 */
	public void setMatchColumn(int columnIdx) throws SQLException {
		// validate, if col is ok to be set
		if (columnIdx < 0) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.matchcols1").toString());
		} else {
			// set iMatchColumn
			iMatchColumns.set(0, new Integer(columnIdx));
			// strMatchColumn = null;
		}
	}

	/**
	 * Sets the designated parameter to the given <code>String</code> object.
	 * This forms the basis of the join for the <code>JoinRowSet</code> as the
	 * column which will form the basis of the join.
	 * <P>
	 * The parameter value set by this method is stored internally and will be
	 * supplied as the appropriate parameter in this rowset's command when the
	 * method <code>getMatchColumn</code> is called.
	 * 
	 * @param columnName
	 *            the name of the column into this rowset object's internal
	 *            representation of parameter values
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds
	 */
	public void setMatchColumn(String columnName) throws SQLException {
		// validate, if col is ok to be set
		if (columnName.equals(null) || ((columnName = columnName.trim()) == "")) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.matchcols2").toString());
		} else {
			// set strMatchColumn
			strMatchColumns.set(0, columnName);
			// iMatchColumn = -1;
		}
	}

	/**
	 * Unsets the designated parameter to the given <code>int</code> object.
	 * This was set using <code>setMatchColumn</code> as the column which will
	 * form the basis of the join.
	 * <P>
	 * The parameter value unset by this method should be same as was set.
	 * 
	 * @param columnIdx
	 *            the index into this rowset object's internal representation of
	 *            parameter values
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds or
	 *             if the columnIdx is not the same as set using
	 *             <code>setMatchColumn(int)</code>
	 */
	public void unsetMatchColumn(int columnIdx) throws SQLException {
		// check if we are unsetting the SAME column
		if (!iMatchColumns.get(0).equals(new Integer(columnIdx))) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.unsetmatch").toString());
		} else if (strMatchColumns.get(0) != null) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.unsetmatch1").toString());
		} else {
			// that is, we are unsetting it.
			iMatchColumns.set(0, new Integer(-1));
		}
	}

	/**
	 * Unsets the designated parameter to the given <code>String</code> object.
	 * This was set using <code>setMatchColumn</code> as the column which will
	 * form the basis of the join.
	 * <P>
	 * The parameter value unset by this method should be same as was set.
	 * 
	 * @param columnName
	 *            the index into this rowset object's internal representation of
	 *            parameter values
	 * @throws SQLException
	 *             if an error occurs or the parameter index is out of bounds or
	 *             if the columnName is not the same as set using
	 *             <code>setMatchColumn(String)</code>
	 */
	public void unsetMatchColumn(String columnName) throws SQLException {
		// check if we are unsetting the same column
		columnName = columnName.trim();

		if (!((strMatchColumns.get(0)).equals(columnName))) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.unsetmatch").toString());
		} else if (((Integer) (iMatchColumns.get(0))).intValue() > 0) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.unsetmatch2").toString());
		} else {
			strMatchColumns.set(0, null); // that is, we are unsetting it.
		}
	}

	/**
	 * Notifies registered listeners that a RowSet object in the given
	 * RowSetEvent object has populated a number of additional rows. The
	 * <code>numRows</code> parameter ensures that this event will only be fired
	 * every <code>numRow</code>.
	 * <p>
	 * The source of the event can be retrieved with the method event.getSource.
	 * 
	 * @param event
	 *            a <code>RowSetEvent</code> object that contains the
	 *            <code>RowSet</code> object that is the source of the events
	 * @param numRows
	 *            when populating, the number of rows interval on which the
	 *            <code>CachedRowSet</code> populated should fire; the default
	 *            value is zero; cannot be less than <code>fetchSize</code> or
	 *            zero
	 */
	public void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException {

		if (numRows < 0 || numRows < getFetchSize()) {
			throw new SQLException(resBundle.handleGetObject("cachedrowsetimpl.numrows").toString());
		}

		if (size() % numRows == 0) {
			RowSetEvent event_temp = new RowSetEvent(this);
			event = event_temp;
			notifyRowSetChanged();
		}
	}

	/**
	 * Retrieves the value of the designated <code>SQL XML</code> parameter as a
	 * <code>SQLXML</code> object in the Java programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return a SQLXML object that maps an SQL XML value
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated <code>SQL XML</code> parameter as a
	 * <code>SQLXML</code> object in the Java programming language.
	 * 
	 * @param colName
	 *            the name of the column from which to retrieve the value
	 * @return a SQLXML object that maps an SQL XML value
	 * @throws SQLException
	 *             if a database access error occurs
	 */
	public SQLXML getSQLXML(String colName) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a java.sql.RowId object in the Java
	 * programming language.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @return the column value if the value is a SQL <code>NULL</code> the
	 *         value returned is <code>null</code>
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public RowId getRowId(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a java.sql.RowId object in the Java
	 * programming language.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @return the column value if the value is a SQL <code>NULL</code> the
	 *         value returned is <code>null</code>
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public RowId getRowId(String columnName) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a <code>RowId</code> value. The
	 * updater methods are used to update column values in the current row or
	 * the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow<code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @param x
	 *            the column value
	 * @throws SQLException
	 *             if a database access occurs
	 * @since 6.0
	 */
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a <code>RowId</code> value. The
	 * updater methods are used to update column values in the current row or
	 * the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow<code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the column value
	 * @throws SQLException
	 *             if a database access occurs
	 * @since 6.0
	 */
	public void updateRowId(String columnName, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the holdability of this ResultSet object
	 * 
	 * @return either ResultSet.HOLD_CURSORS_OVER_COMMIT or
	 *         ResultSet.CLOSE_CURSORS_AT_COMMIT
	 * @throws SQLException
	 *             if a database error occurs
	 * @since 6.0
	 */
	public int getHoldability() throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves whether this ResultSet object has been closed. A ResultSet is
	 * closed if the method close has been called on it, or if it is
	 * automatically closed.
	 * 
	 * @return true if this ResultSet object is closed; false if it is still
	 *         open
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public boolean isClosed() throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * This method is used for updating columns that support National Character
	 * sets. It can be used for updating NCHAR,NVARCHAR and LONGNVARCHAR
	 * columns.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @param nString
	 *            the value for the column to be updated
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public void updateNString(int columnIndex, String nString) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * This method is used for updating columns that support National Character
	 * sets. It can be used for updating NCHAR,NVARCHAR and LONGNVARCHAR
	 * columns.
	 * 
	 * @param columnName
	 *            name of the Column
	 * @param nString
	 *            the value for the column to be updated
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public void updateNString(String columnName, String nString) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * This method is used for updating SQL <code>NCLOB</code> type that maps to
	 * <code>java.sql.Types.NCLOB</code>
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @param nClob
	 *            the value for the column to be updated
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * This method is used for updating SQL <code>NCLOB</code> type that maps to
	 * <code>java.sql.Types.NCLOB</code>
	 * 
	 * @param columnName
	 *            name of the column
	 * @param nClob
	 *            the value for the column to be updated
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 6.0
	 */
	public void updateNClob(String columnName, NClob nClob) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a <code>NClob</code> object in the Java
	 * programming language.
	 * 
	 * @param i
	 *            the first column is 1, the second is 2, ...
	 * @return a <code>NClob</code> object representing the SQL
	 *         <code>NCLOB</code> value in the specified column
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 6.0
	 */
	public NClob getNClob(int i) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a <code>NClob</code> object in the Java
	 * programming language.
	 * 
	 * @param colName
	 *            the name of the column from which to retrieve the value
	 * @return a <code>NClob</code> object representing the SQL
	 *         <code>NCLOB</code> value in the specified column
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 6.0
	 */
	public NClob getNClob(String colName) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
		return null;
	}

	public boolean isWrapperFor(Class<?> interfaces) throws SQLException {
		return false;
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.SQLXML</code>
	 * object. The driver converts this to an SQL <code>XML</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param xmlObject
	 *            a <code>SQLXML</code> object that maps an SQL <code>XML</code>
	 *            value
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 1.6
	 */
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.SQLXML</code>
	 * object. The driver converts this to an <code>SQL XML</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param xmlObject
	 *            a <code>SQLXML</code> object that maps an <code>SQL XML</code>
	 *            value
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 1.6
	 */
	public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.RowId</code>
	 * object. The driver converts this to a SQL <code>ROWID</code> value when
	 * it sends it to the database
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, ...
	 * @param x
	 *            the parameter value
	 * @throws SQLException
	 *             if a database access error occurs
	 * 
	 * @since 1.6
	 */
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.RowId</code>
	 * object. The driver converts this to a SQL <code>ROWID</code> when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 1.6
	 */
	public void setRowId(String parameterName, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to the
	 * national character set in the database.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>setNCharacterStream</code> which takes a length parameter.
	 * 
	 * @param parameterIndex
	 *            of the first parameter is 1, the second is 2, ...
	 * @param value
	 *            the parameter value
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur ; if a database access error occurs; or this method is
	 *             called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>java.sql.NClob</code> object.
	 * The object implements the <code>java.sql.NClob</code> interface. This
	 * <code>NClob</code> object maps to a SQL <code>NCLOB</code>.
	 * 
	 * @param parameterName
	 *            the name of the column to be set
	 * @param value
	 *            the parameter value
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; or if a database access error occurs
	 * @since 1.6
	 */
	public void setNClob(String parameterName, NClob value) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a <code>java.io.Reader</code> object. It
	 * is intended for use when accessing <code>NCHAR</code>,
	 * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
	 * 
	 * @return a <code>java.io.Reader</code> object that contains the column
	 *         value; if the value is SQL <code>NULL</code>, the value returned
	 *         is <code>null</code> in the Java programming language.
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 1.6
	 */
	public java.io.Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a <code>java.io.Reader</code> object. It
	 * is intended for use when accessing <code>NCHAR</code>,
	 * <code>NVARCHAR</code> and <code>LONGNVARCHAR</code> columns.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @return a <code>java.io.Reader</code> object that contains the column
	 *         value; if the value is SQL <code>NULL</code>, the value returned
	 *         is <code>null</code> in the Java programming language
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 1.6
	 */
	public java.io.Reader getNCharacterStream(String columnName) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a <code>java.sql.SQLXML</code> value.
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @param xmlObject
	 *            the value for the column to be updated
	 * @throws SQLException
	 *             if a database access error occurs
	 * @since 1.6
	 */
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a <code>java.sql.SQLXML</code> value.
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param xmlObject
	 *            the column value
	 * @throws SQLException
	 *             if a database access occurs
	 * @since 1.6
	 */
	public void updateSQLXML(String columnName, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a <code>String</code> in the Java
	 * programming language. It is intended for use when accessing
	 * <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code>
	 * columns.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         value returned is <code>null</code>
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 1.6
	 */
	public String getNString(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Retrieves the value of the designated column in the current row of this
	 * <code>ResultSet</code> object as a <code>String</code> in the Java
	 * programming language. It is intended for use when accessing
	 * <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code>
	 * columns.
	 * 
	 * @param columnName
	 *            the SQL name of the column
	 * @return the column value; if the value is SQL <code>NULL</code>, the
	 *         value returned is <code>null</code>
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 1.6
	 */
	public String getNString(String columnName) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The driver does the necessary
	 * conversion from Java character format to the national character set in
	 * the database. It is intended for use when updating NCHAR,NVARCHAR and
	 * LONGNVARCHAR columns. The updater methods are used to update column
	 * values in the current row or the insert row. The updater methods do not
	 * update the underlying database; instead the updateRow or insertRow
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            - the first column is 1, the second is 2, ...
	 * @param x
	 *            - the new column value
	 * @param length
	 *            - the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 1.6
	 */
	public void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The driver does the necessary
	 * conversion from Java character format to the national character set in
	 * the database. It is intended for use when updating NCHAR,NVARCHAR and
	 * LONGNVARCHAR columns. The updater methods are used to update column
	 * values in the current row or the insert row. The updater methods do not
	 * update the underlying database; instead the updateRow or insertRow
	 * methods are called to update the database.
	 * 
	 * @param columnName
	 *            - name of the Column
	 * @param x
	 *            - the new column value
	 * @param length
	 *            - the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs
	 * @since 1.6
	 */
	public void updateNCharacterStream(String columnName, java.io.Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.opnotysupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value. The driver
	 * does the necessary conversion from Java character format to the national
	 * character set in the database. It is intended for use when updating
	 * <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code>
	 * columns.
	 * 
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateNCharacterStream</code> which takes a length parameter.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateNCharacterStream(int columnIndex, java.io.Reader x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value. The driver
	 * does the necessary conversion from Java character format to the national
	 * character set in the database. It is intended for use when updating
	 * <code>NCHAR</code>,<code>NVARCHAR</code> and <code>LONGNVARCHAR</code>
	 * columns.
	 * 
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateNCharacterStream</code> which takes a length parameter.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param reader
	 *            the <code>java.io.Reader</code> object containing the new
	 *            column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateNCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	// ////////////////////////

	/**
	 * Updates the designated column using the given input stream, which will
	 * have the specified number of bytes. When a very large ASCII value is
	 * input to a <code>LONGVARCHAR</code> parameter, it may be more practical
	 * to send it via a <code>java.io.InputStream</code>. Data will be read from
	 * the stream as needed until end-of-file is reached. The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of bytes in the parameter data.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given input stream, which will
	 * have the specified number of bytes. When a very large ASCII value is
	 * input to a <code>LONGVARCHAR</code> parameter, it may be more practical
	 * to send it via a <code>java.io.InputStream</code>. Data will be read from
	 * the stream as needed until end-of-file is reached. The JDBC driver will
	 * do any necessary conversion from ASCII to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of bytes in the parameter data.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given input stream. When a very
	 * large ASCII value is input to a <code>LONGVARCHAR</code> parameter, it
	 * may be more practical to send it via a <code>java.io.InputStream</code>.
	 * Data will be read from the stream as needed until end-of-file is reached.
	 * The JDBC driver will do any necessary conversion from ASCII to the
	 * database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>updateBlob</code> which
	 * takes a length parameter.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given input stream. When a very
	 * large ASCII value is input to a <code>LONGVARCHAR</code> parameter, it
	 * may be more practical to send it via a <code>java.io.InputStream</code>.
	 * Data will be read from the stream as needed until end-of-file is reached.
	 * The JDBC driver will do any necessary conversion from ASCII to the
	 * database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>updateBlob</code> which
	 * takes a length parameter.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object,
	 * which is the given number of characters long. When a very large UNICODE
	 * value is input to a <code>LONGVARCHAR</code> parameter, it may be more
	 * practical to send it via a <code>java.io.Reader</code> object. The data
	 * will be read from the stream as needed until end-of-file is reached. The
	 * JDBC driver will do any necessary conversion from UNICODE to the database
	 * char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object,
	 * which is the given number of characters long. When a very large UNICODE
	 * value is input to a <code>LONGVARCHAR</code> parameter, it may be more
	 * practical to send it via a <code>java.io.Reader</code> object. The data
	 * will be read from the stream as needed until end-of-file is reached. The
	 * JDBC driver will do any necessary conversion from UNICODE to the database
	 * char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached. The JDBC driver will do any
	 * necessary conversion from UNICODE to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>updateClob</code> which
	 * takes a length parameter.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached. The JDBC driver will do any
	 * necessary conversion from UNICODE to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>updateClob</code> which
	 * takes a length parameter.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object,
	 * which is the given number of characters long. When a very large UNICODE
	 * value is input to a <code>LONGVARCHAR</code> parameter, it may be more
	 * practical to send it via a <code>java.io.Reader</code> object. The data
	 * will be read from the stream as needed until end-of-file is reached. The
	 * JDBC driver will do any necessary conversion from UNICODE to the database
	 * char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; this method is called on a closed result set, if a
	 *             database access error occurs or the result set concurrency is
	 *             <code>CONCUR_READ_ONLY</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object,
	 * which is the given number of characters long. When a very large UNICODE
	 * value is input to a <code>LONGVARCHAR</code> parameter, it may be more
	 * practical to send it via a <code>java.io.Reader</code> object. The data
	 * will be read from the stream as needed until end-of-file is reached. The
	 * JDBC driver will do any necessary conversion from UNICODE to the database
	 * char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; this method is called on a closed result set; if a
	 *             database access error occurs or the result set concurrency is
	 *             <code>CONCUR_READ_ONLY</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached. The JDBC driver will do any
	 * necessary conversion from UNICODE to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>updateNClob</code>
	 * which takes a length parameter.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; this method is called on a closed result set, if a
	 *             database access error occurs or the result set concurrency is
	 *             <code>CONCUR_READ_ONLY</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column using the given <code>Reader</code> object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached. The JDBC driver will do any
	 * necessary conversion from UNICODE to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>updateNClob</code>
	 * which takes a length parameter.
	 * <p>
	 * The updater methods are used to update column values in the current row
	 * or the insert row. The updater methods do not update the underlying
	 * database; instead the <code>updateRow</code> or <code>insertRow</code>
	 * methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; this method is called on a closed result set; if a
	 *             database access error occurs or the result set concurrency is
	 *             <code>CONCUR_READ_ONLY</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with an ascii stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the <code>updateRow</code>
	 * or <code>insertRow</code> methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());

	}

	/**
	 * Updates the designated column with a binary stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the <code>updateRow</code>
	 * or <code>insertRow</code> methods are called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The updater methods are used to
	 * update column values in the current row or the insert row. The updater
	 * methods do not update the underlying database; instead the
	 * <code>updateRow</code> or <code>insertRow</code> methods are called to
	 * update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateCharacterStream(int columnIndex, java.io.Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value, which will
	 * have the specified number of bytes. The updater methods are used to
	 * update column values in the current row or the insert row. The updater
	 * methods do not update the underlying database; instead the
	 * <code>updateRow</code> or <code>insertRow</code> methods are called to
	 * update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param reader
	 *            the <code>java.io.Reader</code> object containing the new
	 *            column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateCharacterStream(String columnLabel, java.io.Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with an ascii stream value, which will have
	 * the specified number of bytes.. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the <code>updateRow</code>
	 * or <code>insertRow</code> methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateAsciiStream(String columnLabel, java.io.InputStream x, long length) throws SQLException {
	}

	/**
	 * Updates the designated column with a binary stream value, which will have
	 * the specified number of bytes. The updater methods are used to update
	 * column values in the current row or the insert row. The updater methods
	 * do not update the underlying database; instead the <code>updateRow</code>
	 * or <code>insertRow</code> methods are called to update the database.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the label is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBinaryStream(String columnLabel, java.io.InputStream x, long length) throws SQLException {
	}

	/**
	 * Updates the designated column with a binary stream value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the <code>updateRow</code> or <code>insertRow</code> methods are called
	 * to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateBinaryStream</code> which takes a length parameter.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBinaryStream(int columnIndex, java.io.InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with a binary stream value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the <code>updateRow</code> or <code>insertRow</code> methods are called
	 * to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateBinaryStream</code> which takes a length parameter.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateBinaryStream(String columnLabel, java.io.InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the <code>updateRow</code> or <code>insertRow</code> methods are called
	 * to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateCharacterStream</code> which takes a length parameter.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateCharacterStream(int columnIndex, java.io.Reader x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with a character stream value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the <code>updateRow</code> or <code>insertRow</code> methods are called
	 * to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateCharacterStream</code> which takes a length parameter.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param reader
	 *            the <code>java.io.Reader</code> object containing the new
	 *            column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with an ascii stream value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the <code>updateRow</code> or <code>insertRow</code> methods are called
	 * to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateAsciiStream</code> which takes a length parameter.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateAsciiStream(int columnIndex, java.io.InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Updates the designated column with an ascii stream value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the <code>updateRow</code> or <code>insertRow</code> methods are called
	 * to update the database.
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>updateAsciiStream</code> which takes a length parameter.
	 * 
	 * @param columnLabel
	 *            the label for the column specified with the SQL AS clause. If
	 *            the SQL AS clause was not specified, then the la bel is the
	 *            name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database access error occurs, the result set
	 *                concurrency is <code>CONCUR_READ_ONLY</code> or this
	 *                method is called on a closed result set
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void updateAsciiStream(String columnLabel, java.io.InputStream x) throws SQLException {

	}

	/**
	 * Sets the designated parameter to the given <code>java.net.URL</code>
	 * value. The driver converts this to an SQL <code>DATALINK</code> value
	 * when it sends it to the database.
	 * 
	 * @param parameterIndex
	 *            the first parameter is 1, the second is 2, ...
	 * @param x
	 *            the <code>java.net.URL</code> object to be set
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setURL(int parameterIndex, java.net.URL x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. This
	 * method differs from the <code>setCharacterStream (int, Reader)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>NCLOB</code>. When the
	 * <code>setCharacterStream</code> method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be sent to the
	 * server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setNClob</code> which
	 * takes a length parameter.
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if parameterIndex does not correspond to a parameter marker
	 *             in the SQL statement; if the driver does not support national
	 *             character sets; if the driver can detect that a data
	 *             conversion error could occur; if a database access error
	 *             occurs or this method is called on a closed
	 *             <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>reader</code> must contain the number of characters specified by
	 * length otherwise a <code>SQLException</code> will be generated when the
	 * <code>CallableStatement</code> is executed. This method differs from the
	 * <code>setCharacterStream (int, Reader, int)</code> method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a <code>NCLOB</code>. When the <code>setCharacterStream</code> method
	 * is used, the driver may have to do extra work to determine whether the
	 * parameter data should be send to the server as a
	 * <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 * 
	 * @param parameterName
	 *            the name of the parameter to be set
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if parameterIndex does not correspond to a parameter marker
	 *             in the SQL statement; if the length specified is less than
	 *             zero; if the driver does not support national character sets;
	 *             if the driver can detect that a data conversion error could
	 *             occur; if a database access error occurs or this method is
	 *             called on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. This
	 * method differs from the <code>setCharacterStream (int, Reader)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>NCLOB</code>. When the
	 * <code>setCharacterStream</code> method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be send to the
	 * server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setNClob</code> which
	 * takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; if a database access error occurs or this method is
	 *             called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setNClob(String parameterName, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The reader
	 * must contain the number of characters specified by length otherwise a
	 * <code>SQLException</code> will be generated when the
	 * <code>PreparedStatement</code> is executed. This method differs from the
	 * <code>setCharacterStream (int, Reader, int)</code> method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a <code>NCLOB</code>. When the <code>setCharacterStream</code> method
	 * is used, the driver may have to do extra work to determine whether the
	 * parameter data should be sent to the server as a
	 * <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if parameterIndex does not correspond to a parameter marker
	 *             in the SQL statement; if the length specified is less than
	 *             zero; if the driver does not support national character sets;
	 *             if the driver can detect that a data conversion error could
	 *             occur; if a database access error occurs or this method is
	 *             called on a closed <code>PreparedStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>java.sql.NClob</code> object.
	 * The driver converts this to a SQL <code>NCLOB</code> value when it sends
	 * it to the database.
	 * 
	 * @param parameterIndex
	 *            of the first parameter is 1, the second is 2, ...
	 * @param value
	 *            the parameter value
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur ; or if a database access error occurs
	 * @since 1.6
	 */
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated paramter to the given <code>String</code> object. The
	 * driver converts this to a SQL <code>NCHAR</code> or <code>NVARCHAR</code>
	 * or <code>LONGNVARCHAR</code> value (depending on the argument's size
	 * relative to the driver's limits on <code>NVARCHAR</code> values) when it
	 * sends it to the database.
	 * 
	 * @param parameterIndex
	 *            of the first parameter is 1, the second is 2, ...
	 * @param value
	 *            the parameter value
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur ; or if a database access error occurs
	 * @since 1.6
	 */
	public void setNString(int parameterIndex, String value) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated paramter to the given <code>String</code> object. The
	 * driver converts this to a SQL <code>NCHAR</code> or <code>NVARCHAR</code>
	 * or <code>LONGNVARCHAR</code>
	 * 
	 * @param parameterName
	 *            the name of the column to be set
	 * @param value
	 *            the parameter value
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; or if a database access error occurs
	 * @since 1.6
	 */
	public void setNString(String parameterName, String value) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to the
	 * national character set in the database.
	 * 
	 * @param parameterIndex
	 *            of the first parameter is 1, the second is 2, ...
	 * @param value
	 *            the parameter value
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur ; or if a database access error occurs
	 * @since 1.6
	 */
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to the
	 * national character set in the database.
	 * 
	 * @param parameterName
	 *            the name of the column to be set
	 * @param value
	 *            the parameter value
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur; or if a database access error occurs
	 * @since 1.6
	 */
	public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>Reader</code> reads the data till end-of-file is reached. The
	 * driver does the necessary conversion from Java character format to the
	 * national character set in the database.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>setNCharacterStream</code> which takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param value
	 *            the parameter value
	 * @throws SQLException
	 *             if the driver does not support national character sets; if
	 *             the driver can detect that a data conversion error could
	 *             occur ; if a database access error occurs; or this method is
	 *             called on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given
	 * <code>java.sql.Timestamp</code> value, using the given
	 * <code>Calendar</code> object. The driver uses the <code>Calendar</code>
	 * object to construct an SQL <code>TIMESTAMP</code> value, which the driver
	 * then sends to the database. With a a <code>Calendar</code> object, the
	 * driver can calculate the timestamp taking into account a custom timezone.
	 * If no <code>Calendar</code> object is specified, the driver uses the
	 * default timezone, which is that of the virtual machine running the
	 * application.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @param cal
	 *            the <code>Calendar</code> object the driver will use to
	 *            construct the timestamp
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getTimestamp
	 * @since 1.4
	 */
	public void setTimestamp(String parameterName, java.sql.Timestamp x, Calendar cal) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The
	 * <code>reader</code> must contain the number of characters specified by
	 * length otherwise a <code>SQLException</code> will be generated when the
	 * <code>CallableStatement</code> is executed. This method differs from the
	 * <code>setCharacterStream (int, Reader, int)</code> method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a <code>CLOB</code>. When the <code>setCharacterStream</code> method
	 * is used, the driver may have to do extra work to determine whether the
	 * parameter data should be send to the server as a <code>LONGVARCHAR</code>
	 * or a <code>CLOB</code>
	 * 
	 * @param parameterName
	 *            the name of the parameter to be set
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if parameterIndex does not correspond to a parameter marker
	 *             in the SQL statement; if the length specified is less than
	 *             zero; a database access error occurs or this method is called
	 *             on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setClob(String parameterName, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Clob</code>
	 * object. The driver converts this to an SQL <code>CLOB</code> value when
	 * it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            a <code>Clob</code> object that maps an SQL <code>CLOB</code>
	 *            value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setClob(String parameterName, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. This
	 * method differs from the <code>setCharacterStream (int, Reader)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>CLOB</code>. When the
	 * <code>setCharacterStream</code> method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be send to the
	 * server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setClob</code> which
	 * takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on
	 *             a closed <code>CallableStatement</code>
	 * 
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setClob(String parameterName, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Date</code>
	 * value using the default time zone of the virtual machine that is running
	 * the application. The driver converts this to an SQL <code>DATE</code>
	 * value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getDate
	 * @since 1.4
	 */
	public void setDate(String parameterName, java.sql.Date x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Date</code>
	 * value, using the given <code>Calendar</code> object. The driver uses the
	 * <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
	 * which the driver then sends to the database. With a a
	 * <code>Calendar</code> object, the driver can calculate the date taking
	 * into account a custom timezone. If no <code>Calendar</code> object is
	 * specified, the driver uses the default timezone, which is that of the
	 * virtual machine running the application.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @param cal
	 *            the <code>Calendar</code> object the driver will use to
	 *            construct the date
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getDate
	 * @since 1.4
	 */
	public void setDate(String parameterName, java.sql.Date x, Calendar cal) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Time</code>
	 * value. The driver converts this to an SQL <code>TIME</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getTime
	 * @since 1.4
	 */
	public void setTime(String parameterName, java.sql.Time x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Time</code>
	 * value, using the given <code>Calendar</code> object. The driver uses the
	 * <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
	 * which the driver then sends to the database. With a a
	 * <code>Calendar</code> object, the driver can calculate the time taking
	 * into account a custom timezone. If no <code>Calendar</code> object is
	 * specified, the driver uses the default timezone, which is that of the
	 * virtual machine running the application.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @param cal
	 *            the <code>Calendar</code> object the driver will use to
	 *            construct the time
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getTime
	 * @since 1.4
	 */
	public void setTime(String parameterName, java.sql.Time x, Calendar cal) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. This
	 * method differs from the <code>setCharacterStream (int, Reader)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>CLOB</code>. When the
	 * <code>setCharacterStream</code> method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be sent to the
	 * server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setClob</code> which
	 * takes a length parameter.
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if a database access error occurs, this method is called on a
	 *             closed <code>PreparedStatement</code>or if parameterIndex
	 *             does not correspond to a parameter marker in the SQL
	 *             statement
	 * 
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>Reader</code> object. The reader
	 * must contain the number of characters specified by length otherwise a
	 * <code>SQLException</code> will be generated when the
	 * <code>PreparedStatement</code> is executed. This method differs from the
	 * <code>setCharacterStream (int, Reader, int)</code> method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a <code>CLOB</code>. When the <code>setCharacterStream</code> method
	 * is used, the driver may have to do extra work to determine whether the
	 * parameter data should be sent to the server as a <code>LONGVARCHAR</code>
	 * or a <code>CLOB</code>
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param reader
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of characters in the parameter data.
	 * @throws SQLException
	 *             if a database access error occurs, this method is called on a
	 *             closed <code>PreparedStatement</code>, if parameterIndex does
	 *             not correspond to a parameter marker in the SQL statement, or
	 *             if the length specified is less than zero.
	 * 
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object. The
	 * inputstream must contain the number of characters specified by length
	 * otherwise a <code>SQLException</code> will be generated when the
	 * <code>PreparedStatement</code> is executed. This method differs from the
	 * <code>setBinaryStream (int, InputStream, int)</code> method because it
	 * informs the driver that the parameter value should be sent to the server
	 * as a <code>BLOB</code>. When the <code>setBinaryStream</code> method is
	 * used, the driver may have to do extra work to determine whether the
	 * parameter data should be sent to the server as a
	 * <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of bytes in the parameter data.
	 * @throws SQLException
	 *             if a database access error occurs, this method is called on a
	 *             closed <code>PreparedStatement</code>, if parameterIndex does
	 *             not correspond to a parameter marker in the SQL statement, if
	 *             the length specified is less than zero or if the number of
	 *             bytes in the inputstream does not match the specfied length.
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object. This
	 * method differs from the <code>setBinaryStream (int, InputStream)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>BLOB</code>. When the
	 * <code>setBinaryStream</code> method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be sent to the
	 * server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setBlob</code> which
	 * takes a length parameter.
	 * 
	 * @param parameterIndex
	 *            index of the first parameter is 1, the second is 2, ...
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if a database access error occurs, this method is called on a
	 *             closed <code>PreparedStatement</code> or if parameterIndex
	 *             does not correspond to a parameter marker in the SQL
	 *             statement,
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object. The
	 * <code>inputstream</code> must contain the number of characters specified
	 * by length, otherwise a <code>SQLException</code> will be generated when
	 * the <code>CallableStatement</code> is executed. This method differs from
	 * the <code>setBinaryStream (int, InputStream, int)</code> method because
	 * it informs the driver that the parameter value should be sent to the
	 * server as a <code>BLOB</code>. When the <code>setBinaryStream</code>
	 * method is used, the driver may have to do extra work to determine whether
	 * the parameter data should be sent to the server as a
	 * <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 * 
	 * @param parameterName
	 *            the name of the parameter to be set the second is 2, ...
	 * 
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @param length
	 *            the number of bytes in the parameter data.
	 * @throws SQLException
	 *             if parameterIndex does not correspond to a parameter marker
	 *             in the SQL statement, or if the length specified is less than
	 *             zero; if the number of bytes in the inputstream does not
	 *             match the specfied length; if a database access error occurs
	 *             or this method is called on a closed
	 *             <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>java.sql.Blob</code>
	 * object. The driver converts this to an SQL <code>BLOB</code> value when
	 * it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            a <code>Blob</code> object that maps an SQL <code>BLOB</code>
	 *            value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setBlob(String parameterName, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to a <code>InputStream</code> object. This
	 * method differs from the <code>setBinaryStream (int, InputStream)</code>
	 * method because it informs the driver that the parameter value should be
	 * sent to the server as a <code>BLOB</code>. When the
	 * <code>setBinaryStream</code> method is used, the driver may have to do
	 * extra work to determine whether the parameter data should be send to the
	 * server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
	 * 
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setBlob</code> which
	 * takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param inputStream
	 *            An object that contains the data to set the parameter value
	 *            to.
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on
	 *             a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * 
	 * @since 1.6
	 */
	public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the value of the designated parameter with the given object. The
	 * second argument must be an object type; for integral values, the
	 * <code>java.lang</code> equivalent objects should be used.
	 * 
	 * <p>
	 * The given Java object will be converted to the given targetSqlType before
	 * being sent to the database.
	 * 
	 * If the object has a custom mapping (is of a class implementing the
	 * interface <code>SQLData</code>), the JDBC driver should call the method
	 * <code>SQLData.writeSQL</code> to write it to the SQL data stream. If, on
	 * the other hand, the object is of a class implementing <code>Ref</code>,
	 * <code>Blob</code>, <code>Clob</code>, <code>NClob</code>,
	 * <code>Struct</code>, <code>java.net.URL</code>, or <code>Array</code>,
	 * the driver should pass it to the database as a value of the corresponding
	 * SQL type.
	 * <P>
	 * Note that this method may be used to pass datatabase- specific abstract
	 * data types.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the object containing the input parameter value
	 * @param targetSqlType
	 *            the SQL type (as defined in java.sql.Types) to be sent to the
	 *            database. The scale argument may further qualify this type.
	 * @param scale
	 *            for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
	 *            this is the number of digits after the decimal point. For all
	 *            other types, this value will be ignored.
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if <code>targetSqlType</code> is a <code>ARRAY</code>,
	 *                <code>BLOB</code>, <code>CLOB</code>,
	 *                <code>DATALINK</code>, <code>JAVA_OBJECT</code>,
	 *                <code>NCHAR</code>, <code>NCLOB</code>,
	 *                <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                or <code>STRUCT</code> data type and the JDBC driver does
	 *                not support this data type
	 * @see Types
	 * @see #getObject
	 * @since 1.4
	 */
	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the value of the designated parameter with the given object. This
	 * method is like the method <code>setObject</code> above, except that it
	 * assumes a scale of zero.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the object containing the input parameter value
	 * @param targetSqlType
	 *            the SQL type (as defined in java.sql.Types) to be sent to the
	 *            database
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if <code>targetSqlType</code> is a <code>ARRAY</code>,
	 *                <code>BLOB</code>, <code>CLOB</code>,
	 *                <code>DATALINK</code>, <code>JAVA_OBJECT</code>,
	 *                <code>NCHAR</code>, <code>NCLOB</code>,
	 *                <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
	 *                <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
	 *                or <code>STRUCT</code> data type and the JDBC driver does
	 *                not support this data type
	 * @see #getObject
	 * @since 1.4
	 */
	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the value of the designated parameter with the given object. The
	 * second parameter must be of type <code>Object</code>; therefore, the
	 * <code>java.lang</code> equivalent objects should be used for built-in
	 * types.
	 * 
	 * <p>
	 * The JDBC specification specifies a standard mapping from Java
	 * <code>Object</code> types to SQL types. The given argument will be
	 * converted to the corresponding SQL type before being sent to the
	 * database.
	 * 
	 * <p>
	 * Note that this method may be used to pass datatabase- specific abstract
	 * data types, by using a driver-specific Java type.
	 * 
	 * If the object is of a class implementing the interface
	 * <code>SQLData</code>, the JDBC driver should call the method
	 * <code>SQLData.writeSQL</code> to write it to the SQL data stream. If, on
	 * the other hand, the object is of a class implementing <code>Ref</code>,
	 * <code>Blob</code>, <code>Clob</code>, <code>NClob</code>,
	 * <code>Struct</code>, <code>java.net.URL</code>, or <code>Array</code>,
	 * the driver should pass it to the database as a value of the corresponding
	 * SQL type.
	 * <P>
	 * This method throws an exception if there is an ambiguity, for example, if
	 * the object is of a class implementing more than one of the interfaces
	 * named above.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the object containing the input parameter value
	 * @exception SQLException
	 *                if a database access error occurs, this method is called
	 *                on a closed <code>CallableStatement</code> or if the given
	 *                <code>Object</code> parameter is ambiguous
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getObject
	 * @since 1.4
	 */
	public void setObject(String parameterName, Object x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes. When a very large ASCII value is input to
	 * a <code>LONGVARCHAR</code> parameter, it may be more practical to send it
	 * via a <code>java.io.InputStream</code>. Data will be read from the stream
	 * as needed until end-of-file is reached. The JDBC driver will do any
	 * necessary conversion from ASCII to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the Java input stream that contains the ASCII parameter value
	 * @param length
	 *            the number of bytes in the stream
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setAsciiStream(String parameterName, java.io.InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given input stream, which will have
	 * the specified number of bytes. When a very large binary value is input to
	 * a <code>LONGVARBINARY</code> parameter, it may be more practical to send
	 * it via a <code>java.io.InputStream</code> object. The data will be read
	 * from the stream as needed until end-of-file is reached.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the java input stream which contains the binary parameter
	 *            value
	 * @param length
	 *            the number of bytes in the stream
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setBinaryStream(String parameterName, java.io.InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code> object,
	 * which is the given number of characters long. When a very large UNICODE
	 * value is input to a <code>LONGVARCHAR</code> parameter, it may be more
	 * practical to send it via a <code>java.io.Reader</code> object. The data
	 * will be read from the stream as needed until end-of-file is reached. The
	 * JDBC driver will do any necessary conversion from UNICODE to the database
	 * char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            the <code>java.io.Reader</code> object that contains the
	 *            UNICODE data used as the designated parameter
	 * @param length
	 *            the number of characters in the stream
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setCharacterStream(String parameterName, java.io.Reader reader, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given input stream. When a very
	 * large ASCII value is input to a <code>LONGVARCHAR</code> parameter, it
	 * may be more practical to send it via a <code>java.io.InputStream</code>.
	 * Data will be read from the stream as needed until end-of-file is reached.
	 * The JDBC driver will do any necessary conversion from ASCII to the
	 * database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setAsciiStream</code>
	 * which takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the Java input stream that contains the ASCII parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setAsciiStream(String parameterName, java.io.InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given input stream. When a very
	 * large binary value is input to a <code>LONGVARBINARY</code> parameter, it
	 * may be more practical to send it via a <code>java.io.InputStream</code>
	 * object. The data will be read from the stream as needed until end-of-file
	 * is reached.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of <code>setBinaryStream</code>
	 * which takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the java input stream which contains the binary parameter
	 *            value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setBinaryStream(String parameterName, java.io.InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given <code>Reader</code> object.
	 * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
	 * parameter, it may be more practical to send it via a
	 * <code>java.io.Reader</code> object. The data will be read from the stream
	 * as needed until end-of-file is reached. The JDBC driver will do any
	 * necessary conversion from UNICODE to the database char format.
	 * 
	 * <P>
	 * <B>Note:</B> This stream object can either be a standard Java stream
	 * object or your own subclass that implements the standard interface.
	 * <P>
	 * <B>Note:</B> Consult your JDBC driver documentation to determine if it
	 * might be more efficient to use a version of
	 * <code>setCharacterStream</code> which takes a length parameter.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param reader
	 *            the <code>java.io.Reader</code> object that contains the
	 *            Unicode data
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @throws SQLFeatureNotSupportedException
	 *             if the JDBC driver does not support this method
	 * @since 1.6
	 */
	public void setCharacterStream(String parameterName, java.io.Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given
	 * <code>java.math.BigDecimal</code> value. The driver converts this to an
	 * SQL <code>NUMERIC</code> value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getBigDecimal
	 * @since 1.4
	 */
	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>String</code>
	 * value. The driver converts this to an SQL <code>VARCHAR</code> or
	 * <code>LONGVARCHAR</code> value (depending on the argument's size relative
	 * to the driver's limits on <code>VARCHAR</code> values) when it sends it
	 * to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getString
	 * @since 1.4
	 */
	public void setString(String parameterName, String x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java array of bytes. The
	 * driver converts this to an SQL <code>VARBINARY</code> or
	 * <code>LONGVARBINARY</code> (depending on the argument's size relative to
	 * the driver's limits on <code>VARBINARY</code> values) when it sends it to
	 * the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getBytes
	 * @since 1.4
	 */
	public void setBytes(String parameterName, byte x[]) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given
	 * <code>java.sql.Timestamp</code> value. The driver converts this to an SQL
	 * <code>TIMESTAMP</code> value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getTimestamp
	 * @since 1.4
	 */
	public void setTimestamp(String parameterName, java.sql.Timestamp x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to SQL <code>NULL</code>.
	 * 
	 * <P>
	 * <B>Note:</B> You must specify the parameter's SQL type.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            the SQL type code defined in <code>java.sql.Types</code>
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setNull(String parameterName, int sqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to SQL <code>NULL</code>. This version of
	 * the method <code>setNull</code> should be used for user-defined types and
	 * REF type parameters. Examples of user-defined types include: STRUCT,
	 * DISTINCT, JAVA_OBJECT, and named array types.
	 * 
	 * <P>
	 * <B>Note:</B> To be portable, applications must give the SQL type code and
	 * the fully-qualified SQL type name when specifying a NULL user-defined or
	 * REF parameter. In the case of a user-defined type the name is the type
	 * name of the parameter itself. For a REF parameter, the name is the type
	 * name of the referenced type. If a JDBC driver does not need the type code
	 * or type name information, it may ignore it.
	 * 
	 * Although it is intended for user-defined and Ref parameters, this method
	 * may be used to set a null parameter of any JDBC type. If the parameter
	 * does not have a user-defined or REF type, the given typeName is ignored.
	 * 
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param sqlType
	 *            a value from <code>java.sql.Types</code>
	 * @param typeName
	 *            the fully-qualified name of an SQL user-defined type; ignored
	 *            if the parameter is not a user-defined type or SQL
	 *            <code>REF</code> value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>boolean</code>
	 * value. The driver converts this to an SQL <code>BIT</code> or
	 * <code>BOOLEAN</code> value when it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @see #getBoolean
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @since 1.4
	 */
	public void setBoolean(String parameterName, boolean x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>byte</code> value.
	 * The driver converts this to an SQL <code>TINYINT</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getByte
	 * @since 1.4
	 */
	public void setByte(String parameterName, byte x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>short</code> value.
	 * The driver converts this to an SQL <code>SMALLINT</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getShort
	 * @since 1.4
	 */
	public void setShort(String parameterName, short x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>int</code> value.
	 * The driver converts this to an SQL <code>INTEGER</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getInt
	 * @since 1.4
	 */
	public void setInt(String parameterName, int x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>long</code> value.
	 * The driver converts this to an SQL <code>BIGINT</code> value when it
	 * sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getLong
	 * @since 1.4
	 */
	public void setLong(String parameterName, long x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>float</code> value.
	 * The driver converts this to an SQL <code>FLOAT</code> value when it sends
	 * it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getFloat
	 * @since 1.4
	 */
	public void setFloat(String parameterName, float x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * Sets the designated parameter to the given Java <code>double</code>
	 * value. The driver converts this to an SQL <code>DOUBLE</code> value when
	 * it sends it to the database.
	 * 
	 * @param parameterName
	 *            the name of the parameter
	 * @param x
	 *            the parameter value
	 * @exception SQLException
	 *                if a database access error occurs or this method is called
	 *                on a closed <code>CallableStatement</code>
	 * @exception SQLFeatureNotSupportedException
	 *                if the JDBC driver does not support this method
	 * @see #getDouble
	 * @since 1.4
	 */
	public void setDouble(String parameterName, double x) throws SQLException {
		throw new SQLFeatureNotSupportedException(resBundle.handleGetObject("cachedrowsetimpl.featnotsupp").toString());
	}

	/**
	 * This method re populates the resBundle during the deserialization process
	 * 
	 */
	protected void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Default state initialization happens here
		ois.defaultReadObject();
		// Initialization of transient Res Bundle happens here .
		resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();

	}

	static final long serialVersionUID = 1884577171200622428L;

	public int[] getMatchColumnIndexes() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String[] getMatchColumnNames() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void execute(Connection conn) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void acceptChanges() throws SyncProviderException {
		throw new UnsupportedOperationException();
	}

	public void acceptChanges(Connection con) throws SyncProviderException {
		throw new UnsupportedOperationException();
	}

	public void restoreOriginal() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void release() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void undoDelete() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void undoInsert() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void undoUpdate() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Collection<?> toCollection() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Collection<?> toCollection(int column) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Collection<?> toCollection(String column) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SyncProvider getSyncProvider() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setSyncProvider(String provider) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public CachedRowSet createCopySchema() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public CachedRowSet createCopyNoConstraints() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void commit() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void rollback() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void rollback(Savepoint s) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void populate(ResultSet rs, int startRow) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setPageSize(int size) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getPageSize() {
		throw new UnsupportedOperationException();
	}

	public boolean nextPage() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean previousPage() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public List<Row> getRvh() {
		return rvh;
	}
}
