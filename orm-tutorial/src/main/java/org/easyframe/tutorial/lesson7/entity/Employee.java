package org.easyframe.tutorial.lesson7.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import jef.database.DataObject;

@Entity
public class Employee extends DataObject {

	@Id
    @Column(columnDefinition = "char(6)", name = "empno")
    private String id;

    private String name;

    @Column(precision=8,scale=2)
    private double salary;
    
    private int bonus;

    public String getId() {
        return id;
    }

    public int getBonus() {
		return bonus;
	}

	public void setBonus(int bonus) {
		this.bonus = bonus;
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

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public enum Field implements jef.database.Field {
        id, name, salary, bonus
    }

	@Override
	public String toString() {
		return name+" salary:"+salary+"  bonus:"+bonus;
	}
    
    
}
