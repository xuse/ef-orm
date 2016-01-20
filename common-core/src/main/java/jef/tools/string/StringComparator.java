package jef.tools.string;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 字符串排序器，目的是处理字符串和数字混杂时的，对于位于相同位置的数字串，按数字大小排序，而不是按其ASCII编码排序
 * @see java.util.Comparator
 */
public final class StringComparator implements Comparator<String> {
	public static final StringComparator INSTANCE = new StringComparator();

	@Override
	public int compare(String o1, String o2) {
		int len1 = o1.length();
		int len2 = o2.length();
		int lim = Math.min(len1, len2);
		int j = 0;
		int k = 0;
		StringBuilder numCache1 = new StringBuilder();
		StringBuilder numCache2 = new StringBuilder();
		while (k < lim && j<lim) {
			char c1 = o1.charAt(j++);
			char c2 = o2.charAt(k++);
			if (c1 != c2) {
				if (CharUtils.isNumber(c1) && CharUtils.isNumber(c2)) {
					while (CharUtils.isNumber(c1)) {
						numCache1.append(c1);
						if (j == len1)
							break;
						c1 = o1.charAt(j++);
					}
					while (CharUtils.isNumber(c2)) {
						numCache2.append(c2);
						if (k == len2)
							break;
						c2 = o2.charAt(k++);
					}
					long n1 = Long.valueOf(numCache1.toString());
					long n2 = Long.valueOf(numCache2.toString());
					numCache1.setLength(0);
					numCache2.setLength(0);
					if (n1 != n2) {
						return n1 - n2 > 0 ? 1 : -1;
					}else {
						continue;
					}
				}
				return c1 - c2;
			}
		}
		return len1 - len2;
	}

	/**
	 * 测试案例
	 * @param args
	 */
	public static void main(String[] args) {
		String s1 = "V1.03";
		String s2 = "V1.21";
		String s3 = "version1000";
		String s4 = "version999";
		String s5 = "10x0888version1";
		String s6 = "10x001999version1";
		String s7 = "10x01999version10";
		String s8 = "10x1999version9";
		List<String> list = Arrays.asList(s1, s2, s3, s4, s5,s6,s7, s8);
		Collections.sort(list, StringComparator.INSTANCE);
		System.out.println("排序后输出");
		for (String s : list) {
			System.out.println(s);
		}
	}
}
