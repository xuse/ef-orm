package jef.common;

/**
 * 轻量级容器
 * @author jiyi
 *
 * @param <F>
 * @param <S>
 */
public class Triple<F,S,T> {
	public F first;
	public S second;
	public T third;
	
	public Triple(F f,S s,T t) {
		first = f;
		second = s;
		third=t;
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

	public T getThird() {
		return third;
	}

	public void setThird(T third) {
		this.third = third;
	}
}
