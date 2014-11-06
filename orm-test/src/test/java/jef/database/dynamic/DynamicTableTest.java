package jef.database.dynamic;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.NativeQuery;
import jef.database.QB;
import jef.database.RecordHolder;
import jef.database.RecordsHolder;
import jef.database.VarObject;
import jef.database.dialect.ColumnType;
import jef.database.meta.TupleMetadata;
import jef.database.query.Func;
import jef.database.query.Join;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseDestroy;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.onetable.model.TestEntity;
import jef.script.javascript.Var;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 这个类测试动态表的创建、单表操作、批操作、查询、删除、表修改等功能
 * 
 * @author jiyi
 * 
 */
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
public class DynamicTableTest extends org.junit.Assert {
	private DbClient db;
	private TupleMetadata meta;
	private TupleMetadata GroupTable;
	
	/**
	 * 添加了@DatabaseInit注解的方法将在每次连接到数据库后执行。
	 * @throws SQLException
	 */
	@DatabaseInit
	public void prepareTable() throws SQLException {
		db.dropTable(meta);
		db.dropTable(GroupTable);
		
		db.createTable(meta);
		db.createTable(GroupTable);

	}
	/**
	 * 添加了@DatabaseDestroy註解的方法在每次数据库关闭时执行
	 */
	@DatabaseDestroy
	public void destoryTable(){
		
	}
	
	public DynamicTableTest(){
		meta = new TupleMetadata("URM_SERVICE_1");
		meta.addColumn("id", new ColumnType.AutoIncrement(8));
		meta.addColumn("name", new ColumnType.Varchar(100));
		meta.addColumn("pname", new ColumnType.Varchar(100));
		meta.addColumn("flag", new ColumnType.Boolean());
		meta.addColumn("photo", new ColumnType.Blob());
		meta.addColumn("groupid", new ColumnType.Int(10));
//		meta.addIndex("pname", "unique");
//		meta.addIndex(new String[]{"groupid","pname","name","flag"}, "unique");
		
		GroupTable = new TupleMetadata("URM_GROUP");
		GroupTable.addColumn("id", new ColumnType.AutoIncrement(8));
		GroupTable.addColumn("serviceId", new ColumnType.Int(8));
		GroupTable.addColumn("name", new ColumnType.Varchar(100));
		GroupTable.addCascadeOneToMany("services", meta, QB.on(GroupTable.f("id"),meta.f("groupid")));
	}
	

	@Test
	public void testTuple(){
		TupleMetadata meta=new TupleMetadata("asdf");
		meta.addColumn("aa", new ColumnType.AutoIncrement(10));
		meta.getAllColumnNames();
		meta.getColumns();
		meta.removeColumn("aa");
		System.out.println(meta);
	}
	
	
	/**
	 * 测试插入记录到动态表
	 * @throws SQLException
	 */
	@Test
	public void testInsert() throws SQLException {
		try{
			doInsert();
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}
	}

	private int doInsert() throws SQLException {
		VarObject obj = new VarObject(meta);
		obj.put("name", "MyName is Jiyi");
		obj.put("pname", "assa");
		obj.put("flag", false);
		obj.put("photo", new File("c:/config.sys"));//将本地文件存入BLOB
		db.insert(obj);
		return (Integer) obj.get("id");
	}

	/**
	 * 测试批量插入动态表
	 * @throws SQLException
	 */
	@Test
	public void testInsertBatch() throws SQLException {
		File file=null;
		file=new File("c:/bootmgr");
		if(!file.exists()){
			file=null;
			throw new IllegalArgumentException("测试用的本地文件没有找到！");
		}
		VarObject obj1 = meta.newInstance();
		obj1.put("name", "My Name is Jiyi");
		obj1.put("pname", "assa");
		obj1.put("flag", false);
		obj1.put("photo", file);

		VarObject obj2 = meta.newInstance();
		obj2.put("name", "My Name is Jiyi");
		obj2.put("pname", "assa");
		obj2.put("flag", false);
		obj2.put("photo", file);

		VarObject obj3 = meta.newInstance();
		obj3.put("name", "My Name is Jiyi");
		obj3.put("pname", "assa");
		obj3.put("flag", false);
		obj3.put("photo", file);

		VarObject obj4 = meta.newInstance();
		obj4.put("name", "My Name is Jiyi");
		obj4.put("pname", "assa");
		obj4.put("flag", false);
		obj4.put("photo", file);
		db.batchInsert(Arrays.asList(obj1, obj2, obj3, obj4));
	}
	
