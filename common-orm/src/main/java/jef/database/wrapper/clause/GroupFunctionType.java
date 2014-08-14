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
package jef.database.wrapper.clause;

/**
 * 描述Select中各个字段的分组特性
 * @author jiyi
 *
 */
public enum GroupFunctionType {
	/**
	 * 分组统计——平均值
	 * 这是唯一一个无法后来计算的内存分组函数
	 */
	AVG,
	/**
	 * 分组统计，总计值
	 */
	SUM,
	/**
	 * 分组统计最小值
	 */
	MIN,
	/**
	 * 分组统计——最大值
	 */
	MAX,
	/**
	 * 分组统计——计数
	 */
	COUNT,
	/**
	 * 分组统计——数组组合
	 */
	ARRAY_TO_LIST,
	/**
	 * 非以上的任何一种情况 
	 */
	NORMAL,
	/**
	 * 分组字段
	 */
	GROUP,
}
