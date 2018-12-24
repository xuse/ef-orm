package jef.database.cache;

import java.util.List;

import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;

/**
 * 缓存链
 * @author jiyi
 *
 */
@SuppressWarnings("rawtypes")
public final class CacheChain implements Cache{
	private Cache[] chains;
	
	/**
	 * 构造缓存链，本级在前，上级在后
	 * @param chain
	 */
	public CacheChain(Cache... chain) {
		this.chains=chain;
	}

	@Override
	public boolean contains(Class cls, Object primaryKey) {
		for(Cache c: chains) {
			if(c.contains(cls, primaryKey)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void evict(Class cls, Object primaryKey) {
		for(Cache c: chains) {
			c.evict(cls,primaryKey);
		}
	}

	@Override
	public void evict(Class cls) {
		for(Cache c: chains) {
			c.evict(cls);
		}
	}

	@Override
	public void evictAll() {
		for(Cache c: chains) {
			c.evictAll();
		}
	}

	@Override
	public <T> void onLoad(CacheKey key, List<T> result, Class<T> clz) {
		for(Cache c: chains) {
			//重新考虑一下，算不算需要每级缓存都记录？是不是可以是从最顶层缓存起。。。
			c.onLoad(key, result, clz);
		}
		
	}

	@Override
	public List load(CacheKey key) {
		if(key==null)return null;
		for(Cache c: chains) {
			List l=c.load(key);
			if(l!=null) {
				return l;
			}
		}
		return null;
	}

	@Override
	public void evict(CacheKey cacheKey) {
		for(Cache c: chains) {
			c.evict(cacheKey);
		}
		
	}

	@Override
	public void evict(Object cacheKey) {
		for(Cache c: chains) {
			c.evict(cacheKey);
		}
	}

	@Override
	public void onInsert(Object	 obj, String table) {
		for(Cache c: chains) {
			c.onInsert(obj, table);
		}
	}

	@Override
	public void onDelete(String table, String where, List<Object> bind) {
		for(Cache c: chains) {
			//FIXME 这样做造成SQL解析两次，待改进
			c.onDelete(table, where, bind);
		}
	}

	@Override
	public void onUpdate(String table, String where, List<Object> bind) {
		for(Cache c: chains) {
			//FIXME 这样做造成SQL解析两次，待改进
			c.onUpdate(table, where, bind);
		}
	}

	@Override
	public boolean isDummy() {
		for(Cache c: chains) {
			if(!c.isDummy()) {
				return false;
			}
		}
		return true;
		
	}

	@Override
	public void process(Truncate st, List<Object> list) {
		for(Cache c: chains) {
			c.process(st, list);
		}
	}

	@Override
	public void process(Delete st, List<Object> list) {
		for(Cache c: chains) {
			c.process(st, list);
		}
	}

	@Override
	public void process(Insert st, List<Object> list) {
		for(Cache c: chains) {
			c.process(st, list);
		}
	}

	@Override
	public void process(Update st, List<Object> list) {
		for(Cache c: chains) {
			c.process(st, list);
		}
	}

	@Override
	public long getHitCount() {
		long total=0;
		for(Cache c: chains) {
			total+=c.getHitCount();
		}
		return total;
	}

	@Override
	public long getMissCount() {
		long total=0;
		for(Cache c: chains) {
			total+=c.getMissCount();
		}
		return total;
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return this.chains[0].unwrap(cls);
	}

}
