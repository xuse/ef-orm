package jef.database.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimCache {

	private Map<List<?>, List<?>> sqlCache = new HashMap<List<?>, List<?>>(10);

	public List<?> load(List<?> params) {
		return sqlCache.get(params);
	}

	public void remove(List<?> params) {
		sqlCache.remove(params);
	}

	public void put(List<?> params, List<?> obj) {
		sqlCache.put(params, obj);
	}

	@Override
	public String toString() {
		return sqlCache.toString();
	}
	
	
}
