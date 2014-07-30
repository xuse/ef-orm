package jef.orm.multitable2;

import java.sql.SQLException;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.meta.DbProperty;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.tools.StringUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 测试一些基础性的案例
 * @author jiyi
 *
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db")
})
public class BasicTest extends org.junit.Assert{
	private DbClient db;


	
	@Test
	public void testCheckSql() throws SQLException {
		String sql = db.getProfile().getProperty(DbProperty.CHECK_SQL);
		if (!StringUtils.isEmpty(sql)) {
			LogUtil.show(db.getResultSet(sql,0));
		}
	}
}
