package com.github.geequery.codegen;

import jef.database.dialect.ColumnType;
import jef.database.meta.Column;

import com.github.geequery.codegen.ast.JavaField;
import com.github.geequery.codegen.ast.JavaUnit;

/**
 * 用于监测代码生成过程的回调
 * @author jiyi
 *
 */
public interface EntityProcessorCallback {
	/**
	 * 要生成的实体总数
	 * @param n
	 */
	void setTotal(int n);
	/**
	 * 初始化
	 * @param meta
	 * @param tablename
	 * @param tableComment
	 * @param schema
	 * @param java
	 */
	void init(Metadata meta, String tablename, String tableComment, String schema,JavaUnit java);
	
	/**
	 * 添加Field时
	 * @param java
	 * @param field
	 * @param c
	 * @param columnType
	 */
	void addField(JavaUnit java, JavaField field, Column c, ColumnType columnType);
	
	
	/**
	 * 将列名转换为字段名
	 * @param columnName
	 * @return
	 */
	String columnToField(String columnName);
	
	/**
	 * 完成时
	 * @param java
	 */
	void finish(JavaUnit java);
}
