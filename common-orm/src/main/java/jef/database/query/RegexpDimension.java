package jef.database.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jef.database.annotation.PartitionFunction;
import jef.tools.string.CharUtils;

public class RegexpDimension implements Dimension{
	private String baseExp;
	
	
	public RegexpDimension(String baseExp){
		this.baseExp=baseExp;
	}
	
	public boolean isValid() {
		return baseExp!=null;
	}

	public Dimension mergeAnd(Dimension d) {
		throw new UnsupportedOperationException();
	}

	public Dimension mergeOr(Dimension d) {
		throw new UnsupportedOperationException();
	}

	public Dimension mergeNot() {
		throw new UnsupportedOperationException();
	}
	@SuppressWarnings("rawtypes")
	public Collection<?> toEnumationValue(Collection<PartitionFunction> function) {
		if(function.size()==1){
			return process(function.iterator().next());
		}
		Set<?> result=new HashSet();
		for(PartitionFunction<?> f:function){
			Collection c=process(f);
			if(c.isEmpty())return c;
			result.addAll(c);
		}
		return result;
	}

	private Collection<?> process(PartitionFunction<?> next) {
		if(next.acceptRegexp()){
			return next.iterator(this);
		}
		List<String> list=new ArrayList<String>(100);
		for(char c: CharUtils.ALPHA_NUM_UNDERLINE){
			list.add(baseExp+c);
		}
		return list;
	}

	public String getBaseExp() {
		return baseExp;
	}
}
