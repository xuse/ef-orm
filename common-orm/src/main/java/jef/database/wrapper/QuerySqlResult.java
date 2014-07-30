package jef.database.wrapper;

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
import jef.tools.StringUtils;

/**
 * 必要Part五部分， 4+1
 * 
 * @author Administrator
 * 
 */
public class QuerySqlResult implements IQuerySqlResult {
	/*
	 * 这两部分总是只有一个有值 当单表查询时支持分表，所以是PartitionResult 当多表关联时，目前不支持分表，所以是string
	 */
	private String tableDefinition;
	private PartitionResult[] tables;
	
//	//是否为union
//	private boolean isUnion = false;
	//Select部分
	private SelectResult selectPart;
	//Where
	private String wherePart;
	//groupBy
	private String grouphavingPart;
	//排序
	private OrderResult orderbyPart = OrderResult.DEFAULT;
	//绑定变量
	private List<BindVariableDescription> bind;
	//范围
	private IntRange pageRange;

	private DatabaseDialect profile;
	
	public QuerySqlResult(DatabaseDialect profile){
		this.profile=profile;
	}
	public IntRange getPageRange() {
		return pageRange;
	}

	public void setPageRange(IntRange pageRange) {
		this.pageRange = pageRange;
	}

	public OrderResult getOrderbyPart() {
		return orderbyPart;
	}

	public void setOrderbyPart(OrderResult orderbyPart) {
		this.orderbyPart = orderbyPart;
	}

	public String getGrouphavingPart() {
		return grouphavingPart;
	}

	public void setGrouphavingPart(String grouphavingPart) {
		this.grouphavingPart = grouphavingPart;
	}

	public SelectResult getSelectPart() {
		return selectPart;
	}

	public void setSelectPart(SelectResult selectPart) {
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

//	public boolean isUnion() {
//		return isUnion;
//	}
//
//	public void setUnion(boolean isUnion) {
//		this.isUnion = isUnion;
//	}

	@Override
	public String toString() {
		return String.valueOf(getSql());
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
		sb.append(ORMConfig.getInstance().wrap);
		sb.append(wherePart);
		if(!delayProcessGroupClause)sb.append(grouphavingPart);
		return sb.toString();
	}

	public BindSql getSql() {
		if (tableDefinition != null) {
			return new BindSql(withPage(getSql(tableDefinition,false).concat(orderbyPart.getSql()),false), bind);
		}

		for (PartitionResult site : tables) {
			StringBuilder sb = new StringBuilder(200);
			List<BindVariableDescription> bind = this.bind;
			boolean moreTable=site.tableSize()>1;
			for (int i = 0; i < site.tableSize(); i++) {
				String tableName = site.getTables().get(i);
				sb.append(getSql(tableName.concat(" t"),moreTable && StringUtils.isNotEmpty(grouphavingPart)));//为多表、并且有groupby时需要特殊处理
				if (site.tableSize() > 1)
					sb.append("\n");
				if (site.tableSize() > 1 && i < site.tableSize() - 1) {
					sb.append(" union all \n");
				}
			}

			// 不带group by、having、order by从句的情况下，无需再union一层，
			// 否则，对查询列指定别名时会产生异常。
			if (moreTable &&
					(StringUtils.isNotEmpty(grouphavingPart) ||
							StringUtils.isNotEmpty(orderbyPart.getSql()))) {
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
		throw new IllegalArgumentException("The partition result does not return any result!");
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
}
