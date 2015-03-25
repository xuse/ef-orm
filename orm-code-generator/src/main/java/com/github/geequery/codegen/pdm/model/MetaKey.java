package com.github.geequery.codegen.pdm.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jef.database.meta.PrimaryKey;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author <a href="mailto:nikola.petkov@gmail.com">Nikola Petkov &lt;nikola.petkov@gmail.com&gt;</a>
 */
public class MetaKey{

    protected String code;

    protected List<MetaColumn> columnsList = new ArrayList<MetaColumn>();

    protected String name;

    /**
     *  
     */
    public MetaKey() {
    }

    /**
     * 
     * @param aName
     * @param aCode
     */
    public MetaKey(String aName, String aCode) {
        name = aName;
        code = aCode;
    }

    /**
     * Adds a new MetaColumn into key group
     * 
     * @param aMetaColumn
     */
    public void addColumn(MetaColumn aMetaColumn) {
        columnsList.add(aMetaColumn);
    }

    /**
     * @param aMetaColumn
     * @return true if aMetaColumn is part of this MetaKey
     */
    public boolean containsColumn(MetaColumn aMetaColumn) {
        return columnsList.contains(aMetaColumn);
    }

    /**
     * @param aColCode
     * @return @throws
     *         NullPointerException
     */
    public boolean containsColumn(String aColCode)throws NullPointerException {
    	for(MetaColumn column:this.columnsList){
    		if(aColCode.equals(column.getCode())){
    			return true;
    		}
    	}
        return false;
    }

    /**
     * @return Enumeration of all columns of this MetaKey
     */
    public Collection<MetaColumn> getColumns() {
        return columnsList;
    }

    /**
     * @return code.
     */
    public String getCode() {
        return code;
    }

    /**
     * @return name.
     */
    public String getName() {
        return name;
    }

    @Override
	public String toString() {
		return StringUtils.join(columnsList,',');
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

	public PrimaryKey toJefPk() {
		PrimaryKey key=new PrimaryKey(code);
		String[] columns=new String[this.columnsList.size()];
		for(int i=0;i<columnsList.size();i++){
			columns[i]=columnsList.get(i).getCode();
		}
		key.setColumns(columns);
		return key;
	}
}
