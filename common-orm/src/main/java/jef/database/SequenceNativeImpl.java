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
package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.DbMetaData.ObjectType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.AbstractSequence;
import jef.database.meta.DbProperty;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

/**
 * 默认的数据库原生Sequence实现
 * <p>
 * 注：当某{@code SequenceKeyHolder}实例创建后，与该实例相关的sequence被删掉，则该sequence不会再被自动创建。
 * </p>
 * 
 */
final class SequenceNativeImpl extends AbstractSequence {
	/**
	 * Sequence Schema
	 */
	private String schema;
	/**
	 * Sequence Name
	 */
	private String sequence;
	/**
	 * Sequence Step
	 */
	private int step = 1;
	/**
	 * 描述Sequence是否存在
	 */
	private boolean exists;

	/**
	 */
	private String selectSql;

	private int initValue;
	private int length;
	private String table;
	private String column;

	/*
	 * @param rawSeqName 名称
	 * 
	 * @param cacheSize 缓存大小
	 * 
	 * @param client 获取数据库
	 * 
	 * @param columnSize 列大小（当seq自动创建时使用）
	 * 
	 * @param table 表名 (可为null) 用于校准初始值
	 * 
	 * @param columne 列名 (可为null) 用于校准初始值
	 * 
	 * @throws SQLException
	 */
	SequenceNativeImpl(String name, OperateTarget target, int length, String table, String column, int initValue,SequenceManager parent) throws SQLException {
		super(target,parent);
		initName(name);
		this.length = length;
		this.table = table;
		this.column = column;
		this.initValue = initValue;
		if (target != null) {
			tryInit();
		}
	}

	@Override
	protected boolean doInit(DbClient session, String dbKey) throws SQLException {
		DbMetaData meta=session.getMetaData(dbKey);
		ensureExists(meta, table, column, length, initValue);
		this.selectSql = generateSQL(meta.getProfile());
		if (exists) {
			OperateTarget target=session.selectTarget(dbKey);
			initStep(target, selectSql);
		}
		return true;
	}

	private String generateSQL(DatabaseDialect dialect) {
		String sql;
		if (schema == null) {
			sql = sequence;
		} else {
			sql = schema + "." + sequence;
		}
		String template = dialect.getProperty(DbProperty.SEQUENCE_FETCH);
		if (template != null) {
			return String.format(template, sql);
		}

		sql = sql + ".nextval";
		template = dialect.getProperty(DbProperty.SELECT_EXPRESSION);
		if (template == null) {
			return "SELECT " + sql;
		} else {
			return String.format(template, sql);
		}
	}

	private void ensureExists(DbMetaData meta, String table, String column, int length, int initValue) throws SQLException {
		// 检测Sequence
		if (meta.existsInSchema(ObjectType.SEQUENCE, schema, sequence)) {
			exists = true;
			return;
		}
		// can be created automatically/
		if (!ORMConfig.getInstance().isAutoCreateSequence()) {
			throw new PersistenceException("Sequence " + schema + "." + sequence + " does not exist on " + meta + "!");
		}
		long max = StringUtils.toLong(StringUtils.repeat('9', length), Long.MAX_VALUE);
		long start = caclStartValue(meta, schema, table, column, initValue, max);
		try {
			meta.createSequence(schema, sequence, start+1, max);
			exists = true;
		} catch (SQLException e) {
			LogUtil.error("Sequence [{}.{}] create error on database {}", schema, sequence, meta);
			throw e;
		}
	}

	private void initName(String name) {
		int index = name.indexOf('.');
		if (index > 0) {
			this.schema = name.substring(0, index);
			this.sequence = name.substring(index + 1);
		} else {
			this.sequence = name;
		}
	}

	/*
	 * 初始化Sequence步长
	 */
	private void initStep(OperateTarget client, String selectSql) {
		int step = JefConfiguration.getInt(DbCfg.DB_SEQUENCE_STEP, 0);
		if (step > 0) {
			return;
		}
		// 通过方言，不消耗值计算步长
		step = client.getProfile().calcSequenceStep(client, this.schema, this.sequence, step);

		if (step == -1) {// 强制计算步长
			try {
				long[] min_max = getSequenceStepByConsumeTwice(client, selectSql);
				step = (int) (min_max[1] - min_max[0]);
				pushRange(min_max[0], min_max[1]);// 加入缓存，避免浪费序列号
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
		if (step < 1)
			step = 1;// 不允许出现0或者负数
		this.step = step;
		int cacheSize = getCacheSize();
		if (cacheSize > 1)
			this.setCacheSize(cacheSize / step);// 根据步长做除法
	}

	/**
	 * 消耗两次Sequence值，得到两次消耗的Sequence值
	 * 
	 * @param schema
	 * @param sequence
	 * @return
	 * @throws SQLException
	 */
	long[] getSequenceStepByConsumeTwice(OperateTarget client, String sql) throws SQLException {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = client.prepareStatement(sql);
			st.setMaxRows(1);
			rs = st.executeQuery();
			long min = 0;
			if (!rs.next()) {
				throw new SQLException("The select expression not return any result.");
			}
			min = rs.getLong(1);
			DbUtils.close(rs);
			rs = st.executeQuery();
			if (!rs.next()) {
				throw new SQLException("The select expression not return any result.");
			}
			return new long[] { min, rs.getLong(1) };
		} finally {
			DbUtils.close(rs);
			DbUtils.close(st);
			client.releaseConnection();
		}
	}

	protected long getFirstAndPushOthers(int size, DbClient conn, String dbKey) throws SQLException {
		// 开始
		long start = System.currentTimeMillis();
		OperateTarget target = (OperateTarget) conn.getSqlTemplate(dbKey);
		PreparedStatement ps = null;
		long result = 0;
		try {
			ps = target.prepareStatement(selectSql);
			ps.setMaxRows(1);
			long value = queryOnce(ps);
			result = value; // 直接将选出的值作为sequence
			// 向后取值（比如value=10, step=5, 那么实际有效的是10,11,12,13,14）
			pushRange(value + 1, value + step - 1); // 从第二个值开始推送到缓存
			for (int i = 1; i < size; i++) {// 获取多次
				value = queryOnce(ps);
				pushRange(value, value + step - 1);
			}
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, selectSql);
			throw e;
		} finally {
			DbUtils.close(ps);
			target.releaseConnection();
		}
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.info(StringUtils.concat(selectSql, " (fetch size=", String.valueOf(size), ")\t[Cost:", String.valueOf(System.currentTimeMillis() - start), "ms]|", target.getTransactionId()));
		}
		return result;
	}

	private long queryOnce(PreparedStatement ps) throws SQLException {
		ResultSet rs = ps.executeQuery();
		try {
			rs.next();
			return rs.getLong(1);
		} finally {
			rs.close();
		}
	}

	@Override
	public String toString() {
		return this.schema + "." + this.sequence;
	}

	public boolean isTable() {
		return false;
	}

	public String getName() {
		if (schema == null)
			return sequence;
		return schema + "." + sequence;
	}

	public boolean isRawNative() {
		return step==1;
	}
}