	/**
	 * 测试更新动态表
	 * @throws SQLException
	 */

	@Test
	public void testUpdate() throws SQLException {
		int id = doInsert();

		VarObject obj1 = meta.newInstance();
		obj1.set("id", id);
		VarObject obj2 = db.load(obj1);
		System.out.println(obj2);

		obj2.set("name", "XFire");
		db.update(obj2);
	}
	
	/**
	 * 测试批量更新动态表
	 * @throws SQLException
	 */
	@Test
	public void testUpdateBatch() throws SQLException {
		testInsert();
		List<VarObject> result = db.select(QB.create(meta));
		System.out.println(result.size());
		int n = 0;
		for (VarObject element : result) {
			element.set("name", "test" + n++);
		}
		db.batchUpdate(result);

	}

	/**
	 * 测试删除记录
	 * @throws SQLException
	 */
	@Test
	public void testRemove() throws SQLException {
		testInsert();
		List<VarObject> result = db.select(QB.create(meta));
		if (result.size() > 0) {
			db.delete(result.get(0));
		}
		int deleted = db.delete(QB.create(meta));
		assertTrue(deleted > 0);
	}

	/**
	 * 测试批量删除记录
	 * @throws SQLException
	 */
	@Test
	public void testRemoveBatch() throws SQLException {
		testInsertBatch();
		List<VarObject> result = db.select(QB.create(meta));
		db.executeBatchDeletion(result);

	}

	/**
	 * 测试NativeQuery
	 * @throws SQLException
	 */
	@Test
	public void testSelectNativeQuery() throws SQLException {
		testInsertBatch();
		NativeQuery<VarObject>  q= db.createNativeQuery("select * from URM_SERVICE_1", meta);
		List<VarObject> result = q.getResultList();
		VarObject first=result.get(0);
		System.out.println(first);
	}
	
	/**
	 * 测试选出记录，并直接在游标上修改 (单条)
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({"sqlite","sqlserver"})
	public void testLoadForUpdate() throws SQLException {
		int id = doInsert();
		VarObject var = meta.newInstance();
		var.set("id", id);
		RecordHolder<VarObject> holder = db.loadForUpdate(var);

		holder.get().set("name", "呵呵");
		holder.commit();
		holder.close();

		var = db.load(var);
		assertEquals(var.get("name"), "呵呵");
	}

	/**
	 * 测试选出记录，并直接在游标上修改 (多条)
	 * 包括在游标上直接创建新记录， 以及删除就记录
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({"sqlite","sqlserver"})
	public void testSelectForUpdate() throws SQLException {
		doInsert();
		doInsert();
		int max=doInsert();
		RecordsHolder<VarObject> holder = db.selectForUpdate(QB.create(meta), null);
		
		int n = 0;
		//更新数据
		for (VarObject var : holder.get()) {
			var.set("name", "更新字段" + n++);
		}
		
		//插入一条记录
		if(holder.supportsNewRecord()){
			VarObject insert=holder.newRecord();
//			insert.set("id", ++max);
			insert.set("name", "新插入的记录");	
		}
		
		holder.commit(false); // 提交，但不关闭结果集
		
		// 继续修改
		for (VarObject var : holder.get()) {
			holder.delete(var);
		}
		holder.commit();
		List<VarObject> result = db.select(QB.create(meta));
		System.out.println("Size=" + result.size());
		assertEquals(result.size(), holder.supportsNewRecord()?1:0); //之前查出的记录全部被删除，只剩下新插入的记录卡了

		//打印出操作后的全部记录
		for (VarObject var : result) {
			System.out.println(var.get("name"));
		}
		//进一步测试：之前测试发现在Derby上，在执行完此次操作后执行insert操作，会出现主键冲突，因此加以测试
		doInsert();
	}

	/**
	 * 测试修改表结构，在表中添加两个字段
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({"sqlite","sqlserver"})
	public void testAlterTable_AddColumn() throws SQLException {
		meta.addColumn("addColumn1", new ColumnType.Date());
		meta.addColumn("addColumn2", new ColumnType.TimeStamp().notNull().defaultIs(Func.now));
		try {
			db.refreshTable(meta,ps);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test
	@IgnoreOn(allButExcept="sqlite")
	public void testAlterTable_AddColumn_forSqlite() throws SQLException {
		meta.addColumn("addColumn1", new ColumnType.Date());
		meta.addColumn("addColumn2", new ColumnType.TimeStamp());
		try {
			db.refreshTable(meta,ps);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 测试修改表结构，在表中删除字段
	 * @throws SQLException
	 */
	@Test
	public void testAlterTable_RemoveColumn() throws SQLException {
		meta.removeColumn("flag");
		meta.removeColumn("photo");
		try {
			db.refreshTable(meta,ps);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}

	}
	
//	private ProgressSample ps=new ProgressSample();
	private ProgressSample ps=null;

