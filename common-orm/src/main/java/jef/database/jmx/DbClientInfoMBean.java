package jef.database.jmx;

/**
 * DbClient的JMX定义
 * @author jiyi
 *
 */
public interface DbClientInfoMBean {
	/**
	 * 各个数据源的名称
	 * @return
	 */
	String getDatasourceNames();

	/**
	 * 是否多数据源
	 * @return
	 */
	boolean isRoutingDbClient();
	/**
	 * 是否已链接
	 * @return
	 */
	boolean isConnected();
	/**
	 * 检查命名查询是否已更新
	 */
	void checkNamedQueryUpdate();
	/**
	 * 清理全局缓存
	 */
	void clearGlobalCache();
}
