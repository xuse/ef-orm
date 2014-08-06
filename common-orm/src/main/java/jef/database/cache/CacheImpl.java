package jef.database.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.BindVariableDescription;
import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.ORMConfig;
import jef.database.SelectProcessor;
import jef.database.SqlProcessor;
import jef.database.jsqlparser.VisitorAdapter;
import jef.database.jsqlparser.expression.JdbcParameter;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.schema.Table;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.SqlContext;
import jef.database.wrapper.BindSql;
import jef.database.wrapper.IQuerySqlResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default level 1 Cache implementation.
 * @author jiyi
 *
 */
@SuppressWarnings("rawtypes")
public final class CacheImpl implements TransactionCache{
	SqlProcessor sqlP;
	SelectProcessor selectp;
	private boolean debug=ORMConfig.getInstance().isCacheDebug();
	private static Logger logger=LoggerFactory.getLogger(CacheImpl.class);
	private AtomicInteger hit=new AtomicInteger();
	private AtomicInteger miss=new AtomicInteger();
	
	public CacheImpl(SqlProcessor sql,SelectProcessor selectp){
		this.sqlP=sql;
		this.selectp=selectp;
	}
	private Map<String,Map<KeyDimension,DimCache>> cache=new HashMap<String,Map<KeyDimension,DimCache>>();

	public boolean contains(Class cls, Object primaryKey) {
		Map<KeyDimension,DimCache> tableCache=cache.get(cls.getName());
		if(tableCache==null || tableCache.isEmpty())return false;
		
		ITableMetadata meta=MetaHolder.getMeta(cls);
		IQueryableEntity data=meta.newInstance();
		DbUtils.setPrimaryKeyValue(data, primaryKey);
		
		BindSql sql=sqlP.toPrepareWhereSql(data.getQuery(), new SqlContext(null,data.getQuery()), false);
		DimCache dc=tableCache.get(new KeyDimension(sql.getSql(),null));
		if(dc==null)return false;
		return dc.load(toParamList(sql.getBind()))!=null;
	}

	public static List<Object> toParamList(List<BindVariableDescription> bind) {
		Object[] array=new Object[bind.size()];
		int n=0;
		for(BindVariableDescription b:bind){
			array[n++]=b.getBindedVar();
		}
		return Arrays.asList(array);
	}

	public void evict(Class cls, Object primaryKey) {
		Map<KeyDimension,DimCache> tableCache=cache.get(cls.getName());
		if(tableCache==null || tableCache.isEmpty())return;
		
		ITableMetadata meta=MetaHolder.getMeta(cls);
		IQueryableEntity data=meta.newInstance();
		DbUtils.setPrimaryKeyValue(data, primaryKey);
		
		BindSql sql=sqlP.toPrepareWhereSql(data.getQuery(), new SqlContext(null, data.getQuery()), false);
	
		DimCache dc=tableCache.get(new KeyDimension(sql.getSql(),null));
		if(dc==null)return;
		dc.remove(toParamList(sql.getBind()));
	}

	public void evict(Class cls) {
		this.cache.remove(cls.getName());
	}

	public void evictAll() {
		this.cache.clear();
		
	}

	public void evict(CacheKey key) {
		Map<KeyDimension,DimCache> tableCache=cache.get(key.getTable());
		if(tableCache==null || tableCache.isEmpty())return;
		
		DimCache dc=tableCache.get(key.getDimension());
		if(dc==null)return;
		dc.remove(key.getParams());
	}

