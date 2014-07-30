package jef.common.pool;

/**
 * 对象池工场
 * @author jiyi
 *
 * @param <T>
 */
public interface ObjectFactory<T> {
	/**
	 * 检查对象是否有效，如果无效的话丢弃并创建一个新对象代替原对象。
	 * @param conn
	 * @return
	 */
	T ensureOpen(T obj);
	

	/**
	 * 释放对象
	 * @param conn
	 */
	void release(T obj);

	/**
	 * 创建对象
	 * @return
	 */
	T create();
}
