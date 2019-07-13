package jef.database.nativequery;

import java.sql.SQLException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.NamedQueryConfig;
import jef.database.NativeQuery;
import jef.database.PagingIterator;
import jef.database.QB;
import jef.database.query.SqlExpression;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.multitable2.TextValuePair;
import jef.orm.multitable2.model.Child;
import jef.orm.multitable2.model.EnumationTable;
import jef.orm.multitable2.model.Leaf;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.string.RandomData;

/**
 * NamedQuery测试类
 * 
 * @see NamedQueryConfig
 * @see NativeQuery
 * 
 * @Date 2013-1-4
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="${derby.url}"),
	 @DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "${sqlite.url}"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class NamedQueryTest {
	private DbClient db;
	
	@DatabaseInit
	public void setUp() {
		try {
			// clear table
			db.dropTable(Leaf.class);
			db.dropTable(Child.class);
			db.dropTable(EnumationTable.class);
			db.createTable(Leaf.class,Child.class,EnumationTable.class);
			
			
			// insert data
			Leaf leaf;
			for (int i = 0, n = 5; i < n; i++) {
				leaf = RandomData.newInstance(Leaf.class);
				db.insert(leaf);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}	

	/**
	 * @测试目的 测试以下特性：自行编写的NativeQuery也可以由框架来分页查询
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testPaging() {
		LogUtil.show(StringUtils.center("testPaging", 50, "="));

		try {
			NativeQuery nq = db.createNamedQuery("testPaging");
			String rawSql = ((NamedQueryConfig) BeanUtils.getFieldValue(nq, "config")).getRawsql();
			LogUtil.show("========rawSql: " + rawSql);
			Assert.assertEquals("select * from leaf", rawSql);

			Leaf leaf = new Leaf();
			leaf.getQuery().setAllRecordsCondition();
			leaf.getQuery().setCascade(false);
			int count = db.select(leaf).size();
			PagingIterator pageIter = db.pageSelect(nq, 2);
			Assert.assertEquals(count % 2 == 0 ? count / 2 : count / 2 + 1, pageIter.getTotalPage());
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * @测试目的 测试以下特性：动态省略SQL表达式
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testDynamicOmit() {
		LogUtil.show(StringUtils.center("testDynamicOmit", 50, "="));

		NativeQuery nq = db.createNamedQuery("testDynamicOmit");
		String rawSql = ((NamedQueryConfig) BeanUtils.getFieldValue(nq, "config")).getRawsql();
		LogUtil.show("========rawSql: " + rawSql);
		Assert.assertEquals("select * from leaf where name =:name and childid = :childid<int>",
				rawSql);

		nq.setParameterByString("childid", "4");
		List result = nq.getResultList();
		LogUtil.show("========rawSql result: " + result);
	}

	/**
	 * @测试目的 测试以下特性：使用动态SQL片段来更自由定义SQL(要检索的字段名及order by为动态传入的场景)
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testDynamicSegments() {
		LogUtil.show(StringUtils.center("testDynamicSegments", 50, "="));

		NativeQuery nq = db.createNamedQuery("testDynamicSegments");
		String rawSql = ((NamedQueryConfig) BeanUtils.getFieldValue(nq, "config")).getRawsql();
		LogUtil.show("========rawSql: " + rawSql);
		Assert.assertEquals("select :column<sql> from leaf order by :orderBy<sql>", rawSql);

		nq.setParameterByString("column", "id,name");
		nq.setParameterByString("orderBy", "id desc");
		List result = nq.getResultList();
		LogUtil.show("========rawSql result: " + result);

		try {
			int count = db.count(QB.create(Leaf.class));
			Assert.assertEquals(count, nq.getResultCount());
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * @测试目的 测试以下特性：<br>
	 *       1)使用动态SQL片段来更自由定义SQL(表名为动态传入的场景)<br>
	 *       2)返回值为{@code TextValuePair}类型的
	 */
	@Test
	public void testDynamicSegmentsForTableName() {
		LogUtil.show(StringUtils.center("testDynamicSegmentsForTableName", 50, "="));

		NativeQuery<TextValuePair> nq = db.createNamedQuery("testDynamicSegmentsForTableName",
				TextValuePair.class);
		String rawSql = ((NamedQueryConfig) BeanUtils.getFieldValue(nq, "config")).getRawsql();
		LogUtil.show("========rawSql: " + rawSql);
		Assert.assertEquals("select id as text, name as value from :tableName<sql>", rawSql);

		// 以下几种写法均可以
//		nq.setParameterByString("tableName", new String[] { "leaf" });
//		nq.setParameterByString("tableName", "leaf");
//		nq.setParameter("tableName", new String[] { "leaf" });
//		nq.setParameter("tableName", "leaf");
		nq.setParameter("tableName", new SqlExpression("leaf"));

		List<TextValuePair> result = nq.getResultList();
		LogUtil.show("========rawSql result: " + result);

		try {
			int count = db.count(QB.create(Leaf.class));
			Assert.assertEquals(count, nq.getResultCount());
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

}
