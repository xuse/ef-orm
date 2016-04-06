package jef.tools.string;

import java.util.Comparator;

/**
 * 字符串排序器，目的是处理字符串和数字混杂时的，对于位于相同位置的数字串，按数字大小排序，而不是按其ASCII编码排序
 * 
 * @see java.util.Comparator
 */
public final class StringComparator implements Comparator<String> {
	public static final StringComparator INSTANCE = new StringComparator();
	
	private StringComparator() {
	}

	@Override
	public int compare(String o1, String o2) {
		int len1 = o1.length();
		int len2 = o2.length();
		int lim = Math.min(len1, len2);
		int j = 0;
		int k = 0;
		StringBuilder numCache1 = new StringBuilder();
		StringBuilder numCache2 = new StringBuilder();
		while (k < lim && j < lim) {
			char c1 = o1.charAt(j++);
			char c2 = o2.charAt(k++);
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
				if(numCache1.length()>18 || numCache2.length()>18) {
					int n=numCache1.toString().compareTo(numCache2.toString());
					numCache1.setLength(0);
					numCache2.setLength(0);
					if(n!=0) {
						return n;
					}else {
						continue;
					}
				}else {
					long n1 = Long.valueOf(numCache1.toString());
					long n2 = Long.valueOf(numCache2.toString());
					numCache1.setLength(0);
					numCache2.setLength(0);
					if (n1 != n2) {
						return n1 - n2 > 0 ? 1 : -1;
					} else {
						continue;
					}
				}
			}
			if (c1 != c2) {
				return c1 - c2;
			}
		}
		return len1 - j - len2 + k;
	}
}
