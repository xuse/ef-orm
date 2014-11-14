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

package jef.database.jdbc.rowset;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * A class that keeps track of a row's values. A <code>Row</code> object
 * maintains an array of current column values and an array of original column
 * values, and it provides methods for getting and setting the value of a
 * column. It also keeps track of which columns have changed and whether the
 * change was a delete, insert, or update.
 * <P>
 * Note that column numbers for rowsets start at <code>1</code>, whereas the
 * first element of an array or bitset is <code>0</code>. The argument for the
 * method <code>getColumnUpdated</code> refers to the column number in the
 * rowset (the first column is <code>1</code>); the argument for
 * <code>setColumnUpdated</code> refers to the index into the rowset's internal
 * bitset (the first bit is <code>0</code>).
 */
public class Row {

	/**
	 * An array containing the current column values for this <code>Row</code>
	 * object.
	 * 
	 * @serial
	 */
	private Object[] currentVals;

	/**
	 * Creates a new <code>Row</code> object with the given number of columns.
	 * The newly-created row includes an array of original values, an array for
	 * storing its current values, and a <code>BitSet</code> object for keeping
	 * track of which column values have been changed.
	 */
	public Row(int numCols) {
		currentVals = new Object[numCols];
	}

	/**
	 * Creates a new <code>Row</code> object with the given number of columns
	 * and with its array of original values initialized to the given array. The
	 * new <code>Row</code> object also has an array for storing its current
	 * values and a <code>BitSet</code> object for keeping track of which column
	 * values have been changed.
	 */
	public Row(int numCols, Object[] vals) {
		currentVals = new Object[numCols];
		for (int i = 0; i < numCols; i++) {
			currentVals[i] = vals[i];
		}
	}

	/**
	 * 
	 * This method is called internally by the
	 * <code>CachedRowSet.populate</code> methods.
	 * 
	 * @param idx
	 *            the number of the column in this <code>Row</code> object that
	 *            is to be set; the index of the first column is <code>1</code>
	 * @param val
	 *            the new value to be set
	 */
	public void initColumnObject(int idx, Object val) {
		currentVals[idx - 1] = val;
	}

	/**
	 * 
	 * This method is called internally by the
	 * <code>CachedRowSet.updateXXX</code> methods.
	 * 
	 * @param idx
	 *            the number of the column in this <code>Row</code> object that
	 *            is to be set; the index of the first column is <code>1</code>
	 * @param val
	 *            the new value to be set
	 */
	public void setColumnObject(int idx, Object val) {
		currentVals[idx - 1] = val;
	}

	/**
	 * Retrieves the column value stored in the designated column of this
	 * <code>Row</code> object.
	 * 
	 * @param columnIndex
	 *            the index of the column value to be retrieved; the index of
	 *            the first column is <code>1</code>
	 * @return an <code>Object</code> in the Java programming language that
	 *         represents the value stored in the designated column
	 * @throws SQLException
	 *             if there is a database access error
	 */
	public Object getColumnObject(int columnIndex) {
		return (currentVals[columnIndex - 1]); // maps to array!!
	}

	public Object getArrayObject(int columnIndex) {
		return (currentVals[columnIndex]); // maps to array!!
	}

	public void setArrayObject(int idx, Object val) {
		currentVals[idx] = val;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(currentVals);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Row){
			return Arrays.equals(this.currentVals, ((Row)obj).currentVals);	
		}else{
			return false;
		}
		
	}
}
