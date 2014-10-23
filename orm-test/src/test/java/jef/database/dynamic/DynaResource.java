package jef.database.dynamic;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import jef.database.annotation.DynamicPropertyGetter;
import jef.database.annotation.DynamicPropertySetter;
import jef.database.annotation.DynamicTable;
import jef.database.annotation.EasyEntity;

@EasyEntity
@DynamicTable(resourceTypeField = "resourceType")
public class DynaResource extends jef.database.DataObject {

	@Id
	@Column
	@GeneratedValue
    private String indexCode;

	@Column
    private String name;

	@Column
    private int price;

	@Column
    private double elevation;

	@Column
    private int status;

	@Column
    private String resourceType;
    
    private final Map<String,Object> specProps=new HashMap<String,Object>();

	/**
	 * 设置扩展属性
	 * @param prop
	 * @param value
	 */
    @DynamicPropertySetter
	public void setExtendProp(String prop,Object value){
		specProps.put(prop, value);
	}
	
	/**
	 * 获取扩展属性
	 * @param prop
	 * @return
	 */
    @DynamicPropertyGetter
	public Object getExtendProp(String prop){
		return specProps.get(prop);
	}
	
    public DynaResource() {
    }
    public DynaResource(String string) {
    	this.resourceType=string;
	}


	public String getIndexCode() {
        return indexCode;
    }

    public void setIndexCode(String indexCode) {
        this.indexCode = indexCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public enum Field implements jef.database.Field {
        indexCode, name, price, elevation, status, resourceType
    }
}
