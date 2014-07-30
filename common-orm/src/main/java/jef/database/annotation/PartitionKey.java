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
package jef.database.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jef.database.KeyFunction;

@Target(TYPE) 
@Retention(RUNTIME)
public @interface PartitionKey {
	/**
	 * 指定对应的字段(分表分库规则的依据字段)
	 * @return
	 */
	String field();
	/**
	 * 指定字段要做的函数(一般是对日期进行处理)
	 * @return
	 */
	KeyFunction function() default KeyFunction.RAW;
	/**
	 * 指定一个类名称，来提供对分表字段的处理。<br><br>
	 * 例如，配置为ModulusFunction.class，便表示对指定的分表字段进行取模运算。<br>
	 * 
	 * 此参数经常需要和functionClassConstructorParams注解同时使用，用于提供构造处理函数所需的构造参数。<br>
	 * <p>
	 * <b>此注解不可与function注解同时使用!</b><br>
	 * 
	 * JEF-ORM默认提供了以下的PartitionFunction类：
	 * <li>jef.database.partition.ModulusFunction</li>
	 * 
	 * <p>
	 * 一个例子是这样的：
	 * <pre>@PartitionKey(
	 *        field ="amount", length = 2,
	 *        functionClass=ModulusFunction.class,
	 *        functionClassConstructorParams={"5"}
	 *        )
	 * </pre>
	 * 这个例子表示，对amount字段，按5取模，然后补充到两位数后作为分表后缀名。
	 * 
	 * @See jef.database.partition.ModulusFunction
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends PartitionFunction> functionClass() default PartitionFunction.class;
	
	/**
	 * 此注解必须与functionClass同时使用，描述functionClass构造时的构造参数<br>
	 * @return
	 */
	String[] functionConstructorParams() default {};
	
	/**
	 * 当此值设置为true时，当前ParitionKey计算出来的字符串不是作为分表名称的一部分，而是作为一个独立的数据库名。<br>
	 * 这种用法用于当应用部署在多个独立的数据库上时，可以实现跨库的数据库操作<br>
	 * 
	 * 每次数据库操作可以通过这个对象得到其他的数据库的连接，从而实现跨库的数据库操作。<br>
	 * 但是跨库数据库操作的事务不会加以处理，需要在底层使用JOTM的XA事务管理。<br>
	 */
	boolean isDbName() default false;
	/**
	 * 当指定字段无值时，使用的缺省值值<br>
	 * @return
	 */
	String defaultWhenFieldIsNull() default "";
	/**
	 * 指定字符串长度，0表示不限制。<br>
	 * 当长度限定时，如果不足会填充。如果超过会截断<br>
	 */
	int length() default 0;
	/**
	 * 填充字符，当不满足长度要求时使用此字符在左侧填充
	 */
	char filler() default '0';
	/**
	 * 是否支持Session中获取数据<br>
	 * 为Asiainfo-linkage的特殊分表而设计的扩展字段。用于描述分表参数的来源获取方式，即分表维度值除了从Query中获得外，还可以从其他位置获得。<br>
	 * JEF本身不使用此字段。因为JEF已经能自动化地从Query中获取足够的维度信息来进行分表计算，此外JEF还提供的attribute等手段携带足够的附加信息。<br>
	 * 参照：
	 * <li>SESSION_ONLY=1</li>
	 * <li>QUERY_ONLY=2</li>
	 * <li>QUERY_FIRST=3</li>
	 * <li>SESSION_FIRST=4</li>
	 * @return
	 */
	int sourceDesc() default 0;
}
