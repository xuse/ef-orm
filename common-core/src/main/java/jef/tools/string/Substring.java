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
package jef.tools.string;

import jef.common.Entry;

/**
 * @author jiyi
 */
public class Substring implements java.io.Serializable, Comparable<CharSequence>,CharSequence {
	private static final long serialVersionUID = 3538223148105733732L;

	private String source;
	private int start;
	private int end;
	/*
	 * 比大小(non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(CharSequence o) {
		int mlen=length();
		int len=Math.min(mlen, o.length());
		for(int i=0;i<len;i++){
			char a=charAt(i);
			char b=o.charAt(i);
			if(a>b){
				return 1;
			}else if(a<b){
				return -1;
			}
		}
		if(mlen>o.length()){
			return 1;
		}else if(mlen<o.length()){
			return -1;
		}
		return 0;
	}
	
	protected int getEnd() {
		return end;
	}

	protected void setEnd(int end) {
		this.end = end;
	}

	protected int getStart() {
		return start;
	}

	protected void setStart(int start) {
		this.start = start;
	}

	public char charAt(int index) {
		return source.charAt(start + index);
	}

	public int length() {
		return end - start;
	}

	public Substring sub(int startA,int endA){
		int newStart = this.start + startA;
		int newEnd = this.start + endA;

		if (newStart < this.start)
			throw new IndexOutOfBoundsException();
		if (newEnd > end) {
			throw new IndexOutOfBoundsException();
		}
		return new Substring(source, newStart, newEnd);
	}
	public CharSequence subSequence(int startA, int endA) {
		return sub(startA,endA);
	}

	protected Substring subAbsoultOffset(int startA, int endA) {
		if (startA < this.start)
			throw new IllegalArgumentException();
		if (endA > end) {
			throw new IllegalArgumentException();
		}
		return new Substring(source, startA, endA);
	}

	public Substring(String source, int start, int end) {
		this.source = source;
		if (end > source.length()) {
			throw new IllegalArgumentException("the end offset of Substring must not greater than the length.");
		}
		if (start < 0) {
			throw new IllegalArgumentException("the begin of Substring must >=0");
		}
		if (start > end) {
			throw new IllegalArgumentException("The begin (" + start
					+ ") must not greater than end (" + end + ")");
		}
		this.start = start;
		this.end = end;
	}
	
	public Substring(String s1, int i) {
		this(s1,i,s1.length());
	}

	public Substring(String source) {
		this.source = source;
		this.start = 0;
		this.end = source.length();
	}

	public String toString() {
		return source.substring(start, end);
	}

	/**
	 * fromIndex 从左边数跳过的字符个数
	 */
	public int indexOf(char c, int fromIndex) {
		for (int i = fromIndex; i < length(); i++) {
			if (charAt(i) == c) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * fromRightIndex 从右边数跳过的字符个数
	 */
	public int lastIndexOf(char c, int fromRightIndex) {
		for (int i = length() - 1 - fromRightIndex; i > -1; i--) {
			if (charAt(i) == c) {
				return i;
			}
		}
		return -1;
	}

	public int indexOf(String str, int fromIndex) {
		char[] chs = str.toCharArray();
		if (chs.length == 0)
			return -1;
		int skipChar = fromIndex;
		while (true) {
			int n = indexOf(chs[0], skipChar);
			if (n > -1) {
				if (matches(n, chs)) {
					return n;
				}
				skipChar = n + 1;
			} else {
				return -1;
			}
		}
	}

	public int lastIndexOf(String str, int fromRightIndex) {
		char[] chs = str.toCharArray();
		int skipChar = fromRightIndex;

		while (true) {
			int x = skipChar + chs.length - 1;
			int n = lastIndexOf(chs[0], x);
			if (n > -1) {
				if (matches(n, chs)) {
					return n;
				}
				skipChar = length() - n;
			} else {
				return -1;
			}
		}
	}

	// 判断从第n开头的字符串是否和指定的系列相等
	private boolean matches(int n, char[] chs) {
		if (n + chs.length > length())
			return false;
		for (int i = 0; i < chs.length; i++) {
			if (charAt(n + i) != chs[i]) {
				return false;
			}
		}
		return true;
	}

	public int lastIndexOf(char c) {
		return lastIndexOf(c, 0);
	}

	public int indexOf(char c) {
		return indexOf(c, 0);
	}

	public int lastIndexOf(String str) {
		return lastIndexOf(str, 0);
	}

	public int indexOf(String str) {
		return indexOf(str, 0);
	}

	/**
	 * 合并相邻的两个SubString, 如果不是从同一个Source建立的Substring会抛出异常。
	 * 
	 * @param str1
	 * @return
	 */
	public static Substring merge(Substring str1, Substring str2) {
		if (str1.source != str2.source) {
			throw new IllegalArgumentException("two substring is not generate on the same string");
		}
		if (str1.start == str2.end) {
			return new Substring(str1.source, str2.start, str1.end);
		} else if (str1.end == str2.start) {
			return new Substring(str1.source, str1.start, str2.end);
		}
		throw new IllegalArgumentException("the two Substring does not reached.");//不是相邻的。
	}

	public boolean startsWith(String key) {
		return matches(0, key.toCharArray());
	}

	/**
	 * 是否以指定的任意字串开头，返回匹配到的长度最大的一个字串
	 * 
	 * @param keys
	 * @return
	 */
	public String startsWithAny(String[] keys) {
		String matched = null;
		for (String key : keys) {
			if (startsWith(key)) {
				if (matched == null || key.length() > matched.length()) {
					matched = key;
				}
			}
		}
		return matched;
	}

	public String endsWithAny(String[] keys) {
		String matched = null;
		for (String key : keys) {
			if (endsWith(key)) {
				if (matched == null || key.length() > matched.length()) {
					matched = key;
				}
			}
		}
		return matched;
	}

	public boolean endsWith(String key) {
		return matches(length() - key.length(), key.toCharArray());
	}

	public boolean isEmpty() {
		return length() == 0;
	}

	public Character lastCharacter() {
		int n = length();
		if (n == 0)
			return null;
		return charAt(n - 1);
	}

	public Character firstCharacter() {
		if (length() == 0)
			return null;
		return charAt(0);
	}

	public Substring siblingRight() {
		return new Substring(source, end, source.length());

	}

	public Substring siblingLeft() {
		return new Substring(source, 0, start);
	}

	/**
	 * 查找匹配字串中的任意一个，返回最先出现的那个关键字和位置
	 * 
	 * @param ss
	 * @return
	 */
	public Entry<Integer, String> indexOfAny(String[] ss) {
		int find = -1;
		String key = null;
		for (String str : ss) {
			int n = source.indexOf(str);
			if (n > -1 && (n < find || find == -1)) {
				find = n;
				key = str;
			}
		}
		return new Entry<Integer, String>(find, key);
	}

	/**
	 * 从右向左查找匹配字串中的任意一个，返回最先出现的那个关键字和位置
	 * 
	 * @param ss
	 * @return
	 */
	public Entry<Integer, String> lastIndexOfAny(String[] ss) {
		int find = -1;
		String key = null;
		for (String str : ss) {
			int n = source.lastIndexOf(str);
			if (n > find || find == -1) {
				find = n;
				key = str;
			}
		}
		return new Entry<Integer, String>(find, key);
	}

	/**
	 * 注意，此方法调用后是直接截取原对象上两边的空格，返回的也是原对象，并不会新建一个对象。
	 * 
	 * @return
	 */
	public Substring trim() {
		int tmp = -1;
		for (int i = start; i < end; i++) {
			if (source.charAt(i) == 32) {
				tmp = i;
			} else {
				break;
			}
		}
		if (tmp > -1)
			start = tmp + 1;
		tmp = -1;
		for (int i = end - 1; i >= start; i--) {
			if (source.charAt(i) == 32) {
				tmp = i;
			} else {
				break;
			}
		}
		if (tmp > -1)
			end = tmp;
		return this;
	}

	@Override
	public int hashCode() {
		int h = source.hashCode();
		h=h+start*1024+end;
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof CharSequence){
			CharSequence cs=(CharSequence)obj;
			if(this.length()!=cs.length())return false;
			for(int i=0;i<this.length();i++){
				if(this.charAt(i)!=cs.charAt(i))return false;
			}
			return true;
		}else{
			return false;
		}
	}

}
