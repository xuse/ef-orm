package jef.database.routing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.DbClient;
import jef.database.datasource.AbstractDataSource;
import jef.database.datasource.DataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.innerpool.JConnection;

import org.easyframe.enterprise.spring.TransactionType;

/**
 * 将EF-ORM封装为一个标准的JDBC DataSource。
 * <pre>
 * 使用此DataSource进行JDBC操作，可以享受由EF-ORM带来的以下特性——
 * 1、数据库SQL改写，支持本地化操作。目前支持的SQL特性包括
 *    数据库函数转换
 *    SQL || /concat运算符转换
 *    日期函数、Interval关键字等转换
 *    表名/列名 as alias的兼容处理。
 *    Oracle Start with.. connect by
 *    limit ? offset ?关键字的分页
 * 2、分库分表路由
 *     支持insert / update /select /delete四类语句，要求均为单表操作。
 *     分库条件：对于insert语句采用values值计算目标，对于另外三种语句根据where值计算目标。
 *     insert场景下，路由条件必须完整。
 *     其他三种语句下，可以支持不完整的路由条件（往往会引起多表和多库查询）
 *     
 * 支持的特性——
 *     单库CRUD操作
 *     跨库count / max /min /sum函数
 *     跨库group by / having
 *     跨库distinct
 *     跨库limit分页
 *     跨库排序
 * 不支持的特性——
 *     多表关联
 *     跨库时：select avg(x) from table
 *     </pre>
 */
public class JDataSource extends AbstractDataSource{
	private DbClient db;
	
	public JDataSource(){
	}
	
	public JDataSource(DbClient lookup){
		this.db=lookup;
	}
	/**
	 * 构造
	 * @param lookup
	 */
	public JDataSource(DataSourceLookup lookup){
		this.db=new DbClient(new RoutingDataSource(lookup),0,TransactionType.DATASOURCE);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return new JConnection(db);
	}
	
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}
	
	public DbClient getDbClient(){
		return db;
	}
}
