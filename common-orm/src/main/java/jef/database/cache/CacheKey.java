package jef.database.cache;

import java.io.Serializable;
import java.util.List;


/**
 * Cache Key
 * @author jiyi
 *
 */
public interface CacheKey extends Serializable{
	
	/**
	 * 缓存，得到该缓存操作存储空间
	 * 即tableDef所约定的空间
	 * @return
	 */
	String getStoreSpace();
	/**
	 * 缓存，得到该缓存操作会收到以下空间的影响
	 * @return
	 */
	List<String> getAffectedKey();
	/**
	 * 记录影响维度，即一个n维向量空间
	 * @return
	 */
	KeyDimension getDimension();
	/**
	 * 维度内的参数（坐标）
	 * @return
	 */
	List<?> getParams();
}
