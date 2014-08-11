package jef.database;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.common.pool.PoolStatus;
import jef.database.cache.TransactionCache;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.AbstractRefField;
import jef.database.meta.Feature;
import jef.database.meta.Reference;
import jef.database.query.Query;
import jef.tools.StringUtils;

/**
 * 用于编写一些直接操作数据库的特殊用法，以得到最好的性能
 * 
 * @author Administrator
 * 
 */
public class DebugUtil {
	static java.lang.reflect.Field sqlState;
	static {
		try {
			sqlState = SQLException.class.getDeclaredField("SQLState");
			if (sqlState != null) {
				sqlState.setAccessible(true);
			}
		} catch (Throwable t) {
			LogUtil.exception(t);
		}
	}

	public ILazyLoadContext getLazy(DataObject o){
		return o.lazyload;
	}
	
	public static void addLazy(DataObject o,LazyLoadProcessor lazy){
		if(o.lazyload==null){
			o.lazyload=new LazyLoadContext(lazy);
		}else{
			throw new IllegalStateException();
		}
	}
	
	private DebugUtil() {
	}
	
	public static void bindQuery(DataObject d,Query<?> e){
		d.query=e;
	}

	/**
	 * 将指定的文本，通过反射强行赋值到SQLException类的seqState字段，用于在不重新封装SQLException的情况下，在SQLException中携带一些自定义的Message。
	 * @param e
	 * @param sql
	 */
	public static void setSqlState(SQLException e, String sql) {
		if(sqlState!=null){
			try {
				sqlState.set(e, sql);
			} catch (Exception e1) {
				LogUtil.exception(e1);
			}
		}
	}
	
	public static PartitionSupport getPartitionSupport(Session db){
		return db.getPool().getPartitionSupport();
		
	}
	public static PoolStatus getPoolStatus(DbClient db){
		return db.getPool().getStatus();
	}
	
	public static String getTransactionId(Session db) {
		return db.getTransactionId(null);
	}

	public static IConnection getConnection(SqlTemplate db) {
		return ((OperateTarget)db).getRawConnection();
	}

	public static IConnection getPooledConnection(DbClient db) {
		try {
			return db.getPool().poll();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage()+" "+e.getSQLState(),e);
		}
	}


	/**
	 * 得到表的全部字段，小写
	 * 
	 * @param db
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public static Set<String> getColumnsInLowercase(OperateTarget db, String tableName) throws SQLException {
		Set<String> set = new HashSet<String>();
		tableName = db.getProfile().getObjectNameIfUppercase(tableName);
		IConnection conn = DebugUtil.getConnection(db);
		try {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			String schema = null;
			if (db.getProfile().has(Feature.USER_AS_SCHEMA)) {
				schema = StringUtils.upperCase(databaseMetaData.getUserName());
			} else if (db.getProfile().has(Feature.DBNAME_AS_SCHEMA))
				schema = db.getDbName();

			int n = tableName.indexOf('.');
			if (n > 0) {// 尝试从表名中计算schema
				schema = tableName.substring(0, n);
				tableName = tableName.substring(n + 1);
			}
			ResultSet rs = databaseMetaData.getColumns(null, schema, tableName, "%");
			try {
				while (rs.next()) {
					String columnName = rs.getString("COLUMN_NAME");
					set.add(columnName.toLowerCase());
				}
			} finally {
				rs.close();
			}
			return set;
		} finally {
			db.releaseConnection();
		}
	}

	/**
	 * 判断表中是否有指定的列
	 * 
	 * @param db
	 * @param tableName
	 * @param column
	 * @return
	 * @throws SQLException
	 */
	public static boolean hasColumn(OperateTarget db, String tableName, String column) throws SQLException {
		if (StringUtils.isEmpty(column))
			return false;
		boolean has = false;
		tableName = db.getProfile().getObjectNameIfUppercase(tableName);
		IConnection conn = DebugUtil.getConnection(db);
		try {
			DatabaseMetaData databaseMetaData = conn.getMetaData();
			String schema = null;
			if (db.getProfile().has(Feature.USER_AS_SCHEMA)) {
				schema = StringUtils.upperCase(databaseMetaData.getUserName());
			} else if (db.getProfile().has(Feature.DBNAME_AS_SCHEMA))
				schema = db.getDbName();

			int n = tableName.indexOf('.');
			if (n > 0) {// 尝试从表名中计算schema
				schema = tableName.substring(0, n);
				tableName = tableName.substring(n + 1);
			}
			ResultSet rs = databaseMetaData.getColumns(null, schema, tableName, "%");
			try {
				while (rs.next()) {
					String columnName = rs.getString("COLUMN_NAME");
					if (column.equalsIgnoreCase(columnName)) {
						has = true;
						break;
					}
				}
			} finally {
				rs.close();
			}
			return has;
		} finally {
			db.releaseConnection();
		}
	}
	
	public static LazyLoadTask getLazyTaskMarker(Map.Entry<Reference, List<AbstractRefField>> entry, Map<Reference,List<Condition>> filters, Session session) {
		return new VsManyLoadTask(entry, filters);
	}
	
	public static TransactionCache getCache(Session session){
		return session.getCache();
	}
}
