package jef.database.wrapper.clause;

import java.sql.SQLException;

import jef.database.rowset.CachedRowSetImpl;

public interface InMemoryProcessor {
	void process(CachedRowSetImpl rows)throws SQLException;
}
