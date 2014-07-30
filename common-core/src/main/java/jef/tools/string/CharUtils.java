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

import jef.common.wrapper.IntRange;
import jef.common.wrapper.IntRangeGroup;
import jef.tools.StringUtils;

public final class CharUtils extends org.apache.commons.lang.CharUtils {
	/*
	long int char byte经常转来转去，但实际上危险性是很大的。
	long占用8个字节，int 占用4个字节，short / char占用两个字节  byte一个字节。
	凡是窄类型转宽类型是安全的，但是宽类型转窄类型就可能造成数据丢失,是危险的。
	long转int会丢失高位，这个很容易引起注意,但是其他几种有很容易出问题。
	short的范围是-32767 ~ 32768.
	比如  InputStream.read(),得到一个int,转成char后，范围就是0~65535.(0~FFFF)，
	如果要判断流的结束（-1），那么就必须在转成char之前，用int去判断，转成char之后判断就有错。
	但是偏偏java语法是允许你写出 char ==-1这样的直接比较（隐式转换，从char转int）.
	
	byte的范围是 -128 ~ 127.而不是0 到255，这也是很容易搞错的地方，
	byte的实际范围是 -128 ~ 127 ，即Integer的cache的范围，因此将int转换到
	因此，将byte转int应该这样写
	int unsignedByte = signedByte >= 0 ? signedByte : 256 + signedByte;  
	将int转byte应该这样写（这个逻辑和(byte)的转换效果应该是等同的）
	int byteValue;  
	int temp = intValue % 256;  
	if ( intValue < 0) {  
	  byteValue =  temp < -128 ? 256 + temp : temp;  
	}  
	else {  
	  byteValue =  temp > 127 ? temp - 256 : temp;  
	}  
	System.out.println();
	System.out.println(byte2hex(md));
	*/
	
	/**
	 * 常量：所有数字字符
	 */
	public static final char[] NUMBERS = "1234567890".toCharArray();
	/**
	 * 常量：十六进制数字字符
	 */
	public static final char[] HEX_NUMBERS = "1234567890ABCDEFabcdef".toCharArray();
	/**
	 * 常量：所有大写字母
	 */
	public static final char[] ALPHA_UPPERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	/**
	 * 常量：所有小写字母
	 */
	public static final char[] ALPHA_LOWERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	/**
	 * 常量：大写和小写字母
	 */
	public static final char[] ALPHAS = "ABCDEFGHIJKLMNOPQRSTNVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	/**
	 * 常量：所有常用标点符号和空格
	 */
	public static final char[] SYMBOLS = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray();
	/**
	 * 常量：字母，数字和下划线
	 */
	public static final char[] ALPHA_NUM_UNDERLINE = "ABCDEFGHIJKLMNOPQRSTNVWXYZabcdefghijklmnopqrstuvwxyz1234567890_".toCharArray();
	/**
	 * 常量：允许在URL中出现的字符，包含字母数字下划线和其他常用符号
	 */
	public static final char[] CHARS_IN_URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890_&?=#%;~,./-+".toCharArray();

	public static final String JAP_POINT=new String(new char[]{12539,65381,40658,65378,65379,9834, 12316, 65533,8722,8810,8811,63});
	public static final String CHN_POINT=new String(new char[]{183,  183,  40657,'『','』',    65374 ,65374,13199,'-','《','》' ,'？'});
	
	/**
	 *  是否为数字
	 * @param c
	 * @return true if char is a number
	 */
	public static final boolean isNumber(char c) {
		return c >= 48 && c <= 57;
	}

	/**
	 *  是否为空格(含中文空格)
	 * @param c
	 * @return true if the char is space (chinese space included)
	 */
	public static final boolean isSpace(char c) {
		return c == 32 || c == 12288;
	}

	/**
	 *  是否为大写字母
	 * @param c
	 * @return true if the char is a alphabat in upper case
	 */
	public static final boolean isUpperAlpha(char c) {
		return c >= 65 && c <= 90;
	}

	/**
	 *  是否为小写字母
	 * @param c
	 * @return true if the char is alphabat in lower case.
	 */
	public static final boolean isLowerAlpha(char c) {
		return c >= 97 && c <= 122;
	}

	/**
	 * 是否为各种符号
	 * @param c
	 * @return true if the char is a symbol
	 */
	public static boolean isSymbol(char c) {
		return (c >= 32 && c <= 47) || (c >= 58 && c <= 64) || (c >= 91 && c <= 96) || (c > 122 && c < 127);
	}

	/**
	 * 是否为控制字符
	 * @param c
	 * @return true if the char is not a visible character
	 */
	public static final boolean isCtrl(char c) {
		return (c < 32) || c > 255;
	}

	public static boolean isChinese(char c) {
		return c>=0x4e00 && c<=0x9fa5;
	}
	
	/**
	 * 是否为东亚字符(含符号)
	 * @param c
	 * @return <tt>true</tt> if char is chinese or japanese.., otherwise <tt>false</tt>
	 */
	public static boolean isAsian(char c) {
		return (c > 255 && c != 65279);
	}

