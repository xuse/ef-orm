package jef.database;

import java.sql.SQLException;
import java.util.Collection;

/**
 * 延迟加载执行句柄
 * @author jiyi
 *
 */
public interface LazyLoadTask {

	void process(Session db, Object obj) throws SQLException;

	Collection<String> getEffectFields();
}
