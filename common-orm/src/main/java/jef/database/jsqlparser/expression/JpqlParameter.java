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

import javax.persistence.Parameter;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;
import jef.tools.StringUtils;

/**
 * A '?' in a statement
 * 
 * @see JpqlDataType
 */
@SuppressWarnings("rawtypes")
public class JpqlParameter implements Expression,FromItem,Parameter {
	/**
	 * 参数名称， 对于 :name 类型的
	 */
	private String name;
	/**
	 * 参数序号 ,对于 ?1 类型的 
	 */
	private int index = -1;
	
	/**
	 * 参数类型
	 */
	private JpqlDataType type;
	
	private ThreadLocal<Object> resolved = new ThreadLocal<Object>(); //-1未解析 0已解析单参数 >0数组参数，需要对应多次绑定 String SQL片段

	public String getName() {
		return name;
	}

	public JpqlDataType getDataType() {
		return type;
	}

	public boolean isNamedParam(){
		return name!=null;
	}
	
	public boolean isIndexParam(){
		return index>-1;
	}
	
	public int getIndex() {
		return index;
	}

	/**
	 * -2 SQL片段 -1未解析 0单参数 >0数组参数
	 * @return
	 */
	public int resolvedCount(){
		Object obj=resolved.get();
		if(obj==null)return -1;
		if(obj instanceof String){
			return -2;
		}
		return (Integer)obj;
	}
	
	public Object getResolved() {
		return resolved.get();
	}
	public void setResolved(String text) {
		resolved.set(text);
	}
	public void setResolved(int resolved) {
		this.resolved.set(resolved);
	}
	public void setNotUsed(){
		this.resolved.set(null);
	}
	
	public JpqlParameter(String str, boolean isNumber,String type) {
		if (isNumber) {
			this.index = Integer.valueOf(str);
		} else {
			this.name = str;
		}
		if(type!=null){
			this.type=JpqlDataType.valueOf(type.toUpperCase());	
		}
	}

	public void accept(ExpressionVisitor expressionVisitor) {
		expressionVisitor.visit(this);
	}


	public void appendTo(StringBuilder sb) {
		String s=getReslovedString();
		sb.append(s);
		if(!StringUtils.isEmpty(tableAlias)){
			sb.append(' ').append(tableAlias);
		}
	}
	
	public String toString() {
		String s=getReslovedString();
		if(StringUtils.isEmpty(tableAlias)){
			return s;
		}else{
			return StringUtils.concat(s," ",tableAlias);
		}
	}

	private String getReslovedString() {
		Object obj=resolved.get();
		if(obj instanceof String){
			return (String)obj;
		}
		Integer value=(Integer)obj;
		if(value==null || value<0){//未解析
			if (name == null) {
				return "?".concat(String.valueOf(index));
			} else {
				return ":".concat(name);
			}
		}
		if(value==0){//单参数
			return "?";
		}else{//value>0
			StringBuilder sb=new StringBuilder("?");
			StringUtils.repeat(sb,",?", value-1);
			return sb.toString();
		}
	}

	public Object getKey() {
		if(name!=null)return name;
		return index;
	}

	public void accept(SelectItemVisitor fromItemVisitor) {
		fromItemVisitor.visit(this);
	}
	private String tableAlias;

	public String getAlias() {
		return tableAlias;
	}

	public void setAlias(String alias) {
		this.tableAlias=alias;
	}

	public String toWholeName() {
		return getReslovedString();
	}

	public Integer getPosition() {
		return index;
	}

	public Class<?> getParameterType() {
		if(this.type==null)return Object.class;
		switch(this.type){
		case $STRING:
		case $STRING$:
		case STRING$:
		case STRING:
		case SQL:
			return String.class;
		case BOOLEAN:
			return Boolean.class;
		case DATE:
			return java.sql.Date.class;
		case TIMESTAMP:
			return java.sql.Timestamp.class;
		case DOUBLE:
			return Double.class;
		case FLOAT:
			return Float.class;
		case INT:
			return Integer.class;
		case LONG:
			return Long.class;
		case SHORT:
			return Short.class;
		}
		return Object.class;
	}
	
	public ExpressionType getType() {
		return ExpressionType.param;
	}
}
