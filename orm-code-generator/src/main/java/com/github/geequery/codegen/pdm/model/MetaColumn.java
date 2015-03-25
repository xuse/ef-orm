package com.github.geequery.codegen.pdm.model;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jef.database.meta.Column;

import org.apache.commons.lang.StringUtils;

/**
 * PDM元数据的列
 * @author <a href="mailto:nikola.petkov@gmail.com">Nikola Petkov &lt;nikola.petkov@gmail.com&gt;</a>
 */
public class MetaColumn {
    protected String code;

    protected String comment;

    protected MetaColumn fkColParent = null;

    protected String type;

    protected int length;

    protected boolean mandatory = false;

    protected String name;

    protected MetaKey parentKey = null;

    protected String parentTable;

    protected int precision;

    protected String defaultVal;

    protected LinkedHashMap<String, String> lhmListOfVals = new LinkedHashMap<String, String>();

    /**
     * 将当前PDM元数据转换为JEF元数据返回
     * @return
     */
    public Column toJefColumn(){
    	Column c=new Column();
    	c.setColumnName(code);
    	c.setColumnSize(length);
    	c.setDataType(StringUtils.upperCase(type));
    	c.setDataTypeCode(-1);
    	c.setDecimalDigit(precision);
    	c.setNullAble(!mandatory);
    	c.setRemarks(comment);
    	c.setTableName(parentTable);
    	if(StringUtils.isNotEmpty(this.defaultVal)){
    		String typeStr=this.getType().toLowerCase();
    		if(typeStr.startsWith("varchar") || typeStr.startsWith("char")||typeStr.startsWith("text")){
    			c.setColumnDef("'"+this.defaultVal+"'");
    		}else{
    			c.setColumnDef(this.defaultVal);	
    		}
    	}
    	return c;
    }
    
    /**
     * Creates full initialized instance of MetaColumn
     * 
     * @param aCode
     *            code
     * @param aName
     *            name
     * @param aParentTable
     *            name of the table owner
     * @param aMandatory
     *            true if this MetaColumn is mandatory
     * @param aFKParent
     *            Foreign Key parent MetaColumn. It could be null value
     * @param aJClassName
     * @param aLength
     * @param aPrecision
     * @param aComment
     */
    public MetaColumn(String aCode, String aName, String aParentTable,
            boolean aMandatory, MetaColumn aFKParent, String aJClassName,
            int aLength, int aPrecision, String aComment) {
        super();
        code = aCode;
        name = aName;
        parentTable = aParentTable;
        mandatory = aMandatory;
        fkColParent = aFKParent;
        type = aJClassName;
        length = aLength;
        precision = aPrecision;
        comment = aComment;
    }

    /**
     * Returns code of this MetaColumn
     * 
     * @return code.
     */
    public String getCode() {
        return code;
    }

    /**
     * @return comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns foreign key MetaColumn of this MetaColumn
     * 
     * @return fkColParent.
     */
    public MetaColumn getFkColParent() {
        return fkColParent;
    }


    /**
     * @return length.
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns name of this MetaColumn
     * 
     * @return name of this MetaColumn
     */
    public String getName() {
        return name;
    }

    /**
     * @return parentKey.
     */
    public MetaKey getParentKey() {
        return parentKey;
    }

    /**
     * Returns parent MetaTable of this MetaColumn
     * 
     * @return parent MetaTable of this MetaColumn
     */
    public String getParentTable() {
        return parentTable;
    }

    /**
     * @return precision.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Returns "parentTableCode" + "." + "thisColumnCode"
     * 
     * @return "parentTableCode" + "." + "thisColumnCode"
     */
    public String getTableDotColumnCode() {
        return parentTable + "." + code;
    }

    /**
     * Returns true if this MetaColumn is mandatory
     * 
     * @return true if this MetaColumn is mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Returns true if this MetaColumn is part of foreign key
     * 
     * @return true if this MetaColumn is part of foreign key fkColParent !=
     *         null
     */
    public boolean isPartOfFK() {
        return fkColParent != null;
    }

    /**
     * Returns true if this MetaColumn is part of primary key
     * 
     * @return true if this MetaColumn is part of primary key
     */
    public boolean isPartOfPK() {
        return parentKey != null;
    }

    /**
     * @return
     */
    private String recursiveFkHTMLTable() {
        String retVal = "<TABLE border=\"1\">" + "<CAPTION><B>" + parentTable
                + "." + code + "</B></CAPTION>";
        retVal += "<TBODY>";
        retVal += "<TR><TD align=\"right\">" + "<B>name</B>" + "</TD>"
                + "<TD align=\"left\">" + name + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>code</B>" + "</TD>"
                + "<TD align=\"left\">" + code + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>fkColParent</B>" + "</TD>";
        if (fkColParent != null) {
            retVal += "<TD>" + fkColParent.recursiveFkHTMLTable() + "</TD>";
        }
        retVal += "</TBODY></TABLE>";
        return retVal;
    }

    /**
     * Sets code for this MetaColumn
     * 
     * @param aCode
     *            the code to set
     */
    public void setCode(String aCode) {
        code = aCode;
    }

