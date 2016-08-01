package jef.database.cache;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.ORMConfig;
import jef.database.SelectProcessor;
import jef.database.SqlProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.JdbcParameter;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.processor.BindVariableDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * The default level 1 Cache implementation.
 * 
 * @author jiyi
 * 
 */
@SuppressWarnings("rawtypes")
public class CacheImpl implements Cache {
	SqlProcessor preparedSqlProcessor;
	SelectProcessor selectp;
	private ORMConfig config = ORMConfig.getInstance();
	private static Logger logger = LoggerFactory.getLogger(CacheImpl.class);
	/**
	 * 缓存计数器
	 */
	private AtomicLong hit = new AtomicLong();
	/**
	 * 缓存计数器
	 */
	private AtomicLong miss = new AtomicLong();
	/**
	 * 缓存失效周期，单位秒
	 */
	private int expireInterval;
	/**
	 * 缓存名称
	 */
	private String name;

	private DatabaseDialect profile;

	/**
	 * 
	 * 缓存实际上是三级Map。 第一级是以查询用的表（或多表）为key。 第二级是以where语句的模版为key
	 * 第三级是以where语句中的参数为key。
	 * 
	 * 定时失效机制如何添加：拟添加在第三层 清洗规则中，可以以任意一级为条件进行清洗
	 * 
	 */
	private final Map<String, Map<KeyDimension, DimCache>> cache = new ConcurrentHashMap<String, Map<KeyDimension, DimCache>>();

	/**
	 * 构造
	 * 
	 * @param sql
	 *            SQL处理器
	 * @param selectp
	 *            查询处理器
	 * @param expireTime
	 *            缓存过期事件
	 */
	public CacheImpl(SqlProcessor sql, SelectProcessor selectp, int expireInterval, String name) {
		this.preparedSqlProcessor = sql;
		this.selectp = selectp;
		this.expireInterval = expireInterval;
		this.name = name;
		this.profile = preparedSqlProcessor.getProfile();
	}

