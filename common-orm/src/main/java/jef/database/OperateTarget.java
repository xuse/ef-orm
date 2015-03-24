package jef.database;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.Session.PopulateStrategy;
import jef.database.Transaction.TransactionFlag;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IManagedConnectionPool;
import jef.database.innerpool.PartitionSupport;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jdbc.JDBCTarget;
import jef.database.jdbc.result.IResultSet;
import jef.database.jdbc.result.ResultSetContainer;
import jef.database.jdbc.result.ResultSetHolder;
import jef.database.jdbc.result.ResultSetImpl;
import jef.database.jdbc.result.ResultSetWrapper;
import jef.database.jdbc.statement.ProcessablePreparedStatement;
import jef.database.jdbc.statement.ProcessableStatement;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.jsqlparser.SqlFunctionlocalization;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.meta.DbProperty;
import jef.database.meta.ITableMetadata;
import jef.database.query.EntityMappingProvider;
import jef.database.query.SqlExpression;
import jef.database.routing.jdbc.UpdateReturn;
import jef.database.routing.sql.InMemoryOperateProvider;
import jef.database.support.SqlLog;
import jef.database.wrapper.ResultIterator;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.database.wrapper.populator.ResultSetExtractor;
import jef.database.wrapper.populator.Transformer;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.MathUtils;
import jef.tools.StringUtils;

/**
 * OperateTarge描述了一个带有状态的数据库操作对象。 这一情况发生在支持路由的多数据源的场合下。
 * 
 * 
 * @author jiyi
 * 
 */
public class OperateTarget implements SqlTemplate, JDBCTarget {
	private Session session;
	private String dbkey;
	private DatabaseDialect profile;
	private IConnection conn;

	public String getDbkey() {
		return dbkey;
	}

	public Session getSession() {
		return session;
	}

	public Sequence getSequence(AutoIncrementMapping mapping) throws SQLException {
		return session.getNoTransactionSession().getSequenceManager().getSequence(mapping, this);
	}

	/**
	 * 获得Sequence对象
	 * 
	 * @param name
	 *            名称
	 * @param len
	 *            Sequence长度（位数）
	 * @return sequence
	 * @throws SQLException
	 */
	public Sequence getSequence(String name, int len) throws SQLException {
		return session.getNoTransactionSession().getSequenceManager().getSequence(name, this, len);
	}

	public OperateTarget(Session tx, String key) {
		this.session = tx;
		this.dbkey = key;
		this.profile = session.getProfile(key);
	}

	public SqlProcessor getProcessor() {
		return session.rProcessor;
	}

	public DatabaseDialect getProfile() {
		return profile;
	}

	/**
	 * 释放连接，不再持有。相当于关闭
	 */
	public void releaseConnection() {
		if (conn != null) {
			session.releaseConnection(conn);
			conn = null;
		}
	}

	public String getTransactionId() {
		return this.session.getTransactionId(dbkey);
	}

	public void notifyDisconnect(SQLException e) {
		IConnection conn = getConnection(dbkey);
		if (getProfile().isIOError(e)) {
			LogUtil.warn("IO error on connection detected. closing current connection to refersh a new db connection.");
			conn.closePhysical();
			if (session.getPool() instanceof IManagedConnectionPool) {
				((IManagedConnectionPool) session.getPool()).notifyDbDisconnect();
			}

		}
	}

	public Statement createStatement() throws SQLException {
		return profile.wrap(getConnection(dbkey).createStatement(), isJpaTx());
	}

	public Statement createStatement(ResultSetLaterProcess rslp, boolean isUpdatable) throws SQLException {
		Statement st;
		int rsType = (isUpdatable) ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY;
		int rsUpdate = isUpdatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY;
		st = getConnection(dbkey).createStatement(rsType, rsUpdate);
		if (rslp != null) {
			st = new ProcessableStatement(st, rslp);
		}
		return profile.wrap(st, isJpaTx());
	}

	/*
	 * 准备执行SQL
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return profile.wrap(getConnection(dbkey).prepareStatement(sql), isJpaTx());
	}

	/*
	 * 准备执行SQL，插入
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return profile.wrap(getConnection(dbkey).prepareStatement(sql, columnNames), isJpaTx());
	}

	/*
	 * 准备执行SQL，插入
	 */
	public PreparedStatement prepareStatement(String sql, int generateKeys) throws SQLException {
		return profile.wrap(getConnection(dbkey).prepareStatement(sql, generateKeys), isJpaTx());
	}

