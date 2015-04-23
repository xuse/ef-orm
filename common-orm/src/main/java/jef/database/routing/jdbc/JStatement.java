package jef.database.routing.jdbc;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jef.database.DbUtils;
import jef.database.innerpool.JConnection;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.parser.StSqlParser;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class JStatement implements java.sql.Statement {
	private static final Logger log = LoggerFactory.getLogger(JStatement.class);

	private JConnection conn;

	/**
	 * 经过计算后的结果集，允许使用 getResult函数调用.
	 * 
	 * 一个statement只允许有一个结果集
	 */
	protected ResultSet resultSet;

	/**
	 * 插入等操作产生的自增主键
	 */
	protected UpdateReturn updateReturn;

	/**
	 * 貌似是只有存储过程中会出现多结果集 因此不支持
	 */
	protected boolean moreResults;

	/**
	 * 判断当前statment 是否是关闭的
	 */
	protected boolean closed;
	/**
	 * 
	 */
	private int resultSetType = -1;
	/**
	 * 
	 */
	private int resultSetConcurrency = -1;
	/**
	 * 
	 */
	private int resultSetHoldability = -1;
	/**
	 * 超时时间
	 */
	protected int queryTimeout = 0;
	/**
	 * 最大结果限制
	 */
	protected int maxRows = 0;
	/**
	 * 获取批大小
	 */
	protected int fetchSize = 0;

	protected List<String> batchedArgs;

	public JStatement(JConnection routingConnection) {
		this.conn = routingConnection;
	}

	public JStatement(JConnection routingConnection, int resultsetType, int resultSetConcurrency, int resultSetHoldability) {
		this.conn = routingConnection;
		this.resultSetType = resultsetType;
		this.resultSetConcurrency = resultSetConcurrency;
		this.resultSetHoldability = resultSetHoldability;
	}

	public int executeUpdate(String sql) throws SQLException {
		return executeUpdateInternal(sql, GenerateKeyReturnOper.NONE, null);
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return executeUpdateInternal(sql, autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS ? GenerateKeyReturnOper.RETURN_KEY : GenerateKeyReturnOper.NONE, null);
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return executeUpdateInternal(sql, new GenerateKeyReturnOper.ReturnByColumnIndex(columnIndexes), null);
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return executeUpdateInternal(sql, new GenerateKeyReturnOper.ReturnByColumnNames(columnNames), null);
	}

	private boolean executeInternal(String sql, GenerateKeyReturnOper oper) throws SQLException {
		if (SqlTypeParser.isQuerySql(sql)) {
			this.resultSet = executeQuery(sql);
			return true;
		} else {
			executeUpdateInternal(sql, oper, Collections.EMPTY_LIST);
			return false;
		}
	}

	public boolean execute(String sql) throws SQLException {
		return executeInternal(sql, GenerateKeyReturnOper.NONE);
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return executeInternal(sql, autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS ? GenerateKeyReturnOper.RETURN_KEY : GenerateKeyReturnOper.NONE);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return executeInternal(sql, new GenerateKeyReturnOper.ReturnByColumnIndex(columnIndexes));
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return executeInternal(sql, new GenerateKeyReturnOper.ReturnByColumnNames(columnNames));
	}

	public void addBatch(String sql) throws SQLException {
		checkClosed();
		if (batchedArgs == null) {
			batchedArgs = new LinkedList<String>();
		}
		if (sql != null) {
			batchedArgs.add(sql);
		}
	}

	public void clearBatch() throws SQLException {
		checkClosed();
		if (batchedArgs != null) {
			batchedArgs.clear();
		}
	}

	public void close() throws SQLException {
		if (closed) {
			return;
		}
		if (updateReturn != null) {
			updateReturn.close();
		}
		this.updateReturn = null;
		closed = true;
		if (resultSet != null) {
			DbUtils.close(resultSet);
			resultSet = null;
		}
	}

	public Connection getConnection() throws SQLException {
		return conn;
	}

	/**
	 * 以下为不支持的方法
	 */
	public int getFetchDirection() throws SQLException {
		throw new UnsupportedOperationException("getFetchDirection");
	}

	public int getFetchSize() throws SQLException {
		return this.fetchSize;
	}

	public int getMaxFieldSize() throws SQLException {
		throw new UnsupportedOperationException("getMaxFieldSize");
	}

	public int getMaxRows() throws SQLException {
		return this.maxRows;
	}

	public boolean getMoreResults() throws SQLException {
		return moreResults;
	}

	public int getQueryTimeout() throws SQLException {
		return queryTimeout;
	}

	public void setQueryTimeout(int queryTimeout) throws SQLException {
		this.queryTimeout = queryTimeout;
	}

	public void setCursorName(String cursorName) throws SQLException {
		throw new UnsupportedOperationException("setCursorName");
	}

	public void setEscapeProcessing(boolean escapeProcessing) throws SQLException {
		throw new UnsupportedOperationException("setEscapeProcessing");
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public boolean getMoreResults(int current) throws SQLException {
		throw new UnsupportedOperationException("getMoreResults");
	}

	public ResultSet getResultSet() throws SQLException {
		return resultSet;
	}

	public int getResultSetConcurrency() throws SQLException {
		return resultSetConcurrency;
	}

	public int getResultSetHoldability() throws SQLException {
		return resultSetHoldability;
	}

	public int getResultSetType() throws SQLException {
		return resultSetType;
	}

	public int getUpdateCount() throws SQLException {
		if (updateReturn == null)
			return 0;
		return updateReturn.getAffectedRows();
	}

	public void setFetchDirection(int fetchDirection) throws SQLException {
		throw new UnsupportedOperationException("setFetchDirection");
	}

	public void setFetchSize(int fetchSize) throws SQLException {
		this.fetchSize = fetchSize;
	}

	public void setMaxFieldSize(int maxFieldSize) throws SQLException {
		throw new UnsupportedOperationException("setMaxFieldSize");
	}

	public void setMaxRows(int maxRows) throws SQLException {
		this.maxRows = maxRows;
	}

	/*
	 * 这也是在PreparedStatement上才支持的
	 */
	public ResultSet getGeneratedKeys() throws SQLException {
		if (updateReturn != null) {
			return updateReturn.getGeneratedKeys();
		} else {
			throw new UnsupportedOperationException("getGeneratedKeys");
		}
	}

	public void cancel() throws SQLException {
		throw new UnsupportedOperationException("cancel");
	}

	public int getQueryTimeOut() {
		return queryTimeout;
	}

	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	public void setResultSetConcurrency(int resultSetConcurrency) {
		this.resultSetConcurrency = resultSetConcurrency;
	}

	public void setResultSetHoldability(int resultSetHoldability) {
		this.resultSetHoldability = resultSetHoldability;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.getClass().isAssignableFrom(iface);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		try {
			return (T) this;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	public void setPoolable(boolean poolable) throws SQLException {
		throw new SQLException("not support exception");
	}

	public boolean isPoolable() throws SQLException {
		return false;
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		return executeQueryInternal(sql, Collections.EMPTY_LIST);
	}

	/*
	 * 
	 */
	protected ResultSet executeQueryInternal(String sql, List<ParameterContext> params) throws SQLException {
		jef.database.jsqlparser.visitor.Statement st = parse(sql);
		SQLExecutor executor;
		if (st == null) {
			executor = new SimpleSQLExecutor(conn.get().selectTarget(null), sql);
		} else {
			executor = new RoutingSQLExecutor(conn.get(), st);
		}
		executor.setFetchSize(this.fetchSize);
		executor.setMaxResults(this.maxRows);
		executor.setQueryTimeout(this.queryTimeout);
		return executor.getResultSet(resultSetType, resultSetConcurrency, resultSetHoldability, params);

	}

	protected int[] executeBatchInternal(String sql, GenerateKeyReturnOper oper, List<List<ParameterContext>> params) throws SQLException {
		jef.database.jsqlparser.visitor.Statement st = parse(sql);
		SQLExecutor se;
		if (st == null) { // 无法解析，直接运行
			se = new SimpleSQLExecutor(conn.get().selectTarget(null), sql);
		} else {
			se = new RoutingSQLExecutor(conn.get(), st);
		}
		if (queryTimeout > 0)
			se.setQueryTimeout(queryTimeout);
		BatchReturn br = se.executeBatch(oper, params);
		updateReturn = br;
		return br.getBatchResult();
	}

	protected int executeUpdateInternal(String sql, GenerateKeyReturnOper oper, List<ParameterContext> params) throws SQLException {
		jef.database.jsqlparser.visitor.Statement st = parse(sql);
		SQLExecutor se;
		if (st == null) { // 无法解析，直接运行
			se = new SimpleSQLExecutor(conn.get().selectTarget(null), sql);
		} else {
			se = new RoutingSQLExecutor(conn.get(), st);
		}
		if (queryTimeout > 0)
			se.setQueryTimeout(queryTimeout);
		this.updateReturn = se.executeUpdate(oper, params);
		return updateReturn.getAffectedRows();
	}

	/*
	 * Batch一般都在PrepraredStatement上执行
	 */
	public int[] executeBatch() throws SQLException {
		if (batchedArgs == null)
			return ArrayUtils.EMPTY_INT_ARRAY;
		int[] result = new int[batchedArgs.size()];
		for (int i = 0; i < batchedArgs.size(); i++) {
			String sql = batchedArgs.get(i);
			result[i] = executeUpdate(sql);
		}
		return result;
	}

	/**
	 * 如果新建了查询，那么上一次查询的结果集应该被显示的关闭掉。这才是符合jdbc规范的
	 * 
	 * @throws SQLException
	 */
	protected void ensureResultSetIsEmpty() throws SQLException {
		if (resultSet != null) {
			log.debug("result set is not null,close current result set");
			try {
				resultSet.close();
			} catch (SQLException e) {
				log.error("exception on close last result set . can do nothing..", e);
			} finally {
				// 最终要显示的关闭它
				resultSet = null;
			}
		}
	}

	protected jef.database.jsqlparser.visitor.Statement parse(String sql) {
		StSqlParser parser = new StSqlParser(new StringReader(sql));
		try {
			return parser.Statement();
		} catch (ParseException e) {
			log.error("Parse Error: {}", sql);
			return null;
		}
	}

	protected void checkClosed() throws SQLException {
		if (closed) {
			throw new SQLException("No operations allowed after statement closed.");
		}
	}

	@Override
	public void closeOnCompletion() throws SQLException {
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}

}
