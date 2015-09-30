package jef.common;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Objects;

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
	public int hashCode() {
		return new HashCodeBuilder().append(first).append(second).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Pair){
			Pair<?,?> rhs=(Pair<?,?>)obj;
			if(!Objects.equal(first, rhs.first)){
				return false;
			}
			return Objects.equal(this.second, rhs.second);
		}
		return super.equals(obj);
	}
}
