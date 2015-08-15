package jef.database.routing.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jef.database.innerpool.JConnection;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.tools.Assert;
import jef.tools.collection.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPreparedStatement extends JStatement implements java.sql.PreparedStatement {

	private static final Logger log = LoggerFactory.getLogger(JPreparedStatement.class);

	// 准备好的原始SQL语句
	private final String originalSQL;

	// 批处理任务
	List<List<ParameterContext>> batchArgs;

	// executeUpdate参数
	private GenerateKeyReturnOper generateKeys = GenerateKeyReturnOper.NONE;

	// 当前的任务参数
	private Map<Integer, ParameterContext> parameterSettings = new TreeMap<Integer, ParameterContext>();

	/**
	 * 构造
	 * 
	 * @param sql
	 * @param routingConnection
	 * @param resultsetType
	 * @param resultSetConcurrency
	 */
	public JPreparedStatement(String sql, JConnection routingConnection, int resultsetType, int resultSetConcurrency, int resultHolder) {
		super(routingConnection, resultsetType, resultSetConcurrency, resultHolder);
		this.originalSQL = sql;
	}

	public JPreparedStatement(String sql, JConnection jConnection, GenerateKeyReturnOper oper) {
		super(jConnection, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		this.originalSQL = sql;
		this.generateKeys = oper;
	}

	public void clearParameters() throws SQLException {
		parameterSettings.clear();
	}

	public boolean execute() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("invoke execute, sql = " + originalSQL);
		}
		if (SqlTypeParser.isQuerySql(originalSQL)) {
			executeQuery();
			return true;
		} else {
			executeUpdate();
			return false;
		}
	}

	public ResultSet executeQuery() throws SQLException {
		return executeQueryInternal(originalSQL, toValues(this.parameterSettings));
	}

	public int executeUpdate() throws SQLException {
		return executeUpdateInternal(originalSQL, generateKeys, toValues(this.parameterSettings));
	}

	private List<ParameterContext> toValues(Map<Integer, ParameterContext> parameterSettings2) {
		List<ParameterContext> l = new ArrayList<ParameterContext>();
		for (Entry<Integer, ParameterContext> e : parameterSettings2.entrySet()) {
			CollectionUtils.setElement(l, e.getKey() - 1, e.getValue());
		}
		return l;
	}

	/**
	 * batch 操作中，如果一个连接内的pst出现更新异常。 则整个连接的后续更新都会终止。 但其他连接的更新还会继续。 目前并不提供返回值设置。
	 */
	public int[] executeBatch() throws SQLException {
		if (batchArgs == null || batchArgs.isEmpty()) {
			return new int[0];
		}
		if (log.isDebugEnabled()) {
			log.debug("invoke executeBatch, sql = " + originalSQL);
		}
		checkClosed();
		ensureResultSetIsEmpty();
		return executeBatchInternal(originalSQL, generateKeys, this.batchArgs);
	}

	public void addBatch() throws SQLException {
		if (batchArgs == null) {
			batchArgs = new LinkedList<List<ParameterContext>>();
		}
		check(parameterSettings);
		batchArgs.add(new ArrayList<ParameterContext>(parameterSettings.values()));
		parameterSettings.clear();
	}

	private void check(Map<Integer, ParameterContext> parameterSettings2) {
		if (parameterSettings2.isEmpty())
			return;
		Integer[] ints = parameterSettings2.keySet().toArray(new Integer[parameterSettings2.size()]);
		Assert.isTrue(ints[0] == 1);
		Assert.isTrue(ints[ints.length - 1] == ints.length);
	}

	// /////////////////////以下为不支持的方法/////////////////////////////////////////////////////
	public ResultSetMetaData getMetaData() throws SQLException {
		throw new UnsupportedOperationException("getMetaData");
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		throw new UnsupportedOperationException("getParameterMetaData");
	}

	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setNString(int parameterIndex, String value) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLException("not support exception");
	}

	// //////////////////////////////设置参数///////////////////////////////////

	public void setArray(int i, Array x) throws SQLException {
		parameterSettings.put(i, new ParameterContext(ParameterMethod.setArray, i, x));
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setAsciiStream, parameterIndex, x, length));
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBigDecimal, parameterIndex, x));
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBinaryStream, parameterIndex, x, length));
	}

	public void setBlob(int i, Blob x) throws SQLException {
		parameterSettings.put(i, new ParameterContext(ParameterMethod.setBlob, i, x));
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBoolean, parameterIndex, x));
	}

	public void setByte(int parameterIndex, byte x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setByte, parameterIndex, x));
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setBytes, parameterIndex, x));
	}

	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setCharacterStream, parameterIndex, reader, length));
	}

	public void setClob(int i, Clob x) throws SQLException {
		parameterSettings.put(i, new ParameterContext(ParameterMethod.setClob, i, x));
	}

	public void setDate(int parameterIndex, Date x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setDate1, parameterIndex, x));
	}

	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setDate2, parameterIndex, x, cal));
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setDouble, parameterIndex, x));
	}

	public void setFloat(int parameterIndex, float x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setFloat, parameterIndex, x));
	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setInt, parameterIndex, x));
	}

	public void setLong(int parameterIndex, long x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setLong, parameterIndex, x));
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setNull1, parameterIndex, sqlType));
	}

	public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
		parameterSettings.put(paramIndex, new ParameterContext(ParameterMethod.setNull2, paramIndex, sqlType, typeName));
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setObject1, parameterIndex, x));
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setObject2, parameterIndex, x, targetSqlType));
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setObject3, parameterIndex, x, targetSqlType, scale));
	}

	public void setRef(int i, Ref x) throws SQLException {
		parameterSettings.put(i, new ParameterContext(ParameterMethod.setRef, i, x));
	}

	public void setShort(int parameterIndex, short x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setShort, parameterIndex, x));
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setString, parameterIndex, x));
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTime1, parameterIndex, x));
	}

	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTime2, parameterIndex, x, cal));
	}

	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTimestamp1, parameterIndex, x));
	}

	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setTimestamp2, parameterIndex, x, cal));
	}

	public void setURL(int parameterIndex, URL x) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setURL, parameterIndex, x));
	}

	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		parameterSettings.put(parameterIndex, new ParameterContext(ParameterMethod.setUnicodeStream, parameterIndex, x, length));
	}
}
