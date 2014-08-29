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
package jef.database.meta;

import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.query.JpqlExpression;
import jef.database.query.Query;

/**
 * 为了向Table metadata当中添加一个FBI索引描述时使用。(也可以昨为带有函数的Where语句查询条件)
 * 例如：
 *    meta.putIndex(new FBIField("upper(name)||upper(id_card)").index(), "");
 * 当JEF发现一个FBIField 时，就会将其中的内容作为索引。
 * 注意：并非所有的数据库都支持FBI索引，仅Oracle,SQLServer等数据库支持。
 */
public class FBIField extends JpqlExpression implements Field{
	private static final long serialVersionUID = -3274832755965207227L;
	/**
	 * 构造
	 * @param str 表达式
	 * @param query 绑定表实例
	 */
	public FBIField(String str,Query<?> query){
		super(str,query);
	}
	
	/**
	 * 构造
	 * @param str 表达式
	 */
	public FBIField(String str){
		super(str);
	}

	public FBIField(String str, IQueryableEntity p) {
		super(str,p.getQuery());
	}

	public FBIField(Expression exp, Query<?> q) {
		super(exp,q);
	}

	public Field[] index(){
		return new Field[]{this};
	}
	
	public String name() {
		return super.toString();
	}
}
