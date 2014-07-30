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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jef.tools.ArrayUtils;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 汉字拼音工具，使用pinyin4j
 * 
 * pinyin4j支持将汉字转化成六种拼音表示法。其对应关系是:
 * <table width="90%" border=1>
 * <tr>
 * <td><b>汉语拼音-Hanyu Pinyin</b></td>
 * <td><b>即现在大陆主流的汉语拼音</b></td>
 * </tr>
 * <tr>
 * <td>通用拼音-Tongyong Pinyin</td>
 * <td>台湾现在正式的官方汉语音译编码</td>
 * </tr>
 * <tr>
 * <td>威妥玛拼音(威玛拼法)-Wade-Giles Pinyin</td>
 * <td>19世纪中叶由英国人威妥玛（Thomas Francis Wade）发明，后由翟理斯（Herbert Allen
 * Giles）完成修订，并编入其所撰写的汉英字典</td>
 * </tr>
 * <tr>
 * <td>注音符号第二式-MPSII Pinyin</td>
 * <td>国语注音符号，台湾地区的主流注音方式</td>
 * </tr>
 * <tr>
 * <td>耶鲁拼法-Yale Pinyin</td>
 * <td>第二次世界大战期间由美国军方发明的编码系统，主要为了让在中国地区作战的美军士兵能够快速地熟悉汉语发音，很少人用</td>
 * </tr>
 * <tr>
 * <td>国语罗马字-Gwoyeu Romatzyh。</td>
 * <td>国语罗马字，它是由林语堂提议建立的，在1928年由国民政府大学堂颁布推行，很少人用</td>
 * </tr>
 * </table>
 * 
 * 关于输出格式Format 声调选项 WITH_TONE_NUMBER(以数字代替声调) : zhong1 zhong4 WITHOUT_TONE
 * (无声调) : zhong zhong WITH_TONE_MARK (有声调) : zhōng zhòng (需要用unicode支持)
 * 
 * u v u(上两点)的的表示方式，吕的输出为 WITH_U_AND_COLON : lu:3 (u加上冒号) WITH_V : lv3
 * (用v表示u上两点,符合现行输入法习惯) WITH_U_UNICODE : lü3 (用unocide的U上两点符号)
 * 
 * @author Jiyi
 * 
 */
public class PinyinUtil {
	
	private static final HanyuPinyinOutputFormat FORMAT_DEFAULT = new HanyuPinyinOutputFormat();
	private static final HanyuPinyinOutputFormat FORMAT_WITH_TONE = new HanyuPinyinOutputFormat();
	static{
		FORMAT_DEFAULT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		FORMAT_DEFAULT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		FORMAT_DEFAULT.setVCharType(HanyuPinyinVCharType.WITH_V);	
		
		FORMAT_WITH_TONE.setVCharType(HanyuPinyinVCharType.WITH_V);
	}
	
	/**
	 * 将指定字符串转为拼音
	 * 
	 * @param source
	 */
	public static String getPinyin(String source){
		return getPinyin(source,"");
	}
	
	/**
	 * 将指定字符串转为拼音
	 * 
	 * @param source
	 * @param sep 子与子枝间的分隔符
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String getPinyin(String source,String sep){
		try {
			return PinyinHelper.toHanyuPinyinString(source, FORMAT_DEFAULT, sep);
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	
	static class PinyinResult{
		String result;
		final LinkedList<String>       afterResult=new LinkedList<String>();
		PinyinResult parent;
		public PinyinResult(String s,PinyinResult parent) {
			this.result=s;
			this.parent=parent;
		}
		public void add(String newPinyin){
			afterResult.addFirst(newPinyin);
		}
		public List<PinyinResult> add(String[] newPinyin){
			if(newPinyin.length>1){
				List<PinyinResult> children=new ArrayList<PinyinResult>(3);
				for(String s:newPinyin){
					PinyinResult choise=new PinyinResult(s,this);
					children.add(choise);
				}
				return children;
			}
			throw new IllegalArgumentException();
		}
		public String compute(String sep) {
			List<String> path=new ArrayList<String>();
			PinyinResult obj=this;
			while(obj.parent!=null){
				path.addAll(obj.afterResult);
				path.add(obj.result);
				obj=obj.parent;
			}
			path.addAll(obj.afterResult);
			StringBuilder sb=new StringBuilder();
			for(int i=path.size()-1;i>=0;i--){
				sb.append(path.get(i)).append(sep);
			}
			return sb.toString();
		}
	}
	
	/**
	 * 将指定字符串转为拼音，如果有多音字列出所有组合
	 * 
	 * @param src 要转换的文字
	 * @return 拼音,原文中的英文不变
	 * @throws
	 */
	public static String[] getAllPingYin(String src) {
		return getAllPingYin(src," ");
	}
	
	/**
	 * 将指定字符串转为拼音，如果有多音字列出所有组合
	 * 
	 * @param src 要转换的文字
	 * @param sep 分隔符
	 * @return 拼音,原文中的英文不变
	 * @throws
	 */
	public static String[] getAllPingYin(String src,String sep) {
		PinyinResult root=new PinyinResult(null,null);//根节点
		List<PinyinResult> lastLeaves=new ArrayList<PinyinResult>();
		lastLeaves.add(root);
		
		try {
			StringBuilder english=new StringBuilder();
			for (int i = 0; i < src.length(); i++) {
				char c=src.charAt(i);
				String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT_DEFAULT);
				if(pinyin==null){
					english.append(c);
					continue;
				}
				if(english.length()>0){
					String s=english.toString();
					english.setLength(0);
					for(PinyinResult r:lastLeaves){
						r.add(s);
					}
				}
				if(pinyin.length>1){
					pinyin=ArrayUtils.removeDups(pinyin);						
				}
				if(pinyin.length==1){
					for(PinyinResult r:lastLeaves){
						r.add(pinyin[0]);
					}
				}else{
					List<PinyinResult> newleaves=new ArrayList<PinyinResult>();
					for(PinyinResult pinyinLeaf:lastLeaves){
						newleaves.addAll(pinyinLeaf.add(pinyin));	
					}
					lastLeaves=newleaves;
				}
			}
			//将所有拼音的组合构成一棵树
			List<String> results=new ArrayList<String>(lastLeaves.size());
			for(PinyinResult pinyin:lastLeaves){
				results.add(pinyin.compute(sep));
			}
			return results.toArray(new String[results.size()]);
		} catch (BadHanyuPinyinOutputFormatCombination ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}

	/**
	 * 返回中文的首字母
	 */
	public static String getPinYinHeadChar(String str) {
		StringBuilder  convert = new StringBuilder();
		for (int j = 0; j < str.length(); j++) {
			char c = str.charAt(j);
			String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
			if (pinyinArray != null) {
				convert.append(pinyinArray[0].charAt(0));
			} else {
				convert.append(c);
			}
		}
		return convert.toString();
	}
}