	public boolean contains(Class cls, Object primaryKey) {
		AbstractMetadata meta = MetaHolder.getMeta(cls);
		KeyDimension dim = meta.getPKDimension(profile);

		Map<KeyDimension, DimCache> tableCache = cache.get(dim.getTableDefinition());
		if (tableCache == null || tableCache.isEmpty())
			return false;

		DimCache dc = tableCache.get(dim);
		if (dc == null)
			return false;

		List<Serializable> pks = toPrimaryKey(primaryKey);
		return dc.load(pks) != null;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> toParamList(List<BindVariableDescription> bind) {
		if (bind == null)
			return Collections.EMPTY_LIST;
		Object[] array = new Object[bind.size()];
		int n = 0;
		for (BindVariableDescription b : bind) {
			array[n++] = b.getBindedVar();
		}
		return Arrays.asList(array);
	}

	/**
	 * 精确清除缓存。没有什么实际意义。不建议使用！！！。 因为EF的缓存空间是一个多维度的空间，并且维度之间还存在关系。定点清除没有任何意义。
	 */
	public void evict(Class cls, Object primaryKey) {
		throw new UnsupportedOperationException();
		// AbstractMetadata meta = MetaHolder.getMeta(cls);
		// KeyDimension dim = meta.getPKDimension(profile);
		//
		// Map<KeyDimension, DimCache> tableCache =
		// cache.get(dim.getTableDefinition());
		// if (tableCache == null || tableCache.isEmpty())
		// return;
		// @SuppressWarnings("deprecation")
		// DimCache dc = tableCache.get(dim);
		// if (dc == null)
		// return;
		//
		// List<Serializable> pks = toPrimaryKey(primaryKey);
		// dc.remove(pks);
	}

	public void evict(Class cls) {
		AbstractMetadata meta = MetaHolder.getMeta(cls);
		KeyDimension dim = meta.getPKDimension(profile);
		Map<KeyDimension, DimCache> tableSpace = this.cache.get(dim.getTableDefinition());
		for (DimCache cache : tableSpace.values()) {
			cache.clear();
		}
	}

	public void evictAll() {
		for (Map<KeyDimension, DimCache> space : cache.values()) {
			space.clear();
		}
	}

	/**
	 * for JPA Only
	 * 
	 * @param key
	 */
	public void evict(CacheKey key) {
		Map<KeyDimension, DimCache> tableCache = cache.get(key.getStoreSpace());
		if (tableCache == null || tableCache.isEmpty())
			return;

		DimCache dc = tableCache.get(key.getDimension());
		if (dc == null)
			return;
		dc.remove(key.getParams());
	}

	public <T> void onLoad(CacheKey key, List<T> result, Class<T> clz) {
		// 结果集太大不缓存
		if (key == null)
			return;
		if (result.size() > 5000)
			return;
		DimCache dc;
		{
			Map<KeyDimension, DimCache> tableCache = getCreateTableCache(key.getStoreSpace());
			dc = tableCache.get(key.getDimension());
			if (dc == null) {
				dc = expireInterval > 0 ? new DimCacheExpImpl(expireInterval) : new DimCacheImpl();
				tableCache.put(key.getDimension(), dc);
			}
			dc.put(key.getParams(), ImmutableList.copyOf(result));
		}

		if (key.getAffectedKey() != null) {
			for (String indexKey : key.getAffectedKey()) {
				// DC串门算法。通过DC串门，让关联空间也能发现这个DC，从而清除DC
				Map<KeyDimension, DimCache> tableCache = getCreateTableCache(indexKey);
				tableCache.put(key.getDimension(), dc);
			}
		}
		if (config.cacheDebug) {
			logger.info("{}-Cache Store:{}, Size={}", name, key, result.size());
		}
	}

	public List load(CacheKey key) {
		if (key == null)
			return null;
		Map<KeyDimension, DimCache> tableCache = cache.get(key.getStoreSpace());
		boolean debug = config.cacheDebug;
		if (tableCache == null || tableCache.isEmpty()) {
			miss.getAndIncrement();
			if (debug)
				logger.info("{}-Cache  Miss: {}", name, key);
			return null;
		}

		DimCache dc = tableCache.get(key.getDimension());
		if (dc == null) {
			miss.getAndIncrement();
			if (debug)
				logger.info("{}-Cache  Miss: {}", name, key);
			return null;
		}
		List list = dc.load(key.getParams());
		if (debug) {
			if (list == null) {
				miss.getAndIncrement();
				logger.info("{}-Cache  Miss: {}", name, key);
			} else {
				hit.incrementAndGet();
				logger.info("{}-Cache   Hit: {}", name, key);
			}
		}
		return list;
	}

	public void onInsert(IQueryableEntity obj, String table) {
		if (obj == null)
			return;

		/*
		 * 2015-6-15 由于在存入对象时，没有延迟加载钩子，造成读取到缓存中的对象时，延迟加载功能未生效。
		 * 由于Entity有状态（延迟加载钩子），缓存中如何处理？
		 * 
		 * 办法1：先停止插入时的缓存。 解决办法2：级联关系不缓存，也就是说，每次单表查询完成后，无论是否命中缓存都要重新计算级联关系。
		 * 
		 * 目前两个方案都启用。
		 * 采用方案1由于存入缓存的对象句柄依然在外部，意味着用户可以随意修改该对象用作其他用途，因此将这样的对象直接缓存下来是危险的。
		 */
		AbstractMetadata meta = MetaHolder.getMeta(obj);
		KeyDimension dim = null;

		if (!meta.getPKFields().isEmpty()) {
			List<Serializable> pks = DbUtils.getPKValueSafe(obj);
			if (pks == null)
				return;

			if (table != null) {
				dim = meta.getPKDimension(profile).newKeyDimensionOf(table, profile);
			} else {
				dim = meta.getPKDimension(profile);
			}
			CacheKey pkCache = new SqlCacheKey(dim, pks);

			Map<KeyDimension, DimCache> tableCache = getCreateTableCache(pkCache.getStoreSpace());
			refreshCache(tableCache, pkCache, null);
		} else {
			Map<KeyDimension, DimCache> tableCache = cache.get(meta.getTableName(false).toUpperCase());
			if (tableCache != null)
				refreshCacheExcept(tableCache, null);
		}
	}

	public void evict(IQueryableEntity obj) {
		if (obj == null)
			return;
		ITableMetadata meta = MetaHolder.getMeta(obj);
		String baseTableName = meta.getTableName(false);

		Map<KeyDimension, DimCache> tableCache = cache.get(baseTableName);
		if (tableCache == null || tableCache.isEmpty())
			return;

		if (obj.hasQuery()) {
			QueryClause ir = selectp.toQuerySql(obj.getQuery(), null, false);
			evict(ir.getCacheKey());
			return;
		}
		BindSql sql = preparedSqlProcessor.toWhereClause(obj.getQuery(), new SqlContext(null, obj.getQuery()), null, null);
		obj.clearQuery();
		DimCache dc = tableCache.get(KeyDimension.forSingleTable(baseTableName, sql.getSql(), null, profile));
		if (dc == null)
			return;
		dc.remove(toParamList(sql.getBind()));
	}

	public void onDelete(String table, String where, List<Object> object) {
		CacheKey key = new SqlCacheKey(KeyDimension.forSingleTable(table, where, null, profile), object);
		Map<KeyDimension, DimCache> tableCache = this.cache.get(key.getStoreSpace());
		if (tableCache == null || tableCache.isEmpty()) {
			return;
		}
		refreshCache(tableCache, key, null);
	}

	public void onUpdate(String table, String where, List<Object> object) {
		CacheKey key = new SqlCacheKey(KeyDimension.forSingleTable(table, where, null, profile), object);
		Map<KeyDimension, DimCache> tableCache = this.cache.get(key.getStoreSpace());
		if (tableCache == null || tableCache.isEmpty()) {
			return;
		}
		refreshCache(tableCache, key, null);
	}

	/**
	 * 缓存刷新策略 同维度。key所指定的缓存更新或失效 异维度，全部失效
	 * 
	 * @param tableCache
	 *            目标空间
	 * @param key
	 *            被保留的维度
	 * @param obj
	 *            在被保留维度中放入缓存的对象
	 */
	private void refreshCache(Map<KeyDimension, DimCache> tableCache, CacheKey key, IQueryableEntity obj) {
		DimCache cache = tableCache.remove(key.getDimension());
		for (DimCache other : tableCache.values()) {
			// 其他维度一律失效。必须用此种方法，才能清除掉串门的缓存
			other.clear();
		}

		if (cache == null) {
			if (obj != null) {// 添加缓存
				cache = expireInterval > 0 ? new DimCacheExpImpl(expireInterval) : new DimCacheImpl();
				cache.put(key.getParams(), Arrays.asList(obj));
				if (config.cacheDebug)
					logger.info("{}-Cache Store: {}", name, key);
			}
			// else{} 本来就没有，什么也不用做
		} else {
			if (obj != null) {// 更新缓存
				cache.put(key.getParams(), Arrays.asList(obj));
				if (config.cacheDebug)
					logger.info("{}-Cache Store: {}", name, key);
			} else { // 删除缓存
				cache.remove(key.getParams());
			}
		}
		// 计算完成后，回写缓存
		if (cache != null)
			tableCache.put(key.getDimension(), cache);
	}

	/**
	 * 清除除了指定维度之外的全部缓存
	 * 
	 * @param tableCache
	 * @param key
	 */
	private void refreshCacheExcept(Map<KeyDimension, DimCache> tableCache, KeyDimension key) {
		if (key == null) {
			for (Map.Entry<KeyDimension, DimCache> other : tableCache.entrySet()) {
				other.getValue().clear();
			}
		} else {
			for (Map.Entry<KeyDimension, DimCache> other : tableCache.entrySet()) {
				// 其他维度一律失效。必须用此种方法，才能清除掉串门的缓存
				if (!other.getKey().equals(key)) {
					other.getValue().clear();
				}
			}
		}
	}

	private Map<KeyDimension, DimCache> getCreateTableCache(String table) {
		Map<KeyDimension, DimCache> tableCache = this.cache.get(table);
		if (tableCache == null) {
			tableCache = new ConcurrentHashMap<KeyDimension, DimCache>();
			this.cache.put(table, tableCache);
		}
		return tableCache;
	}

	public boolean isDummy() {
		return false;
	}

	public void process(Truncate st, List<Object> list) {
		Table t = st.getTable();
		String tableName = t.getName().toUpperCase();
		Map<KeyDimension, DimCache> tableCache = this.cache.get(tableName);
		if (tableCache != null) {
			refreshCacheExcept(tableCache, null);
		}
	}

	public void process(Delete st, List<Object> list) {
		if (st.getTable() instanceof Table) {
			Table t = (Table) st.getTable();
			KeyDimension dim = new KeyDimension(t, st.getWhere(), null);
			CacheKey key = new SqlCacheKey(dim, list);
			Map<KeyDimension, DimCache> tableCache = this.cache.get(key.getStoreSpace());

			if (tableCache == null)
				return;
			// 删除了该表中的若干数据
			// 该表相关缓存中，除了相同维度，且未被删除的数据之外，全部清除。
			refreshCache(tableCache, key, null);
		}
	}

	public void process(Insert st, List<Object> list) {
		if (st.getTable() instanceof Table) {
			Table t = (Table) st.getTable();
			Map<KeyDimension, DimCache> tableCache = this.cache.get(t.getName().toUpperCase());
			if (tableCache == null)
				return;

			AbstractMetadata meta = MetaHolder.lookup(t.getSchemaName(), t.getName());
			if (meta == null) {
				refreshCacheExcept(tableCache, null);
			} else {
				KeyDimension key = meta.getPKDimension(profile);
				refreshCacheExcept(tableCache, key);
			}
		}
	}

	public void process(Update st, List<Object> list) {
		if (st.getTable() instanceof Table) {
			Table t = (Table) st.getTable();

			Map<KeyDimension, DimCache> tableCache = this.cache.get(t.getName().toUpperCase());
			if (tableCache == null)
				return;

			AbstractMetadata meta = MetaHolder.lookup(t.getSchemaName(), t.getName());
			if (meta == null) {
				refreshCacheExcept(tableCache, null);
				return;
			}

			KeyDimension dim = new KeyDimension(t, st.getWhere(), null);
			final AtomicInteger count = new AtomicInteger();
			st.getWhere().accept(new VisitorAdapter() {
				@Override
				public void visit(JdbcParameter jdbcParameter) {
					count.incrementAndGet();
				}

				@Override
				public void visit(JpqlParameter parameter) {
					count.incrementAndGet();
				}
			});
			CacheKey key = new SqlCacheKey(dim, list.subList(list.size() - count.get(), list.size()));
			refreshCache(tableCache, key, null);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Serializable> toPrimaryKey(Object primaryKey) {
		if (primaryKey instanceof List) {
			return (List<Serializable>) primaryKey;
		} else {
			return Arrays.asList((Serializable) primaryKey);
		}
	}

	public long getHitCount() {
		return hit.get();
	}

	public long getMissCount() {
		return miss.get();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return (T)this;
	}
}
