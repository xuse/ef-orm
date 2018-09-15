package jef.tools.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import jef.common.IntList;
import jef.common.Pair;

/**
 * 对一个数组的几个局部元素进行排序
 * 
 * @author jiyi
 *
 * @param <T>
 */
public class LocalMappedSorter<T, S> {

	private final IntList slots = new IntList();

	private final List<T> data;

	private final List<Pair<T, S>> tmpList = new ArrayList<>();

	/**
	 * 转换器
	 */
	private Function<T, S> mapper;

	/**
	 * 过滤器1
	 */
	private Predicate<T> filter1 = e -> true;

	/**
	 * 过滤器2
	 */
	private Predicate<S> filter2 = e -> true;

	/**
	 * 构造局部排序任务
	 * 
	 * @param data
	 */
	public LocalMappedSorter(List<T> data, Function<T, S> mapper) {
		this.data = data;
		this.mapper = mapper;
	}

	public void filterBefore(Predicate<T> f) {
		this.filter1 = f;
	}

	public void filterAfter(Predicate<S> f) {
		this.filter2 = f;
	}

	public void map(Function<T, S> f) {
		this.mapper = f;
	}

	public void sort(Comparator<S> c) {
		for (int i = 0; i < data.size(); i++) {
			T t = data.get(i);
			if (!filter1.test(t)) {
				continue;
			}
			S s = mapper.apply(t);
			if (filter2.test(s)) {
				slots.add(i);
				tmpList.add(new Pair<T, S>(t, s));
			}
		}
		Collections.sort(tmpList, (s1, s2) -> c.compare(s1.second, s2.second));
		for (int j = 0; j < slots.size(); j++) {
			int index = slots.get(j);
			data.set(index, tmpList.get(j).first);
		}
	}
}
