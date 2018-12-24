package jef.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.junit.Test;

import jef.common.log.LogUtil;
import jef.tools.string.StringComparator;
import jef.tools.string.StringSpliter;
import jef.tools.string.Substring;

public class StringUtilsTest extends org.junit.Assert {

	@Test
	public void testIgnoree() {
		String s = " \r\t\n　　 [1123]  　　\r\t\n";
		int i = StringUtils.ignoreWhiteSpace(s);
		int j = StringUtils.ignoreRightWhiteSpace(s);
		System.out.println(s.charAt(i));
		System.out.println(s.charAt(j));
		assertEquals("[1123]", s.substring(i, j));
	}

	@Test
	public void stringTokens() {
		String command = "cmd \"c:\\program files\\aaa\\bbb.exe\" /s /t \"file name\"";
		StringTokenizer st = new StringTokenizer(command);
		String[] cmdarray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++)
			cmdarray[i] = st.nextToken();
		LogUtil.show(cmdarray);
	}
	
	
	@Test
	public void testUppercase() throws IOException {
		String s = "HÀN ZÌ ZHUǍN PĪN YĪN";
		System.out.println(Strings.lower(s));
		System.out.println(Strings.upper(s));
		
		
		
		//IOUtils.saveAsFile(new File("c:/sss.txt"), Charsets.GB18030,s.toLowerCase(Locale.CHINESE)+s.toUpperCase(Locale.CHINESE));
	}
	
	
	@Test
	public void testReplace2() throws IOException {
		String text="    混合双打双方   ,就是反对色    ,看书打发士大夫    ";
		int startOf=StringUtils.getBeginPos(text);
		System.out.println(startOf);
		String result=StringUtils.replace(text, "    ", "【】",startOf);
		System.out.println(result);
		
	}
	

	@Test
	public void testSubstring() {
		String sss = "12345[67890abcdef]ghijklmn";
		LogUtil.show(StringUtils.splitOfAny(sss, new String[] { "<", "(", "]", ">", "[" }));

		Substring sb = new Substring(sss);
		StringSpliter sp = new StringSpliter(sb);

		// sp.setMode(StringSpliter.MODE_FROM_RIGHT);
		sp.setKey("90ab");
		System.out.println("========");

		System.out.println(sp.getLeft());
		System.out.println(sp.getKeyword());
		System.out.println(sp.getRight());
		System.out.println("========");
		sp.expandKeyLeftUntil("[".toCharArray());
		System.out.println(sp.getLeft());
		System.out.println(sp.getKeyword());
		System.out.println(sp.getRight());
		System.out.println(sp.getLeftWithKey());
		System.out.println(sp.getRightWithKey());

		System.out.println("==��String����Ƚ�indexOf�߼�=====");
		String ss = "dssd1d3sdsdsdcmkdssddscmdssdk123f456dssd7890";
		sb = new Substring(ss, 4, ss.length() - 3);
		ss = sb.toString();
		System.out.println(ss);
		System.out.println(sb.indexOf("cm", 6));
		System.out.println(ss.indexOf("cm", 6));
		System.out.println(sb.lastIndexOf("cm"));
		System.out.println(ss.lastIndexOf("cm"));

		String keyLike = "?[?3aaa4?]??";

		sp = new StringSpliter(keyLike);
		sp.setKey("aaa");
		sp.expandKey("34[]".toCharArray());
		System.out.println(sp.getLeft());
		System.out.println(sp.getKeyword());
		System.out.println(sp.getRight());

		String xPath = "count:1sdsdsd/dsds";
		if (xPath.startsWith("count:")) {
			xPath = xPath.substring(6);
		}
		LogUtil.show(xPath);
	}

	@Test
	public void testMatchChars() {
		String a = "    at you are not along!";
		String b = "you are";
		boolean flag = StringUtils.matchChars(a, 7, b);
		System.out.println(flag);
		Assert.isTrue(flag);
	}

	@Test
	public void testMatch() {
		String clz = "com.ailk.openbilling.persistence.newModule.entity.Lineitem";
		String key = "com.ailk.*.persistence.*";
		boolean flag = StringUtils.matches(clz, key, false);
		assertTrue(flag);
	}

	@Test
	public void testToMap() {
		String s = "aa=\"123\"bb=\"456\"cc=\"789\"";
		StringTokenizer t = new StringTokenizer(s, "\"=", false);
		Map<String, String> map = new HashMap<String, String>();
		while (t.hasMoreTokens()) {
			String key = t.nextToken();
			String value = t.hasMoreTokens() ? t.nextToken() : "";
			map.put(key, value);
		}
		assertEquals(3, map.size());
	}

	@Test
	public void testSplitLast() {
		String s = "usertname.txt.bat";
		String[] ss = StringUtils.splitLast(s, ".");
		String newName = ss[0] + "(2)." + ss[1];
		assertEquals("usertname.txt(2).bat", newName);

	}

	@Test
	public void testSplitLast123() {
		String s = "u";
		{
			System.out.println(StringUtils.getSHA256(s));
		}
		{
			System.out.println(StringUtils.getSHA1(s));
		}
		{
			System.out.println(StringUtils.getMD5(s));
		}

	}

	@Test
	public void testStringRegexp() {
		String sql = "update TEST_ENTITY set DOUBLEFIELD2 = ?, FOLATFIELD2 = ?, BOOLFIELD = ?, CREATE_TIME = ?, FLOATFIELD = ?, LONGFIELD2 = ?, DOUBLEFIELD = ?, FIELD_1 = ?, FIELD_2 = ?, BINARYDATA = ?, INT_FIELD_1 = ?, INT_FIELD_2 = ?, LONGFIELD = ?, DATEFIELD = ?, BOOLFIELD2 = ? where LONGFIELD2=?\r\naa";
		Pattern p = Pattern.compile("update TEST_ENTITY set.+", Pattern.MULTILINE);
		System.out.println(p.matcher(sql).matches());
	}

	@Test
	public void testSkip() throws IOException {
		System.out.println(StringUtils.fromHexUnicodeString("\u200b"));
		String s = StringUtils.generateGuid();
		System.out.println(s);

		System.out.println(StringUtils.getMD5(s));
		System.out.println(StringUtils.getSHA1(s));
		System.out.println(StringUtils.getSHA256(s));
	}

	/**
	 * 测试案例
	 * 
	 * @param args
	 */
	@Test
	public void stringSort() {
		String s1 = "V1.01.1b";
		String s2 = "V1.1.10";
		String s3 = "version1000";
		String s4 = "version999";
		String s5 = "10x0888version1";
		String s6 = "10x1111111111111111111111111111111111111111111111111111111version1";
		String s7 = "10x222222222222222222222222222222222222222222222222222222222version10";
		String s8 = "10x1999version9";
		List<String> list = Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8);
		Collections.sort(list, StringComparator.INSTANCE);
		assertEquals(list, Arrays.asList(s5, s6, s8, s7, s1, s2, s4, s3));
	}

	@Test
	public void testIsRepeatOf() {
		System.out.println(StringUtils.isRepeatOf("=================", "="));

		System.out.println(StringUtils.isRepeatOf("123412341234", "1234"));

		System.out.println(StringUtils.isRepeatOf("123412341234", "123"));

		System.out.println(StringUtils.isRepeatOf("!=!", "!=!"));
		System.out.println(StringUtils.isRepeatOf("!=!", "!="));

		System.out.println(StringUtils.isRepeatOf("!=!=", "!="));
		System.out.println(StringUtils.isRepeatOf("!=!=", "!=!"));

	}
}
