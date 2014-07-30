package jef.database;

import java.sql.SQLException;
import java.util.Collection;

public interface LazyLoadTask {

	void process(Session db, Object obj) throws SQLException;

	Collection<String> getEffectFields();
}
