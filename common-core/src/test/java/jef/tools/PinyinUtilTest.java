package jef.tools;

import jef.common.log.LogUtil;
import jef.tools.chinese.PinyinUtil;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import org.junit.Test;

public class PinyinUtilTest extends Assert{
	private String test="移除指定的keys，如果该key不存在则将会被忽略。";
	
	@Test
	public void testgetAll(){
		LogUtil.show(
		PinyinUtil.getAllPingYin(test)
		);
		
		LogUtil.show(
				PinyinUtil.getPinyin(test)
				);
		
		LogUtil.show(
				PinyinUtil.getPinyin(test," ")
				);
		
	}
	
//	@Test
//	public void testPinying(){
//		String result=PinyinUtil.getPingYin(test);
//		System.out.println(result);
//	}
//	
	@Test
	public void testPinyingHead(){
		String result=PinyinUtil.getPinYinHeadChar(test);
		System.out.println(result);
	}
	
	@Test
	public void testPinyinHelper() throws BadHanyuPinyinOutputFormatCombination{
//		System.out.println(
//		PinyinHelper.toHanyuPinyinString(test, new HanyuPinyinOutputFormat(), " ")
//		);
		HanyuPinyinOutputFormat FORMAT_DEFAULT = new HanyuPinyinOutputFormat();
		FORMAT_DEFAULT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		LogUtil.show(
		PinyinHelper.toHanyuPinyinStringArray('中', FORMAT_DEFAULT)
		);
	}
}
