package jef.tools.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import jef.common.IntList;

/**
 * 对一个数组的几个局部元素进行排序
 * 
 * @author jiyi
 *
 * @param <T>
 */
public final class LocalSorter<T> {

	private final IntList slots = new IntList();

	private final List<T> data;

	private final List<T> tmpList = new ArrayList<T>();

	/**
	 * 构造局部排序任务
	 * 
	 * @param data
	 */
	public LocalSorter(List<T> data, Predicate<T> p) {
		this.data = data;
		filter(p);
	}

	private int filter(Predicate<T> p) {
		for (int i = 0; i < data.size(); i++) {
			T t = data.get(i);
			if (p.test(t)) {
				slots.add(i);
				tmpList.add(t);
			}
		}
		return slots.size();
	}

	public void sort(Comparator<T> c) {
		Collections.sort(tmpList, c);
		for (int j = 0; j < slots.size(); j++) {
			int index = slots.get(j);
			data.set(index, tmpList.get(j));
		}
	}
}
