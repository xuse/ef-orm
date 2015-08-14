package jef.common;

/**
 * 轻量级容器
 * 
 * @author jiyi
 * 
 */
public class PairSO<T> {
	public String first;
	public T second;

	public PairSO() {
	}
	
	public PairSO(String f, T s) {
		first = f;
		second = s;
	}

	/**
	 * 利用Java的泛型擦除机制，将当前对象的second赋值为另一类型，并将当前对象当做另一泛型实例使用。
	 * 
	 * @param v
	 * @return
	 */
	public <V> PairSO<V> replaceSecond(V v) {
		@SuppressWarnings("unchecked")
		PairSO<V> r = (PairSO<V>) this;
		r.second = v;
		return r;
	}

	/**
	 * 将当前对象的泛型参数转换为另一泛型参数.
	 * @return
	 */
	public <V> PairSO<V> cast() {
		@SuppressWarnings("unchecked")
		PairSO<V> r = (PairSO<V>) this;
		return r;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public T getSecond() {
		return second;
	}

	public void setSecond(T second) {
		this.second = second;
	}
	@Override
	public String toString() {
		return first + ":" + second;
	}
}