	public <T> void onLoad(CacheKey key, List<T> result, Class<T> clz) {
		//结果集太大不缓存
		if(key==null)return;
		if(result.size()>5000)return;
//		if(clz.getName().equals(key.getTable())){
			Map<KeyDimension,DimCache> tableCache=getCreateTableCache(key.getTable());
			DimCache dc=tableCache.get(key.getDimension());
			if(dc==null){
				dc=new DimCache();
				tableCache.put(key.getDimension(), dc);
			}
			dc.put(key.getParams(), result);
			if(debug){
				logger.info("L1-Cache Store:{}", key);
			}
//		}else{
//			//Join类的查询请求，Cache效率不高，而且可靠性可能有点问题，以前的做法是不缓存。但在启用outerJoin功能后，其实需要支持的。
//			String table=formatTable(key.getTable());
//			Map<KeyDimension,DimCache> tableCache=getCreateTableCache(table);
//			DimCache dc=tableCache.get(key.getDimension());
//			if(dc==null){
//				dc=new DimCache();
//				tableCache.put(key.getDimension(), dc);
//			}
//			dc.put(key.getParams(), result);
//			if(debug){
//				logger.info("L1-Cache Store:{}", key);
//			}
//		}
	}

	public List load(CacheKey key) {
		if(key==null)return null;
		Map<KeyDimension,DimCache> tableCache=cache.get(key.getTable());
		if(tableCache==null || tableCache.isEmpty()){
			miss.getAndIncrement();
			if(debug)
				logger.info("L1-Cache  Miss: {}", key);
			return null;
		}
		
		DimCache dc=tableCache.get(key.getDimension());
		if(dc==null){
			miss.getAndIncrement();
			if(debug)
				logger.info("L1-Cache  Miss: {}", key);
			return null;
		}
		List list=dc.load(key.getParams());
		if(debug){
			if(list==null){
				miss.getAndIncrement();
				logger.info("L1-Cache  Miss: {}",key);
			}else{
				hit.incrementAndGet();
				logger.info("L1-Cache   Hit: {}",key);
			}
		}
		return list;
	}

	public void onInsert(IQueryableEntity obj) {
		if(obj==null)return;
		ITableMetadata meta=MetaHolder.getMeta(obj);
		String table=meta.getName();
		
		CacheKey pkCache=null;
		if(!meta.getPKField().isEmpty()){
			//FIXME getPkDimenision
			BindSql sql=sqlP.toPrepareWhereSql(obj.getQuery(),  new SqlContext(null, obj.getQuery()), false);
			obj.clearQuery();
			KeyDimension dim=new KeyDimension(sql.getSql(),null);
			pkCache = new SqlCacheKey(table, dim, toParamList(sql.getBind()));
		}
		if(pkCache!=null){
			Map<KeyDimension,DimCache> tableCache=getCreateTableCache(table);
			refreshCache(tableCache,pkCache,obj);
		}
	}
	
	public void evict(IQueryableEntity obj) {
		if(obj==null)return;
		ITableMetadata meta=MetaHolder.getMeta(obj);
		Map<KeyDimension,DimCache> tableCache=cache.get(meta.getName());
		if(tableCache==null || tableCache.isEmpty())return;
		
		if(obj.hasQuery()){
			IQuerySqlResult ir=	selectp.toQuerySql(obj.getQuery(), null, null,false);
			evict(ir.getCacheKey());
			return;
		}
		BindSql sql=sqlP.toPrepareWhereSql(obj.getQuery(),  new SqlContext(null, obj.getQuery()), false);
		obj.clearQuery();
		DimCache dc=tableCache.get(new KeyDimension(sql.getSql(),null));
		if(dc==null)return;
		dc.remove(toParamList(sql.getBind()));
	}
	
	public void onDelete(String table, String where, List<Object> object) {
		CacheKey key=new SqlCacheKey(table, new KeyDimension(where,null), object);
		Map<KeyDimension,DimCache> tableCache = this.cache.get(table);
		if(tableCache==null || tableCache.isEmpty()){
			return;
		}
		refreshCache(tableCache, key, null);
	}
	
	public void onUpdate(String table, String where, List<Object> object) {
		CacheKey key=new SqlCacheKey(table, new KeyDimension(where,null), object);
		Map<KeyDimension,DimCache> tableCache = this.cache.get(table);
		if(tableCache==null || tableCache.isEmpty()){
			return;
		}
		refreshCache(tableCache, key, null);
	}

