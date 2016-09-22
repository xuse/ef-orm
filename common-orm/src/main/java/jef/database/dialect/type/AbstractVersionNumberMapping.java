package jef.database.dialect.type;

import java.sql.SQLException;
import java.util.List;

import jef.accelerator.bean.BeanAccessor;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.annotation.DateGenerateType;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.query.BindVariableField;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.processor.InsertStepAdapter;
import jef.tools.Assert;
import jef.tools.reflect.Property;

public abstract class AbstractVersionNumberMapping extends AColumnMapping implements VersionSupportColumn {
	private boolean version;
	private Property accessor;
	protected DateGenerateType dateType;

	private Processor STEP;

	private static abstract class Processor extends InsertStepAdapter {
		abstract void update(String columnName, UpdateClause update);

		abstract Object getNextValue(Object current);
	}

	private static class InsertTime extends Processor {
		private Property accessor;
		private AbstractVersionNumberMapping parent;
		private DateGenerateType dateType;
		InsertTime(AbstractVersionNumberMapping parent) {
			this.parent = parent;
			this.accessor = parent.accessor;
			this.dateType=parent.dateType;
			Assert.notNull(dateType);
			Assert.notNull(accessor);
		}
		public void callBefore(List<? extends IQueryableEntity> data) throws SQLException {
			for (IQueryableEntity q : data) {
				accessor.set(q, parent.transfer(dateType.generateLong()));
			}
		}
		void update(String columnName, UpdateClause result) {
			result.addEntry(columnName, new BindVariableField(parent.transfer(dateType.generateLong())));
		}
		Object getNextValue(Object current) {
			return parent.transfer(dateType.generateLong());
		}
	};

	private static class InsertNum extends Processor {
		private Property accessor;
		private AbstractVersionNumberMapping parent;
		InsertNum(AbstractVersionNumberMapping parent) {
			this.parent = parent;
			this.accessor = parent.accessor;
			Assert.notNull(accessor);
		}
		public void callBefore(List<? extends IQueryableEntity> data) throws SQLException {
			for (IQueryableEntity q : data) {
				accessor.set(q, parent.transfer(1L));
			}
		}
		void update(String columnName, UpdateClause result) {
			result.addEntry(columnName, columnName + "+1");
		}
		Object getNextValue(Object current) {
			return parent.increament(current);
		}
	};

	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		BeanAccessor ba = meta.getContainerAccessor();
		accessor = ba.getProperty(field.name());
		if (type instanceof ColumnType.Int) {
			this.version = ((ColumnType.Int) type).isVersion();
			this.dateType = ((ColumnType.Int) type).getGenerateType();
			if (dateType != null) {
				STEP = new InsertTime(this);
			} else {
				STEP = new InsertNum(this);
			}
		}
	}

	@Override
	public void processPreparedInsert(IQueryableEntity obj, List<String> cStr, List<String> vStr, InsertSqlClause result, boolean dynamic) throws SQLException {
		if (!obj.isUsed(field) && version) {
			result.getCallback().addProcessor(STEP);
		}
		super.processPreparedInsert(obj, cStr, vStr, result, dynamic);
	}

	@Override
	public void processAutoUpdate(DatabaseDialect profile, UpdateClause result) {
		String columnName = getColumnName(profile, true);
		STEP.update(columnName, result);
	}

	@Override
	public boolean isVersion() {
		return version;
	}

	@Override
	public Object getAutoUpdateValue(DatabaseDialect profile, Object bean) {
		Object value = accessor.get(bean);
		value = STEP.getNextValue(value);
		accessor.set(bean, value);
		return value;
	}

	abstract Object increament(Object value);
	protected abstract Object transfer(long n);
	
	@Override
	public boolean isUpdateAlways() {
		return version;
	}

	@Override
	public boolean isGenerated() {
		return version;
	}
}
