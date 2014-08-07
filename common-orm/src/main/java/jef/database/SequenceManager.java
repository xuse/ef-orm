package jef.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import jef.database.DbMetaData.ObjectType;
import jef.database.annotation.HiloGeneration;
import jef.database.dialect.ColumnType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.meta.AbstractSequence;
import jef.database.meta.Feature;
import jef.database.meta.TupleMetadata;
import jef.database.wrapper.populator.ResultSetTransformer;
import jef.tools.Assert;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;

/**
 * Sequence管理接口。
 * 
 * @author jiyi
 * 
 */
public final class SequenceManager {

	private final HashMap<String, Sequence> holders = new HashMap<String, Sequence>();
	private boolean hiloFlag = JefConfiguration.getBoolean(DbCfg.DB_AUTOINCREMENT_HILO, false);
	private DbClient parent;
	public ExecutorService es= new ThreadPoolExecutor(2, 8,60000L, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());
	/**
	 * 建sequence时对表名长度的限制，因为默认的seq名是:表名_SEQ 数据库对sequence名字长度的限制是30
	 */

	protected SequenceManager(DbClient parent) {
		this.parent=parent;
	}

	/**
	 * 获取Sequence，无论什么数据库都可以获取Sequence。如果是原生支持Sequence的数据库，会返回原生的实现；如果非原生支持，
	 * 会返回用数据表的模拟实现。
	 * 
	 * @param seqName
	 * @param client
	 * @return
	 * @throws SQLException
	 */
	public Sequence getSequence(AutoIncrementMapping<?> fieldDef, OperateTarget client) throws SQLException {
		if (fieldDef == null)
			return null;
		String name = fieldDef.getSequenceName(client.getProfile());
		Sequence s = holders.get(name);
		if (s == null) {
			synchronized (holders) {
				s = holders.get(name);
				if (s == null) {// 双重检查锁定: 防止被多线程的情况下初始化多次
					DatabaseDialect profile = client.getProfile();
					AutoIncrement a = (AutoIncrement) fieldDef.get();
					GenerationType type = a.getGenerationType(profile, false);

					// 绑定DataSource
					String datasource = fieldDef.getSequenceDataSource(profile);
					if (datasource != null) {// 必须绑定DataSource
						client = (OperateTarget) client.getSession().getSqlTemplate(StringUtils.trimToNull(datasource));
					}
					if (type == GenerationType.SEQUENCE) {
						s = createSequence(name, client, a.getLength(), fieldDef.getMeta().getTableName(true), fieldDef.columnName(), a.getSeqGenerator());
					} else if (type == GenerationType.TABLE) {
						s = createTable(name, client, a.getLength(), fieldDef.getMeta().getTableName(true), fieldDef.columnName(), a.getTableGenerator());
					}
					holders.put(name, wrapForHilo((AbstractSequence) s, a.getHiloConfig()));
				}
			}
		}
		return s;
	}

	/**
	 * 获取Sequence，无论什么数据库都可以获取Sequence。如果是原生支持Sequence的数据库，会返回原生的实现；如果非原生支持，
	 * 会返回用数据表的模拟实现。
	 * 
	 * @param seqName
	 * @param client
	 * @param length
	 * @return
	 * @throws SQLException
	 */
	public Sequence getSequence(String seqName, OperateTarget client, int length) throws SQLException {
		Sequence s = holders.get(seqName);
		if (s == null) {
			synchronized (holders) {
				s = holders.get(seqName);
				if (s == null) {// 双重检查锁定: 防止被多线程的情况下初始化多次
					if(client==null){
						client=this.parent.asOperateTarget(null);
					}
					if (client.getProfile().has(Feature.SUPPORT_SEQUENCE)) {
						s = createSequence(seqName, client, length, null, null, null);
					} else {
						s = createTable(seqName, client, length, null, null, null);
					}
					holders.put(seqName, s);
				}
			}
		}
		return s;
	}

	/**
	 * 删除Sequence
	 * 
	 * @param mapping
	 * @param meta
	 * @throws SQLException
	 */
	public void dropSequence(AutoIncrementMapping<?> mapping, OperateTarget meta) throws SQLException {
		DatabaseDialect profile = meta.getProfile();
		GenerationType type = mapping.getGenerationType(profile);
		String datasource = mapping.getSequenceDataSource(profile);

		if (datasource != null) {// 必须绑定DataSource
			meta = (OperateTarget) meta.getSession().getSqlTemplate(StringUtils.trimToNull(datasource));
		}
		String name = mapping.getSequenceName(profile);
		if (type == GenerationType.SEQUENCE) {
			meta.getMetaData().dropSequence(name);
		} else if (type == GenerationType.TABLE) {
			String pname = JefConfiguration.get(DbCfg.DB_PUBLIC_SEQUENCE_TABLE);
			if (StringUtils.isEmpty(pname)) {
				meta.getMetaData().dropTable(name);
			} else {
				removeRecordInSeqTable(pname, name, meta);
			}
		}
	}

