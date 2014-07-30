package jef.database.dynamic;

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name="URM_SERVICE_1")
public class ServiceItem extends jef.database.DataObject {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
    private int id;

	@Column(name="GROUP_ID")
    private int groupId;

    private String name;

    private String pname;

    private File photo;

    private Boolean flag;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public File getPhoto() {
        return photo;
    }

    public void setPhoto(File photo) {
        this.photo = photo;
    }
    
    public Boolean getFlag() {
		return flag;
	}

	public void setFlag(Boolean flag) {
		this.flag = flag;
	}



	public enum Field implements jef.database.Field {
        id, groupId, name, pname, photo, flag
    }
}
