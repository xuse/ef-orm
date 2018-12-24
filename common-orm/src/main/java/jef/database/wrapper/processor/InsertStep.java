/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.wrapper.processor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jef.database.DbUtils;
import jef.database.Sequence;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.Property;

/**
 * 描述各种用于在插入后获取自增量字段值并更新到Bean中的回调方法
 * 
 * @author Administrator
 *
 */
public interface InsertStep {
	/**
	 * 在插入前执行
	 * 
	 * @param data
	 * @throws SQLException
	 */
	void callBefore(List<?> data) throws SQLException;

	/**
	 * 在插入Batch后执行
	 * 
	 * @param data
	 * @throws SQLException
	 */
	void callAfterBatch(List<?> data) throws SQLException;

	/**
	 * 在插入后执行
	 * 
	 * @param data
	 * @throws SQLException
	 */
	void callAfter(Object data) throws SQLException;

	/**
	 * 自生成主键处理策略： 值已经预先生成，只需要要插入后调用此类更新到Bean中
	 * 
	 * 此接口
	 * 
	 * @author Administrator
	 */
	final static class SingleKeySetCallback implements InsertStep {
		private Property fieldName;
		private long key;
		private String sKey = null;

		// 必须传入Long型的Property
		public SingleKeySetCallback(Property fieldName, long key) {
			this.fieldName = fieldName;
			this.key = key;
		}

		// 必须传入Long型的Property
		public SingleKeySetCallback(Property fieldName, String key) {
			this.fieldName = fieldName;
			this.sKey = key;
		}

		public void callAfterBatch(List<?> data) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void callAfter(Object data) throws SQLException {
			if (sKey == null) {
				fieldName.set(data, key);
			} else {
				fieldName.set(data, sKey);
			}
		}

		public void callBefore(List<?> data) throws SQLException {
		}
	}

	/**
	 * 对批量的Entity对象生成Sequence的主键，并赋值
	 * 这个回调方法要求在插入到数据库之前运行，直接更新Bean中的值。然后再用更新后的Bean插入数据库
	 * 
	 * @author Administrator
	 */
	final static class SequenceGenerateCallback implements InsertStep {
		private Property field;
		private Sequence holder;

		public SequenceGenerateCallback(Property fieldName, Sequence holder) {
			Assert.notNull(holder);
			this.field = fieldName;
			this.holder = holder;
		}

		public void callBefore(List<?> data) throws SQLException {
			for (Object o : data) {
				long key = -1;
				key = holder.next();
				if (key > -1) {
					field.set(o, key);
				} else {
					throw new SQLException("AutoIncreatment generate error.");
				}
			}
		}

		public void callAfterBatch(List<?> data) throws SQLException {
		}

		@Override
		public void callAfter(Object data) throws SQLException {
		}
	}

	/**
	 * 对批量的DO对象生成Sequence的主键，并赋值
	 * 这个回调方法要求在插入到数据库之前运行，直接更新Bean中的值。然后再用更新后的Bean插入数据库
	 * 
	 * @author Administrator
	 */
	static class GUIDGenerateCallback implements InsertStep {
		private Property field;
		private boolean removeDash;

		public GUIDGenerateCallback(Property fieldName, boolean b) {
			this.field = fieldName;
			this.removeDash = b;
		}

		public void callBefore(List<?> data) throws SQLException {
			for (Object o : data) {
				String key = UUID.randomUUID().toString();
				if (removeDash)
					key = StringUtils.remove(key, '-');
				field.set(o, key);
			}
		}

		@Override
		public void callAfterBatch(List<?> data) throws SQLException {
		}

		@Override
		public void callAfter(Object data) throws SQLException {

		}
	}

	/**
	 * 用于在插入时返回Oracle Rowid的回调
	 * 
	 * @author jiyi
	 *
	 */
	static class OracleRowidKeyCallback implements InsertStep, StatementPreparer {
		private Statement st;

