package jef.database;

import java.util.List;

/**
 * 可查询的Query对象
 * @author jiyi
 *
 * @param <X>
 */
public interface CountableQuery<X> {
	/**
	 * 返回查询结果
	 * @return
	 */
	public List<X> getResultList();

	/**
	 * 获得查询结果（分页）
	 * @return
	 */
	public List<X> getResultList(long start, int limit);
	
	/**
	 * 返回查询结果的总记录条数
	 * @return
	 */
	public long getResultCount();
}
