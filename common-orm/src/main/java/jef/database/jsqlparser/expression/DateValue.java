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
import java.sql.Date;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.SqlValue;

/**
 * A Date in the form {d 'yyyy-mm-dd'}
 */
public class DateValue implements Expression,SqlValue {

    private Date value;
    private String str;

    public DateValue(String value) {
    	this.str=value;
        this.value = Date.valueOf(value.substring(1, value.length() - 1));
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public Date getValue() {
        return value;
    }

    public void setValue(Date d) {
        value = d;
    }
    
	@Override
	public String toString() {
		return "{d "+str+"}";
	}

	public void appendTo(StringBuilder sb) {
		sb.append("{d ").append(str).append('}');
	}

	public ExpressionType getType() {
		return ExpressionType.value;
	}

	public Object formatNumber(BigDecimal negate) {
		throw new UnsupportedOperationException();
	}
	
	
}
