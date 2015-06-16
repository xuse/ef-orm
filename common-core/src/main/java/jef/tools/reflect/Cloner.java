package jef.tools.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("rawtypes")
public abstract class Cloner {
	abstract public Object clone(Object object, int restLevel);

	public static final Cloner RAW = new Cloner() {
		@Override
		public Object clone(Object object, int restLevel) {
			return object;
		}
	};
	public static final Cloner DATE = new Cloner() {
		@Override
		public Object clone(Object object, int restLevel) {
			Date date = (Date) object;
			return new java.util.Date(date.getTime());
		}
	};
	public static final Cloner TIMESTAMP = new Cloner() {
		@Override
		public Object clone(Object object, int restLevel) {
			Timestamp ts = (Timestamp) object;
			return new java.sql.Timestamp(ts.getTime());
		}
	};
	public static final Cloner TIME = new Cloner() {
		@Override
		public Object clone(Object object, int restLevel) {
			Time time = (Time) object;
			return new java.sql.Time(time.getTime());
		}
	};
	public static final Cloner SQL_DATE = new Cloner() {
		@Override
		public Object clone(Object object, int restLevel) {
			java.sql.Date time = (java.sql.Date) object;
			return new java.sql.Date(time.getTime());
		}
	};

	static final Cloner _ArrayList = new Cloner() {
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, int restLevel) {
			List source = (List) object;
			ArrayList list = new ArrayList(source.size());
			for (Object obj : source) {
				list.add(CloneUtils._clone(obj, restLevel - 1));
			}
			return list;
		}
	};

	static final Cloner _HashSet = new Cloner() {
		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, int restLevel) {
			Set source = (Set) object;
			Set list = new HashSet(source.size());
			for (Object obj : source) {
				list.add(CloneUtils._clone(obj, restLevel - 1));
			}
			return list;
		}
	};

	static final Cloner _HashMap = new Cloner() {
		@Override
		@SuppressWarnings("unchecked")
		public Object clone(Object object, int restLevel) {
			Map source = (Map) object;
			Map map = new HashMap(source.size());
			Set<Map.Entry> entries = source.entrySet();
			restLevel--;
			for (Map.Entry entry : entries) {
				map.put(CloneUtils._clone(entry.getKey(), restLevel), CloneUtils._clone(entry.getValue(), restLevel));
			}
			return map;
		}
	};

	static final Cloner _Array = new Cloner() {
		@Override
		public Object clone(Object obj, int restLevel) {
			int len = Array.getLength(obj);
			Class<?> priType = obj.getClass().getComponentType();
			Object clone = Array.newInstance(priType, len);
			if (priType.isPrimitive()) {
				System.arraycopy(obj, 0, clone, 0, len);
			} else {
				for (int i = 0; i < len; i++) {
					Array.set(clone, i, CloneUtils._clone(Array.get(obj, i), restLevel - 1));
				}
			}
			return clone;
		}
	};

	/**
	 * 通用的Map克隆器。 传入一个带空构造的Collection实现类即可。（非抽象）
	 */
	static final class _OtherCollection extends Cloner {
		private Class clz;

		public _OtherCollection(Class clz) {
			if (clz == null || Modifier.isAbstract(clz.getModifiers())) {
				throw new IllegalArgumentException();
			}
			this.clz = clz;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, int restLevel) {
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
	 * 通用的Map克隆器。 传入一个带空构造的Map实现类即可。（非抽象）
	 */
	static final class _OtherMap extends Cloner {
		private Class clz;

		public _OtherMap(Class clz) {
			if (clz == null || Modifier.isAbstract(clz.getModifiers())) {
				throw new IllegalArgumentException();
			}
			this.clz = clz;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object clone(Object object, int restLevel) {
			Map source = (Map) object;
			try {
				Map map = (Map) clz.newInstance();
				Set<Map.Entry> entries = source.entrySet();
				restLevel--;
				for (Map.Entry entry : entries) {
					map.put(CloneUtils._clone(entry.getKey(), restLevel), CloneUtils._clone(entry.getValue(), restLevel));
				}
				return map;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
