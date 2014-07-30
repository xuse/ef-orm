package jef.orm.multitable2;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.PagingIterator;
import jef.database.QB;
import jef.database.Session;
import jef.database.Transaction;
import jef.database.query.Query;
import jef.database.query.RefField;
import jef.database.query.Selects;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.orm.multitable2.model.Child;
import jef.orm.multitable2.model.EnumationTable;
import jef.orm.multitable2.model.Leaf;
import jef.orm.multitable2.model.Parent;
import jef.orm.multitable2.model.Root;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * 多表数据库操作 
 * @author Administrator
 *
 */
@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	 @DataSource(name="oracle",url="${oracle.url}",user="${oracle.user}",password="${oracle.password}"),
	 @DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	 @DataSource(name="postgresql",url="${postgresql.url}",user="${postgresql.user}",password="${postgresql.password}"),
	 @DataSource(name="derby",url="jdbc:derby:./db;create=true"),
	 @DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	 @DataSource(name = "sqlite", url = "jdbc:sqlite:test.db")
})
public class CascadeModel2Test extends org.junit.Assert{
	private DbClient db;
	
	@BeforeClass
	public static void setUp() throws SQLException{
		EntityEnhancer en=new EntityEnhancer();
		en.enhance("jef.orm.multitable2.model");
	}
	
	@DatabaseInit
	public void prepareData() throws SQLException {
		db.dropTable(Root.class,Parent.class,Child.class,Leaf.class,EnumationTable.class);
		db.createTable(Root.class,
				Parent.class,
				Child.class,
				Leaf.class,
				EnumationTable.class);
		doInsert(db);
	}
	
	
	/**
	 * 案例一： 一次性插入四层级联关系的记录
	 * @throws SQLException 
	 */
	@Test
	public void testInsertCascade() throws SQLException{
		Transaction db=this.db.startTransaction();
		int count1=db.count(QB.create(Root.class));
		int count2=db.count(QB.create(Parent.class));
		int count3=db.count(QB.create(Child.class));
		int count4=db.count(QB.create(Leaf.class));
		int rootId=doInsert(db);
		assertEquals(count1+1, db.count(QB.create(Root.class)));
		assertEquals(count2+1, db.count(QB.create(Parent.class)));
		assertEquals(count3+2, db.count(QB.create(Child.class)));
		assertEquals(count4+4, db.count(QB.create(Leaf.class)));
		
		//检测，从数据库读取所有数据
		{
			int n=0;
			Root root=new Root(rootId);
			root=db.load(root);
			n++;
			System.out.println("staep1");
			List<Parent> ps=root.getChildren();
			for(Parent p: ps){
				n++;
				System.out.println("staep2");
				List<Child> cs=p.getChildren();
				for(Child c:cs){
					n++;
					System.out.println("staep3");
					List<Leaf> leaf=c.getChildren();
					n+=leaf.size();
				}
			}
			assertEquals(8, n);
		}
		db.rollback();
	}

	/**
	 * 案例二： 一次性删除四层级联关系的记录
	 * @throws SQLException
	 */
	@Test
	public void testDelete() throws SQLException{
		Transaction db=this.db.startTransaction();
		int count1=db.count(QB.create(Root.class));
		int count2=db.count(QB.create(Parent.class));
		int count3=db.count(QB.create(Child.class));
		int count4=db.count(QB.create(Leaf.class));
		int n=doDelete(db);
		assertEquals(count1-n, db.count(QB.create(Root.class)));
		assertEquals(count2-n, db.count(QB.create(Parent.class)));
		assertEquals(count3-2*n, db.count(QB.create(Child.class)));
		assertEquals(count4-4*n, db.count(QB.create(Leaf.class)));
		db.rollback();
	}

