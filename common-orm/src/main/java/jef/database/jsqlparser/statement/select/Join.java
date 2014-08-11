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
package jef.database.jsqlparser.statement.select;

import java.util.List;

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;

/**
 * A join clause
 */
public class Join implements SqlAppendable{

	private boolean outer = false;

	private boolean right = false;

	private boolean left = false;

	private boolean natural = false;

	private boolean full = false;

	private boolean inner = false;

	private boolean simple = false;

	private FromItem rightItem;

	private Expression onExpression;

	private List<Column> usingColumns;

	/**
	 * Whether is a tab1,tab2 join
	 * 
	 * @return true if is a "tab1,tab2" join
	 */
	public boolean isSimple() {
		return simple;
	}

	public void setSimple(boolean b) {
		simple = b;
	}

	/**
	 * Whether is a "INNER" join
	 * 
	 * @return true if is a "INNER" join
	 */
	public boolean isInner() {
		return inner;
	}

	public void setInner(boolean b) {
		inner = b;
	}

	/**
	 * Whether is a "OUTER" join
	 * 
	 * @return true if is a "OUTER" join
	 */
	public boolean isOuter() {
		return outer;
	}

	public void setOuter(boolean b) {
		outer = b;
	}

	/**
	 * Whether is a "LEFT" join
	 * 
	 * @return true if is a "LEFT" join
	 */
	public boolean isLeft() {
		return left;
	}

	public void setLeft(boolean b) {
		left = b;
	}

	/**
	 * Whether is a "RIGHT" join
	 * 
	 * @return true if is a "RIGHT" join
	 */
	public boolean isRight() {
		return right;
	}

	public void setRight(boolean b) {
		right = b;
	}

	/**
	 * Whether is a "NATURAL" join
	 * 
	 * @return true if is a "NATURAL" join
	 */
	public boolean isNatural() {
		return natural;
	}

	public void setNatural(boolean b) {
		natural = b;
	}

	/**
	 * Whether is a "FULL" join
	 * 
	 * @return true if is a "FULL" join
	 */
	public boolean isFull() {
		return full;
	}

	public void setFull(boolean b) {
		full = b;
	}

	/**
	 * Returns the right item of the join
	 */
	public FromItem getRightItem() {
		return rightItem;
	}

	public void setRightItem(FromItem item) {
		rightItem = item;
	}

	/**
	 * Returns the "ON" expression (if any)
	 */
	public Expression getOnExpression() {
		return onExpression;
	}

	public void setOnExpression(Expression expression) {
		onExpression = expression;
	}

	/**
	 * Returns the "USING" list of {@link jef.database.jsqlparser.expression.Column}
	 * s (if any)
	 */
	public List<Column> getUsingColumns() {
		return usingColumns;
	}

	public void setUsingColumns(List<Column> list) {
		usingColumns = list;
	}

	public String toString() {
		StringBuilder sb=new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}
	
	public void appendTo(StringBuilder sb){
		if(this.simple){
			rightItem.appendTo(sb);
		}else{
			if (isRight())
				sb.append("RIGHT ");
			else if (isNatural())
				sb.append("NATURAL ");
			else if (isFull())
				sb.append("FULL ");
			else if (isLeft())
				sb.append("LEFT ");
			if (isOuter())
				sb.append("OUTER ");
			else if (isInner())
				sb.append("INNER ");
			sb.append("JOIN ");
			rightItem.appendTo(sb);
			if(onExpression != null){
				sb.append(" ON ");
				onExpression.appendTo(sb);
			}
			PlainSelect.getFormatedList(sb,usingColumns, " using", true);
		}
	}
	
}
