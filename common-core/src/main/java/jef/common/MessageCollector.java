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
package jef.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.tools.StringUtils;

/**
 * 本类是线程安全的，用于收集处理过程中，单线程或多线程的各种提示信息
 * @author Administrator
 */
public class MessageCollector implements Serializable,Iterable<String>{
	private static final long serialVersionUID = 4301312475200231841L;
	
	private List<String> list;
	
	/**
	 * 构造
	 */
	public MessageCollector(){
		list=new ArrayList<String>();
	}
	public MessageCollector(int size){
		list=new ArrayList<String>(size);
	}
	public synchronized void addMessage(String msg){
		list.add(msg);
	}
	public synchronized String toString(String spChar){
		int size=list.size();
		if(size>0){
			StringBuilder sb=new StringBuilder(64);
			sb.append(list.get(0));
			for(int i=1;i<size;i++){
				sb.append(spChar).append(list.get(i));
			}
			return sb.toString();
		}
		return "";
	}
	public synchronized String toString(){
		return toString(StringUtils.CRLF_STR);
	}
	public synchronized String toHtmlString(){
		return toString("<br>");
	}
	public synchronized String[] toArray(){
		return list.toArray(new String[list.size()]);
	}
	public synchronized int size(){
		return list.size();
	}
	public synchronized boolean isEmpty(){
		return list.isEmpty();
	}
	public synchronized void clear(){
		list.clear();
	}
	public Iterator<String> iterator() {
		return list.iterator();
	}
}
