package org.easyframe.tutorial.lessona.entity;

import java.util.Date;

import javax.persistence.Entity;

import jef.database.DataObject;
import jef.database.KeyFunction;
import jef.database.annotation.PartitionKey;
import jef.database.annotation.PartitionTable;

@Entity
@PartitionTable(key = {
		@PartitionKey(field = "indexcode",function=KeyFunction.RAW,length=2),
		@PartitionKey(field = "indexcode",function=KeyFunction.MAP,
				  functionConstructorParams="10-20:DVR,21-32:SEV,33-567:CAB,*:",isDbName=true)
})
public class Device extends DataObject {
	/**
	 * 编号
	 */
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
}
