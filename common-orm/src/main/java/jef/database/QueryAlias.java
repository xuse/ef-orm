package jef.database;

import java.util.List;

import jef.database.meta.IReferenceAllTable;
import jef.database.meta.IReferenceColumn;
import jef.database.meta.Reference;
import jef.database.query.ISelectItemProvider;
import jef.database.query.Query;
import jef.database.query.SelectItemProvider;


/**
 * 描述一个单表请求，和该表的别名
 * @Company: Asiainfo-Linkage Technologies(China),Inc.  Hangzhou
 * @author Administrator
 * @Date 2011-4-19
 */
public class QueryAlias extends SelectItemProvider {
	//用于描述绑定信息的字段
	Reference staticRef;
	@Override
	public String toString() {
		return table.toString()+" " +schema;
	}
	public QueryAlias(String alias,Query<?> q){
		super(alias,q);
	}
	
	public Query<?> getQuery() {
		return table;
	}
	public void setQuery(Query<?> query) {
		this.table = query;
	}
	public String getAlias() {
		return schema;
	}
	public QueryAlias setAlias(String alias) {
		this.schema = alias;
		return this;
	}
	public void setStaticRef(Reference staticRef) {
		this.staticRef = staticRef;
	}
	public Reference getStaticRef() {
		return staticRef;
	}
	
	public ISelectItemProvider copyOf(List<IReferenceColumn> fields,IReferenceAllTable allCols) {
		QueryAlias q=new QueryAlias(schema,table);
		q.referenceCol=fields;
		q.referenceObj=allCols;
		q.staticRef=this.staticRef;
		return q;
	}
}
