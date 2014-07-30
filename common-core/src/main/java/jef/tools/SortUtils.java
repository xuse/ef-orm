/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools;

/**
 * 排序工具
 * 
 * @author jiyi
 * 
 */
public class SortUtils {
	/**
	 * 排序(快速排序)
	 * 
	 * @param data
	 */
	public static <T extends Comparable<T>> void sort(T[] data) {
		sort(data, ALGORITHM_IMPROVED_QUICK);
	}

	/**
	 * 用指定的算法排序
	 * 
	 * @param data
	 * @param algorithm
	 */
	public static <T extends Comparable<T>> void sort(T[] data, int algorithm) {
		impl[algorithm - 1].sort(data);
	}

	/**
	 * 得到算法名称
	 * 
	 * @param algorithm
	 * @return
	 */
	public static String toAlgorithmName(int algorithm) {
		return name[algorithm - 1];
	}

	/** 插入排序 */
	public final static int ALGORITHM_INSERT = 1;
	/** 冒泡排序 */
	public final static int ALGORITHM_BUBBLE = 2;
	/** 选择排序 */
	public final static int ALGORITHM_SELECTION = 3;
	/** 希尔排序 */
	public final static int ALGORITHM_SHELL = 4;
	/**快速排序 */
	public final static int ALGORITHM_QUICK = 5;
	/** 快速排序 改 */
	public final static int ALGORITHM_IMPROVED_QUICK = 6;
	/** 归并排序  */
	public final static int ALGORITHM_MERGE = 7;
	/** 归并排序 改 */
	public final static int ALGORITHM_IMPROVED_MERGE = 8;
	/** 堆排序 */
	public final static int ALGORITHM_HEAP = 9;

	private static String[] name = { "insert",
		"bubble", "selection", "shell", "quick", 
		"improved_quick", "merge", "improved_merge", "heap" };
	
	private static Sort[] impl = new Sort[] { 
		new InsertSort(), new BubbleSort(), new SelectionSort(), 
		new ShellSort(), new QuickSort(), new ImprovedQuickSort(), 
		new MergeSort(), new ImprovedMergeSort(), new HeapSort()
	};

	static interface Sort {
		public <T extends Comparable<T>> void sort(T[] data);
	}

	static void swap(Comparable<?>[] data, int i, int j) {
		Comparable<?> temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}
}

// 插入排序：
class InsertSort implements SortUtils.Sort {
	public <T extends Comparable<T>> void sort(T[] data) {
		// int temp;
		for (int i = 1; i < data.length; i++) {
			for (int j = i; (j > 0) && (data[j].compareTo(data[j - 1]) < 0); j--) {
				SortUtils.swap(data, j, j - 1);
			}
		}
	}
}

// 冒泡排序：
class BubbleSort implements SortUtils.Sort {
	public <T extends Comparable<T>> void sort(T[] data) {
		for (int i = 0; i < data.length; i++) {
			for (int j = data.length - 1; j > i; j--) {
				if (data[j].compareTo(data[j - 1]) < 0) {
					SortUtils.swap(data, j, j - 1);
				}
			}
		}
	}

}

// 选择排序：
class SelectionSort implements SortUtils.Sort {
	public <T extends Comparable<T>> void sort(T[] data) {
		// int temp;
		for (int i = 0; i < data.length; i++) {
			int lowIndex = i;
			for (int j = data.length - 1; j > i; j--) {
				if (data[j].compareTo(data[lowIndex]) < 0) {
					lowIndex = j;
				}
			}
			SortUtils.swap(data, i, lowIndex);
		}
	}
}

// Shell排序：
class ShellSort implements SortUtils.Sort {
	public <T extends Comparable<T>> void sort(T[] data) {
		for (int i = data.length / 2; i > 2; i /= 2) {
			for (int j = 0; j < i; j++) {
				insertSort(data, j, i);
			}
		}
		insertSort(data, 0, 1);
	}

	private <T extends Comparable<T>> void insertSort(T[] data, int start, int inc) {
		// int temp;
		for (int i = start + inc; i < data.length; i += inc) {
			for (int j = i; (j >= inc) && (data[j].compareTo(data[j - inc]) < 0); j -= inc) {
				SortUtils.swap(data, j, j - inc);
			}
		}
	}
}

// 快速排序：
class QuickSort implements SortUtils.Sort {
	public <T extends Comparable<T>> void sort(T[] data) {
		quickSort(data, 0, data.length - 1);
	}

	private <T extends Comparable<T>> void quickSort(T[] data, int i, int j) {
		int pivotIndex = (i + j) / 2;
		// swap
		SortUtils.swap(data, pivotIndex, j);

		int k = partition(data, i - 1, j, data[j]);
		SortUtils.swap(data, k, j);
		if ((k - i) > 1)
			quickSort(data, i, k - 1);
		if ((j - k) > 1)
			quickSort(data, k + 1, j);
	}

	private <T extends Comparable<T>> int partition(T[] data, int l, int r, T pivot) {
		do {
			while (data[++l].compareTo(pivot) < 0)
				;
			while ((r != 0) && data[--r].compareTo(pivot) > 0)
				;
			SortUtils.swap(data, l, r);
		} while (l < r);
		SortUtils.swap(data, l, r);
		return l;
	}
}

// 改进后的快速排序：(默认)
class ImprovedQuickSort implements SortUtils.Sort {
	private static int MAX_STACK_SIZE = 4096;
	private static int THRESHOLD = 10;

