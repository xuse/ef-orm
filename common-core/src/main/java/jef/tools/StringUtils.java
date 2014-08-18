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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import jef.common.BooleanList;
import jef.common.DoubleList;
import jef.common.FloatList;
import jef.common.IntList;
import jef.common.LongList;
import jef.tools.string.RegexpUtils;
import jef.tools.string.StringSpliter;
import jef.tools.string.Substring;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.math.NumberUtils;

public final class StringUtils extends org.apache.commons.lang.StringUtils {
	public static final byte CR = 0x0D;
	public static final byte LF = 0x0A;
	public static final byte[] CRLF = { CR, LF };
	public static final String CRLF_STR = "\r\n";
	private static final String[] ISO1_SYMBOL = new String[] { "&#8216;", "&#8217;", "&#8221;", "&#8220;", "&#8230;" };
	private static final String[] ASCII_SYMBOL = new String[] { "'", "'", "\"", "\"", "--" };
	private static final String EXTEND_ISO_CHAR = new String(new char[] { (char) 146, (char) 147, (char) 148 });
	private static final String EXTEND_ISO_CHAR_MAPPING = (char) 39 + "\"\"";
	private static final String invalidCharsInFilename = "\t\\/|\"*?:<>\t\n\r";// 文件名中禁用的字符

	// ---------------------------------------------------------------------
	// General convenience methods for working with Strings
	// ---------------------------------------------------------------------

	/**
	 * 比较两个对象的大小，允许比较null值，null值作为最小处理
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public static <T extends Comparable<T>> int compareNull(T o1, T o2) {
		if (o1 == null && o2 == null)
			return 0;
		if (o1 == null)
			return -1;
		if (o2 == null)
			return 1;
		return o1.compareTo(o2);
	}

	/**
	 * 获得文本长度，其中双字节字符按2计算。 举例： getLengthInBytes("中国") = 4
	 * getLengthInBytes("卡拉OK") = 6 getLengthInBytes("太阳　月亮") = 10
	 * 
	 * @param str
	 * @return
	 */
	public static int getLengthInBytes(String str) {
		if (str == null)
			return 0;
		int len = 0;
		int max = str.length();
		for (int i = 0; i < max; i++) {
			char c = str.charAt(i);
			if (c > 255 && c != 65279) {
				len++;
			}
		}
		return str.length() + len;
	}

	/**
	 * 替换最后一个出现
	 * 
	 * @param text
	 * @param searchString
	 * @param replacement
	 * @return
	 */
	public static String replaceLast(String text, String searchString, String replacement) {
		if (isEmpty(text))
			return text;
		int n = text.lastIndexOf(searchString);
		if (n < 0)
			return text;
		StringBuilder sb = new StringBuilder(text.length() - searchString.length() + replacement.length());
		sb.append(text.substring(0, n));
		sb.append(replacement);
		sb.append(text.substring(n + searchString.length()));
		return sb.toString();
	}

	/**
	 * 替换最后一个出现
	 * 
	 * @param text
	 * @param searchString
	 * @param replacement
	 * @return
	 */
	public static String replaceLast(String text, char searchString, char replacement) {
		if (isEmpty(text))
			return text;
		int n = text.lastIndexOf(searchString);
		if (n < 0)
			return text;
		StringBuilder sb = new StringBuilder(text.length());
		sb.append(text.substring(0, n));
		sb.append(replacement);
		sb.append(text.substring(n + 1));
		return sb.toString();
	}

	/**
	 * 把[offset,offset+length)范围内的字符替换成replacement
	 * 
	 * @param text
	 * @param replacement
	 * @param offset
	 *            :[0,text.length-1]
	 * @param length
	 *            :<=text.length
	 * @return 替换后的字符串
	 */
	public static String replace(String text, char replacement, int offset, int length) {
		if (isEmpty(text) || offset >= text.length() || length <= 0)
			return text;
		if (offset < 0)
			offset = 0;
		int end = offset + length;
		if (end > text.length())
			end = text.length();

		char chars[] = text.toCharArray();
		for (int i = offset; i < end; i++)
			chars[i] = replacement;
		return new String(chars);
	}

	/**
	 * 把[offset,offset+length)范围内的字符替换成fixed个replacement
	 * 
	 * @param text
	 *            原字符串
	 * @param replacement
	 *            替换符
	 * @param fixed
	 *            个数
	 * @param offset
	 *            替换起始位置
	 * @param length
	 *            替换长度
	 * @return
	 */
	public static String replace(String text, char replacement, int fixed, int offset, int length) {
		if (isEmpty(text) || offset >= text.length() || length <= 0)
			return text;
		if (offset < 0)
			offset = 0;
		int end = offset + length;
		if (end > text.length())
			end = text.length();

		String left = text.substring(0, offset);
		String right = text.substring(end);

		char chars[] = new char[fixed];
		for (int i = 0; i < fixed; i++)
			chars[i] = replacement;

		return concat(left, new String(chars), right);
	}

	/**
	 * 将异常信息中的摘要输出到StringBuilder中
	 * 
	 * @param e
	 * @param sb
	 */
	public static void exceptionSummary(Throwable e, StringBuilder sb) {
		String msg = e.getLocalizedMessage();
		StackTraceElement[] stacks = e.getStackTrace();
		if (msg == null && e.getCause() != null) {
			exceptionSummary(e.getCause(), sb);
		}
		String stack = stacks.length > 0 ? stacks[0].toString() : "";
		sb.append(e.getClass().getSimpleName()).append(':').append(msg).append('\n').append(stack);
	}

	/**
	 * 返回异常信息的堆栈摘要
	 * 
	 * @param e
	 * @return
	 */
	public static String exceptionSummary(Throwable e) {
		String msg = e.getLocalizedMessage();
		StackTraceElement[] stacks = e.getStackTrace();
		if (msg == null && e.getCause() != null) {
			msg = exceptionSummary(e.getCause());
		}
		String stack = stacks.length > 0 ? stacks[0].toString() : "";
		return StringUtils.concat(e.getClass().getSimpleName(), ":", msg, "\r\n", stack);
	}

	/**
	 * 將错误堆栈信息转换为String
	 * 
	 * @param e
	 * @param pkgStart
	 * @return
	 */
	public static String exceptionStack(Throwable e, final String... pkgStart) {
		return exceptionStack("\r\n", e, pkgStart);
	}

