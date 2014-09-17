package jef.database.routing.sql;

import jef.common.wrapper.IntRange;
import jef.database.wrapper.result.MultipleResultSet;

public interface InMemoryOperateContext {
	boolean hasInMemoryOperate();
	void parepareInMemoryProcess(IntRange range,MultipleResultSet rs);

}
