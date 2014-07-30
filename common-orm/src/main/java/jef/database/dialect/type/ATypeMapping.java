package jef.database.dialect.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Expression;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.InsertSqlResult;
import jef.tools.reflect.BeanUtils;

public abstract class ATypeMapping<T> implements MappingType<T>{
	/**
	 * 原始的ColumnName
	 */
	protected String rawColumnName;
	protected ITableMetadata meta;
	private String fieldName;
	protected Field field;
	protected ColumnType ctype;
	protected Class<T> clz;
	private Class<?>   primitiveClz;
	private boolean pk;
	protected transient String cachedEscapeColumnName;
	protected transient DatabaseDialect bindedProfile;
	
	@SuppressWarnings("unchecked")
	public ATypeMapping(){
		Type type=this.getClass().getGenericSuperclass();
		if (type instanceof ParameterizedType) {
			Type[] p = ((ParameterizedType) type).getActualTypeArguments();
			if(p[0] instanceof Class){
				this.clz = (Class<T>) p[0];
			}
			this.primitiveClz=BeanUtils.toPrimitiveClass(this.clz);
		}
	}
	
	public boolean isPk() {
		return pk;
	}


	public void init(Field field,String columnName,ColumnType type,ITableMetadata meta){
		this.field=field;
		this.fieldName=field.name();
		this.rawColumnName=columnName;
		this.meta=meta;
		this.ctype=type;
	}
	
	public String fieldName(){
		return fieldName;
	}
	
	public String columnName(){
		return rawColumnName;
	}
	
	public Field field(){
		return field;
	}
	
	public ITableMetadata getMeta(){
		return meta;
	}
	
	public ColumnType get() {
		return ctype;
	}

	public Class<T> getFieldType() {
		return clz;
	}
	
	public Class<?> getPrimitiveType() {
		return primitiveClz;
	}

	public String getColumnName(DatabaseDialect profile, boolean escape) {
		if(escape && bindedProfile==profile){
			return cachedEscapeColumnName;
		}
		String name = profile.getColumnNameIncase(rawColumnName);
		if(escape){
			String escapedColumn=DbUtils.escapeColumn(profile, name);
			rebind(escapedColumn,profile);
			return escapedColumn;
		}
		return name;
	}

	protected void rebind(String escapedColumn, DatabaseDialect profile) {
		bindedProfile=profile;
		cachedEscapeColumnName=escapedColumn;
	}

	public void setPk(boolean b) {
		this.pk=b;
	}
	
	/**
	 * 用单引号包围字符串，并将其中的单引号按SQL转义
	 * @param s
	 * @return
	 */
	public final static String wrapSqlStr(String s){
		StringBuilder sb=new StringBuilder(s.length()+16);
		sb.append('\'');
		for(int i=0;i<s.length();i++){
			char c=s.charAt(i);
			if(c=='\''){
				sb.append("''");
			}else{
				sb.append(c);
			}
		}
		sb.append('\'');
		return sb.toString();
	}
	
	public String getSqlStr(Object value,DatabaseDialect profile){
		if(value==null){
			return "null";
		}else if(value instanceof Expression){
			return value.toString();
		}
		return getSqlExpression(value,profile);
	}
	
	/**
	 * @param value 不为null，不为Expression
	 */
	protected abstract String getSqlExpression(Object value,DatabaseDialect profile);
	
	         
	public void processInsert(Object value, InsertSqlResult result, List<String> cStr, List<String> vStr,boolean smart,IQueryableEntity obj)throws SQLException {
		String columnName=getColumnName(result.profile, true);
		if (value==null){
			if(result.profile.has(Feature.NOT_SUPPORT_KEYWORD_DEFAULT)) {// 必须使用默认的方法(即不插入)来描述缺省值
			} else {
				cStr.add(columnName);// 为空表示不指定，使用缺省值
				vStr.add("DEFAULT");
			}
			return;
		}
		if (smart && !obj.isUsed(field)) {
			return;
		}
		cStr.add(columnName);
		vStr.add(getSqlStr(value,result.profile));
	}
	
	public void processPreparedInsert(IQueryableEntity obj, List<String> cStr, List<String> vStr, InsertSqlResult result, boolean dynamic)throws SQLException{
		if (dynamic && !obj.isUsed(field)) {
			return;
		}
		String columnName=getColumnName(result.profile, true);
		cStr.add(columnName);
		vStr.add("?");
		result.addField(field);		
	}
	
	public boolean isLob() {
		return false;
	}

	public boolean applyFor(int type) {
		return type==getSqlType();
	}
	
}