	/**
	 * 测试修改表结构，变更字段的类型，延长varchar长度，并且设置为not null
	 * @throws SQLException
	 */
	@Test
	public void testAlterTable_ChangeColumn() throws SQLException {
		meta.updateColumn("flag1", new ColumnType.Boolean());
		meta.updateColumn("name", new ColumnType.Varchar(200).notNull());
		try {
			db.refreshTable(meta,ps);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * 测试其他单表查询
	 * @throws SQLException
	 */
	@Test
	public void testFreeConditionQuery() throws SQLException{
		this.prepareTable();
		doInsert();
		Query<VarObject> q=QB.create(meta);
		q.addCondition(meta.f("name"), Operator.MATCH_START,"MyName");
		List<VarObject> list=db.select(q);
		assertTrue(list.size()>0);
		
	}
	
	/**
	 * 测试两张动态表关联查询
	 * @throws SQLException
	 */
	@Test
	public void test2TableJoin() throws SQLException{
		db.truncate(meta);
		db.truncate(GroupTable);
		//准备数据
		try{
			int key1=doInsert();
			VarObject group=GroupTable.newInstance();
			group.set("serviceId", key1);
			group.set("name", "We are about to make all these cases be fine");
			db.insert(group);
			System.out.println(group.get("id"));//得到t2的自增主键
			
			//开始
			Query<VarObject> t1=QB.create(meta);
			Query<VarObject> t2=QB.create(GroupTable);
			t2.addCondition(GroupTable.f("name"),Operator.MATCH_ANY ,"these cases");
			Join join=QB.leftJoin(t1, t2, QB.on(meta.f("id"), GroupTable.f("serviceId")));
			
			Selects select=QB.selectFrom(join);
			select.allColumns(t1);
			select.column(GroupTable.f("id"));
			select.sqlExpression("t1.name || t2.name").as("MyColumn");
			List<Var> object=db.selectAs(join, Var.class);
			assertTrue(object.size()>0);
			Var obj=object.get(0);
			System.out.println(obj);
			assertEquals("MyName is JiyiWe are about to make all these cases be fine",obj.get("MyColumn"));
			System.out.println(object);	
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}
		
	}
	
	/**
	 * 测试两个动态之间存在1对多关系时的级联操作
	 * @throws SQLException
	 */
	@Test
	public void testCascade1vN() throws SQLException{
		db.delete(QB.create(meta));
		int id;
		{
			/**
			 * 级联插入
			 */
			VarObject group=GroupTable.newInstance();
			group.set("name", "My Group 1");
			group.set("services", Arrays.asList(meta.newInstance().set("name", "service1"),meta.newInstance().set("name", "service2")));
			db.insertCascade(group);
			id=(Integer)group.get("id");
			System.out.println("新插入的group对象id为:"+id);
			
			//查询时即检查数据
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
			List<VarObject> list=(List<VarObject>) group.get("services");
			list.get(0).set("pname",list.get(0).get("name"));
			list.get(0).set("name", null);
			list.get(0).set("flag", false);  //修改一个子表的记录
			
			list.remove(1);					//删除一个子表的记录
			
			list.add(meta.newInstance().set("name", "service3"));//新增一个子表的记录
			/*
			 * 这个操作会对应4个SQL操作，分别用来更新父表、更新子表、删除子表记录、插入子表记录
			 */
			db.updateCascade(group); //
			
			//检查数据
			group=db.load(GroupTable.newInstance().set("id", id));
			assertEquals("更新字段",group.get("name"));
			assertEquals(123,group.get("serviceId"));
			int count=0;
			for(VarObject child: (List<VarObject>)group.get("services")){
				count++;
				if(child.get("name")==null){
					assertNotNull(child.get("pname"));
				}
				if("service2".equals(child.get("name"))){
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
			int count=db.count(QB.create(meta));
			assertEquals(0,count);
		}
		
	}
	
	@IgnoreOn(allButExcept="oracle")
	@Test
	public void testX() throws SQLException{
		TupleMetadata meta=new TupleMetadata("XXASD");
		meta.addColumn("XXX", new ColumnType.Double(17, 12));
		db.dropTable(TestEntity.class);
		db.dropTable(meta);
		db.createTable(meta);
		db.createTable(TestEntity.class);
	}
	
}
