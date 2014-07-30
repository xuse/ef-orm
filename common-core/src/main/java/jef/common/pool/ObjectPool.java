package jef.common.pool;

import java.sql.SQLException;

/**
 * 通用的对象池。
 * 
 * @author jiyi
 * 
 * @param <T>
 */
public interface ObjectPool<T> {
	/**
	 * 获取池中的对象
	 * 
	 * @return
	 * @throws SQLException
	 */
	public T poll();

	/**
	 * 归还池中的对象
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public void offer(T conn);

	/**
	 * 关闭池，释放全部资源
	 */
	public void closePool();
}
