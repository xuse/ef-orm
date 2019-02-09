package jef.database.wrapper.processor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.query.BindVariableField;
import jef.database.query.ConditionQuery;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.support.SqlLog;
import jef.tools.IOUtils;
import jef.tools.reflect.BeanWrapper;

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

	// public void setObject(int count, Object value,int type,int length) throws
	// SQLException {
	// psmt.setObject(count, value,type,length);
	// }

	private Object setValueInPsmt(int count, Object value) throws SQLException {
		if (value != null) {
			if ((value instanceof File)) {
				File file = (File) value;
				try {
					psmt.setBinaryStream(count, IOUtils.getInputStream(file), file.length());
				} catch (IOException e) {
					throw new IllegalArgumentException();
				}
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
	private Object setValueInPsmt(int count, Object value, ColumnMapping cType) throws SQLException {
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

	private static final Object NOT_FOUND = new Object();

	/**
	 * 
	 */
	public enum SqlType {
		INSERT, UPDATE, DELETE, SELECT
	}

	private void setUpdateMapValue(Map<Field, Object> updateMap, Field field, ColumnMapping cType, int count, BeanWrapper bean) throws SQLException {
		if (updateMap.containsKey(field)) {
			Object value = updateMap.get(field);
			try {
				value = this.setValueInPsmt(count, value, cType);
			} catch (ClassCastException e) {
				e.printStackTrace();
				throw new SQLException("The query param type error, field=" + field.name() + ":" + e.getMessage());
			}
			this.log(count, field, value);
		} else {
			Object value = bean.getPropertyValue(field.name());
			value = this.setValueInPsmt(count, value, cType);
			this.log(count, field, value);
		}
	}

	private Object setWhereVariable(BindVariableDescription variableDesc, ConditionQuery query, int count) throws SQLException {
		Collection<Condition> conds = null;
		IQueryableEntity obj = null;
		if (query != null) {
			if (query instanceof JoinElement) {
				conds = ((JoinElement) query).getConditions();
				if (query instanceof Query<?>) {
					obj = ((Query<?>) query).getInstance();
				}
			}
		}
		Object value = getWhereVariable(conds, variableDesc, obj);
		try {
			value = this.setValueInPsmt(count, value, variableDesc.getColumnType());
		} catch (Exception e) {
			String field = variableDesc.getField().name();
			ColumnMapping colType = variableDesc.getColumnType();
			throw new SQLException("The query param type error, field=" + field + " type=" + (colType == null ? "" : colType.getClass().getSimpleName()) + "\n" + e.getClass().getName() + ":" + e.getMessage());
		}
		this.log(count, variableDesc.getField(), value);
		return value;
	}

	/**
	 * 从conditionList或者bean当中获取指定的field的绑定参数值
	 * 
	 * @param conds
	 *            条件列表
	 * @param bean
	 *            实例
	 * @param variableDesc
	 *            要获取的具体值的匹配条件标记
	 * @return
	 */
	private static Object getWhereVariable(Collection<Condition> conds, BindVariableDescription variableDesc, IQueryableEntity obj) {
		Object result = NOT_FOUND;
		if (variableDesc.isInBatch()) {// 批操作的情况下
			for (Condition c : conds) {
				if (c.getField() == variableDesc.getField() && c.getOperator() == variableDesc.getOper()) {
					result = c.getValue();
					break;
				}
			}
			if (result == NOT_FOUND && obj != null && variableDesc.getOper() == Operator.EQUALS) {
				BeanWrapper bean = BeanWrapper.wrap(obj, BeanWrapper.FAST);
				result = bean.getPropertyValue(variableDesc.getField().name());
			}
		} else {
			result = variableDesc.getBindedVar();
		}
		if (result == NOT_FOUND) {
			// 因为发生了TupleField在批操作过程中发生变化，造成无法定位这一特殊BUG，此处针对这一特定场景进行检测，用来提供更为实用的信息。
			for (Condition c : conds) {
				if (c.getField().name().equals(variableDesc.getField().name()) && c.getOperator() == variableDesc.getOper()) {
					throw new IllegalArgumentException(variableDesc + " has a match condition but belongs to another table metadata." + c.getField() + " <> " + variableDesc.getField());
				}
			}
			throw new IllegalArgumentException(variableDesc + "'s value not found in a batch update query.");
		}
		if (variableDesc.getCallback() == null) {// 非条件容器
			return result;
		} else {
			return variableDesc.getCallback().process(result);
		}
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
	public List<Object> setVariables(ConditionQuery da, List<Field> writeFields, List<BindVariableDescription> whereFiels) throws SQLException {
		int count = 0;
		// 更新值绑定
		if (writeFields != null) {
			Query<?> query = (Query<?>) da;
			ITableMetadata meta = query.getMeta();
			BeanWrapper bean = BeanWrapper.wrap(query.getInstance(), BeanWrapper.FAST);
			for (Field field : writeFields) {
				count++;
				if (field instanceof BindVariableField) {
					Object value = ((BindVariableField) field).value;
					psmt.setObject(count, value);
					this.log(count, "", value);
					continue;
				}
				ColumnMapping cType = meta.getColumnDef(field);
				try {
					setUpdateMapValue(query.getInstance().getUpdateValueMap(), field, cType, count, bean);
				} catch (SQLException ex) {
					LogUtil.error("Error while setting [{}] into bean [{}] for {}", field.name(), query.getType(), cType.getClass().getName());
					throw ex;
				}
			}
		}
		// 条件绑定
		if (whereFiels != null) {
			Object[] actualWhereParams = new Object[whereFiels.size()];
			int n = 0;
			for (BindVariableDescription field : whereFiels) {
				count++;
				try {
					Object obj = setWhereVariable(field, da, count);
					actualWhereParams[n++] = obj;
				} catch (SQLException ex) {
					LogUtil.error("Error while setting [{}], error type={}", field.name(), ex.getClass().getName(),ex);
					throw ex;
				}
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
				throw new SQLException("The query param type error, field=" + field.fieldName() ,e);
			}
		}
	}
}
