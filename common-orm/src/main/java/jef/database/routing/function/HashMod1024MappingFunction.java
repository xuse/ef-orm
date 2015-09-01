package jef.database.routing.function;

import java.util.Collection;
import java.util.Collections;

import jef.database.annotation.PartitionFunction;
import jef.database.query.RegexpDimension;
import shaded.org.apache.commons.lang3.StringUtils;

public class HashMod1024MappingFunction implements PartitionFunction<String>{

	public HashMod1024MappingFunction(String string) {
		// TODO Auto-generated constructor stub
	}

	public HashMod1024MappingFunction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String eval(String value) {
		int buck=value==null?0:(value.hashCode()%1024);
		return getResult(buck);
	}
	
	private Collection<String> allModulus() {
		return null;
	}
	

	private String getResult(int buck) {
		
		return null;
	}

	@Override
	public Collection<String> iterator(String min, String max, boolean left, boolean right) {
		//由于
		if(min==null || max==null){
			return allModulus();
		}
		if(StringUtils.equals(min, max)) {
			if(!left && left==right) {
				return Collections.emptyList();
			}else {
				return Collections.singletonList(eval(min));
			}
		}
		return allModulus();
	}



	@Override
	public boolean acceptRegexp() {
		return false;
	}

	@Override
	public Collection<String> iterator(RegexpDimension regexp) {
		throw new UnsupportedOperationException();
	}

}
