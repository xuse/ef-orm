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
package jef.database.wrapper.populator;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.database.query.SqlContext;
import jef.tools.StringUtils;

public class ColumnMeta{
	ColumnDescription[] columns;
	/**
	 * 基于原始的NAME到备注的索引(KEY全大写)
	 */
	Map<String,ColumnDescription> nameIndex;
	/**
	 * 基于多个解析后的schema
	 */
	private Map<String,ColumnDescription[]> schemaIndex;
	
	private ResultSetMetaData meta;
	
	/**
	 *构造
	 * <p>Title: </p>
	 * <p>Description:</p>
	 * @param columnNames
	 */
	public ColumnMeta(List<ColumnDescription> columnList,ResultSetMetaData meta) {
		this.meta=meta;
		this.columns=columnList.toArray(new ColumnDescription[columnList.size()]);
		initName();
	}
	
	private void initName(){
		nameIndex=new HashMap<String,ColumnDescription>(16,0.6f);
		for(ColumnDescription c: columns){
			nameIndex.put(c.getName().toUpperCase(), c);
		}
	}
	
	/**
	 * 按序号返回ColumnDescription
	 * @param n
	 * @return
	 */
	public ColumnDescription getN(int n){
		return columns[n];
	}

	//初始化Schema
	public void initSchemas(Transformer transformers){
		if(schemaIndex!=null)return;
		transformers.prepareTransform(nameIndex);//注意这个方法必须在ignoreSchema操作之前进行计算，否则会造成自定义Mapper找不到需要的列。
		
		Map<String,List<ColumnDescription>> main=new HashMap<String,List<ColumnDescription>>();
		for(ColumnDescription c:columns){
			String s=c.getName();
			if(transformers.hasIgnoreColumn(s.toUpperCase())){
				continue;
			}
			int n=s.indexOf(SqlContext.DIVEDER);
			String schema=(n>-1)?s.substring(0,n):"";
			if(transformers.hasIgnoreSchema(schema.toUpperCase())){
				continue;
			}
			
			c.setSimpleName((n>-1)?s.substring(n+SqlContext.DIVEDER.length()):s);
			List<ColumnDescription> list=main.get(schema);
			if(list==null){
				list=new ArrayList<ColumnDescription>();
				list.add(c);
				main.put(schema, list);
			}else{
				list.add(c);
			}
		}
		schemaIndex=new HashMap<String,ColumnDescription[]>();
		for(String key: main.keySet()){
			List<ColumnDescription> list=main.get(key);
			schemaIndex.put(key, list.toArray(new ColumnDescription[list.size()]));
		}
	}
	
	
	public ColumnDescription[] getColumns(String schema){
		return schemaIndex.get(schema);
	}

	public ColumnDescription getByFullName(String fieldName){
		return this.nameIndex.get(fieldName.toUpperCase());
	}
	
	public Set<String> getSchemas(){
		return schemaIndex.keySet();
	}
	
	@Override
	public String toString() {
		if(this.schemaIndex==null){
			StringBuilder sb=new StringBuilder();
			for(ColumnDescription c: this.columns){
				sb.append(c.getName()).append(',');
			}
			sb.setLength(sb.length()-1);
			return sb.toString();
		}else{
			StringBuilder sb=new StringBuilder();
			for(String key:schemaIndex.keySet()){
				sb.append(key).append(":{").append(StringUtils.join(schemaIndex.get(key), ",")).append('}');
				sb.append('\n');
			}
			return sb.toString();	
		}
	}
	
	public ColumnDescription[] getColumns() {
		return columns;
	}

	public int length() {
		return columns.length;
	}

	public ResultSetMetaData getMeta() {
		return meta;
	}
}