	/*
	 * 准备执行SQL，插入
	 */
	public PreparedStatement prepareStatement(String sql, int[] columnIndexs) throws SQLException {
		return profile.wrap(getConnection(dbkey).prepareStatement(sql, columnIndexs), isJpaTx());
	}

	/*
	 * 准备执行SQL，查询
	 */
	public PreparedStatement prepareStatement(String sql, ResultSetLaterProcess rslp, boolean isUpdatable) throws SQLException {
		PreparedStatement st;
		int rsType = (isUpdatable) ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY;
		int rsUpdate = isUpdatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY;
		st = getConnection(dbkey).prepareStatement(sql, rsType, rsUpdate);
		if (rslp != null) {
			st = new ProcessablePreparedStatement(st, rslp);
		}
		return profile.wrap(st, isJpaTx());
	}

	/*
	 * 准备执行SQL，查询
	 */
	public PreparedStatement prepareStatement(String sql, int rsType, int concurType, int hold) throws SQLException {
		return profile.wrap(getConnection(dbkey).prepareStatement(sql, rsType, concurType, hold), isJpaTx());
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return getConnection(dbkey).prepareCall(sql);
	}

	public boolean isResultSetHolderTransaction() {
		if (session instanceof Transaction) {
			Transaction trans = (Transaction) session;
			return TransactionFlag.ResultHolder == trans.getTransactionFlag();
		}
		return false;
	}

	public void closeTx() {
		if (session instanceof Transaction) {
			Transaction tx = (Transaction) session;
			if (tx.isReadonly()) {
				tx.close();
			} else {
				tx.commit(true);
			}
		}
	}

	IConnection getRawConnection() {
		return getConnection(dbkey);
	}

	<T> List<T> populateResultSet(IResultSet rsw, EntityMappingProvider mapping, Transformer transformer) throws SQLException {
		return session.populateResultSet(rsw, mapping, transformer);
	}

