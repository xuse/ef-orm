package jef.database.wrapper;

import java.sql.SQLException;
import java.util.Map;

import jef.database.wrapper.result.IResultSet;
import jef.tools.reflect.BeanWrapper;

/**
 * 让用户可以自己覆盖实现的Transformer
 * @author jiyi
 *
 * @param <T>
 */
public abstract class Mapper<T> implements IPopulator{
	@SuppressWarnings("unchecked")
	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
		transform((T)wrapper.getWrapped(),rs);
	}
	
	/**
	 * 可以被覆盖，准备填充策略
	 * @param nameIndex
	 */
	protected void prepare(Map<String, ColumnDescription> nameIndex){
	}
	
	/**
	 * 需要被覆盖，用户自定义从ResultSet中获取数据填充到结果对象中
	 * @param obj
	 * @param rs
	 */
	protected abstract void transform(T obj, IResultSet rs)throws SQLException ;
}
