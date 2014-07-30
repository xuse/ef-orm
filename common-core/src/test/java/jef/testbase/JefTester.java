/** Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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
package jef.testbase;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jef.common.Entry;
import jef.tools.StringUtils;
import jef.tools.reflect.ClassLoaderUtil;

import org.apache.commons.lang.CharUtils;
import org.junit.Test;

@SuppressWarnings("unused")
public class JefTester {
	static long total = 0;
	static long totalStat = 0;
	private final static int _SIZE = 500;

	private static int count = 1000;

	private static final char[] END_CHARS = { '\'', '"', ' ', '>' };

	static class Foo {
		private String name;
		private int value;

		public String getGroupValue(String name) {
			if ("name".equals(name)) {
				return this.name;
			} else {
				return String.valueOf(value);
			}
		}

		Foo() {
			System.out.println("aaaaaaaaaaa");
		}

		Foo(String name) {
			this();
			this.name = name;
			this.value = this.hashCode();
		}
	}

	static String hexByte(byte b) {
		String s = "000000" + Integer.toHexString(b);
		return s.substring(s.length() - 2);
	}

	private static final String[] XPATH_KEYS = { "//", "@", "/" };
	
	public static void main(String[] args) throws Exception {
//		IOUtils.processFiles(new File("E:/easyframe/easyframe-core/common-core"), sourceCharset, call, extPatterns)
	       
	}

	static int getFixedLenth(String ss, int start, int len) {
		int offset = start;
		int currentLen = 0;
		while (offset < ss.length() && currentLen < len) {
			char c = ss.charAt(offset++);
			currentLen += jef.tools.string.CharUtils.isAsian(c) ? 2 : 1;
		}
		return offset;
	}

	static List<String> printArray(List<String> oList, List<Integer> printList) {
		Integer more = printList.remove(0);
		List<String> result = new ArrayList<String>();
		if (oList.size() == 0) {
			result.add(more.toString());
		} else {
			for (int i = 0; i < oList.size(); i++) {
				String str = oList.get(i);
				for (int j = 0; j < str.length(); j++) {
					String rStr = str.substring(0, j) + more
							+ str.substring(j, str.length());
					result.add(rStr);
				}
			}
		}
		if (printList.size() == 0)
			return result;
		else
			return printArray(result, printList);
	}

	private static String[] analyzeEntry(String str, AtomicInteger i) {
		int start = str.indexOf(' ', i.get());
		if (start < 0)
			return null;
		start++;
		int keyEnd = str.indexOf('=', start);
		String key = str.substring(start, keyEnd);
		// 找到开始点
		int urlstart = keyEnd + 1;
		char quot = str.charAt(urlstart);
		if (quot == '\'' || quot == '"') {
			urlstart++;
		}
		// 找到结束点
		int urlend = StringUtils.indexOfAny(str, END_CHARS, urlstart);
		String value = str.substring(urlstart, urlend);
		i.set(urlend + 1);
		return new String[] { key, value };
	}


	private static void printMem(MemoryUsage m) {
		System.out.println("限额:" + StringUtils.formatSize(m.getMax()));
		System.out.println("分配到:" + StringUtils.formatSize(m.getCommitted()));
		System.out.println("初始:" + StringUtils.formatSize(m.getInit()));
		System.out.println("已使用:" + StringUtils.formatSize(m.getUsed()));
	}

	@Test
	public void method1() throws Exception {
		System.out.println("aaaa");
		ClassLoaderUtil.displayClassInfo(this.getClass());
		System.out.println(this.getClass().getClassLoader().hashCode());
		System.out.println("bbb");
		ClassLoaderUtil.displayClassInfo(this.getClass());
		System.out.println(this.getClass().getClassLoader().hashCode());
		System.out.println("dsds");
	}

	@Test
	public void methodxxx() throws Exception {
		String a="A123AAA";
		System.out.println(a.toUpperCase()==a);;
	}

	private static void sampleAdd(Map<Integer, Integer> data, BufferedReader r)
			throws IOException {
		String line;
		while ((line = r.readLine()) != null) {
			for (char c : line.toCharArray()) {
				total++;
				Integer i = (int) c;
				if (!CharUtils.isAscii((char) i.intValue())) {
					totalStat++;
					if (data.containsKey(i)) {
						data.put(i, data.get(i) + 1);
					} else {
						data.put(i, 1);
					}
				}
			}
		}
	}

	private static Entry<Integer, Integer> getSmall(Map<Integer, Integer> data) {
		int min = Integer.MAX_VALUE;
		Integer key = null;
		for (Integer i : data.keySet()) {
			if (data.get(i) < min) {
				key = i;
				min = data.get(i);
			}
		}
		return new Entry<Integer, Integer>(key, data.remove(key));
	}
}
