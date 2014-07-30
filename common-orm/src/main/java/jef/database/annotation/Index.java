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
 * 加在类上，用来描述一个索引
 * @author Administrator
 *
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Index {
	/**
	 * 索引的各个字段名称（是java字段名，不是列名）
	 * @return
	 */
	String[] fields();
	/**
	 * 索引名，为空时自动创建名称（但是可能会超出数据库长度，因此也可以手工指定）
	 * @return
	 */
	String name() default "";
	/**
	 * unique,desc等修饰
	 * @return
	 */
	String definition() default "";
}
