package jef.database.routing.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import jef.common.wrapper.IntRange;
import jef.database.OperateTarget.SqlTransformer;
import jef.database.Session;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.clause.InMemoryDistinct;
import jef.database.wrapper.clause.InMemoryGroupBy;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.clause.InMemoryPaging;
import jef.database.wrapper.clause.InMemoryProcessor;

public class ExecutionPlan implements SqlProvider{
	private PartitionResult[] sites;
	// 重新排序部分
	private InMemoryOrderBy inMemoryOrder;
	// 重新分组处理逻辑
	private List<InMemoryProcessor> mustInMemoryProcessor;
	
	private SqlProvider parsedSql;
	
	
	public void setInMemoryOrder(InMemoryOrderBy inMemoryOrder) {
		this.inMemoryOrder = inMemoryOrder;
	}

	public void setInMemoryGroups(InMemoryGroupBy inMemoryGroups) {
		addToInMemprocessor(inMemoryGroups);
	}
	
	public void setInMemoryPage(InMemoryPaging inMemoryPaging) {
		addToInMemprocessor(inMemoryPaging);
	}

	public void setInMemoryDistinct(InMemoryDistinct instance) {
		addToInMemprocessor(instance);
	}
	
	private void addToInMemprocessor(InMemoryProcessor process) {
		if(this.mustInMemoryProcessor==null){
			mustInMemoryProcessor=new ArrayList<InMemoryProcessor>(4);
		}
		mustInMemoryProcessor.add(process);
	}
	//使用plan得到 SQL语句和参数
	//当多表时,参数会复制多份
	public Entry<String, List<Object>> getSql(PartitionResult site) {
		return parsedSql.getSql(site);
	}

	
	public boolean isMultiDatabase(){
		return sites.length>1;
	}
	
	public boolean isSingleTable(){
		return sites.length==1 && sites[0].getTables().size()==1;
	}

	public PartitionResult[] getSites() {
		return sites;
	}

	public void setSites(PartitionResult[] sites) {
		this.sites = sites;
	}
	
	public static ExecutionPlan get(ITableMetadata meta, Statement sql, List<Object> params) {//如果路由结果唯一，则返回null即可
		// TODO Auto-generated method stub
		return null;
	}

	public <X> ResultIterator<X> getIteratorResult(IntRange range, SqlTransformer<X> resultTransformer, int i, int fetchSize) {
		return null;
	}
	
	
	public <X> List<X> getListResult(IntRange range,SqlTransformer<X> rst,int max,int fetchSize) {
		return null;
	}

	public long processCount(PartitionResult site,Session session) {
		return 0;
	}

	public int processUpdate(PartitionResult site,Session session) {
		return 0;
	}

	
}
