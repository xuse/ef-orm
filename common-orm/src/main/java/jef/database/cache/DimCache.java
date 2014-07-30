package jef.database.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimCache {

	private Map<List<Object>, List<?>> sqlCache = new HashMap<List<Object>, List<?>>(10);

	public List<?> load(List<Object> params) {
		return sqlCache.get(params);
	}

	public void remove(List<Object> params) {
		sqlCache.remove(params);
	}

	public void put(List<Object> params, List<?> obj) {
		sqlCache.put(params, obj);
	}

	@Override
	public String toString() {
		return sqlCache.toString();
	}
	
	
}
