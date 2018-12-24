package jef.database;

import java.sql.SQLException;
import java.util.BitSet;

import javax.persistence.PersistenceException;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.alibaba.fastjson.annotation.JSONField;

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

	private transient String _rowid;
	/**
	 * 对应的查询对象
	 */
	@JSONField(serialize = false)
	protected transient Query<?> query;
	/**
	 * 是否进行Touch标记
	 */
	@JSONField(serialize = false)
	protected transient boolean _recordUpdate = true;
	/**
	 * 已经标记为Touch的字段
	 */
	@JSONField(serialize = false)
	transient BitSet ___touchRecord;
	/**
	 * 延迟加载上下文
	 */
	@JSONField(serialize = false)
	transient ILazyLoadContext lazyload;

	/////////////////////////////////////////////////////////////////
	
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
	public final <T> Query<T> getQuery() {
		if (query == null)
			query = new QueryImpl(this);
		return (Query<T>) query;
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
		if (___touchRecord == null) {
			return false;
		} else {
			int index = field.asEnumOrdinal();
			return index < 0 ? false : ___touchRecord.get(index);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.query.UpdateAble#clearUpdate()
	 */
	public final void clearUpdate() {
		if (___touchRecord != null) {
			___touchRecord.clear();
		}
		if (query != null) {
			query.clearUpdateMap();
		}
	}

	@Override
	public boolean needUpdate() {
		if(query!=null && !query.getUpdateValueMap().isEmpty()){
			return true;
		}
		return !this.___touchRecord.isEmpty();
	}

	//为子类提供若干方便，增强代码要用，不可删除
	protected void _touch(int index) {
		BitSet b = this.___touchRecord;
		if (b == null) {
			b = new BitSet();
			___touchRecord = b;
		}
		b.set(index);
	}

	@Override
	public void touchFlag(Field field, boolean value) {
		int index = field.asEnumOrdinal();
		if (index < 0)
			return;
		BitSet b = this.___touchRecord;
		if (b == null) {
			if (!value) {
				return;
			}
			b = new BitSet();
			___touchRecord = b;
		}
		b.set(index, value);
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
		return new HashCodeBuilder().append(query).append(_recordUpdate).toHashCode();
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
		return new EqualsBuilder().append(this.query, rhs.query).append(_recordUpdate, rhs._recordUpdate).isEquals();
	}

}
