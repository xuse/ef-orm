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
package jef.tools.chinese;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;

import jef.tools.IOUtils;
import jef.tools.TextFileCallback;
import jef.tools.TextFileCallback.Dealwith;
import jef.tools.io.Charsets;

import  org.apache.commons.lang3.StringUtils;

/**
 * 繁简体转换工具
 * <p>
 * 备注：从繁体转换到简体是可靠的，安全的。
 * 但是简体字转换到繁体是不推荐的做法。因为简体字将很多繁体字合并了，
 * 反向转换时不可能根据上下文的含义来决定使用哪个字，因此可能会转换到别字上
 * <p>
 * Usage:<pre>
 * CFJUtil.getInstance().fan2jan("中華民國"); //返回 "中华民国"
 * </pre>
 * @author Jiyi
 *
 */
public class CFJUtil {
	private static CFJUtil instance;
	private SoftReference<Mapping[]> jan2fanMapping;
	private SoftReference<Mapping[]> fan2janMapping;

	/**
	 * 获得繁简转换工具的实例
	 * @return 繁简转换工具
	 */
	public static CFJUtil getInstance() {
		if (instance == null) {
			try {
				instance = new CFJUtil();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		return instance;
	}

	private CFJUtil() throws IOException {
		checkAndInit();
	}

	private void checkAndInit() {
		if (jan2fanMapping == null || jan2fanMapping.get() == null) {
			jan2fanMapping = new SoftReference<Mapping[]>(loadMapping(
					"jf_map_utf8.properties", 2700));
		}
		if (fan2janMapping == null || fan2janMapping.get() == null) {
			fan2janMapping = new SoftReference<Mapping[]>(loadMapping(
					"fj_map_utf8.properties", 3180));
		}
	}

	static class Mapping {
		int jId;
		int fId;

		public Mapping(int jChar, int fChar) {
			this.jId = jChar;
			this.fId = fChar;
		}
	}

	/**
	 * 将文字繁体转为简体
	 * @param input 
	 * @return String
	 */
	public String fan2jan(String input) {
		if (input == null)
			return null;
		checkAndInit();
		Mapping[] a=fan2janMapping.get();
		int len = input.length();
		char[] result = new char[len];
		for (int n = 0; n < len; n++) {
			int ch=input.charAt(n);
			int index=f2jSearch(a,ch);
			if(index<0){
				result[n]=(char)ch;
			}else{
				result[n]=(char)a[index].jId;
			}
		}
		return new String(result);
	}

	/**
	 * 简体转为繁体
	 * @param input 简体
	 * @return 繁体
	 */
	public String jan2fan(String input) {
		if (input == null)
			return null;
		checkAndInit();
		Mapping[] a=jan2fanMapping.get();
		int len = input.length();
		char[] result = new char[len];
		for (int n = 0; n < len; n++) {
			int ch=input.charAt(n);
			int index=j2fSearch(a,ch);
			if(index<0){
				result[n]=(char)ch;
			}else{
				result[n]=(char)a[index].fId;
			}
		}
		return new String(result);
	}

	private static int j2fSearch(Mapping[] a, int key) {
		int low = 0;
		int high = a.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			Mapping midVal = a[mid];
			if (midVal.jId < key)
				low = mid + 1;
			else if (midVal.jId > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	private static int f2jSearch(Mapping[] a, int key) {
		int low = 0;
		int high = a.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			Mapping midVal = a[mid];
			if (midVal.fId < key)
				low = mid + 1;
			else if (midVal.fId > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	private static Mapping[] loadMapping(String fileName, int size) {
		String line = null;
		BufferedReader br = null;
		int num = 0;
		try {
			br = IOUtils.getReader(CFJUtil.class, fileName, Charsets.UTF8);
			ArrayList<Mapping> list = new ArrayList<Mapping>(size);
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || StringUtils.isBlank(line)) {
					continue;
				}
				char fChar = line.charAt(0);
				char jChar = line.charAt(2);
				list.add(new Mapping(jChar, fChar));
				num++;
			}
			return list.toArray(new Mapping[list.size()]);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} finally {
			IOUtils.closeQuietly(br);
		}
	}

	/**
	 * 繁体文件转为简体
	 * @param from 输入文件
	 * @param fromCharset 文件编码
	 * @param to          输出文件
	 * @param toCharset   输出编码
	 * @throws IOException
	 */
	public void fan2Jan(File from, String fromCharset, final File to, String toCharset) throws IOException {
		IOUtils.processFile(from, new TextFileCallback(fromCharset,toCharset,Dealwith.NONE) {
			@Override
			protected String processLine(String line) {
				return fan2jan(line);
			}

			@Override
			protected File getTarget(File source) {
				return to;
			}
		});
	}
}
