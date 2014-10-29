package jef.database;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.persistence.PersistenceException;
import javax.xml.bind.annotation.XmlTransient;

import jef.accelerator.bean.BeanAccessor;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.database.query.QueryImpl;
import jef.tools.reflect.BeanWrapper;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 抽象类，用于实现所有Entity默认的各种方法
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("serial")
@XmlTransient
public abstract class DataObject implements IQueryableEntity {
	private transient Map<Field, Object> updateValueMap;
	protected transient Query<?> query;
	protected transient boolean _recordUpdate = true;
	private transient String _rowid;
	transient ILazyLoadContext lazyload;

	private static final ConditionComparator cmp = new ConditionComparator();

	public final void startUpdate() {
		_recordUpdate = true;
	}

	public final void stopUpdate() {
		_recordUpdate = false;
	}

	public final boolean hasQuery() {
		return query != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.IQueryableEntity#getQuery()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final Query<?> getQuery() {
		if (query == null)
			query = new QueryImpl(this);
		return query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.IQueryableEntity#clearQuery()
	 */
	public final void clearQuery() {
		query = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.IQueryableEntity#isUsed(jef.database.Field)
	 */
	public final boolean isUsed(Field field) {
		if (updateValueMap == null)
			return false;
		return updateValueMap.containsKey(field);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#clearUpdate()
	 */
	public final void clearUpdate() {
		updateValueMap = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#getUpdateValueMap()
	 */
	@SuppressWarnings("unchecked")
	public final Map<Field, Object> getUpdateValueMap() {
		if (updateValueMap == null)
			return Collections.EMPTY_MAP;
		return updateValueMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#prepareUpdate(jef.database.Field,
	 * java.lang.Object)
	 */
	public final void prepareUpdate(Field field, Object newValue) {
		prepareUpdate(field, newValue, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#prepareUpdate(jef.database.Field,
	 * java.lang.Object, boolean)
	 */
	public final void prepareUpdate(Field field, Object newValue, boolean force) {
		ITableMetadata meta = MetaHolder.getMeta(this);
		BeanAccessor ba = meta.getBeanAccessor();
		String fieldName = field.name();
		if (updateValueMap == null)
			updateValueMap = new TreeMap<Field, Object>(cmp);
		if (force || !ObjectUtils.equals(ba.getProperty(this,fieldName), newValue)) {
			updateValueMap.put(field, newValue);
		}
		return;
	}

	void markUpdateFlag(Field field, Object newValue) {
		if (updateValueMap == null) {
			updateValueMap = new TreeMap<Field, Object>(cmp);
			updateValueMap.put(field, newValue);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#applyUpdate()
	 */
	public final void applyUpdate() {
		if (updateValueMap == null)
			return;

		ITableMetadata meta = MetaHolder.getMeta(this);
		BeanAccessor ba = meta.getBeanAccessor();
		
		for (Entry<Field, Object> entry : updateValueMap.entrySet()) {
			Object newValue = entry.getValue();
			if (newValue instanceof Expression || newValue instanceof jef.database.Field) {
				continue;
			}
			ba.setProperty(this, entry.getKey().name(), newValue);
		}
		clearUpdate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#needUpdate()
	 */
	public final boolean needUpdate() {
		return (updateValueMap != null) && this.updateValueMap.size() > 0;
	}

	public String rowid() {
		return _rowid;
	}

	public void bindRowid(String rowid) {
		this._rowid = rowid;
	}

	/*
	 * 供子类hashCode（）方法调用，判断内嵌的hashCode方法是否可用
	 */
	protected final int getHashCode() {
		return new HashCodeBuilder().append(query).append(_recordUpdate).append(updateValueMap).toHashCode();
	}

	/*
	 * 处理延迟加载的字段
	 */
	protected final void beforeGet(String fieldname) {
		if (lazyload == null)
			return;
		int id = lazyload.needLoad(fieldname);
		if (id > -1) {
			try {
				if (lazyload.process(this, id)) {
					lazyload = null; // 清理掉，以后不再需要延迟加载
				}
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	/*
	 * 供子类的equals方法调用，判断内嵌的query对象、updateMap对象是否相等
	 */
	protected final boolean isEquals(Object obj) {
		if (!(obj instanceof DataObject)) {
			return false;
		}
		DataObject rhs = (DataObject) obj;
		return new EqualsBuilder().append(this.query, rhs.query).append(_recordUpdate, rhs._recordUpdate).append(this.updateValueMap, rhs.updateValueMap).isEquals();
	}

	private static class ConditionComparator implements Comparator<Field>, Serializable {
		public int compare(Field o1, Field o2) {
			if (o1 == o2)
				return 0;
			if (o1 == null)
				return 1;
			if (o2 == null)
				return -1;
			return o1.name().compareTo(o2.name());
		}
	}
}
