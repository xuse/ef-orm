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
import java.util.BitSet;

/**
 * EasyFrame的原生实体类型，原生实体需要继承IQueryableEntity接口。 从而获得集 增、删、改、查、延迟加载等等复杂操作为一体的对象。
 * 
 * @author jiyi
 *
 */
public interface IQueryableEntity extends Queryable, Serializable, Cloneable {
	/**
	 * 有没有带Query对象
	 * 
	 * @return
	 */
	boolean hasQuery();

	/**
	 * 清除对象中的Query对象，包括延迟加载的钩子等等
	 */
	void clearQuery();

	/**
	 * 打开字段更新记录开关
	 */
	void startUpdate();

	/**
	 * 关闭字段更新记录开关
	 */
	void stopUpdate();

	/**
	 * 判断该字段是否被赋值过
	 * 
	 * @return 该字段是否赋值过
	 */
	boolean isUsed(Field field);

	/**
	 * 设置变更状态
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	void touchFlag(Field field, boolean value);


	//////////////////////////////////////////
	/**
	 * 如果是oracle得到rowid
	 * 
	 * @return
	 */
	String rowid();

	/**
	 * 指定rowid (Oracle)
	 * 
	 * @param rowid
	 */
	void bindRowid(String rowid);
}
