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

import java.util.HashSet;
import java.util.Set;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.query.SqlExpression;


/**
 * A column. It can have the table name it belongs to. 
 */
public class Column implements Expression {
	private String schema;
	private String tableAlias;
    private String columnName = "";

    private final static Set<String> FUNCTION_COLUMN=new HashSet<String>();
    private final static Set<String> VALUE_COLUMN=new HashSet<String>();
    
    static {
    	FUNCTION_COLUMN.add("current_timestamp");
    	FUNCTION_COLUMN.add("current_date");
    	FUNCTION_COLUMN.add("current_time");
    	FUNCTION_COLUMN.add("sysdate");
    	VALUE_COLUMN.add("true");
    	VALUE_COLUMN.add("false");
    }
    
    
    public Column() {
    }

    public Column(String alias, String columnName) {
        this.tableAlias=alias;
        this.columnName = columnName;
    }
    
    public Column(String schema,String name, String columnName) {
    	this.schema=schema;
    	this.tableAlias=name;
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
        return tableAlias;
    }

    public void setColumnName(String string) {
        columnName = string;
    }

    public void setTableAlias(String table) {
        this.tableAlias = table;
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
    	if(tableAlias==null || tableAlias.length()==0){
    		return columnName;
    	}else if(schema!=null){
    		return new StringBuilder(schema.length()+tableAlias.length()+columnName.length()+2).append(schema).append('.').append(tableAlias).append('.').append(columnName).toString();
    	}else{
    		return new StringBuilder(tableAlias.length()+columnName.length()+1).append(tableAlias).append('.').append(columnName).toString();
    	}
    }
    
	public void appendTo(StringBuilder sb) {
		if(tableAlias==null || tableAlias.length()==0){
    		sb.append(columnName);
    	}else if(schema!=null){
    		sb.append(schema).append('.').append(tableAlias).append('.').append(columnName);
    	}else{
    		sb.append(tableAlias).append('.').append(columnName);
    	}
	}
    
    public static Expression getExpression(String columnName){
    	String checkName=columnName.toLowerCase();
    	if(FUNCTION_COLUMN.contains(checkName)){
    		return new Function(columnName);
    	}
    	if(VALUE_COLUMN.contains(checkName)){
    		return new SqlExpression(columnName);
    	}
    	return new Column(null,columnName);
    }

	public ExpressionType getType() {
		return ExpressionType.column;
	}
}
