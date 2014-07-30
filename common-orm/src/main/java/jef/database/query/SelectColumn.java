package jef.database.query;

import java.util.ArrayList;
import java.util.List;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbFunction;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.MappingType;
import jef.database.meta.ITableMetadata;

public final class SelectColumn extends SingleColumnSelect{
	private String populateTo;//拼装路径，默认应该和alias一致
	private String alias;
	private Field targetField;
	private ITableMetadata meta;
	protected List<DbFunctionCall> func;  //附加的函数
	
	
	
	//缓存一个不带别名的列，以提高速度
	private String columnSimpleName;//不带别名的参数

	/**
	 * 转换为Having子句
	 */
	public String toHavingClause(DatabaseDialect profile,String tableAlias,SqlContext context){
		return Condition.toSql(innerGetColumn(profile, tableAlias), havingCondOperator, havingCondValue, profile, null, null);
	}
	
	/**
	 * 设置having子句条件
	 * @param oper
	 * @param value
	 * @return
	 */
	public SelectColumn having(Operator oper,Object value){
		this.projection=projection| PROJECTION_HAVING;
		this.havingCondOperator=oper;
		this.havingCondValue=value;
		return this;
	}
	
	/**
	 * 设置having子句条件（该表达式不作为被选择列）。默认情况下 .column().sum().having(">",100);时， column().sum()也会作为查询列之一被查出。
	 * 使用havingOnly方法添加的having则是纯作为条件，不会作为查询列。
	 * 
	 * @param oper
	 * @param value
	 * @return
	 */
	public SelectColumn havingOnly(Operator oper,Object value){
		this.projection=projection |PROJECTION_HAVING_NOT_SELECT;
		this.havingCondOperator=oper;
		this.havingCondValue=value;
		return this;
	}
	
	/**
	 * count(xxx)
	 * @return
	 */
	public SelectColumn count(){
		this.projection=PROJECTION_COUNT;
		return this;
	}
	
	/**
	 * 求和
	 * @return
	 */
	public SelectColumn sum(){
		this.projection=PROJECTION_SUM;
		return this;
	}
	
	/**
	 * 求平均数
	 * @return
	 */
	public SelectColumn avg(){
		this.projection=PROJECTION_AVG;
		return this;
	}
	
	/**
	 * 取最大值
	 * @return
	 */
	public SelectColumn max(){
		this.projection=PROJECTION_MAX;
		return this;
	}
	
	/**
	 * 取最小值
	 * @return
	 */
	public SelectColumn min(){
		this.projection=PROJECTION_MIN;
		return this;
	}
	
	/**
	 * 对应count(distinct xx)
	 * @return
	 */
	public SelectColumn countDistinct(){
		this.projection=PROJECTION_COUNT_DISTINCT;
		return this;
	}
	
	/**
	 * 指定按照此列进行 group by操作，同时选出此列
	 * @return
	 */
	public SelectColumn group() {
		this.projection=PROJECTION_GROUP;
		return this;
	}
	/**
	 * 指定按此列进行函数计算
	 * @param func
	 * @param params
	 * @return
	 */
	public SelectColumn funcTemplate(String func,String... params){
		this.projection=PROJECTION_CUST_FUNC;
		if(this.func==null){
			this.func=new ArrayList<DbFunctionCall>();	
		}
		this.func.add(new DbFunctionCall(func,params));
		return this;
	}
	/**
	 * 指定按此列进行函数计算
	 * @param func
	 * @param params
	 * @return
	 */
	public SelectColumn func(DbFunction func,String... params){
		this.projection=PROJECTION_CUST_FUNC;
		if(this.func==null){
			this.func=new ArrayList<DbFunctionCall>();	
		}
		this.func.add(new DbFunctionCall(func,params));
		return this;
	}
	
	SelectColumn(Field field,String populateTo){
		this.targetField=field;
		if(populateTo==null)populateTo=field.name();
		this.populateTo=populateTo;
		this.meta=DbUtils.getTableMeta(field);
	}

