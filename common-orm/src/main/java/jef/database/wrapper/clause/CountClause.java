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
package jef.database.wrapper.clause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class CountClause implements SqlClause{
	private Map<String,List<BindSql>> sqls=new LinkedHashMap<String,List<BindSql>>();
	
	public void addSql(String dbName,String... sql){
		List<BindSql> list=sqls.get(dbName);
		if(list==null){
			list=new ArrayList<BindSql>();
			sqls.put(dbName, list);
		}
		for(String s:sql){
			list.add(new BindSql(s));
		}
	}
	
	
	public void addSql(String dbName,List<BindSql> value){
		List<BindSql> list=sqls.get(dbName);
		if(list==null){
			list=new ArrayList<BindSql>();
			sqls.put(dbName, list);
		}
		list.addAll(value);
	}
	
	public void addSql(String dbName,BindSql... sqls){
		addSql(dbName,Arrays.asList(sqls));
	}

	public Map<String,List<BindSql>> getSqls() {
		return sqls;
	}
}
