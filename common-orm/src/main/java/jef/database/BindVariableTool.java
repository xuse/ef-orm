package jef.database;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.Condition.Operator;
import jef.database.dialect.type.MappingType;
import jef.database.meta.ITableMetadata;
import jef.database.query.BindVariableField;
import jef.database.query.ConditionQuery;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.tools.reflect.BeanWrapper;

public final class BindVariableTool {
	private static Object NOT_FOUND = new Object();

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
	 * 注意2: 在此模式下刪除的Jfile對象，會放到回收文件夾下，提交后也不會刪除，需要手工刪除啊
	 * 
	 * @param da
	 * @param operType
	 * @param writeFields
	 * @param whereFiels
	 * @param psmt
	 * @param db
	 * @param batch
	 * @return 如果有where部分，返回where实际使用的参数
	 * @throws SQLException
	 * @author jiyi
	 */
	public static List<Object> setVariables(ConditionQuery da, List<Field> writeFields, List<BindVariableDescription> whereFiels, BindVariableContext context) throws SQLException {
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
					context.setObject(count, value);
					context.log(count, "", value);
					continue;
				}
				MappingType<?> cType = meta.getColumnDef(field);
				try {
					setUpdateMapValue(query.getInstance().getUpdateValueMap(), field, cType, count, bean, context);
				} catch (SQLException ex) {
					System.out.println("Error when set " + field.name() + " into bean " + query.getType() + " for " + cType.getClass().getName());
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
					Object obj = setWhereVariable(field, da, count, context);
					actualWhereParams[n++] = obj;
				} catch (SQLException ex) {
					LogUtil.exception(ex);
					System.out.println("Error when set " + field.name() + " into bean " + da.getClass().getName());
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
	public static void setVariables(BindVariableContext context, List<?> params) throws SQLException {
		int n = 0;
		for (Object value : params) {
			n++;
			try {
				value = context.setValueInPsmt(n, value);
				context.log(n, "", value);
			} catch (SQLException e) {
				String type = value == null ? "null" : value.getClass().getName();
				String message = "Setting bind variable [" + n + "] error, type=" + type;
				LogUtil.error(message);
				throw e;
			}
		}
	}

//	private static void setInsertValue(Field field, BeanWrapper bean, MappingType<?> cType, int count, BindVariableContext context) throws SQLException {
//		String fieldName = field.name();
//		Object value = bean.getPropertyValue(fieldName);
//		try {
//			value = context.setValueInPsmt(count, value, cType);
//		} catch (ClassCastException e) {
//			throw new SQLException("The query param type error, field=" + fieldName + ":" + e.getMessage());
//		}
//		context.log(count, fieldName, value);
//	}

	private static void setUpdateMapValue(Map<Field, Object> updateMap, Field field, MappingType<?> cType, int count, BeanWrapper bean, BindVariableContext context) throws SQLException {
		if (updateMap.containsKey(field)) {
			Object value = updateMap.get(field);
			try {
				value = context.setValueInPsmt(count, value, cType);
			} catch (ClassCastException e) {
				e.printStackTrace();
				throw new SQLException("The query param type error, field=" + field.name() + ":" + e.getMessage());
			}
			context.log(count, field, value);
		} else {
			Object value = bean.getPropertyValue(field.name());
			value = context.setValueInPsmt(count, value, cType);
			context.log(count, field, value);
		}
	}

	private static Object setWhereVariable(BindVariableDescription variableDesc, ConditionQuery query, int count, BindVariableContext context) throws SQLException {
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
			value = context.setValueInPsmt(count, value, variableDesc.getColumnType());
		} catch (Exception e) {
			String field = variableDesc.getField().name();
			MappingType<?> colType = variableDesc.getColumnType();
			throw new SQLException("The query param type error, field=" + field + " type=" + (colType == null ? "" : colType.getClass().getSimpleName()) + "\n" + e.getClass().getName() + ":" + e.getMessage());
		}
		context.log(count, variableDesc.getField(), value);
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
			throw new IllegalArgumentException(variableDesc + "'s value not found in a batch update query.");
		}
		if (variableDesc.getCallback() == null) {// 非条件容器
			return result;
		} else {
			return variableDesc.getCallback().process(result);
		}
	}

	public static void setInsertVariables(IQueryableEntity obj, List<MappingType<?>> fields, BindVariableContext context) throws SQLException {
		int count = 0;
		for (MappingType<?> field : fields) {
			count++;
			Object value = field.getFieldAccessor().get(obj);
			try {
				value = context.setValueInPsmt(count, value, field);
				context.log(count, field, value);
			} catch (ClassCastException e) {
				throw new SQLException("The query param type error, field=" + field.fieldName() + ":" + e.getMessage());
			}
		}
	}
}
