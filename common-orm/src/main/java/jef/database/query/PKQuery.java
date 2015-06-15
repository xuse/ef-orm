package jef.database.query;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IConditionField;
import jef.database.IQueryableEntity;
import jef.database.ORMConfig;
import jef.database.SelectProcessor;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.QueryClauseImpl;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.tools.ArrayUtils;
import jef.tools.Assert;

@SuppressWarnings("serial")
public class PKQuery<T extends IQueryableEntity> extends AbstractQuery<T>{
	
	private List<Serializable> pkValues;
	
	private boolean cascadeViaOuterJoin=ORMConfig.getInstance().isUseOuterJoin();
	/**
	 * 结果转换器
	 */
	protected final Transformer t;
	
	@SuppressWarnings("unchecked")
	public PKQuery(ITableMetadata clz,Serializable... pks){
		if(clz.getType()==EntityType.TEMPLATE){
			String key=String.valueOf(pks[0]);
			pks=(Serializable[]) ArrayUtils.subarray(pks, 1, pks.length);
			this.type=((AbstractMetadata)clz).getExtension(key).getMeta();			
		}else{
			this.type=clz;
		}
		this.instance=(T) clz.newInstance();
		if(type.getPKFields().size()>pks.length){
			throw new IllegalArgumentException("The primark key count is not match input value:"+type.getPKFields().size()+">"+pks.length);
		}
		t=new Transformer(type);
		pkValues=Arrays.asList(pks);
	}

	@SuppressWarnings("unchecked")
	public PKQuery(ITableMetadata meta, List<Serializable> pkValueSafe, IQueryableEntity obj) {
		this.type=meta;
		this.instance=(T) obj;
		t=new Transformer(type);
		pkValues=pkValueSafe;
	}

	public List<Condition> getConditions() {
		return new AbstractList<Condition>() {
			@Override
			public Condition get(int index) {
				ColumnMapping m=type.getPKFields().get(index);
				Object value=pkValues.get(index);
				Assert.notNull(value);
				return new Condition(m.field(),Operator.EQUALS,value);
			}

			@Override
			public int size() {
				return type.getPKFields().size();
			}
		};
	}
	
	@Override
	public QueryClause toPrepareQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		String tableName = (String) getAttribute(JoinElement.CUSTOM_TABLE_NAME);
		if (tableName != null)
			tableName = MetaHolder.toSchemaAdjustedName(tableName);
		PartitionResult[] prs = DbUtils.toTableNames(getInstance(), tableName, this, processor.getPartitionSupport());
		
		
		DatabaseDialect profile = processor.getProfile(prs);
		BindSql whereResult = toPrepareWhereSql(context, profile);
		QueryClauseImpl result = new QueryClauseImpl(profile);
		result.setGrouphavingPart(GroupClause.DEFAULT);
		result.setSelectPart(SelectProcessor.toSelectSql(context, GroupClause.DEFAULT, profile));
		result.setTables(type.getTableName(false), prs);
		result.setWherePart(whereResult.getSql());
		result.setBind(whereResult.getBind());
		return result;
	}

	public BindSql toPrepareWhereSql(SqlContext context, DatabaseDialect profile) {
		int size=pkValues.size();
		StringBuilder sb=new StringBuilder(128).append(" where ");
		List<BindVariableDescription> bind = new ArrayList<BindVariableDescription>(size);
		
		Iterator<ColumnMapping> pkfields=type.getPKFields().iterator();
		
		int n=0;
		ColumnMapping field=pkfields.next();
		sb.append(field.getColumnName(profile, true)).append("= ?");
		bind.add(new BindVariableDescription(field.field(), Operator.EQUALS, pkValues.get(n++)));
		while(pkfields.hasNext()){
			field=pkfields.next();
			sb.append(" and ").append(field.getColumnName(profile, true)).append("= ?");
			bind.add(new BindVariableDescription(field.field(), Operator.EQUALS, pkValues.get(n++)));
		}
		return new BindSql(sb.toString(), bind);
	}

	public EntityMappingProvider getSelectItems() {
		return null;
	}

	public void setSelectItems(Selects select) {
		throw new UnsupportedOperationException();
	}

	public SqlContext prepare() {
		return new SqlContext("t", this);
	}

	public void clearQuery() {
	}

	@SuppressWarnings("unchecked")
	public Collection<OrderField> getOrderBy() {
		return Collections.EMPTY_LIST;
	}

	public void setOrderBy(boolean asc, Field... orderby) {
		throw new UnsupportedOperationException();
	}

	public void addOrderBy(boolean asc, Field... orderby) {
		throw new UnsupportedOperationException();
	}

	public void setAttribute(String key, Object value) {
		throw new UnsupportedOperationException();
	}

	public Object getAttribute(String key) {
		return null;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getAttributes() {
		return Collections.EMPTY_MAP;
	}

	public Query<T> addExtendQuery(Query<?> querys) {
		throw new UnsupportedOperationException();
	}

	public Query<T> setAllRecordsCondition() {
		throw new UnsupportedOperationException();
	}

	public Query<T> addCondition(Field field, Object value) {
		throw new UnsupportedOperationException();
	}

	public Query<T> addCondition(Field field, Operator oper, Object value) {
		throw new UnsupportedOperationException();
	}

	public Query<T> addCondition(IConditionField field) {
		throw new UnsupportedOperationException();
	}

	public Query<T> addCondition(Condition condition) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public Map<Reference, List<Condition>> getFilterCondition() {
		return Collections.EMPTY_MAP;
	}

	public Query<?>[] getOtherQueryProvider() {
		return EMPTY_Q;
	}

	public void setCustomTableName(String name) {
		throw new UnsupportedOperationException();
	}

	public Query<T> addCascadeCondition(Condition cond) {
		throw new UnsupportedOperationException();
	}

	public Query<T> addCascadeCondition(String refName, Condition... conds) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return type.getName()+"[PK]";
	}

	public boolean isCascadeViaOuterJoin() {
		return cascadeViaOuterJoin;
	}

	public void setCascadeViaOuterJoin(boolean cascadeViaOuterJoin) {
		this.cascadeViaOuterJoin=cascadeViaOuterJoin;
	}
	
	public void setCascade(boolean cascade) {
		t.setLoadVsMany(cascade);
		t.setLoadVsOne(cascade);
	}

	@Override
	public boolean isAll() {
		return false;
	}

	@Override
	public Transformer getResultTransformer() {
		return t;
	}

	@Override
	public boolean isSelectCustomized() {
		PopulateStrategy[] s=t.getStrategy();
		if(s!=null && s.length>0)return true;
		return false;
	}
}
