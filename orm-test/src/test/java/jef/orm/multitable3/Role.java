package jef.orm.multitable3;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity()
public class Role extends jef.database.DataObject {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int id;

    private String name;

    public Role() {
    }

    public Role(String string) {
        this.name = string;
    }

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
