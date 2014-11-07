package jef.tools.reflect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("rawtypes")
public abstract class Cloner {
	abstract public Object clone(Object object,boolean deep);
	
	public static final Cloner RAW=new Cloner(){
		@Override
		public Object clone(Object object, boolean deep) {
			return object;
		}
		
	};
	
	static class _ArrayList extends Cloner{
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object,boolean deep) {
			List source=(List)object;
			ArrayList list=new ArrayList(source.size());
			for(Object obj:source){
				list.add(CloneUtils.clone(obj));
			}
			return list;
		}
	}
	
	static class _HashSet extends Cloner{
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object,boolean deep) {
			Set source=(Set)object;
			Set list=new HashSet(source.size());
			for(Object obj:source){
				list.add(CloneUtils.clone(obj));
			}
			return list;
		}
	}
	
	static class _HashMap extends Cloner{
		@Override
		@SuppressWarnings("unchecked")
		public Object clone(Object object,boolean deep) {
			Map source =(Map)object;
			Map map=new HashMap(source.size());
			Set<Map.Entry> entries=source.entrySet();
			for(Map.Entry key: entries){
				map.put(CloneUtils.clone(key.getKey()),CloneUtils.clone(key.getValue()));
			}
			return map;
		}
		
	}
}