	/*
	 * 缓存刷新策略
	 * 同维度。key所指定的缓存更新或失效
	 * 异维度，全部失效 
	 */
	private void refreshCache(Map<KeyDimension, DimCache> tableCache, CacheKey key, IQueryableEntity obj) {
		DimCache cache=tableCache.remove(key.getDimension());
		tableCache.clear(); //其他维度一律失效
		if(cache==null){
			if(obj!=null){//添加缓存
				cache=new DimCache();
				cache.put(key.getParams(),Arrays.asList(obj));
				if(debug)
					logger.info("L1-Cache Store: {}",key);
			}
//			else{} 本来就没有，什么也不用做
		}else{
			if(obj!=null){//更新缓存
				cache.put(key.getParams(),Arrays.asList(obj));
				if(debug)
					logger.info("L1-Cache Store: {}", key);
			}else{ //删除缓存
				cache.remove(key.getParams());
			}
		}
		//计算完成后，回写缓存
		if(cache!=null)tableCache.put(key.getDimension(), cache);
	}
	
	private Map<KeyDimension, DimCache> getCreateTableCache(String table) {
		Map<KeyDimension,DimCache> tableCache = this.cache.get(table);
		if(tableCache==null){
			tableCache=new HashMap<KeyDimension,DimCache>();
			this.cache.put(table, tableCache);
		}
		return tableCache;
	}


	public boolean isDummy() {
		return false;
	}

	public void process(Truncate st, List<Object> list) {
		Table t=st.getTable();
		ITableMetadata meta=MetaHolder.lookup(t.getSchemaName(), t.getName());
		if(meta==null)return;
		this.cache.remove(meta.getName());
	}

	public void process(Delete st, List<Object> list) {
		if(st.getTable() instanceof Table){
			Table t=(Table) st.getTable();
			ITableMetadata meta=MetaHolder.lookup(t.getSchemaName(), t.getName());
			if(meta==null)return;
			Map<KeyDimension,DimCache> tableCache = this.cache.get(meta.getName());
			if(tableCache==null)return;
			KeyDimension dim= new KeyDimension(st.getWhere(), null);
			CacheKey key=new SqlCacheKey(meta.getName(), dim, list);
			refreshCache(tableCache, key, null);
		}
	}

	public void process(Insert st, List<Object> list) {
		if(st.getTable() instanceof Table){
			Table t=(Table) st.getTable();
			ITableMetadata meta=MetaHolder.lookup(t.getSchemaName(), t.getName());
			if(meta==null)return;
			Map<KeyDimension,DimCache> tableCache = this.cache.get(meta.getName());
			if(tableCache==null)return;
			tableCache.clear();
		}
	}

	public void process(Update st, List<Object> list) {
		if(st.getTable() instanceof Table){
			Table t=(Table) st.getTable();
			ITableMetadata meta=MetaHolder.lookup(t.getSchemaName(), t.getName());
			if(meta==null)return;
			Map<KeyDimension,DimCache> tableCache = this.cache.get(meta.getName());
			if(tableCache==null)return;
			
			KeyDimension dim= new KeyDimension(st.getWhere(), null);
			final AtomicInteger count=new AtomicInteger();
			st.getWhere().accept(new VisitorAdapter(){
				@Override
				public void visit(JdbcParameter jdbcParameter) {
					count.incrementAndGet();
				}

				@Override
				public void visit(JpqlParameter parameter) {
					count.incrementAndGet();
				}
			});
			CacheKey key=new SqlCacheKey(meta.getName(), dim, list.subList(list.size()-count.get(), list.size()));
			refreshCache(tableCache, key, null);
		}
	}

	public int getHitCount() {
		return hit.get();
	}

	public int getMissCount() {
		return miss.get();
	}
}
