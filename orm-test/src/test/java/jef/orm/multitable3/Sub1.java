package jef.orm.multitable3;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity()
public class Sub1 extends jef.database.DataObject {

	@Id
	@GeneratedValue
    private int id;

    private int factorId;

    private String name;

    public Sub1() {
    	
    }
    public Sub1(String string) {
    	this.name=string;
	}

	public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFactorId() {
        return factorId;
    }

    public void setFactorId(int factorId) {
        this.factorId = factorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum Field implements jef.database.Field {

        id, factorId, name
    }
}
