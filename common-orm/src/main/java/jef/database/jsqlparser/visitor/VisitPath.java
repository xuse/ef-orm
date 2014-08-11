package jef.database.jsqlparser.visitor;

import java.util.ArrayDeque;
import java.util.Deque;


public class VisitPath {
	final Deque<Object> path= new ArrayDeque<Object>();
	
	public void push(Object current){
		path.push(current);
	}
	public Object pop(){
		return path.pop();
	}
}
