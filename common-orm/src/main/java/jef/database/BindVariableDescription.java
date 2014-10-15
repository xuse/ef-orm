package jef.database;

import jef.database.Condition.Operator;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleField;
import jef.tools.Assert;

/*
 * 描述字段加条件用于匹配绑定变量参数
 * 每个实例对应一个SQL中的问号，通过List来确定其在绑定SQL中的序号
 */
public class BindVariableDescription{
	private Field field;//这个Field可以描述一个实际的条件路径
	private Operator oper;	//操作符
	private Object bindedVar;//绑定变量的值
	private ITableMetadata meta;
	
	private VariableCallback callback;
	private boolean inBatch;
	
	public boolean isInBatch() {
		return inBatch;
	}
	public void setInBatch(boolean inBatch) {
		this.inBatch = inBatch;
	}
	public BindVariableDescription(Field field,Operator oper,Object bindVar){
		Class<?> clz=field.getClass();
		if(clz==TupleField.class){
			this.meta=((TupleField)field).getMeta();
			this.field=field;
			this.oper=oper;
			this.bindedVar=bindVar;
			return;
		}
		Assert.isTrue(clz.isEnum()||clz==FBIField.class,"The Field in bind variable desc must be a metamodel field");
		this.field=field;
		this.oper=oper;
		this.bindedVar=bindVar;
		this.meta=DbUtils.getTableMeta(field);
	}
	
	@SuppressWarnings("rawtypes")
	public ColumnMapping getColumnType(){
		return meta==null?null:meta.getColumnDef(field); 
	}
	
	public VariableCallback getCallback() {
		return callback;
	}
	public void setCallback(VariableCallback callback) {
		this.callback = callback;
	}
	public Object getBindedVar() {
		if(inBatch)throw new UnsupportedOperationException("The Query is in batch Mode, you should not fetch param from a bind desc.");
		return bindedVar;
	}
	public String toString(){
		return name();
	}
	
	public Field getField() {
		return field;
	}
	public Operator getOper() {
		return oper;
	}
	public String name() {
		return field.name().concat(" "+oper.getKey());
	}
}

