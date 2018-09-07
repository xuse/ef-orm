package com.github.geequery.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.geequery.codegen.ast.JavaAnnotation;

import jef.database.DbUtils;
import jef.database.meta.object.Column;
import jef.database.meta.object.ForeignKeyItem;
import jef.database.meta.object.PrimaryKey;

public class Metadata{
	private PrimaryKey primaryKey;
	private List<Column> columns;
	private List<ForeignKeyItem> foreignKey;
	
	private Map<String, String> custParams;
	
	public Map<String, String> getCustParams() {
		return custParams;
	}
	public void setCustParams(Map<String, String> custParams) {
		this.custParams = custParams;
	}
	public List<ForeignKeyItem> getForeignKey() {
		return foreignKey;
	}
	public void setForeignKey(List<ForeignKeyItem> foreignKey) {
		this.foreignKey = foreignKey;
	}
	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}
	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}
	public List<Column> getColumns() {
		return columns;
	}
	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}
	public Column findColumn(String columnName) {
		for(Column c: columns){
			if(columnName.equalsIgnoreCase(c.getColumnName())){
				return c;
			}
		}
		return null;
	} 
	public static class ColumnEx extends Column{
		private String fieldName;
		private Boolean generated; //强制描述是否为自动生成键值
		private Class<?> javaType;
		private String initValue;
		private Collection<JavaAnnotation> annotation;
		public Collection<JavaAnnotation> getAnnotation() {
			return annotation;
		}

		public void setAnnotation(Collection<JavaAnnotation> annotation) {
			this.annotation = annotation;
		}

		public String getInitValue() {
			return initValue;
		}

		public void setInitValue(String initValue) {
			this.initValue = initValue;
		}
		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public Class<?> getJavaType() {
			return javaType;
		}

		public void setJavaType(Class<?> javaType) {
			this.javaType = javaType;
		}

		public Boolean getGenerated() {
			return generated;
		}

		public void setGenerated(Boolean generated) {
			this.generated = generated;
		}
	}
	
	public static class ForeignKeyEx extends ForeignKeyItem{
		private String fieldName;

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
	}
	/**
	 * 将主键拆分为由字段名/列名构成的对
	 * @param data
	 * @return
	 */
	public Map<String,String> getPkFieldAndColumnNames(){
		Map<String,String> pkColumns=new HashMap<String,String>();
		if(getPrimaryKey()!=null){
			for(String columnName: getPrimaryKey().getColumns()){
				Column column=findColumn(columnName);
				String fieldName;
				if(column instanceof ColumnEx){
					fieldName=((ColumnEx) column).getFieldName();
				}else{
					fieldName=DbUtils.underlineToUpper(columnName.toLowerCase(),false);	
				}
				pkColumns.put(fieldName,columnName);
			}	
		}
		return pkColumns;
	}
}
