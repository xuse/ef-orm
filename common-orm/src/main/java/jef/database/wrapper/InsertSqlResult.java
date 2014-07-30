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
package jef.database.wrapper;

import java.util.ArrayList;
import java.util.List;

import jef.database.AutoIncreatmentCallBack;
import jef.database.Field;
import jef.database.Session;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;

public class InsertSqlResult{
	private String columnsPart;
	private String valuesPart;
	private PartitionResult tableNames;
	private AutoIncreatmentCallBack callback;
	final List<Field> fields;
	
	public Session parent;
	public DatabaseDialect  profile;
	private boolean forBatch;
	/**
	 * 描述应该在何时调用Callback.
	 * true:在插入完成后，调用Callback来获取数据库生成的主键
	 * false:在插入完成前，就调用Callback在提前生成主键
	 */
	public InsertSqlResult(){
		fields=null;
	}
	public InsertSqlResult(boolean isBatch){
		fields=new ArrayList<Field>();
		forBatch=isBatch;
	}

	public String getSql(String tablename) {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tablename);
		sb.append("(").append(columnsPart).append(") values(");
		sb.append(valuesPart).append(")");
		return sb.toString();
	}
	
	public String getSql() {
		String tableName=tableNames.getAsOneTable();
		return getSql(tableName);
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
	public PartitionResult getTableNames() {
		return tableNames;
	}
	public void setTableNames(PartitionResult tableName) {
		this.tableNames = tableName;
	}
	public AutoIncreatmentCallBack getCallback() {
		return callback;
	}
	public void setCallback(AutoIncreatmentCallBack callback) {
		this.callback = callback;
	}
	
	public void addField(Field field) {
		fields.add(field);
	}
	public List<Field> getFields() {
		return fields;
	}
	public boolean isForPrepare(){
		return fields!=null;
	}
	public boolean isForBatch() {
		return forBatch;
	}
}
