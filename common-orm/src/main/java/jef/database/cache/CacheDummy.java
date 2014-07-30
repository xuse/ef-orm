package jef.database.cache;

import java.util.List;

import jef.database.IQueryableEntity;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;


@SuppressWarnings("rawtypes") 
public final class CacheDummy  implements TransactionCache{
	
	static CacheDummy instance=new CacheDummy();
	public static CacheDummy getInstance(){
		return instance;
	}
	private CacheDummy(){
	}
	public boolean contains(Class cls, Object primaryKey) {
		return false;
	}

	public void evict(Class cls, Object primaryKey) {
	}

	public void evict(Class cls) {
	}

	public void evictAll() {
	}

	public void evict(CacheKey cacheKey) {
	}

	public <T> void onLoad(CacheKey sql, List<T> result, Class<T> clz) {
	}
	public void evict(IQueryableEntity cacheKey) {
	}
	public List load(CacheKey sql) {
		return null;
	}

	public void onInsert(IQueryableEntity obj) {
	}

	public void onDelete(String table, String where, List<Object> bind) {
	}
	public void onUpdate(String table, String where, List<Object> bind) {
	}
	public boolean isDummy() {
		return true;
	}
	public void process(Truncate st, List<Object> list) {
		throw new UnsupportedOperationException();
	}
	public void process(Delete st, List<Object> list) {
		throw new UnsupportedOperationException();
	}
	public void process(Insert st, List<Object> list) {
		throw new UnsupportedOperationException();
	}
	public void process(Update st, List<Object> list) {
		throw new UnsupportedOperationException();
	}
	public int getHitCount() {
		return 0;
	}
	public int getMissCount() {
		return 0;
	}
}