	void setProjection(int projection) {
		this.projection = projection;
	}
	
	/**
	 * 将Projection重置
	 */
	public void clearProjection(){
		this.projection=PROJECTION_NORMAL;
		this.func=null;
	}

	/**
	 * 指定别名。这个方法不仅仅指定该查询列的别名，还指定该列将写入到指定对象字段中去
	 * @param alias
	 * @return
	 */
	public SelectColumn as(String alias) {
		this.alias = alias;
		if(alias!=null){
			this.populateTo=alias;	
		}
		return this;
	}

	public String getName() {
		return populateTo;
	}
//	生成选择语句时生成列名别，null表示无别名
	public String getSelectedAlias(String tableAlias,DatabaseDialect profile,boolean isSelect) {
		if(isSelect)return DbUtils.escapeColumn(profile,profile.getColumnNameIncase(alias));
		if(alias==null){
			if(columnSimpleName==null){
				throw new IllegalArgumentException();
			}
			return profile.getColumnNameIncase(columnSimpleName);
		}else{
			return profile.getColumnNameIncase(alias);	
		}
	}
	
	private String innerGetColumn(DatabaseDialect profile,String tableAlias){
		//因为第一次操作列名改为额大写，造成第二次Parse出错
		columnSimpleName=DbUtils.toColumnName(targetField, profile);//不带别名的列名
		
		String name= DbUtils.toColumnName(targetField, profile,tableAlias);
		int key=projection & 0xFF;
		if(key>0){
//			if(StringUtils.isEmpty(alias)){//强行取Alias(无必要)
//				alias="C".concat(RandomStringUtils.randomAlphanumeric(12));
//			}
			switch(key){
			case PROJECTION_COUNT:
				return "count(".concat(name).concat(")");
			case PROJECTION_COUNT_DISTINCT:
				return "count(distinct ".concat(name).concat(")");
			case PROJECTION_SUM:
				return "sum(".concat(name).concat(")");
			case PROJECTION_AVG:
				return "avg(".concat(name).concat(")");
			case PROJECTION_MAX:
				return "max(".concat(name).concat(")");
			case PROJECTION_MIN:
				return "min(".concat(name).concat(")");
			case PROJECTION_CUST_FUNC:
				String start=name;
				for(DbFunctionCall func:this.func){
					Object[] args;
					if(func.getVarIndex()==-1){
						args=new String[]{start};
					}else{
						args=func.getArgs();
						args[func.getVarIndex()]=start;
					}
					if(func.getFunc()==null){
						StringBuilder sb=new StringBuilder(func.getName()).append('(');
						for(int i=0;i<args.length;i++){
							if(i>0){
								sb.append(", ");
							}
							sb.append(args[i]);
						}
						start=sb.append(')').toString();
					}else{
						start=profile.getFunction(func.getFunc(),args);
					}
				}
				return start;
			default:
				throw new IllegalArgumentException("Unknown projection "+ key);
			}
		}else{
			return name;
		}
	}

	//当生成选择语句时计算列名称
	public String getSelectItem(DatabaseDialect profile,String tableAlias,SqlContext context) {
		if(targetField==null)return null;
		if((projection & PROJECTION_HAVING_NOT_SELECT)>0){
			return null;//纯having的列，不进行select,也不进行group
		}
		if(targetField instanceof JpqlExpression){
			return columnSimpleName=((JpqlExpression) targetField).toSqlAndBindAttribs(context, profile);
		}
		return innerGetColumn(profile,tableAlias);
	}

	public boolean isSingleColumn() {
		return true;
	}

	public MappingType<?> getTargetColumnType() {
		return meta.getColumnDef(targetField);
	}

	@Override
	public String toString() {
		return populateTo+":"+targetField+(alias==null?"":" as "+alias);
	}
	
	
}
