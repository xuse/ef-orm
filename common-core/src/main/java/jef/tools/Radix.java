package jef.tools;

/**
 * 进制转换工具
 * 
 * Use {@link #encode(long)} {@link #encodeInt(int)} to convert a decimal number
 * to a N-module number. Use {@link #decodeInt(String)} {@link #decode(String)}
 * to convert the N-module number back to the decimal number
 * 
 * @author Joey
 *
 */
public enum Radix {
	/**
	 * 二进制
	 */
	D2("01".toCharArray(), 64),

	/**
	 * 三进制
	 */
	D3("012".toCharArray(), 40),
	/**
	 * 七进制
	 */
	D7("0123456".toCharArray(), 30),
	/**
	 * 八进制
	 */
	D8("01234567".toCharArray(), 21) {
		@Override
		protected void encode0(long num, StringBuilder sb) {
			while (num >= scale) {
				int remainder = (int) (num & 7);
				sb.append(codeTable[remainder]);
				num = num >> 3;
			}
			sb.append(codeTable[(int) num]);
		}
	},
	/**
	 * 九进制
	 */
	D9("012345678".toCharArray(), 20),
	/**
	 * 10进制
	 */
	D10("0123456789".toCharArray(), 19),

	/**
	 * 十进制中文
	 */
	D10C("零一二三四五六七八九".toCharArray(), 19),
	/**
	 * 16进制
	 */
	D16("0123456789ABCDEF".toCharArray(), 16) {
		@Override
		protected void encode0(long num, StringBuilder sb) {
			while (num >= scale) {
				int remainder = (int) (num & 15);
				sb.append(codeTable[remainder]);
				num = num >> 4;
			}
			sb.append(codeTable[(int) num]);
		}
	},
	/**
	 * 62进制
	 */
	D62("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray(), 12),
	/**
	 * 64进制
	 */
	D64("$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz".toCharArray(), 11) {
		@Override
		protected void encode0(long num, StringBuilder sb) {
			while (num >= scale) {
				int remainder = (int) (num & 63);
				sb.append(codeTable[remainder]);
				num = num >> 6;
			}
			sb.append(codeTable[(int) num]);
		}
	},

	/**
	 * 七十二进制
	 */
	D72("$0123456789=@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{}~".toCharArray(), 11),

	/**
	 * 八十四进制
	 */
	D84("!#$%&()*+.0123456789:=@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_abcdefghijklmnopqrstuvwxyz{|}~".toCharArray(), 11),;
	/**
	 * 进制
	 */
	public final int scale;
	/**
	 * 码表
	 */
	final char[] codeTable;
	/**
	 * 转换乘方表，避免每次执行乘方运算
	 */
	private final long[] powTable;

	/**
	 * 字符码表是否符合编码顺序
	 */
	private int codeAlgorithm = BINARY_SEARCH;
	
	private static final int FAST_CONVERT = 0;
	private static final int BINARY_SEARCH = 1;
	private static final int INDEXOF = 2;

	/**
	 * NOTE:乘方计算，不能用Math.pow()会丢失精度。 例如Math.pow(3,39) = {@code 4052555153018976256}
	 * 正确结果为 {@code 4052555153018976267}
	 * 这造成3进制和7进制等计算结果总是错误，直到反复核对才发现Math.pow对大整数的计算是不准确的。
	 * 
	 * @param base
	 * @param pow
	 * @return
	 */
	public static long pow(long base, int pow) {
		long n = 1;
		for (int i = 0; i < pow; i++) {
			n *= base;
		}
		return n;
	}

	/**
	 * 构造
	 * 
	 * @param cs
	 */
	Radix(char[] cs, int powerTableSize) {
		this.scale = cs.length;
		this.codeTable = cs;
		// 计算乘方表
		powTable = new long[powerTableSize];
		for (int i = 0; i < powerTableSize; i++) {
			// must not use Math.pow()
			powTable[i] = pow(scale, i);
		}
		// 判断码表是否为有序集合，这将决定解码时的查找算法
		if (cs[0] - 48 == 0 && cs[scale - 1] - 48 == scale - 1) {
			codeAlgorithm = FAST_CONVERT;
		} else {
			char last = 0;
			for (char c : cs) {
				if (c < last) {
					codeAlgorithm = INDEXOF;
					break;
				}
				last = c;
			}
		}
	}

	/**
	 * 对int进行序列化 (支持负数)
	 * 
	 * @param num
	 * @return
	 */
	public String encodeInt(int num) {
		if (num < 0) {
			return encode(num + 0x0100000000L);
		} else {
			return encode((long) num);
		}
	}

	/**
	 * 解码为整数 (支持负数)
	 * 
	 * @param val
	 * @return
	 */
	public int decodeInt(String val) {
		long num = decode(val);
		return num > Integer.MAX_VALUE ? (int) (num - 0x0100000000L) : (int) num;
	}

	/**
	 * 编码为指定的进制
	 * 
	 * @param num Long 型数字
	 * @return 编码后进制字符串
	 */
	public String encode(long num) {
		StringBuilder sb = new StringBuilder();
		if (num < 0) {
			encode0(-num, sb);
			sb.append('-');
		} else {
			encode0(num, sb);
		}
		return sb.reverse().toString();
	}

	/**
	 * 62进制字符串转为数字
	 *
	 * @param str 编码后的62进制字符串
	 * @return 解码后的 10 进制字符串
	 */
	public long decode(String str) {
		if (str == null || str.isEmpty()) {
			return 0;
		}
		boolean minus = str.charAt(0) == '-';
		long num = decode0(str, minus ? 1 : 0);
		return minus ? -num : num;
	}

	private long decode0(String str, int begin) {
		int len = str.length();
		long num = 0;
		switch(codeAlgorithm) {
		case FAST_CONVERT:
			for (int i = begin; i < len; i++) {
				int index = str.charAt(i) - 48;
				num += index * powTable[len - i - 1];
			}
		case BINARY_SEARCH:
			for (int i = begin; i < len; i++) {
				int index = binarySearch(str.charAt(i));
				num += index * powTable[len - i - 1];
			}
		case INDEXOF:
			for (int i = begin; i < len; i++) {
				int index = indexOf(str.charAt(i));
				num += index * powTable[len - i - 1];
			}
		}
		return num;
	}

	/*
	 * 默认算法效率较低，对于2的n次方进制，可以通过按位右移来代替除法和求余
	 */
	protected void encode0(long num, StringBuilder sb) {
		while (num >= scale) {
			long num1 = num / scale;
			sb.append(codeTable[(int) (num - num1 * scale)]);
			num = num1;
		}
		sb.append(codeTable[(int) num]);
	}

	/**
	 * 对于无序码表，遍历查找
	 * 
	 * @param charAt
	 * @return
	 */
	private int indexOf(char charAt) {
		for (int i = 0; i < scale; i++) {
			if (codeTable[i] == charAt) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 对于有序码表，二分法查找 对于10以内的数字，其实最快的还是char-48。
	 * 
	 * @param charAt
	 * @return
	 */
	private int binarySearch(char charAt) {
		int min = 0;
		int max = scale - 1;
		while (min <= max) {
			int middle = (min + max) / 2;
			char middleChar = codeTable[middle];
			if (middleChar == charAt) {
				return middle;
			} else if (charAt > middleChar) {
				min = middle + 1;
			} else {
				max = middle - 1;
			}
		}
		return -1;
	}
	
	public static void main(String[] args) {
		byte b=0x45;
		System.out.println(D16.encodeInt(b));
	}

}
