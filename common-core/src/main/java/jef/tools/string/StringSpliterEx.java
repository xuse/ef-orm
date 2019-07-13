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

import  org.apache.commons.lang3.ArrayUtils;

/**
 * 用于描述将一个Substring，用两个keyword分割成为五个部分的情形，
 * 所有的对象均用int型表示其在原String对象上的位置，从而避免String的大量内存操作
 */
public class StringSpliterEx {
	private Substring keyword1;
	private Substring keyword2;
	private Substring source;
	
	//------left-----| key1 | --middle- |key2|----right------  
	
	public static final int MODE_FROM_LEFT = 0;// 从左向右查找
	public static final int MODE_FROM_ENDS = 1;// 从两端向中间查找
	private int findMode = MODE_FROM_LEFT;

	public StringSpliterEx(Substring source) {
		this.source = source;
	}
	public StringSpliterEx(String source) {
		this.source = new Substring(source);
	}
	public Substring getKeyword1() {
		return keyword1;
	}
	public Substring getKeyword2() {
		return keyword2;
	}
	public Substring getSource() {
		return source;
	}
	public void setSource(Substring source) {
		this.source = source;
		this.keyword1=null;
		this.keyword2=null;
	}

	/**
	 * 获取左边部分
	 */
	public Substring getLeft() {
		if(keyword1==null && keyword2==null){
			return null;
		}else if(keyword1==null){
			return source.subAbsoultOffset(source.getStart(), keyword2.getStart());
		}else{
			return source.subAbsoultOffset(source.getStart(), keyword1.getStart());
		}
	}

	/**
	 * 获取右边部分
	 */
	public Substring getRight() {
		if(keyword1==null && keyword2==null){
			return null;
		}else if(keyword2==null){
			return source.subAbsoultOffset(keyword1.getEnd(),source.getEnd());
		}else{
			return source.subAbsoultOffset(keyword2.getEnd(),source.getEnd());
		}
		
	}

	/**
	 * 得到中间部分
	 * @return
	 */
	public Substring getMiddle() {
		if(keyword1==null && keyword2==null){
			return null;
		}else if(keyword1==null){
			return null;
		}else if(keyword2==null){
			return null;
		}else{
			return source.subAbsoultOffset(keyword1.getEnd(), keyword2.getStart());
		}
	}
	
	public static final int RESULT_NO_KEY=3;
	public static final int RESULT_BOTH_KEY=0;
	public static final int RESULT_KEY1_ONLY=1;
	public static final int RESULT_KEY2_ONLY=2;
	/**
	 * 设置一个值，查找，如果找到就形成keyStr对象，从而将对象可拆分为五个Substring.
	 * @param key1, key2
	 */
	public int setKeys(String key1,String key2) {
		StringSpliter sp=new StringSpliter(source);
		sp.setKey(key1);
		if(sp.getKeyword()!=null){
			this.keyword1=sp.getKeyword();
			sp=new StringSpliter(sp.getRight());
			if (findMode == MODE_FROM_ENDS) {
				sp.setMode(StringSpliter.MODE_FROM_RIGHT);
			}
			sp.setKey(key2);
			this.keyword2=sp.getKeyword();
			if(sp.getKeyword()!=null){
				return RESULT_BOTH_KEY;
			}else{
				return RESULT_KEY1_ONLY;
			}
		}else{
			if (findMode == MODE_FROM_ENDS) {
				sp.setMode(StringSpliter.MODE_FROM_RIGHT);	
			}
			sp.setKey(key2);
			this.keyword2=sp.getKeyword();
			if(sp.getKeyword()!=null){
				return RESULT_KEY2_ONLY;	
			}else{
				return RESULT_NO_KEY;	
			}
		}
		
	}

	public int setKeyOfAny(String[] key1,String[] key2) {
		StringSpliter sp=new StringSpliter(source);
		sp.setKeyOfAny(key1);
		if(sp.getKeyword()!=null){
			this.keyword1=sp.getKeyword();
			sp=new StringSpliter(sp.getRight());
			if (findMode == MODE_FROM_ENDS) {
				sp.setMode(StringSpliter.MODE_FROM_RIGHT);
			}
			sp.setKeyOfAny(key2);
			this.keyword2=sp.getKeyword();
			if(sp.getKeyword()!=null){
				return RESULT_BOTH_KEY;
			}else{
				return RESULT_KEY1_ONLY;
			}
		}else{
			if (findMode == MODE_FROM_ENDS) {
				sp.setMode(StringSpliter.MODE_FROM_RIGHT);	
			}
			sp.setKeyOfAny(key2);
			this.keyword2=sp.getKeyword();
			if(sp.getKeyword()!=null){
				return RESULT_KEY2_ONLY;	
			}else{
				return RESULT_NO_KEY;	
			}
		}
	}
	
	/**
	 * 和setKeyOfAny的区别在于，第一个匹配到的元素，在第二组key中只有位置相对的那个字串才有效。
	 * 如果 key1= "<","[" key2=">","]"， 当左<匹配后，右边只有>才能匹配。
	 * @param key1
	 * @param key2
	 * @return
	 */
	public int setKeyPairOfAny(String[] key1,String[] key2) {
		if(key1.length<key2.length){
			throw new IllegalArgumentException("the length between two input keys do not equals!");
		}
		StringSpliter sp=new StringSpliter(source);
		sp.setKeyOfAny(key1);
		if(sp.getKeyword()!=null){
			this.keyword1=sp.getKeyword();
			int n=ArrayUtils.indexOf(key1,this.keyword1.toString());
			if(n>key2.length-1){//key1找到，但没有指定可以匹配的key2
				return RESULT_KEY1_ONLY;
			}
			String realKey2=key2[n];
			sp=new StringSpliter(sp.getRight());
			if (findMode == MODE_FROM_ENDS) {
				sp.setMode(StringSpliter.MODE_FROM_RIGHT);
			}
			sp.setKey(realKey2);
			this.keyword2=sp.getKeyword();
			if(sp.getKeyword()!=null){
				return RESULT_BOTH_KEY;
			}else{
				return RESULT_KEY1_ONLY;
			}
		}else{
			if (findMode == MODE_FROM_ENDS) {
				sp.setMode(StringSpliter.MODE_FROM_RIGHT);	
			}
			sp.setKeyOfAny(key2);
			this.keyword2=sp.getKeyword();
			if(sp.getKeyword()!=null){
				return RESULT_KEY2_ONLY;	
			}else{
				return RESULT_NO_KEY;	
			}
		}
	}

	public int getMode() {
		return findMode;
	}

	public void setMode(int mode) {
		this.findMode = mode;
	}
}
