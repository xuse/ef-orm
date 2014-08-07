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
package jef.database.wrapper.result;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ResultSetMetaDataWrapper implements ResultSetMetaData {
	private ResultSetMetaData wrapped;

	public ResultSetMetaDataWrapper(ResultSetMetaData wrapped) {
		this.wrapped = wrapped;
	}

	public String getCatalogName(int column) throws SQLException {
		return this.wrapped.getCatalogName(column);
	}

	public String getColumnClassName(int column) throws SQLException {
		return this.wrapped.getColumnClassName(column);
	}

	public int getColumnCount() throws SQLException {
		return this.wrapped.getColumnCount();
	}

	public int getColumnDisplaySize(int column) throws SQLException {
		return this.wrapped.getColumnDisplaySize(column);
	}

	public String getColumnLabel(int column) throws SQLException {
		return this.wrapped.getColumnLabel(column);
	}

	public String getColumnName(int column) throws SQLException {
		return this.wrapped.getColumnName(column);
	}

	public int getColumnType(int column) throws SQLException {
		return this.wrapped.getColumnType(column);
	}

	public String getColumnTypeName(int column) throws SQLException {
		return this.wrapped.getColumnTypeName(column);
	}

	public int getPrecision(int column) throws SQLException {
		return this.wrapped.getPrecision(column);
	}

	public int getScale(int column) throws SQLException {
		int scale = this.wrapped.getScale(column);
		return scale < 0 ? 0 : scale;
	}

	public String getSchemaName(int column) throws SQLException {
		return this.wrapped.getSchemaName(column);
	}

	public String getTableName(int column) throws SQLException {
		return this.wrapped.getTableName(column);
	}

	public boolean isAutoIncrement(int column) throws SQLException {
		return this.wrapped.isAutoIncrement(column);
	}

	public boolean isCaseSensitive(int column) throws SQLException {
		return wrapped.isCaseSensitive(column);
	}

	public boolean isCurrency(int column) throws SQLException {
		return this.wrapped.isCurrency(column);
	}

	public boolean isDefinitelyWritable(int column) throws SQLException {
		return this.wrapped.isDefinitelyWritable(column);
	}

	public int isNullable(int column) throws SQLException {
		return this.wrapped.isNullable(column);
	}

	public boolean isReadOnly(int column) throws SQLException {
		return this.wrapped.isReadOnly(column);
	}

	public boolean isSearchable(int column) throws SQLException {
		return this.wrapped.isSearchable(column);
	}

	public boolean isSigned(int column) throws SQLException {
		return this.wrapped.isSigned(column);
	}

	public boolean isWritable(int column) throws SQLException {
		return wrapped.isWritable(column);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(wrapped);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

}
