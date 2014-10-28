package jef.database.dynamic;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.dialect.ColumnType;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
// @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
// @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}",
// password = "${mysql.password}"),
// @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
// @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
// @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
// @DataSource(name = "sqlserver", url =
// "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class DynamicExtendsTest {
	private DbClient db;

	public DynamicExtendsTest() throws SQLException {
		new EntityEnhancer().enhance("jef.database.dynamic");
	}

	/**
	 * 测试两种动态属性扩展的实现方式，
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testAttrTable() throws SQLException {
		initUserExtendInfo();
		db.createTable(UserEx.class);
		UserEx user = new UserEx();
		user.setComm("TEST of USEREXT");
		user.setName("张三");
		user.setAtribute("QQ", "213324333");
		user.setAtribute("E_MAIL", "dsff@google.com");
		user.setAtribute("address", "重点萨芬撒地方");
		user.setAtribute("day", 123);
		db.insert(user);

	}

	@Test
	public void testRealDynmicTable() throws SQLException {
		initResourceMetadata();
		db.createTable(new DynaResource("桌子"));
		db.createTable(new DynaResource("电视机"));
		db.createTable(new DynaResource("计算机"));
		String id1;
		String id2;
		String id3;
		{
			DynaResource resource = new DynaResource("桌子");
			resource.setName("一张大桌子");
			resource.setElevation(120.243);
			resource.setPrice(199);
			resource.setStatus(0);
			resource.setAtribute("height", 100);
			resource.setAtribute("width", 100);
			resource.setAtribute("type", "SQUARE");

			db.insert(resource);
			id1 = resource.getIndexCode();
		}
		{
			DynaResource resource = new DynaResource("电视机");
			resource.setName("一台电视机");
			resource.setElevation(120.243);
			resource.setPrice(4280);
			resource.setStatus(0);
			resource.setAtribute("pixel", "1920x1080");
			resource.setAtribute("brand", "Sharp");
			db.insert(resource);
			id2 = resource.getIndexCode();
		}
		{
			DynaResource resource = new DynaResource("计算机");
			resource.setName("一台计算机");
			resource.setElevation(1.234);
			resource.setPrice(6999);
			resource.setStatus(0);
			resource.setAtribute("CPU", "Intel XEON E4200");
			resource.setAtribute("mainboard", "gigabyte 227LE");
			resource.setAtribute("memory", "4Gx2");
			resource.setAtribute("disk_size", "Seagate 2T");
			resource.setAtribute("DISPLAY_CARD", "ATI 9600");
			resource.setAtribute("monitor", "PHILIPS 227E'");
			resource.setAtribute("NET", "Realtek PCIe 1000M");
			db.insert(resource);
			id3 = resource.getIndexCode();
		}
		{
			DynaResource res = db.load(DynaResource.class, "桌子", "123");
			System.out.println(res);

			Query<DynaResource> q = QB.create(DynaResource.class,"桌子");

			q.terms().gt("width", 100).or().gt("height", 100);
			q.terms().not().in("type", new String[] { "CRICLE", "BOX" });
			System.out.println(q.getConditions());
		}
	}

	/**
	 * 初始化商品资源的扩展字段信息
	 */
	private void initResourceMetadata() {
		TupleMetadata meta = new TupleMetadata("桌子");
		meta.addColumn("height", new ColumnType.Int(8));
		meta.addColumn("width", new ColumnType.Int(8));
		meta.addColumn("type", new ColumnType.Varchar(40));
		MetaHolder.putDynamicMeta(meta);

		meta = new TupleMetadata("电视机");
		meta.addColumn("pixel", new ColumnType.Varchar(20));
		meta.addColumn("brand", new ColumnType.Varchar(40));
		meta.addColumn("color", new ColumnType.Boolean().defaultIs(true));
		MetaHolder.putDynamicMeta(meta);

		meta = new TupleMetadata("计算机");
		meta.addColumn("CPU", new ColumnType.Varchar(20));
		meta.addColumn("mainboard", new ColumnType.Varchar(40));
		meta.addColumn("memory", new ColumnType.Varchar(40));
		meta.addColumn("disk_size", new ColumnType.Varchar(64));
		meta.addColumn("DISPLAY_CARD", new ColumnType.Varchar(64));
		meta.addColumn("monitor", new ColumnType.Varchar(64));
		meta.addColumn("NET", new ColumnType.Varchar(64));
		MetaHolder.putDynamicMeta(meta);

	}

	/**
	 * 初始化用户的扩展字段信息
	 */
	private void initUserExtendInfo() {
		TupleMetadata meta = new TupleMetadata("USER_EX");
		meta.addColumn("QQ", new ColumnType.Varchar(20));
		meta.addColumn("E_MAIL", new ColumnType.Varchar(40));
		meta.addColumn("address", new ColumnType.Varchar(40));
		meta.addColumn("day", new ColumnType.Int(10));
		MetaHolder.putDynamicMeta(meta);
	}
}
