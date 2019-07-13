package jef.common;

import  org.apache.commons.lang3.builder.EqualsBuilder;
import  org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 轻量级容器
 * 
 * @author jiyi
 *
 */
public class PairSS {
	public String first;
	public String second;

	public PairSS() {
	}

	public PairSS(String f, String s) {
		first = f;
		second = s;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
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
		if(obj instanceof PairSS) {
			PairSS rhs=(PairSS)obj;
			return new EqualsBuilder().append(first, rhs.first).append(second, rhs.second).isEquals();
		}
		return false;
	}
}
