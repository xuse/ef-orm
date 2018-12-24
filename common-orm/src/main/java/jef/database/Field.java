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
package jef.database;

import java.io.Serializable;
import java.util.Collection;

import jef.database.Condition.Operator;
import jef.database.query.SqlExpression;

/**
 * 用于描述JEF元模型的借口 Field有以下实现种类 1、Enum型，即各个实体的元模型字段 2、FBIField 两个作用<BR>
 * (a)描述一个索引字段<BR>
 * (b)用来描述带函数的查询条件左表达式 3、IConditionField 用来描述一个完整的查询条件。有多种实现 4、RefField:
 * 包含一个实例和一个Field模型字段，用于描述在一个实体的Field中引用另一个实体的Field
 * 
 * @author Administrator
 *
 */
public interface Field extends Serializable {
	public String name();

	/**
	 * 等于
	 * 
	 * @param value
	 * @return
	 */
	default Condition eq(Object value) {
		return Condition.get(this, Operator.EQUALS, value);
	}

	/**
	 * 大于
	 * 
	 * @param value
	 * @return
	 */
	default Condition gt(Object value) {
		return Condition.get(this, Operator.GREAT, value);
	}

	/**
	 * 大于等于
	 * 
	 * @param value
	 * @return
	 */
	default Condition ge(Object value) {
		return Condition.get(this, Operator.GREAT_EQUALS, value);
	}

	/**
	 * 小于
	 * 
	 * @param value
	 * @return
	 */
	default Condition lt(Object value) {
		return Condition.get(this, Operator.LESS, value);
	}

	/**
	 * 小于等于
	 * 
	 * @param value
	 * @return
	 */
	default Condition le(Object value) {
		return Condition.get(this, Operator.LESS_EQUALS, value);
	}

	/**
	 * 不等于
	 * 
	 * @param value
	 * @return
	 */
	default Condition ne(Object value) {
		return Condition.get(this, Operator.NOT_EQUALS, value);
	}

	/**
	 * In条件
	 * 
	 * @param values
	 * @return
	 */
	default Condition in(Object[] values) {
		return Condition.get(this, Operator.IN, values);
	}

	/**
	 * In条件
	 * 
	 * @param values
	 * @return
	 */
	default Condition in(int[] values) {
		return Condition.get(this, Operator.IN, values);
	}

	/**
	 * In条件
	 * 
	 * @param values
	 * @return
	 */
	default Condition in(long[] values) {
		return Condition.get(this, Operator.IN, values);
	}

	/**
	 * In条件
	 * 
	 * @param values
	 * @return
	 */
	default Condition in(Collection<?> values) {
		return Condition.get(this, Operator.IN, values);
	}

	/**
	 * Not In条件
	 * 
	 * @param field
	 * @param values
	 * @return
	 */
	public static Condition notin(Field field, Object[] values) {
		return Condition.get(field, Operator.NOT_IN, values);
	}

	/**
	 * isnull条件
	 * 
	 * @return
	 */
	default Condition isNull() {
		return Condition.get(this, Operator.IS_NULL, null);
	}

	/**
	 * Notnull条件
	 * 
	 * @return
	 */
	default Condition isNotNull() {
		return Condition.get(this, Operator.IS_NOT_NULL, null);
	}

	/**
	 * 生成Between条件
	 * 
	 * @param begin
	 * @param end
	 * @return
	 */
	default <T extends Comparable<T>> Condition between(T begin, T end) {
		return Condition.get(this, Operator.BETWEEN_L_L, new Object[] { begin, end });
	}

	/**
	 * 产生MatchEnd条件，
	 * 
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like '%str' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	default Condition matchEnd(String str) {
		return Condition.get(this, Operator.MATCH_END, str);
	}

	/**
	 * 产生MatchStart条件
	 * 
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like 'str%' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	default Condition matchStart(String str) {
		return Condition.get(this, Operator.MATCH_START, str);
	}

	/**
	 * 得到一个Like %str%的条件
	 * 
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like '%str%' } 这样的条件，str中原来的的'%' '_'符号会被转义
	 */
	default Condition matchAny(String str) {
		return Condition.get(this, Operator.MATCH_ANY, str);
	}

	/**
	 * 得到一个Like条件，参数为自定义的字符串。 例如
	 * <p>
	 * {@code like(field, "%123_456%")}
	 * <p>
	 * 相当于
	 * <p>
	 * {@code WHERE field LIKE '%123_456%'  }
	 * <p>
	 * 
	 * <h3>注意</h3> 这个方法可以自由定义复杂的匹配模板外，但是和matchxxx系列的方法相比，不会对字符串中的'%'
	 * '_'做转义。因此实际使用不当会有SQL注入风险。
	 * <p>
	 * 
	 * @param field
	 *            field 表的字段（也可以是函数表达式）
	 * @param str
	 *            要匹配的字符串
	 * @return 产生形如 {@code field like 'str' } 这样的条件，str中原来的的'%' '_'符号会被保留
	 */
	default Condition like(String str) {
		return Condition.get(this, Operator.MATCH_ANY, new SqlExpression(str));
	}

	default int asEnumOrdinal() {
		if (this instanceof Enum) {
			return ((Enum<?>) this).ordinal();
		} else {
			return -1;
		}
	}

}