	private IConnection getConnection(String dbkey2) {
		if (conn == null) {
			try {
				conn = session.getConnection();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		conn.setKey(dbkey2);
		return conn;
	}

	public String getDbName() {
		return session.getDbName(dbkey);
	}

	public int executeSqlBatch(String sql, List<?>... params) throws SQLException {
		return innerExecuteSqlBatch(sql, params);
	}

	final int innerExecuteSqlBatch(String sql, List<?>... params) throws SQLException {
		SqlLog log = ORMConfig.getInstance().newLogger(sql.length() + 64);
		log.append(sql);
		PreparedStatement st = null;
		long start = System.currentTimeMillis();
		try {
			st = prepareStatement(sql);
			st.setQueryTimeout(ORMConfig.getInstance().getUpdateTimeout() * 2);// 批量操作允许更多的时间。
			int maxBatchlog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < params.length; i++) {
				session.getListener().beforeSqlExecute(sql, params[i]);
				BindVariableContext context = new BindVariableContext(st, getProfile(), log);
				BindVariableTool.setVariables(context, params[i]);
				st.addBatch();
				if (log.isDebug()) {
					log.output();
					if (i >= maxBatchlog) {
						log = SqlLog.DUMMY;
					}
				}
				session.checkCacheUpdate(sql, params[i]);
			}
			int[] result = st.executeBatch();
			long dbAccess = System.currentTimeMillis();
			int total = MathUtils.sum(result);

			log.directLog(StringUtils.concat("Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms) |", getTransactionId()));
			// 执行回调
			for (int i = 0; i < params.length; i++) {
				session.getListener().afterSqlExecuted(sql, i < result.length ? result[i] : 1, params[i]);
			}
			return total;
		} catch (SQLException e) {
			DbUtils.processError(e, sql, this);
			throw e;
		} finally {
			if (st != null)
				st.close();
			releaseConnection();
		}
	}

	public final UpdateReturn innerExecuteUpdate(String sql, List<Object> ps, GenerateKeyReturnOper keyOper) throws SQLException {
		Object[] params = ps.toArray();

		session.getListener().beforeSqlExecute(sql, params);
		SqlLog sb = ORMConfig.getInstance().newLogger();

		long start = System.currentTimeMillis();
		PreparedStatement st = null;
		UpdateReturn result;
		long dbAccess;
		int total;
		sb.append(sql).append(this);
		try {
			st = keyOper.prepareStatement(this, sql);
			st.setQueryTimeout(ORMConfig.getInstance().getUpdateTimeout());
			if (!ps.isEmpty()) {
				BindVariableContext context = new BindVariableContext(st, getProfile(), sb);
				BindVariableTool.setVariables(context, ps);
			}
			total = st.executeUpdate();
			result = new UpdateReturn(total);
			dbAccess = System.currentTimeMillis();
			keyOper.getGeneratedKey(result, st);
			if (total > 0) {
				session.checkCacheUpdate(sql, ps);
			}
		} catch (SQLException e) {
			DbUtils.processError(e, sql, this);
			throw e;
		} finally {
			sb.output();
			DbUtils.close(st);
			releaseConnection();
		}
		sb.directLog(StringUtils.concat("Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms) |", getTransactionId()));
		session.getListener().afterSqlExecuted(sql, total, params);
		return result;
	}

	public final <T> T innerSelectBySql(String sql, ResultSetExtractor<T> rst, List<?> objs, InMemoryOperateProvider lazy) throws SQLException {
		PreparedStatement st = null;
		ResultSet rs = null;
		SqlLog sb = ORMConfig.getInstance().newLogger();
		try {
			sb.ensureCapacity(sql.length() + 30 + objs.size() * 20);
			sb.append(sql).append(this);
			long start = System.currentTimeMillis();
			ResultSetLaterProcess isReverse = lazy == null ? null : lazy.getRsLaterProcessor();
			st = prepareStatement(sql, isReverse, false);

			BindVariableContext context = new BindVariableContext(st, getProfile(), sb);
			BindVariableTool.setVariables(context, objs);
			rst.apply(st);
			rs = st.executeQuery();
			long dbAccessed = System.currentTimeMillis();
			if (lazy != null && lazy.hasInMemoryOperate()) {
				rs = ResultSetContainer.toInMemoryProcessorResultSet(lazy, new ResultSetHolder(this, st, rs));
			}
			T t;
			if (rst.autoClose()) {
				t = rst.transformer(new ResultSetImpl(rs, getProfile()));
			} else {
				t = rst.transformer(new ResultSetWrapper(this, st, rs));
			}
			rst.appendLog(sb, t);
			sb.append("\tTime cost([DbAccess]:", dbAccessed - start).append("ms, [Populate]:", System.currentTimeMillis() - dbAccessed).append("ms").append(this);
			return t;
		} catch (SQLException e) {
			DbUtils.processError(e, sql, this);
			throw e;
		} finally {
			sb.output();
			if (rst.autoClose()) {
				DbUtils.close(rs);
				DbUtils.close(st);
				releaseConnection();
			}
		}
	}

	// ///////////////////////////////SqlTemplate /////////////////////////
	public <T> NativeQuery<T> createNativeQuery(String sqlString, Class<T> clz) {
		return new NativeQuery<T>(this, sqlString, new Transformer(clz));
	}

	<T> NativeQuery<T> createNativeQuery(NQEntry nc, Class<T> resultClz) {
		return new NativeQuery<T>(this, nc.get(this.profile.getName()), new Transformer(resultClz));
	}

	public <T> NativeQuery<T> createNativeQuery(String sqlString, ITableMetadata meta) {
		NativeQuery<T> q = new NativeQuery<T>(this, sqlString, new Transformer(meta));
		return q;
	}

	<T> NativeQuery<T> createNativeQuery(NQEntry nc, ITableMetadata meta) {
		NativeQuery<T> q = new NativeQuery<T>(this, nc.get(this.profile.getName()), new Transformer(meta));
		return q;
	}

	public NativeCall createNativeCall(String procedureName, Type... paramClass) throws SQLException {
		return new NativeCall(this, procedureName, paramClass, false);
	}

	public NativeCall createAnonymousNativeCall(String callString, Type... paramClass) throws SQLException {
		return new NativeCall(this, callString, paramClass, true);
	}

	public <T> NativeQuery<T> createQuery(String jpql, Class<T> resultClass) throws SQLException {
		NativeQuery<T> query = new NativeQuery<T>(this, jpql, new Transformer(resultClass));
		query.setIsNative(false);
		return query;
	}

	public <T> PagingIterator<T> pageSelectBySql(String sql, Class<T> clz, int pageSize) throws SQLException {
		return new PagingIteratorSqlImpl<T>(sql, pageSize, new Transformer(clz), this);
	}

	public <T> PagingIterator<T> pageSelectBySql(String sql, ITableMetadata meta, int pageSize) throws SQLException {
		return new PagingIteratorSqlImpl<T>(sql, pageSize, new Transformer(meta), this);
	}

	public int executeJPQL(String jpql, Map<String, Object> params) throws SQLException {
		NativeQuery<?> nq = this.createQuery(jpql, null);
		if (params != null) {
			nq.setParameterMap(params);
		}
		return nq.executeUpdate();
	}

	public <T> List<T> selectByJPQL(String jpql, Class<T> resultClz, Map<String, Object> params) throws SQLException {
		NativeQuery<T> nq = this.createQuery(jpql, resultClz);
		if (params != null) {
			nq.setParameterMap(params);
		}
		return nq.getResultList();
	}

	public long countBySql(String countSql, Object... params) throws SQLException {
		long start = System.currentTimeMillis();
		Long num = innerSelectBySql(countSql, ResultSetExtractor.GET_FIRST_LONG, Arrays.asList(params), null);
		if (ORMConfig.getInstance().isDebugMode()) {
			long dbAccess = System.currentTimeMillis();
			LogUtil.show(StringUtils.concat("Count:", String.valueOf(num), "\t [DbAccess]:", String.valueOf(dbAccess - start), "ms) |", getTransactionId()));
		}
		return num;
	}

	public int executeSql(String sql, Object... params) throws SQLException {
		return innerExecuteUpdate(sql, Arrays.asList(params), GenerateKeyReturnOper.NONE).getAffectedRows();
	}

	public final <T> List<T> selectBySql(String sql, Class<T> resultClz, Object... params) throws SQLException {
		return selectBySql(sql, new Transformer(resultClz), null, params);
	}

	public final <T> List<T> selectBySql(String sql, Transformer transformer, IntRange range, Object... params) throws SQLException {
		BindSql bs = range == null ? new BindSql(sql) : getProfile().getLimitHandler().toPageSQL(sql, range.toStartLimitSpan());
		long start = System.currentTimeMillis();
		TransformerAdapter<T> sqlTransformer = new TransformerAdapter<T>(transformer, this);
		List<T> list = innerSelectBySql(bs.getSql(), sqlTransformer, Arrays.asList(params), bs);
		if (ORMConfig.getInstance().isDebugMode()) {
			long dbAccess = sqlTransformer.dbAccess;
			LogUtil.show(StringUtils.concat("Result Count:", String.valueOf(list.size()), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms, [Populate]:", String.valueOf(System.currentTimeMillis() - dbAccess), "ms) |", getTransactionId()));
		}
		return list;
	}

	public final <T> T loadBySql(String sql, Class<T> t, Object... params) throws SQLException {
		TransformerAdapter<T> rst = new TransformerAdapter<T>(new Transformer(t), this);
		rst.setMaxRows(2);
		long start = System.currentTimeMillis();
		List<T> result = innerSelectBySql(sql, rst, Arrays.asList(params), null);
		if (ORMConfig.getInstance().isDebugMode()) {
			long dbAccess = rst.dbAccess;
			LogUtil.show(StringUtils.concat("Result Count:", String.valueOf(result.size()), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms, [Populate]:", String.valueOf(System.currentTimeMillis() - dbAccess), "ms) |", getTransactionId()));
		}
		if (result.size() > 1) {
			throw new IllegalArgumentException("得到多个结果:" + result.size());
		} else if (result.isEmpty()) {
			return null;
		} else {
			return result.get(0);
		}
	}

	public final <T> ResultIterator<T> iteratorBySql(String sql, Transformer transformers, int maxReturn, int fetchSize, Object... objs) throws SQLException {
		TransformerIteratrAdapter<T> t = new TransformerIteratrAdapter<T>(transformers, this);
		t.setMaxRows(maxReturn);
		t.setFetchSize(fetchSize);
		return this.innerSelectBySql(sql, t, Arrays.asList(objs), null);
	}

	final <T> ResultIterator<T> iteratorBySql(String sql, Transformer transformers, int maxReturn, int fetchSize, InMemoryOperateProvider lazy, Object... objs) throws SQLException {
		TransformerIteratrAdapter<T> t = new TransformerIteratrAdapter<T>(transformers, this);
		t.setMaxRows(maxReturn);
		t.setFetchSize(fetchSize);
		return this.innerSelectBySql(sql, t, Arrays.asList(objs), lazy);
	}

	public DbMetaData getMetaData() throws SQLException {
		return session.getNoTransactionSession().getMetaData(dbkey);
	}

	/**
	 * 在这里支持内存混合处理
	 * 
	 * @author jiyi
	 * 
	 * @param <T>
	 */
	public static class TransformerAdapter<T> extends AbstractResultSetTransformer<List<T>> {
		final Transformer transformers;
		public long dbAccess;
		private OperateTarget db;

		TransformerAdapter(Transformer t, OperateTarget db) {
			this.transformers = t;
			this.db = db;
		}

		@Override
		public PopulateStrategy[] getStrategy() {
			return transformers.getStrategy();
		}

		public List<T> transformer(IResultSet rs) throws SQLException {
			dbAccess = System.currentTimeMillis();
			return db.populateResultSet(rs, null, transformers);
		}

		public Session getSession() {
			return db.session;
		}

		@Override
		public void appendLog(SqlLog log, List<T> result) {
			log.append("\nResult Count:", result.size());
		}
	}

	public static class TransformerIteratrAdapter<T> extends AbstractResultSetTransformer<ResultIterator<T>> {
		final Transformer transformers;
		private OperateTarget db;

		TransformerIteratrAdapter(Transformer t, OperateTarget db) {
			this.transformers = t;
			this.db = db;
		}

		@Override
		public PopulateStrategy[] getStrategy() {
			return transformers.getStrategy();
		}

		@SuppressWarnings("unchecked")
		@Override
		public ResultIterator<T> transformer(IResultSet rs) throws SQLException {
			return new ResultIterator.Impl<T>(db.session.iterateResultSet(rs, null, transformers), rs);
		}

		@Override
		public boolean autoClose() {
			return false;
		}

		@Override
		public void appendLog(SqlLog log, ResultIterator<T> result) {
			log.append("\nResult: Iterator");
		}
	}

	public <T> T getExpressionValue(DbFunction func, Class<T> clz, Object... params) throws SQLException {
		SqlExpression ex = func(func, params);
		return getExpressionValue(ex.toString(), clz);
	}

	public <T> T getExpressionValue(String expression, Class<T> clz, Object... params) throws SQLException {
		String sql = "select " + expression + " from dual";
		StSqlParser parser = new StSqlParser(new StringReader(sql));
		List<SelectItem> sts;
		try {
			sts = parser.PlainSelect().getSelectItems();
		} catch (ParseException e) {
			throw new SQLException("ParseError:[" + sql + "] Detail:" + e.getMessage());
		}
		// 进行本地语言转化
		DatabaseDialect dialect = this.profile;
		SqlFunctionlocalization visitor = new SqlFunctionlocalization(dialect, this);
		for (SelectItem item : sts) {
			item.accept(visitor);
		}
		// 形成语句
		String template = dialect.getProperty(DbProperty.SELECT_EXPRESSION);
		String exps = StringUtils.join(sts, ',');
		if (template == null) {
			sql = "SELECT " + exps;
		} else {
			sql = String.format(template, exps);
		}
		return this.loadBySql(sql, clz, params);
	}

	public SqlExpression func(DbFunction func, Object... params) {
		return new SqlExpression(getProfile().getFunction(func, params));
	}

	private boolean isJpaTx() {
		return session.isJpaTx();
	}

	public PartitionSupport getPartitionSupport() {
		return session.getPartitionSupport();
	}

	public OperateTarget getTarget(String database) {
		if (StringUtils.equals(dbkey, database)) {
			return this;
		}
		return session.selectTarget(database);
	}
}
