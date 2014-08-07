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
package jef.database.wrapper.populator;

import java.sql.SQLException;
import java.util.Map;

import jef.database.wrapper.result.IResultSet;
import jef.tools.reflect.BeanWrapper;

/**
 * 让用户可以自己覆盖实现的Transformer
 * @author jiyi
 *
 * @param <T>
 */
public abstract class Mapper<T> implements IPopulator{
	@SuppressWarnings("unchecked")
	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
		transform((T)wrapper.getWrapped(),rs);
	}
	
	/**
	 * 可以被覆盖，准备填充策略
	 * @param nameIndex
	 */
	protected void prepare(Map<String, ColumnDescription> nameIndex){
	}
	
	/**
	 * 需要被覆盖，用户自定义从ResultSet中获取数据填充到结果对象中
	 * @param obj
	 * @param rs
	 */
	protected abstract void transform(T obj, IResultSet rs)throws SQLException ;
}
