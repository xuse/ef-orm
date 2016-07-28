package jef.orm.multitable2;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.database.test.LogListener;
import jef.orm.multitable2.model.Child;
import jef.orm.multitable2.model.Code;
import jef.orm.multitable2.model.EnumationTable;
import jef.orm.multitable2.model.Parent;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db?date_string_format=yyyy-MM-dd HH:mm:ss"),
	 @DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class SpecialCases extends org.junit.Assert{
	
	//TODO 增加测试案例关于 rowid, 增加案例关于getFunction
	// ===== 关于rowid的支持 ==============
	// 1、除非配置db.enable.rowid=false，否则在Oracle环境下总是会启用rowid特性。
	//
	// 当使用rowid特性后，凡是单表查询、单条插入都会在对象中设置rowid。
	// 凡是无Query场合下的查询、删除、更新，总是会使用rowid作为条件。
	// 此外，可以通过api obj.rowid()获取rowid(),如果对象中没有获得rowid，那么总是返回null.
	//
	// 关于sysdate支持
	// 1、当date类型字段的Annotation定义中加上
	// @GeneratedValue后，如果没有手工设置过date字段的值，那么就会在插入数据库的时候将其改用
	// sysdate 对Oracle
	// now() 对MySQL
	// current_timestamp 对Derby
	// 要注意，目前没有提供任何办法将这个字段立刻返回到对象中，除非load一次。
	//
	//
	// 2、查询和更新时
	// 更新时：
	// t1.prepareUpdate(TestEntity.Field.dateField, db.get(DbFunction.SYSDATE));
	//
	// 查询和删除时
	// t1.getQuery().addCondition(TestEntity.Field.dateField,
	// Operator.BETWEEN_L_L,new
	// Object[]{db.get(DbFunction.SYSDATE),TestEntity.Field.dateField});
	
	
	private DbClient db;
	
	@DatabaseInit
	public void parepare() throws SQLException{
		db.createTable(Child.class);
		db.createTable(Parent.class);
		db.createTable(EnumationTable.class);
		db.createTable(Code.class);
	}
	
	/**
	 * 从实体关联以不同关联路径多次关联另一张表，
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectDoubleJoin() throws SQLException {
		Query<Code> cc = QB.create(Code.class);
		db.select(cc, null);
	}
	
	
	/**
	 * Test Sequence name
	 * 案例说明<p>
	 * 在ChildCC中，通过<code>@SequenceGenerator(sequenceName="MAIN1")</code> 的注解来指定Sequence的名称，从而不使用默认的Sequence名称。
	 * @throws SQLException 
	 */
	@Test
	@IgnoreOn(allButExcept={"oracle"})
	public void testCustomSequenceName() throws SQLException{
		db.dropTable(Code.class);
		LogListener listener=new LogListener("create sequence ([a-zA-z0-9\\.]+)\\s+.*");
		db.createTable(Code.class);
		String sequenceName=listener.getSingleMatch()[0];
		assertEquals("MAIN1", sequenceName);
	}
	/**
	 * 可以在静态的Join参数中配置JPQL变量甚至SQL片段。
	 * @throws SQLException
	 */
	@Test
	public void testJoinWithCustomCondition() throws SQLException {
		Child c=new Child();
		c.setId(2);
		c.getQuery().setAttribute("aaa", "child");
		db.load(c);
	}
}
