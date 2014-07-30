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

import org.apache.commons.lang.ArrayUtils;

public class StringSpliter {
	/**
	 * 这个对象是为了在复杂的字符串分割过程中，避免在内存中产生过多的String对象,实现节约资源提供性能的目的,
	 * 尤其是在对较长的String对象操作时，效率要高出很多。
	 * 实现原理是使用了Substring类，由于Substring类只通过简单的int型数字记录分割点的位置，不实际分割字串对象，所以节省系统资源
	 * 
	 * Chart of a String cutter 
	 * -----------============------------------
	 * |           |         |                 |
	 * begin          keyStr                 end
	 * 
	 */
	private Substring keyword;
	private Substring source;
	public static final int MODE_FROM_LEFT = 0;// 从左向右查找
	public static final int MODE_FROM_RIGHT = 1;// 从右向左查找
	private int findMode = MODE_FROM_LEFT;

	public StringSpliter(Substring source) {
		this.source = source;
		this.keyword = null;
	}

//	public StringSpliter(Substring source, int begin, int end) {
//		this.source = source;
//		this.keyword = source.sub(begin, end);
//	}

	public StringSpliter(String source){
		this.source = new Substring(source);
		this.keyword = null;
	}
	
	public Substring getKeyword() {
		return keyword;
	}

	public Substring getSource() {
		return source;
	}

	public void setSource(Substring source) {
		this.source = source;
		this.keyword=null;
	}
	
	/**
	 * 将key向两边扩展，如果两边的相邻字符和指定的字符相同的话，将其扩展到key的范围
	 * @param chars
	 */
	public boolean expandKey(char[] chars){
		boolean res=expandKeyLeft(chars);
		if(expandKeyRight(chars)){
			res=true;
		}
		return res;
	}

	/**
	 * 如果左侧字符在指定列表中，则向左扩展key
	 * @param chars
	 * @return
	 */
	public boolean expandKeyLeft(char[] chars){
		boolean res=false;
		if(keyword==null)return res;
		Substring left=getLeft();
		while(left.lastCharacter()!=null && ArrayUtils.contains(chars,left.lastCharacter().charValue())){
			left.setEnd(left.getEnd()-1);
			res=true;
		}
		if(res)this.keyword=source.subAbsoultOffset(left.getEnd(), keyword.getEnd());
		return res;
	}

	/**
	 * 如果右侧字符在指定的列表中，则向右扩展key
	 * @param chars
	 * @return
	 */
	public boolean expandKeyRight(char[] chars){
		boolean res=false;
		if(keyword==null)return res;
		Substring right=getRight();
		while(right.firstCharacter()!=null && ArrayUtils.contains(chars,right.firstCharacter().charValue())){
			right.setStart(right.getStart()+1);
			res=true;
		}
		if(res)this.keyword=source.subAbsoultOffset(keyword.getStart(), right.getStart());
		return res;
	}
	
	public boolean expandKeyLeft(int count){
		if(source.getStart()>keyword.getStart()-count){//如果key越过souce边界，则返回
			return false;			
		}
		if(keyword.getEnd()<keyword.getStart()-count){//如果key越过key end边界，则返回
			return false;			
		}
		keyword.setStart(keyword.getStart()-count);
		return true;
		
	}
	
	public boolean expandKeyRight(int count){
		if(source.getEnd()<keyword.getEnd()+count){
			return false;	
		}
		if(keyword.getStart()>keyword.getEnd()+count){
			return false;	
		}
		keyword.setEnd(keyword.getEnd()+count);
		return true;
	}
	
	/**
	 * 向两端扩张key的范围，直到出现指定的字符才停下（指定字符不含）
	 * @param chars
	 * @return
	 */
	public boolean expandKeyUntil(char[] chars){
		boolean res=expandKeyLeftUntil(chars);
		if(expandKeyRightUntil(chars)){
			res=true;
		}
		return res;
	}
	
	/**
	 * 向左扩张Key的范围，直到出现指定的字符才停下（指定字符不含）
	 * @param chars
	 * @return
	 */
	public boolean expandKeyLeftUntil(char[] chars){
		boolean res=false;
		if(keyword==null)return res;
		Substring left=getLeft();
		while(left.lastCharacter()!=null && !ArrayUtils.contains(chars,left.lastCharacter().charValue())){
			left.setEnd(left.getEnd()-1);
			res=true;
		}
		if(res)this.keyword=source.subAbsoultOffset(left.getEnd(), keyword.getEnd());
		return res;
	}
	
