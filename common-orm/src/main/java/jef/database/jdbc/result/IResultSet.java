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
package jef.database.jdbc.result;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.populator.ColumnMeta;

/**
 * 这个类是JDBC ResultSet的封装，大部分方法都和JDBC ResultSet一致
 * 
 * 下一步重构，将其继承ResultSet，进一步简化代码
 * 
 * @author jiyi
 * 
 */
public interface IResultSet extends ResultSet {
	Map<Reference, List<Condition>> getFilters();

	DatabaseDialect getProfile();

	ColumnMeta getColumns();
	
	boolean next();
}
