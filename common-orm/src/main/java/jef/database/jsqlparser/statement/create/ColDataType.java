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
package jef.database.jsqlparser.statement.create;

import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.tools.StringUtils;

public class ColDataType implements SqlAppendable{

    private String dataType;

    private List<String> argumentsStringList;

    public List<String> getArgumentsStringList() {
        return argumentsStringList;
    }

    public String getDataType() {
        return dataType;
    }

    public void setArgumentsStringList(List<String> list) {
        argumentsStringList = list;
    }

    public void setDataType(String string) {
        dataType = string;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append(dataType);
    	if(argumentsStringList!=null && argumentsStringList.size()>0){
    		sb.append('(');
    		StringUtils.joinTo(argumentsStringList, ",", sb);
    		sb.append(')');
    	}
	}
}