	public <T extends Comparable<T>> void sort(T[] data) {
		int[] stack = new int[MAX_STACK_SIZE];

		int top = -1;
		T pivot;
		int pivotIndex, l, r;

		stack[++top] = 0;
		stack[++top] = data.length - 1;

		while (top > 0) {
			int j = stack[top--];
			int i = stack[top--];

			pivotIndex = (i + j) / 2;
			pivot = data[pivotIndex];

			SortUtils.swap(data, pivotIndex, j);

			// partition
			l = i - 1;
			r = j;
			do {
				while (data[++l].compareTo(pivot) < 0)
					;
				while ((r != 0) && (data[--r].compareTo(pivot) > 0))
					;
				SortUtils.swap(data, l, r);
			} while (l < r);
			SortUtils.swap(data, l, r);
			SortUtils.swap(data, l, j);

			if ((l - i) > THRESHOLD) {
				stack[++top] = i;
				stack[++top] = l - 1;
			}
			if ((j - l) > THRESHOLD) {
				stack[++top] = l + 1;
				stack[++top] = j;
			}

		}
		// new InsertSort().sort(data);
		insertSort(data);
	}

	private <T extends Comparable<T>> void insertSort(T[] data) {
		// int temp;
		for (int i = 1; i < data.length; i++) {
			for (int j = i; (j > 0) && (data[j].compareTo(data[j - 1]) < 0); j--) {
				SortUtils.swap(data, j, j - 1);
			}
		}
	}
}

// 归并排序：
class MergeSort implements SortUtils.Sort {
	@SuppressWarnings("unchecked")
	public <T extends Comparable<T>> void sort(T[] data) {
		T[] temp = (T[])new Comparable[data.length];
		mergeSort(data, temp, 0, data.length - 1);
	}

	private <T extends Comparable<T>> void mergeSort(T[] data, T[] temp, int l, int r) {
		int mid = (l + r) / 2;
		if (l == r)
			return;
		mergeSort(data, temp, l, mid);
		mergeSort(data, temp, mid + 1, r);
		for (int i = l; i <= r; i++) {
			temp[i] = data[i];
		}
		int i1 = l;
		int i2 = mid + 1;
		for (int cur = l; cur <= r; cur++) {
			if (i1 == mid + 1)
				data[cur] = temp[i2++];
			else if (i2 > r)
				data[cur] = temp[i1++];
			else if (temp[i1].compareTo(temp[i2]) < 0)
				data[cur] = temp[i1++];
			else
				data[cur] = temp[i2++];
		}
	}
}

// 改进后的归并排序:
@SuppressWarnings("unchecked")
class ImprovedMergeSort implements SortUtils.Sort {
	private static final int THRESHOLD = 10;
	private <T extends Comparable<T>> void mergeSort(T[] data, T[] temp, int l, int r) {
		int i, j, k;
		int mid = (l + r) / 2;
		if (l == r)
			return;
		if ((mid - l) >= THRESHOLD)
			mergeSort(data, temp, l, mid);
		else
			insertSort(data, l, mid - l + 1);
		if ((r - mid) > THRESHOLD)
			mergeSort(data, temp, mid + 1, r);
		else
			insertSort(data, mid + 1, r - mid);

		for (i = l; i <= mid; i++) {
			temp[i] = data[i];
		}
		for (j = 1; j <= r - mid; j++) {
			temp[r - j + 1] = data[j + mid];
		}
		T a = temp[l];
		T b = temp[r];
		for (i = l, j = r, k = l; k <= r; k++) {
			if (a.compareTo(b) < 0) {
				data[k] = temp[i++];
				a = temp[i];
			} else {
				data[k] = temp[j--];
				b = temp[j];
			}
		}
	}

	private <T extends Comparable<T>> void insertSort(T[] data, int start, int len) {
		for (int i = start + 1; i < start + len; i++) {
			for (int j = i; (j > start) && data[j].compareTo(data[j - 1]) < 0; j--) {
				SortUtils.swap(data, j, j - 1);
			}
		}
	}

	public <T extends Comparable<T>> void sort(T[] data) {
		T[] temp = (T[])new Comparable[data.length];
		mergeSort(data, temp, 0, data.length - 1);
	}
}

// 堆排序：
@SuppressWarnings("unchecked")
class HeapSort implements SortUtils.Sort {

	public <T extends Comparable<T>> void sort(T[] data) {
		MaxHeap<T> h = new MaxHeap<T>();
		h.init(data);
		for (int i = 0; i < data.length; i++)
			h.remove();
		System.arraycopy(h.queue, 1, data, 0, data.length);

	}

	private static class MaxHeap<T extends Comparable<T>> {
		void init(T[] data) {
			this.queue = (T[]) new Comparable[data.length + 1];
			for (int i = 0; i < data.length; i++) {
				queue[++size] = data[i];
				fixUp(size);
			}
		}

		private int size = 0;

		private T[] queue;

		public void remove() {
			SortUtils.swap(queue, 1, size--);
			fixDown(1);
		}

		// fixdown
		private void fixDown(int k) {
			int j;
			while ((j = k << 1) <= size) {
				if (j < size && queue[j].compareTo(queue[j + 1]) < 0)
					j++;
				if (queue[k].compareTo(queue[j]) > 0) // 不用交换
					break;
				SortUtils.swap(queue, j, k);
				k = j;
			}
		}

		private void fixUp(int k) {
			while (k > 1) {
				int j = k >> 1;
				if (queue[j].compareTo(queue[k]) > 0)
					break;
				SortUtils.swap(queue, j, k);
				k = j;
			}
		}
	}
}
