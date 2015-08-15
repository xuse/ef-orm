package jef.database.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jef.database.DbUtils;
import jef.database.DefaultSqlProcessor;
import jef.database.Field;
import jef.database.SelectProcessor;
import jef.database.meta.FBIField;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.CountClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.QueryClauseImpl;
import jef.database.wrapper.clause.QueryClauseSqlImpl;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.tools.StringUtils;

/**
 * 描述union的查询请求
 * @author Administrator
 *
 * @param <T>
 */
public class UnionQuery<T> implements ComplexQuery,TypedQuery<T> {
	private List<ConditionQuery> querys;
	private List<OrderField> orderBy = new ArrayList<OrderField>();
	private boolean isAll = true;
	private Transformer t;
	private boolean cacheable = true;
	
	UnionQuery(List<ConditionQuery> ts,ITableMetadata meta){
		this.querys=ts;
		this.t=new Transformer(meta);
	}
	
	UnionQuery(List<ConditionQuery> ts,Class<T> clz){
		this.querys=ts;
		this.t=new Transformer(clz);
	}
	
	public void clearQuery() {
		for(ConditionQuery q:querys){
			q.clearQuery();
		}
	}

	public List<OrderField> getOrderBy() {
		return orderBy;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) t.getResultClazz();
	}

	/**
	 * 是否为 union all语句
	 * @return 如果是union语句返回false, union all 语句返回true
	 */
	public boolean isAll() {
		return isAll;
	}

	/**
	 * 设置为 union all 语句
	 * @param isAll
	 */
	public void setAll(boolean isAll) {
		this.isAll = isAll;
	}

	public void setOrderBy(List<OrderField> orderBy) {
		this.orderBy = orderBy;
	}

	public void addOrderBy(boolean asc, Field... orderbys) {
		for(Field f:orderbys){
			if(f instanceof RefField){
				orderBy.add(new OrderField(((RefField) f).getField(),asc));
			}else if(f instanceof FBIField){
				orderBy.add(new OrderField(f,asc));
			}else{
//				Assert.isTrue(DbUtils.getDoClass(f)==this.getType());
				orderBy.add(new OrderField(f,asc));	
			}
		}
	}
	
	public UnionQuery<T> orderByAsc(String... ascFields) {
		for(String f:ascFields){
			addOrderBy(true, new SqlExpression(f));
		}
		return this;
	}
	public UnionQuery<T> orderByAsc(Field... ascFields) {
		addOrderBy(true, ascFields);
		return this;
	}

	public UnionQuery<T> orderByDesc(Field... descFields) {
		addOrderBy(false, descFields);
		return this;
	}
	
	public UnionQuery<T> orderByDesc(String... descFields) {
		for(String f:descFields){
			addOrderBy(false, new SqlExpression(f));
		}
		return this;
	}

	public void setOrderBy(boolean asc, Field... orderbys) {
		orderBy.clear();
		addOrderBy(asc,orderbys);
	}

	public SqlContext prepare() {
		return null;
	}

	public int size(){
		return querys.size();
	}
	public CountClause toCountSql(SelectProcessor processor) throws SQLException {
		CountClause count=new CountClause();
		if(isAll){//union all是可以优化的，union是没有办法的
			for(int i=0;i<size();i++){
				ConditionQuery cq=querys.get(i);
				if(cq instanceof Query){
					Query<?> qq=(Query<?>) cq;
					if(qq.getOtherQueryProvider().length>0){
						cq=DbUtils.toReferenceJoinQuery(qq, null);
					}
				}
				CountClause result1=processor.toCountSql(cq);
				for(Map.Entry<String,List<BindSql>> dbAndSql:result1.getSqls().entrySet()){
					count.addSql(dbAndSql.getKey(),dbAndSql.getValue());	
				}
			}
		}else{//union，无法优化
			String sql=toQuerySql(processor);
			count.addSql(null,DefaultSqlProcessor.wrapCount(sql));
		}
		return count;
	}
	
	public CountClause toPrepareCountSql(SelectProcessor processor,SqlContext context) throws SQLException {
		CountClause count=new CountClause();
		if(isAll){//union all是可以优化的，union是没有办法的
			for(int i=0;i<size();i++){//拆成很多个count单句，每个单句进行count
				ConditionQuery cq=querys.get(i);
				if(cq instanceof Query){
					Query<?> qq=(Query<?>) cq;
					if(qq.getOtherQueryProvider().length>0){
						cq=DbUtils.toReferenceJoinQuery(qq, null);
					}
				}
				CountClause cr=processor.toCountSql(cq);
				for(Map.Entry<String,List<BindSql>> dbAndSql:cr.getSqls().entrySet()){
					count.addSql(dbAndSql.getKey(),dbAndSql.getValue());
				}
			}
		}else{//union，无法优化只能直接查询,最简单粗暴
			BindSql sql=toPrepareQuerySql0(processor,context,true);
			if(sql==null){
				return count;
			}
			sql.setSql(DefaultSqlProcessor.wrapCount(sql.getSql()));
			count.addSql(null,sql);
		}
		return count;
	}

	private BindSql toPrepareQuerySql0(SelectProcessor processor, SqlContext context,boolean isCount) {
		List<BindVariableDescription> binds=new ArrayList<BindVariableDescription>();
		List<String> sqls=new ArrayList<String>(size());
		boolean withBuck=processor.getProfile().has(Feature.UNION_WITH_BUCK);
				
		for(int i=0;i<size();i++){
			ConditionQuery cq=querys.get(i);
			if(cq instanceof Query){
				Query<?> qq=(Query<?>) cq;
				if(qq.getOtherQueryProvider().length>0){
					cq=DbUtils.toReferenceJoinQuery(qq, null);
				}
			}
			QueryClause sql=processor.toQuerySql(cq, null,false);
			if(sql.isEmpty()){
				continue;
			}
			BindSql qresult=sql.getSql(null);
			if(withBuck && !isCount){
				sqls.add("("+qresult.getSql()+")");
			}else{
				sqls.add(qresult.getSql());
			}
			binds.addAll(qresult.getBind());
		}
		if(sqls.isEmpty()){
			return null;
		}
		String union=isAll?"\n union all\n":"\n union\n";
		String sql=StringUtils.join(sqls,union);//QuerySqlResult.toString()已经能自动转换为SQL语句
		return new BindSql(sql,binds);
	}
	

	public String toQuerySql(SelectProcessor processor) {
		String[] sqls=new String[size()];
		boolean withBuck=processor.getProfile().has(Feature.UNION_WITH_BUCK);
		for(int i=0;i<size();i++){
			ConditionQuery cq=querys.get(i);
			if(cq instanceof Query){
				Query<?> qq=(Query<?>) cq;
				if(qq.getOtherQueryProvider().length>0){
					cq=DbUtils.toReferenceJoinQuery(qq, null);
				}
			}
			QueryClause sql=processor.toQuerySql(cq, null,false);
			if(withBuck){
				sqls[i]="("+sql.toString()+")";	
			}else{
				sqls[i]=sql.toString();
			}
			
		}
		String union=isAll?"\n union all\n":"\n union\n";
		return StringUtils.join(sqls,union);
	}
	
	private int maxResult;
	private int fetchSize;
	private int queryTimeout;

	public void setMaxResult(int size) {
		this.maxResult=size;
	}

	public void setFetchSize(int size) {
		this.fetchSize=size;
	}

	public void setQueryTimeout(int timout) {
		this.queryTimeout=timout;
	}

	public int getMaxResult() {
		return maxResult;
	}

	public int getFetchSize() {
		return fetchSize;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	public ITableMetadata getMeta() {
		return t.getResultMeta();
	}
	
	public Transformer getResultTransformer(){
		return t;
	}

	@Override
	public QueryClause toQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		@SuppressWarnings("deprecation")
		QueryClauseSqlImpl clause = new QueryClauseSqlImpl(processor.getProfile(), true);
		clause.setBody(toQuerySql(processor));
		if(order){
			clause.setOrderbyPart(processor.toOrderClause(this, context,processor.getProfile()));
		}
		return clause;
	}

	@Override
	public QueryClause toPrepareQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		BindSql sql = toPrepareQuerySql0(processor, context,false);
		if(sql==null)return QueryClauseImpl.EMPTY;
		@SuppressWarnings("deprecation")
		QueryClauseSqlImpl result = new QueryClauseSqlImpl(processor.getProfile(), true);
		result.setBody(sql.getSql());
		result.setBind(sql.getBind());
		if (order) {
			result.setOrderbyPart(processor.toOrderClause(this, context,processor.getProfile()));
		}
		return result;
	}

	@Override
	public boolean isSelectCustomized() {
		return false;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}
}
