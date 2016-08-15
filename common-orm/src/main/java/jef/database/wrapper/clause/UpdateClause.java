package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.common.PairSS;
import jef.database.wrapper.variable.Variable;

public class UpdateClause {
	private final List<PairSS> entries = new ArrayList<PairSS>();
	
	private List<Variable> variables = new ArrayList<Variable>();
	
	public void addEntry(String column,String update){
		entries.add(new PairSS(column,update));
	}
	
	public void addEntry(String column,Variable field){
		entries.add(new PairSS(column,"?"));
		variables.add(field);
	}

	public void addField(Variable bindVariableField) {
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

	public List<Variable> getVariables() {
		return variables;
	}
}
