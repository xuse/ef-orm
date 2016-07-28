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

import jef.database.cache.CacheKey;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.ResultSetContainer;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.tools.PageLimit;

public class QueryClauseSqlImpl implements QueryClause {
	private String body;
	private OrderClause orderbyPart;
	private List<BindVariableDescription> bind;
	private PageLimit pageRange;
	private boolean isUnion;

	public PageLimit getPageRange() {
		return pageRange;
	}

	public void setPageRange(PageLimit pageRange) {
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
		BindSql r = withPage(body.concat(orderbyPart.getSql()));
		r.setBind(bind);
		return r;
	}

	private DatabaseDialect profile;

	public QueryClauseSqlImpl(DatabaseDialect profile, boolean isUnion) {
		this.profile = profile;
		this.isUnion = isUnion;
	}

	private BindSql withPage(String sql) {
		if (pageRange != null) {
			return profile.getLimitHandler().toPageSQL(sql, pageRange.toArray(), isUnion);
		}
		return new BindSql(sql);
	}

	public PartitionResult[] getTables() {
		return P;
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
		return GroupClause.DEFAULT;
	}

	public boolean isDistinct() {
		return false;
	}

	@Override
	public boolean hasInMemoryOperate() {
		return false;
	}

	@Override
	public void parepareInMemoryProcess(PageLimit range, ResultSetContainer rs) {
	}

	@Override
	public ResultSetLaterProcess getRsLaterProcessor() {
		return null;
	}
}
