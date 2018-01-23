/**
 * 
 */
package jef.database.dialect;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbMetaData;
import jef.database.meta.object.Constraint;

/**
 * 系统约束获取测试类
 * @author qihongfei
 *
 */
public class ConstraintTest {

	/**
	 * 测试Postgres9.4的约束
	 */
	@Test
	public void testPG94Constraint(){
		DbClient db=new DbClientBuilder().setDataSource("jdbc:postgresql://localhost:5432/postgres", "postgres", "admin").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("postgresql");
		try {
			// 准备数据
			this.prepareTestData(db);
						
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), "A", "UQ_A");
			cons.forEach(c -> System.out.println(c.toString()));
			
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), null, null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
				this.afterTest(db);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 测试oracle的约束
	 */
	@Test
	public void testOracleConstraint(){
		
		DbClient db=new DbClientBuilder().setDataSource("jdbc:oracle:thin:@localhost:1521:XE", "system", "admin").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("oracle");
		try {
			// 准备数据
			this.prepareTestData(db);
			
			// 创建带约束的视图
			db.executeSql("create or replace view view_a1 as select id from A where id < 10 with check option constraint view_ck_1");
			db.executeSql("create or replace view view_a2 as select id from A where id > 10 with read only constraint view_ck_2");
			
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), "A", "UQ_A");
			cons.forEach(c -> System.out.println(c.toString()));
			
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), null, null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
				this.afterTest(db);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 测试Mysql的约束
	 */
	@Test
	public void testMysqlConstraint(){
		
		DbClient db=new DbClientBuilder().setDataSource("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8", "root", "admin").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("mysql");
		try {
			// 准备数据
			this.prepareTestData(db);
			
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), "A", "UQ_A");
			cons.forEach(c -> System.out.println(c.toString()));
			
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), null, null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
				this.afterTest(db);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 测试sqlserver的约束
	 */
	@Test
	public void testSqlServerConstraint(){
		
		DbClient db=new DbClientBuilder().setDataSource("jdbc:sqlserver://api.hikvision.com.cn; DatabaseName=test", "sa", "Hik12345+").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("sqlserver");
		try {
			
			// 准备数据
			this.prepareTestData(db);
			
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), "A", "UQ_A");
			cons.forEach(c -> System.out.println(c.toString()));
			
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), null, null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
				this.afterTest(db);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 测试derby的约束
	 */
	@Test
	public void testDerbyConstraint(){
		
		DbClient db=new DbClientBuilder().setDataSource("jdbc:derby:F:\\derby\\mydb;create=true", "", "").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("derby");
		try {
			
			// 准备数据
			this.prepareTestData(db);
			
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), "A", "UQ_A");
			cons.forEach(c -> System.out.println(c.toString()));
						
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), null, null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
				this.afterTest(db);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 测试hsqldb的约束
	 */
	@Test
	public void testHsqlConstraint(){
		
		DbClient db=new DbClientBuilder().setDataSource("jdbc:hsqldb:mem:testdb", "SA", "").build();
		DbMetaData meta=db.getMetaData(null);
		DatabaseDialect dialect=AbstractDialect.getDialect("hsqldb");
		try {
			
			// 准备数据
			this.prepareTestData(db);
			
			// 查询单个约束
			List<Constraint> cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), "A", "UQ_A");
			cons.forEach(c -> System.out.println(c.toString()));
						
			// 查询schema下所有约束
			cons = dialect.getConstraintInfo(meta, meta.getCurrentSchema(), null, null);
			cons.forEach(c -> System.out.println(c.toString()));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				db.close();
				this.afterTest(db);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 准备测试用的表和约束
	 * @param db
	 * @throws SQLException
	 */
	private void prepareTestData(DbClient db) throws SQLException{
		
		if(db.existsTable("B")){
			db.executeSql("drop table B");
		}
		if(db.existsTable("A")){
			db.executeSql("drop table A");
		}
		
		// 创建表A，B
		String tableA = "create table A(\n"+
			    "ID int not null,\n"+
			    "NAME varchar(255) not null,\n"+
			    "A1 varchar(255),\n"+
			    "A2 varchar(255),\n"+
			    "constraint PK_A primary key(ID),\n"+
			    "constraint UQ_A UNIQUE(A1, A2),\n"+
			    "constraint CK_A CHECK(ID < 100) \n"+
			")";
		
		String tableB = "create table B(\n"+
			    "ID int not null,\n"+
				"AID INT,\n"+
			    "NAME varchar(255),\n"+
			    "B1 varchar(255),\n"+
			    "B2 varchar(255),\n"+
			    "constraint PK_B primary key(ID),\n"+
			    "constraint UQ_B UNIQUE(NAME),\n"+
			    "constraint CK_B CHECK(ID < 200),\n"+
			    "constraint FK_B FOREIGN KEY(AID) REFERENCES A(ID) \n"+
			")";
		
		db.executeSql(tableA);
		db.executeSql(tableB);
	}
	
	/**
	 * 测试结束工作
	 * @param db
	 * @throws SQLException
	 */
	private void afterTest(DbClient db) throws SQLException{
		
		// 删除表A，B
//		db.executeSql("drop table B");
//		db.executeSql("drop table A");
	}
	
}