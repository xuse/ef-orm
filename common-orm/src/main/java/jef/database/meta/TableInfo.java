package jef.database.meta;

import jef.tools.StringUtils;

/**
 * 一张数据库表或视图。<br/>
 * Indicates a table/view in database.
 * @author xuse
 *
 */
public class TableInfo {
	private String catalog;
	private String schema;
	private String name;
	private String remarks;
	private String type;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotEmpty(schema)) {
			sb.append(schema).append('.');
		}
		sb.append(name);
		if (StringUtils.isNotEmpty(remarks)) {
			sb.append(':').append(remarks);
		}
		return sb.toString();
	}

	/**
	 * 数据库表所属catalog
	 * 
	 * @return catalog
	 */
	public String getCatalog() {
		return catalog;
	}

	/**
	 * 设置Catalog
	 * 
	 * @param catalog
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/**
	 * 获得表所在schema
	 * 
	 * @return the schema of table
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * 设置 schema
	 * 
	 * @param schema
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

	/**
	 * 获得表/视图(等)的名称
	 * 
	 * @return 名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置名称
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获得表的备注信息
	 * 
	 * @return 备注
	 */
	public String getRemarks() {
		return remarks;
	}

	/**
	 * 设置备注
	 * 
	 * @param remarks
	 *            备注
	 */
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	/**
	 * 获得表的类型
	 * 
	 * @return 类型
	 */
	public String getType() {
		return type;
	}

	/**
	 * 设置表类型
	 * 
	 * @param type
	 *            类型
	 */
	public void setType(String type) {
		this.type = type;
	}
}