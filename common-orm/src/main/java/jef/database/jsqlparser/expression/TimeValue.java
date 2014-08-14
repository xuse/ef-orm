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
import java.sql.Time;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.SqlValue;

/**
 * A Time in the form {t 'hh:mm:ss'}
 */
public class TimeValue implements Expression,SqlValue {

    private Time value;

    public TimeValue(String value) {
        this.value = Time.valueOf(value.substring(1, value.length() - 1));
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public Time getValue() {
        return value;
    }

    public void setValue(Time d) {
        value = d;
    }

    public String toString() {
        return "{t '" + value + "'}";
    }

	public void appendTo(StringBuilder sb) {
		sb.append("{t '").append(value).append("'}");
	}
	
	public ExpressionType getType() {
		return ExpressionType.value;
	}

	public Object formatNumber(BigDecimal negate) {
		throw new UnsupportedOperationException();
	}
}
