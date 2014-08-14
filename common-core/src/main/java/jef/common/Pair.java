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
}
