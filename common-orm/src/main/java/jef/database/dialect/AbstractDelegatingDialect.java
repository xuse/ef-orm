package jef.database.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import jef.database.ConnectInfo;
import jef.database.DbFunction;
import jef.database.DbMetaData;
import jef.database.datasource.DataSourceInfo;
import jef.database.dialect.handler.LimitHandler;
import jef.database.dialect.type.AColumnMapping;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.ParserFactory;
import jef.database.exception.ViolatedConstraintNameExtracter;
import jef.database.jdbc.JDBCTarget;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.meta.Column;
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.FunctionMapping;
import jef.database.meta.SequenceInfo;
import jef.database.support.RDBMS;
import jef.database.wrapper.clause.InsertSqlClause;

/**
 * 
 * @author jiyi
 *
 */
public class AbstractDelegatingDialect implements DatabaseDialect{
	protected DatabaseDialect dialect;

	@Override
	public RDBMS getName() {
		return dialect.getName();
	}

	@Override
	public void processConnectProperties(DataSourceInfo dsw) {
		dialect.processConnectProperties(dsw);
	}

	@Override
	public String getCreationComment(ColumnType vType, boolean typeStrOnly) {
		return dialect.getCreationComment(vType, typeStrOnly);
	}

	@Override
	public CachedRowSet newCacheRowSetInstance() throws SQLException {
		return dialect.newCacheRowSetInstance();
	}

	@Override
	public ColumnType getProprtMetaFromDbType(Column dbTypeName) {
		return dialect.getProprtMetaFromDbType(dbTypeName);
	}

	@Override
	public boolean notHas(Feature feature) {
		return dialect.notHas(feature);
	}

	@Override
	public boolean has(Feature feature) {
		return dialect.has(feature);
	}

	@Override
	public String getCatlog(String schema) {
		return dialect.getCatlog(schema);
	}

	@Override
	public String getSchema(String schema) {
		return dialect.getSchema(schema);
	}

	@Override
	public String getDriverClass(String url) {
		return dialect.getDriverClass(url);
	}

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		return dialect.generateUrl(host, port, pathOrName);
	}

	@Override
	public String getObjectNameToUse(String name) {
		return dialect.getObjectNameToUse(name);
	}

	@Override
	public String getColumnNameToUse(String name) {
		return dialect.getColumnNameToUse(name);
	}

	@Override
	public Timestamp toTimestampSqlParam(Date timestamp) {
		return dialect.toTimestampSqlParam(timestamp);
	}

	@Override
	public boolean isIOError(SQLException se) {
		return dialect.isIOError(se);
	}

	@Override
	public void parseDbInfo(ConnectInfo connectInfo) {
		dialect.parseDbInfo(connectInfo);
	}

	@Override
	public String getProperty(DbProperty key) {
		return dialect.getProperty(key);
	}

	@Override
	public String getProperty(DbProperty key, String defaultValue) {
		return dialect.getProperty(key, defaultValue);
	}

	@Override
	public String getDefaultSchema() {
		return dialect.getDefaultSchema();
	}

	@Override
	public Map<String, FunctionMapping> getFunctions() {
		return dialect.getFunctions();
	}

	@Override
	public Map<DbFunction, FunctionMapping> getFunctionsByEnum() {
		return dialect.getFunctionsByEnum();
	}

	@Override
	public String getFunction(DbFunction function, Object... params) {
		return dialect.getFunction(function, params);
	}

	@Override
	public void processIntervalExpression(BinaryExpression parent, Interval interval) {
		dialect.processIntervalExpression(parent, interval);
	}

	@Override
	public void processIntervalExpression(Function func, Interval interval) {
		dialect.processIntervalExpression(func, interval);
	}

	@Override
	public boolean containKeyword(String name) {
		return dialect.containKeyword(name);
	}

	@Override
	public String getSqlDateExpression(Date value) {
		return dialect.getSqlDateExpression(value);
	}

	@Override
	public String getSqlTimeExpression(Date value) {
		return dialect.getSqlTimeExpression(value);
	}

	@Override
	public String getSqlTimestampExpression(Date value) {
		return dialect.getSqlTimestampExpression(value);
	}

	@Override
	public long getColumnAutoIncreamentValue(AutoIncrementMapping mapping, JDBCTarget db) {
		return dialect.getColumnAutoIncreamentValue(mapping, db);
	}

	@Override
	public Statement wrap(Statement stmt, boolean isInJpaTx) throws SQLException {
		return dialect.wrap(stmt, isInJpaTx);
	}

	@Override
	public PreparedStatement wrap(PreparedStatement stmt, boolean isInJpaTx) throws SQLException {
		return dialect.wrap(stmt, isInJpaTx);
	}

	@Override
	public void accept(DbMetaData asOperateTarget) {
		dialect.accept(asOperateTarget);
	}

	@Override
	public void toExtremeInsert(InsertSqlClause sql) {
		dialect.toExtremeInsert(sql);
	}

	@Override
	public String toDefaultString(Object defaultValue, int sqlType, int changeTo) {
		return dialect.toDefaultString(defaultValue, sqlType,changeTo);
	}

	@Override
	public int getImplementationSqlType(int sqlType) {
		return dialect.getImplementationSqlType(sqlType);
	}

	@Override
	public int getPropertyInt(DbProperty key) {
		return dialect.getPropertyInt(key);
	}

	@Override
	public LimitHandler getLimitHandler() {
		return dialect.getLimitHandler();
	}

	@Override
	public String getColumnNameToUse(AColumnMapping name) {
		return dialect.getColumnNameToUse(name);
	}

	@Override
	public ParserFactory getParserFactory() {
		return dialect.getParserFactory();
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return dialect.getViolatedConstraintNameExtracter();
	}

	@Override
	public long getPropertyLong(DbProperty key) {
		return dialect.getPropertyLong(key);
	}

	@Override
	public List<SequenceInfo> getSequenceInfo(DbMetaData conn, String schema, String seqName) {
		return dialect.getSequenceInfo(conn, schema, seqName);
	}
}
