package jef.tools.algorithm;

import java.util.Arrays;

/**
 * 常用算法工具BFPRT
 * 线性查找算法
 * 
 * @author jiyi
 * 
 */
public class BFPRT {
	
	/**
	 * 
	 * @param a
	 * @param id
	 * @return
	 */
	public static <T extends Comparable<T>> T get(T[] a, int id) {
		return get(a,0,a.length,id);
		
	}
	/**
	 * BFPRT算法实现—— 在传入的数组中，获得第id个序号的数值。 序号从1开始。 求数组a下标l到r中的第id个数
	 * 
	 * @param a
	 * @param fromIndex
	 * @param toIndex
	 * @param id
	 * @return
	 */
	public static <T extends Comparable<T>> T get(T[] a, int fromIndex, int toIndex, int id) {
		// 小于等于5个数，直接排序得到结果
		if (toIndex - fromIndex <= 5) {
			Arrays.sort(a, fromIndex, toIndex);
			return a[fromIndex + id - 1];
		}

		int t = fromIndex - 1; // 当前替换到前面的中位数的下标
		for (int st = fromIndex, ed; (ed = st + 5) <= toIndex; st += 5) // 每5个进行处理
		{
			Arrays.sort(a, st, ed); // 5个数的排序
			t++;
			swap(a, t, st + 2); // 将中位数替换到数组前面，便于递归求取中位数的中位数
		}

		int pivotId = (fromIndex + t) >> 1; // l到t的中位数的下标，作为主元的下标
		get(a, fromIndex, t, pivotId - fromIndex + 1);// 不关心中位数的值，保证中位数在正确的位置
		int m = partition(a, fromIndex, toIndex, pivotId), cur = m - fromIndex + 1;
		if (id == cur)
			return a[m]; // 刚好是第id个数
		else if (id < cur)
			return get(a, fromIndex, m, id);// 第id个数在左边
		else
			return get(a, m + 1, toIndex, id - cur); // 第id个数在右边
	}

	private static void swap(Object[] a, int t, int i) {
		Object tmp = a[t];
		a[t] = a[i];
		a[i] = tmp;
	}

	// 对数组a下标从l到r的元素进行划分
	private static <T extends Comparable<T>> int partition(T[] a, int l, int r, int pivotId) {
		// 以pivotId所在元素为划分主元
		swap(a, pivotId, --r);
		int j = l - 1; // 左边数字最右的下标
		for (int i = l; i < r; i++)
			if (a[i].compareTo(a[r]) <= 0)
				swap(a, ++j, i);
		swap(a, ++j, r);
		return j;
	}
}
