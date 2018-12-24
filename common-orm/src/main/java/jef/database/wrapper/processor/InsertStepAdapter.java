package jef.database.wrapper.processor;

import java.sql.SQLException;
import java.util.List;

public class InsertStepAdapter implements InsertStep {
	public void callBefore(List<?> data) throws SQLException {
	}

	public void callAfterBatch(List<?> data) throws SQLException {
	}

	public void callAfter(Object data) throws SQLException {
	}
}
