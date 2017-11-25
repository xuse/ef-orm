package jef.database.meta.object;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 描述数据库主键的信息
 * @author Administrator
 *
 */
public class PrimaryKey {
	/**
	 * 主键名
	 */
	String name;
	/**
	 * 列名
	 */
	String[] columns;
	
	public PrimaryKey(String pkName) {
		this.name=pkName;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String[] getColumns() {
		return columns;
	}
	public void setColumns(String[] columns) {
		this.columns = columns;
	}
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append(name);
		sb.append("(").append(StringUtils.join(columns)).append(")");
		return sb.toString(); 
	}
	public boolean hasColumn(String columnName) {
		return ArrayUtils.contains(columns, columnName);
	}
}
