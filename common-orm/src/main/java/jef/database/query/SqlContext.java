package jef.database.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.Field;
import jef.database.ORMConfig;
import jef.database.QueryAlias;
import jef.database.meta.Reference;

//SQL语句上下文,由所有的IQuery对象和对应的Alias构成

public class SqlContext extends AbstractEntityMappingProvider  implements EntityMappingProvider{
	private static final long serialVersionUID = -636020153595472094L;
	public static final String DIVEDER = "__";
	
	private Map<Reference, List<Condition>> filters;
	
	private int currentIndex;
	Map<String,Object> attribute;
	SelectsImpl selectsImpl;
	
	@Override
	public boolean isDistinct() {
		if(selectsImpl!=null)return selectsImpl.distinct;
		return super.isDistinct();
	}
	@Override
	public List<ISelectItemProvider> getReference() {
		if(selectsImpl!=null)return selectsImpl.getReference();
		return super.getReference();
	}
	public SelectsImpl getSelectsImpl() {
		return selectsImpl;
	}

	public SqlContext(SqlContext old,String newAlias,Query<?> newQuery) {
		this.queries=new ArrayList<QueryAlias>(old.queries);
		queries.add(new QueryAlias(newAlias, newQuery));
		currentIndex=queries.size()-1;
		this.attribute=old.attribute;
		this.selectsImpl=old.selectsImpl;
		this.distinct=old.distinct;
	}
	
	public SqlContext(String alias, Query<?> query) {
		this.currentIndex=0;
		QueryAlias qa=new QueryAlias(alias,query);
		AllTableColumns ac=new AllTableColumns(query);
		ac.setLazyLob(ORMConfig.getInstance().isEnableLazyLob());
		qa.setFields(ac);
		this.queries=Arrays.asList(qa);
	}
	
	public SqlContext(int current, List<QueryAlias> queries, Selects selected) {
		this.currentIndex=current;
		this.queries=queries;
		if(selected!=null){
			this.distinct=selected.isDistinct();
			this.selectsImpl=(SelectsImpl)selected;
		}
	}
	
	public SqlContext(Selects selected) {
		SelectsImpl select=(SelectsImpl)selected;//目前只有一个已知实现
		this.queries=select.queries;
		this.distinct=select.distinct;
		this.selectsImpl=select;
	}
	public QueryAlias findCurrent() {
		if(currentIndex<0){
			return null;			
		}
		return queries.get(currentIndex);
	}
	public String getAliasOf(ConditionQuery q){
		if(q==null ||q instanceof Join)return null;
		for(ISelectItemProvider qa: queries){
			if(qa.getTableDef()==q){
				return qa.getSchema();
			}
		}
		return null;
	}
	
	/**
	 * 查找以指定的Query为Current的SqlContext，如果没有匹配，或者输入的是一个Join，那么输出Alias为null的SqlContext
	 * @param q
	 * @return
	 */
	public SqlContext getContextOf(JoinElement q){ //此处要收敛为Query
		if(q instanceof Join)return new SqlContext(-1,this);
		for(int i=0;i<queries.size();i++){
			QueryAlias qa=queries.get(i);	
			if(qa.getTableDef()==q){
				return new SqlContext(i,this);
			}
		}
		return new SqlContext(-1,this);
	}
	
	private SqlContext(int current, SqlContext raw) {
		this.currentIndex=current;
		this.queries=raw.queries;
		this.attribute=raw.attribute;
		this.selectsImpl=raw.selectsImpl;
		this.distinct=raw.distinct;
	}
	
	public String getCurrentAlias() {
		if(currentIndex>-1){
			return queries.get(currentIndex).getAlias();
		}
		return null;
	}
	
	public String getCurrentAliasAndCheck(Field field) {
		if(currentIndex>-1){
//			这里加入校验的目的是为了防止用户将非当前Query的Field插入以作为Condition。但目前由于Query中增加了检查和RefField包装，此处简化。以后视情况回复检查
//			QueryAlias qa=queries.get(currentIndex);
//			ITableMetadata meta=DbUtils.getTableMeta(field);
//			if(qa.getQuery().getMeta().containsMeta(meta)){
//				return qa.getAlias();	
//			}
//			throw new IllegalArgumentException("The Field [" +meta.getSimpleName()+"."+ field +"] is not belongs to ["+qa.getQuery().getMeta().getSimpleName()+"]");			
			return queries.get(currentIndex).getAlias();
		}
		return null;
	}
	
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}
	public void setFilters(Map<Reference, List<Condition>> filters) {
		this.filters = filters;
	}
	public int size() {
		return queries.size();
	}

	public String getAliasOf(int i) {
		return queries.get(i).getSchema();
	}
}
