package jef.database.routing.jdbc;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.common.wrapper.Holder;
import jef.database.OperateTarget;
import jef.database.jsqlparser.expression.JdbcParameter;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.create.CreateTable;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.drop.Drop;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.replace.Replace;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SubJoin;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

public class SqlAnalyzer {
	private static ITableMetadata tryGetEntityFromStatement(Statement sql) {
		TableMetaCollector collector=new TableMetaCollector();
		sql.accept(collector);
		return collector.get();
	}
	static class TableMetaCollector extends Holder<ITableMetadata> implements StatementVisitor{
		private boolean breakProcess;
		public void visit(Select select) {
			SelectBody body=select.getSelectBody();
			process(body);
		}

		private void process(SelectBody body) {
			if(body instanceof PlainSelect){
				FromItem item=((PlainSelect)body).getFromItem();
				processFromItem(item);
			}else if(body instanceof Union){
				Union union=(Union)body;
				for(PlainSelect sel:union.getPlainSelects()){
					processFromItem(sel.getFromItem());
					if(breakProcess)
						break;
				}
			}
		}

		private void processFromItem(FromItem item) {
			if(item instanceof SubJoin){
				breakProcess=true;
			}else if(item instanceof JpqlParameter){
				breakProcess=true;
			}else if(item instanceof SubSelect){
				process(((SubSelect) item).getSelectBody());
			}else if(item instanceof Table){
				process((Table)item);
			}
		}

		private void process(Table table) {
			String schema=table.getSchemaName();
			String name=table.getName();
			ITableMetadata meta=MetaHolder.lookup(schema, name);
			if(this.get()==null){
				this.set(meta);
			}else if(this.get()!=meta){
				this.set(null);
				breakProcess=true;
			}
		}

		public void visit(Delete delete) {
			processFromItem(delete.getTable());
		}

		public void visit(Update update) {
			processFromItem(update.getTable());
		}

		public void visit(Insert insert) {
			processFromItem(insert.getTable());
		}
		public void visit(Replace replace) {
			//不支持
		}

		public void visit(Drop drop) {
			//不支持
		}

		public void visit(Truncate truncate) {
			//不支持
		}

		public void visit(CreateTable createTable) {
			//不支持
		}
	}
	public static ExecutionPlan getExecutionPlan(Statement sql, List<Object> value,OperateTarget db) {
		ITableMetadata meta=tryGetEntityFromStatement(sql);
		if(meta==null || meta.getPartition()==null){
			return null;
		}
		Map<Expression,Object> params=reverse(sql,value); //参数对应关系还原
		
		if(sql instanceof Insert){
			return getExePlan(meta,(Insert)sql,params);
		}else if(sql instanceof Update){
			return getExePlan(meta,(Update)sql,params);
		}else if(sql instanceof Delete){
			return getExePlan(meta,(Delete)sql,params);
		}else{
			Select select=(Select)sql;
			SelectBody body=select.getSelectBody();
			if(body instanceof PlainSelect){
				return getExePlan(meta,(PlainSelect)body,params);
			}else{
				throw new UnsupportedOperationException();		
			}
		}
	}
	
	//将顺序的参数重新变为和JpqlParameter对应的map
	static class ParamReverser extends VisitorAdapter{
		ParamReverser(List<Object> raw){
			this.rawParams=raw.iterator();
		}
		
		final Map<Expression,Object> params=new IdentityHashMap<Expression, Object>();
		private Iterator<Object> rawParams;
		
		@Override
		public void visit(JpqlParameter parameter) {
			int res=parameter.resolvedCount();
			if(res==0){
				params.put(parameter,rawParams.next());
			}else if(res>0){
				Object[] array=new Object[res];
				for(int i=0;i<res;i++){
					array[i]=rawParams.next();
				}
				params.put(parameter,array);
			}
		}

		@Override
		public void visit(JdbcParameter jdbcParameter) {
			params.put(jdbcParameter, rawParams.next());
		}

		public Map<Expression, Object> getParams() {
			return params;
		}
	}
	private static Map<Expression, Object> reverse(Statement sql, List<Object> value) {
		ParamReverser p=new ParamReverser(value);
		sql.accept(p);
		return p.params;
	}

	private static ExecutionPlan getExePlan(ITableMetadata meta, Delete sql, Map<Expression,Object> value) {
		throw new UnsupportedOperationException();
	}

	private static ExecutionPlan getExePlan(ITableMetadata meta, Update sql, Map<Expression,Object> value) {
		throw new UnsupportedOperationException();
	}

	private static ExecutionPlan getExePlan(ITableMetadata meta, Insert sql, Map<Expression,Object> value) {
		throw new UnsupportedOperationException();
	}

	//获得分库分表的执行计划
	private static ExecutionPlan getExePlan(ITableMetadata meta, PlainSelect sql, Map<Expression,Object> value) {
		
		
		sql.getFromItem();
		
		
		
		return null;
	}
	
	
	
}
