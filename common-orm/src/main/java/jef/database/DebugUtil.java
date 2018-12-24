package jef.database;

import java.lang.reflect.Type;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.log.LogUtil;
import jef.database.cache.Cache;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.PartitionSupport;
import jef.database.innerpool.WrapableConnection;
import jef.database.meta.AbstractRefField;
import jef.database.meta.Feature;
import jef.database.meta.Reference;
import jef.database.query.Query;
import jef.tools.StringUtils;
import jef.tools.reflect.BooleanProperty;
import jef.tools.reflect.Property;

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

	private DebugUtil() {
	}

	public static void bindQuery(DataObject d, Query<?> e) {
		d.query = e;
	}

	/**
	 * 将指定的文本，通过反射强行赋值到SQLException类的seqState字段，用于在不重新封装SQLException的情况下，
	 * 在SQLException中携带一些自定义的Message。
	 * 
	 * @param e
	 * @param sql
	 */
	public static void setSqlState(SQLException e, String sql) {
		if (sqlState != null) {
			try {
				sqlState.set(e, sql);
			} catch (Exception e1) {
				LogUtil.exception(e1);
			}
		}
	}

	public static PartitionSupport getPartitionSupport(Session db) {
		return db.getPartitionSupport();

	}


	public static String getTransactionId(Session db) {
		return db.getTransactionId(null);
	}

	public static IConnection getIConnection(TransactionalSession db) throws SQLException {
		if (db instanceof Transaction) {
			return ((Transaction) db).getConnection();
		} else {
			throw new IllegalArgumentException(db.getClass().getName());
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
		tableName = db.getProfile().getObjectNameToUse(tableName);
		try (WrapableConnection conn=db.get()){
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
		tableName = db.getProfile().getObjectNameToUse(tableName);
		try(WrapableConnection conn = db.get()) {
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
		}
	}

	public static LazyLoadTask getLazyTaskMarker(Map.Entry<Reference, List<AbstractRefField>> entry, Map<Reference, List<Condition>> filters, Session session) {
		return new CascadeLoaderTask(entry, filters);
	}

	public static Cache getCache(Session session) {
		return session.getCache();
	}

	public final static BooleanProperty notTouchProperty = new BooleanProperty() {
		@Override
		public String getName() {
			return "_recordUpdate";
		}

		@Override
		public Object get(Object obj) {
			return getBoolean(obj);
		}

		@Override
		public void set(Object obj, Object value) {
			if (value != null)
				setBoolean(obj, (Boolean) value);

		}

		@Override
		public Class<?> getType() {
			return boolean.class;
		}

		@Override
		public Type getGenericType() {
			return getType();
		}

		@Override
		public boolean getBoolean(Object obj) {
			DataObject d = (DataObject) obj;
			return !d._recordUpdate;
		}

		@Override
		public void setBoolean(Object obj, boolean value) {
			DataObject d = (DataObject) obj;
			d._recordUpdate = !value;
		}
	};
	
	public final static Property TouchRecord=new Property() {

		@Override
		public String getName() {
			return "___touchRecord";
		}

		@Override
		public Object get(Object obj) {
			DataObject d = (DataObject) obj;
			return d.___touchRecord;
		}

		@Override
		public void set(Object obj, Object value) {
			DataObject d = (DataObject) obj;
			d.___touchRecord=(BitSet) value;
			
		}

		@Override
		public Class<?> getType() {
			return BitSet.class;
		}

		@Override
		public Type getGenericType() {
			return BitSet.class;
		}
		
	};
	
	public final static Property LazyContext=new Property() {

		@Override
		public String getName() {
			return "lazyload";
		}

		@Override
		public Object get(Object obj) {
			DataObject d = (DataObject) obj;
			return d.lazyload;
		}

		@Override
		public void set(Object obj, Object value) {
			DataObject d = (DataObject) obj;
			d.lazyload=(ILazyLoadContext) value;
			
		}

		@Override
		public Class<?> getType() {
			return ILazyLoadContext.class;
		}

		@Override
		public Type getGenericType() {
			return ILazyLoadContext.class;
		}
		
	};


	public static boolean record(DataObject o) {
		return o._recordUpdate;
	}


	public static BitSet getTouchRecord(DataObject o) {
		return o.___touchRecord;
	}
	
	public static void setTouchRecord(DataObject o, BitSet bs) {
		o.___touchRecord = bs;
	}

	public static ILazyLoadContext getLazy(DataObject o) {
		return o.lazyload;
	}

	public static void addLazy(DataObject o, LazyLoadProcessor lazy) {
		if (o.lazyload == null) {
			o.lazyload = new LazyLoadContext(lazy);
		} else {
			throw new IllegalStateException();
		}
	}
	
}
