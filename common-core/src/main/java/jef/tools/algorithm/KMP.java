package jef.tools.algorithm;

public class KMP {

	public static void main(String[] args) {
		String s = "abbabbbbcab"; // 主串
		String t = "bbcab"; // 模式串
		char[] ss = s.toCharArray();
		char[] tt = t.toCharArray();
		System.out.println(indexOf(ss, tt)); // KMP匹配字符串
	}

	/**
	 * 获得字符串的next函数值
	 * 
	 * @param t
	 *            字符串
	 * @return next函数值
	 */
	public static int[] next(char[] t) {
		int[] next = new int[t.length];
		next[0] = -1;
		int i = 0;
		int j = -1;
		while (i < t.length - 1) {
			if (j == -1 || t[i] == t[j]) {
				i++;
				j++;
				if (t[i] != t[j]) {
					next[i] = j;
				} else {
					next[i] = next[j];
				}
			} else {
				j = next[j];
			}
		}
		return next;
	}

	/**
	 * KMP匹配字符串
	 * 
	 * @param s
	 *            主串
	 * @param t
	 *            模式串
	 * @return 若匹配成功，返回下标，否则返回-1
	 */
	public static int indexOf(char[] s, char[] t) {
		int[] next = next(t);
		int i = 0;
		int j = 0;
		while (i <= s.length - 1 && j <= t.length - 1) {
			if (j == -1 || s[i] == t[j]) {
				i++;
				j++;
			} else {
				j = next[j];
			}
		}
		if (j < t.length) {
			return -1;
		} else
			return i - t.length; // 返回模式串在主串中的头下标
	}

	/**
	 * 在target字符串中寻找source的匹配
	 * 
	 * @param target
	 * @param source
	 */
	public int indexOf2(String target, String source) {
		int sourceLength = source.length();
		int targetLength = target.length();
		int[] result = next1(source);
		int j = 0;
		int k = 0;
		for (int i = 0; i < targetLength; i++) {
			// 找到匹配的字符时才执行
			while (j > 0 && source.charAt(j) != target.charAt(i)) {
				// 设置为source中合适的位置
				j = result[j - 1];
			}
			// 找到一个匹配的字符
			if (source.charAt(j) == target.charAt(i)) {
				j++;
			}
			// 匹配到一个，输出结果
			if (j == sourceLength) {
				j = result[j - 1];
				k++;
				System.out.println("find");
				return j;
			}
		}
		return -1;
	}

	/**
	 * 预处理
	 * 
	 * @param s
	 * @return
	 */
	private int[] next1(final String s) {
		int size = s.length();
		int[] result = new int[size];
		result[0] = 0;
		int j = 0;
		// 循环计算
		for (int i = 1; i < size; i++) {
			while (j > 0 && s.charAt(j) != s.charAt(i)) {
				j = result[j];
			}
			if (s.charAt(j) == s.charAt(i)) {
				j++;
			}
			// 找到一个结果
			result[i] = j;
		}
		System.out.println(java.util.Arrays.toString(result));
		return result;
	}

}
