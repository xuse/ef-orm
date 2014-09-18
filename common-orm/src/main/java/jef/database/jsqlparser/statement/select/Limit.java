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
package jef.database.jsqlparser.statement.select;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItemVisitor;

/**
 * A limit clause in the form [LIMIT {[offset,] row_count) | (row_count | ALL)
 * OFFSET offset}]
 */
public class Limit implements SqlAppendable {

	private long offset;

	private long rowCount;

	private Expression rowCountJdbcParameter;

	private Expression offsetJdbcParameter;

	private boolean limitAll;

	public Limit() {
	}
	
	/**
	 * Clone constructor
	 * @param limit
	 */
	public Limit(Limit limit) {
		this.offset=limit.offset;
		this.limitAll=limit.limitAll;
		this.offsetJdbcParameter=limit.offsetJdbcParameter;
		this.rowCount=limit.rowCount;
		this.rowCountJdbcParameter=limit.rowCountJdbcParameter;
	}

	public boolean isRowCountJdbcParameter() {
		return rowCountJdbcParameter != null;
	}

	public Expression getRowCountJdbcParameter() {
		return rowCountJdbcParameter;
	}

	public Expression getOffsetJdbcParameter() {
		return offsetJdbcParameter;
	}

	public long getOffset() {
		return offset;
	}

	public long getRowCount() {
		return rowCount;
	}

	public void setOffset(long l) {
		offset = l;
	}

	public void setRowCount(long l) {
		rowCount = l;
	}

	public void setOffsetJdbcParameter(Expression b) {
		offsetJdbcParameter = b;
	}

	public void setRowCountJdbcParameter(Expression b) {
		rowCountJdbcParameter = b;
	}

	/**
	 * @return true if the limit is "LIMIT ALL [OFFSET ...])
	 */
	public boolean isLimitAll() {
		return limitAll;
	}

	public void setLimitAll(boolean b) {
		limitAll = b;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}

	public void appendTo(StringBuilder sb) {
		if (rowCount > 0 || rowCountJdbcParameter != null) {
			sb.append(" LIMIT ");
			if (rowCountJdbcParameter == null) {
				sb.append(rowCount);
			} else {
				rowCountJdbcParameter.appendTo(sb);
			}
		}
		if (offset > 0 || offsetJdbcParameter != null) {
			sb.append(" OFFSET ");
			if (offsetJdbcParameter == null) {
				sb.append(offset);
			} else {
				offsetJdbcParameter.appendTo(sb);
			}
		}
	}

	public void accept(SelectItemVisitor visitorAdapter) {
		visitorAdapter.visit(this);
	}
	
	public void clear(){
		rowCount=0;
		rowCountJdbcParameter=null;
		offset=0;
		offsetJdbcParameter=null;
	}

	public boolean isValid() {
		return rowCount!=0 || rowCountJdbcParameter!=null;
	}

}
