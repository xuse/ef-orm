package jef.database.query;

import jef.database.meta.ITableMetadata;

/**
 * 捆绑到某个类型的查询
 * @Company: Asiainfo-Linkage Technologies(China),Inc.  Hangzhou
 * @author Administrator
 * @Date 2011-6-16 
 * @param <T>
 */
public interface TypedQuery<T> extends ConditionQuery{
	/**
	 * 得到实例类型
	 * @return
	 */
	Class<T> getType();
	
	/**
	 * 因为动态表支持的需要，今后要逐渐用这个方法代替上个方法
	 * @return
	 */
	ITableMetadata getMeta();
}
