package jef.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.accelerator.bean.BeanAccessor;
import jef.common.wrapper.Page;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.TableMetadata;
import jef.database.wrapper.ResultIterator;
import jef.tools.Assert;

public final class PojoWrapper extends DataObject implements Map<String, Object>, VarMeta {
	private TableMetadata meta;
	private Set<String> names;
	private BeanAccessor wrapperAccessor;
	private Object entity;

	public PojoWrapper(Object entity, BeanAccessor metaAccessor, TableMetadata meta, boolean isQuery) {
		this.meta = meta;
		this.wrapperAccessor = metaAccessor;
		this.entity = entity;
		this.names = (Set<String>) metaAccessor.getPropertyNames();
		if (isQuery)
			fillQueryField();
	}

	private void fillQueryField() {
		for (String s : names) {
			Object value = wrapperAccessor.getProperty(entity, s);
			if (wrapperAccessor.getPropertyType(s).isPrimitive()) {
				if(!isDefaultValueOfPromitiveType(wrapperAccessor.getPropertyType(s),value)){
					this.prepareUpdate(meta.getField(s), value, true);
				}
			}else{
				if (value != null) {
					this.prepareUpdate(meta.getField(s), value, true);
				}
			}
		}
		if(!this.needUpdate()){
			this.getQuery().setAllRecordsCondition();
		}
	}

	private boolean isDefaultValueOfPromitiveType(Class<?> javaClass, Object value) {
		if(value==null)return true;
		if (javaClass == Integer.TYPE) {
			return value.equals(0);
		} else if (javaClass == Short.TYPE) {
			return value.equals((short) 0);
		} else if (javaClass == Long.TYPE) {
			return value.equals( 0L);
		} else if (javaClass == Float.TYPE) {
			return value.equals(0f);
		} else if (javaClass == Double.TYPE) {
			return value.equals(0d);
		} else if (javaClass == Byte.TYPE) {
			return value.equals((byte)0);
		} else if (javaClass == Character.TYPE) {
			return value.equals((char)0);
		} else if (javaClass == Boolean.TYPE) {
			return value.equals(false);
		}
		return false;
	}

	public Object get(String key) {
		super.beforeGet(key);
		return wrapperAccessor.getProperty(entity, key);
	}

	public PojoWrapper set(String key, Object value) {
		Field field = meta.getField(key);
		if (field != null && _recordUpdate) {
			super.prepareUpdate(field, value);
		}
		wrapperAccessor.setProperty(entity, key, value);
		return this;
	}

	public Object put(String key, Object value) {
		Assert.notNull(entity);
		Object oldValue = wrapperAccessor.getProperty(entity, key);
		set(key, value);
		return oldValue;
	}

	public int size() {
		return names.size();
	}

	public boolean isEmpty() {
		return false;
	}

	public boolean containsKey(Object key) {
		return names.contains(key);
	}

	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	public Object get(Object key) {
		return get(String.valueOf(key));
	}

	public Object remove(Object key) {
		return set(String.valueOf(key), null);
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public Set<String> keySet() {
		return names;
	}

	public Collection<Object> values() {
		throw new UnsupportedOperationException();
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

	public ITableMetadata meta() {
		return meta;
	}

	public Object get() {
		return entity;
	}

	/**
	 * 将普通类包装为PojoWrapper
	 * @param list
	 * @param isq
	 * @return
	 */
	public static <T> List<PojoWrapper> wrap(List<T> list,boolean isq){
		List<PojoWrapper> result=new ArrayList<PojoWrapper>();
		ITableMetadata meta=MetaHolder.getMeta(list.get(0));
		for(T t:list){
			result.add(meta.transfer(t, isq));
		}
		return result;
		
	}
	
	/**
	 * 将PojoWrapper的列表转封装为T类型的列表
	 * @param wrap
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> unwrapList(List<PojoWrapper> wrap) {
		List<T> result = new ArrayList<T>(wrap.size());
		for (PojoWrapper w : wrap) {
			result.add((T) w.get());
		}
		return result;
	}

	/**
	 * 将PojoWrapper的迭代器转封装为T类型的迭代器
	 * @param result
	 * @return
	 */
	public static <T> ResultIterator<T> unwrapIterator(ResultIterator<PojoWrapper> result) {
		return new  RIWrapper<T>(result);
	}

	/**
	 * 将Wrapper的Page对象转换为真实对象
	 * @param page
	 * @return
	 */
	public static <T> Page<T> unwrapPage(Page<PojoWrapper> page) {
		List<T> list=unwrapList(page.getList());
		return new Page<T>(page.getTotalCount(),list,page.getPageSize());
	};
	
	static class RIWrapper<T> implements ResultIterator<T> {
		RIWrapper(ResultIterator<PojoWrapper> inner) {
			this.inner = inner;
		}

		private ResultIterator<PojoWrapper> inner;

		public boolean hasNext() {
			return inner.hasNext();
		}

		@SuppressWarnings("unchecked")
		public T next() {
			return (T) inner.next().get();
		}

		public void remove() {
			inner.remove();
		}

		public void close(){
			inner.close();
		}
	}

	
}
