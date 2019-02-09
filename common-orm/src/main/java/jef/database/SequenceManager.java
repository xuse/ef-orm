package jef.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.persistence.GenerationType;
import javax.persistence.PersistenceException;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import jef.common.log.LogUtil;
import jef.database.DbMetaData.ObjectType;
import jef.database.annotation.HiloGeneration;
import jef.database.dialect.ColumnType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.AutoIncrementMapping.GenerationResolution;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.AbstractSequence;
import jef.database.meta.Column;
import jef.database.meta.Feature;
import jef.database.meta.TupleMetadata;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.database.wrapper.populator.ResultSetExtractor;
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

	/**
	 * 建sequence时对表名长度的限制，因为默认的seq名是:表名_SEQ 数据库对sequence名字长度的限制是30
	 */

	protected SequenceManager(DbClient parent) {
		this.parent = parent;
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
	public Sequence getSequence(AutoIncrementMapping fieldDef, OperateTarget client) throws SQLException {
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
					String columnName = fieldDef.getColumnName(profile, true);
					if (type == GenerationType.SEQUENCE) {
						s = createSequence(name, client, a.getPrecision(), fieldDef.getMeta().getTableName(true), columnName, a.getSeqGenerator());
					} else if (type == GenerationType.TABLE) {
						s = createTable(name, client, a.getPrecision(), fieldDef.getMeta().getTableName(true), columnName, a.getTableGenerator());
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
	 * @param fieldDef
	 * @param dbKey
	 * @return
	 * @throws SQLException
	 */
	public Sequence getSequence(AutoIncrementMapping fieldDef, String dbKey) throws SQLException {
		return getSequence(fieldDef, parent.selectTarget(dbKey));
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
					if (client == null) {
						client = this.parent.selectTarget(null);
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
	public void dropSequence(AutoIncrementMapping mapping, OperateTarget meta) throws SQLException {
		DatabaseDialect profile = meta.getProfile();
		GenerationResolution type = mapping.getGenerationType(profile);
		String datasource = mapping.getSequenceDataSource(profile);

		if (datasource != null) {// 必须绑定DataSource
			meta = (OperateTarget) meta.getSession().getSqlTemplate(StringUtils.trimToNull(datasource));
		}
		String name = mapping.getSequenceName(profile);
		if (type == GenerationResolution.SEQUENCE) {
			meta.getMetaData().dropSequence(name);
		} else if (type == GenerationResolution.TABLE) {
			String pname = JefConfiguration.get(DbCfg.DB_GLOBAL_SEQUENCE_TABLE);
			if (StringUtils.isEmpty(pname)) {
				meta.getMetaData().dropTable(name);
			} else {
				removeRecordInSeqTable(pname.trim(), name, meta);
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
		table = sqlTemplate.getMetaData().getExists(ObjectType.TABLE, table);
		if (table != null) {
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

	/**
	 * @param name
	 *            Sequence名称，用于唯一标识一个Sequence对象的标识符
	 * @param client
	 * @param length
	 *            列的长度
	 * @param tableName
	 *            使用该Sequence实现自增列的表名
	 * @param columnName
	 *            使用该Sequence实现自增列的列名
	 * @param config
	 *            注解配置
	 * @return
	 */
	private Sequence createTable(String name, OperateTarget client, int length, String tableName, String columnName, TableGenerator config) {
		TableGeneratorDef configData = new TableGeneratorDef();
		boolean singleMode = false;
		configData.name = name;
		DatabaseDialect dialect = client.getProfile();

		String globalDefaultTable = JefConfiguration.get(DbCfg.DB_GLOBAL_SEQUENCE_TABLE);
		if (StringUtils.isNotEmpty(globalDefaultTable)) { // 如果配置了DB_GLOBAL_SEQUENCE_TABLE，那么优先度最高。
			configData.table = escapeColumn(dialect, globalDefaultTable);
		} else if (config != null && StringUtils.isNotEmpty(config.table())) {// 如果指定了表名，其次
			configData.table = escapeColumn(dialect, config.table());
		} else { // 啥也没配置，采用sequenceName作为表名
			configData.table = escapeColumn(dialect, name);
			singleMode = true;// 单独模式:即整张表仅为一个Sequence使用
		}
		if (config == null || config.pkColumnName() == null) {
			if (singleMode) {
				configData.pkColumnName = null;// 无需该列
			} else {
				configData.pkColumnName = "T";// 需要键值列
			}
		} else {
			configData.pkColumnName = escapeColumn(dialect, StringUtils.trimToNull(config.pkColumnName()));
		}
		if (!singleMode) {
			configData.pkColumnValue = escapeColumn(dialect, config == null ? name : config.pkColumnValue());
		}
		// 基本信息填入
		configData.catalog = config == null ? null : config.catalog();
		configData.schema = config == null ? null : config.schema();
		configData.valueColumnName = config == null ? "V" : escapeColumn(dialect, config.valueColumnName());
		configData.initialValue = config == null ? 0 : config.initialValue();
		configData.allocationSize = config == null ? 0 : config.allocationSize();
		return new AdvSeqTableImpl(configData, tableName, columnName, this, client);
	}

	private String escapeColumn(DatabaseDialect dialect, String globalDefaultTable) {
		return DbUtils.escapeColumn(dialect, dialect.getObjectNameToUse(globalDefaultTable));
	}

	private Sequence createSequence(String seqName, OperateTarget client, int columnSize, String tableName, String columnName, SequenceGenerator config)
			throws SQLException {
		int initValue = 1;
		if (config != null) {
			seqName = config.sequenceName();
			initValue = config.initialValue();
			if (StringUtils.isNotEmpty(config.schema())) {
				seqName = config.schema().trim() + "." + seqName;
			}
		}
		return new SequenceNativeImpl(seqName, client, columnSize, tableName, columnName, initValue, this);
	}

	private static class TableGeneratorDef {
		private String name;
		private String table;
		private String catalog;
		private String schema;
		private String pkColumnName;
		private String pkColumnValue;
		private String valueColumnName;
		private int initialValue;
		private int allocationSize;

		public String name() {
			return name;
		}

		public String table() {
			return table;
		}

		public String pkColumnName() {
			return pkColumnName;
		}

		public String pkColumnValue() {
			return pkColumnValue;
		}

		public String valueColumnName() {
			return valueColumnName;
		}

		public int initialValue() {
			return initialValue;
		}
	}

	/**
	 * 第二种SQL实现，所有Sequence公用一张表
	 */
	private static final class AdvSeqTableImpl extends AbstractSequence {
		private static final String UPDATE = "UPDATE %table% SET %valueColumnName% = ? WHERE %valueColumnName% = ? AND %pkColumnName% = '%pkColumnValue%'";
		private static final String SELECT = "SELECT %valueColumnName% FROM %table% WHERE %pkColumnName% = '%pkColumnValue%'";

		private TableGeneratorDef config;
		private String rawTable;
		private String rawColumn;

		private int valueStep;
		private String update;
		private String select;
		private long last = -1;

		/*
		 * @param key Sequence名称
		 * 
		 * @param tableName 表名
		 */
		AdvSeqTableImpl(TableGeneratorDef config, String rawTable, String rawColumn, SequenceManager parent, OperateTarget target) {
			super(target, parent);
			Assert.notNull(target);
			this.config = config;
			this.rawTable = rawTable;
			this.rawColumn = rawColumn;
			this.valueStep = JefConfiguration.getInt(DbCfg.SEQUENCE_BATCH_SIZE, 20);
			if (config.allocationSize > 0) {
				valueStep = config.allocationSize;
			}
			if (valueStep < 1)
				valueStep = 20;
			if (config.pkColumnName() == null) {
				this.update = "UPDATE " + config.table() + " SET " + config.valueColumnName() + "=? WHERE " + config.valueColumnName() + "=?";
				this.select = "SELECT " + config.valueColumnName() + " FROM " + config.table();
			} else {
				this.update = StringUtils.replaceEach(UPDATE, new String[] { "%table%", "%valueColumnName%", "%pkColumnName%", "%pkColumnValue%" },
						new String[] { config.table(), config.valueColumnName(), config.pkColumnName(), config.pkColumnValue().replace("'", "''") });
				this.select = StringUtils.replaceEach(SELECT, new String[] { "%table%", "%valueColumnName%", "%pkColumnName%", "%pkColumnValue%" },
						new String[] { config.table(), config.valueColumnName(), config.pkColumnName(), config.pkColumnValue().replace("'", "''") });
			}
			if (target != null) {
				tryInit();
			}
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
			LogUtil.info("Fetch Table-Sequence {} for column [{}.{}], from {} to {}.", config.name, this.rawTable, this.rawColumn, result, nextVal);
			return result;
		}

		private long queryLast(DbMetaData conn) throws SQLException {
			long value = conn.selectBySql(select, GET_LONG_OR_TABLE_NOT_EXIST, 1, Collections.EMPTY_LIST);
			if (value == -9999L) {// 没有该条记录
				long start = super.caclStartValue(conn, null, rawTable, rawColumn, config.initialValue(), 99999999999L);
				if (config.pkColumnName() == null) {
					conn.executeSql("INSERT INTO " + config.table() + " VALUES(?)", start);
				} else {
					conn.executeSql("INSERT INTO " + config.table() + "(" + config.valueColumnName() + "," + config.pkColumnName() + ") VALUES(?,?)", start,
							config.pkColumnValue());
				}
				value = start;
			}
			return value;
		}

		public boolean isTable() {
			return true;
		}

		public String getName() {
			return config.name();
		}

		@Override
		protected boolean doInit(DbClient session, String dbKey) throws SQLException {
			DbMetaData meta = session.getMetaData(dbKey);
			String exists = meta.getExists(ObjectType.TABLE, config.table());
			if (exists == null) {
				if (ORMConfig.getInstance().isAutoCreateSequence()) {
					TupleMetadata tuple = new TupleMetadata(config.table());
					tuple.addColumn(config.valueColumnName(), new ColumnType.Int(12));
					if (config.pkColumnName() != null) {
						tuple.addColumn(config.pkColumnName(), config.pkColumnName(), new ColumnType.Varchar(64), true);
					}
					meta.createTable(tuple, null);
				} else {
					throw new PersistenceException("Table for sequence " + config.table() + " does not exist on " + meta + "!");
				}
			} else {
				List<Column> columns = meta.getColumns(exists);
				int expect = (config.pkColumnName() == null ? 1 : 2);
				if (columns.size() != expect) {
					throw new IllegalArgumentException("The sequence-table " + exists + " has " + columns.size() + " columns, but sequence " + this.config.name
							+ " needs " + expect + " columns in table.");
				}
				config.table = exists;
			}
			return true;
		}

		public boolean isRawNative() {
			return false;
		}
	}

	/**
	 * 从结果中获得单个LONG值
	 */
	private static final ResultSetExtractor<Long> GET_LONG_OR_TABLE_NOT_EXIST = new AbstractResultSetTransformer<Long>() {
		public Long transformer(IResultSet rs) throws SQLException {
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				return -9999L;
			}
		}
	};

	public void close() {
		this.clearHolders();
	}
}
