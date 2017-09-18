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

import java.sql.SQLException;
import java.util.Map;

import jef.database.query.Query;

public interface Queryable {
	@SuppressWarnings("rawtypes")
	public Query getQuery();
	

	/**
	 * 清除待更新数据
	 */
	public void clearUpdate();
	
	/**
	 * 获取目前的updateMap
	 */
	Map<Field, Object> getUpdateValueMap();
	
	/**
	 * 准备更新数据(与现有值相等的不会被更新)
	 * @param field
	 * @param newValue
	 * @throws SQLException
	 */
	public void prepareUpdate(Field field,Object newValue);
	
	/**
	 * 准备更新 数据(强制)
	 * 
	 * @Title: prepareUpdate
	 */
	void prepareUpdate(Field field, Object newValue, boolean force);
	
	/**
	 * 判断是否需要更新
	 * @return
	 */
	public boolean needUpdate();
}
