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


/**
 * 描述一个范围
 * @author Administrator
 */
public interface Range<T extends Comparable<T>> extends Serializable {
	/**
	 * 得到范围中最大的值
	 * @return
	 */
	public abstract T getGreatestValue();

	/**
	 * 得到范围中最小的值
	 * @return
	 */
	public abstract T getLeastValue();

	/**
	 * 描述范围是否连续
	 * @return
	 */
	public abstract boolean isContinuous();
	
	/**
	 * 判断给定的值是否在范围中
	 * @param obj
	 * @return
	 */
	public abstract boolean contains(T obj);
}
