package jef.database.dynamic;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.VarObject;
import jef.database.dialect.ColumnType;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseDestroy;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 静态表与动态表的关联操作测试
 * @author jiyi
 *
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
public class DynamicAndStaticTableTest  extends org.junit.Assert {
	private DbClient db;
	private TupleMetadata GroupTable;
	
	/**
	 * 添加了@DatabaseInit注解的方法将在每次连接到数据库后执行。
	 * @throws SQLException
	 */
	@DatabaseInit
	public void prepareTable() throws SQLException {
		db.dropTable(ServiceItem.class);
		db.dropTable(GroupTable);
		
		db.createTable(ServiceItem.class);
		db.createTable(GroupTable);

	}
	/**
	 * 添加了@DatabaseDestroy註解的方法在每次数据库关闭时执行
	 */
	@DatabaseDestroy
	public void destoryTable(){
		
	}
	
	public DynamicAndStaticTableTest(){
		GroupTable = new TupleMetadata("URM_GROUP");
		GroupTable.addColumn("id", new ColumnType.AutoIncrement(8));
		GroupTable.addColumn("serviceId", new ColumnType.Int(8));
		GroupTable.addColumn("name", new ColumnType.Varchar(100));
		GroupTable.addCascadeOneToMany("services", MetaHolder.getMeta(ServiceItem.class), QB.on(GroupTable.f("id"),ServiceItem.Field.groupId));
		
	}
	
	/**
	 * 测试动态表和静态表之间存在1对多级联关系时的操作
	 */
	@Test
	public void testStaticCascade1vN() throws SQLException{
		int id;
		{
			/**
			 * 级联插入
			 */
			VarObject group=GroupTable.newInstance();
			group.set("name", "My Group 1");
			ServiceItem s1=new ServiceItem();
			s1.setName("service1");
			
			ServiceItem s2=new ServiceItem();
			s2.setName("service2");
			
			group.set("services", Arrays.asList(s1,s2));
			db.insertCascade(group);
			id=(Integer)group.get("id");
			System.out.println("新插入的group对象id为:"+id);
			
		}
		
		{
			/**
			 * 级联查询
			 */
			VarObject group=db.load(GroupTable.newInstance().set("id", id));
			assertEquals(id, group.get("id"));
			@SuppressWarnings("unchecked")
			List<VarObject> list=(List<VarObject>) group.get("services");
			assertEquals(2, list.size());  //子表的记录也一同查出
			System.out.println(list);
			
		}
		{
			/**
			 * 级联更新
			 */
			VarObject group=db.load(GroupTable.newInstance().set("id", id));
			assertEquals(id, group.get("id"));
			
			group.set("name", "更新字段");
			group.set("serviceId", 123);
			@SuppressWarnings("unchecked")
			List<ServiceItem> list=(List<ServiceItem>) group.get("services");
			list.get(0).setPname(list.get(0).getName());
			list.get(0).setName(null);
			list.get(0).setFlag(false);  //修改一个子表的记录
			
			list.remove(1);					//删除一个子表的记录
			
			ServiceItem s=new ServiceItem();
			s.setName("service3");
			list.add(s);//新增一个子表的记录
			/*
			 * 这个操作会对应4个SQL操作，分别用来更新父表、更新子表、删除子表记录、插入子表记录 ,语句举例如下： 
			 */
			db.updateCascade(group); //
			
			//检查数据
			group=db.load(GroupTable.newInstance().set("id", id));
			assertEquals("更新字段",group.get("name"));
			assertEquals(123,group.get("serviceId"));
			int count=0;
			for(ServiceItem child: (List<ServiceItem>)group.get("services")){
				count++;
				if(child.getName()==null){
					assertNotNull(child.getPname());
				}
				if("service2".equals(child.getName())){
					throw new IllegalStateException();//这条记录应该已经被删除了，不可能查出来的。
				}
			}
			assertEquals(2,count);
			
		}
		{
			/**
			 * 级联删除，这个操作将删除子表中相关的2条记录。然后再删除父表中的记录
			 */
			db.deleteCascade(GroupTable.newInstance().set("id", id));
			//检查数据
			VarObject group=db.load(GroupTable.newInstance().set("id", id));
			assertNull(group);
			int count=db.count(QB.create(ServiceItem.class));
			assertEquals(0,count);
		}
		
	}
	
	
	//从静态表一端添加动态表的引用问题
}
