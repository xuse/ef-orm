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

import java.util.Iterator;

/**
 * 这也是基于分隔符的分词器，作用相当于 StringTokenizer 的强化版
 * @author Administrator
 *
 */
public class SubstringIterator implements Iterator<Substring>{
	Substring source;
	Substring left;
	Substring lastKey;
	String[] keywords;
	boolean includeKeys=true;
	
	/**
	 * @param str 要分割的字符串
	 * @param keywords 用于分割的字符串列表
	 */
	public SubstringIterator(Substring str, String[] keywords){
		this(str, keywords,true);
	}
	
	/**
	 * 作用相当于 StringTokenizer 的强化版
	 * @param str 要分割的字符串
	 * @param keywords 用于分割的字符串列表
	 * @param includeKeys 如果为yes,则匹配字符串会在分离串的前面输出
	 */
	public SubstringIterator(Substring str, String[] keywords,boolean includeKeys){
		this.source=str;
		this.keywords=keywords;
		this.left=source;
		this.includeKeys=includeKeys;
	}

	public boolean hasNext() {
		if(includeKeys && lastKey!=null){
			return !lastKey.isEmpty();
		}else{
			return left!=null;	
		}
	}

	public Substring next() {
		StringSpliter sp=new StringSpliter(left);
		Substring result=null;
		if(sp.setKeyOfAny(keywords)){
			result=sp.getLeft();
		}else{
			result=sp.getSource();
		}
		if(includeKeys && lastKey!=null){
			result=Substring.merge(lastKey,result);
		}
		lastKey=sp.getKeyword();
		left=sp.getRight();
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Substring getLeft() {
		return left;
	}
	
}
