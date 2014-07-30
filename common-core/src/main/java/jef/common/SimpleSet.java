package jef.common;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import jef.common.wrapper.ArrayIterator;
import jef.tools.ArrayUtils;

/**
 * 用List实现的最简单的Set，目标是占用内存最小，不考虑性能，事实上元素不多的情况下性能不是什么问题
 * @param <K>
 * @param <V>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SimpleSet<K> extends AbstractSet<K> implements Set<K>, Serializable {
	private static final long serialVersionUID = -2332764981462364197L;
	private K[] data;
	
	@Override
	public Iterator<K> iterator() {
		return new ArrayIterator<K>(data==null?ArrayUtils.EMPTY_OBJECT_ARRAY:data);
	}

	@Override
	public int size() {
		if(data==null)return 0;
		return data.length;
	}

	@Override
	public boolean add(K e) {
		if(ArrayUtils.contains(data, e)){
			return false;
		}
		data=ArrayUtils.addElement(data, e);
		return true;
	}

	public K[] getData() {
		return data;
	}

	public void setData(K[] data) {
		this.data = data;
	}
}
