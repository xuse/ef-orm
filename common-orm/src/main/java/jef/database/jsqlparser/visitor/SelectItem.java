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
package jef.database.jsqlparser.visitor;

import jef.database.jsqlparser.statement.SqlAppendable;

/**
 * Anything between "SELECT" and "FROM"<BR>
 * (that is, any column or expression etc to be retrieved with the query)
 */
public interface SelectItem extends SqlAppendable{

    public void accept(SelectItemVisitor selectItemVisitor);
    
    /**
     * 返回查询表达式。如果是AllTable * /t.*等格式则返回null
     * @return
     */
    public Expression getExpression();

	/**
	 * 拼入对象
	 * @param sb
	 * @param noGroupFunc
	 */
	public void appendTo(StringBuilder sb,boolean noGroupFunc);
}
