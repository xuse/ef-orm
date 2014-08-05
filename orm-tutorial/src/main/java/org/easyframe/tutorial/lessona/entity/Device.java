package org.easyframe.tutorial.lessona.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.DataObject;
import jef.database.KeyFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;
import jef.tools.DateFormats;

@Entity
@PartitionTable(key ={
		@PartitionKey(field = "indexcode",function=KeyFunction.RAW,length=1),
		@PartitionKey(field = "indexcode",function=KeyFunction.MAPPING,
			functionConstructorParams="10-19:datasource1,20-39:datasource2,40-79:datasource3,*:",isDbName=true)
}
)
public class Device extends DataObject {
	/**
	 * 编号
	 */
	@Id
    private String indexcode;
    /**
     * 名称
     */
    private String name;
    /**
     * 类型
     */
    private String type;

    /**
     * 记录创建日期
     */
    private Date createDate;

    public enum Field implements jef.database.Field {
    	indexcode, name, type, createDate
    }
    
  
    public String getIndexcode() {
		return indexcode;
	}

	public void setIndexcode(String indexcode) {
		this.indexcode = indexcode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		if(createDate==null){
			return indexcode+" "+name;
		}else{
			return indexcode+" "+DateFormats.DATE_CS.get().format(createDate)+"  "+name;
		}
	}
}
