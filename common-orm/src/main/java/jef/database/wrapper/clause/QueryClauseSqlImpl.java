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
package jef.database.wrapper.clause;

import java.util.List;

import jef.common.wrapper.IntRange;
import jef.database.BindVariableDescription;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKey;
import jef.database.dialect.DatabaseDialect;

public class QueryClauseSqlImpl implements IQueryClause {
	private String body;
	private OrderClause orderbyPart;
	private List<BindVariableDescription> bind;
	private IntRange pageRange;
	private boolean isUnion;
	
	public IntRange getPageRange() {
		return pageRange;
	}
	public void setPageRange(IntRange pageRange) {
		this.pageRange = pageRange;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public OrderClause getOrderbyPart() {
		return orderbyPart;
	}
	public void setOrderbyPart(OrderClause orderbyPart) {
		this.orderbyPart = orderbyPart;
	}
	public List<BindVariableDescription> getBind() {
		return bind;
	}
	public void setBind(List<BindVariableDescription> bind) {
		this.bind = bind;
	}
	@Override
	public String toString() {
		return getSql(null).getSql();
	}
	public BindSql getSql(PartitionResult site) {
		return new BindSql(withPage(body.concat(orderbyPart.getSql())),bind);
	}
	private DatabaseDialect profile;
	public QueryClauseSqlImpl(DatabaseDialect profile,boolean isUnion){
		this.profile=profile;
		this.isUnion=isUnion;
	}

	private String withPage(String sql) {
		if (pageRange != null) {
			return profile.toPageSQL(sql, pageRange,isUnion);
		}
		return sql;
	}
	static final PartitionResult[] P=new PartitionResult[]{new PartitionResult("")};
	public PartitionResult[] getTables() {
		return null;
	}
	public SelectPart getSelectPart() {
		return null;
	}
	public CacheKey getCacheKey() {
		return null;
	}
	public boolean isGroupBy() {
		return false;
	}
	public boolean isEmpty() {
		return false;
	}
	public boolean isMultiDatabase() {
		return false;
	}
	public GroupClause getGrouphavingPart() {
		return GroupClause.EMPTY;
	}
}
