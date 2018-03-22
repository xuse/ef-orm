package jef.database.wrapper.populator;

import java.sql.SQLException;
import java.util.Map;

import javax.persistence.PersistenceException;

import jef.database.DataObject;
import jef.database.DebugUtil;
import jef.database.IQueryableEntity;
import jef.database.LazyLoadProcessor;
import jef.database.dialect.type.ColumnMappings;
import jef.database.innerpool.InstancePopulator;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.ITableMetadata;
import jef.tools.reflect.BeanWrapper;

public final class ObjectPopulator implements InstancePopulator {
	ITableMetadata meta;
	Map<String, ColumnDescription> data;
	int bindRowidForColumn;
	LazyLoadProcessor processor;

	public ObjectPopulator(ITableMetadata meta, Map<String, ColumnDescription> data) {
		this.meta = meta;
		this.data = data;
	}

	/**
	 * @param wrapper
	 * @param rs
	 * @return true if there is any valid field for thie object.
	 * @throws SQLException
	 */
	public boolean processOrNull(BeanWrapper wrapper, IResultSet rs) throws SQLException {
		boolean flag = false;
		for (Map.Entry<String, ColumnDescription> entry : data.entrySet()) {
			String fieldName = entry.getKey();
			ColumnDescription c = entry.getValue();
			// Note: 使用getObject方法时，在Oracle 2008-2-2
			// 10.2.4.0驱动下会变为getDate()，从而丢失时分秒。
			try {
				Object obj = c.getAccessor().jdbcGet(rs, c.getN());
				if (!flag && obj != null) {
					flag = true;
				}
				if (obj != null) {
					wrapper.setPropertyValue(fieldName, obj);
				}
			} catch (SQLException e) {
				throw new PersistenceException("Error occured while getting value from resultset. field=[" + fieldName + "], column=[" + c.getName() + "]", e);
			}
		}

		if (bindRowidForColumn > 0) {
			String rowid = (String) ColumnMappings.ROWID.jdbcGet(rs, bindRowidForColumn);
			((IQueryableEntity) wrapper.getWrapped()).bindRowid(rowid);
		}
		if (processor != null) {
			DataObject da = (DataObject) wrapper.getWrapped();
			DebugUtil.addLazy(da, processor);
		}
		return flag;
	}

	public void process(BeanWrapper wrapper, IResultSet rs) throws SQLException {
		for (Map.Entry<String, ColumnDescription> entry : data.entrySet()) {
			String fieldName = entry.getKey();
			ColumnDescription c = entry.getValue();
			// Note: 使用getObject方法时，在Oracle 2008-2-2
			// 10.2.4.0驱动下会变为getDate()，从而丢失时分秒。
			try {
				Object obj = c.getAccessor().jdbcGet(rs, c.getN());
				wrapper.setPropertyValue(fieldName, obj);
			} catch (SQLException e) {
				throw new PersistenceException("Error occured while getting value from resultset. field=[" + fieldName + "], column=[" + c.getName() + "]", e);
			}
		}
		if (bindRowidForColumn > 0) {
			String rowid = (String) ColumnMappings.ROWID.jdbcGet(rs, bindRowidForColumn);
			((IQueryableEntity) wrapper.getWrapped()).bindRowid(rowid);
		}
		if (processor != null) {
			DataObject da = (DataObject) wrapper.getWrapped();
			DebugUtil.addLazy(da, processor);
		}
	}

	public void setProcessor(LazyLoadProcessor processor) {
		this.processor = processor;
	}

	public IQueryableEntity instance() {
		IQueryableEntity entity = meta.newInstance();
		entity.stopUpdate();
		return entity;
	}

	public Class<?> getObjectType() {
		return meta.getContainerType();
	}
}
