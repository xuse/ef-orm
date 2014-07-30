package jef.database.query;

import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.meta.IReferenceAllTable;
import jef.database.meta.IReferenceColumn;
import jef.database.meta.Reference;
import jef.http.client.support.CommentEntry;

/**
 * 供一个select字句发挥作用、以及描述该部分字段如何装配到对象
 * 对应复杂查询中的一个表定义的所有查询字段。
 * 
 * 包含以下信息： 
 * 1、表定义别名
 * 2、选择的列名称与别名
 * 3、选择列别名到装配目标
 * @Date 2011-6-17
 */
public interface ISelectItemProvider {
	/**
	 * 获得Schema
	 * @return
	 */
	String getSchema();
	
	/**
	 * 获得引用字段列表和目标实体的字段(从拼装目的出发)
	 * 每个IReferenceField描述一个位于某路径的字段（即拼装路径），
	 * 以及该数据来自当前查询实例的字段名（是否需要加上schema待定）
	 * 
	 * 简单来说，就是： 选择列名（即别名(getName)） -> 实体属性的相对路径
	 * 包含两种模式：
	 *   单列 
	 *   全部列
	 * FIXME 应该拆分为两个方法，分别返回两类引用  
	 * @return 从拼装目的来描述的列选择，如果为null，表示装配到基本对象上
	 */
	IReferenceAllTable getReferenceObj();
	
	List<IReferenceColumn> getReferenceCol();
	
	/**
	 * 获得要选择的列和别名(拼装目的不考虑)
	 * 用于生成select字句
	 */
	CommentEntry[] getSelectColumns(DatabaseDialect profile,boolean groupMode,SqlContext context);
	
	Query<?> getTableDef();
	
	void addField(IReferenceAllTable field);
	void addField(IReferenceColumn field);
	
	void setFields(IReferenceAllTable all,IReferenceColumn... ref);
	void setFields(IReferenceAllTable all,List<IReferenceColumn> reference);
	
	
	boolean isAllTableColumns();
	ISelectItemProvider copyOf(List<IReferenceColumn> fields,IReferenceAllTable allCols);
	public Reference getStaticRef();
}
