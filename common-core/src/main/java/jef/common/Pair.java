package jef.common;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 轻量级容器
 * @author jiyi
 *
 * @param <F>
 * @param <S>
 */
public class Pair<F,S> {
	public F first;
	public S second;
	
	public Pair(F f,S s) {
		first = f;
		second = s;
	}

	public Pair() {
	}
	public F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
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
		if(obj instanceof Pair) {
			Pair<?,?> rhs=(Pair<?,?>)obj;
			return new EqualsBuilder().append(first, rhs.first).append(second, rhs.second).isEquals();
		}
		return false;
	}
}
