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

import java.io.Serializable;

/**
 * 用于描述JEF元模型的借口
 * Field有以下实现种类
 * 1、Enum型，即各个实体的元模型字段
 * 2、FBIField 两个作用<BR>
 * 		(a)描述一个索引字段<BR>
 * 		(b)用来描述带函数的查询条件左表达式
 * 3、IConditionField 用来描述一个完整的查询条件。有多种实现
 * 4、RefField: 包含一个实例和一个Field模型字段，用于描述在一个实体的Field中引用另一个实体的Field
 * 
 * @author Administrator
 *
 */
public interface Field extends Serializable {
	public String name();

}
