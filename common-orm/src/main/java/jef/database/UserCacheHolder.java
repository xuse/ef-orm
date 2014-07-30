package jef.database;

import java.util.HashSet;
import java.util.Set;

/**
 * 应用的一些缓存数据，每个数据库独立实例
 * @author jiyi
 *
 */
public abstract class UserCacheHolder {
	public final Set<String> checkedFunctions=new HashSet<String>();
}
