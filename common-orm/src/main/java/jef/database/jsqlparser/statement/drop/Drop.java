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
package jef.database.jsqlparser.statement.drop;

import java.util.List;

import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;
import jef.tools.StringUtils;

public class Drop implements Statement {

    private String type;

    private String name;

    private List<String> parameters;

    public void accept(StatementVisitor statementVisitor) {
        statementVisitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getType() {
        return type;
    }

    public void setName(String string) {
        name = string;
    }

    public void setParameters(List<String> list) {
        parameters = list;
    }

    public void setType(String string) {
        type = string;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(128);
    	sb.append("DROP " ).append(type).append(' ').append(name);
        if (parameters != null && parameters.size() > 0) {
        	sb.append(' ');
        	StringUtils.joinTo(parameters, ",", sb);
//            PlainSelect.getStringList(sb,parameters,",",false);
        }
        return sb.toString();
    }
}
