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
import java.util.List;

import jef.database.Session;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.processor.AutoIncreatmentCallBack;

public class InsertSqlClause{
	private String columnsPart;
	private String valuesPart;
	private PartitionResult table;
	private AutoIncreatmentCallBack callback;
	final List<ColumnMapping> fields;
	private String insert="insert into ";
	private String tailer="";
	public Session parent;
	public DatabaseDialect  profile;
	private boolean extreme;
	/**
	 * 描述应该在何时调用Callback.
	 * true:在插入完成后，调用Callback来获取数据库生成的主键
	 * false:在插入完成前，就调用Callback在提前生成主键
	 */
	public InsertSqlClause(){
		fields=null;
	}
	public InsertSqlClause(boolean extreme){
		fields=new ArrayList<ColumnMapping>();
		this.extreme=extreme;
	}

	/**
	 * 传入表名并返回SQL
	 * @param tablename
	 * @return
	 */
	public String getSql(String tablename) {
		StringBuilder sb = fields==null?new StringBuilder():new StringBuilder(fields.size()*8+32);
		sb.append(insert).append(tablename);
		sb.append("(").append(columnsPart).append(") values(");
		sb.append(valuesPart).append(")");
		sb.append(tailer);
		return sb.toString();
	}
	
	public String getSql() {
		return getSql(table.getAsOneTable());
	}
	public String getColumnsPart() {
		return columnsPart;
	}
	public void setColumnsPart(String columnsPart) {
		this.columnsPart = columnsPart;
	}
	public String getValuesPart() {
		return valuesPart;
	}
	public void setValuesPart(String valuesPart) {
		this.valuesPart = valuesPart;
	}
	public PartitionResult getTable() {
		return table;
	}
	public void setTableNames(PartitionResult tableName) {
		this.table = tableName;
	}
	public AutoIncreatmentCallBack getCallback() {
		return callback;
	}
	public void setCallback(AutoIncreatmentCallBack callback) {
		this.callback = callback;
	}
	public void addField(ColumnMapping field) {
		fields.add(field);
	}
	public List<ColumnMapping> getFields() {
		return fields;
	}
	public boolean isForPrepare(){
		return fields!=null;
	}
	public boolean isExtreme() {
		return extreme;
	}
	public String getInsert() {
		return insert;
	}
	public void setInsert(String insert) {
		this.insert = insert;
	}
	public String getTailer() {
		return tailer;
	}
	public void setTailer(String tailer) {
		this.tailer = tailer;
	}
}
