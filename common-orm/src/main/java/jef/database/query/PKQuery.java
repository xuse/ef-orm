package jef.database.query;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.Field;
import jef.database.IConditionField;
import jef.database.IQueryableEntity;
import jef.database.ORMConfig;
import jef.database.dialect.type.MappingType;
import jef.database.meta.ITableMetadata;
import jef.database.meta.Reference;
import jef.database.wrapper.populator.Transformer;
import jef.tools.Assert;

public class PKQuery<T extends IQueryableEntity> extends AbstractQuery<T>{
	
	private List<Serializable> pkValues;
	
	private boolean cascadeViaOuterJoin=ORMConfig.getInstance().isUseOuterJoin();
	
	@SuppressWarnings("unchecked")
	public PKQuery(ITableMetadata clz,Serializable... pks){
		this.type=clz;
		this.instance=(T) clz.instance();
		pkValues=Arrays.asList(pks);
	}

	public List<Condition> getConditions() {
		return new AbstractList<Condition>() {
			@Override
			public Condition get(int index) {
				MappingType<?> m=type.getPKFields().get(index);
//				Object value=m.getFieldAccessor().get(instance);
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

	public EntityMappingProvider getSelectItems() {
		return null;
	}

	public void setSelectItems(Selects select) {
		throw new UnsupportedOperationException();
	}

	public SqlContext prepare() {
		SqlContext context = new SqlContext("t", this);
		return context;
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
		if(t==null){
			t=new Transformer(type);
		}
		t.setLoadVsMany(cascade);
		t.setLoadVsOne(cascade);
	}

	@Override
	public boolean isAll() {
		return false;
	}
}
