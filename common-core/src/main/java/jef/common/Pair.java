package jef.common;

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
}
