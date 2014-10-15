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
 * Every number without a point or an exponential format is a LongValue
 */
public class LongValue implements Expression,SqlValue {

    private long value;

    private String stringValue;
    
    public LongValue(){
    }
    
    public LongValue(String value) {
        if (value.charAt(0) == '+') {
            value = value.substring(1);
        }
        this.value = Long.parseLong(value);
        this.stringValue = value;
    }
    
    public LongValue(long value){
    	this.value=value;
    	this.stringValue=String.valueOf(value);
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public Long getValue() {
        return value;
    }

    public void setValue(long d) {
        value = d;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public String toString() {
        return stringValue;
    }

    public static final LongValue L0=new LongValue(0);
    public static final LongValue L1=new LongValue(1);
    
	public void appendTo(StringBuilder sb) {
		sb.append(stringValue);
	}
	
	public ExpressionType getType() {
		return ExpressionType.value;
	}

	public Object formatNumber(BigDecimal negate) {
		return negate.longValue();
	}
}

