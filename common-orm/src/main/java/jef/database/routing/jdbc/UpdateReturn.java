package jef.database.routing.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.DbUtils;
import jef.database.jdbc.rowset.CachedRowSetImpl;

/**
 * 插入、更新、删除操作返回结果
 */
public class UpdateReturn {
	private int affectedRows;
	/**
	 * 插入时的返回自增主键值
	 */
	protected CachedRowSetImpl generatedKeys;

	public boolean isBatch(){
		return false;
	}

	public UpdateReturn(int count) {
		this.affectedRows = count;
	}


	public int getAffectedRows() {
		return affectedRows;
	}

	public void setAffectedRows(int affectedRows) {
		this.affectedRows = affectedRows;
	}

	public void close() {
		DbUtils.close(generatedKeys);
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
}
