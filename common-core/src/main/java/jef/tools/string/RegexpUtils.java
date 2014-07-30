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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 用于生成正則表达式相关计算的工具
 * @author jiyi
 */
public abstract class RegexpUtils {
	//用于匹配任意字符串的非贪婪表达式
	public static final String MATCH_ANY_STRING=".*?";
	
	//正则表达式缓存池，为了防止反复编译相同的正则表达式浪费时间，设置长度为64的正则表达式缓存(pattern是线程安全的,Matcher不是)
	private static final int PATTERN_CACHE_SIZE=64;
	private static final Map<String, Pattern> cache=new ConcurrentHashMap<String,Pattern>(PATTERN_CACHE_SIZE);
	
	public static final char[] STAR_QUESTION=new char[]{'*','?','+'};
	
	//常见匹配预定义
	public static final String MATCH_ANY=".";
	public static final String MATCH_NUMBER="\\d";
	public static final String MATCH_NON_NUMBER="\\D";
	public static final String MATCH_BLANK="\\s";
	public static final String MATCH_NON_BLANK="\\S";
	public static final String MATCH_WORD_CHAR="\\w";
	public static final String MATCH_NON_WORD_CHAR="\\W";
	public static final String MATCH_START="^";
	public static final String MATCH_END="$";
	
	//常见数量词预定义
	public static final String COUNT_0_N="*";
	public static final String COUNT_1_N="+";
	public static final String COUNT_0_1="?";
	public static final String COUNT_0_N_RELUCTANT="*?";
	public static final String COUNT_1_N_RELUCTANT="+?";
	public static final String COUNT_0_1_RELUCTANT="??";
	
	public static final Pattern E_MAIL=Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	public static final Pattern URL=Pattern.compile("[a-zA-z]+://[^\\s]*");
	public static final Pattern USER_ACCOUNT=Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{4,15}$");
	public static final Pattern TELEPHONE_NUMBER=Pattern.compile("\\d{3}-\\d{8}|\\d{4}-\\d{7}");
	public static final Pattern CHINA_POSTCODE=Pattern.compile("[1-9]\\d{5}(?!\\d)");
	public static final Pattern PERSONAL_ID_CARD=Pattern.compile("\\d{15}|\\d{18}");
	public static final Pattern IP_ADDR=Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
	public static final Pattern CHINESE_CHAR=Pattern.compile("\\u4e00-\\u9fa5");
	public static final Pattern GET_HREF_LINK=Pattern.compile("(h|H)(r|R)(e|E)(f|F)  *=  *('|\")?(\\w|\\\\|\\/|\\.)+('|\"|  *|>)?");
	public static final Pattern GET_NUM=Pattern.compile("(-?\\d*)(\\.\\d+)?");
	
	/**
	 * 预定义的字符，凡是字符串中出现这些字符的话，都需要转义
	 * 目前设置的转义字符有14个
	 */
	public static final char[] REGEXP_KEY_CHARS=new char[]{
		'\\',	//将下一个字符标记为或特殊字符、或原义字符、或后向引用、或八进制转义符
		  //注意：由于替换顺序，作为替换符的\\必须在最前面.
		'(',')',//匹配分组关键字
		'{','}',//匹配次数关键字
		'[',']',//匹配字符集描述字
		'*',	//匹配次数快捷定义：0..n
		'+',	//匹配次数快捷定义: 1..n,也用于描述占有匹配
		'?',	//匹配次数快捷定义: 0..1，也用于辅助描述非贪婪描述（即要求匹配取尽量少的匹配次数)
		'.',	//匹配任意字符
		'$',	//匹配行尾
		'^',	//有两种含义，匹配行首。在[]中时表示对字符集合求非
		'|'		//用于分隔若干项匹配，表示其中之一
		//注1：部分正则表达式规范使用< >来表示单词的开始与结束，实际测试发现Java不支持此规格，因此<,>不是Java中的元字符。Java使用\b来表示单词边界
		//注2：'-'  是元字符，但是很奇怪的，不需要转义，当出现在方括号中时会被当作元字符，不在方括号中会被当作普通字符，
		//但是如果你使用了转义描述，也能得到一样的结果。所以在Java中可转可不转。同样的情况还有'&'符
		//注3：正则表达式在编译中，对于不匹配的括号，会当作字面值在处理，但是对于+ * ?则会严格处理，不会当作字面值，因此这些字符一定要转义。
		//注4：本文中所说的非贪婪均指英语的reluctant，一般来说，正则表达式中有greedy(贪婪)、和reluctant、 possessive(占有)三种匹配方式，
		//由于 possessive方式过于刚性，会造成整体匹配失败，实际使用价值不大，本类不予考虑。
	};
	
