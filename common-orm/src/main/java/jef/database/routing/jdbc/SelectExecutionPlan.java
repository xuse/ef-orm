package jef.database.routing.jdbc;

import java.util.ArrayList;
import java.util.List;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.BindVariableDescription;
import jef.database.ORMConfig;
import jef.database.OperateTarget.SqlTransformer;
import jef.database.Session;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.clause.InMemoryProcessor;

public class SelectExecutionPlan implements ExecutionPlan {
	//路由结果
	private PartitionResult[] sites;
	//绑定变量参数
	private List<Object> params;
	//原始SQL语句（解析后的）
	private PlainSelect st;
	
	
	//内存重新排序
	private InMemoryOrderBy inMemoryOrder;
	//内存处理运算
	private List<InMemoryProcessor> mustInMemoryProcessor;
	
	//解析结果，表名替换句柄
	private List<Table> modfications;
	//解析结果，分组函数替换句柄
	//（当出现一个库有多表时，如果发现是group语句，那么——
	//方法1 改写成先union再重新groupBy的逻辑）
	//方法2 改写成完全去除group的方式，然后内存group()
	//方法3 
	
	//

	public SelectExecutionPlan(PartitionResult[] results, List<Table> mods, PlainSelect st) {
		LogUtil.show(results);
		this.sites = results;
		this.modfications = mods;
		this.st = st;
		init();
	}

	//目前支持五种内存处理算法。
	//初始化,根据结果解析,是否需要内存分页\内存排序\内存分组\内存去重\内存递归过滤
	//此外，多表下的group操作需要支持select函数改写和SQL语句重新生成Deparser定制。
	private void init() {
		// TODO Auto-generated method stub
		
	}


	private void addToInMemprocessor(InMemoryProcessor process) {
		if (this.mustInMemoryProcessor == null) {
			mustInMemoryProcessor = new ArrayList<InMemoryProcessor>(4);
		}
		mustInMemoryProcessor.add(process);
	}
	
	

	// 使用plan得到 SQL语句和参数
	// 当多表时,参数会复制多份
	public PairSO<List<Object>> getSql(PartitionResult site) {
		List<String> tables=site.getTables();
		if(tables.size()==1){
			return new PairSO<List<Object>>(getSql(tables.get(0)),params);
		}else if(tables.size()>1){
			StringBuilder sb = new StringBuilder(200);
			for (int i = 0; i < tables.size(); i++) {
				if (i > 0) {
					sb.append("\n union all \n");
				}
				String tableName = site.getTables().get(i);
				st.getOrderBy();
				st.getGroupByColumnReferences();
				st.getHaving();
				sb.append(getSql(tableName.concat(" t"), grouphavingPart.isNotEmpty()));// 为多表、并且有groupby时需要特殊处理
			}
			
		}
		
		
		
		
		List<BindVariableDescription> bind = this.bind;
		boolean moreTable = site.tableSize() > 1;
		for (int i = 0; i < site.tableSize(); i++) {
			if (i > 0) {
				sb.append("\n union all \n");
			}
			String tableName = site.getTables().get(i);
			sb.append(getSql(tableName.concat(" t"), moreTable && grouphavingPart.isNotEmpty()));// 为多表、并且有groupby时需要特殊处理

		}

		// 不带group by、having、order by从句的情况下，无需再union一层，
		// 否则，对查询列指定别名时会产生异常。
		if (moreTable && (grouphavingPart.isNotEmpty() || orderbyPart.isNotEmpty())) {
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
		return new BindSql(withPage(sb.toString(), moreTable), bind);
	}

	private String getSql(String name,boolean delayProcessGroupClause) {
		for (Table tb : modfications) {
			tb.setReplace(name);
		}
		if(delayProcessGroupClause){//在select部分中清除group函数。同时去除结尾的groupBy部分
			for(SelectItem select:pls.getSelectItems()){
//				select.accept(new )
				select function
			}
		}
		String sql = st.toString();
		//清理现场
		for (Table tb : modfications) {
			tb.removeReplace();
		}
		return sql;
	}

	/**
	 * 是否为多库查询。
	 * @return
	 */
	public boolean isMultiDatabase() {
		return sites.length > 1;
	}

	/**
	 * 是否为简单查询——路由结果指向单数据库的单表。这意味着所有的内存处理和SQL改写都不用做了。
	 * （除了表名改写以外）
	 * @return
	 */
	public boolean isSingleTable() {
		return sites.length == 1 && sites[0].getTables().size() == 1;
	}

	public PartitionResult[] getSites() {
		return sites;
	}

	public static ExecutionPlan get(ITableMetadata meta, Statement sql, List<Object> params) {// 如果路由结果唯一，则返回null即可
		// TODO Auto-generated method stub
		return null;
	}

	public <X> ResultIterator<X> getIteratorResult(IntRange range, SqlTransformer<X> resultTransformer, int i, int fetchSize) {
		return null;
	}

	public <X> List<X> getListResult(IntRange range, SqlTransformer<X> rst, int max, int fetchSize) {
		return null;
	}

	public long processCount(PartitionResult site, Session session) {
		return 0;
	}

	public int processUpdate(PartitionResult site, Session session) {
		return 0;
	}

}
