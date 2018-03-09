/**
 * 
 */
package jef.database.ddl;

import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.DbClient;
import jef.database.DbMetaData;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.object.Index;
import jef.database.support.RDBMS;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.JefJUnit4DatabaseTestRunner;

/**
 * 索引约束的DDL测试
 * @author qihongfei
 *
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ 
		@DataSource(name = "oracle", url = "${oracle.url}", user = "system", password = "admin"),
        @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
        @DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
        @DataSource(name = "derby", url = "${derby.url}"),
        @DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
//        @DataSource(name = "sqlite", url = "${sqlite.url}"),
        @DataSource(name = "sqlserver", url = "${sqlserver.url}", user = "${sqlserver.user}", password = "${sqlserver.password}") 
})
public class ConstraintAndIndexDDLTest {
	
	
	private DbClient db;
	
	// 测试修改表之前
	@Test
	public void beforeRefreshTable() throws SQLException {
		
		db.dropTable(TableForTest.class);
        db.createTable(TableForTest.class); // 测试新建表
        
        DatabaseDialect dialect = db.getProfile(null);
        DbMetaData meta = db.getMetaData(dialect.getName().name());
        String tablename = "TABLE_FOR_TEST";
        
        // 删除UQ1_FOR_TEST
        if(RDBMS.mysql == dialect.getName() || RDBMS.mariadb == dialect.getName()){
        	db.executeSql("DROP INDEX UQ1_FOR_TEST ON TABLE_FOR_TEST");
        }else{
        	db.executeSql("ALTER TABLE TABLE_FOR_TEST DROP CONSTRAINT UQ1_FOR_TEST");
        }
//        meta.dropConstraint(tablename, "UQ1_FOR_TEST"); 

        // 添加UQ3_FOR_TEST
        db.executeSql("ALTER TABLE TABLE_FOR_TEST ADD CONSTRAINT UQ_ADDED UNIQUE(name)");
        
        // 删除IDX_DEFAULT_TEST索引
        Index index = new Index();
        index.setTableName(tablename);
        index.setIndexName("IDX_DEFAULT_TEST");
        meta.dropIndex(index);

        // 添加IDX_FOR_TEST(code, name desc)索引
        index = new Index();
        index.setTableName(tablename);
        index.setIndexName("IDX_ADDED");
        index.setTableSchema(meta.getCurrentSchema());
        index.setUnique(true);
        index.addColumn("code", true);
        index.addColumn("name", false);
        meta.createIndex(index);
	}
	
	// 测试修改表
	@Test
	public void refreshTable() throws SQLException {
		db.refreshTable(TableForTest.class);
	}
	
}
