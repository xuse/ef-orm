package jef.database.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.QueryAlias;
import jef.database.Session.PopulateStrategy;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.populator.Transformer;

public abstract class AbstractJoinImpl implements Join{
	//额外的数据属性
	protected final Map<String,Object> attribute = new HashMap<>(4);
	private int maxResult;
	private int fetchSize;
	private int timeout;
	private boolean cacheable=true;
	private Transformer t;
	
	//调用prepare后，查询进入实例阶段
	protected SqlContext context; //只有rootJoin才有Context
	
	private Map<Query<?>,List<Condition>> refConditions;

	@Override
	public boolean isSelectCustomized() {
		PopulateStrategy[] s=t.getStrategy();
		if(s!=null && s.length>0)return true;
		return context.getSelectsImpl()!=null;
	}
	
	public EntityMappingProvider getSelectItems() {
		return context.getSelectsImpl()==null?context:context.getSelectsImpl();
	}

	public void fillAttribute(Query<?> q){
		if(q.getAttributes()!=null) {
			this.attribute.putAll(q.getAttributes());
		}
		this.t=q.getResultTransformer();
	}
	
	public void setMaxResult(int size) {
		this.maxResult=size;
		
	}

	public void setFetchSize(int size) {
		this.fetchSize=size;
	}

	public void setQueryTimeout(int timout) {
		this.timeout=timout;
	}

	public int getMaxResult() {
		return maxResult;
	}

	public int getFetchSize() {
		return fetchSize;
	}

	public int getQueryTimeout() {
		return timeout;
	}

	public Map<String, Object> getAttributes() {
		return attribute;
	}
	/**
	 *得到所有条件
	 */
	public List<Condition> getConditions() {
		List<Condition> cond=new ArrayList<Condition>();
		for(JoinElement je:elements()){
			cond.addAll(je.getConditions());
		}
		return cond;
	}

	@SuppressWarnings("unchecked")
	public Map<Query<?>, List<Condition>> getRefConditions() {
		return refConditions==null?Collections.EMPTY_MAP:refConditions;
	}

	public void addRefConditions(Query<?> q,List<Condition> cons) {
		if(refConditions==null){
			refConditions=new HashMap<Query<?>,List<Condition>>();
		}
		refConditions.put(q, cons);
	}

	/**
	 * 返回全部排序字段
	 */
	public List<OrderField> getOrderBy(){
		List<OrderField> result=new ArrayList<OrderField>();
		for(QueryAlias q:this.allElements()){
			for(OrderField field: q.getQuery().getOrderBy()){
				field.prepareFlow((Query<?>)q.getQuery());
				result.add(field);	
			}
		}
		return result;
	}
	public void setAttribute(String key, Object value) {
		attribute.put(key, value);
	}

	public Object getAttribute(String key) {
		if(attribute==null){
			return null;
		}
		return attribute.get(key);
	}
	
	public void setSelectItems(Selects select) {
		this.context.selectsImpl=(SelectsImpl)select;
	}

	public void addOrderBy(boolean asc, Field... orderbys) {
		List<Query<?>> elements=elements();
		Query<?> ele=elements().get(elements.size()-1);
		for (Field f : orderbys) {
			if ((f instanceof RefField)) {
				ele.addOrderBy(asc, f);		
			}else{
				ITableMetadata meta=DbUtils.getTableMeta(f);
				Query<?> q=findBind(meta,elements);
				if(q==ele){
					ele.addOrderBy(asc, f);	
				}else{
					ele.addOrderBy(asc, new RefField(q,f));
				}
			}
		}
	}

	private Query<?> findBind(ITableMetadata meta,List<Query<?>> elements) {
		Query<?> result=null;
		for(Query<?> ele: elements){
			if(meta==ele.getMeta()){
				if(result==null){
					result=ele;
				}else{
					throw new IllegalStateException("Multiple Query of ["+meta.getSimpleName()+"] in the join, please user RefField to Bind to a special Query.");
				}
			}
		}
		return result;
	}

	public JoinElement orderByAsc(Field... ascFields) {
		addOrderBy(true, ascFields);
		return this;
	}

	public JoinElement orderByDesc(Field... descFields) {
		addOrderBy(false, descFields);
		return this;
	}
	
	public void setOrderBy(boolean asc, Field... orderby) {
		List<Query<?>> elements=elements();
		JoinElement ele=elements().get(elements.size()-1);
		ele.setOrderBy(asc, orderby);
	}
	

	/**
	 * 清除查询条件
	 */
	public void clearQuery() {
		for(QueryAlias al:allElements()){
			al.getQuery().clearQuery();
		}
	}
	
	public Transformer getResultTransformer(){
		if(t==null){
			t=new Transformer();
		}
		return t;
	}
	
	public void setResultTransformer(Transformer t){
		this.t=t;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}
}
