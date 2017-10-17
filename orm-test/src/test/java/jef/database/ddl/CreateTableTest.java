package jef.database.ddl;

import java.sql.SQLException;
import java.util.Date;

import jef.database.DbClient;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ @DataSource(name = "oracle", url = "${oracle.url}", user = "test", password = "test"),
        @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
        @DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
        @DataSource(name = "derby", url = "jdbc:derby:./db;create=true"),
        @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
        @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db?date_string_format=yyyy-MM-dd HH:mm:ss"),
        @DataSource(name = "sqlserver", url = "${sqlserver.url}", user = "${sqlserver.user}", password = "${sqlserver.password}") })
public class CreateTableTest {
    private DbClient db;

    @Test
    public void testCreate() throws SQLException {
        db.dropTable(TableForTest.class);
        db.createTable(TableForTest.class);

        TableForTest tb = new TableForTest();
        tb.setAmount(10L);
        tb.setCode("123");
        tb.setData("sads".getBytes());
        db.insert(tb);
        System.out.println(tb.getModified());// 目前不会回写

        System.out.println("====================Step.2====================");
        tb.setName("修改");
        db.update(tb);

        tb = new TableForTest();
        tb.setAmount(12L);
        tb.setCode("124");
        tb.setData("sadssdd".getBytes());
        tb.setModified(new Date());
        db.insert(tb);
        System.out.println(tb.getModified()); // 事先获得
    }

    @Test
    @IgnoreOn(allButExcept="oracle")
    public void testTableRefresh() throws SQLException {
        db.dropTable(TableForTest.class);
        db.createTable(TableForTest.class);
        
//        db.refreshTable(TableForTest.class);
        
    }

}
