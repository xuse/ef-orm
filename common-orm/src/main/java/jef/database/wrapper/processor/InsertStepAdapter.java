package jef.database.wrapper.processor;

import java.sql.SQLException;
import java.util.List;

import jef.database.IQueryableEntity;

public class InsertStepAdapter implements InsertStep {
	public void callBefore(List<? extends IQueryableEntity> data) throws SQLException {
	}

	public void callAfterBatch(List<? extends IQueryableEntity> data) throws SQLException {
	}

	public void callAfter(IQueryableEntity data) throws SQLException {
	}
}
