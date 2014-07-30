package org.easyframe.tutorial.lesson2.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import jef.database.DataObject;

@Entity
public class UserBalance extends DataObject {
	private double amout;

	@Id
	@GeneratedValue
	private int id;
	
	
	private double todayAmount;
	
	
	private double totalAmount;
	
	private Date updateTime;

	public double getAmout() {
		return amout;
	}

	public void setAmout(double amout) {
		this.amout = amout;
	}

	public int getId() {
		return id;
	}
	
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public void setId(int id) {
		this.id = id;
	}
	public double getTodayAmount() {
		return todayAmount;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public void setTodayAmount(double todayAmount) {
		this.todayAmount = todayAmount;
	}
	public enum Field implements jef.database.Field {
		amout, id, todayAmount,updateTime,totalAmount
	}
}
