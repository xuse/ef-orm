package jef.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jef.common.log.LogUtil;
import jef.tools.string.StringSpliter;
import jef.tools.string.Substring;

import org.junit.Test;

public class StringUtilsTest extends org.junit.Assert {

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
		String s = "usertname.txt.bat";
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
	public void testStringRegexp(){
		String sql="update TEST_ENTITY set DOUBLEFIELD2 = ?, FOLATFIELD2 = ?, BOOLFIELD = ?, CREATE_TIME = ?, FLOATFIELD = ?, LONGFIELD2 = ?, DOUBLEFIELD = ?, FIELD_1 = ?, FIELD_2 = ?, BINARYDATA = ?, INT_FIELD_1 = ?, INT_FIELD_2 = ?, LONGFIELD = ?, DATEFIELD = ?, BOOLFIELD2 = ? where LONGFIELD2=?\r\naa";
		Pattern p=Pattern.compile("update TEST_ENTITY set.+",Pattern.MULTILINE );
		System.out.println(p.matcher(sql).matches());
	}
}
