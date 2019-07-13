package jef.common;

import  org.apache.commons.lang3.builder.EqualsBuilder;
import  org.apache.commons.lang3.builder.HashCodeBuilder;

public class PairIS {
	public int first;
	public String second;
	
	public PairIS(int f,String s) {
		first = f;
		second = s;
	}

	public PairIS() {
	}
	
	public int getFirst() {
		return first;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public String getSecond() {
		return second;
	}

	public void setSecond(String second) {
		this.second = second;
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
		if(obj instanceof PairIS) {
			PairIS rhs=(PairIS)obj;
			return new EqualsBuilder().append(first, rhs.first).append(second, rhs.second).isEquals();
		}
		return false;
	}
	
	
}
