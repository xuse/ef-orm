package jef.database.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.Field;

/**
 * 可以作为连接元素的查询对象 (PlainSelect)
 * @author Administrator
 * @Date 2011-6-16
 * @see Query
 * @see Join
 */
public interface JoinElement extends ConditionQuery{
	/**
	 * 获取现有全部条件
	 * @return 查询请求中的全部条件。
	 */
	List<Condition> getConditions();
	
	/**
	 * 返回查询请求中的Select项，平台拼SQL语句时需要用到。
	 * @return 查询请求中的所有Select项
	 */
	EntityMappingProvider getSelectItems();

	/**
	 * 设置平查询请求中的Select项。
	 * @param select 选择项
	 */
	void setSelectItems(Selects select);

	/**
	 * 清除全部的请求数据
	 */
	void clearQuery();

	/**
	 * 获取现有排序字段
	 * 
	 * @return
	 */
	Collection<OrderField> getOrderBy();
	/**
	 * 设置排序
	 * @param asc true is ASC, false is DESC
	 * @param orderby 要排序的Field对象
	 */
	void setOrderBy(boolean asc,Field... orderby);
	
	/**
	 * 添加排序
	 * @param asc  true is ASC, false is DESC
	 * @param orderby 要排序的field对象。
	 */
	void addOrderBy(boolean asc,Field... orderby);
	
	/**
	 * 添加排序 , 正序字段<br>
	 * 等效于调用 {@link #addOrderBy(boolean, Field...)}且第一个参数为true。
	 * @param ascFields 要正序的字段
	 * @return 当前Query对象本身
	 */
	JoinElement orderByAsc(Field... ascFields);
	
	/**
	 * 添加排序, 倒序字段<br>
	 * 等效于调用 {@link #addOrderBy(boolean, Field...)}且第一个参数为false。
	 * @param descFields 要倒序的字段
	 * @return 当前Query对象本身
	 */
	JoinElement orderByDesc(Field... descFields);
	
	/**
	 * 设置属性
	 * 属性一般有以下作用：
	 * <li>用于填充表达式条件</li>
	 * <li>用于填充Join过滤条件</li>
	 * @param key
	 * @param value
	 */
	void setAttribute(String key,Object value);
	/**
	 * 获取属性
	 * @param key 属性名
	 * @return
	 */
	Object getAttribute(String key);
	/**
	 * 得到所有属性
	 * @return 所有属性
	 */
	Map<String,Object> getAttributes();
}
