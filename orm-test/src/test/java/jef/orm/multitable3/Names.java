package jef.orm.multitable3;

import javax.persistence.Entity;

@Entity()
public class Names extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	private int id;

	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public enum Field implements jef.database.Field {
		id, name
	}
}
