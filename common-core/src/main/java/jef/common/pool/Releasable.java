package jef.common.pool;



/**
 * 可释放的
 * @author jiyi
 *
 */
public interface Releasable {

	/**
	 * 收缩，关闭多余的连接。具体实现取决于实现内部。
	 * 这个方法是让连接池尽量少占数据库资源
	 */
	void releaseTillMinSize();
	
	/**
	 * 获得对象池状态
	 * @return
	 */
	PoolStatus getStatus();
}
