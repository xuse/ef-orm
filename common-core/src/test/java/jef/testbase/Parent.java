package jef.testbase;

import java.util.ArrayList;
import java.util.List;

import jef.tools.ArrayUtils;

public class Parent implements Cloneable{
	private int id =1;
	private String name="Jiyio";
	private ArrayList<String> list = new ArrayList<String>();
	private Child child = new Child();
	private List<Child> children = ArrayUtils.asList(new Child());
	private Data data = new Data();
	
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
	public ArrayList<String> getList() {
		return list;
	}
	public void setList(ArrayList<String> list) {
		this.list = list;
	}
	public Child getChild() {
		return child;
	}
	public void setChild(Child child) {
		this.child = child;
	}
	public List<Child> getChildren() {
		return children;
	}
	public void setChildren(List<Child> children) {
		this.children = children;
	}
	public Data getData() {
		return data;
	}
	public void setData(Data data) {
		this.data = data;
	}
}