	/**
	 * 将异常堆栈信息转换为String
	 * 
	 * @param cr
	 *            换行符
	 * @param e
	 *            异常
	 * @param pkgStart
	 *            包的开头描述
	 * @return
	 */
	public static String exceptionStack(final String cr, Throwable e, final String... pkgStart) {
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w) {
			@Override
			public void println() {
			}

			@Override
			public void write(String x) {
				x = rtrim(x, '\r', '\n', '\t');
				if (x.length() == 0) {
					return;
				}
				if (pkgStart.length == 0) {
					super.write(x, 0, x.length());
					super.write(cr, 0, cr.length());
					return;
				}
				String y = x.trim();
				if (!y.startsWith("at ")) {
					super.write(x, 0, x.length());
					super.write(cr, 0, cr.length());
					return;
				}
				for (String s : pkgStart) {
					if (matchChars(y, 3, s)) {
						super.write(x, 0, x.length());
						super.write(cr, 0, cr.length());
						return;
					}
				}
			}
		});
		w.flush();
		IOUtils.closeQuietly(w);
		return w.getBuffer().toString();
	}

	/**
	 * 判断字符串的一部分是否和制定的文字匹配
	 * 
	 * @param source
	 * @param offset
	 * @param keyword
	 * @return
	 */
	public static boolean matchChars(CharSequence source, int offset, CharSequence keyword) {
		int max = offset + keyword.length();
		for (int i = offset; i < max; i++) {
			if (source.charAt(i) != keyword.charAt(i - offset)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 将形如\\uxxxx的字符还原回unicode字符，即Asc2Native
	 * 
	 * @return
	 * @throws IOException
	 */
	public static String fromHexUnicodeString(String source) throws IOException {
		StringReader in = new StringReader(source);
		int len = source.length();
		StringWriter result = new StringWriter((len > 32) ? len / 2 : 16);
		fromHexUnicodeString(in, result);
		return result.toString();
	}

	/**
	 * 将所有的中文字符转换为\\uxxxx形式,即Native2AscII
	 * 
	 * @param source
	 * @return
	 */
	public static String toHexUnicodeString(String source) {
		StringWriter out = new StringWriter(source.length() * 4);
		StringReader in = new StringReader(source);
		toHexUnicodeString(in, out, "\\u");
		return out.toString();
	}

	/**
	 * 将所有的中文字符转换为{前缀}xxxx形式
	 * 
	 * @param source
	 * @return
	 */
	public static String toHexUnicodeString(String source, String prefix) {
		StringWriter out = new StringWriter(source.length() * 4);
		StringReader in = new StringReader(source);
		toHexUnicodeString(in, out, prefix);
		return out.toString();
	}

	/**
	 * 将所有的中文字符转换为{前缀}xxxx形式(xxxx为十六进制unicode)
	 * 
	 * @param source
	 * @return
	 */
	public static void toHexUnicodeString(Reader in, Writer out, String prefix) {
		int i;
		try {
			while ((i = in.read()) > 0) {
				if (i > 255) {
					out.write(prefix);
					String s = Integer.toHexString(i);
					if (s.length() == 3)
						out.write('0');
					out.write(s);
				} else {
					out.write((char) i);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 将形如\\uxxxx的字符还原回来
	 * 
	 * @return
	 * @throws IOException
	 */
	public static void fromHexUnicodeString(Reader in, Writer result) throws IOException {
		char[] buffer = new char[4];
		int i;
		while ((i = in.read()) > 0) {
			if (i == '\\') {
				i = in.read();
				if (i == 'u') {
					int count = in.read(buffer);
					if (count == 4) {
						String unicode = new String(buffer);
						char uni = (char) Integer.valueOf(unicode, 16).intValue();
						result.write(uni);
					} else {//
						result.write("\\u");
						result.write(buffer, 0, count);
					}
				} else {
					result.write('\\');
					if (i >= 0)
						result.write((char) i);
				}
			} else {
				result.write((char) i);
			}
		}
	}

	/**
	 * 通过增加数字后缀来避免名称的重复
	 * 
	 * @param base
	 *            原值
	 * @param exists
	 *            已有的值
	 * @param allowRaw
	 *            不添加后缀也不重复的情况下返回原值
	 * @param appendFormat
	 *            后缀格式
	 * @return
	 */
	public static String escapeName(String base, String[] exists, boolean allowRaw, String appendFormat, int start) {
		if (allowRaw && !ArrayUtils.contains(exists, base)) {
			return base;
		}
		int n = start;
		while (ArrayUtils.contains(exists, base + (appendFormat == null ? n : String.format(appendFormat, n)))) {
			n++;
		}
		return base + (appendFormat == null ? n : String.format(appendFormat, n));
	}

	/**
	 * 将ISO8859码的文字转换成ASCII字符。 主要是西欧的一些引号等特殊字符的转换
	 * 
	 * @param line
	 * @return
	 */
	public static String ISO8859ToASCII(String line) {
		line = replaceEach(line, ISO1_SYMBOL, ASCII_SYMBOL);
		line = replaceChars(line, EXTEND_ISO_CHAR, EXTEND_ISO_CHAR_MAPPING);
		return line;
	}

	/**
	 * 将字节码按指定编码编码为文本，不抛出受检异常
	 * 
	 * @param data
	 * @param encode
	 * @return
	 */
	public static String convert(byte[] data, String encode) {
		try {
			return new String(data, encode);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 从文本中删除除了指定字符以外的全部字符
	 * 
	 * @param str
	 * @param notremove
	 * @return
	 */
	public static String removeCharsExcept(String str, char... notremove) {
		if (isEmpty(str))
			return str;
		char chars[] = str.toCharArray();
		int pos = 0;
		for (int i = 0; i < chars.length; i++)
			if (ArrayUtils.contains(notremove, chars[i]))
				chars[pos++] = chars[i];
		return new String(chars, 0, pos);
	}

	/**
	 * 从文本中删除指定的字符
	 * 
	 * @param str
	 * @param remove
	 * @return
	 */
	public static String removeChars(String str, char... remove) {
		if (isEmpty(str))
			return str;
		char chars[] = str.toCharArray();
		int pos = 0;
		for (int i = 0; i < chars.length; i++)
			if (!ArrayUtils.contains(remove, chars[i]))
				chars[pos++] = chars[i];
		return new String(chars, 0, pos);
	}

	/**
	 * URL解码
	 * 
	 * @param source
	 * @param charset
	 * @return
	 */
	public static String urlDecode(String source, String charset) {
		try {
			return URLDecoder.decode(source, charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * URL解码
	 * 
	 * @param source
	 * @return
	 */
	public static String urlDecode(String source) {
		return urlDecode(source, "UTF-8");
	}

	/**
	 * URL编码
	 * 
	 * @param source
	 * @return
	 */
	public static String urlEncode(String source) {
		try {
			return URLEncoder.encode(source, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static String urlEncode(String source, String charset) {
		try {
			return URLEncoder.encode(source, charset).replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 文本转换到整数Long
	 * 
	 * @param o
	 * @param defaultValue
	 * @return
	 */
	public static long toLong(String o, Long defaultValue) {
		if (isBlank(o))
			return defaultValue;// 空白则返回默认值，即便默认值为null也返回null
		try {
			return NumberUtils.createLong(o);
		} catch (NumberFormatException e) {
			if (defaultValue == null)// 默认值为null，且数值非法的情况下抛出异常
				throw e;
			return defaultValue;
		}
	}

	/**
	 * 文本转换到整数int
	 * 
	 * @param o
	 * @param defaultValue
	 * @return
	 */
	public static int toInt(String o, Integer defaultValue) {
		if (isBlank(o))
			return defaultValue;// 空白则返回默认值，即便默认值为null也返回null
		try {
			return Integer.valueOf(o);
		} catch (NumberFormatException e) {
			if (defaultValue == null)// 默认值为null，且数值非法的情况下抛出异常
				throw e;
			return defaultValue;
		}
	}

	/**
	 * 文本转换到小数float
	 * 
	 * @param o
	 * @param defaultValue
	 * @return
	 */
	public static float toFloat(String o, Float defaultValue) {
		if (isBlank(o))
			return defaultValue;
		try {
			return NumberUtils.createFloat(o);
		} catch (NumberFormatException e) {
			if (defaultValue == null)// 默认值为null，且数值非法的情况下抛出异常
				throw e;
			return defaultValue;
		}
	}

	/**
	 * 文本转换到小数double
	 * 
	 * @param o
	 * @param defaultValue
	 * @return
	 */
	public static double toDouble(String o, Double defaultValue) {
		if (isBlank(o))
			return defaultValue;
		try {
			return NumberUtils.createDouble(o);
		} catch (NumberFormatException e) {
			if (defaultValue == null)// 默认值为null，且数值非法的情况下抛出异常
				throw e;
			return defaultValue;
		}
	}

	/**
	 * 将两个数值的比值作为百分比显示
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static String toPercent(long a, long b) {
		return String.valueOf(10000 * a / b / 100f).concat("%");
	}

	/**
	 * 文本转换为boolean，如果不能转换则返回默认值
	 * 
	 * @param s
	 * @param defaultValue
	 * @return
	 */
	public static final boolean toBoolean(String s, Boolean defaultValue) {
		if ("true".equalsIgnoreCase(s) || "Y".equalsIgnoreCase(s) || "1".equals(s) || "ON".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "T".equalsIgnoreCase(s)) {
			return true;
		}
		if ("false".equalsIgnoreCase(s) || "N".equalsIgnoreCase(s) || "0".equals(s) || "OFF".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "F".equalsIgnoreCase(s)) {
			return false;
		}
		if (defaultValue == null) {// 特别的用法，不希望有缺省值，如果字符串不能转换成布尔，则抛出异常。
			throw new IllegalArgumentException(s + "can't be cast to boolean.");
		}
		return defaultValue;
	}

	/**
	 * 将文件名中的非法字符替换成合法字符
	 * 
	 * @param fname
	 * @param to
	 * @return
	 */
	public static String toFilename(String fname, String to) {
		StringBuilder sb = new StringBuilder();
		for (char c : fname.toCharArray()) {
			if (invalidCharsInFilename.indexOf(c) > -1) {
				sb.append(to);
			} else {
				sb.append(c);
			}
		}
		fname = sb.toString();
		if (fname.endsWith(".")) {
			fname = StringUtils.substringBeforeLast(fname, ".");
		}
		return fname;
	}

	/**
	 * 数字转换为文本，位数不足在前面补0 convert number to String, add '0' before string.
	 * 
	 * @param number
	 * @param length
	 * @return
	 */
	public static String toFixLengthString(int number, int length) {
		String a = String.valueOf(number);
		if (length > a.length()) {
			return repeat("0", length - a.length()) + a;
		} else {
			return a;
		}
	}
	
	/**
	 * 将字符串格式化为固定大小
	 * @param number
	 * @param length
	 * @return
	 */
	public static String toFixLengthString(String text, int length,boolean padOnLeft, char padChar) {
		if(text.length()==length){
			return text;
		}else if(text.length()>length){
			return text.substring(0,length);
		}
		StringBuilder sb=new StringBuilder(length);
		if(padOnLeft){
			repeat(sb,padChar, length-text.length());
		}
		sb.append(text);
		if(!padOnLeft){
			repeat(sb,padChar, length-text.length());
		}
		return sb.toString();
	}

	/**
	 * 取字符串右侧的部分
	 * 
	 * @param source
	 *            源字符串
	 * @param rev
	 *            查找
	 * @param keepSourceIfNotFound
	 *            为true时找不到字串时返回全部，否则返回空串
	 * @return
	 */
	public static Substring stringRight(String source, String rev, boolean keepSourceIfNotFound) {
		if (source == null)
			return null;
		int n = source.indexOf(rev);
		if (n == -1) {
			if (keepSourceIfNotFound) {
				return new Substring(source);
			} else {
				return new Substring(source, source.length(), source.length());
			}
		}
		return new Substring(source, n + rev.length(), source.length());
	}

	/**
	 * 取字符串左侧的部分
	 * 
	 * @param source
	 *            源字符串
	 * @param rev
	 *            查找
	 * @param keepSourceIfNotFound为true时找不到字串时返回全部
	 *            ，否则返回空串
	 * @return
	 */
	public static Substring stringLeft(String source, String rev, boolean keepSourceIfNotFound) {
		if (source == null)
			return null;
		int n = source.indexOf(rev);
		if (n == -1) {
			if (keepSourceIfNotFound) {
				return new Substring(source);
			} else {
				return new Substring(source, 0, 0);
			}
		}
		return new Substring(source, 0, n);
	}

	/**
	 * 返回字串，如果查找的字串不存在则返回全部 和substringAfter方法不同，substringAfter方法在查找不到时返回空串
	 * 
	 * @param source
	 * @param rev
	 * @return
	 */
	public static String substringAfterIfExist(String source, String rev) {
		if (source == null)
			return source;
		int n = source.indexOf(rev);
		if (n == -1)
			return source;
		return source.substring(n + rev.length());
	}

	/**
	 * 返回字串，如果查找的字串不存在则返回全部
	 * 和substringAfterLast方法不同，substringAfterLast方法在查找不到时返回空串
	 * 
	 * @param source
	 * @param rev
	 * @return
	 */
	public static String substringAfterLastIfExist(String source, String rev) {
		if (source == null)
			return source;
		int n = source.lastIndexOf(rev);
		if (n == -1)
			return source;
		return source.substring(n + rev.length());
	}

	/**
	 * 在StringBuilder或各种Appendable中重复添加某个字符串若干次
	 * 
	 * @param sb
	 * @param str
	 * @param n
	 */
	public static void repeat(Appendable sb, CharSequence str, int n) {
		if (n <= 0)
			return;
		try {
			for (int i = 0; i < n; i++) {
				sb.append(str);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 在StringBuilder或各种Appendable中重复添加某个字符串若干次
	 * 
	 * @param sb
	 * @param str
	 * @param n 重复次数，如果传入小于等于0的值，不作处理
	 */
	public static void repeat(Appendable sb, char str, int n) {
		if (n <= 0)
			return;
		try {
			for (int i = 0; i < n; i++) {
				sb.append(str);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 重复字符串repeat the string for n times.
	 * 
	 * @param str
	 *            String to repeat
	 * @param n
	 *            repeat times.
	 * @return
	 */
	public static String repeat(CharSequence str, int n) {
		if (n <= 0)
			return "";
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < n; i++) {
			s.append(str);
		}
		return s.toString();
	}

	/**
	 * 重复字符
	 * 
	 * @param c
	 * @param n
	 * @return
	 */
	public static String repeat(char c, int n) {
		if (n <= 0)
			return "";
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < n; i++) {
			s.append(c);
		}
		return s.toString();
	}

	// //////////HTML处理
	private static final String htmlEscEntities = " <>&\"'\u00A9\u00AE";
	private static final String htmlEscapeSequence[] = { "&nbsp;", "&lt;", "&gt;", "&amp;", "&quot;", "&acute;", "&copy;", "&reg;" };

	/**
	 * HTML转义
	 */
	public static CharSequence escapeHtml(CharSequence s, boolean unicode) {
		if (unicode)
			return StringEscapeUtils.escapeHtml(s.toString());
		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int n = htmlEscEntities.indexOf(c);
			if (n > -1) {
				sb.append(htmlEscapeSequence[n]);
			} else {
				sb.append(c);
			}
		}
		return sb.length() == s.length() ? s : sb.toString();// 长度一样表示没有转义发生，因此无需再创建string对象，直接返回原来的
	}

	/**
	 * 转义到HTML文本
	 * 
	 * @param s
	 * @return
	 */
	public static CharSequence escapeHtml(CharSequence s) {
		return escapeHtml(s, false);
	}

	/**
	 * HTML反转义
	 * 
	 * @param s
	 * @return
	 */
	public static String unescapeHtml(String s) {
		return StringEscapeUtils.unescapeHtml(s);
	}

	/**
	 * 将输入的HTML字符集转换为数据库字符集 HTML字符集：特殊字符已经被转义HTML文本 数据库字符集: 特殊字符'被转义成两个'的文本
	 * 用于将页面输入转换为SQL语句
	 */
	public static String unescapeHtmlToSql(String s) {
		if (s == null)
			return null;
		return StringEscapeUtils.escapeSql(StringEscapeUtils.unescapeHtml(s));
	}

	/**
	 * 覆盖父类的方法，从两端取,Apache commons默认是从前取的
	 * 
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @return
	 */
	public static String substringBetween(String arg1, String arg2, String arg3) {
		int a = arg1.indexOf(arg2);
		int b = arg1.lastIndexOf(arg3);
		if (a == -1 || b == -1)
			return "";
		if (a == b)
			return "";
		if (a > b)
			return "";
		return arg1.substring(a + arg2.length(), b);
	}

	/**
	 * 判断字符串中是否包含指定关键字 关键字中可以使用*匹配任意数量字符，使用?匹配0到1个字符
	 * 
	 * @param s
	 * @param key
	 * @param IgnoreCase
	 * @return
	 */
	public static boolean contains(String s, String key, boolean IgnoreCase) {
		return matches(s, key, IgnoreCase, false, false, false);
	}

	/**
	 * 用简易语法校验两个String是否匹配。 注意，这个方法和String.matches的逻辑完全不同 注：
	 * 简易语法就是用*表示匹配任意字符，用?表示匹配0~1个任意字符，用+表示匹配1个或以上任意字符。 其他字符一律按照字面理解。
	 * 这是为了和Windows用户的习惯相吻合
	 */
	public static boolean matches(String s, String key, boolean IgnoreCase) {
		return matches(s, key, IgnoreCase, true, true, false);
	}

	/**
	 * 用简易语法校验两个String是否匹配。 注意，这个方法和String.matches的逻辑完全不同 注：
	 * 简易语法就是用*表示匹配任意字符，用?表示匹配0~1个任意字符，用+表示匹配1个或以上任意字符。 其他字符一律按照字面理解。
	 * 这是为了和Windows用户的习惯相吻合
	 * 
	 * @param IgnoreCase
	 *            忽略大小写
	 * @param matchStart
	 *            要求头部匹配（即源字符串在头部没有多余的字符）
	 * @param matchEnd
	 *            要求尾部匹配（即源字符串在尾部没有多余的字符）
	 * @param wildcardSpace
	 *            关键字中的空格可以匹配任意数量的（\n\t空格等）
	 * @return
	 */
	public static boolean matches(String s, String key, boolean IgnoreCase, boolean matchStart, boolean matchEnd, boolean wildcardSpace) {
		if (s == null && key == null)
			throw new NullPointerException();
		if (s == null)
			return false;
		if (key == null)
			return true;
		if (IgnoreCase) {
			s = s.toUpperCase();
		}
		Pattern p = RegexpUtils.simplePattern(key, IgnoreCase, matchStart, matchEnd, wildcardSpace);
		return p.matcher(s).matches();
	}

	private static DecimalFormat floatFormat = new DecimalFormat("#0.00");

	/**
	 * 小数格式化（保留两位）
	 * 
	 * @param number
	 * @return
	 */
	public static final String formatFloat(float number) {
		return floatFormat.format(number);
	}

	/**
	 * 用指定的格式将数字格式化
	 * 
	 * @param num
	 * @param template
	 * @return
	 */
	public static final String formatNumber(Number num, String template) {
		DecimalFormat f = new DecimalFormat(template);
		return f.format(num);
	}

	/**
	 * 把字符串左边的空格给去掉
	 */
	public static final String ltrim(String s) {
		int len = s.length();
		int st = 0;
		int off = 0; /* avoid getfield opcode */
		char[] val = s.toCharArray(); /* avoid getfield opcode */
		while ((st < len) && (val[off + st] <= ' ')) {
			st++;
		}
		return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
	}

	/**
	 * 从左侧删除指定的字符
	 * 
	 * @param s
	 * @param trimChars
	 * @return
	 */
	public static final String ltrim(String s, char... trimChars) {
		int len = s.length();
		int st = 0;
		int off = 0;
		while ((st < len) && (ArrayUtils.contains(trimChars, s.charAt(off + st)))) {
			st++;
		}
		return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
	}

	/**
	 * 从右侧删除指定的字符
	 * 
	 * @param s
	 * @param trimChars
	 * @return
	 */
	public static final String rtrim(String s, char... trimChars) {
		int len = s.length();
		int st = 0;
		int off = 0; /* avoid getfield opcode */
		while ((st < len) && ArrayUtils.contains(trimChars, s.charAt(off + len - 1))) {
			len--;
		}
		return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
	}

	/**
	 * 把字符串右边的空格给去掉
	 * 
	 * @param s
	 * @return
	 */
	public static final String rtrim(String s) {
		int len = s.length();
		int st = 0;
		int off = 0; /* avoid getfield opcode */
		char[] val = s.toCharArray(); /* avoid getfield opcode */
		while ((st < len) && (val[off + len - 1] <= ' ')) {
			len--;
		}
		return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
	}

	/**
	 * 左右两边做不同的trim
	 * 
	 * @param s
	 * @param lTrimChars
	 *            左侧要trim的字符
	 * @param rTrimChars
	 *            右侧要trim的字符
	 * @return
	 */
	public static final String lrtrim(String s, char[] lTrimChars, char[] rTrimChars) {
		int len = s.length();
		int st = 0;
		int off = 0;
		while ((st < len) && (ArrayUtils.contains(lTrimChars, s.charAt(off + st)))) {
			st++;
		}
		while ((st < len) && ArrayUtils.contains(rTrimChars, s.charAt(off + len - 1))) {
			len--;
		}
		return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
	}

	/**
	 * 产生8位的随机数字
	 * 
	 * @return
	 */
	public static final String randomString() {
		return RandomStringUtils.randomNumeric(8);
	}

	/**
	 * 获得32位的Hex uuid(實際長度36)
	 */
	public static final String generateGuid() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	/**
	 * 计算CRC摘要,8位十六进制数
	 */
	public static String getCRC(InputStream in) {
		CRC32 crc32 = new CRC32();
		byte[] b = new byte[4096];
		int len = 0;
		try {
			while ((len = in.read(b)) != -1) {
				crc32.update(b, 0, len);
			}
			return Long.toHexString(crc32.getValue());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * 计算CRC摘要,8位十六进制数
	 */
	public static String getCRC(String s) {
		ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
		return getCRC(in);
	}

	/**
	 * 计算MD5摘要,
	 * 
	 * @param s
	 *            输入
	 * @return 32位十六进制数的MD5值
	 */
	public final static String getMD5(String s) {
		ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
		byte[] md = hash(in, "MD5");
		return join(md, (char) 0, 0, md.length);
	}

	/**
	 * 计算MD5摘要
	 * 
	 * @param in
	 * @return 32位十六进制数的MD5值
	 */
	public final static String getMD5(InputStream in) {
		byte[] md = hash(in, "MD5");
		return join(md, (char) 0, 0, md.length);
	}

	/**
	 * 计算SHA-1
	 * 
	 * @param s
	 * @return
	 */
	public final static String getSHA1(String s) {
		ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
		byte[] md = hash(in, "SHA-1");
		return join(md, (char) 0, 0, md.length);
	}

	/**
	 * 计算SHA256
	 * 
	 * @param s
	 * @return
	 */
	public final static String getSHA256(String s) {
		ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
		byte[] md = hash(in, "SHA-256");
		return join(md, (char) 0, 0, md.length);
	}

	/**
	 * 计算SHA-1摘要
	 * 
	 * @param in
	 * @return
	 */
	public final static String getSHA1(InputStream in) {
		byte[] md = hash(in, "SHA-1");
		return join(md, (char) 0, 0, md.length);
	}

	private static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * 将byte数组转换为可显示的十六进制文本串。效果等同于Integer.toHexString，但是实测发现JDK的方法慢3倍以上,
	 * 所以还是用自己写的
	 * 
	 * @see jef.tools.ByteUtils#hex2byte(CharSequence, boolean) 其逆运算 (hex2byte)
	 */
	public static String byte2hex(byte[] b) {
		return join(b, ' ', 0, b.length);
	}

	/**
	 * 将byte数组转换为可显示的十六进制文本串
	 * 
	 * @param b
	 * @param offset
	 * @param len
	 * @return
	 * @see jef.tools.ByteUtils#hex2byte(CharSequence, boolean) 其逆运算 (hex2byte)
	 */
	public static String byte2hex(byte[] b, int offset, int len) {
		return join(b, ' ', offset, len);
	}

	/*
	 * 计算消息摘要
	 */
	public final static byte[] hash(InputStream in, String algorithm) {
		try {
			MessageDigest mdTemp = MessageDigest.getInstance(algorithm);
			byte[] b = new byte[4096];
			int len = 0;
			while ((len = in.read(b)) != -1) {
				mdTemp.update(b, 0, len);
			}
			return mdTemp.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/** 得到以当前毫秒数的字串 */
	public static String getTimeStamp() {
		return String.valueOf(System.currentTimeMillis());
	}

	/**
	 * 计算双字节字符
	 * 
	 * @param s
	 * @return
	 */
	public static int countAsian(String s) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 255 && c != 65279) {
				n++;
			}
		}
		return n;
	}

	/**
	 * 是否存在东亚字符
	 * 
	 * @param s
	 * @return
	 */
	public static boolean hasAsian(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 255 && c != 65279) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 对象转文本
	 * 
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		if (obj == null)
			return EMPTY;
		return obj.toString();
	}

	/**
	 * 取得字符串左边的部分，要求从左数第一位开始，且字符必须在指定的范围中
	 * 
	 * @return
	 */
	public static Substring subLeftWithChar(String ss, char[] chars) {
		int n = ss.length();
		for (int i = 0; i < ss.length(); i++) {
			char c = ss.charAt(i);
			if (!ArrayUtils.contains(chars, c)) {
				n = i;
				break;
			}
		}
		return new Substring(ss, 0, n);
	}

	/**
	 * 取得字符串右边的部分，要求从右数第一位开始，且字符必须在指定的范围中
	 * 
	 * @return
	 */
	public static Substring subRightWithChar(String ss, char[] chars) {
		int n = 0;
		for (int i = ss.length() - 1; i >= 0; i--) {
			char c = ss.charAt(i);
			if (!ArrayUtils.contains(chars, c)) {
				n = i + 1;
				break;
			}
		}
		return new Substring(ss, n, ss.length());
	}

	/**
	 * 分隔成多个子串。用分隔符之一
	 * 
	 * @param unknown
	 * @param tokens
	 * @return
	 */
	public static String[] splitOfAny(String unknown, char[] tokens) {
		List<String> list = new ArrayList<String>();
		StringSpliter sp = new StringSpliter(unknown);
		while (sp.setKeyOfAny(tokens)) {
			list.add(sp.getLeft().toString());
			sp = new StringSpliter(sp.getRight());
		}
		list.add(sp.getSource().toString());
		return list.toArray(new String[list.size()]);
	}

	/**
	 * 分隔成多个子串。用分隔符之一
	 * 
	 * @param unknown
	 * @param tokens
	 * @return
	 */
	public static String[] splitOfAny(String unknown, String[] tokens) {
		List<String> list = new ArrayList<String>();
		StringSpliter sp = new StringSpliter(unknown);
		while (sp.setKeyOfAny(tokens)) {
			list.add(sp.getLeft().toString());
			sp = new StringSpliter(sp.getRight());
		}
		list.add(sp.getSource().toString());
		return list.toArray(new String[list.size()]);
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param b
	 * @param c
	 * @return
	 */
	public static String join(byte[] b, char c) {
		if (b == null)
			return "";
		return join(b, c, 0, b.length);
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param b
	 * @param dchar
	 * @return
	 */
	public static String join(byte[] b, char dchar, int offset, int len) {
		if (b == null || b.length == 0)
			return "";
		boolean appendSpace = (dchar != 0);
		int j = offset + len;
		if (j > b.length)
			j = b.length; // 上限
		char str[] = new char[j * ((appendSpace) ? 3 : 2)];
		int k = 0;
		for (int i = offset; i < j; i++) {
			byte byte0 = b[i];
			str[k++] = hexDigits[byte0 >>> 4 & 0xf]; // >>是带符号移位， >>>是无符号移位
			str[k++] = hexDigits[byte0 & 0xf];
			if (appendSpace)
				str[k++] = dchar;
		}
		if (appendSpace) {
			return new String(str, 0, k - 1);
		} else {
			return new String(str);
		}
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(int[] ss, String string) {
		StringBuilder sb = new StringBuilder();
		if (ss != null && ss.length > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(float[] ss, String string) {
		StringBuilder sb = new StringBuilder();
		if (ss != null && ss.length > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(double[] ss, String string) {
		StringBuilder sb = new StringBuilder();
		if (ss != null && ss.length > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 数组转文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(boolean[] ss, String string) {
		StringBuilder sb = new StringBuilder();
		if (ss != null && ss.length > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(long[] ss, String string) {
		StringBuilder sb = new StringBuilder();
		if (ss != null && ss.length > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(short[] ss, String string) {
		StringBuilder sb = new StringBuilder();
		if (ss != null && ss.length > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param string
	 * @return
	 */
	public static String join(char[] ss, String string) {
		int len = ss == null ? 0 : ss.length;
		StringBuilder sb = new StringBuilder(len + string.length() * (len - 1));
		if (len > 0) {
			int n = 0;
			sb.append(ss[n++]);
			while (n < ss.length) {
				sb.append(string);
				sb.append(ss[n++]);
			}
		}
		return sb.toString();
	}

	/**
	 * 将数组或列表拼成文本
	 * 
	 * @param ss
	 * @param sep
	 * @return
	 */
	public static String join(Object[] os, String separator) {
		if (os == null || os.length == 0)
			return EMPTY;
		String[] ss = new String[os.length];
		int len = 0;
		int sepLen = separator.length();
		for (int i = 0; i < os.length; i++) {
			Object o = os[i];
			ss[i] = o == null ? "" : o.toString();
			len += ss[i].length();
			len += sepLen;
		}
		StringBuilder sb = new StringBuilder(len - sepLen);
		int n = 0;
		sb.append(ss[n++]);
		while (n < ss.length) {
			sb.append(separator);
			sb.append(ss[n++]);
		}
		return sb.toString();
	}

	/**
	 * 解析字符串中的$[key}，将其用properties中的值替代
	 * 
	 * @param s
	 * @param prop
	 * @return
	 */
	public static String convertProperty(String s, Properties prop) {
		int i = s.indexOf("${");
		if (i > -1) {
			StringBuilder sb = new StringBuilder();
			int j = -1;
			while (i > -1) {
				sb.append(s.subSequence(j + 1, i));
				j = s.indexOf('}', i + 1);
				String key = "";
				if (j > 0) {// Invalid block
					key = s.substring(i + 2, j);
				} else {
					j = s.indexOf("${", i + 2) - 1;// 下一处的起点作为本次的终点
					if (j < 0) {
						j = s.length() - 1;
					}
				}
				if (StringUtils.isEmpty(key)) {
					sb.append(s.subSequence(i, j + 1));// 将J也包进去
				} else {
					String value = prop.getProperty(key);
					if (value != null)
						sb.append(value);
				}
				i = s.indexOf("${", j);
			}
			sb.append(s.substring(j + 1));
			return sb.toString();
		}
		return s;

	}

	/**
	 * 将数组拼成文本 当知道obj isArray的情况，但是不清楚具体的类型的情况下，做Join计算
	 * 
	 * @Title: join
	 * @param 参数
	 * @return String 返回类型
	 * @throws
	 */
	public static String join(Object obj, String dchar) {
		Assert.notNull(obj);
		if (!obj.getClass().isArray()) {
			throw new IllegalArgumentException("The input object to join must be a Array and not null.");
		}
		Class<?> priType = obj.getClass().getComponentType();
		if (priType == Boolean.TYPE) {
			return join((boolean[]) obj, dchar);
		} else if (priType == Byte.TYPE) {
			return join((byte[]) obj, dchar);
		} else if (priType == Character.TYPE) {
			return join((char[]) obj, dchar);
		} else if (priType == Integer.TYPE) {
			return join((int[]) obj, dchar);
		} else if (priType == Long.TYPE) {
			return join((long[]) obj, dchar);
		} else if (priType == Float.TYPE) {
			return join((float[]) obj, dchar);
		} else if (priType == Double.TYPE) {
			return join((double[]) obj, dchar);
		} else if (priType == Short.TYPE) {
			return join((short[]) obj, dchar);
		} else {
			return join((Object[]) obj, dchar);
		}
	}

	private static final double SIZE_1K = 1024;
	private static final double SIZE_1M = 1048576;
	private static final double SIZE_1G = 1073741824;

	/**
	 * 将文件大小格式化成xxG xxM等格式
	 * 
	 * @param size
	 * @return
	 */
	public static String formatSize(long size) {
		DecimalFormat df = new DecimalFormat("#.##");
		if (size < SIZE_1K) {
			return String.valueOf(size);
		} else if (size < SIZE_1M) {
			return df.format(size / SIZE_1K).concat("K");
		} else if (size < SIZE_1G) {
			return df.format(size / SIZE_1M).concat("M");
		} else {
			return df.format(size / SIZE_1G).concat("G");
		}
	}

	/**
	 * 字符串插入
	 * 
	 * @param source
	 * @param n
	 * @param str
	 * @return
	 */
	public static String insert(String source, int n, String str) {
		if (source.length() <= n)
			return source.concat(str);
		if (n < 0)
			n = 0;
		StringBuilder sb = new StringBuilder(source.length() + str.length());
		sb.append(source, 0, n);
		sb.append(str);
		sb.append(source, n, source.length());
		return sb.toString();
	}

	/**
	 * 文本截断
	 * 
	 * @param str
	 * @param maxLength
	 * @param append
	 *            阶段后要添加的内容
	 * @return
	 */
	public static String truncate(String str, int maxLength, String... append) {
		if (str.length() <= maxLength)
			return str;
		str = str.substring(0, maxLength);
		if (append.length > 0) {
			return str.concat(append[0]);
		} else {
			return str;
		}
	}

	/**
	 * 在str1当中除去str2（仅一次）。如果str1不包含str2,则返回null
	 * 
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static String removeOnce(String str1, String str2) {
		int n = str1.indexOf(str2);
		if (n == -1)
			return null;
		if (n == 0) {
			return str1.substring(n + str2.length());
		} else if (n + str2.length() == str1.length()) {
			return str1.substring(0, n);
		} else {
			return str1.substring(0, n).concat(str1.substring(n + str2.length()));
		}
	}

	/**
	 * 检查一个字符串是否符合数字的格式
	 * 
	 * @Title: isNumericOrMinus
	 * @param isFloat
	 *            是否允许小数
	 * @return boolean 返回类型
	 * @throws
	 */
	public static boolean isNumericOrMinus(String str, boolean isFloat) {
		if (str == null)
			return false;
		int sz = str.length();
		if (sz == 0)
			return false;
		short hasPoint = 0;
		short start = 0;
		if (str.charAt(0) == '-')
			start = 1;
		for (int i = start; i < sz; i++) {
			char c = str.charAt(i);
			if (!Character.isDigit(c)) {
				if (c == '.' && hasPoint == 0 && isFloat) {
					hasPoint = 1;
				} else {
					return false;
				}
			}
		}
		return (sz - start) > hasPoint;
	}

	/**
	 * 合并多个String,在参数为3个和以内时请直接使用String.concat。
	 * 5个和超过5个String相加后，concat方法性能急剧下降，此时此方法最快
	 * 
	 * @param args
	 * @return
	 */
	public final static String concat(String... args) {
		if (args.length == 1)
			return args[0];
		int n = 0;
		for (String s : args) {
			if (s == null)
				continue;
			n += s.length();
		}
		StringBuilder sb = new StringBuilder(n);
		for (String s : args) {
			if (s == null)
				continue;
			sb.append(s);
		}
		return sb.toString();
	}

	/**
	 * 判断是否为合法的数字（包括负数、小数）
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isValidNumer(String str) {
		return isNumericOrMinus(str, true);
	}

	/**
	 * 是否为合法的十六进制字符，a-f A-F 0-9
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isHexString(String str) {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (c < 48 || (c > 57 && c < 65) || (c > 70 && c < 97) || c > 102) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 判断是否为合法的数字（包括负数，但不能为小数）
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumericOrMinus(String str) {
		return isNumericOrMinus(str, false);
	}

	/**
	 * 当参数为对象时的非空判断，以toString后的值为准
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isNotEmpty(Object value) {
		return !isEmpty(value);
	}

	/**
	 * 当参数为对象时的空判断，以toString后的值为准
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isEmpty(Object value) {
		if (value == null)
			return true;
		if (value instanceof CharSequence) {
			return ((CharSequence) value).length() == 0;
		}
		return false;
	}

	/**
	 * useful
	 * 
	 * @param source
	 * @param key
	 * @return
	 */
	public static String[] splitLast(String source, String key) {
		if (source == null)
			return null;
		int n = source.lastIndexOf(key);
		if (n < 0)
			return new String[] { source, "" };
		return new String[] { source.substring(0, n), source.substring(n + key.length()) };
	}

	/**
	 * 将文本拆成两个字符串
	 * @param source
	 * @param key
	 * @return
	 */
	public static String[] splitLast(String source, char key) {
		if (source == null)
			return null;
		int n = source.lastIndexOf(key);
		if (n < 0)
			return new String[] { source, "" };
		return new String[] { source.substring(0, n), source.substring(n + 1) };
	}

	/**
	 * 将一串文本解析为Key/Value的若干值 对
	 * 
	 * @param source
	 *            源数据
	 * @param entrySep
	 *            entry间的分隔符
	 * @param keyValueSep
	 *            key/value的分隔符
	 * @param keyUpper
	 *            key是否转大写，0不修改， -1转小写， 1转大写
	 * 
	 * @return
	 */
	public static Map<String, String> toMap(String source, String entrySep, String keyValueSep, int keyUpper) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		if (source != null) {
			for (String entry : StringUtils.split(source, entrySep)) {
				entry = entry.trim();
				int index = entry.indexOf(keyValueSep);
				if (index > -1) {
					String key = entry.substring(0, index).trim();
					if (keyUpper > 0) {
						key = key.toUpperCase();
					} else if (keyUpper < 0) {
						key = key.toLowerCase();
					}
					result.put(key, entry.substring(index + 1).trim());
				} else {
					String key = entry;
					if (keyUpper > 0) {
						key = key.toUpperCase();
					} else if (keyUpper < 0) {
						key = key.toLowerCase();
					}
					result.put(key, "");
				}
			}
		}
		return result;
	}
	
	/**
	 * {@link #toMap}的逆运算，将map转回到string
	 * @param map
	 * @param entrySep
	 * @param keyValueSep
	 * @return
	 */
	public static String toString(Map<String,String> map,String entrySep,String keyValueSep){
		StringBuilder sb=new StringBuilder();
		Iterator<Map.Entry<String, String>> iter=map.entrySet().iterator();
		if(iter.hasNext()){
			{
				Map.Entry<String,String> e=iter.next();
				sb.append(e.getKey()).append(keyValueSep).append(e.getValue());	
			}
			for(;iter.hasNext();){
				Map.Entry<String,String> e=iter.next();
				sb.append(entrySep).append(e.getKey()).append(keyValueSep).append(e.getValue());	
			}
		}
		return sb.toString();
	}

	/**
	 * 查找字符串的方法
	 * 
	 * @param str
	 * @param searchChars
	 * @param startPos
	 * @return
	 */
	public static int indexOfAny(String str, char[] searchChars, int startPos) {
		if (isEmpty(str) || ArrayUtils.isEmpty(searchChars)) {
			return -1;
		}
		for (int i = startPos; i < str.length(); i++) {
			char ch = str.charAt(i);
			for (int j = 0; j < searchChars.length; j++) {
				if (searchChars[j] == ch) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * 给定若干字符，从后向前寻找，任意一个匹配的字符。
	 * @param str
	 * @param searchChars
	 * @param startPos
	 * @return
	 */
	public static int lastIndexOfAny(String str, char[] searchChars, int startPos) {
		if ((str == null) || (searchChars == null)) {
			return -1;
		}
		for (int i = str.length() - 1; i > 0; i--) {
			char c = str.charAt(i);
			for (int j = 0; j < searchChars.length; j++) {
				if (c == searchChars[j]) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * 用于字符串的拼接
	 * 
	 * @param data
	 *            数据
	 * @param sep
	 *            分隔符
	 * @param sb
	 *            拼接目标
	 */
	public static void joinTo(Collection<?> data, String sep, StringBuilder sb) {
		if (data == null || data.isEmpty())
			return;
		Iterator<?> iterator = data.iterator();
		sb.append(String.valueOf(iterator.next()));
		while (iterator.hasNext()) {
			Object obj = iterator.next();
			sb.append(sep).append(String.valueOf(obj));
		}
	}

	/**
	 * 用于字符串的拼接
	 * 
	 * @param data
	 *            数据
	 * @param sep
	 *            分隔符
	 * @param sb
	 *            拼接目标
	 */
	public static void joinTo(Object[] data, char sep, StringBuilder sb) {
		if (data == null || data.length == 0)
			return;
		sb.append(String.valueOf(data[0]));
		for (int i = 1; i < data.length; i++) {
			sb.append(sep).append(String.valueOf(data[i]));
		}
	}

	/**
	 * 替换环境变量
	 * 
	 * @param content
	 * @return
	 */
	public static String replaceTextByJvmArg(String content) {
		StringBuffer result = new StringBuffer();
		Properties p = System.getProperties();
		int pos = 0;
		int indexStart = -1;
		int indexEnd = -1;
		String jmvParam = null;
		String jmvVal = null;
		do {
			indexStart = content.indexOf("${", pos);
			if (indexStart > 0) {
				indexEnd = content.indexOf("}");
				if (indexEnd < 0)
					throw new IllegalArgumentException("tag ${ and }  should appear in pair, ${ existed, but } can't find");
			} else {
				indexEnd = -1;
			}
			if (indexStart != -1 && indexEnd != -1) {
				jmvParam = content.substring(indexStart + 2, indexEnd);
				jmvVal = p.getProperty(jmvParam);
				result.append(content.substring(pos, indexStart));
				if (jmvVal != null) {
					result.append(jmvVal);
				} else {
					throw new IllegalArgumentException("argument ${" + jmvParam + "} can't be found in jvm argument");
				}
			} else {
				result.append(content.substring(pos));
			}
			pos = indexEnd + 1;
		} while (pos > 0);
		return result.toString();
	}

	/**
	 * 得到字符转为小写以后的hashcode
	 * 
	 * @param text
	 * @return
	 */
	public static int lowerHashCode(String text) {
		if (text == null) {
			return 0;
		}
		// return text.toLowerCase().hashCode();
		int h = 0;
		for (int i = 0; i < text.length(); ++i) {
			char ch = text.charAt(i);
			if (ch >= 'A' && ch <= 'Z') {
				ch = (char) (ch + 32);
			}

			h = 31 * h + ch;
		}
		return h;
	}

	/**
	 * 将文本转换为int的列表。
	 * 
	 * @param text
	 *            文本
	 * @param dem
	 *            分隔字符
	 * @param defaultValue
	 *            数值不合法时的缺省值
	 * @return 数组
	 */
	public static int[] toIntArray(String text, char dem, int defaultValue) {
		String[] ss = StringUtils.split(text, dem);
		int[] result = new int[ss.length];
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i];
			if (s.length() > 0) {
				try {
					int value = Integer.valueOf(s.trim());
					result[i] = value;
				} catch (NumberFormatException e) {
					result[i] = defaultValue;
				}
			}
		}
		return result;
	}

	/**
	 * 将文本转换为int的列表。不合法的数值将被丢弃
	 * 
	 * @param text
	 *            文本
	 * @param dem
	 *            分隔字符
	 * @return 数组
	 */
	public static int[] toIntArray(String text, char dem) {
		String[] ss = StringUtils.split(text, dem);
		IntList result = new IntList();
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i].trim();
			if (s.length() > 0) {
				try {
					int value = Integer.parseInt(s.trim());
					result.add(value);
				} catch (NumberFormatException e) {
				}
			}
		}
		return result.toArrayUnsafe();
	}

	/**
	 * 将文本转换为float的列表。
	 * 
	 * @param text
	 *            文本
	 * @param dem
	 *            分隔字符
	 * @return 数组
	 */
	public static float[] toFloatArray(String text, char dem) {
		String[] ss = StringUtils.split(text, dem);
		FloatList result = new FloatList();
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i].trim();
			if (s.length() > 0) {
				try {
					float value = Float.parseFloat(s.trim());
					result.add(value);
				} catch (NumberFormatException e) {
				}
			}
		}
		return result.toArrayUnsafe();
	}

	/**
	 * 将文本转换为long的列表。
	 * 
	 * @param text
	 *            文本
	 * @param dem
	 *            分隔字符
	 * @return 数组
	 */
	public static long[] toLongArray(String text, char dem) {
		String[] ss = StringUtils.split(text, dem);
		LongList result = new LongList();
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i].trim();
			if (s.length() > 0) {
				try {
					long value = Long.parseLong(s.trim());
					result.add(value);
				} catch (NumberFormatException e) {
				}
			}
		}
		return result.toArrayUnsafe();
	}

	/**
	 * 将文本转换为double的列表。
	 * 
	 * @param text
	 *            文本
	 * @param dem
	 *            分隔字符
	 * @return 数组
	 */
	public static double[] toDoubleArray(String text, char dem) {
		String[] ss = StringUtils.split(text, dem);
		DoubleList result = new DoubleList();
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i].trim();
			if (s.length() > 0) {
				try {
					double value = Double.parseDouble(s.trim());
					result.add(value);
				} catch (NumberFormatException e) {
				}
			}
		}
		return result.toArrayUnsafe();
	}

	/**
	 * 将文本转换为double的列表。
	 * 
	 * @param text
	 *            文本
	 * @param dem
	 *            分隔字符
	 * @return 数组
	 */
	public static boolean[] toBooleanArray(String text, char dem) {
		String[] ss = StringUtils.split(text, dem);
		BooleanList result = new BooleanList();
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i].trim();
			if (s.length() > 0) {
				try {
					boolean value = toBoolean(s.trim(), null);
					result.add(value);
				} catch (IllegalArgumentException e) {
				}
			}
		}
		return result.toArrayUnsafe();
	}
	
	/**
	 * 转换为Date数组
	 * @param text
	 * @param dem
	 * @param df
	 * @return
	 */
	public static Date[] toDateArray(String text, char dem, DateFormat df) {
		String[] ss = StringUtils.split(text, dem);
		List<Date> list=new ArrayList<Date>();
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i].trim();
			if (s.length() > 0) {
				try {
					list.add(df.parse(s));
				} catch (ParseException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		return list.toArray(new Date[list.size()]);
	}

	/**
	 * Tokenize the given String into a String array via a StringTokenizer.
	 * Trims tokens and omits empty tokens.
	 * <p>
	 * The given delimiters string is supposed to consist of any number of
	 * delimiter characters. Each of those characters can be used to separate
	 * tokens. A delimiter is always a single character; for multi-character
	 * delimiters, consider using {@code delimitedListToStringArray}
	 * 
	 * @param str
	 *            the String to tokenize
	 * @param delimiters
	 *            the delimiter characters, assembled as String (each of those
	 *            characters is individually considered as delimiter).
	 * @return an array of the tokens
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(String str, String delimiters) {
		return tokenizeToStringArray(str, delimiters, true, true);
	}

	/**
	 * Tokenize the given String into a String array via a StringTokenizer.
	 * <p>
	 * The given delimiters string is supposed to consist of any number of
	 * delimiter characters. Each of those characters can be used to separate
	 * tokens. A delimiter is always a single character; for multi-character
	 * delimiters, consider using {@code delimitedListToStringArray}
	 * 
	 * @param str
	 *            the String to tokenize
	 * @param delimiters
	 *            the delimiter characters, assembled as String (each of those
	 *            characters is individually considered as delimiter)
	 * @param trimTokens
	 *            trim the tokens via String's {@code trim}
	 * @param ignoreEmptyTokens
	 *            omit empty tokens from the result array (only applies to
	 *            tokens that are empty after trimming; StringTokenizer will not
	 *            consider subsequent delimiters as token in the first place).
	 * @return an array of the tokens ({@code null} if the input String was
	 *         {@code null})
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (str == null) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}

	/**
	 * Copy the given Collection into a String array. The Collection must
	 * contain String elements only.
	 * <p/>
	 * <p>
	 * Copied from the Spring Framework while retaining all license, copyright
	 * and author information.
	 * 
	 * @param collection
	 *            the Collection to copy
	 * @return the String array (<code>null</code> if the passed-in Collection
	 *         was <code>null</code>)
	 */
	public static String[] toStringArray(Collection<?> collection) {
		if (collection == null) {
			return null;
		}
		return (String[]) collection.toArray(new String[collection.size()]);
	}
	
	/**
	 * Determines whether or not the sting 'searchIn' contains the string
	 * 'searchFor', disregarding case and leading whitespace
	 * 
	 * @param searchIn
	 *            the string to search in
	 * @param searchFor
	 *            the string to search for
	 * 
	 * @return true if the string starts with 'searchFor' ignoring whitespace
	 */
	public static boolean startsWithIgnoreCaseAndWs(String searchIn,
			String searchFor) {
		return startsWithIgnoreCaseAndWs(searchIn, searchFor, 0);
	}

	/**
	 * Determines whether or not the sting 'searchIn' contains the string
	 * 'searchFor', disregarding case and leading whitespace
	 * 
	 * @param searchIn
	 *            the string to search in
	 * @param searchFor
	 *            the string to search for
	 * @param beginPos
	 *            where to start searching
	 * 
	 * @return true if the string starts with 'searchFor' ignoring whitespace
	 */
	public static boolean startsWithIgnoreCaseAndWs(String searchIn,
			String searchFor, int beginPos) {
		if (searchIn == null) {
			return searchFor == null;
		}

		int inLength = searchIn.length();

		for (; beginPos < inLength; beginPos++) {
			if (!Character.isWhitespace(searchIn.charAt(beginPos))) {
				break;
			}
		}

		return startsWithIgnoreCase(searchIn, beginPos, searchFor);
	}

	/**
	 * Determines whether or not the string 'searchIn' contains the string
	 * 'searchFor', dis-regarding case starting at 'startAt' Shorthand for a
	 * String.regionMatch(...)
	 * 
	 * @param searchIn
	 *            the string to search in
	 * @param startAt
	 *            the position to start at
	 * @param searchFor
	 *            the string to search for
	 * 
	 * @return whether searchIn starts with searchFor, ignoring case
	 */
	public static boolean startsWithIgnoreCase(String searchIn, int startAt,
			String searchFor) {
		return searchIn.regionMatches(true, startAt, searchFor, 0, searchFor
				.length());
	}
}
