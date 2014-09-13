package jef.database.routing.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jef.database.DbUtils;
import jef.database.rowset.CachedRowSetImpl;

/**
 * @author junyu
 * 
 */
public class UpdateReturn {
	private int affectedRows;
	private List<SQLException> exceptions;
	private ResultSet generatedKeys;
	private int[] batchResult;

	public UpdateReturn(int count) {
		this.affectedRows = count;
	}

	public UpdateReturn(int[] batchCount) {
		this.batchResult = batchCount;
	}

	public int getAffectedRows() {
		return affectedRows;
	}

	public void setAffectedRows(int affectedRows) {
		this.affectedRows = affectedRows;
	}

	public List<SQLException> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<SQLException> exceptions) {
		this.exceptions = exceptions;
	}

	public void close() {
	}

	public void cacheGeneratedKeys(ResultSet resultSet) throws SQLException {
		if (resultSet == null)
			return;
		CachedRowSetImpl cache = new CachedRowSetImpl();
		cache.populate(resultSet);
		DbUtils.close(resultSet);
		generatedKeys = cache;
	}

	public ResultSet getGeneratedKeys() {
		return generatedKeys;
	}

	public int[] getBatchResult() {
		return batchResult;
	}
}
