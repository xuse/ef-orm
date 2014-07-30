package jef.database.dialect;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.NativeQuery;
import jef.database.QB;
import jef.database.VarObject;
import jef.database.dialect.type.ColumnMappings;
import jef.database.meta.TupleMetadata;
import jef.database.query.Func;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.support.RDBMS;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.Foo;
import jef.script.javascript.Var;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 这个类测试各个数据库方言的兼容性
 * 
 * @author jiyi
 * 
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}",
 password = "${mysql.password}"),
 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa",
 password = ""),
@DataSource(name = "sqlite", url = "jdbc:sqlite:test.db") })
public class DialectFunctionTest extends org.junit.Assert {
	private DbClient db;
	private TupleMetadata tuple;

	@BeforeClass
	public static void enhance() {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance("jef.orm.onetable.model");
	}

	public DialectFunctionTest() {
		tuple = new TupleMetadata("tuple_table");
		tuple.addColumn("id", new ColumnType.AutoIncrement(8));
		tuple.addColumn("name", new ColumnType.Varchar(100));
		tuple.addColumn("pname", new ColumnType.Varchar(100).notNull().defaultIs("N/A"));
		tuple.addColumn("flag", new ColumnType.Boolean().notNull().defaultIs(true));
		tuple.addColumn("age", new ColumnType.Int(8));
		tuple.addColumn("pid", new ColumnType.Int(8).defaultIs(0));
		tuple.addColumn("percent", new ColumnType.Double(8, 4));
		tuple.addColumn("photo", new ColumnType.Blob());
		tuple.addColumn("DOB", new ColumnType.Date().notNull());
		tuple.addColumn("DOD", new ColumnType.TimeStamp().notNull().defaultIs(Func.now));

	}

	@DatabaseInit
	public void parepare() throws SQLException {
		db.dropTable(tuple);
		db.dropTable(Foo.class);
		db.createTable(tuple);
		db.createTable(Foo.class);
		db.getSequenceManager().clearHolders();

		Foo foo = new Foo();
		foo.setModified(new Date());
		foo.setName("Test");
		db.insert(foo);

		VarObject tupleObj = tuple.newInstance();
		tupleObj.set("name", "tuple");
		tupleObj.set("pname", "Not Parent");
		tupleObj.set("DOB", new Date());
		tupleObj.set("age", 100);
		db.insert(tupleObj, true);
	}

	@Test
	public void testColumnAccessor() {
		NativeQuery<Var> query = db.createNativeQuery("select 1 as bool_column from foo", Var.class);
		query.setColumnAccessor("bool_column", ColumnMappings.BOOLEAN);
		Boolean flag = (Boolean)query.getSingleResult().get("bool_column");
		Assert.assertTrue(flag);
		System.out.println(flag.getClass()+"  "+flag);
	}

	/**
	 * 测试NativeQuery形式
	 * 
	 * @throws SQLException
	 * 
	 */
	@Test
	public void testNativeQuery1() throws SQLException {
		parepare();

		NativeQuery<Var> query = db.createNativeQuery("select (select name from foo f where f.id=t1.id) foo_name,t1.name,t1.flag,t1.age from" + " tuple_table t1 where pname like 'N%' and upper(name)||'A'!='A' order by t1.id", Var.class);
		if (db.getProfile().getName() == RDBMS.oracle || db.getProfile().getName() == RDBMS.mysql || db.getProfile().getName()==RDBMS.sqlite) {//
			// 对于不支持boolean类型的数据库，设置一个转换器，让其将制定(char类型)格式化成需要的类型(boolean)
			query.setColumnAccessor("flag", ColumnMappings.CHAR_BOOLEAN);
			query.setColumnAccessor("age", ColumnMappings.INT);
		}

		List<Var> result = query.getResultList();
		Var var = result.get(0);
		assertEquals(true, var.get("flag"));
		assertEquals("Test", var.get("foo_name"));
		assertEquals(100, var.get("age"));
		assertEquals("tuple", var.get("name"));
	}

	@Test
	public void testNativeQuery2() throws SQLException {
		NativeQuery<Var> query = db.createNativeQuery("select (select nvl(name,'N/A') from foo f where f.id=t1.id),t1.name,t1.flag,t1.age from" + " tuple_table t1 where pname like 'N%' and upper(name)||'A'='A'", Var.class);
		LogUtil.show(query.getResultList());
	}