	/**
	 * must clean cache on junit 4 tests....
	 */
	public void clearHolders() {
		for (Sequence s : holders.values()) {
			s.clear();
		}
		holders.clear();
	}

	private static boolean removeRecordInSeqTable(String table, String key, OperateTarget sqlTemplate) throws SQLException {
		if (StringUtils.isEmpty(table) || StringUtils.isEmpty(key)) {
			throw new IllegalArgumentException();
		}
		if(sqlTemplate.getMetaData().exists(ObjectType.TABLE, table)){
			String sql = "delete from " + table + " where T=?";
			int i = sqlTemplate.executeSql(sql, key);
			return i > 0;	
		}
		return false;
		
	}

	private Sequence wrapForHilo(AbstractSequence s, HiloGeneration hilo) {
		if (hilo != null && hilo.maxLo() > 1) {
			if (hiloFlag || hilo.always()) {
				if (s instanceof AbstractSequence) {// 减少Cache大小
					AbstractSequence as = (AbstractSequence) s;
					as.setCacheSize(as.getCacheSize() / hilo.maxLo());
				}
				s = new SequenceHiloGenerator(s, hilo.maxLo());
			}
		}
		return s;
	}

	private Sequence createTable(String name, OperateTarget client, int length, String tableName, String columnName, TableGenerator config) {
		String pname = JefConfiguration.get(DbCfg.DB_PUBLIC_SEQUENCE_TABLE);
		if (StringUtils.isEmpty(pname)) {
			return new SeqTableImpl(client, name, config, tableName, columnName,this);
		} else {
			return new AdvSeqTableImpl(client, name, config, tableName, columnName,this);
		}
	}

	private Sequence createSequence(String seqName, OperateTarget client, int columnSize, String tableName, String columnName, SequenceGenerator config) throws SQLException {
		int initValue = 1;
		if (config != null) {
			seqName = config.sequenceName();
			initValue = config.initialValue();
			if (StringUtils.isNotEmpty(config.schema())) {
				seqName = config.schema().trim() + "." + seqName;
			}
		}
		return new SequenceNativeImpl(seqName, client, columnSize, tableName, columnName, initValue,this);
	}

	/**
	 * 第一种SQL实现，每个Sequence一张表
	 */
	private static final class SeqTableImpl extends AbstractSequence {
		static TupleMetadata seqtable;
		static {
			seqtable = new TupleMetadata("SEQ");
			seqtable.addColumn("V", new ColumnType.Int(12));
		}
		private int valueStep;
		private String table;

		private String rawTable;
		private String rawColumn;
		private int initValue;

		private long last = -1;
		private String update;
		private String select;

		/*
		 * @param rawSeqName Sequence表名称
		 * 
		 * @param tableName 表名
		 * 
		 * @param columnName 列名
		 * 
		 * @throws SQLException
		 */
		SeqTableImpl(OperateTarget meta, String seqTable, TableGenerator config, String rawTableName, String rawColumnName,SequenceManager parent) {
			super(meta,parent);
			Assert.notNull(meta);
			this.table = seqTable;
			this.rawTable = rawTableName;
			this.rawColumn = rawColumnName;
			if (config != null)
				this.initValue = config.initialValue();
			String valueColumn = config == null ? "V" : config.valueColumnName();

			this.update = "UPDATE " + table + " SET " + valueColumn + "=? WHERE " + valueColumn + "=?";
			this.select = "SELECT " + valueColumn + " FROM " + table;
			this.valueStep = JefConfiguration.getInt(DbCfg.SEQUENCE_BATCH_SIZE, 20);// 每次取一批
			if (valueStep < 1)
				valueStep = config == null ? 20 : config.allocationSize();
		}

		@Override
		protected long getFirstAndPushOthers(int num, DbClient conn, String dbKey) throws SQLException {
			DbMetaData meta = conn.getMetaData(dbKey);
			if (last < 0) {
				last = queryLast(meta);
			}
			long nextVal = last + valueStep;
			int updated = conn.executeSql(update, nextVal, last);
			while (updated == 0) { // 基于CAS操作的乐观锁,
				last = queryLast(meta);
				nextVal = last + valueStep;
				updated = conn.executeSql(update, nextVal, last);
			}
			long result = last + 1;
			super.pushRange(last + 2, nextVal);
			last = nextVal;
			return result;
		}

