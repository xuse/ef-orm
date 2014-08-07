package jef.database.wrapper.clause;

import java.sql.SQLException;
import java.util.List;

import jef.database.rowset.Row;

public interface InMemoryProcessor {
	void process(List<Row> rows)throws SQLException;
}
