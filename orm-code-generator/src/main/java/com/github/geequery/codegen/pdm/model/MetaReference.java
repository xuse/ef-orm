/* 
 * This file is part of Mosquito meta-loader.
 *
 * Mosquito meta-loader is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mosquito meta-loader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.geequery.codegen.pdm.model;

import java.util.Collection;
import java.util.Map.Entry;

import jef.common.SimpleMap;
import jef.database.meta.ForeignKey;

/**
 *
 */
public class MetaReference {

	protected String code;

	protected SimpleMap<MetaColumn, MetaColumn> joinColumns = new SimpleMap<MetaColumn, MetaColumn>();

	protected String name;

	protected MetaTable sourceTable;

	protected MetaTable parentTable;
	
	protected String cardinality;

	
	
	public SimpleMap<MetaColumn, MetaColumn> getJoinColumns() {
		return joinColumns;
	}

	/**
     * 
     */
	public MetaReference() {
	}

	/**
	 * @param aName
	 * @param aCode
	 */
	public MetaReference(String aName, String aCode,String cardinality) {
		name = aName;
		code = aCode;
		this.cardinality=cardinality;
	}
	
	public String getCardinality() {
		return cardinality;
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(60);
		sb.append(code).append(' ').append(sourceTable).append(".").append(this.joinColumns.keySet()).append("->");
		sb.append(parentTable).append(".").append(this.joinColumns.values()).append(' ').append(this.cardinality);
		return sb.toString();
	}

	/**
	 * Adds a new MetaColumn into key group
	 * 
	 * @param aMetaColumn
	 */
	public void addColumn(MetaColumn aMetaColumn,MetaColumn bMetaColumn) {
		joinColumns.put(aMetaColumn, bMetaColumn);
	}

	/**
	 * @param aMetaColumn
	 * @return true if aMetaColumn is part of this MetaKey
	 */
	public boolean containsColumn(MetaColumn aMetaColumn) {
		return joinColumns.containsValue(aMetaColumn);
	}

	/**
	 * @param aColCode
	 * @return
	 * @throws NullPointerException
	 */
	public boolean containsColumnCode(String aColCode) throws NullPointerException {
		return joinColumns.containsKey(aColCode);
	}

	/**
	 * @return Enumeration of all columns of this MetaKey
	 */
	public Collection<MetaColumn> getColumns() {
		return joinColumns.keySet();
	}

	/**
	 * @return Enumeration of all columns of this MetaKey
	 */
	public Collection<MetaColumn> getRefColumns() {
		return joinColumns.values();
	}
	
	/**
	 * @return code.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param aCode
	 * @return MetaColumn if it exists, otherwise returns null
	 */
	public MetaColumn getColByCode(String aCode) {
		MetaColumn retVal = null;
		try {
			retVal = (MetaColumn) joinColumns.get(aCode);
		} catch (Exception e) {
			retVal = null;
		}
		return retVal;
	}

	/**
	 * @return name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param aCode
	 *            The code to set.
	 */
	public void setCode(String aCode) {
		code = aCode;
	}

	/**
	 * @param aName
	 *            The name to set.
	 */
	public void setName(String aName) {
		name = aName;
	}

	/**
	 * Get referenced table
	 * 
	 * @return
	 */
	public MetaTable getParentTable() {
		return parentTable;
	}

	/**
	 * @param aParentTable
	 */
	public void setParentTable(MetaTable aParentTable) {
		parentTable = aParentTable;
	}

	/**
	 * @return
	 */
	public MetaTable getSourceTable() {
		return sourceTable;
	}

	/**
	 * @param aSourceTable
	 */
	public void setSourceTable(MetaTable aSourceTable) {
		sourceTable = aSourceTable;
	}

	public ForeignKey toJefFK(Entry<MetaColumn, MetaColumn> e) {
		ForeignKey fk=new ForeignKey();
		fk.setFromColumn(e.getKey().getCode());
		fk.setReferenceColumn(e.getValue().getCode());
		fk.setFromTable(this.getSourceTable().getCode());
		fk.setReferenceTable(this.getParentTable().getCode());
		fk.setName(this.getCode());
		return fk;
	}

}
