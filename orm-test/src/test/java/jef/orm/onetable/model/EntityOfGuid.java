package jef.orm.onetable.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class EntityOfGuid implements Serializable {
	@Id
	@GeneratedValue
	@Column(length = 32)
	private String id;

	@Column
	private String name;

	@Column
	@Version
	private int version;

	@Column(name = "create_time")
	@GeneratedValue(generator = "created")
	private Date createTime;

	@Column(name = "update_time")
	@GeneratedValue(generator = "modified")
	private Date updateTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public enum Field implements jef.database.Field {
		id, name, version, createTime, updateTime
	}
}
