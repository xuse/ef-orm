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

/**
 * Every number with a point or a exponential format is a DoubleValue
 */
public class DoubleValue implements Expression {

    private double value;

    private String stringValue;

    public DoubleValue(String value) {
        if (value.charAt(0) == '+') {
            value = value.substring(1);
        }
        this.value = Double.parseDouble(value);
        this.stringValue = value;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double d) {
        value = d;
    }

    public String toString() {
        return stringValue;
    }

	public void appendTo(StringBuilder sb) {
		sb.append(stringValue);
		
	}
}
