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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 加在一个映射到数据库的field上，描述该字段上存在单键索引. 
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Indexed {
	/**
	 * 索引名称
	 * @return
	 */
	String name() default "";
	/**
	 * 索引修饰，如bitmap, clustered等
	 * @return
	 */
	String definition() default "";
	/**
	 * 是否倒序
	 * @return
	 */
	boolean desc() default false;
	/**
	 * 键值是否唯一
	 * @return
	 */
	boolean unique() default false;
}
