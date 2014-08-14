/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.jsqlparser.expression;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;


/**
 * A column. It can have the table name it belongs to. 
 */
public class Column implements Expression {
    private String columnName = "";

    private String alias;
    private String schema;

    public Column() {
    }

    public Column(String alias, String columnName) {
        this.alias=alias;
        this.columnName = columnName;
    }
    
    public Column(String schema,String name, String columnName) {
    	this.schema=schema;
    	this.alias=name;
        this.columnName = columnName;
    }
    

    public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getColumnName() {
        return columnName;
    }
    
    public String getSchema() {
		return schema;
	}

	public String getTableAlias() {
        return alias;
    }

    public void setColumnName(String string) {
        columnName = string;
    }

    public void setTableAlias(String table) {
        this.alias = table;
    }

    /**
     * 
	 * @return the name of the column, prefixed with 'tableName' and '.' 
	 */
    public String getWholeColumnName() {
    	return toString();
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public String toString() {
    	if(alias==null || alias.length()==0){
    		return columnName;
    	}else if(schema!=null){
    		return new StringBuilder(schema.length()+alias.length()+columnName.length()+2).append(schema).append('.').append(alias).append('.').append(columnName).toString();
    	}else{
    		return new StringBuilder(alias.length()+columnName.length()+1).append(alias).append('.').append(columnName).toString();
    	}
    }
    
	public void appendTo(StringBuilder sb) {
		if(alias==null || alias.length()==0){
    		sb.append(columnName);
    	}else if(schema!=null){
    		sb.append(schema).append('.').append(alias).append('.').append(columnName);
    	}else{
    		sb.append(alias).append('.').append(columnName);
    	}
	}
    
    public static Expression getExpression(String columnName){
    	if("sysdate".equals(columnName) || columnName.equals("current_timestamp") || columnName.equals("current_date")||columnName.equals("current_time")){
    		return new Function(columnName);
    	}
    	return new Column(null,columnName);
    }

	public ExpressionType getType() {
		return ExpressionType.column;
	}
}
