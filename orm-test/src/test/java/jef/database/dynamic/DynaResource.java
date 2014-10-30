package jef.database.dynamic;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import jef.database.EntityExtensionSupport;
import jef.database.annotation.DynamicTable;
import jef.database.annotation.EasyEntity;

@EasyEntity
@DynamicTable(resourceTypeField = "resourceType")
public class DynaResource extends EntityExtensionSupport{

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
	
	@ManyToOne()
	@JoinColumn(name="status",referencedColumnName="code")
	private Status statusObj;

	@Column
    private String resourceType;
 
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
    
    public Status getStatusObj() {
		return statusObj;
	}
	public void setStatusObj(Status statusObj) {
		this.statusObj = statusObj;
	}



	public enum Field implements jef.database.Field {
        indexCode, name, price, elevation, status, resourceType
    }
}
