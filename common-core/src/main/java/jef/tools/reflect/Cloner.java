package jef.tools.reflect;

import java.lang.reflect.Modifier;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("rawtypes")
public abstract class Cloner {
	abstract public Object clone(Object object, boolean deep);

	public static final Cloner RAW = new Cloner() {
		@Override
		public Object clone(Object object, boolean deep) {
			return object;
		}
	};
	public static final Cloner DATE = new Cloner() {
		@Override
		public Object clone(Object object, boolean deep) {
			Date date = (Date) object;
			return new java.util.Date(date.getTime());
		}
	};
	public static final Cloner TIMESTAMP = new Cloner() {
		@Override
		public Object clone(Object object, boolean deep) {
			Timestamp ts = (Timestamp) object;
			return new java.sql.Timestamp(ts.getTime());
		}
	};
	public static final Cloner TIME = new Cloner() {
		@Override
		public Object clone(Object object, boolean deep) {
			Time time = (Time) object;
			return new java.sql.Time(time.getTime());
		}
	};
	public static final Cloner SQL_DATE = new Cloner() {
		@Override
		public Object clone(Object object, boolean deep) {
			java.sql.Date time = (java.sql.Date) object;
			return new java.sql.Date(time.getTime());
		}
	};

	static class _ArrayList extends Cloner {
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, boolean deep) {
			List source = (List) object;
			ArrayList list = new ArrayList(source.size());
			for (Object obj : source) {
				list.add(CloneUtils.clone(obj,deep));
			}
			return list;
		}
	}

	/**
	 * 通用的Map克隆器。
	 * 传入一个带空构造的Collection实现类即可。（非抽象）
	 */
	static class _OtherCollection extends Cloner {
		private Class clz;

		public _OtherCollection(Class clz) {
			if(clz==null || Modifier.isAbstract(clz.getModifiers())){
				throw new IllegalArgumentException();
			}
			this.clz = clz;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, boolean deep) {
			Collection source = (Collection) object;
			try {
				Collection list = (Collection) clz.newInstance();
				for (Object obj : source) {
					list.add(CloneUtils.clone(obj));
				}
				return list;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * 通用的Map克隆器。
	 * 传入一个带空构造的Map实现类即可。（非抽象）
	 */
	static class _OtherMap extends Cloner {
		private Class clz;
		
		public _OtherMap(Class clz){
			if(clz==null || Modifier.isAbstract(clz.getModifiers())){
				throw new IllegalArgumentException();
			}
			this.clz=clz;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, boolean deep) {
			Map source=(Map)object;
			try {
				Map map = (Map) clz.newInstance();
				Set<Map.Entry> entries = source.entrySet();
				for (Map.Entry entry : entries) {
					map.put(CloneUtils.clone(entry.getKey(),deep), CloneUtils.clone(entry.getValue(),deep));
				}
				return map;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
	
	static class _HashSet extends Cloner {
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, boolean deep) {
			Set source = (Set) object;
			Set list = new HashSet(source.size());
			for (Object obj : source) {
				list.add(CloneUtils.clone(obj,deep));
			}
			return list;
		}
	}

	static class _HashMap extends Cloner {
		@Override
		@SuppressWarnings("unchecked")
		public Object clone(Object object, boolean deep) {
			Map source = (Map) object;
			Map map = new HashMap(source.size());
			Set<Map.Entry> entries = source.entrySet();
			for (Map.Entry entry : entries) {
				map.put(CloneUtils.clone(entry.getKey(),deep), CloneUtils.clone(entry.getValue(),deep));
			}
			return map;
		}

	}
}
