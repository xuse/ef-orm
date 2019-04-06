package com.github.geequery.entity;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiConsumer;

import javax.persistence.PersistenceException;

import jef.database.DataObject;
import jef.database.DebugUtil;
import jef.database.Field;
import jef.database.ILazyLoadContext;
import jef.database.IQueryableEntity;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.PKQuery;
import jef.database.query.Query;
import jef.database.query.QueryImpl;
import jef.tools.reflect.Property;

public abstract class Entities {
	private Entities() {
	}

	// 用来标记某个延迟加载列被人工设置过值了。
	public static void beforeRefSet(Object obj, String fieldname) {
		AbstractMetadata em = MetaHolder.getMeta(obj);
		ILazyLoadContext lazyload = (ILazyLoadContext) em.getLazyAccessor().get(obj);
		if (lazyload == null)
			return;
		lazyload.markProcessed(fieldname);
	}

	// 用来标记某个字段被设置过值
	public static void beforeSet(Object obj, int fieldIndex) {
		AbstractMetadata em = MetaHolder.getMeta(obj);
		if (!em.getTouchIgnoreFlag().getBoolean(obj)) {
			Property accessor = em.getTouchRecord();
			BitSet result = (BitSet) accessor.get(obj);
			if (result == null) {
				result = new BitSet();
				accessor.set(obj, result);
			}
			result.set(fieldIndex);
		}
		
		// 如果要处理LOB字段问题
		// ILazyLoadContext lazyload = (ILazyLoadContext)
		// em.getLazyAccessor().getObject(obj);
		// if (lazyload == null)
		// return;
		// lazyload.markProcessed(fieldname);
	}

	// 触发并处理延迟加载的字段
	public static <T> void beforeGet(Object obj, String fieldname) {
		AbstractMetadata em = MetaHolder.getMeta(obj);
		ILazyLoadContext lazyload = (ILazyLoadContext) em.getLazyAccessor().get(obj);
		if (lazyload == null)
			return;
		int id = lazyload.needLoad(fieldname);
		if (id > -1) {
			try {
				if (lazyload.process(obj, id)) {
					lazyload = null; // 清理掉，以后不再需要延迟加载
				}
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	public static boolean isUsed(Object instance, Field fld) {
		if (instance instanceof IQueryableEntity) {
			return ((IQueryableEntity) instance).isUsed(fld);
		} else {
			AbstractMetadata em = MetaHolder.getMeta(instance);
			return em.isTouched(instance, ((Enum) fld).ordinal());
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Query<T> asQuery(T obj) {
		if (obj instanceof IQueryableEntity) {
			return (Query<T>) ((IQueryableEntity) obj).getQuery();
		} else {
			return new QueryImpl<T>(obj);
		}
	}

	/**
	 * 有标记的字段数量
	 * @param obj
	 * @return
	 */
	public static int sizeOfSetFields(Object obj) {
		BitSet bs;
		if (obj instanceof DataObject) {
			bs = DebugUtil.getTouchRecord((DataObject) obj);
		} else {
			AbstractMetadata em = MetaHolder.getMeta(obj);
			bs = (BitSet) em.getTouchRecord().get(obj);
		}
		if (bs == null) {
			return 0;
		}
		return bs.cardinality();
	}

	/**
	 * 为每个有标记的字段执行
	 * @param obj
	 * @param consumer
	 * @return
	 */
	public static int forSetFields(Object obj, BiConsumer<ColumnMapping, Object> consumer) {
		BitSet bs;
		ITableMetadata meta;
		if (obj instanceof DataObject) {
			bs = DebugUtil.getTouchRecord((DataObject)obj);
			meta = MetaHolder.getMeta(obj);
		} else {
			AbstractMetadata em = MetaHolder.getMeta(obj);
			meta = em;
			bs = (BitSet) em.getTouchRecord().get(obj);
		}
		if (bs == null) {
			return 0;
		}
		int count = 0;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			ColumnMapping map = meta.getColumnDef(i);
			Object value = map.getFieldAccessor().get(obj);
			consumer.accept(map, value);
			count++;
		}
		return count;
	}

	public static BitSet getTouchRecord(Object obj) {
		if (obj instanceof DataObject) {
			return DebugUtil.getTouchRecord((DataObject)obj);
		} else {
			AbstractMetadata em = MetaHolder.getMeta(obj);
			return (BitSet) em.getTouchRecord().get(obj);
		}
	}

	public static <T> Query<T> asPKQuery(T d, List<Serializable> pks) {
		ITableMetadata meta = MetaHolder.getMeta(d);
		return new PKQuery<T>(meta, pks, d);
	}

	public static boolean acceptTouch(Object obj) {
		ITableMetadata meta = MetaHolder.getMeta(obj);
		return !meta.getTouchIgnoreFlag().getBoolean(obj);
	}

	// default void setTouchFlag(Object obj, boolean doTouch) {
	// Assert.notNull(obj);
	// getTouchIgnoreFlag().setBoolean(obj, !doTouch);
	// }

	@SuppressWarnings("unchecked")
	public static <T> Query<T> asUpdateQuery(T entity, boolean dynamic) {
		Query<T> q;
		ITableMetadata meta;
		if (entity instanceof IQueryableEntity) {
			q=(Query<T>) ((IQueryableEntity) entity).getQuery();
			if(q.needUpdate()) {
				return q;
			}
			meta= q.getMeta();
		}else {
			meta= MetaHolder.getMeta(entity);
			q=new QueryImpl<T>(entity);
		}
		
		BitSet bs = (BitSet)meta.getTouchRecord().get(entity);
		if(bs==null) {
			return q;
		}
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			ColumnMapping map = meta.getColumnDef(i);
			Object value = map.getFieldAccessor().get(entity);
			if(map.isPk()) {
				q.addCondition(map.field().eq(value));
			}else {
				q.prepareUpdate(map.field(), value);
			}
		}
		return q;
	}

	public static Query<?> hasQueryOrNull(Object obj) {
		if (obj instanceof IQueryableEntity) {
			IQueryableEntity e = (IQueryableEntity) obj;
			return e.hasQuery() ? e.getQuery() : null;
		}
		return null;
	}

	public static void clearUpdate(Object obj) {
		if(obj instanceof IQueryableEntity) {
			((IQueryableEntity) obj).clearUpdate();
		}
		
	}

	public static void clearQuery(Object obj) {
		if(obj instanceof IQueryableEntity) {
			((IQueryableEntity) obj).clearQuery();
		}
	}

	public static void notTouch(Object obj) {
		if(obj instanceof IQueryableEntity) {
			((IQueryableEntity) obj).stopUpdate();
		}else {
			ITableMetadata meta = MetaHolder.getMeta(obj);
			meta.getTouchIgnoreFlag().setBoolean(obj, true);
		}
	}
	
	public static void enableTouch(Object obj) {
		if(obj instanceof IQueryableEntity) {
			((IQueryableEntity) obj).startUpdate();
		}else {
			ITableMetadata meta = MetaHolder.getMeta(obj);
			meta.getTouchIgnoreFlag().setBoolean(obj, false);
		}
	}

	public static boolean needUpdate(Object obj) {
		if(obj instanceof DataObject) {
			DataObject e=(DataObject)obj;
			return (e.hasQuery() && e.getQuery().needUpdate()) || !DebugUtil.getTouchRecord(e).isEmpty();
		}else {
			ITableMetadata meta = MetaHolder.getMeta(obj);
			BitSet bs=(BitSet)meta.getTouchRecord().get(obj);
			return !bs.isEmpty();
		}
	}
}
