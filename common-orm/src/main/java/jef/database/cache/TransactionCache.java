package jef.database.cache;

import java.util.List;

import javax.persistence.Cache;

import jef.database.IQueryableEntity;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;

/**
 * 以事务为生命周期的缓存，Level 1 Cache
 * @author Jiyi
 *
 */
public interface TransactionCache extends Cache{
	/**
	 * Store the results into cache.
	 * @param key Key of cache.
	 * @param result value of cache
	 * @param clz
	 */
	public <T> void onLoad(CacheKey key,List<T> result,Class<T> clz);

	/**
	 * load from cache.
	 * @param key the Key of cache
	 * @return null if no value cached.
	 */
	@SuppressWarnings("rawtypes")
	public List load(CacheKey key);

	/**
	 * remove from cache
	 * @param cacheKey
	 */
	public void evict(CacheKey cacheKey);
	
	/**
	 * remove all cache data of the type.
	 * @param cacheKey
	 */
	public void evict(IQueryableEntity cacheKey);

	/**
	 * on object insert , store the cache
	 * @param obj
	 * @param table 自定义表名，无特殊情况时传入null即可
	 */
	public void onInsert(IQueryableEntity obj,String table);

	/**
	 * on object delete, remove the cache
	 * @param table
	 * @param where
	 * @param bind
	 */
	public void onDelete(String table, String where, List<Object> bind);

	/**
	 * on object update, refresh cache.
	 * @param table
	 * @param where
	 * @param bind
	 */
	public void onUpdate(String table, String where, List<Object> bind);
	/**
	 * is this cache a dummy cache( no cache)
	 * @return
	 */
	public boolean isDummy();
	/**
	 * trigger while a truncat sql executed.
	 * @param st
	 * @param list
	 */
	public void process(Truncate st, List<Object> list);

	/**
	 * trigger while a delete sql executed.
	 * @param st
	 * @param list
	 */
	public void process(Delete st, List<Object> list);

	/**
	 * trigger while a insert sql executed.
	 * @param st
	 * @param list
	 */
	public void process(Insert st, List<Object> list);

	/**
	 * trigger while a update sql executed.
	 * @param st
	 * @param list
	 */
	public void process(Update st, List<Object> list);
	
	int getHitCount();
	
	int getMissCount();
}
