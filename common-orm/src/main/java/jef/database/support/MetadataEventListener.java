package jef.database.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.Column;
import jef.database.meta.ColumnModification;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.executor.StatementExecutor;

/**
 * 表结构变更时，整个操作过程进度的事件监听器(可以干涉修改表的过程)
 * 
 * <ul>
 * 按事件的时间顺序如下
 * <li>{@linkplain #beforeTableRefresh(ITableMetadata, String) 变更表之前} </li>
 * <li>{@linkplain #onTableCreate(ITableMetadata, String) 如果表不存在，创建表之前}</li>
 * <li>{@linkplain #onCompareColumns(String, List, Map) 如果表存在，在比较表之前}</li>
 * <li>{@linkplain #onColumnsCompared(String, ITableMetadata, Map, List, List) 数据库中的表和元数据比较完成后}</li>
 * <li>{@linkplain #beforeAlterTable(String, ITableMetadata, Connection, List) 比较完成并生成SQL后，在操作表之前}</li>
 * <li>{@linkplain #onAlterSqlFinished(String, String, List, int, long) 在每句SQL执行成功后}</li>
 * <li>{@linkplain #onSqlExecuteError(SQLException, String, String, List, int) 在任何一句SQL执行失败后}</li>
 * <li>{@linkplain #onTableFinished(ITableMetadata, String) 在所有SQL都执行完成后}</li>
 * </ul>
 * @author jiyi
 * 
 */
public interface MetadataEventListener {
	/**
	 * 当开始变更一张表时执行
	 * @param meta 表的元数据
	 * @param table 数据库中的表名
	 * @return false，则放弃变更此表
	 */
	boolean beforeTableRefresh(ITableMetadata meta, String table);
	/**
	 * 表不存在，将会创建表
	 * @param meta 表的元数据
	 * @param tablename 数据库中的表名
	 * @return false，则放弃创建此表
	 */
	boolean onTableCreate(ITableMetadata meta, String tablename);

	/**
	 * 表结构比较完成后出发，提供了表的对比结果供用户判断。
	 * @param tablename  表名
	 * @param meta  表的元数据
	 * @param insert      将要增加的数据库列(列名、数据类型)
	 * @param changed     数据格式发生了变更的列
	 * @param delete      将要删除的列
	 * @return  false，则放弃变更此表
	 */
	boolean onColumnsCompared(String tablename, ITableMetadata meta, Map<String, ColumnType> insert, List<ColumnModification> changed, List<String> delete);

	/**
	 * 框架根据表对比结果，生成SQL语句后触发此事件，用户事件中可以获得所有的SQL语句。
	 * @param tablename 表名
	 * @param meta      元数据
	 * @param conn       数据库连接
	 * @param sql  所有的SQL语句
	 */
	void beforeAlterTable(String tablename, ITableMetadata meta, StatementExecutor conn, List<String> sql);

	/**
	 * 每一句SQL语句执行完成后
	 * @param tablename  表名
	 * @param sql        刚完成的SQL语句
	 * @param n			  序号，第几句SQL，编号从0开始
	 * @param size		  总的SQL语句数量
	 * @param cost		  刚才的SQL语句耗时(ms)
	 */
	void onAlterSqlFinished(String tablename, String sql,  List<String> sqls, int n,long cost);
	
	/**
	 * 当某句SQL语句执行失败后触发
	 * @param e 异常
	 * @param tablename 表名
	 * @param sql    出错的SQL语句
	 * @param sqls   所有SQL语句
	 * @param n      出错的SQL语句序号
	 * @return 如果返回true，表示继续执行后续的语句，如果返回false表示不再向后执行
	 */
	boolean onSqlExecuteError(SQLException e, String tablename, String sql, List<String> sqls, int n);
	
	/**
	 * 表变更完成
	 * @param meta 表元数据
	 * @param tablename 表名
	 */
	void onTableFinished(ITableMetadata meta, String tablename);
	
	/**
	 * 开始进行表结构的比较
	 * @param tablename 表名
	 * @param columns  数据库中的所有列
	 * @param defined  表模型中的所有列
	 * @return 是否继续比较这张表
	 */
	boolean onCompareColumns(String tablename, List<Column> columns, Map<Field, ColumnMapping<?>> defined);

}
