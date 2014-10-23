package jef.database.query;

import java.util.Collection;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.Field;
import jef.database.IConditionField.Not;
import jef.database.QB;
import jef.database.meta.ITableMetadata;


/**
 * 查询条件生成器
 * 可以更方便的设置各种查询条件
 * 
 */
public class Terms {
	protected ConditionAccessor accessor;
	protected TermsEnd end;
	private Query<?> query;
	
	Terms(Query<?> q) {
		ConditionAccessor accessor=new ConditionAccessor.Q(q);
		this.query=q;
		this.accessor=accessor;
		this.end=new TermsEnd(accessor,q,this);
	}
	
	protected Terms(ConditionAccessor accessor,Query<?> query){
		this.query=query;
		this.accessor=accessor;
		this.end=new TermsEnd(accessor,query,this);
		
	}

	public TermsEnd gt(String key, Object value) {
		Condition cond=QB.gt(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}

	public TermsEnd lt(String key, Object value) {
		Condition cond=QB.lt(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd ge(String key, Object value) {
		Condition cond=QB.ge(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd le(String key, Object value) {
		Condition cond=QB.le(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd eq(String key, Object value) {
		Condition cond=QB.eq(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd ne(String key, Object value) {
		Condition cond=QB.ne(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}

	public TermsEnd in(String key, long[] value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd in(String key, int[] value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd in(String key, Object[] value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd in(String key, Collection<?> value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	
	public TermsEnd gt(Field key, Object value) {
		Condition cond=QB.gt(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd lt(Field key, Object value) {
		Condition cond=QB.lt(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	public TermsEnd ge(Field key, Object value) {
		Condition cond=QB.ge(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	public TermsEnd le(Field key, Object value) {
		Condition cond=QB.le(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	public TermsEnd eq(Field key, Object value) {
		Condition cond=QB.eq(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	public TermsEnd ne(Field key, Object value) {
		Condition cond=QB.ne(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd in(Field key, Object[] value) {
		Condition cond=QB.in(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd in(Field key, int[] value) {
		Condition cond=QB.in(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsEnd in(Field key, long[] value) {
		Condition cond=QB.in(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	public TermsConnector not() {
		Not not=new Not();
		Condition cond=Condition.get(not, Operator.EQUALS, null);
		ConditionAccessor.I context=new ConditionAccessor.I(not,cond);
		context.parent=accessor;
		return new TermsConnector(context,query,3,this);
	}
	
	boolean isBracket(){
		return false;
	}
	
	private Field getField(String key) {
		ITableMetadata meta=query.getMeta();
		Field field= meta.getField(key);
		if(field==null){
			field=meta.getExtendsField(key);
		}
		return field;
	}
	
	boolean bracket;

	/**
	 * 相当于左括号
	 * @return
	 */
	public Terms L$() {
		bracket=true;
		return this;
	}
}