	/**
	 * 将使用简易匹配语法的关键字修改为标准正则表达式
	 * 简易语法就是用*表示匹配任意数量字符，用?表示匹配0~1个字符，其他字符一律按照字面理解。 这是为了和Windows用户的习惯相吻合
	 * @param key
	 * @return
	 */
	public static String simpleMatchToRegexp(String key) {
		//将除了* ?之外的字符全部修改为正则表达式原义
		key=escapeRegChars(key,STAR_QUESTION);
		key=StringUtils.replaceEach(key, new String[]{"*","?","+"}, new String[]{".*",".?",".+"});
		return key;
	}

	/**
	 * 得到简易通配的正则pattern
	 * @param key
	 * @param IgnoreCase
	 * @param matchStart
	 * @param matchEnd
	 * @param wildcardSpace
	 * @return
	 */
	public static Pattern simplePattern(String key, boolean IgnoreCase, boolean matchStart, boolean matchEnd, boolean wildcardSpace){
		if (IgnoreCase) {
			key = key.toUpperCase();
		}
		// 将除了指定字符之外的所有正则元字符全部用转义符更改为字面含义
		String regStr = simpleMatchToRegexp(key);
		regStr = ((matchStart) ? "" : RegexpUtils.MATCH_ANY_STRING) + ((wildcardSpace) ? key.replace(" ", "\\s+") : regStr) + ((matchEnd) ? "" : RegexpUtils.MATCH_ANY_STRING);
		Pattern p=Pattern.compile(regStr);
		return p;
	}
	
	/**
	 * 将除了指定字符之外的所有正则元字符全部用转义符更改为字面含义
	 * @param key
	 * @param sTARQUESTION
	 * @return
	 */
	public static String escapeRegChars(String key, char[] keeps) {
		for(char c:REGEXP_KEY_CHARS){
			if(!ArrayUtils.contains(keeps, c)){
				key=key.replace(String.valueOf(c),"\\"+c);
			}
		}
		return key;
	}
	
	/**
	 * 只要字符串中有部分匹配正则表达式，就返回true
	 * @param str
	 * @param regexp
	 * @return
	 */
	public static boolean contains(String str, String regexp){
		return matches(str,regexp,false);
	}
	
	/**
	 * 效果等同于String.matches. 不过可以使用缓存
	 * @param str
	 * @param regexp
	 * @return
	 */
	public static boolean matches(String str,String regexp){
		return matches(str,regexp,true);
	}
	
	/**
	 * 判断字符串和正则表达式是否匹配
	 * @param key
	 * @param regexp
	 * @param strict
	 * @return
	 */
	public static boolean matches(String str,String regexp,boolean strict){
		Matcher m=getMatcher(str,regexp,strict);
		return m.matches();
	}
	
	public static String[] getSimpleMatchResult(String str,String key,boolean strict){
		key=escapeRegChars(key,STAR_QUESTION);
		key=StringUtils.replaceEach(key, new String[]{"*","?","+"}, new String[]{"(.*)","(.?)","(.+)"});
		return getMatcherResult(str,key,strict);
	}

	/**
	 * 得到匹配结果分组。
	 * @param pattern 正则表达式
	 * @param str     字符串
	 * @param strict  使用严格方式，即只有字符完全匹配上正则表达式时才返回结果
	 * @return        如果没有匹配上，返回一个null。
	 */
	public static String[] getMatcherResult(String str,String regexp,boolean strict){
		if(!strict){
			String tmp=StringUtils.remove(str, "\\(");
			tmp=StringUtils.remove(str, "\\)");
			if(tmp.indexOf('(')>-1 && tmp.indexOf(')')>-1){
				//用户给出的正则中已经有了分组信息
			}else{
				regexp="("+regexp+")";//补充一个默认分组
			}
		}
		Matcher m=getMatcher(str,regexp,strict);
		if(!m.matches())return null;
		int n=m.groupCount();
		if(n==0)return new String[]{m.group()};
		String[] result=new String[n];
		for(int i=1;i<=n;i++){
			result[i-1]=m.group(i);
		}
		return result;
	}
	
	private static Matcher getMatcher(String str, String regexp, boolean strict) {
		if(!strict)regexp=MATCH_ANY_STRING+regexp+MATCH_ANY_STRING;
		Pattern p=cache.get(regexp);
		if(p==null){
			p=Pattern.compile(regexp);
			addToCache(p);
		}
		return p.matcher(str);
	}
	//将Pattern缓存 
	private static void addToCache(Pattern p) {
		if(cache.size()==PATTERN_CACHE_SIZE){
			cache.clear();
		}
		cache.put(p.pattern(), p);
	}
}
