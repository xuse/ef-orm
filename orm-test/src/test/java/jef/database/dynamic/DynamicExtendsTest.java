package jef.database.dynamic;

import java.sql.SQLException;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.query.Query;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
//	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
//	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
//	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
//	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
//	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db"),
//	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class DynamicExtendsTest {
	private DbClient db;

	
	public DynamicExtendsTest() throws SQLException {
		new EntityEnhancer().enhance("jef.database.dynamic");
	}
	
	/**
	 * 测试两种动态属性扩展的实现方式，
	 * @throws SQLException 
	 */
	@Test
	public void testAttrTable() throws SQLException{
		initUserExtendInfo();
		db.createTable(UserEx.class);
		UserEx user=new UserEx();
		user.setComm("TEST of USEREXT");
		user.setName("张三");
		user.setExtProp("QQ", "213324333");
		user.setExtProp("E-MAIL", "dsff@google.com");
		user.setExtProp("Address", "重点萨芬撒地方");
		db.insert(user);
	}
	
	


	@Test
	public void testRealDynmicTable() throws SQLException{
//		initResourceMetadata();
//		db.createTable(new DynaResource("桌子"));
//		db.createTable(new DynaResource("电视机"));
//		db.createTable(new DynaResource("计算机"));
//		String id1;
//		String id2;
//		String id3;
//		{
//			DynaResource resource=new DynaResource("桌子");
//			resource.setName("一张大桌子");
//			resource.setElevation(120.243);
//			resource.setPrice(199);
//			resource.setStatus(0);
//			resource.setExtendProp("height", 100);
//			resource.setExtendProp("width", 100);
//			resource.setExtendProp("type", "SQUARE");
//			
//			db.insert(resource);
//			id1=resource.getIndexCode();
//		}
//		{
//			DynaResource resource=new DynaResource("电视机");
//			resource.setName("一台电视机");
//			resource.setElevation(120.243);
//			resource.setPrice(4280);
//			resource.setStatus(0);
//			resource.setExtendProp("pixel", "1920x1080");
//			resource.setExtendProp("brand", "Sharp");
//			db.insert(resource);
//			id2=resource.getIndexCode();
//		}
//		{
//			DynaResource resource=new DynaResource("计算机");
//			resource.setName("一台计算机");
//			resource.setElevation(1.234);
//			resource.setPrice(6999);
//			resource.setStatus(0);
//			resource.setExtendProp("CPU", "Intel XEON E4200");
//			resource.setExtendProp("mainboard", "gigabyte 227LE");
//			resource.setExtendProp("memory", "4Gx2");
//			resource.setExtendProp("disk_size", "Seagate 2T");
//			resource.setExtendProp("DISPLAY_CARD", "ATI 9600");
//			resource.setExtendProp("monitor", "PHILIPS 227E'");
//			resource.setExtendProp("NET", "Realtek PCIe 1000M");
//			db.insert(resource);
//			id3=resource.getIndexCode();
//		}
		{
//			DynaResource res=db.load(DynaResource.class, "桌子","123");
//			System.out.println(res);
	
			
			Query<DynaResource> q=QB.create(DynaResource.class);
			q.getInstance().setResourceType("桌子");
			
			q.terms().gt("width",100).or().gt("height",100);
			q.terms().not().in("type", new String[]{"CRICLE","BOX"});
			System.out.println(q.getConditions());
		}
				
		
	}


	/**
	 * 初始化商品资源的扩展字段信息
	 */
	private void initResourceMetadata() {
		
	}

	/**
	 * 初始化用户的扩展字段信息
	 */
	private void initUserExtendInfo() {
		// TODO Auto-generated method stub
		
	}
}