	/**
	 * 向右扩张Key的范围，直到出现指定的字符才停下（指定字符不含）
	 * @param chars
	 * @return
	 */
	public boolean expandKeyRightUntil(char[] chars){
		boolean res=false;
		if(keyword==null)return res;
		Substring right=getRight();
		while(right.firstCharacter()!=null && !ArrayUtils.contains(chars,right.firstCharacter().charValue())){
			right.setStart(right.getStart()+1);
			res=true;
		}
		if(res)this.keyword=source.subAbsoultOffset(keyword.getStart(), right.getStart());
		return res;
	}
	/**
	 * 获取左边部分
	 * @return
	 */
	public Substring getLeft() {
		if (keyword == null) {
			if (findMode == MODE_FROM_LEFT) {
				return source;
			} else {
				return null;
			}
		} else {
			return source.subAbsoultOffset(source.getStart(), keyword.getStart());
		}
	}

	/**
	 * 获取右边部分
	 * 
	 * @return
	 */
	public Substring getRight() {
		if (keyword == null) {
			if (findMode == MODE_FROM_LEFT) {
				return null;
			} else {
				return source;
			}
		} else {
			return source.subAbsoultOffset(keyword.getEnd(), source.getEnd());
		}
	}

	/**
	 * 得到带key的左边部分
	 */
	public Substring getLeftWithKey() {
		if (keyword == null) {
			if (findMode == MODE_FROM_LEFT) {
				return null;
			} else {
				return source;
			}
		} else {
			return source.subAbsoultOffset(source.getStart(), keyword.getEnd());
		}
	}

	/**
	 * 得到带key的右边部分
	 */
	public Substring getRightWithKey() {
		if (keyword == null) {
			if (findMode == MODE_FROM_LEFT) {
				return null;
			} else {
				return source;
			}
		} else {
			return source.subAbsoultOffset(keyword.getStart(), source.getEnd());
		}
	}

	/**
	 * 设置一个值，查找，如果找到就形成keyStr对象，从而将对象可拆分为三个Substring.
	 * @param str
	 */
	public boolean setKey(String str) {
		if(str==null){
			this.keyword = null;
			return false;
		}
		int n=-1;
		if (findMode == MODE_FROM_RIGHT) {
			n= source.lastIndexOf(str);
		} else {
			n= source.indexOf(str);
		}
		if(n>-1){
			this.keyword = source.sub(n, n+str.length());
			return true;
		}else{
			this.keyword = null;
			return false;
		}
	}
	
	public boolean setKey(int ketStart,int keyEnd) {
		try{
			this.keyword=source.sub(ketStart, keyEnd);
			return true;
		}catch (IllegalArgumentException e){
			return false;
		}
	}
	
	public boolean setKey(char str) {
		if(str==0){
			this.keyword = null;
			return false;
		}
		int n=-1;
		if (findMode == MODE_FROM_RIGHT) {
			n= source.lastIndexOf(str);
		} else {
			n= source.indexOf(str);
		}
		if(n>-1){
			this.keyword = source.sub(n, n+1);
			return true;
		}else{
			this.keyword = null;
			return false;
		}
	}
//目前算法效率不够高，还可以继续优化
	public boolean setKeyOfAny(String[] ss) {
		int find=-1;
		int length=0;
		for(String str:ss){
			if (findMode == MODE_FROM_RIGHT) {
				int n= source.lastIndexOf(str);
				if(n>find || find==-1){
					find=n;
					length=str.length();
				}
			} else {
				int n= source.indexOf(str);
				if(n>-1 && (n<find || find==-1)){
					find=n;
					length=str.length();
				}
			}
		}
		if(find>-1){
			this.keyword = source.sub(find, find+length);
		}else{
			this.keyword = null;
		}
		return keyword!=null;
	}

	public boolean setKeyOfAny(char[] chars) {
		int find=-1;
		for(char c:chars){
			if (findMode == MODE_FROM_RIGHT) {
				int n= source.lastIndexOf(c);
				if(n>find || find==-1){
					find=n;
				}
			} else {
				int n= source.indexOf(c);
				if(n>-1 && (n<find || find==-1)){
					find=n;
				}
			}
		}
		if(find>-1){
			this.keyword = source.sub(find, find+1);
		}else{
			this.keyword = null;
		}
		return keyword!=null;
	}

	public int getMode() {
		return findMode;
	}

	public void setMode(int mode) {
		this.findMode = mode;
	}
}
