package jef.orm.onetable;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.Foo;
import jef.tools.Exceptions;

/**
 * 自定义表名测试
 * @author jiyi
 *
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
 @DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
 @DataSource(name="derby",url="${derby.url}"),
 @DataSource(name = "sqlite", url = "${sqlite.url}"),
 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class CustomTableNameTest extends org.junit.Assert {
	private static final String MY_FOO_TABLE="MY_FOO_TABLE";
	
	private DbClient db;
	
	/**
	* Use CustomTable name, not the defaultname in table model.
	 * @throws SQLException
	 */
	@DatabaseInit
	public void start() throws SQLException {
		try{
			db.dropTable(Foo.class);
			db.createTable(Foo.class);
			db.dropTable(MY_FOO_TABLE);
			db.createTable(Foo.class, MY_FOO_TABLE,null);
		} catch (Exception e) {
			Exceptions.log(e);
		}
	}

	@Test
	public void testPlayWithMyTable() throws SQLException {
		int id;
		{//insert
			Foo foo = new Foo();
			foo.setName("My play!");
			foo.setModified(new Date());
			db.insert(foo, MY_FOO_TABLE);
			id=foo.getId();
		}
		{//select & update 
			Query<Foo> query=QB.create(Foo.class);
			query.addCondition(QB.eq(Foo.Field.id, id));
			query.setCustomTableName(MY_FOO_TABLE);
			List<Foo> foos=db.select(query,null);
			Foo foo=foos.get(0);
			foo.setName("Play of update!");
			db.update(foo.getQuery(),MY_FOO_TABLE);
		}
		{//delete
			Foo foo=new Foo();
			foo.setId(id);
			foo.getQuery().setCustomTableName(MY_FOO_TABLE);
			db.delete(foo);
		}
	}
}
