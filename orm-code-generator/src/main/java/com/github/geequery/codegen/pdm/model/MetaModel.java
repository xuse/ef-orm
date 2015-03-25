package com.github.geequery.codegen.pdm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class MetaModel {
	static final MetaReference[] EMPTY = new MetaReference[0];
	protected String code;

	protected String comment;
	protected List<MetaTable> vTables = new ArrayList<MetaTable>();
	protected String name;

	private HashMap<String, MetaTable> tableIndex = new HashMap<String, MetaTable>();
	protected HashMap<String, MetaReference> reference = new HashMap<String, MetaReference>();

	/**
	 * @param ref
	 * @return
	 */
	public MetaReference getReference(String ref) {
		return reference.get(ref);
	}

	/**
	 * @param aCode
	 *            code
	 * @param aName
	 *            name
	 * @param aComment
	 *            comment for this MetaModel
	 */
	public MetaModel(String aCode, String aName, String aComment) {
		super();
		code = aCode;
		name = aName;
		comment = aComment;
	}

	/**
	 * Returns reference of MetaTable with specified code
	 * 
	 * @param aCode
	 *            code of MetaTable
	 * @return reference of MetaTable with specified code
	 * @throws NullPointerException
	 */
	public MetaTable getTable(String aCode) throws NullPointerException {
		return tableIndex.get(StringUtils.upperCase(aCode));
	}

	/**
	 * @param aTable
	 *            MetaTable to add
	 * @throws NullPointerException
	 */
	public void addTable(MetaTable aTable) {
		vTables.add(aTable);
		tableIndex.put(StringUtils.upperCase(aTable.getCode()), aTable);
		for (MetaReference ref : aTable.importRefs) {
			reference.put(ref.getCode(), ref);
		}
	}

	/**
	 * Returns collection of tables
	 * 
	 * @return collection of tables
	 */
	public List<MetaTable> getTables() {
		return vTables;
	}

	private List<MetaTable> getSortedTables() {
		MetaTable[] ts = this.vTables.toArray(new MetaTable[vTables.size()]);
		Arrays.sort(ts, new Comparator<MetaTable>() {
			public int compare(MetaTable o1, MetaTable o2) {
				if (o1 == null && o2 == null)
					return 0;
				if (o1 == null)
					return -1;
				if (o2 == null)
					return 1;
				if (o1.name == null && o2.name == null)
					return 0;
				if (o1.name == null)
					return -1;
				if (o2.name == null)
					return 1;
				return o1.name.compareTo(o2.name);
			}
		});
		return Arrays.asList(ts);
	}

	/**
	 * Returns code of this MetaModel
	 * 
	 * @return code of this MetaModel
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Returns total columns of all tables within this MetaModel
	 * 
	 * @return total columns of all tables within this MetaModel
	 */
	public int getAllColumnCount() {
		int retVal = 0;
		for (MetaTable table : vTables) {
			retVal += table.getTotalColumns();
		}
		return retVal;
	}

	/**
	 * Returns description of this MetaModel
	 * 
	 * @return description of this MetaModel
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Returns name of this MetaModel
	 * 
	 * @return name of this MetaModel
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns total count of all MetaTable(s) within this MetaModel
	 * 
	 * @return total count of all MetaTable(s) within this MetaModel
	 */
	public int getTableCount() {
		return vTables.size();
	}

	/**
	 * Sets the code of this MetaModel
	 * 
	 * @param aCode
	 *            code to set
	 */
	public void setCode(String aCode) {
		code = aCode;
	}

	/**
	 * Sets description of this MetaModel
	 * 
	 * @param aDescription
	 *            the description to set
	 */
	public void setComment(String aDescription) {
		comment = aDescription;
	}

	/**
	 * Sets the name of this MetaModel
	 * 
	 * @param aName
	 *            The name to set.
	 */
	public void setName(String aName) {
		name = aName;
	}

	/**
	 * Returns the string representation of this MetaModel
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return name;
	}

	/**
	 * Returns verbose description of this MetaModel
	 * 
	 * @return verbose description of this MetaModel
	 */
	public String toStringVerbose() {
		String retVal = "<HTML><TABLE border=\"1\">" + "<CAPTION><B>" + code + "</B></CAPTION>";
		retVal += "<TBODY>";
		retVal += "<TR><TD align=\"right\">" + "<B>name</B>" + "</TD>" + "<TD align=\"left\">" + name + "</TD></TR>";
		retVal += "<TR><TD align=\"right\">" + "<B>code</B>" + "</TD>" + "<TD align=\"left\">" + code + "</TD></TR>";
		retVal += "<TR><TD align=\"right\">" + "<B>comment</B>" + "</TD>" + "<TD align=\"left\">" + MetaUtils.insertHTMLBreaks(comment, 50) + "</TD></TR>";
		retVal += "</TBODY></TABLE>";
		retVal += "<TABLE border=\"1\">" + "<CAPTION><B> TABLES" + "</B></CAPTION><TBODY>";
		retVal += "<TH><I>No.</I></TH><TH><I>Name</I></TH><TH><I>Code</I></TH>";
		int count = 0;
		for (MetaTable crntTable : this.getSortedTables()) {
			retVal += "<TR><TD align=\"right\">";
			retVal += ++count;
			retVal += "</TD><TD>";
			retVal += crntTable.getName();
			retVal += "</TD><TD>";
			retVal += crntTable.getCode();
			retVal += "</TD></TR>";
		}
		retVal += "</TBODY></TABLE></HTML>";
		return retVal;
	}
}
