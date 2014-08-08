package jef.database.query;

import java.io.Serializable;
import java.util.List;

import jef.common.Entry;

/**
 * 一个查询字段映射到对象层级结构的数据模型。框架内部使用。
 * @author Administrator
 *
 */
public interface EntityMappingProvider extends Serializable{
	/**
	 * 获取映射模型当中的所有查询。框架内部使用。
	 * @return
	 */
	List<ISelectItemProvider> getReference();

	/**
	 * 获取映射模型。框架内部使用。
	 * @return
	 */
	Entry<String[],ISelectItemProvider[]> getPopulationDesc();
	
	/**
	 * 是否为多表查询
	 * @return
	 */
	public boolean isMultiTable();
}
