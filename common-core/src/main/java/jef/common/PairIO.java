package jef.common;

import  org.apache.commons.lang3.builder.EqualsBuilder;
import  org.apache.commons.lang3.builder.HashCodeBuilder;

public class PairIO<T> {
	public int first;
	public T second;
	
	public PairIO(int f,T s) {
		first = f;
		second = s;
	}

	@Override
	public String toString() {
		return first+":"+second;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(first).append(second).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PairIO) {
			PairIO<?> rhs=(PairIO<?>)obj;
			return new EqualsBuilder().append(first, rhs.first).append(second, rhs.second).isEquals();
		}
		return false;
	}
}