	private int doDelete(Session db) throws SQLException {
		Query<Root> root=QB.create(Root.class);
		root.addCondition(QB.eq(Root.Field.code, "test1"));
		return db.deleteCascade(root.getInstance());
	}

	
	private int doInsert(Session db) throws SQLException {
		Root root=new Root();
		Parent parent=new Parent();
		Child child1=new Child();
		Child child2=new Child();
		Leaf leaf1=new Leaf();
		Leaf leaf2=new Leaf();
		Leaf leaf3=new Leaf();
		Leaf leaf4=new Leaf();

		root.setChildren(Arrays.asList(parent));
		parent.setChildren(Arrays.asList(child1,child2));
		child1.setChildren(Arrays.asList(leaf1,leaf2));
		child2.setChildren(Arrays.asList(leaf3,leaf4));

		root.setCode("test1");
		child1.setCode("test1");
		child2.setCode("test2");
		leaf1.setCode("test1");
		leaf2.setCode("test1");
		leaf3.setCode("test1");
		leaf4.setCode("test1");
		root.setName("测试Root");
		parent.setName("测试parent");
		child1.setName("测试child1");
		child2.setName("测试child2");
		leaf1.setName("测试leaf1");
		leaf2.setName("测试leaf2");
		leaf3.setName("测试leaf3");
		leaf4.setName("测试leaf4");
		db.insertCascade(root);
		return root.getId();
	}

	
	/**
	 * @案例描述
	 * Like的用法：四种运算当中，前三种都是Like的进一步封装，框架会执行关键字中的转义，添加通配符等操作。
	 * 一般在做页面查询等功能时，这三种操作就足够用了。而且没有被SQL注入攻击的可能。
	 * 
	 * 最后一个操作就是数据库原生的like操作。
	 * 
	 * @测试功能
	 *  matchStart运算符  matchEnd运算符 matchAny运算符 like运算符
	 * @throws SQLException
	 */
	@Test
	public void testLike() throws SQLException {
		db.select(QB.create(Root.class).addCondition(
				QB.matchStart(Root.Field.name, "aa_")
		));
		db.select(QB.create(Root.class).addCondition(
				QB.matchEnd(Root.Field.name, "aa_")
		));
		db.select(QB.create(Root.class).addCondition(
				QB.matchAny(Root.Field.name, "aa_")
		));
		db.select(QB.create(Root.class).addCondition(
				QB.like(Root.Field.name, "%aa_")
		));
		
		//此处为演示like操作的四种用法，观察输出即可
	}
	

	/**
	 * 在这个案例中，演示添加附加关联关系的例子。
	 * 四个对象的关系如下所示
	 * <pre>
	 * Leaf (*--1) Child (*--1) parent (*--1) Root</pre>
	 * 当我们查询Leaf时，默认只查询到Child表，也就是两表关联。而实际上从Leaf到Parent和Root表都是多对一关系。
	 * 一次查询其实就可以将Leaf和其所属的上级对象全部查出。
	 * 为此，可以通过以下两种方式之一将Parent表和Root表添加到这次的查询范围中<ol>
	 * <li>使用RefField, FilterCondition等方式添加基于表Root和Parent的排序列或查询条件</li>
	 * <li>调用addExtendQuery方法，提示框架查询时带上指定的表进行查询</li>
	 * </ol>
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testTable4Population() throws SQLException {
		doDelete(db);
		doInsert(db);
		//案例二: 查询数据 一次性完成四层级联关系的填充
		Query<Leaf> leaf = QB.create(Leaf.class);
		leaf.addCascadeCondition(QB.matchAny(Child.Field.name, "测试"));
		
		leaf.addOrderBy(true, new RefField(Parent.Field.name));
		leaf.addExtendQuery(QB.create(Root.class));
		PagingIterator<Leaf> leafs = db.pageSelect(leaf, 4);
		assertEquals(4,leafs.getTotal());
		for (Leaf l : leafs.next()) {
			Child c=l.getParent();
			Parent p=c==null?null:c.getParent();
			Root t=p==null?null:p.getRoot();
			assertNotNull(t);
			assertEquals("测试Root", t.getName().trim()); //因为root是char(40)，数据库会补空格到40字符
			assertEquals("测试parent", p.getName());
		}
	}

	/**
	 * 测试带Ref条件的查询
	 * @throws SQLException
	 */
	@Test
	public void testSelect() throws SQLException {
		System.out.println("=========== testSelect Begin ==========");
		Query<Leaf> q=QB.create(Leaf.class);
		
		q.setAllRecordsCondition();
		q.addExtendQuery(QB.create(Parent.class)); //通过addExtendQuery告知Parent表也参与联查。
		q.addCondition(new RefField(Root.Field.name),Operator.MATCH_ANY,"测试");
		q.addOrderBy(false, Leaf.Field.childId);
		q.addOrderBy(true, new RefField(Root.Field.name));
		q.setCascadeViaOuterJoin(true);
		List<Leaf> ps=db.select(q.getInstance());
		assertEquals(4,ps.size());
		System.out.println("=========== testSelect End ==========");
	}
	
	/**
	 * 测试级联场景下获取数量
	 * @throws SQLException
	 */
	@Test
	public void testCount() throws SQLException {
		System.out.println("=========== testCount Begin ==========");
		Query<Leaf> q=QB.create(Leaf.class);
		q.setCascadeViaOuterJoin(true);
		q.setAllRecordsCondition();
		q.addExtendQuery(QB.create(Parent.class));//
		q.addCondition(new RefField(Root.Field.name),Operator.MATCH_ANY,"123");
		System.out.println(db.count(q));
		System.out.println("=========== testCount End ==========");
		
//		select count(*)
//		  from leaf T1
//		  left join child T2 ON T1.childId = T2.id
//		  left join enumationtable T3 ON T1.code = T3.code
//		                             and T3.type = '4'
//		  left join parent T4 ON T2.parentId = T4.id
//		  left join root T5 ON T4.rootId = T5.ID1
//		 where T5.THE_NAME like ? escape '/'
	}
	
