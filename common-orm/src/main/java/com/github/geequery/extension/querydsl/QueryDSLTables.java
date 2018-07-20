package com.github.geequery.extension.querydsl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.database.DataObject;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

public class QueryDSLTables {
	private static Map<String, SQLRelationalPath<?>> sqlRelationalPathBaseMap = new ConcurrentHashMap<>();

	/**
	 * 批量初始化缓存
	 * @param classMap
	 */
	public static void initBatch(Collection<ITableMetadata> classMap) {
		for (ITableMetadata meta : classMap) {
			table(meta);
		}
	}

	/**
	 * 生成QueryDSL的表达式对象
	 * @param clz
	 * @return
	 */
	public static <T extends DataObject> SQLRelationalPath<T> table(Class<T> clz) {
		AbstractMetadata tm = MetaHolder.getMeta(clz);
		return table(tm);
	}

	/**
	 * 生成QueryDSL的表达式对象
	 * 
	 * @param tm
	 * @return
	 * @deprecated use {@link #table(Class)} please.
	 */
	public static <T extends DataObject> SQLRelationalPath<T> relationalPathBase(Class<T> tm) {
		return table(MetaHolder.getMeta(tm), null);
	}
	
	
	/**
	 * 生成QueryDSL的表达式对象
	 * 
	 * @param tm
	 * @return
	 * @deprecated use {@link #table(ITableMetadata)} please.
	 */
	public static <T extends DataObject> SQLRelationalPath<T> relationalPathBase(ITableMetadata tm) {
		return table(tm, null);
	}

	/**
	 * 生成QueryDSL的表达式对象
	 * 
	 * @param tm
	 * @return
	 */
	public static <T extends DataObject> SQLRelationalPath<T> table(ITableMetadata tm) {
		return table(tm, null);
	}

	/**
	 * 生成QueryDSL的表达式对象
	 * 
	 * @param tm
	 * @param name
	 *            当有多个不同的表达式对象时
	 * @return
	 */
	public static <T extends DataObject> SQLRelationalPath<T> table(ITableMetadata tm, String variable) {
		if (variable == null) {
			variable = tm.getThisType().getName();
		}
		SQLRelationalPath<?> pb = sqlRelationalPathBaseMap.get(variable);
		if (pb == null) {
			pb = new SQLRelationalPath(tm, variable);
			sqlRelationalPathBaseMap.put(variable, pb);
		}
		return (SQLRelationalPath<T>) pb;
	};

}
