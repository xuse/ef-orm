package jef.database.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;

/**
 * 缓存存储的最后一道
 * 
 * 参数 <-> 缓存
 * @author jiyi
 *
 */
final class DimCacheExpImpl extends DimCache{

	private final com.google.common.cache.Cache<List<?>, List<?>> sqlCache;

	/**
	 * 构造
	 * @param seconds 缓存过期时间，单位秒
	 */
	DimCacheExpImpl(int seconds){
		sqlCache=CacheBuilder.newBuilder().expireAfterWrite(seconds, TimeUnit.SECONDS).build();
	}
	
	/**
	 * 加载缓存
	 * @param params
	 * @return
	 */
	public List<?> load(List<?> params) {
		return sqlCache.getIfPresent(params);
	}

	/**
	 * 删除缓存
	 * @param params
	 */
	public void remove(List<?> params) {
		sqlCache.invalidate(params);
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
		sqlCache.invalidateAll();
	}
	
	
}
