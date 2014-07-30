package jef.orm.exists.model;

import javax.persistence.Entity;

@Entity()
public class TableB extends jef.database.DataObject {
    private Integer id;
    
    public TableB(){};
    public TableB(int id){
    	this.id=id;
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public enum Field implements jef.database.Field {
        id
    }
}
