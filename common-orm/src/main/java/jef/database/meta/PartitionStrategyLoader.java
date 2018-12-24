package jef.database.meta;

import jef.database.annotation.PartitionTable;

/**
 * 分表规则加载器
 * @author Administrator
 *
 */
public interface PartitionStrategyLoader {
	PartitionTable get(Class<?> clz);
}
