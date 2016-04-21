package jef.orm.onetable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.DbClient;
import jef.database.NativeQuery;
import jef.database.QB;
import jef.database.meta.FBIField;
import jef.database.query.AllTableColumns.AliasMode;
import jef.database.query.Join;
import jef.database.query.Query;
import jef.database.query.RefField;
import jef.database.query.Selects;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.Foo;
import jef.orm.onetable.model.TestEntity;
import jef.script.javascript.Var;
import jef.tools.string.RandomData;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 测试一些复杂的场景下的行为
 * 
 * @author jiyi
 * 
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({ 
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"), 
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
	@DataSource(name = "derby", url = "jdbc:derby:./db;create=true"),
	@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	@DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"), 
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class ComplexQuerysTest {

	private DbClient db;

	@DatabaseInit
	public void init() {
		try {
			db.dropTable(TestEntity.class, Foo.class);
			db.createTable(TestEntity.class, Foo.class);
		} catch (Exception e) {
			LogUtil.exception(e);
		}
	}

	
	/**
	 * 使用SQL表达式作为 Join的On条件的场景
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase4() throws SQLException {
		Query<TestEntity> t1 = QB.create(TestEntity.class);
		Query<Foo> t2 = QB.create(Foo.class);

		Join join = QB.leftJoin(t1, t2, QB.on(new FBIField("upper($1.field1)", t1), Foo.Field.name));
		List<Var> vars = db.selectAs(join, Var.class);

		LogUtil.show(vars);
	}
	

	/**
	 * 当自表关联时的Join语句的中的Join Key如何解析的问题
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase2() throws SQLException {
		Query<TestEntity> t1 = QB.create(TestEntity.class);
		Query<TestEntity> t2 = QB.create(TestEntity.class);
		Query<TestEntity> t3 = QB.create(TestEntity.class);

		// ///////////可能1 左右都不指定field所属的查询表，自动匹配
		// Join join=QB.innerJoin(t1, t2, QB.on(TestEntity.Field.intField2,
		// TestEntity.Field.intFiled)); // JOIN test_entity T2 ON T1.intField2 =
		// T2.intFiled
		// join= QB.leftJoin(join, t3, QB.on(TestEntity.Field.field1,
		// TestEntity.Field.field2)); // LEFT JOIN test_entity T3 ON T1.field_1
		// = T3.field_2

		// /////////可能2 仅有左边指定了field所属的查询表，自动匹配（乱了）
		// Join join=QB.leftJoin(t1,t2, QB.on(new
		// RefField(t2,TestEntity.Field.intField2),
		// TestEntity.Field.intFiled));//JOIN test_entity T2 ON T2.intField2 =
		// T2.intFiled
		// join= QB.leftJoin(join, t3, QB.on(TestEntity.Field.field1,
		// TestEntity.Field.field2)); //LEFT JOIN test_entity T3 ON T1.field_1 =
		// T3.field_2

		// ////////可能3, 两边都指定了field所属的查询表。
		Join join = QB.leftJoin(t1, t2, QB.on(t2, TestEntity.Field.intField2, t1, TestEntity.Field.intFiled));// JOIN
																												// test_entity
																												// T2
																												// ON
																												// T2.intField2
																												// =
																												// T2.intFiled
		join = QB.leftJoin(join, t3, QB.on(TestEntity.Field.field1, TestEntity.Field.field2)); // LEFT
																								// JOIN
																								// test_entity
																								// T3
																								// ON
																								// T1.field_1
																								// =
																								// T3.field_2

		List<Var> vars = db.selectAs(join, Var.class);

		LogUtil.show(vars);
	}

	/**
	 * 表A关联到B,B在以另一个关系关联到A的场景
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase3() throws SQLException {
		Query<TestEntity> t1 = QB.create(TestEntity.class);
		Query<Foo> t2 = QB.create(Foo.class);
		Query<TestEntity> t3 = QB.create(TestEntity.class);

		Join join = QB.leftJoin(t1, t2, QB.on(new RefField(t2, Foo.Field.id), TestEntity.Field.intFiled));
		join = QB.leftJoin(join, t3, QB.on(TestEntity.Field.field1, TestEntity.Field.field2));

		List<Var> vars = db.selectAs(join, Var.class);

		LogUtil.show(vars);
	}



	/**
	 * 根据API，使用随机数作为列别名的场景
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCase5() throws SQLException {
		TestEntity[] data = RandomData.newArrayInstance(TestEntity.class, 4);
		db.batchInsert(Arrays.asList(data));

		Query<TestEntity> t1 = QB.create(TestEntity.class);
		Selects select = QB.selectFrom(t1);
		select.allColumns(t1).setAliasType(AliasMode.RANDOM);
		List<TestEntity> l = db.select(t1);
		System.out.println(l.get(0).getField2());
		LogUtil.show(l);
		List<TestEntity> l2 = db.select(t1);
	}
	
	@Test
	public void testUnionSQL() throws SQLException{
		NativeQuery<Foo> q=db.createNativeQuery("select id,name from foo union all select id,name from foo order by id",Foo.class);
		q.setRange(new IntRange(6,10));
		q.getResultList();
	}
}
