package jef.testbase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jef.tools.StringUtils;

import org.junit.Test;

public class StringAnalyzeTest {
	private static final char[] END_CHARS = { '\'', '"', ' ', '>' };

	@Test
	public void test() {
		String str = "<a href='http://www.baidu.com' title='百度' width=\"100%\" height=50>";
		System.out.println(analyze(str));
	}
	
	public static Map<String,String> analyze(String str) {
		Map<String, String> result = new HashMap<String, String>();
		AtomicInteger pos = new AtomicInteger(0);
		String[] entry = nextEntry(str, pos);
		while (entry != null) {
			result.put(entry[0], entry[1]);
			entry = nextEntry(str, pos);
		}
		return result;
	}

	private static String[] nextEntry(String str, AtomicInteger i) {
		int start = str.indexOf(' ', i.get());
		if (start < 0)
			return null;
		start++;
		int keyEnd = str.indexOf('=', start);
		String key = str.substring(start, keyEnd);

		// 找到开始点
		int valueStart = keyEnd + 1;
		char quot = str.charAt(valueStart);
		if (quot == '\'' || quot == '"') {
			valueStart++;
		}
		// 找到结束点
		int urlend = StringUtils.indexOfAny(str, END_CHARS, valueStart);
		String value = str.substring(valueStart, urlend);
		i.set(urlend + 1);
		return new String[] { key, value };
	}
}
