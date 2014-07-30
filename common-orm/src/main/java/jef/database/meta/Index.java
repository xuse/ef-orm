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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 数据库中查到的索引信息
 * @author Administrator
 *
 */
public class Index {
	private String tableName;
	private String indexName;
	private String[] columnName; 
	private boolean unique;
	private boolean orderAsc;
	private int type;
	
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append(indexName).append(" on ").append(tableName);
		sb.append("(").append(StringUtils.join(columnName,",")).append(")");
		if(unique)sb.append(" unique");
		if(!orderAsc)sb.append(" desc");
		return sb.toString();
	}
	/**
	 * 索引类型，有以下几类
	 * <UL>
     *      <LI> tableIndexStatistic(0) - this identifies table statistics that are
     *           returned in conjuction with a table's index descriptions
     *      <LI> tableIndexClustered(1) - this is a clustered index
     *      <LI> tableIndexHashed   (2) - this is a hashed index
     *      <LI> tableIndexOther    (3) - this is some other style of index
     * </UL>
	 * @return 索引类型
	 */
	public int getType() {
		return type;
	}
	/**
	 * 设置索引类型
	 * @param type 索引类型
	 */
	public void setType(int type) {
		this.type = type;
	}
	/**
	 * 得到索引中的所有列
	 * @return 所有列名
	 */
	public String[] getColumnName() {
		return columnName;
	}
	/**
	 * 添加一个列 
	 * @param column 列名称
	 */
	public void addColumnName(String column) {
		if(columnName==null || columnName.length==0){
			columnName = new String[]{column};
		}else{
			columnName = (String[]) ArrayUtils.add(columnName,column);
		}
	}
	/**
	 * 设置索引中的列
	 * @param columnName 索引中的列
	 */
	public void setColumnName(String[] columnName) {
		this.columnName = columnName;
	}
	/**
	 * 获得索引名称
	 * @return 索引名称
	 */
	public String getIndexName() {
		return indexName;
	}
	/**
	 * 设置索引名称
	 * @param indexName 索引名称
	 */
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
	/**
	 * 该索引是否唯一约束
	 * @return 如有唯一约束返回true
	 */
	public boolean isUnique() {
		return unique;
	}
	/**
	 * 设置该索引是否有唯一约束
	 * @param unique 是否有唯一约束
	 */
	public void setUnique(boolean unique) {
		this.unique = unique;
	}
	/**
	 * 该索引是否为正序索引
	 * @return true为正序，false为倒序
	 */
	public boolean isOrderAsc() {
		return orderAsc;
	}
	/**
	 * 设置该索引是否为正序索引
	 * @param orderAsc 正序为true
	 */
	public void setOrderAsc(boolean orderAsc) {
		this.orderAsc = orderAsc;
	}
	/**
	 * 获得索引所在的表名
	 * @return 索引所在的表名
	 */ 
	public String getTableName() {
		return tableName;
	}
	/**
	 * 设置索引所在的表名
	 * @param tableName 索引所在的表名
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
}
