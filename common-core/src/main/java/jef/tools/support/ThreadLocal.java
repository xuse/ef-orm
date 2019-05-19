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
package jef.tools.support;

import java.lang.reflect.InvocationTargetException;

import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.MethodEx;

/**
 * 编程中碰到一个问题，某个ThreadLocal为所有的线程保存的局部变量。
 * 现在要在一个线程中操作另外一个线程中的局部变量。
 * 从代码看java是为每个线程创建了一个threadlocalMap，将局部变量统一存在在这个map中。
 * 糟糕的是相关的方法和变量都是包机私有的，所以为了突破限制，编写了这个类。
 * 本类使用反射来突破安全性限制。并不是很好的方法。
 * 一个可行的办法是自己编写整个 ThreadLocal的逻辑，用weakHashMap来存放数据。
 * @author Administrator
 *
 * @param <T>
 */
public class ThreadLocal<T> extends java.lang.ThreadLocal<T>{
	private Logger log=LoggerFactory.getLogger(ThreadLocal.class);	
	private static final Class<?> superClass=java.lang.ThreadLocal.class;
	private static boolean isForJDK6=System.getProperty("java.specification.version").startsWith("1.6");
	private MethodEx getMethod;
	private MethodEx setMethod;
	
	public ThreadLocal(){
		super();
	}
	private void initMethod(Class<?> c){
		if(getMethod==null){
			getMethod=BeanUtils.getCompatibleMethod(c, (isForJDK6?"getEntry":"get"), superClass);
		}
		if(setMethod==null){
			setMethod=BeanUtils.getCompatibleMethod(c, "set", superClass,Object.class);
		}
	}
	
	/**
	 * JEF特有的方法，设置指定线程中的局部变量值
s	 * @param 参数
	 * @return void    返回类型
	 * @throws
	 */
	public void set(Thread t,T value){
		try{
			Object threadlocalMap = BeanUtils.invokeMethod(this, "getMap", t);
			if (threadlocalMap != null){
				initMethod(threadlocalMap.getClass());
				setMethod.invoke(threadlocalMap, this, value);
			}else{
				MethodEx createMap=BeanUtils.getCompatibleMethod(superClass, "createMap", Thread.class,Object.class);
				createMap.invoke(this, t,value);
			}
		} catch (ReflectionException e) {
			log.error("error",e);
		} catch (IllegalArgumentException e) {
			log.error("error",e);
		} catch (IllegalAccessException e) {
			log.error("error",e);
		} catch (InvocationTargetException e) {
			log.error("error",e);
		}
	}
	
	/**
	 * JEF特有的方法，获取指定线程中的局部变量值
s	 * @param 参数
	 * @return void    返回类型
	 * @throws
	 */
	@SuppressWarnings("unchecked")
	public T getExistValue(Thread t){
		try{
			Object threadlocalMap = BeanUtils.invokeMethod(this, "getMap", t);
			if (threadlocalMap != null){
				initMethod(threadlocalMap.getClass());
				if(isForJDK6){
					Object entryObject=getMethod.invoke(threadlocalMap,this);
					return (T)BeanUtils.getFieldValue(entryObject, "value");
				}else{
					return (T)getMethod.invoke(threadlocalMap, this);	
				}
			}
			return null;
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (ReflectionException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
  
}