		public void callAfterBatch(List<?> data) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void callAfter(Object entity) throws SQLException {
			ResultSet rs = st.getGeneratedKeys();
			if (rs == null)
				throw new SQLException("getGeneratedKeys() returns null from the " + st + ".");
			try {
				Assert.isTrue(rs.next(), "The JDBC Driver may not support you operation.");
				//FIXME
//				entity.bindRowid(rs.getString(1));
			} finally {
				rs.close();
			}

		}

		public PreparedStatement doPrepareStatement(Connection conn, String sql) throws SQLException {
			PreparedStatement pst = conn.prepareStatement(sql, 1);
			this.st = pst;
			return pst;
		}

		public void callBefore(List<?> data) throws SQLException {
		}
	}

	/**
	 * 对批量或单个对象，赋予从JDBC得到的数据库所生成的主键值(需要驱动支持)
	 * 
	 * @author Administrator
	 * @threadsafe 由于缓存了此对象，因此要求必须是线程安全的
	 * 
	 */
	static class JdbcAutoGeneratedKeyCallback implements InsertStep, StatementPreparer {
		private Property fieldName; // 主键字段
		private String[] columnName; // 返回的行
		private boolean isGuessMode; // 是否要猜测
		private final ThreadLocal<Statement> st = new ThreadLocal<Statement>(); // Statement
		private boolean isBatchForFunction;
		private String getFunction; //

		/**
		 * 
		 * @param fieldName
		 *            必须是Long型号的Property，如果不是请先在外部包装
		 * @param guessMode
		 * @param columnName
		 */
		public JdbcAutoGeneratedKeyCallback(Property fieldName, String columnName, DatabaseDialect profile) {
			this.fieldName = fieldName;
			this.isGuessMode = profile.has(Feature.BATCH_GENERATED_KEY_ONLY_LAST);
			this.isBatchForFunction = profile.has(Feature.BATCH_GENERATED_KEY_BY_FUNCTION);
			this.columnName = new String[] { columnName };
			if (isBatchForFunction) {
				this.getFunction = profile.getProperty(DbProperty.GET_IDENTITY_FUNCTION);
			}
		}

		public void callAfterBatch(List<?> data) throws SQLException {
			if (data.size() == 0)
				return;
			if (isBatchForFunction) {
				byFunction(data);
				return;
			}
			ResultSet rs = st.get().getGeneratedKeys();
			if (rs == null)
				throw new SQLException("getGeneratedKeys() returns null from the " + st + ".");
			try {
				if (isGuessMode) {
					Assert.isTrue(rs.next(), "The JDBC Driver may not support getGeneratedKeys() operation.");
					long max = rs.getLong(1);
					for (int i = data.size() - 1; i >= 0; i--) {
						Object o = data.get(i);
						fieldName.set(o, max--);
					}
				} else {
					for (Object o : data) {
						if (rs.next()) {
							fieldName.set(o, rs.getLong(1));
						} else {
							throw new SQLException("The count of generated key from statement is not match to required.");
						}
					}
					if (rs.next()) {
						throw new SQLException("The count of generated key from statement is greater than the size.");
					}
				}
			} finally {
				rs.close();
			}
		}

		private void byFunction(List<?> data) throws SQLException {
			long generatedKey = -1;
			Statement st2 = null;
			ResultSet rs = null;
			try {
				st2 = st.get().getConnection().createStatement();
				rs = st2.executeQuery(this.getFunction);
				rs.next();
				generatedKey = rs.getLong(1);
			} finally {
				DbUtils.close(st2);
				DbUtils.close(rs);
			}
			for (int i = data.size() - 1; i >= 0; i--) {
				Object o = data.get(i);
				fieldName.set(o, generatedKey--);
			}
		}

		public PreparedStatement doPrepareStatement(Connection conn, String sql) throws SQLException {
			PreparedStatement pst = conn.prepareStatement(sql, columnName);
			this.st.set(pst);
			return pst;
		}

		public void callBefore(List<?> data) throws SQLException {
		}

		@Override
		public void callAfter(Object data) throws SQLException {
			callAfterBatch(Arrays.asList(data));
		}
	}

}
