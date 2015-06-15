package jef.database.cache;

import java.util.List;

public abstract class DimCache {
	/**
	 * 加载缓存
	 * @param params
	 * @return
	 */
	public abstract List<?> load(List<?> params);

	/**
	 * 删除缓存
	 * @param params
	 */
	public abstract void remove(List<?> params);

	/**
	 * 将结果放入缓存
	 * @param params 查询参数
	 * @param obj    查询结果（不可修改的List）
	 */
	public abstract void put(List<?> params, List<?> obj);
	/**
	 * 清除缓存
	 */
	public abstract void clear();
}
