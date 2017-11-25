package jef.database.meta.object;

import javax.persistence.Column;

import jef.database.DbMetaData.ObjectType;

/**
 * 描述数据库中的Function（函数) / procedure(存储过程)对象
 * 
 * 
 * @author jiyi
 * 
 */
public class Function {
	private ObjectType objectType;

	@Column(name = "FUNCTION_CAT")
	private String catalog;
	@Column(name = "FUNCTION_SCHEM")
	private String schema;
	@Column(name = "FUNCTION_NAME")
	private String name;
	@Column(name = "REMARKS")
	private String remarks;

	public Function(ObjectType objType) {
		this.objectType = objType;
	}

	public Function() {
		this(ObjectType.FUNCTION);
	}

	/**
	 * short => kind of function: functionResultUnknown - Cannot determine if a
	 * return value or table will be returned functionNoTable- Does not return a
	 * table functionReturnsTable - Returns a table
	 */
	@Column(name = "FUNCTION_TYPE")
	private int type;

	@Column(name = "SPECIFIC_NAME")
	private String specificName;

	/**
	 * 获取catalog
	 * @return 所在catalog
	 */
	public String getCatalog() {
		return catalog;
	}
	/**
	 * 设置catalog
	 * @param catalog
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}
	/**
	 * 获取schema
	 * @return 所在schema
	 */
	public String getSchema() {
		return schema;
	}
	/**
	 * 设置schema
	 * @param schema
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}
	/**
	 * get the functuon/ procedure name
	 * @return name
	 */
	public String getName() {
		return name;
	}
	/**
	 * set the functuon/procedure name
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取备注
	 * @return 备注
	 */
	public String getRemarks() {
		return remarks;
	}
	/**
	 * 设置备注
	 * @param remarks
	 */
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	/**
	 * 获取类型
	 * @return type
	 */
	public int getType() {
		return type;
	}
	/**
	 * 设置类型
	 * @param type
	 */
	public void setType(int type) {
		this.type = type;
	}
	/**
	 * 获取specificName
	 * @return specificName
	 */
	public String getSpecificName() {
		return specificName;
	}
	/**
	 * 设置specificName
	 * @param specificName
	 */
	public void setSpecificName(String specificName) {
		this.specificName = specificName;
	}

	@Override
	public String toString() {
		return schema + "." + name;
	}
	/**
	 * 返回对象类型
	 * @return 下列之一<ul>
	 * <li>{@link ObjectType#PROCEDURE} 存储过程</li>
	 * <li>{@link ObjectType#FUNCTION} 函数</li></ul>
	 * @see ObjectType
	 */
	public ObjectType getObjectType() {
		return objectType;
	}
}
