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

import java.util.ArrayList;
import java.util.List;

import jef.common.wrapper.IntRange;
import jef.database.BindVariableDescription;
import jef.database.ORMConfig;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheImpl;
import jef.database.cache.CacheKey;
import jef.database.cache.KeyDimension;
import jef.database.cache.SqlCacheKey;
import jef.database.dialect.DatabaseDialect;

/**
 * 必要Part五部分， 4+1
 * 
 * @author Administrator
 * 
 */
public class QueryClauseImpl implements IQueryClause {
	/*
	 * 这两部分总是只有一个有值 当单表查询时支持分表，所以是PartitionResult 当多表关联时，目前不支持分表，所以是string
	 */
	private String tableDefinition;
	private PartitionResult[] tables;
	
	public static final QueryClauseImpl EMPTY=new QueryClauseImpl(new PartitionResult[0]);
	
//	//是否为union
//	private boolean isUnion = false;
	//Select部分
	private SelectPart selectPart;
	//Where
	private String wherePart;
	//groupBy
	private GroupClause grouphavingPart;//=GroupClause.DEFAULT
	//排序
	private OrderClause orderbyPart = OrderClause.DEFAULT;
	//绑定变量
	private List<BindVariableDescription> bind;
	//范围
	private IntRange pageRange;

	private DatabaseDialect profile;
	
	public QueryClauseImpl(DatabaseDialect profile){
		this.profile=profile;
	}
	private QueryClauseImpl(PartitionResult[] partitionResults) {
		this.tables=partitionResults;
	}
	public IntRange getPageRange() {
		return pageRange;
	}

	public void setPageRange(IntRange pageRange) {
		this.pageRange = pageRange;
	}

	public OrderClause getOrderbyPart() {
		return orderbyPart;
	}

	public void setOrderbyPart(OrderClause orderbyPart) {
		this.orderbyPart = orderbyPart;
	}

	public GroupClause getGrouphavingPart() {
		return grouphavingPart;
	}

	public void setGrouphavingPart(GroupClause grouphavingPart) {
		this.grouphavingPart = grouphavingPart;
	}

	public SelectPart getSelectPart() {
		return selectPart;
	}

	public void setSelectPart(SelectPart selectPart) {
		this.selectPart = selectPart;
	}

	public String getWherePart() {
		return wherePart;
	}

	public void setWherePart(String wherePart) {
		this.wherePart = wherePart;
	}

	public String getTableDefinition() {
		return tableDefinition;
	}

	public void setTableDefinition(String tableDefinition) {
		this.tableDefinition = tableDefinition;
	}

	public void setBind(List<BindVariableDescription> bind) {
		this.bind = bind;
	}

	public PartitionResult[] getTables() {
		return tables;
	}

	public void setTables(PartitionResult[] tables,String rawClass) {
		this.tables = tables;
		this.rawClass=rawClass;
	}

	@Override
	public String toString() {
		return String.valueOf(getSql(null));
	}

	/*
	 * 
	 * @param tableDef
	 * @param delayProcessGroupClause 说明是在union字句中，需要确保是先把unionall 再groupby
	 * @return
	 */
	private String getSql(String tableDef,boolean delayProcessGroupClause) {
		StringBuilder sb = new StringBuilder(200);
		selectPart.append(sb,delayProcessGroupClause);
		sb.append(" from ");
		sb.append(tableDef);
		if(wherePart.length()>0){
			sb.append(ORMConfig.getInstance().wrap);
			sb.append(wherePart);	
		}
		if(!delayProcessGroupClause)sb.append(grouphavingPart);
		return sb.toString();
	}

	public BindSql getSql(PartitionResult site) {
		if (tableDefinition != null) {
			return new BindSql(withPage(getSql(tableDefinition,false).concat(orderbyPart.getSql()),false), bind);
		}
		if(site==null){
			if(tables.length==0){
				throw new IllegalArgumentException("The partition result does not return any result!");
			}
			site=this.tables[0];
		}
		StringBuilder sb = new StringBuilder(200);
		List<BindVariableDescription> bind = this.bind;
		boolean moreTable=site.tableSize()>1;
		for (int i = 0; i < site.tableSize(); i++) {
			if (i>0) {
				sb.append("\n union all \n");
			}
			String tableName = site.getTables().get(i);
			sb.append(getSql(tableName.concat(" t"),moreTable && grouphavingPart.isNotEmpty()));//为多表、并且有groupby时需要特殊处理
			
		}

		// 不带group by、having、order by从句的情况下，无需再union一层，
		// 否则，对查询列指定别名时会产生异常。
		if (moreTable &&
				(grouphavingPart.isNotEmpty() ||
						orderbyPart.isNotEmpty())) {
			StringBuilder sb2 = new StringBuilder();
			selectPart.append(sb2);

			sb2.append(" from (").append(sb).append(") t");
			sb2.append(ORMConfig.getInstance().wrap);
			sb2.append(grouphavingPart);
			sb = sb2;
		}

		if (moreTable) {
			// 当复杂情况下，绑定变量也要翻倍
			bind = new ArrayList<BindVariableDescription>();
			for (int i = 0; i < site.tableSize(); i++) {
				bind.addAll(this.bind);
			}
		}

		sb.append(orderbyPart.getSql());
		return new BindSql(withPage(sb.toString(),moreTable), bind);
	}

	private String withPage(String sql,boolean union) {
		if (pageRange != null) {
			return profile.toPageSQL(sql, pageRange,union);
		}
		return sql;
	}
	
	private CacheKey cacheKey;
	private String rawClass;
	public CacheKey getCacheKey() {
		if(cacheKey!=null)return cacheKey;
		String table=rawClass==null?tableDefinition:rawClass;
		CacheKey key=new SqlCacheKey(table,new KeyDimension(wherePart,orderbyPart.getSql()),CacheImpl.toParamList(this.bind));
		this.cacheKey=key;
		return key;
	}
	public boolean isGroupBy() {
		return this.grouphavingPart!=null && !grouphavingPart.isNotEmpty();
	}
	public boolean isEmpty() {
		return this.tables!=null && tables.length==0;
	}
	public boolean isMultiDatabase() {
		return this.tables!=null && tables.length>1;
	}
}
