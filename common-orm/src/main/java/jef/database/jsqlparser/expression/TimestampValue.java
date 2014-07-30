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

import java.sql.Timestamp;

/**
 * A Timestamp in the form {ts 'yyyy-mm-dd hh:mm:ss.f . . .'}
 */
public class TimestampValue implements Expression {

    private Timestamp value;

    public TimestampValue(String value) {
        this.value = Timestamp.valueOf(value.substring(1, value.length() - 1));
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public Timestamp getValue() {
        return value;
    }

    public void setValue(Timestamp d) {
        value = d;
    }

    public String toString() {
        return "{ts '" + value + "'}";
    }

	public void appendTo(StringBuilder sb) {
		sb.append("{ts '").append(value).append("'}");
	}
}
