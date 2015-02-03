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
	/**
	 * 产生相等条件 (=)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field = value} 这样的条件
	 */
	public TermsEnd eq(String key, Object value) {
		Condition cond=QB.eq(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * 产生不等条件 (<> 或 !=)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field != value} 这样的条件
	 */
	public TermsEnd ne(String key, Object value) {
		Condition cond=QB.ne(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * 产生IN条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public TermsEnd in(String key, long[] value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * 产生IN条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public TermsEnd in(String key, int[] value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * 产生IN条件 (in)
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 数组
	 * @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public TermsEnd in(String key, Object[] value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * 产生IN条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param value
	 *            条件的值 集合
	 * @return @return 表达式为 {@code field in (value,...)} 这样的条件
	 */
	public TermsEnd in(String key, Collection<?> value) {
		Condition cond=QB.in(getField(key), value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生大于条件 ( >)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field > value} 这样的条件
	 */
	public TermsEnd gt(Field key, Object value) {
		Condition cond=QB.gt(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生小于条件 （<）
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field < value} 这样的条件
	 */
	public TermsEnd lt(Field key, Object value) {
		Condition cond=QB.lt(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生大于等于条件 （ >= ）
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field >= value} 这样的条件
	 */
	public TermsEnd ge(Field key, Object value) {
		Condition cond=QB.ge(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生小于等于条件 （ <= )
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field <= value} 这样的条件
	 */
	public TermsEnd le(Field key, Object value) {
		Condition cond=QB.le(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生相等条件 (=)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field = value} 这样的条件
	 */
	public TermsEnd eq(Field key, Object value) {
		Condition cond=QB.eq(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生不等条件 (<> 或 !=)
	 * 
	 * @param field
	 *            表的字段 （也可以是函数表达式）
	 * @param value
	 *            条件的值，一般传入String,Number,Date等基本数据，也可传入Field对象、
	 *            或者是SqlExpression等对象。
	 * @return 表达式为 {@code field != value} 这样的条件
	 */
	public TermsEnd ne(Field key, Object value) {
		Condition cond=QB.ne(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * 产生MatchStart条件
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like 'str%' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	public TermsEnd matchStart(Field key, String value) {
		Condition cond=QB.matchStart(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * 产生MatchEnd条件，
	 * 
	 * @param field
	 *            表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like '%str' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	public TermsEnd matchEnd(Field key, String value) {
		Condition cond=QB.matchEnd(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	
	/**
	 * Like %str%的条件
	 * 
	 * @param field
	 *            field 表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like '%str%' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	public TermsEnd matchAny(Field key, String value) {
		Condition cond=QB.matchAny(key, value);
		cond=accessor.add(cond);
		return end.set(cond);
	}
	/**
	 * Like条件，参数为自定义的字符串。 例如
	 * <p>
	 * {@code like(field, "%123_456%")}
	 * <p>
	 * 相当于
	 * <p>
	 * {@code WHERE field LIKE '%123_456%'  }
	 * <p>
	 * 
	 * <h3>注意</h3> 这个方法可以自由定义复杂的匹配模板外，但是和matchxxx系列的方法相比，不会对字符串中的'%'
	 * '_'做转义。因此实际使用不当会有SQL注入风险。
	 * <p>
	 * 
	 * @param field
	 *            field 表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like 'str' } 这样的条件，str中原来的的'%' '_'符号会被保留
	 */
	public TermsEnd like(Field key, String value) {
		Condition cond=QB.like(key, value);
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
