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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jef.database.meta.object.Column;
import jef.database.meta.object.ForeignKey;
import jef.database.meta.object.PrimaryKey;

/**
 * Class represent of DB Table's meta data
 *
 */
public class MetaTable implements Iterable<MetaColumn> {
	protected String name;

    protected String code;

    protected String comment;

    protected List<MetaKey> htKeys = new ArrayList<MetaKey>();

    protected List<MetaReference> importRefs = new ArrayList<MetaReference>();
    protected List<MetaReference> exportRefs = new ArrayList<MetaReference>();
    protected List<MetaColumn> vColumns = new ArrayList<MetaColumn>();
    
    public void addExportRef(MetaReference currMRef) {
    	exportRefs.add(currMRef);
	}
    
    /**
     * @param aCode
     *            code
     * @param aName
     *            name
     * @param aComment
     *            comment about this MetaTable
     */
    public MetaTable(String aCode, String aName, String aComment) {
        super();
        code = aCode;
        name = aName;
        comment = aComment;
    }

    /**
     * Adds the new MetaColumn into this MetaTable container
     * 
     * @param aMC
     */
    public void addCol(MetaColumn aMC) {
        aMC.setParentTable(code);
        for ( MetaKey mk: htKeys) {
            if (mk.containsColumn(aMC))
                aMC.setParentKey(mk);
        }
        vColumns.add(aMC);
    }

    /**
     * @param aMK
     */
    public void addKey(MetaKey aMK) {
        htKeys.add(aMK);
    }

    /**
     * @param aMR
     */
    public void addRef(MetaReference aMR) {
        importRefs.add(aMR);
    }

    /**
     * Returns collection of MetaColumn(s) of this MetaTable
     * 
     * @return collection of MetaColumn(s) of this MetaTable
     */
    public List<MetaColumn> getColumns() {
        return vColumns;
    }

    /**
     * @return Enumeration of MetaKeys
     */
    public List<MetaKey> getKeys() {
        return htKeys;
    }

    /**
     * @return Enumeration of References
     */
    public List<MetaReference> getImportKeys() {
        return importRefs;
    }
    
    /**
     * @return Enumeration of References
     */
    public List<MetaReference> getExportKeys() {
        return this.exportRefs;
    }

    /**
     * Returns code of this MetaTable
     * 
     * @return code of this MetaTable
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns MetaColumn of this MetaTable by specified column.getCode()
     * 
     * @param columnCode
     *            specified code
     * @return MetaColumn of this MetaTable by specified columnCode if exists, otherwise it returns
     *         null
     * @throws NullPointerException
     *             if aCode is null
     */
	public MetaColumn getColumnByCode(String columnCode) {
		for(MetaColumn m: this.vColumns){
			if(columnCode.equals(m.getCode())){
				return m;
			}
		}
		return null;
	}

    /**
     * Returns description of this MetaTable
     * 
     * @return description of this MetaTable
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns name of this MetaTable
     * 
     * @return name of this MetaTable
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the count of all MetaColumn(s) within this MetaTable
     * 
     * @return the count of all MetaColumn(s) within this MetaTable
     */
    public int getTotalColumns() {
        return vColumns.size();
    }

    public Iterator<MetaColumn> iterator() {
        //return htColumns.values().iterator();
        return vColumns.iterator();
    }

    /**
     * Sets code for this MetaTable
     * 
     * @param code
     *            the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Sets description of this MetaTable
     * 
     * @param aDescription
     *            the description to set
     */
    public void setComment(String aDescription) {
        comment = aDescription;
    }

