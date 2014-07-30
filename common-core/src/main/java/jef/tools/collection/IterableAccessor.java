package jef.tools.collection;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

public class IterableAccessor<T> implements Iterable<T>{
	private Object arrayOrCollection;
	private int len;
	public IterableAccessor(Object obj){
		this.arrayOrCollection=obj;
		this.len=CollectionUtil.length(obj);
	}
	
	public int length(){
		return len;
	}
	
	public boolean isEmpty(){
		return len==0;
	}

	@Override
	public String toString() {
		Iterator<T> iter=iterator();
		StringBuilder sb=new StringBuilder();
		if(iter.hasNext()){
			sb.append(StringUtils.toString(iter.next()));
		}
		for(;iter.hasNext();){
			sb.append(',').append(StringUtils.toString(iter.next()));
		}
		return sb.toString();
	}

	/**
	 * 传入一个Object，如果这个Object是可以遍历的，那么遍历它 否则返回null
	 * 
	 * @param arrayOrCollection
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Iterator<T> iterator() {
		if (arrayOrCollection.getClass().isArray()) {
			final Object[] array = ArrayUtils.toObject(arrayOrCollection);
			return new Iterator<T>() {
				private int i = 0;

				public boolean hasNext() {
					return i < array.length;
				}

				public T next() {
					T r = (T)array[i];
					i++;
					return r;
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		} else if (arrayOrCollection instanceof Collection) {
			return ((Collection) arrayOrCollection).iterator();
		} else if (arrayOrCollection instanceof Enumeration) {
			return ArrayUtils.toIterable((Enumeration) arrayOrCollection).iterator();
		} else {
			return null;
		}
	}
}
