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

/**
 * 用于配置数据分库分表（分片）的注解
 * @author jiyi
 *
 */
@Target({TYPE}) 
@Retention(RUNTIME)
public @interface PartitionTable {
	/**
	 * 描述原始表名和分区后缀之间的分隔符，默认"_"
	 */
	String appender() default "_";
	
	/**
	 * 描述多个KEY之间的分隔符，默认""
	 */
	String keySeparator() default "";
	
	/**
	 * 描述分区的计算字段
	 */
	PartitionKey[] key();
}