	@Test
	@IgnoreOn(allButExcept="hsqldb")
	public void testCountDistinct() throws SQLException {
		Query<Leaf> q=QB.create(Leaf.class);
		q.setCascade(true);
		
		Selects items=QB.selectFrom(q);
		items.column(Leaf.Field.name);
		items.setDistinct(true);
		q.getResultTransformer().setResultType(String.class);
		q.setMaxResult(1);
		
		
		long total=db.count(q);//取总数
		LogUtil.show(db.select(q)); //查询，由于总数被限制为1，因此只会查出第一条。
	}
	
	
	/**
	 * TODO 当使用了distinct后的自动转换count语句非常复杂，尤其是和分库分表一起使用以后几乎就是一团乱麻。
	 * 目前支持得不是很好，还需要补充更多案例。
	 * @throws SQLException
	 */
	@Test
	public void testPageWithDistinct() throws SQLException {
		Query<Leaf> q=QB.create(Leaf.class);
		q.setCascade(false);
		Selects items=QB.selectFrom(q);
		items.column(Leaf.Field.name);
		items.setDistinct(true);
//		db.select(q);
		q.getResultTransformer().setResultType(String.class);
		PagingIterator<String> rp=db.pageSelect(q, 3);
		LogUtil.show(rp.next());
		
		
		
	}
	
	@Test
	public void testCount2() throws SQLException {
		System.out.println("=========== testCount Begin ==========");
		Query<Leaf> q=QB.create(Leaf.class);
		q.addExtendQuery(QB.create(Parent.class));//
		q.setCascadeViaOuterJoin(true);
		q.addCondition(QB.like(new RefField(Root.Field.code),"t%t_"));
		int count=db.count(q);
		assertEquals(4, count);
		System.out.println("=========== testCount End ==========");
	}
	
	@Test
	public void testPaging() throws SQLException{
		System.out.println("=========== testPaging(Model2)  Begin ==========");
		Query<Leaf> q=QB.create(Leaf.class);
		PagingIterator<Leaf> page=db.pageSelect(q, 5);
		System.out.println("Total Page:" + page.getTotalPage());
		for(;page.hasNext();){
			List<Leaf> list=page.next();
		}
		
		
		Query<Leaf> q2=QB.create(Leaf.class);
		q2.addCondition(Leaf.Field.name,Operator.MATCH_ANY,"a");
		page=db.pageSelect(q2, 5);
		System.out.println("Total Page:" + page.getTotalPage());
		for(;page.hasNext();){
			List<Leaf> list=page.next();
		}
		
		
		Query<Leaf> q3=QB.create(Leaf.class);
		q3.addCondition(new RefField(Parent.Field.name),Operator.MATCH_ANY,"a");
		q3.setCascadeViaOuterJoin(true);
		page=db.pageSelect(q2, 5);
		System.out.println("Total Page:" + page.getTotalPage());
		for(;page.hasNext();){
			List<Leaf> list=page.next();
		}
		System.out.println("=========== testPaging(Model2)  End ==========");
	}
	
	
	/**
	 * @案例描述 测试关联查询
	 * 
	 *       Leaf当中有到Child对象的引用，因此查询时会关联这张表
	 * 
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testTableCascade1() throws SQLException {
		Query<Leaf> leaf = QB.create(Leaf.class);
		List<Leaf> leaves = db.select(leaf);
		for (Leaf l : leaves) {
			if (l.getParent() != null) {
				assertEquals(l.getParentName(), l.getParent().getName());
			} else {
				assertNull(l.getParentName());
			}
		}
	}

	/**
	 * 测试关联查询
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testTableCascade2() throws SQLException {
		Query<Child> q = QB.create(Child.class);
		q.addCascadeCondition(QB.eq(Leaf.Field.name, "6")); // 关联查询条件
		q.setAttribute("aaa", "3");
		List<Child> leaves = db.select(q);
		for (Child l : leaves) {
			if (l.getParent() != null) {
				assertEquals(l.getParentId(), Integer.valueOf(l.getParent().getId()));
			} else {
				assertNull(l.getParentId());
			}
			if (l.getChildren() != null && l.getChildren().size() > 0) {
				for (Leaf leaf : l.getChildren()) {
					assertEquals(leaf.getParent().getId(), l.getId());
				}
			}
		}
	}


}

