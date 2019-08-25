package jef.database;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.PersistenceException;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.alibaba.fastjson.annotation.JSONField;

import jef.accelerator.bean.BeanAccessor;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.database.query.QueryImpl;

/**
 * 抽象类，用于实现所有Entity默认的各种方法
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("serial")
@XmlTransient
public abstract class DataObject implements IQueryableEntity {
	/*
	 * 用于与条件做排序
	 * 为什么要对条件做排序？是为了避免让条件因为HashCode变化或者加入先后顺序变化而排序。最终引起SQL硬解析。
	 * 比如 where name = ? and index = ? 和 where index = ? and name = ? 本质上是一个SQL条件，但是因为顺序不同变成了两个SQL语句。 
	 */
	private static final ConditionComparator cmp = new ConditionComparator();
	
	private transient String _rowid;
	
	@JSONField(serialize=false)
	private transient Map<Field, Object> updateValueMap;
	@JSONField(serialize=false)
	protected transient Query<?> query;
	@JSONField(serialize=false)
	protected transient boolean _recordUpdate = true;
	@JSONField(serialize=false)
	transient ILazyLoadContext lazyload;

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
		lazyload = null;
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
	 * @see jef.database.IQueryableEntity#touchUsedFlag(jef.database.Field,
	 * boolean)
	 */
	public void touchUsedFlag(Field field, boolean flag) {
		if (flag) {
			if (updateValueMap == null)
				updateValueMap = new TreeMap<Field, Object>(cmp);
			if (updateValueMap.containsKey(field)) {
				return;
			}
			ITableMetadata meta = MetaHolder.getMeta(this);
			BeanAccessor ba = meta.getContainerAccessor();
			updateValueMap.put(field, ba.getProperty(this, field.name()));
		} else {
			if (updateValueMap != null) {
				updateValueMap.remove(field);
			}
		}
	}

	public final void prepareUpdate(Field field, Object newValue) {
		if (updateValueMap == null)
			updateValueMap = new TreeMap<Field, Object>(cmp);
		updateValueMap.put(field, newValue);
	}


	/**
	 * @deprecated will be removed in ths next release.
	 */
	public final void prepareUpdate(Field field, Object newValue, boolean force) {
		prepareUpdate(field, newValue);	
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

	protected final void beforeSet(String fieldname) {
		if (lazyload == null)
			return;
		lazyload.markProcessed(fieldname);
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
