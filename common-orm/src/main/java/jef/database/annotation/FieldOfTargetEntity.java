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
 * JPA中只支持对实体的多表引用，EasyFrame中还支持对单个字段的跨表引用。
 * 为此增加了FieldOfTargetEntity这个Annonation，用于描述字段引用自目标实体的某个属性。
 * 
 * 字段引用都只用于查询操作，不能用于插入和更新操作
 * <p>为什么要有这个功能？</p>
 * 笔者在设计JEF时，考虑到性能问题，很多时候不需要查询目标整个实体，只需要个别字段即可。
 * 
 * 
 * 
 * @Date 2011-4-12
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface FieldOfTargetEntity {
	String value();
}
