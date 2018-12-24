package jef.database.dynamic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.Transaction;
import jef.database.dialect.ColumnType;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"),
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
	@DataSource(name="derby",url="${derby.url}"),
	@DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
	@DataSource(name = "sqlite", url = "${sqlite.url}"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class DynamicExtendsTest extends org.junit.Assert{
	private DbClient db;

	@DatabaseInit
	public void setup() throws SQLException{
		db.dropTable(Status.class);
		db.createTable(Status.class);
		db.merge(new Status(0,"不可用"));
		db.merge(new Status(1,"正常"));
		db.merge(new Status(2,"警戒"));
		db.merge(new Status(3,"异常"));
		db.merge(new Status(4,"短路"));
		db.merge(new Status(5,"网元故障"));
	}

	/**
	 * 测试两种动态属性扩展的实现方式，
	 * 
	 * 当使用[]作为SQLSERVER的关键字回避方式后，
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testAttrTable() throws SQLException {
//		ORMConfig.getInstance().setUseOuterJoin(false);
//		ORMConfig.getInstance().setCacheLevel1(true);
//		ORMConfig.getInstance().setCacheDebug(true);
		initUserExtendInfo();
		db.createTable(UserEx.class);
		
		Transaction db=this.db.startTransaction();
		
		UserEx user = new UserEx();
		user.setComm("TEST of USEREXT");
		user.setName("张三");
		user.setStatus(2);
		user.setAtribute("QQ", "213324333");
		user.setAtribute("E_MAIL", "dsff@google.com");
		user.setAtribute("address", "重点萨芬撒地方");
		user.setAtribute("day", 123);
		db.insert(user);
		
		user = new UserEx();
		user.setComm("TEST of 2");
		user.setName("李四");
		user.setAtribute("QQ", "77853431");
		user.setAtribute("E_MAIL", "Lisi@baidu.com");
		user.setAtribute("address", "China town");
		user.setAtribute("day", 8873);
		user.setStObj(new Status(6,"测试"));
		db.insert(user);
		
		{
			List<UserEx> list=db.selectByField(UserEx.Field.name, "李四");
			UserEx ex=list.get(0);
			System.out.println(ex.getName());
			System.out.println(ex.getAtribute("QQ"));
			System.out.println(ex.getAtribute("E_MAIL"));
			System.out.println(ex.getAtribute("address"));
			System.out.println(ex.getAtribute("day"));
			assertNotNull(ex.getId());
			assertEquals(8873, ex.getAtribute("day"));
			assertEquals("Lisi@baidu.com", ex.getAtribute("E_MAIL"));
			assertEquals("77853431", ex.getAtribute("QQ"));
			
			ex.setAtribute("QQ", "6320535");
			int i=db.update(ex);
			assertEquals(0, i); //由于记录本身没有字段变化，仅级联对象变化，更新后记录行数仅返回主表更新行数，所以为0
		}
		{
			List<UserEx> list=db.selectByField(UserEx.Field.name, "李四");
			UserEx ex=list.get(0);
			assertEquals(8873, ex.getAtribute("day"));
			assertEquals("6320535", ex.getAtribute("QQ"));
			ex.setComm("TestUpdated2");
			int i=db.update(ex);
			assertEquals(1, i);
		}
		{
			List<UserEx> list=db.selectByField(UserEx.Field.name, "李四");
			UserEx ex=list.get(0);
			assertEquals("TestUpdated2", ex.getComm());
			int i=db.delete(ex);
			assertEquals(1, i);
			int j=db.delete(QB.create(UserEx.class));
			assertEquals(1, i);
		}
		db.commit(true);
	}

	@Test
	public void testRealDynmicTable() throws SQLException {
		initResourceMetadata();
		db.createTableByInstance(new DynaResource("桌子"));
		db.createTableByInstance(new DynaResource("电视机"));
		db.createTableByInstance(new DynaResource("计算机"));
		String id1;
		String id2;
		String id3;
		{
			List<DynaResource> list=new ArrayList<DynaResource>();
			DynaResource resource = new DynaResource("桌子");
			resource.setName("一张大桌子");
			resource.setElevation(120.243);
			resource.setPrice(199);
			resource.setStatus(0);
			resource.setAtribute("height", 100);
			resource.setAtribute("width", 100);
			resource.setAtribute("type", "SQUARE");
			list.add(resource);

			resource = new DynaResource("桌子");
			resource.setName("一张大桌子");
			resource.setElevation(120.243);
			resource.setPrice(199);
			resource.setStatus(0);
			resource.setAtribute("height", 100);
			resource.setAtribute("width", 100);
			resource.setAtribute("type", "SQUARE");
			list.add(resource);
			
			db.batchInsert(list);
			id1= list.get(0).getIndexCode();
			id2 = list.get(1).getIndexCode();
		}
		{
			DynaResource resource = new DynaResource("桌子");
			resource.setIndexCode(id1);
			resource.setStatus(1);
			resource.setElevation(200.0);
			resource.setAtribute("width", 250);
			db.update(resource);
			
			DynaResource loaded=db.load(resource);
			loaded.setAtribute("height", 250);
			db.update(loaded);
		}
		{
			System.out.println("====================测试关联====================");
			List<DynaResource> tables=db.select(QB.create(DynaResource.class, "桌子"));
			for(DynaResource t:tables){
				System.out.println(t.getStatusObj().getData());
			}
		}
		{
			DynaResource r1= new DynaResource("桌子");
			r1.setStatus(1);
			r1.setIndexCode(id1);
			r1.setElevation(200.0);
			r1.setAtribute("width", 250);
			
			DynaResource r2 = new DynaResource("桌子");
			r2.setStatus(1);
			r2.setIndexCode(id2);
			r2.setElevation(200.0);
			r2.setAtribute("width", 250);
			db.batchUpdate(Arrays.asList(r1,r2));
			
		}
		{
			Transaction tx=db.startTransaction();
			DynaResource r1= new DynaResource("桌子");
			r1.setIndexCode(id1);
			DynaResource r2 = new DynaResource("桌子");
			r2.setIndexCode(id2);
			tx.executeBatchDeletion(Arrays.asList(r1,r2));
			tx.rollback(true);
		}
		{
			DynaResource r1= new DynaResource("桌子");
			r1.setIndexCode(id2);
			db.delete(r1);
		}
		{
			DynaResource resource = new DynaResource("电视机");
			resource.setName("一台电视机");
			resource.setElevation(120.243);
			resource.setPrice(4280);
			resource.setStatus(2);
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
			resource.setStatus(3);
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
			q.terms()
			.gt("width", 100).or().gt("height", 100)
			.and().not().in("type", new String[] { "CRICLE", "BOX" });
			System.out.println(q.getConditions());
			db.select(q);
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
		meta.addColumn("color", new ColumnType.Boolean().setDefault(true));
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
