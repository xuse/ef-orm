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

import java.lang.reflect.Field;
import java.util.HashMap;

import jef.common.log.LogUtil;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.CloneUtils;
import jef.tools.reflect.FieldEx;

/**
 * 利用反射备份一个对象(浅拷贝)
 * @author Administrator
 *
 * @param <T>
 */
public class Backup<T> {
	/**
	 * 备份的对象
	 */
	private Object obj;
	/**
	 * 备份的域值
	 */
	private HashMap<Field,Object> fields;
	
	public Backup(T obj, boolean deepCopy){
		this.obj=obj;
		init(deepCopy);
	}
	
	/**
	 * 构造
	 * @param obj
	 */
	public Backup(T obj){
		this.obj=obj;
		init(false);
	}
	
	/*
	 * 深拷贝：
	 * 递归复制所有域，直到该域是一个不可变对象为止：
	 * 目前已知的不可变对象:
	 * 八原生，八原生包裝类，String，
	 * java.lang
	 */
	private void init(boolean deepCopy){
		FieldEx[] fs=BeanUtils.getFields(obj.getClass());
		fields=new HashMap<Field,Object>(fs.length);
		for(FieldEx f: fs){
			try {
				if(deepCopy){
					fields.put(f.getJavaField(), CloneUtils.clone(f.get(obj)));
				}else{
					fields.put(f.getJavaField(), f.get(obj));
				}
			} catch (IllegalArgumentException e) {
				LogUtil.exception(e);
			}
		}
	}
	
	/**
	 * 变更目标
	 * @param obj
	 */
	public void changeTarget(T obj){
		this.obj=obj;
	}
	
	/**
	 * 比较对象
	 */
	public void compare(){
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 恢复对象
	 */
	public void restore(){
		for(Field f: fields.keySet()){
			try {
				f.set(obj, fields.get(f));
			} catch (IllegalArgumentException e) {
				LogUtil.exception(e);
			} catch (IllegalAccessException e) {
				LogUtil.exception(e);
			}
		}
	}
}
