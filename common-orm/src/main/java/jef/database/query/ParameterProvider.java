package jef.database.query;

import java.util.Map;


public interface ParameterProvider {
	Object getNamedParam(String name);
	
	Object getIndexedParam(int index);
	
	boolean containsParam(Object key);
	
	/**
	 * 由Map实现的最简单的参数供应器
	 * @author Administrator
	 *
	 */
	final class MapProvider implements ParameterProvider{
		private Map<String,Object> obj;
		public MapProvider(Map<String,Object> obj){
			this.obj=obj;
		}
		public Object getNamedParam(String name) {
			return obj.get(name);
		}

		public Object getIndexedParam(int index) {
			return obj.get(String.valueOf(index));
		}

		public boolean containsParam(Object key) {
			return obj.containsKey(String.valueOf(key));
		}
	}
}
