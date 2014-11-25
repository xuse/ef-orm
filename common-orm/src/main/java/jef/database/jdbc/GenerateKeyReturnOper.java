package jef.database.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import jef.database.routing.jdbc.UpdateReturn;

public abstract class GenerateKeyReturnOper {

	public abstract PreparedStatement prepareStatement(JDBCTarget target, String sql) throws SQLException;

	public abstract void getGeneratedKey(UpdateReturn result, Statement st) throws SQLException;

	public final static GenerateKeyReturnOper NONE = new GenerateKeyReturnOper() {
		@Override
		public PreparedStatement prepareStatement(JDBCTarget target, String sql) throws SQLException {
			return target.prepareStatement(sql);
		}

		@Override
		public void getGeneratedKey(UpdateReturn result, Statement st) {
		}
	};

	public final static GenerateKeyReturnOper RETURN_KEY = new GenerateKeyReturnOper() {
		@Override
		public PreparedStatement prepareStatement(JDBCTarget target, String sql) throws SQLException {
			return target.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		}

		@Override
		public void getGeneratedKey(UpdateReturn result, Statement st) throws SQLException {
			result.cacheGeneratedKeys(st.getGeneratedKeys());
		}
	};

	public static final class ReturnByColumnNames extends GenerateKeyReturnOper {
		private String[] columnNames;

		public ReturnByColumnNames(String[] columnNames) {
			this.columnNames = columnNames;
		}

		@Override
		public PreparedStatement prepareStatement(JDBCTarget target, String sql) throws SQLException {
			return target.prepareStatement(sql, columnNames);
		}

		@Override
		public void getGeneratedKey(UpdateReturn result, Statement st) throws SQLException {
			result.cacheGeneratedKeys(st.getGeneratedKeys());
		}
	}

	public static final class ReturnByColumnIndex extends GenerateKeyReturnOper {
		private int[] columnIndexs;

		public ReturnByColumnIndex(int[] indexes) {
			this.columnIndexs = indexes;
		}

		@Override
		public PreparedStatement prepareStatement(JDBCTarget target, String sql) throws SQLException {
			return target.prepareStatement(sql, columnIndexs);
		}

		@Override
		public void getGeneratedKey(UpdateReturn result, Statement st) throws SQLException {
			result.cacheGeneratedKeys(st.getGeneratedKeys());
		}
	}
}