    /**
     * Sets name for this MetaTable
     * 
     * @param name
     *            the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns string representation of this MetaTable (name)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }

    /**
     * Returns verbose description of this MetaTable (HTML) - recursive fks
     * 
     * @return verbose description of this MetaTable
     */
    public String toStringVerbose() {
        String retVal = "<HTML><TABLE border=\"1\">" + "<CAPTION><B>" + code
                + "</B></CAPTION>";
        retVal += "<TBODY>";
        retVal += "<TR><TD align=\"right\">" + "<B>name</B>" + "</TD>"
                + "<TD align=\"left\">" + name + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>code</B>" + "</TD>"
                + "<TD align=\"left\">" + code + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>comment</B>" + "</TD>"
                + "<TD align=\"left\" ROWSPAN=\"2\">"
                + MetaUtils.insertHTMLBreaks(comment, 50) + "</TD></TR>";
        retVal += "</TBODY></TABLE>";
        // KEYS
        retVal += "<TABLE border=\"1\">" + "<CAPTION><B> KEYS"
                + "</B></CAPTION>";
        retVal += "<TH><I>Name</I></TH><TH><I>Code</I></TH><TH><I>Columns</I></TH><TBODY>";
        for (MetaKey crntKey : htKeys) {
            retVal += "<TR><TD>";
            retVal += crntKey.getName();
            retVal += "</TD><TD>";
            retVal += crntKey.getCode();
            retVal += "</TD><TD><TABLE><TBODY>";
            for( MetaColumn crntKeyCol: crntKey.getColumns()){
                retVal += "<TR><TD>" + crntKeyCol.getCode() + "</TD><TD>";
                retVal += crntKeyCol.getName() + "</TD></TR>";
            }
            retVal += "</TBODY></TABLE>";
        }
        retVal += "</TBODY></TABLE>";

        // REFERENCES
        retVal += "<TABLE border=\"1\">" + "<CAPTION><B> REFERENCES"
                + "</B></CAPTION>";
        retVal += "<TH><I>Name</I></TH><TH><I>Code</I></TH><TH><I>Parent Table</I></TH>"
                + "<TH><I>Columns</I></TH><TBODY>";
        for ( MetaReference currRef :importRefs) {
            retVal += "<TR><TD>";
            retVal += currRef.getName();
            retVal += "</TD><TD>";
            retVal += currRef.getCode();
            retVal += "</TD><TD>";
            retVal += currRef.getParentTable();
            retVal += "</TD><TD><TABLE><TBODY>";
            for ( MetaColumn crntKeyCol:currRef.getColumns()) {
                retVal += "<TR><TD>" + crntKeyCol.getCode() + "</TD><TD>";
                retVal += crntKeyCol.getName() + "</TD></TR>";
            }
            retVal += "</TD></TBODY></TABLE>";
        }
        retVal += "</TBODY></TABLE>";

        // COLUMNS
        retVal += "<TABLE border=\"1\">" + "<CAPTION><B> COLUMNS"
                + "</B></CAPTION>";
        retVal += "<TH><I>No</I></TH><TH><I>Name</I></TH><TH><I>Code</I></TH><TBODY>";
        int count = 0;
        for(MetaColumn crntCol: vColumns){
            retVal += "<TR><TD align=\"right\">";
            retVal += ++count;
            retVal += "</TD><TD>";
            retVal += crntCol.getName();
            retVal += "</TD><TD>";
            retVal += crntCol.getCode();
            retVal += "</TD></TR>";
        }
        retVal += "</TBODY></TABLE></HTML>";

        return retVal;
    }

	public List<Column> getJefColumns() {
		List<Column> result=new ArrayList<Column>();
		for(MetaColumn c: this.vColumns){
			result.add(c.toJefColumn());
		}
		return result;
	}

	public PrimaryKey getJefPK() {
		if(this.htKeys.isEmpty())return null;
		MetaKey key=this.htKeys.iterator().next();
		return key.toJefPk();
	}

	public List<ForeignKey> getJefFK() {
		List<ForeignKey> result=new ArrayList<ForeignKey>();
		for(MetaReference ref: this.getImportKeys()){
			for(Entry<MetaColumn,MetaColumn> e: ref.getJoinColumns().entrySet()){
				result.add(ref.toJefFK(e));
			}
		}
		return result;
	}
}