    /**
     * @param aComment
     *            The comment to set.
     */
    public void setComment(String aComment) {
        comment = aComment;
    }

    /**
     * Sets foreign key parent MetaColumn of this MetaColumn
     * 
     * @param aFkColParent
     *            foreign key parent MetaColumn
     */
    public void setFkColParent(MetaColumn aFkColParent) {
        fkColParent = aFkColParent;
    }

    public String getType() {
		return type;
	}
	/**
     * @param aLength
     *            The length to set.
     */
    public void setLength(int aLength) {
        length = aLength;
    }

    /**
     * Sets mandatory column
     * 
     * @param aMandatory
     *            The mandatory to set.
     */
    public void setMandatory(boolean aMandatory) {
        mandatory = aMandatory;
    }

    /**
     * Sets the name for this MetaColumn
     * 
     * @param aName
     *            the name to set.
     */
    public void setName(String aName) {
        name = aName;
    }

    /**
     * @param aParentKey
     *            The parentKey to set.
     */
    public void setParentKey(MetaKey aParentKey) {
        parentKey = aParentKey;
    }

    /**
     * Sets name of parent MetaTable of this MetaColumn
     * 
     * @param aParentTable
     *            the parent MetaTable to set
     */
    public void setParentTable(String aParentTable) {
        parentTable = aParentTable;
    }

    /**
     * @param aPrecision
     *            The precision to set.
     */
    public void setPrecision(int aPrecision) {
        precision = aPrecision;
    }

    /**
     * Returns string representation of this MetaColumn
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }

    /**
     * Returns verbose html description of this MetaColumn
     * 
     * @return verbose html description of this MetaColumn
     */
    public String toStringVerbose() {
        String retVal = "<HTML><TABLE border=\"1\" >"//
                + "<CAPTION><STRONG>" + parentTable + "." + code
                + "</STRONG></CAPTION>";
        retVal += "<TBODY>";
        retVal += "<TR><TD align=\"right\">" + "<B>name</B>" + "</TD>"
                + "<TD align=\"left\">" + name + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>code</B>" + "</TD>"
                + "<TD align=\"left\">" + code + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>parentTable</B>" + "</TD>"
                + "<TD align=\"left\">" + parentTable + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>partOfPK</B>" + "</TD>"
                + "<TD align=\"left\">" + isPartOfPK() + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>mandatory</B>" + "</TD>"
                + "<TD align=\"left\">" + mandatory + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>jClassName</B>" + "</TD>"
                + "<TD align=\"left\">" + type + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>length</B>" + "</TD>"
                + "<TD align=\"left\">" + length + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>precision</B>" + "</TD>"
                + "<TD align=\"left\">" + precision + "</TD></TR>";

        retVal += "<TR><TD align=\"right\">" + "<B>defaultVal</B>" + "</TD>"
                + "<TD align=\"left\">" + defaultVal + "</TD></TR>";

        retVal += "<TR><TD align=\"right\">" + "<B>value-label map</B>"
                + "</TD>";
        Set<Map.Entry<String, String>> entries = lhmListOfVals.entrySet();
        if (!lhmListOfVals.isEmpty()) {
            retVal += "<TD><TABLE border=\"1\"><TBODY>";
            for (Iterator<Map.Entry<String, String>> iter = entries.iterator(); iter
                    .hasNext();) {
                Map.Entry<String, String> entry = iter.next();
                retVal += "<TR><TD align=\"right\">" + "<B>value,label</B>"
                        + "</TD>" + "<TD align=\"left\">" + entry.getKey()
                        + "," + entry.getValue() + "</TD></TR>";
            }
            retVal += "</TBODY></TABLE></TD>";
        }
        retVal += "</TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>comment</B>" + "</TD>"
                + "<TD align=\"left\">"
                + MetaUtils.insertHTMLBreaks(comment, 50) + "</TD></TR>";
        retVal += "<TR><TD align=\"right\">" + "<B>fkColParent</B>" + "</TD>";
        if (fkColParent != null) {
            retVal += "<TD rowspan=\"2\">" + fkColParent.recursiveFkHTMLTable()
                    + "</TD>";
        }
        retVal += "</TBODY></TABLE></HTML>";
        return retVal;
    }

    /**
     * @return defaultVal.
     */
    public String getDefaultVal() {
        return defaultVal;
    }

    /**
     * @param aDefaultVal
     *            The defaultVal to set.
     */
    public void setDefaultVal(String aDefaultVal) {
        defaultVal = aDefaultVal;
    }

    /**
     * Returns Pair(Value,Label) of an ListValues entry.
     * 
     * @see java.util.HashMap#entrySet()
     */
    public Set<Map.Entry<String, String>> entrySet_Value_Label() {
        return lhmListOfVals.entrySet();
    }

    /**
     * @return true if list of values is not empty, otherwise false
     * @see java.util.HashMap#isEmpty()
     */
    public boolean hasListOfVals() {
        return !lhmListOfVals.isEmpty();
    }

    /**
     * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String aValue, String aLabel) {
        return lhmListOfVals.put(aValue, aLabel);
    }
}
