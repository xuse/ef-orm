package jef.database.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存存储的最后一道
 * 
 * 参数 <-> 缓存
 * @author jiyi
 *
 */
final class DimCacheImpl extends DimCache{

	private final Map<List<?>, List<?>> sqlCache = new HashMap<List<?>, List<?>>(10);

	/**
	 * 加载缓存
	 * @param params
	 * @return
	 */
	public List<?> load(List<?> params) {
		return sqlCache.get(params);
	}

	/**
	 * 删除缓存
	 * @param params
	 */
	public void remove(List<?> params) {
		sqlCache.remove(params);
	}

	/**
	 * 将结果放入缓存
	 * @param params 查询参数
	 * @param obj    查询结果（不可修改的List）
	 */
	public void put(List<?> params, List<?> obj) {
		sqlCache.put(params, obj);
	}

	@Override
	public String toString() {
		return sqlCache.toString();
	}

	@Override
	public void clear() {
		sqlCache.clear();
	}
}
