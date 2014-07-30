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

import java.util.Enumeration;
import java.util.Iterator;

/**
 * 将讨厌的Enumeration 对象封装为Iterable的对象
 * 
 * 一些早期接口使用Enumeration对象，并且不支持泛型。
 * @author Administrator
 */
public class EnumerationWrapper<T> implements Iterable<T>{
	Enumeration<T> e;
	
	public EnumerationWrapper(Enumeration<T> values) {
		this.e=values;
	}

	public Iterator<T> iterator() {
		return new Iterator<T>(){
			public boolean hasNext() {
				return e.hasMoreElements();
			}

			public T next() {
				return e.nextElement();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
}
