package jef.database.cache;

import java.io.Serializable;
import java.util.List;


/**
 * 
 * @author jiyi
 *
 */
public interface CacheKey extends Serializable{
	/**
	 * 缓存表名，以实体的类名表示
	 * @return
	 */
	String getTable();
	/**
	 * 记录影响维度
	 * @return
	 */
	KeyDimension getDimension();
	/**
	 * 维度内的参数（坐标）
	 * @return
	 */
	List<Object> getParams();
}
