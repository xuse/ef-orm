package jef.database.wrapper.clause;

import java.sql.SQLException;

import jef.database.jdbc.rowset.CachedRowSetImpl;

public interface InMemoryProcessor {
	void process(CachedRowSetImpl rows)throws SQLException;

	String getName();
}
