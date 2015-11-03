package jef.tools.collection;

/**
 * 用于集合元素的过滤器
 * @param <T>
 */
public interface ElementFilter<T> {
	/**
	 * @param obj
	 * @return true 表示不过滤此元素
	 */
	boolean apply(T obj);
}
