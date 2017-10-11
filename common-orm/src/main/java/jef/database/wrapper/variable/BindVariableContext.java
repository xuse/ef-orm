package jef.database.wrapper.variable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.IQueryableEntity;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.query.ConditionQuery;
import jef.database.support.SqlLog;
import jef.tools.IOUtils;

public final class BindVariableContext {
	private PreparedStatement psmt;
	private DatabaseDialect db;
	private final SqlLog logMessage;

	public BindVariableContext(PreparedStatement psmt, DatabaseDialect profile, SqlLog sb) {
		this.psmt = psmt;
		this.logMessage = sb;
		this.db = profile;
	}

	public SqlLog getLogMessage() {
		return logMessage;
	}

	private void log(int count, Object fieldName, Object value) {
		logMessage.append(count, fieldName, value);
	}

	private Object setValueInPsmt(int count, Object value) throws SQLException {
		if (value != null) {
			if ((value instanceof File)) {
				File file = (File) value;
				psmt.setBinaryStream(count, IOUtils.getInputStream(file), file.length());
				return value;
			} else if (value instanceof byte[]) {
				byte[] buf = (byte[]) value;
				psmt.setBinaryStream(count, new ByteArrayInputStream(buf), buf.length);
				return value;
			} else if (value instanceof Enum<?>) {
				value = ((Enum<?>) value).name();
			} else if (value instanceof Character) {
				value = value.toString();
			}
		}
		psmt.setObject(count, value);
		return value;
	}

	/**
	 * 对于绑定变量的SQL对象进行参数赋值
	 * 
	 * @param psmt
	 * @param count
	 * @param value
	 * @param cType
	 * @throws SQLException
	 */
	protected Object setValueInPsmt(int count, Object value, ColumnMapping cType) throws SQLException {
		if (cType == null) {
			if (value.getClass() == java.util.Date.class) {
				value = db.toTimestampSqlParam((Date) value);
			}
			psmt.setObject(count, value);
		} else {
			value = cType.jdbcSet(psmt, value, count, db);
		}
		return value;
	}

	/**
	 * 
	 */
	public enum SqlType {
		INSERT, UPDATE, DELETE, SELECT
	}

	/**
	 * 设置绑定变量值 场景1
	 * 
	 * 用途2 添加Batch任务，可以是Insert或者Update 注意1：
	 * Update任务只会更新Batch创建时指定的字段，不会更新后来新增的字段
	 * 
	 * @param da
	 *            查询对象
	 * @param writeFields
	 *            需要写入的字段，Insert或者update
	 * @param whereFiels
	 *            条件字段
	 * @return 如果有where部分，返回where实际使用的参数
	 * @throws SQLException
	 */
	public List<Object> setVariables(ConditionQuery da, List<Variable> writeFields, List<Variable> whereFiels) throws SQLException {
		int count = 0;
		// 更新值绑定
		if (writeFields != null) {
			for (Variable field : writeFields) {
				Object value = field.jdbcSet(this, ++count, da);
				this.log(count, field.name(), value);
			}
		}
		// 条件绑定
		if (whereFiels != null) {
			Object[] actualWhereParams = new Object[whereFiels.size()];
			int n = 0;
			for (Variable field : whereFiels) {
				Object value = field.jdbcSet(this, ++count, da);
				this.log(count, field.name(), value);
				actualWhereParams[n++] = value;
			}
			return Arrays.asList(actualWhereParams);
		}
		return null;
	}

	/**
	 * 这个方法是在executeSQL和selectBySQL等直接SQL层面的情况下按参数顺序绑定变量使用的
	 * 
	 * @param st
	 * @param params
	 * @param debug
	 * @throws SQLException
	 */
	public void setVariables(List<?> params) throws SQLException {
		int n = 0;
		for (Object value : params) {
			n++;
			try {
				value = this.setValueInPsmt(n, value);
				this.log(n, "", value);
			} catch (SQLException e) {
				String type = value == null ? "null" : value.getClass().getName();
				String message = "Setting bind variable [" + n + "] error, type=" + type;
				LogUtil.error(message);
				throw e;
			}
		}
	}

	/**
	 * 为Insert语句设置绑定变量
	 * 
	 * @param obj
	 *            要插入的对内
	 * @param fields
	 * @throws SQLException
	 */
	public void setInsertVariables(IQueryableEntity obj, List<ColumnMapping> fields) throws SQLException {
		int count = 0;
		for (ColumnMapping field : fields) {
			count++;
			Object value = field.getFieldAccessor().get(obj);
			try {
				value = this.setValueInPsmt(count, value, field);
				this.log(count, field, value);
			} catch (ClassCastException e) {
				throw new SQLException("The query param type error, field=" + field.fieldName() + ":" + e.getMessage());
			}
		}
	}
}