	/**
	 * 是否为GB18030符号或控制字符
	 * @param c
	 * @return <tt>true</tt> if char is a 全角符号 
	 */
	public static boolean isAsianSymbol(char c) {
		if (c > 19968 && c < 40869)
			return false;
		if (isKatakana(c) || isNumberSBC(c) || isHiragana(c) || isSpace(c) || isAlphaSBC(c))
			return false;
		return true;
	}
	public static final IntRange SBC_ALPHA_UPPER = new IntRange(65313, 65338);
	public static final IntRange SBC_ALPHA_LOWER = new IntRange(65345, 65370);
	public static final IntRangeGroup SBC_ALPHA = new IntRangeGroup(SBC_ALPHA_LOWER, SBC_ALPHA_UPPER);
	public static final IntRange SBC_NUMBER = new IntRange(65296, 65305);
	public static final IntRange SBC_CHARS_WITHOUT_SPACE = new IntRange(65281,65374);
	public static final char SBC_SPACE=(char) 12288;
	/**
	 * 是否为GB18030全角数字
	 * @param c
	 * @return
	 */
	public static final boolean isNumberSBC(char c) {
		return SBC_NUMBER.contains(c);
	}

	/**
	 * 是否为GB18030全角字母
	 * @param c
	 * @return true if a char is a 全角中文字母
	 */
	public static final boolean isAlphaSBC(char c) {
		return SBC_ALPHA.contains((int) c);// (c>=65313 && c<=65338) ||(c>=65345
											// && c<=65370);
	}

	/** 
	 * 是否为GB18030片假名
	 * @param c
	 * @return true if the char is a 片假名
	 */
	public static final boolean isKatakana(char c) {
		return (c >= 12449 && c <= 12542);
	}

	/**
	 * 是否为GB18030 平假名
	 * @param c
	 * @return 如果是平假名返回true，反之
	 */
	public static final boolean isHiragana(char c) {
		return (c >= 12353 && c <= 12435);
	}

	/**
	 * 获得字符的类型
	 * @param c
	 * @return enum CharType
	 */
	public static CharType getType(char c) {
		if (isUpperAlpha(c) || isLowerAlpha(c)) {
			return CharType.ALPHA;
		} else if (isNumber(c)) {
			return CharType.NUMBER;
		} else if (isSymbol(c)) {
			return CharType.SYMBOL;
		} else if (isSpace(c)) {
			return CharType.SPACE;
		} else if (isCtrl(c)) {
			return CharType.CTRL;
		} else if (isAsianSymbol(c)) {
			return CharType.ASIAN_SYMBOL;
		} else {
			return CharType.ASIAN;
		}
	}
	
	public enum CharType {
		// 字母 数字 符号 控制字符 空格
		ALPHA, NUMBER, SYMBOL, CTRL, SPACE, ASIAN_SYMBOL, // 东亚字符中的符号（以GB18030编码为准）
		ASIAN, // 东亚字符（不含符号）
	}
	
	/**
	 * 转全角的函数(SBC case)
	 * 全角空格为12288，半角空格为32
	 * 其他字符半角(33-126)与全角(65281-65374)的对应关系是：均相差65248
	 * @param input
	 * @return 全角字符的文字
	 */
	public static String ToSBC(String input) {
		// 半角转全角：
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 32) {
				c[i] = (char) 12288;
				continue;
			}
			if (c[i] < 127)
				c[i] = (char) (c[i] + 65248);
		}
		return new String(c);
	}

	/**
	 * 半角字符转全角
	 * @param c
	 * @return 全角字符
	 */
	public static char toSBC(char c) {
		if (c == 32)return SBC_SPACE;
		if (c < 127){
			return (char) (c + 65248);
		}
		return c;
	}
	
	/**
	*全角字符转半角(DBC case)
	*全角空格为12288，半角空格为32
	*其他字符半角(33-126)与全角(65281-65374)的对应关系是：均相差65248
	*/
	public static String toDBC(String input) {
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 12288) {
				c[i] = (char) 32;
				continue;
			}
			if (c[i] > 65280 && c[i] < 65375)
				c[i] = (char) (c[i] - 65248);
		}
		return new String(c);
	}
	
	/**
	 * 全角字符转半角字符
	 * @param c
	 * @return 半角字符
	 */
	public static char toDBC(char c) {
		if (c == 12288)return (char) 32;
		if (c > 65280 && c < 65375)
			c = (char) (c - 65248);
		return c;
	}
	
	/**
	 * 确保unicode字符串能安全的转换为GB18030，不会丢弃字符。一些GB18030不支持的字符转换到接近的字符上。
	 * @param line
	 * @return
	 */
	public static String toGB18030(String line){
		line = StringUtils.replaceChars(line, JAP_POINT, CHN_POINT);
		char[] cs = line.toCharArray();
		boolean flag = false;
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];
			if (c > 65379 && c < 65440) {
				cs[i] = (char) ((int) cs[i] - 52933);
				flag = true;
			}
		}
		if (flag) {
			line = new String(cs);
		}
		return line;
	}
}
