package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import com.github.geequery.entity.Entities;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.annotation.DateGenerateType;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.query.Func;
import jef.database.query.SqlExpression;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.processor.InsertStep;
import jef.database.wrapper.processor.InsertStepAdapter;
import jef.database.wrapper.variable.ConstantVariable;
import jef.tools.reflect.Property;

abstract class AbstractTimeMapping extends AColumnMapping implements VersionSupportColumn {
	private DateGenerateType generated;
	private boolean version;
	private Property accessor;

	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		if (type instanceof ColumnType.TimeStamp) {
			ColumnType.TimeStamp cType = (ColumnType.TimeStamp) type;
			this.generated = cType.getGenerateType();
			this.version = cType.isVersion();
		} else if (type instanceof ColumnType.Date) {
			this.generated = ((ColumnType.Date) type).getGenerateType();
		}
		if (generated == DateGenerateType.modified_nano) {
			throw new UnsupportedOperationException("the genrator 'modified_nano' only supports java datatype long <-> number in database.");
		}
		// 根据缺省值修复
		Object defaultValue = type.getDefaultValue();
		if (generated == null && (defaultValue == Func.current_date || defaultValue == Func.current_time || defaultValue == Func.now)) {
			generated = DateGenerateType.created;
		}
		if (generated == null && defaultValue != null) {
			String dStr = defaultValue.toString().toLowerCase();
			if (dStr.startsWith("current") || dStr.startsWith("sys")) {
				generated = DateGenerateType.created;
			}
		}
		// 修复不一致的逻辑，当该字段作为version字段时，必须每次自动更新
		if (this.version) {
			if (generated == null) {
				generated = DateGenerateType.modified;
			} else if (generated == DateGenerateType.created) {
				generated = DateGenerateType.modified;
			} else if (generated == DateGenerateType.created_sys) {
				generated = DateGenerateType.modified_sys;
			}
		}
		// 获得Accessor
		BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(meta.getThisType());
		accessor = ba.getProperty(field.name());
	}

	final InsertStep STEP = new InsertStepAdapter() {
		public void callBefore(List<?> data) throws SQLException {
			for (Object q : data) {
				accessor.set(q, getCurrentValue());
			}
		}
	};

	@Override
	public boolean isGenerated() {
		return generated != null;
	}

	@Override
	public void processPreparedInsert(Object obj, List<String> cStr, List<String> vStr, InsertSqlClause result, boolean smart) throws SQLException {
		if (!Entities.isUsed(obj, field) && generated != null) {
			if (isJavaSysdate()) {
				result.getCallback().addProcessor(STEP);
			} else {
				cStr.add(getColumnName(result.profile, true));
				vStr.add(getFunctionString(result.profile));
				return;
			}
		}
		super.processPreparedInsert(obj, cStr, vStr, result, smart);
	}

	public Object getAutoUpdateValue(DatabaseDialect profile, Object bean) {
		if (isJavaSysdate()) {
			Object value = getCurrentValue();
			accessor.set(bean, value);
			return value;
		} else {
			return new SqlExpression(getFunctionString(profile));
		}
	}

	public void processAutoUpdate(DatabaseDialect profile, UpdateClause result) {
		String columnName = getColumnName(profile, true);
		if (isJavaSysdate()) {
			result.addEntry(columnName, new ConstantVariable(getCurrentSqlValue()));
		} else {
			result.addEntry(columnName, getFunctionString(profile));
		}
	}

	public final boolean isUpdateAlways() {
		return generated != null && generated.isModify;
	}

	public boolean isVersion() {
		return version;
	}

	/**
	 * 获得数据库当前日期（或时间）的函数表达式，起到获得数据库时间的作用。 用途是拼接在SQL中，在维护数据库列时将字段写入为数据库的当前时间。
	 * 
	 * @param dialect
	 *            数据库方言
	 * @return 数据库表达式
	 */
	public abstract String getFunctionString(DatabaseDialect dialect);

	/**
	 * 获得当前日期的Java形式 用途是作为参数直接写入到数据库中。即在维护数据库字段时，将字段值写入为当前Java的时间。 <h3>
	 * 注意：要求返回的类型等同于该Java字段的类型 （#getDefaultJavaType 的类型）</h3>
	 * 
	 * @return 写入大到数据库的绑定变量值
	 */
	public abstract Object getCurrentValue();

	private final boolean isJavaSysdate() {
		return generated != null && generated.isJavaTime;
	}

	private Object getCurrentSqlValue() {
		if (this.getSqlType() == Types.TIMESTAMP) {
			return new java.sql.Timestamp(System.currentTimeMillis());
		} else {
			return new java.sql.Date(System.currentTimeMillis());
		}
	}
}
