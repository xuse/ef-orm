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
package jef.common.wrapper;



/**
 * 一个泛型的容器
 * @author Administrator
 * @param <T>
 */
@SuppressWarnings("serial")
public class Holder<T> implements IHolder<T> {
	private T value;
	
	public Holder() {}

	public Holder(T obj) {
		this.value = obj;
	}

	public T get(){
		return value;
	}
	
	public void set(T obj){
		this.value = obj;	
	}

	@Override
	public String toString() {
		if(value==null)return "";
		return value.toString();
	}
}
