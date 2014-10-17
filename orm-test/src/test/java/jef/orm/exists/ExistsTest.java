package jef.orm.exists;

import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.IConditionField.Exists;
import jef.database.IConditionField.NotExists;
import jef.database.QB;
import jef.database.query.Query;
import jef.database.query.RefField;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.exists.model.TableA;
import jef.orm.exists.model.TableB;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
	 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class ExistsTest extends org.junit.Assert{
	private DbClient db;
	
	@DatabaseInit
	public void prepare() throws Exception {
		try{
			EntityEnhancer en=new EntityEnhancer();
			en.enhance("jef.orm.exists");
			db.dropTable(TableA.class);
			db.dropTable(TableB.class);
			
			db.createTable(TableA.class,TableB.class);
			TableA a=new TableA(1);
			db.insert(a);
			a=new TableA(2);
			db.insert(a);
			a=new TableA(3);
			db.insert(a);
	
			TableB b=new TableB(1);
			db.insert(b);
			b=new TableB(2);
			db.insert(b);
			b=new TableB(4);
			db.insert(b);
		} catch (Exception e) {
			LogUtil.exception(e);
		}
	}

	@Test
	public void testExists() throws Exception {
		Query<TableA> entityA=QB.create(TableA.class);
		Query<TableB> entityB=QB.create(TableB.class);
		
		entityB.addCondition(TableB.Field.id, new RefField(entityA,"id"));
		entityA.addCondition(new Exists(entityB));
		entityA.orderByAsc(TableA.Field.id);
		List<TableA> list=db.select(entityA);
		
		Assert.assertEquals(list.size(),2);
		Assert.assertEquals(list.get(0).getId(),Integer.valueOf(1));
		Assert.assertEquals(list.get(1).getId(),Integer.valueOf(2));
	}
	
	@Test
	public void testNotExists()throws Exception {
		Query<TableA> entityA=QB.create(TableA.class);
		Query<TableB> entityB=QB.create(TableB.class);
		
		entityB.addCondition(TableB.Field.id, new RefField(entityA,"id"));
		entityA.addCondition(new NotExists(entityB));
		List<TableA> list=db.select(entityA);
		
		Assert.assertEquals(list.size(),1);
		Assert.assertEquals(list.get(0).getId(),Integer.valueOf(3));
	}

}
