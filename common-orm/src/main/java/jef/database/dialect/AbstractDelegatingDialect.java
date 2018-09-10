package jef.database.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import com.querydsl.sql.SQLTemplates;

import jef.common.log.LogUtil;
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
import jef.database.meta.DbProperty;
import jef.database.meta.Feature;
import jef.database.meta.FunctionMapping;
import jef.database.meta.object.Column;
import jef.database.meta.object.Constraint;
import jef.database.meta.object.SequenceInfo;
import jef.database.support.RDBMS;
import jef.database.wrapper.clause.InsertSqlClause;

/**
 * 
 * @author jiyi
 *
 */
public abstract class AbstractDelegatingDialect implements DatabaseDialect {
	protected DatabaseDialect dialect;
	
	protected AbstractDelegatingDialect(){
		this.dialect = createDefaultDialect();
	}

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
	public final void accept(DbMetaData metadata) {
		DatabaseDialect dialect = decideDialect(metadata);
		if (dialect != null) {
			LogUtil.info("Dialect switched to [{}]", dialect.getClass().getSimpleName());
			this.dialect = dialect;
		}
		this.dialect.accept(metadata);
	}

	/*
	 * 子类必须实现，在得到数据库的DatabaseMetadata后，根据驱动返回的数据库信息，获得一个匹配版本的方言
	 * 
	 * 注意，如果认为当前默认方言无需更改，可能返回null。
	 * 
	 */
	protected abstract DatabaseDialect decideDialect(DbMetaData meta);

	/*
	 * 子类必须实现，在未得到数据库连接时假设一个特定版本的数据库方言
	 */
	protected abstract DatabaseDialect createDefaultDialect();

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
	public void toExtremeInsert(InsertSqlClause sql) {
		dialect.toExtremeInsert(sql);
	}

	@Override
	public String toDefaultString(Object defaultValue, int sqlType) {
		return dialect.toDefaultString(defaultValue, sqlType);
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
	public List<SequenceInfo> getSequenceInfo(DbMetaData conn, String schema, String seqName) throws SQLException{
		return dialect.getSequenceInfo(conn, schema, seqName);
	}

	@Override
	public boolean isCaseSensitive() {
		return dialect.isCaseSensitive();
	}

	@Override
	public SQLTemplates getQueryDslDialect() {
		return dialect.getQueryDslDialect();
	}

	@Override
	public List<Constraint> getConstraintInfo(DbMetaData conn, String schema, String tableName, String constraintName) throws SQLException{
		return dialect.getConstraintInfo(conn, schema, tableName, constraintName);
	}
}
