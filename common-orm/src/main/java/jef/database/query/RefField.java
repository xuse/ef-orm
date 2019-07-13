package jef.database.query;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.QueryAlias;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.Reference;
import jef.tools.Assert;

import  org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Objects;

/**
 * 能绑定到一个SQL中查询表上的Field描述。
 * 正常情况下Field只能对应到一张表（静态上）上。不能对应到查询中的一个表实例(动态)上。
 * 因此，在多表查询时——
 * 1、Refield描述当前Field所对应的表，并不是其所存在的Query对象。
 * 2、当一个Condition中同时涉及两个表的Field时，需要显式的明确每个Field所属的Query。
 * 这个对象就专门为了描述这种情况。
 * <p>
 * 
 * RefField可以迟绑定(在查询时根据类型自动匹配对应Query，仅限查询中该类型的Query唯一时)，
 * 早绑定（在查询前指定对用Query），但无论是哪种绑定方式，RefField所在的Condition仅限于单次查询使用。（区别于CascadeCondition）。
 * <p>
 * RefField的一个灵活之处是，其可以作用于级联引起的Join查询中，甚至如果加上了RefField，会强行开启单次Join查询。（即便是在对多关系中。
 *  此时主表的记录会因为和子表Join而变为多份。不作为BUG，因为RefField为仅允许显式添加的场景下，我们认为这种Join是用户期望的而予以支持).
 * 
 * @author jiyi
 *
 */
public class RefField implements Field,LazyQueryBindField{
	private static final long serialVersionUID = 1925450637038779L;
	
	//下面两个字段信息构成一个绑定：
	private Query<?> instance; //构建时不再绑定
	private AbstractEntityMappingProvider dynamicBindContext;//如果当前的绑定是属于动态绑定，需要存储此值
	
	private ColumnMapping field;
	Reference ref;//默认为null，用于迟邦定的情况下匹配Query
	/**
	 * 绑定构造，显式的指定field所绑定的查询表
	 * @param ins
	 * @param field
	 */
	public RefField(Query<?> ins,Field field){
		ColumnMapping mapping=ins.getMeta().getColumnDef(field);
		Assert.notNull(mapping,"The field in refField must be a metamodel field.");
		this.field=mapping;
		this.instance=ins;
	}
	
	/**
	 * 绑定构造，显式的指定field所绑定的查询表
	 * @param p
	 * @param field
	 */
	public RefField(Query<?> p, String field) {
		this.instance=p;
		this.field=p.getMeta().findField(field);
		Assert.notNull(field);
	}

	/**
	 * 非绑定构造，会在后期自动匹配一个Query实例。
	 * 如果该查询中，相同的表出现多次（如自表关联），那么会抛出异常。
	 * @param fld
	 */
	public RefField(Field fld) {
		ColumnMapping mapping=DbUtils.toColumnMapping(fld);
		Assert.notNull(mapping,"The field in refField must be a metamodel field.");
		this.field=mapping;
	}
	
	/**
	 * <tt>框架内部使用</tt>
	 * 传入context，获取绑定的Query。
	 */
	//如果传入null，且没有绑定，则使用Emptyinstanceo
	public Query<?> getInstanceQuery(AbstractEntityMappingProvider context) {
		//这个逻辑有问题，在分页场合下reffield存在重用现象，因此绑定一次以后的动态查询实例在第二次查询中将不再使用。
		//所以不能再用第一次查询时的查询实例，而是要在第二次查询的上下文中重新查找
		if(instance!=null){
			if(dynamicBindContext==null || context==dynamicBindContext){
				return instance;	
			}
		}
		AbstractMetadata type=DbUtils.getTableMeta(getField());
		if(context!=null){
			QueryAlias al=context.findQuery(type,this.ref);
			if(al!=null){
				rebind(al.getQuery(),context);
			}
		}
		if(instance!=null){
			return instance;
		}else{
			return ReadOnlyQuery.getEmptyQuery(type);
		}
	}
	
	/**
	 *  <tt>框架内部使用</tt>
	 * 检查当前引用是否已经通过给定的上下文得到绑定
	 * @param context
	 * @return
	 */
	public boolean isBindOn(AbstractEntityMappingProvider context) {
		//这个逻辑有问题，在分页场合下reffield存在重用现象，因此绑定一次以后的动态查询实例在第二次查询中将不再使用。
		//所以不能再用第一次查询时的查询实例，而是要在第二次查询的上下文中重新查找
		if(instance!=null){
			if(dynamicBindContext==null || context==dynamicBindContext){
				return true;	
			}
		}
		ITableMetadata type=DbUtils.getTableMeta(getField());
		if(context!=null){
			QueryAlias al=context.findQuery(type,this.ref);
			if(al!=null){
				return true;
			}
		}
		return false;
	}
	
	public ColumnMapping getFieldDef(){
		return field;
	}
	
	/**
	 * 返回field
	 * @return field
	 */
	public Field getField() {
		return field.field();
	}
	/**
	 * 返回名称
	 * @return name 
	 */
	public String name() {
		return field.fieldName();
	}

	@Override
	public String toString() {
		return (instance==null?field.getClass().getName():instance.getType())+"."+field.fieldName();
	}
	
	
	//重新绑定查询实体
	void rebind(Query<?> extQuery,AbstractEntityMappingProvider dynamicContext) {
		this.instance=extQuery;
		this.dynamicBindContext=dynamicContext;
	}

	public boolean isBind() {
		return instance!=null;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hash=new HashCodeBuilder();
		hash.append(field);
		if(instance!=null){
			hash.append(instance.getType());	
		}
		return hash.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof RefField)){
			return false;
		}
		RefField rhs=(RefField)obj;
		if(!Objects.equal(field, rhs.field)){
			return false;
		}
		if(instance==null && rhs.instance==null){
			return true;
		}else if(instance==null || rhs.instance==null){
			return false;
		}
		if(!Objects.equal(instance.getType(), rhs.instance.getType())){
			return false;
		}
		return true;
	}

	public ITableMetadata getMeta() {
		return field.getMeta();
	}

	public Query<?> getBind() {
		return instance;
		
	}
	public void setBind(Query<?> query) {
		this.instance=query;
	}
}
