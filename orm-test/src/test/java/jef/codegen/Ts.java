package jef.codegen;

import javax.persistence.Entity;

@Entity
public class Ts extends jef.database.DataObject {

    private String a;

    private int x;

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public enum Field implements jef.database.Field {
        a, x
    }
}
