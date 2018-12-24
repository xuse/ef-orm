//package jef.database.jdbc;
//
//import java.sql.PreparedStatement;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.List;
//
//import jef.database.DbMetaData;
//import jef.database.dialect.DatabaseDialect;
//import jef.database.innerpool.PartitionSupport;
//import jef.database.jdbc.statement.ResultSetLaterProcess;
//import jef.database.routing.jdbc.UpdateReturn;
//import jef.database.routing.sql.InMemoryOperateProvider;
//import jef.database.wrapper.populator.ResultSetExtractor;
//
//public interface JDBCTarget {
//	PreparedStatement prepareStatement(String sql) throws SQLException;
//
//	PreparedStatement prepareStatement(String sql, int generateKeys) throws SQLException;
//
//	PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException;
//
//	PreparedStatement prepareStatement(String sql, int[] columnIndexs) throws SQLException;
//	
//	PreparedStatement prepareStatement(String sql, int rsType, int concurType, int hold) throws SQLException;
//	
//	public UpdateReturn innerExecuteUpdate(String sql, List<Object> ps, GenerateKeyReturnOper keyOper) throws SQLException;
//
//	public <T> T innerSelectBySql(String sql, ResultSetExtractor<T> rst, List<?> objs, InMemoryOperateProvider lazy) throws SQLException;
//
//	boolean isResultSetHolderTransaction();
//
//	void closeTx();
//
//	DatabaseDialect getProfile();
//
//	DbMetaData getMetaData()throws SQLException ;
//
//	JDBCTarget getTarget(String changeDataSource);
//	DatabaseDialect getDialectOf(String database);
//	
//	String getTransactionId();
//
//	String getDbkey();
//
//	PartitionSupport getPartitionSupport();
//
//	PreparedStatement prepareStatement(String first, ResultSetLaterProcess isReverse, boolean b)throws SQLException;
//
//	Statement createStatement() throws SQLException;
//}
