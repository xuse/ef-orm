package jef.database.wrapper.populator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.database.Session;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.EntityMappingProvider;

public interface ResultSetPopulator {
	/**
	 * 拼装成简单对象
	 * 
	 * @param rs
	 * @param clz
	 * @return
	 */
	public <T> List<T> toSimpleObjects(IResultSet rs, Class<T> clz);

	/**
	 * 简单对象列举
	 * 
	 * @param rs
	 * @param clz
	 * @return
	 */
	public <T> Iterator<T> iteratorSimple(IResultSet rs, Class<T> clz);

	/**
	 * 查询结果的包装
	 * 
	 * @param rs
	 *            ResultSet对象
	 * @param obj
	 *            要封装成的对象的实例
	 * @param j
	 *            连接对象
	 * @return
	 */
	public <T> List<T> toJavaObject(Session session,IResultSet rs, EntityMappingProvider j, Transformer transformers);

	/**
	 * 标准拼装列举
	 * 
	 * @param rs
	 * @param clz
	 * @param j
	 * @return
	 */
	public <T> Iterator<T> iteratorNormal(Session session,IResultSet rs, EntityMappingProvider j, Transformer transformers);
	

	/**
	 * 拼装成多个数据对象
	 * 
	 * @param rs
	 * @param j
	 * @return
	 */
	public List<Object[]> toDataObjectMap(IResultSet rs, EntityMappingProvider j,Transformer transformers);

	/**
	 * 多对象拼装列举
	 * 
	 * @param rs
	 * @param j
	 * @return
	 */
	public Iterator<Object[]> iteratorMultipie(IResultSet rs, EntityMappingProvider j,Transformer transformers);

	/**
	 * 平面模式拼装
	 * 
	 * @param <T>
	 * @param rs
	 * @param clz
	 * @param strategy
	 * @return
	 */
	public <T> List<T> toPlainJavaObject(IResultSet rs, Transformer transformers);

	/**
	 * 平面模式列举
	 * 
	 * @param rs
	 * @param clz
	 * @param strategy
	 * @return
	 */
	public <T> Iterator<T> iteratorPlain(IResultSet rs, Transformer transformers);
	
	/**
	 * Map模式例举
	 * @param rs
	 * @param strategy
	 * @return
	 */
	public Iterator<Map<String,Object>> iteratorMap(IResultSet rs, Transformer transformers);
	

	/**
	 * 转换为Map
	 * 
	 * @param rs
	 * @param strategy
	 * @return
	 */
	public List<Map<String,Object>> toVar(IResultSet rs,Transformer transformers);
	
}