	/**
	 * Sqlite无法通过，因为不支持instr函数且暂无替代方案
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn("sqlite")
	public void testNativeQuery3() throws SQLException {
		NativeQuery<Var> query = db.createNativeQuery("select (select locate(':',t1.name) from foo f where f.id=t1.id),t1.name,t1.flag,t1.age from" + " tuple_table t1 where pname like 'N%' and upper(name)||'A'='A'", Var.class);
		LogUtil.show(query.getResultList());
	}

	@Test
	public void testNativeQuery4() throws SQLException {
		NativeQuery<Var> query = db.createNativeQuery("select concat(substr(t1.name,2,4),lcase(t1.pname)) from tuple_table t1", Var.class);

		LogUtil.show(query.getResultList());
	}

	/**
	 * replace和translate在不同数据库下的表现
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testReplaceTranslate() throws SQLException {
		db.delete(QB.create(Foo.class));
		Foo foo = new Foo();
		foo.setName("abcd1234abcd");
		db.insert(foo);

		NativeQuery<Var> query = db.createNativeQuery("select translate(name,'4321','abcd') A,replace(name,'abc','efg') B from foo t1", Var.class);
		Var var = query.getSingleResult();
		assertEquals("abcddcbaabcd", var.get("A"));
		assertEquals("efgd1234efgd", var.get("B"));

		db.delete(foo);
	}

	/**
	 * Oracle的decode函数，在其他三种数据库下都有不同的表现。 在PG下， case when. 在derby下， case
	 * when。但是注意看，derby和PG的语法是不同的。 在mySQL下， if函数
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testDecode() throws SQLException {
		Foo foo = new Foo();
		foo.setName("abcd1234abcd");
		db.insert(foo);
		db.select(foo);

		NativeQuery<Var> query = db.createNativeQuery("select decode(ID,1,'壹',2,'贰',3,'叁',4,'肆',5,'伍',6,'陆',7,'柒',8,'捌',9,'玖',str(ID)) as C from foo t1", Var.class);
		LogUtil.show(query.getResultList());
	}

	/**
	 * 测试add_month和str函数混用
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testAdd_month() throws SQLException {
		// 准备数据
		db.delete(QB.create(tuple));// 删除全部数据
		VarObject record = tuple.newInstance();
		record.set("name", "1");
		record.set("pname", "test");
		record.set("DOB", new Date());
		record.set("DOD", new Date());
		db.insert(record, null, true);

		// 开始
		Query<VarObject> q = QB.create(tuple);

		Selects select = QB.selectFrom(q);
		select.columns("dob,dod");
		select.sqlExpression("str(add_months(dob,12)) as pname");
		List<VarObject> result = db.select(q);
		LogUtil.show(result);
	}

	/**
	 * 测试trunc函数，这里对Trunc日期的用法不符合除了Oracle以外数据库的预期，所以不支持。 参见
	 * {@linkplain Func#trunc}
	 */
	@Test
	@IgnoreOn({ "derby", "mysql", "postgresql" })
	public void testOracleTrunc() throws SQLException {
		// 准备数据
		db.delete(QB.create(tuple));// 删除全部数据
		VarObject record = tuple.newInstance();
		record.set("name", "1");
		record.set("pname", "test");
		record.set("DOB", new Date());
		record.set("DOD", new Date());
		db.insert(record, null, true);

		// 开始
		Query<VarObject> q = QB.create(tuple);

		Selects select = QB.selectFrom(q);
		select.columns("dob,dod");
		select.sqlExpression("trunc(dob) as pname");
		List<VarObject> result = db.select(q);
		LogUtil.show(result);
	}

	/**
	 * 对数字进行trunc，可以支持
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testTrunc() throws SQLException {
		// 准备数据
		db.delete(QB.create(tuple));// 删除全部数据
		VarObject tupleObj = tuple.newInstance();
		tupleObj.set("name", "tuple");
		tupleObj.set("pname", "Not Parent");
		tupleObj.set("DOB", new Date());
		tupleObj.set("DOD", new Date());
		tupleObj.set("age", 100);
		tupleObj.set("percent", 100.789f);
		db.insert(tupleObj, null, true);

		// 开始
		Query<VarObject> q = QB.create(tuple);

		Selects select = QB.selectFrom(q);
		select.columns("dob,dod");
		select.sqlExpression("trunc(percent) as pname");
		List<VarObject> result = db.select(q);
		LogUtil.show(result);
	}
}
