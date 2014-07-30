package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.Session;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.DdlGenerator;
import jef.database.meta.ITableMetadata;
import jef.database.query.EntityMappingProvider;
import jef.database.support.MetadataEventListener;
import jef.database.wrapper.IResultSet;
import jef.database.wrapper.Transformer;

public interface MetadataService {
	public static final Class<?>[] SIMPLE_CLASSES = new Class<?>[] { String.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class, Date.class ,Object.class};

	/**
	 * 得到指定主键的metadata
	 * @param dbkey
	 * @return
	 */
	DbMetaData getMetadata(String dbkey);
	
	/**
	 * 得到数据简要表
	 * @return
	 */
	DatabaseDialect getProfile(String dbkey);
	
	/**
	 * 得到基本信息
	 * @return
	 */
	ConnectInfo getInfo(String dbkey);

	/**
	 * get the SQL generator..
	 * @return
	 */
	DdlGenerator getDdlGenerator(String dbkey);
	
	/**
	 * 是否具有remark才能获得元数据注解这个特性
	 * @param dbkey
	 * @return
	 */
	boolean hasRemarkFeature(String dbkey);
	
	/**
	 * 
	 * @param dbKey
	 * @return
	 */
	PartitionSupport getPartitionSupport();
	
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
	
	/**
	 * 刷新表
	 * @param meta
	 * @param listener
	 * @throws SQLException
	 */
	void tableRefresh(ITableMetadata meta,MetadataEventListener listener) throws SQLException;
}
