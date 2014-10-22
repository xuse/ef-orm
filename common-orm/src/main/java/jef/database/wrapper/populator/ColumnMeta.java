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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;

import jef.database.query.SqlContext;
import jef.tools.StringUtils;

public class ColumnMeta {
	ColumnDescription[] columns;
	/**
	 * 基于原始的NAME到备注的索引(KEY全大写)
	 */
	Map<String, ColumnDescription> nameIndex;
	/**
	 * 基于多个解析后的schema
	 */
	private Map<String, ColumnDescription[]> schemaIndex;

	private ResultSetMetaData meta;

	/**
	 * 构造
	 * <p>
	 * Title:
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param columnNames
	 */
	public ColumnMeta(ResultSetMetaData meta) {
		this.meta = meta;
		init(meta);

	}

	private void init(ResultSetMetaData meta) {
		try {
			ColumnDescription[] columns = new ColumnDescription[meta.getColumnCount()];
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				// 对于Oracle
				// getCOlumnName和getColumnLabel是一样的（非标准JDBC实现），MySQL正确地实现了JDBC的要求，getLabel得到别名，getColumnName得到表的列名
				columns[i-1]=new ColumnDescription(i, meta.getColumnType(i), meta.getColumnLabel(i), meta.getTableName(i), meta.getSchemaName(i));
			}
			this.columns = columns;
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		nameIndex = new HashMap<String, ColumnDescription>(16, 0.6f);
		for (ColumnDescription c : columns) {
			nameIndex.put(c.getName().toUpperCase(), c);
		}
	}

	/**
	 * 按序号返回ColumnDescription
	 * 
	 * @param n
	 * @return
	 */
	public ColumnDescription getN(int n) {
		return columns[n];
	}

	// 初始化Schema
	public void initSchemas(Transformer transformers) {
		if (schemaIndex != null)
			return;
		transformers.prepareTransform(nameIndex);// 注意这个方法必须在ignoreSchema操作之前进行计算，否则会造成自定义Mapper找不到需要的列。

		Map<String, List<ColumnDescription>> main = new HashMap<String, List<ColumnDescription>>();
		for (ColumnDescription c : columns) {
			String s = c.getName();
			if (transformers.hasIgnoreColumn(s.toUpperCase())) {
				continue;
			}
			int n = s.indexOf(SqlContext.DIVEDER);
			String schema = (n > -1) ? s.substring(0, n) : "";
			if (transformers.hasIgnoreSchema(schema.toUpperCase())) {
				continue;
			}

			c.setSimpleName((n > -1) ? s.substring(n + SqlContext.DIVEDER.length()) : s);
			List<ColumnDescription> list = main.get(schema);
			if (list == null) {
				list = new ArrayList<ColumnDescription>();
				list.add(c);
				main.put(schema, list);
			} else {
				list.add(c);
			}
		}
		schemaIndex = new HashMap<String, ColumnDescription[]>();
		for (String key : main.keySet()) {
			List<ColumnDescription> list = main.get(key);
			schemaIndex.put(key, list.toArray(new ColumnDescription[list.size()]));
		}
	}

	public ColumnDescription[] getColumns(String schema) {
		return schemaIndex.get(schema);
	}

	/**
	 * 优化实现1，如果确定传入字符串为大写时，可调用此方法。
	 * 目前要求传入column时都必须大写。
	 * @param column 大写的列名
	 * @return
	 */
	public ColumnDescription getByUpperName(String column) {
		return this.nameIndex.get(column);
	}

	public Set<String> getSchemas() {
		return schemaIndex.keySet();
	}

	@Override
	public String toString() {
		if (this.schemaIndex == null) {
			StringBuilder sb = new StringBuilder();
			for (ColumnDescription c : this.columns) {
				sb.append(c.getName()).append(',');
			}
			sb.setLength(sb.length() - 1);
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			for (String key : schemaIndex.keySet()) {
				sb.append(key).append(":{").append(StringUtils.join(schemaIndex.get(key), ",")).append('}');
				sb.append('\n');
			}
			return sb.toString();
		}
	}

	public ColumnDescription[] getColumns() {
		return columns;
	}

	public int length() {
		return columns.length;
	}

	public ResultSetMetaData getMeta() {
		return meta;
	}
}