		private long queryLast(DbMetaData conn) throws SQLException {
			long value = conn.selectBySql(select, GET_LONG_OR_TABLE_NOT_EXIST, 1, Collections.EMPTY_LIST);
			if (value == -9999L) {
				long start = super.caclStartValue(conn, null, rawTable, rawColumn, initValue, 99999999999L);
				conn.executeSql("INSERT INTO " + table + " VALUES(?)", Arrays.asList(start));
				value = 0;
			}
			return value;
		}

		public boolean isTable() {
			return true;
		}

		public String getName() {
			return table;
		}

		@Override
		protected boolean doInit(DbClient session, String dbKey) throws SQLException {
			DbMetaData meta = session.getMetaData(dbKey);
			if (!meta.exists(ObjectType.TABLE, table)) {
				int start = 0;
				meta.createTable(seqtable, table);
				meta.executeSql("INSERT INTO " + table + " VALUES(?)", Arrays.asList(start));
			}
			return true;
		}

		public boolean isRawNative() {
			return false;
		}
	}

	/**
	 * 第二种SQL实现，所有Sequence公用一张表
	 */
	private static final class AdvSeqTableImpl extends AbstractSequence {
		static String publicTableName = JefConfiguration.get(DbCfg.DB_PUBLIC_SEQUENCE_TABLE, "JEF_SEQUENCES");
		static TupleMetadata seqtable;
		static {
			seqtable = new TupleMetadata(publicTableName);
			seqtable.addColumn("V", new ColumnType.Int(12));
			seqtable.addColumn("T","T", new ColumnType.Varchar(64),true);
			
		}
		private String table;
		private String key;
		private int valueStep;

		private String rawTable;
		private String rawColumn;
		private int initValue;

		private String update;
		private String select;
		private long last = -1;

		/*
		 * @param key Sequence名称
		 * 
		 * @param tableName 表名
		 */
		AdvSeqTableImpl(OperateTarget meta, String key, TableGenerator config, String rawTable, String rawColumn,SequenceManager parent) {
			super(meta,parent);
			Assert.notNull(meta);
			this.table = publicTableName;
			this.key = key;

			this.rawTable = rawTable;
			this.rawColumn = rawColumn;
			this.initValue = config==null?0:config.initialValue();

			this.valueStep = JefConfiguration.getInt(DbCfg.SEQUENCE_BATCH_SIZE, 20);
			if (valueStep < 1)
				valueStep = 20;

			this.update = "UPDATE " + table + " SET V=? WHERE V=? AND T='" + key + "'";
			this.select = "SELECT V FROM " + table + " WHERE T='" + key + "'";
		}

		@Override
		protected long getFirstAndPushOthers(int num, DbClient conn, String dbKey) throws SQLException {
			DbMetaData meta = conn.getNoTransactionSession().getMetaData(dbKey);
			if (last < 0) {
				last = queryLast(meta);
			}
			long nextVal = last + valueStep;
			int updated = conn.executeSql(update, nextVal, last);
			while (updated == 0) { // 基于CAS操作的乐观锁,
				last = queryLast(meta);
				nextVal = last + valueStep;
				updated = conn.executeSql(update, nextVal, last);
			}
			long result = last + 1;
			super.pushRange(last + 2, nextVal);
			last = nextVal;
			return result;
		}

		private long queryLast(DbMetaData conn) throws SQLException {
			long value = conn.selectBySql(select, GET_LONG_OR_TABLE_NOT_EXIST, 1, Collections.EMPTY_LIST);
			if (value == -9999L) {
				long start = super.caclStartValue(conn, null, rawTable, rawColumn, initValue, 99999999999L);
				conn.executeSql("INSERT INTO " + table + "(V,T) VALUES(?,?)", Arrays.<Object> asList(start, key));
				value = 0;
			}
			return value;
		}

		public boolean isTable() {
			return true;
		}

		public String getName() {
			return key;
		}

		@Override
		protected boolean doInit(DbClient session, String dbKey) throws SQLException {
			DbMetaData meta = session.getMetaData(dbKey);
			if (!meta.exists(ObjectType.TABLE, table)) {
				meta.createTable(seqtable, table);
			}
			return true;
		}

		public boolean isRawNative() {
			return false;
		}
	}

	private static final ResultSetTransformer<Long> GET_LONG_OR_TABLE_NOT_EXIST = new ResultSetTransformer<Long>() {
		public Long transformer(ResultSet rs, DatabaseDialect db) throws SQLException {
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				return -9999L;
			}
		}
	};
	
	public void close(){
		es.shutdown();
		this.clearHolders();
	}
}
