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
package jef.database;

/**
 * 描述一个对指定类型的函数操作，用来将不同的值转化为分表时的表名后缀
 * @author Administrator
 *
 */
public enum KeyFunction {
	
	/**
	 * @deprecated 建议用functionClass来实现，目前此方法只支持按10求余。故不推荐使用
	 */
	@Deprecated
	MODULUS, //取余
	
	/**
	 * 对指定的日期型字段，获取yyyy格式年份
	 */
	YEAR,  //年
	/**
	 * 对指定的日期型字段，获取yyyyMM格式年月
	 */
	YEAR_MONTH,//年+月
	/**
	 * 对指定的日期型字段，获取MM格式月份
	 */
	MONTH,  //月
	/**
	 * 对指定的日期型字段，获取dd格式日
	 */
	DAY,    //日
	/**
	 * 对指定日期类型字段，取年的最后两位
	 */
	YEAR_LAST2,
	/**
	 * 对指定日期类型字段，取小时数字(24)
	 */
	HH24,//24小时的小时数
	/**
	 * 对指定日期类型字段，取星期(0表示星期日，1~6)
	 */
	WEEKDAY,
	/**
	 * 对任意类型的字段，将其数值转换为String<p>
	 * <b>默认值</b>
	 */
	RAW//不处理
}
