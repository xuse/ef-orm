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

import java.math.BigDecimal;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.SqlValue;

/**
 * A string as in 'example_string'
 */
public class StringValue implements Expression,SqlValue {

    private String value = "";

    public StringValue(){
    }
    
    
    public StringValue(String escapedValue) {
        this(escapedValue,true);
    }
    
    public int length(){
    	return value.length();
    }
    
    public StringValue(String value,boolean escape) {
    	if(escape){
    		this.value = value.substring(1, value.length() - 1);
    	}else{
    		this.value=value;
    	}
    }
    
    /**
     * 注意得到得到的Value是SQL转义后的value
     */
    public String getValue() {
        return value;
    }

    public String getNotExcapedValue() {
        StringBuffer buffer = new StringBuffer(value);
        int index = 0;
        int deletesNum = 0;
        while ((index = value.indexOf("''", index)) != -1) {
            buffer.deleteCharAt(index - deletesNum);
            index += 2;
            deletesNum++;
        }
        return buffer.toString();
    }

    public void setValue(String string) {
        value = string;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public String toString() {
        return "'" + value + "'";
    }

	public void appendTo(StringBuilder sb) {
		sb.append('\'').append(value).append('\'');
	}


	public ExpressionType getType() {
		return ExpressionType.value;
	}


	public Object formatNumber(BigDecimal negate) {
		throw new UnsupportedOperationException();
	}
}
