package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.common.PairSS;
import jef.database.Field;
import jef.database.query.BindVariableField;

public class UpdateClause {
	private final List<PairSS> entries = new ArrayList<PairSS>();
	
	private List<Field> variables = new ArrayList<Field>();
	
	public void addEntry(String column,String update){
		entries.add(new PairSS(column,update));
	}
	
	public void addEntry(String column,Field field){
		entries.add(new PairSS(column,"?"));
		variables.add(field);
	}

	public void addField(BindVariableField bindVariableField) {
		variables.add(bindVariableField);
	}

	public String getSql() {
		StringBuilder sb=new StringBuilder(entries.size()*16);
		Iterator<PairSS> iter = entries.iterator();
		if(iter.hasNext()){
			PairSS p=iter.next();
			sb.append(p.first).append(" = ").append(p.second);
		}
		for(;iter.hasNext();){
			PairSS p=iter.next();
			sb.append(", ").append(p.first).append(" = ").append(p.second);
		}
		return sb.toString();
	}

	public List<Field> getVariables() {
		return variables;
	}
	
	public boolean isEmpty(){
		return entries.isEmpty();
	}
}
