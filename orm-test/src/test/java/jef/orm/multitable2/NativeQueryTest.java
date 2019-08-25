package jef.orm.multitable2;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import jef.common.log.LogUtil;
import jef.common.wrapper.Holder;
import jef.common.wrapper.IntRange;
import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.DbMetaData.ObjectType;
import jef.database.NamedQueryConfig;
import jef.database.NativeCall;
import jef.database.NativeQuery;
import jef.database.ORMConfig;
import jef.database.PagingIterator;
import jef.database.QB;
import jef.database.RecordHolder;
import jef.database.RecordsHolder;
import jef.database.SqlTemplate;
import jef.database.Transaction;
import jef.database.VarObject;
import jef.database.jdbc.result.IResultSet;
import jef.database.jmx.JefFacade;
import jef.database.meta.FBIField;
import jef.database.meta.Feature;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;
import jef.database.query.Func;
import jef.database.query.Join;
import jef.database.query.OutParam;
import jef.database.query.Query;
import jef.database.query.RefField;
import jef.database.query.Selects;
import jef.database.query.SqlExpression;
import jef.database.query.UnionQuery;
import jef.database.support.RDBMS;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.IgnoreOn;
import jef.database.test.JefJUnit4DatabaseTestRunner;
import jef.database.test.LogListener;
import jef.database.wrapper.populator.Mapper;
import jef.database.wrapper.populator.Mappers;
import jef.database.wrapper.populator.Transformer;
import jef.orm.multitable.model.Person;
import jef.orm.multitable2.model.Child;
import jef.orm.multitable2.model.EnumationTable;
import jef.orm.multitable2.model.Leaf;
import jef.orm.multitable2.model.Parent;
import jef.orm.multitable2.model.Root;
import jef.orm.multitable2.model.TreeTable;
import jef.orm.onetable.model.Foo;
import jef.script.javascript.Var;
import jef.tools.DateUtils;
import jef.tools.PageLimit;
import jef.tools.string.RandomData;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "oracle", url = "${oracle.url}", user = "${oracle.user}", password = "${oracle.password}"),
	@DataSource(name = "mysql", url = "${mysql.url}", user = "${mysql.user}", password = "${mysql.password}"),
	@DataSource(name = "postgresql", url = "${postgresql.url}", user = "${postgresql.user}", password = "${postgresql.password}"),
	@DataSource(name = "hsqldb", url = "${hsqldb.url}", user = "sa", password = ""),
	@DataSource(name = "derby", url = "${derby.url}"),
	@DataSource(name = "sqlite", url = "${sqlite.url}"),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NativeQueryTest extends org.junit.Assert {
	private DbClient db;

	@BeforeClass
	public static void enhacne() {
		ORMConfig.getInstance().setSelectTimeout(20);
		ORMConfig.getInstance().setUpdateTimeout(20);
		ORMConfig.getInstance().setDeleteTimeout(20);
	}

	@DatabaseInit
	public void setUp() throws SQLException {
		try {
			dropTable();
			createtable();
			prepareData();
		} catch (Exception e) {
			LogUtil.exception(e);
		}
		JefFacade.getOrmConfig().setDebugMode(true);
	}

	/**
	 * 准备数据
	 * 
	 * @throws SQLException
	 */
	private void prepareData() throws SQLException {
		{
			Root[] root = RandomData.newArrayInstance(Root.class, 5);
			int x = 0;
			for (Root r : root) {
				r.setName(String.valueOf(x++));
			}
			db.batchInsert(Arrays.asList(root));
		}
		{
			Leaf[] leaf = RandomData.newArrayInstance(Leaf.class, 5);
			for (int i = 0; i < leaf.length; i++) {
				Leaf e = leaf[i];
				e.setChildId(RandomData.randomInteger(1, 5));
			}
			db.batchInsert(Arrays.asList(leaf));
		}
		{
			EnumationTable[] enums = RandomData.newArrayInstance(EnumationTable.class, 10);
			for (int i = 0; i < enums.length; i++) {
				EnumationTable e = enums[i];
				e.setCode("code" + i);
				e.setType("1");
				e.setEnable(true);
			}
			db.batchInsert(Arrays.asList(enums));

			enums = RandomData.newArrayInstance(EnumationTable.class, 10);
			for (int i = 0; i < enums.length; i++) {
				EnumationTable e = enums[i];
				e.setEnable(true);
				e.setCode("code" + i);
				e.setType("2");
			}
			db.batchInsert(Arrays.asList(enums));
		}

		NamedQueryConfig config = new NamedQueryConfig("test_sql_in", "select count(*) from Person_table where id in (:names<int>)", false, 0);
		if (db.load(config) != null) {
			db.delete(config);
		}
		db.insert(config);

		Parent[] parent = RandomData.newArrayInstance(Parent.class, 4);
		for (Parent p : parent) {
			db.insertCascade(p);
		}
		{
			Child[] children = RandomData.newArrayInstance(Child.class, 4);
			for (int i = 0; i < children.length; i++) {
				children[i].setParentId(parent[i].getId());// 给每个Parent对象再加一个child
				children[i].setCode("code" + i);
			}
			db.batchInsert(Arrays.asList(children));
		}
	}

	private void createtable() throws SQLException {
		db.createTable(Root.class,Foo.class);
		db.createTable(Parent.class,Child.class,Leaf.class);
		db.createTable(EnumationTable.class);

		db.createTable(TreeTable.class);
		db.createTable(Person.class);
		if(db.getProfile().getName()!=RDBMS.oracle && db.getProfile().getName()!=RDBMS.mysql ){
			if(!db.existsTable("dual")){
				db.executeSql("create table dual(X varchar(20))");
			}
			long count=db.getSqlTemplate(null).countBySql("select count(*) from dual");
			if(count==0){
				db.executeSql("insert into dual values('X')");
			}
		}
	}

	private void dropTable() throws SQLException {
		db.dropTable(Root.class,Parent.class,Child.class,Leaf.class);
		db.dropTable(EnumationTable.class);
		db.dropTable(TreeTable.class);
	}

	/**
	 * @案例描述 测试自定义的SQL语句的分页查询. 这个案例中，由于使用了distinct关键字，因此count语句也必须使用distinct关键字.
	 *       对于MYSQL，由于不支持 || 字符串拼接，因此其count会以concat(a,b)的方式计算得出
	 * 
	 * @测试功能 框架可以将SQL语句根据当前数据库改写为对应的count语句，从而自动获得分页计数。甚至包括distincat这种特出场景
	 * @throws SQLException
	 */
	@Test
	public void testNewSQL() throws SQLException {
		long count = db.getSqlTemplate(null).countBySql("SELECT count(*) FROM PARENT T, Child T1 WHERE T.ID = T1.PARENTID");
		System.out.println(count);

		// ///////////////
		String sql = "SELECT distinct T.NAME PNAME, T1.NAME FROM parent T, child T1 WHERE T.ID = T1.PARENTID order by t.name";
		PagingIterator<Var> pp = db.pageSelect(sql, Var.class, 5);
		LogListener listener = new LogListener(".+group by T.NAME,T1.NAME.+");
		LogUtil.show(pp.hasNext());
		LogUtil.show(pp.next());
		if (db.getProfile(null).has(Feature.SUPPORT_CONCAT)) {
			String match = listener.getSingleMatch()[0];
			assertNotNull(match);
		} else {
			listener.close();
		}
		assertEquals(count, pp.getTotal());
		assertEquals(2, pp.getTotalPage());
		if(pp.hasNext()){
			LogUtil.show(pp.next());
		}
	}


	/**
	 * 在Criteria API中使用ResultTransformer
	 * @throws SQLException
	 */
	@Test
	public void testCriteriaQueryWithResultTransformer() throws SQLException {
		Query<Root> t1 = QB.create(Root.class);
		// t1.addCondition(Root.Field.name,Operator.MATCH_END,"A");
		Join join = QB.innerJoin(t1, QB.create(Parent.class), QB.on(Parent.Field.rootId, Root.Field.id));
		join.getResultTransformer().setResultType(Object[].class);
		
		//当查询结果为Object[]时，只会使用自定义Mapper
		
		// 测试1，数组
		join.getResultTransformer().addMapper(new Mapper<Object[]>() {
			@Override
			protected void transform(Object[] obj, IResultSet rs) throws SQLException {
				Foo foo = new Foo();
				foo.setModified(new Date());
				foo.setName("asadsd");
				foo.setId(123);
				obj[0] = foo;
			}
		});

		List<Object[]> result = db.select(join, null);

		// 测试2，普通对象
		Transformer t = join.getResultTransformer();
		t.setResultType(Holder.class);
		//当查询结果不为Object[]，框架会初始化若干缺省的映射器，此时最好清除缺省Mapper再添加自定义Mapper
		t.clearMapper(); // 清除上一次的自定义映射器
		t.ignoreAll();
		t.addMapper(new Mapper<Holder<Object>>() {
			@Override
			protected void transform(Holder<Object> obj, IResultSet rs) throws SQLException {
				Foo foo = new Foo();
				foo.setModified(new Date());
				foo.setName("asadsd");
				foo.setId(123);
				obj.set(foo);
			}
		});

		List<Holder> result1 = db.select(join, null);
		System.out.println(result);
		assertTrue(result.size() > 0);

	}

	/**
	 * 在NativeQuery中使用ResultTransformer
	 * @throws SQLException
	 */
	@Test
	public void testNativeQueryWithResultTransformer() throws SQLException {
		db.createTable(Child.class);
		{
			// parent//children//codeObj//codeText
			List<Child> l1 = db.select(QB.create(Child.class));
			System.out.println(l1);
		}

		NativeQuery<Child> query = db.createNativeQuery("select c.*,e.text,e.type,e.descrption,e.enable from child c, ENUMATIONTABLE e where c.CODE=e.CODE and e.TYPE='1' ", Child.class);
		query.getResultTransformer().addMapper(Mappers.toResultProperty("codeObj", EnumationTable.class));

		// 自己再添加一个
		{
			query.getResultTransformer().addMapper(new Mapper<Child>() {
				protected void transform(Child obj, IResultSet rs) throws SQLException {
					EnumationTable codeObj = new EnumationTable();
					codeObj.stopUpdate();
					codeObj.setCode(rs.getString("CODE"));
					codeObj.setType(rs.getString("TYPE"));
					codeObj.setDesc(rs.getString("DESCRPTION"));
					codeObj.setName(rs.getString("TEXT"));
					codeObj.setEnable(rs.getBoolean("ENABLE"));
					System.out.println("ENABLE=" + rs.getObject("ENABLE") + " obj value=" + codeObj.getEnable());
					obj.setCodeObj(codeObj);
				}
			});
		}

		List<Child> persons = query.getResultList();
		Child first = persons.get(0);
		assertNotNull(first.getCodeObj().getName());
		assertEquals(true, first.getCodeObj().getEnable());
	}

	/**
	 * 在NatiiveQuery中自定义ResultTransformer，返回多个对象（数组）。
	 * @throws SQLException
	 */
	@Test
	public void testNativeQueryReturnMulitpleObjects() throws SQLException {
		NativeQuery<Object[]> query = db.createNativeQuery("select c.*,e.* from child c, ENUMATIONTABLE e where c.CODE=e.CODE and e.TYPE='1' ", Object[].class);

		query.getResultTransformer().addMapper(Mappers.toArrayElement(2, Child.class));
		query.getResultTransformer().addMapper(Mappers.toArrayElement(1, EnumationTable.class));
		List<Object[]> persons = query.getResultList();
		assertNotNull(persons.get(0)[1]);
		assertNotNull(persons.get(0)[2]);
	}


	/**
	 * 这个案例测试直接将简单的列拼为数组进行返回。 如果所有的值可以确定都为String，可以用String[]作为返回格式
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn(allButExcept = "derby")
	public void testSlelectSimpleValueArray() throws SQLException {
		NativeQuery<Object[]> objs = db.createNativeQuery("select 'Asa' as  a ,'B' as b,1+1 as c, current_timestamp as D from dual", Object[].class);
		Object[] result = objs.getSingleResult();
		assertTrue(result[1].getClass() == String.class);
		assertTrue(result[2].getClass() == Integer.class);
		assertTrue(result[3].getClass() == Timestamp.class);
	}

	/**
	 * @错误用法案例
	 * 
	 * 这个案例中，输入的表达式实际上是有多个表达式拼合而成的SQL片段，不符合表达式的定义，因此无法查询。
	 * 
	 * @throws SQLException
	 */
	@Test(expected = Exception.class)
	@IgnoreOn(allButExcept = "derby")
	public void testSelectExpressionOfError() throws SQLException {
		Object[] result = db.getExpressionValue("'Asa' as  a ,'B' as b,1+1 as c, current_timestamp", Object[].class);
		System.out.println(result);
	}


	/**
	 * 
	 * 分类： Criteria API
	 * @案例描述 TreeTable中包含了一个对leaf表的引用。因此虽然查询时我们看似只查一张表，查询时会自动关联两张表。
	 *       同时我们可以使用RefField (Reference
	 *       Field)这个对象将关联表（B)的查询条件，甚至排序条件也传入表（A）的条件中。
	 * 
	 * @测试功能 使用RefField在ManyToOne或OneToOne时，设置关联表的查询条件和排序字段。
	 * @throws SQLException
	 */
	@Test
	public void testRefFieldRefWhererAndOrder() throws SQLException {
		Query<TreeTable> q = QB.create(TreeTable.class);
		q.addCondition(TreeTable.Field.name, "a");
		q.addCondition(new RefField(Leaf.Field.childId), 12);
		q.addOrderBy(true, new RefField(Leaf.Field.id));
		LogListener listener = new LogListener("select .* where (.+) order by (.+) \\|.+", Pattern.DOTALL);
		db.select(q);
		String[] match = listener.getSingleMatch();
		assertEquals("t1.name=? and t2.childid=?", match[0].toLowerCase().trim());
		assertEquals("t2.id asc", match[1].toLowerCase().trim());
	}

	/**
	 * 分类： Criteria API
	 * @案例描述 复杂条件下的关联查询和自定义Join混合使用
	 * 
	 * @测试功能 1. 在Query中添加两个相同的条件，实际使用时会自动合并成一个。 2. QB.or QB.eq QB.ge等条件生成的用法
	 *       3.xxxJoinWithRef可以将Join左边的对象自身的静态关联关系保留的情况下和新的表进行join
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCascadeAndJoin() throws SQLException {
		Child c = new Child();
		c.setName("a");
		c.setCode("code1");
		c.setParentId(10);
		db.insert(c);

		Parent parent = new Parent();
		parent.setName("parent");
		parent.setId(10);
		db.insert(parent);

		Query<Child> query = QB.create(Child.class);
		query.addCondition(QB.or(Condition.get(Child.Field.name, Operator.EQUALS, "a"), QB.ge(Child.Field.parentId, 5)));
		query.addCondition(QB.or(QB.eq(Child.Field.name, "a"), QB.ge(Child.Field.parentId, 5)));
		query.setAttribute("aaa", "3");

		// db.select(query);
		Query<EnumationTable> t3 = QB.create(EnumationTable.class);
		Join j = QB.leftJoinWithRef(query, t3, QB.on(Child.Field.name, EnumationTable.Field.code), QB.on(EnumationTable.Field.type, "2"));

		Selects select = QB.selectFrom(j);
		select.noColums(t3);
		j.setAttribute("aaa", "3");
		j.getResultTransformer().setResultType(Child.class);
		List<Child> list = db.select(j, null);

		LogUtil.show(list);
		assertEquals(db.loadBySql("select count(*) from child where name='a' or parentId>=5", Integer.class).intValue(), list.size());
	}

	/**
	 * 分类Criteria API
	 * 
	 * 带排序的自动分页查询
	 * @throws SQLException
	 */
	@Test
	public void testPageIngIn() throws SQLException {
		Query<Root> t1 = QB.create(Root.class);
		t1.addCondition(Root.Field.name, "123");
		t1.addCondition(QB.between(Root.Field.range, 1, 4));
		t1.addOrderBy(true, Root.Field.id);
		PagingIterator<Root> p = db.pageSelect(t1, 4);
		p.setOffset(3);
		p.getTotal();
	}

	@Test
	public void testAllColumns() throws SQLException {
		Query<Root> t1 = QB.create(Root.class);
		Query<Parent> t2 = QB.create(Parent.class);
		for (int i = 0; i < 10; i++) {
			Root iroot = RandomData.newInstance(Root.class);
			db.insert(iroot);
		}
		for (int i = 0; i < 10; i++) {
			Parent iparent = RandomData.newInstance(Parent.class);
			db.insert(iparent);
		}

		Join join = QB.innerJoin(t1, t2, QB.on(Root.Field.id, Parent.Field.id));
		Selects select = QB.selectFrom(join);
		select.allColumns(t1);
		select.noColums(t2);
		join.getResultTransformer().setResultType(Root.class);
		List<Root> root = db.select(join, null);
		System.out.println(root);
	}

	/**
	 * 一个充分定制化的查询 实际产生的SQL和testSQL是一样的，然后拼装到一个自定义的对象中去
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectJoin() throws SQLException {
		Query<Root> t1 = QB.create(Root.class);

		t1.addCondition(Root.Field.range, 1);
		t1.addCondition(Root.Field.name, "ads");

		Query<Leaf> t2 = QB.create(Leaf.class);
		t2.addCondition(Leaf.Field.name, "sasd");

		Join join = QB.innerJoin(t1, t2, QB.on(Root.Field.id, Leaf.Field.childId));
		t1.setOrderBy(true, Root.Field.id);
		// Query<EnumTable> t3 = join.leftJoin(QB.create(EnumTable.class),
		// QB.on(Root.Field.id, EnumTable.Field.id), QB.on(EnumTable.Field.type,
		// ">1"));
		// Query<EnumTable> t4 = join.leftJoin(QB.create(EnumTable.class),
		// QB.on(Leaf.Field.id, EnumTable.Field.id), QB.on(EnumTable.Field.type,
		// "2"));
		Selects select = QB.selectFrom(join);
		select.columns(t1, "name as rootName,id as rootId");
		select.columns(t2, "id as id,name as name");
		// select.column(t3, "desc").as("enumOfRoot");
		// select.column(t4, "desc").as("enumOfLeaf");
		join.addOrderBy(true, new RefField(Root.Field.range));
		join.getResultTransformer().setResultType(ResultContainer.class);
		List<ResultContainer> map = db.select(join, new PageLimit(0, 1));

		System.out.println("===========result==============");
		// LogUtil.show(map.get(0));
		System.out.println("asaaaaaaaaaaaaaaaaaaaaa===========================");
	}

	/**
	 * 使用SQL来完成上面的复杂查询(testSelectJoin)，效果是一样的
	 * 
	 * 
	 * PG在列的别名之前是一定要加上as的
	 * 
	 * @throws SQLException
	 */
	@IgnoreOn({ "postgresql" })
	@Test
	public void testSQL() throws SQLException {
		String sql = "select T1.THE_NAME rootName,t1.code     code,  T1.ID1      rootId,T2.ID       id, T2.NAME     name, T3.descrption    enumOfRoot,"
				+ "T4.descrption    enumOfLeaf from ROOT T1  INNER JOIN LEAF T2 ON T1.ID1 = T2.CHILDID   LEFT JOIN ENUMATIONTABLE T3 ON T1.Code = T3.code  and T3.TYPE = '1'  LEFT JOIN ENUMATIONTABLE T4 ON T2.Code = T4.code  and T4.TYPE = '2'";
		List<ResultContainer> result = db.selectBySql(sql, new Transformer(ResultContainer.class), new PageLimit(0, 10));
		System.out.println("===========result==============");
		if (result.size() > 0)
			LogUtil.show(result.get(0));
	}

	@IgnoreOn({ "postgresql" })
	@Test
	public void testLoadBySQL() throws SQLException {
		{
			String sql = "select T1.THE_NAME rootName,t1.code     code,  T1.ID1      rootId,T2.ID       id, T2.NAME     name, T3.descrption    enumOfRoot,"
					+ "T4.descrption    enumOfLeaf from ROOT T1  INNER JOIN LEAF T2 ON T1.ID1 = T2.CHILDID   LEFT JOIN ENUMATIONTABLE T3 ON T1.Code = T3.code  and T3.TYPE = '1'  LEFT JOIN ENUMATIONTABLE T4 ON T2.Code = T4.code  and T4.TYPE = '2' where id=?";
			ResultContainer result = db.loadBySql(sql, ResultContainer.class, 2);
			System.out.println("===========result==============");
			// Assert.assertNotNull(result);
			LogUtil.show(result);

			Var v = db.loadBySql(sql, Var.class, 2);
			LogUtil.show(v);
		}
	}

	@IgnoreOn(allButExcept = { "oracle" })
	@Test
	public void testLoadBySQLWithSimpleValue() throws SQLException {
		{
			String sql = "select 'AA' from dual";
			String result = db.loadBySql(sql, String.class);
			System.out.println("===========result==============");
			assertEquals("AA", result);
			LogUtil.show(result);

			Var v = db.loadBySql(sql, Var.class);
			LogUtil.show(v);
		}

		{
			String sql = "select 123 from dual";
			int result = db.loadBySql(sql, Integer.class);
			System.out.println("===========result==============");
			assertEquals(123, result);
			LogUtil.show(result);
		}

		{
			String sql = "select sysdate from dual";
			Date result = db.loadBySql(sql, Date.class);
			System.out.println("===========result==============");
			System.out.println(result.getClass());
			LogUtil.show(result);
			assertTrue(DateUtils.isSameDay(new Date(), result));
		}
	}

	@Test
	@IgnoreOn(allButExcept="sqlite")
	public void testLoadSimpleValue() throws SQLException {
		SqlTemplate sqlTemplate = db.getSqlTemplate(null);
		{
			String result = sqlTemplate.getExpressionValue("'AA'", String.class);
			System.out.println("===========result==============");
			assertEquals("AA", result);
			LogUtil.show(result);
		}

		{
			int result = sqlTemplate.getExpressionValue("123", Integer.class);
			System.out.println("===========result==============");
			assertEquals(123, result);
			LogUtil.show(result);
		}

		{
			Date result = sqlTemplate.getExpressionValue(Func.current_date, Date.class);
			System.out.println("===========result==============");
			LogUtil.show(result);
			System.out.println(new Date());
			assertTrue(DateUtils.isSameDay(new Date(), result));
		}
	}

	/**
	 * 这个案例演示NativeQuery支持重复使用。
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn("derby")
	public void testNativeQueryWithSimpleValue() throws SQLException {

		{
			/**
			 * 测试NativeQuery是否可在查询中绑定多组不同的参数
			 */
			String sql = "select :val from ROOT";
			NativeQuery<String> nq = db.createNativeQuery(sql, String.class);
			nq.setParameter("val", "1234aaa");
			String a = nq.getSingleResult();
			assertEquals("1234aaa", a);

			nq.setParameter("val", "545454aaa");
			a = nq.getSingleResult();
			assertEquals("545454aaa", a);
		}
		{
			Transaction session = db.startTransaction();
			/**
			 * 测试是否可以在写语句中绑定多组不同的参数
			 */
			String sql = "delete from ROOT where THE_NAME=:val";
			NativeQuery<String> nq = session.createNativeQuery(sql, String.class);
			nq.setParameter("val", "1234aaa");
			int a = nq.executeUpdate();
			System.out.println(a);

			nq.setParameter("val", "545454aaa");
			a = nq.executeUpdate();
			System.out.println(a);
			session.close();
		}
		System.out.println("==============================================");
		{
			Transaction session = db.startTransaction();
			try{
				/**
				 * 当绑定多组不同的表达式时，是否可以正确处理二元表达式自动省略
				 */
				String sql = "delete from ROOT where 1=1 and (THE_NAME=:val or THE_NAME =:val2)";
				NativeQuery<String> nq = session.createNativeQuery(sql, String.class);
				nq.setParameter("val", "545454bbbb");
				int a = nq.executeUpdate();
				System.out.println(a);

				nq.setParameter("val", "545454aaa");
				nq.setParameter("val2", "vvv2");
				a = nq.executeUpdate();
				System.out.println(a);

				// 清除之前设置过的参数。
				nq.clearParameters();
				a = nq.executeUpdate();
				System.out.println(a);				
			}finally{
				session.close();	
			}

		}

	}

	/**
	 * 两个单表union查询
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	@Test
	@IgnoreOn(allButExcept="derby")
	public void testSelectUnion() throws SQLException {
		Query<Leaf> q1 = QB.create(Leaf.class);
		q1.addCondition(Leaf.Field.id, 1);
		
		Query<Leaf> q2 = QB.create(Leaf.class);
		q2.addCondition(Leaf.Field.id, 3);
		q2.orderByAsc(Leaf.Field.code);
		UnionQuery<Leaf> union = QB.union(q1, q2);
		union.setOrderBy(false, Leaf.Field.name);
		// union.addOrderBy(false, new FBIField("alias_name"));
		int start = 1;
		int limit = 5;
		PagingIterator<Map> leafp = db.pageSelect(union, Map.class, limit);
		leafp.setOffset(start);
		LogUtil.show(leafp.getTotal());
		LogUtil.show(leafp.next());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSelectUnionAll() throws SQLException {
		Query<Leaf> q1 = QB.create(Leaf.class);
		q1.addCondition(Leaf.Field.id, 1);
		Query<Leaf> q2 = QB.create(Leaf.class);
		q2.addCondition(Leaf.Field.id, 3);
		UnionQuery<Leaf> union = QB.unionAll(q1, q2);
		union.setOrderBy(false, Leaf.Field.name);
		// union.addOrderBy(false, new FBIField("alias_name"));
		union.getResultTransformer().setResultType(Map.class);
		union.orderByAsc(Leaf.Field.code);
		int start = 1;
		int limit = 5;
		PagingIterator<Map> leafp = db.pageSelect(union, limit);
		leafp.setOffset(start);
		LogUtil.show(leafp.getTotal());
		LogUtil.show(leafp.next());
	}

	@Test
	public void testSQLExpression() throws SQLException {
		Query<Root> query = QB.create(Root.class);
		query.addCondition(Root.Field.range, 12);
		query.addCondition(new SqlExpression("THE_NAME='123'"));
		// query.getResultTransformer().setResultType(Map.class);
		List<Map> maps = db.selectAs(query, Map.class);
		LogUtil.show(maps);
		assertTrue(maps.isEmpty());
	}

	/**
	 * @分类 Criteria高级用法
	 * 
	 * 两个完全不同的表，一个连接查询，一个单表查询，用unionAll合并。 并且拼装到自定义的数据结构中去
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({ "gbase" })
	public void testSelectJoinUnion() throws SQLException {
		// 查询1:
		Query<Root> t1 = QB.create(Root.class);
		Query<Leaf> t2 = QB.create(Leaf.class);
		Join join = QB.innerJoin(t1, t2, QB.on(Root.Field.id, Leaf.Field.childId));

		Query<EnumationTable> t3 = QB.create(EnumationTable.class);
		Query<EnumationTable> t4 = QB.create(EnumationTable.class);

		join.leftJoin(t3, QB.on(Root.Field.code, EnumationTable.Field.code), QB.on(EnumationTable.Field.type, "1"));
		join.leftJoin(t4, QB.on(Leaf.Field.code, EnumationTable.Field.code), QB.on(EnumationTable.Field.type, "2"));
		Selects select = QB.selectFrom(join);
		select.column(new FBIField("upper(name)", t1)).as("rootName"); // column
																		// 方法中允许写select的表达式
		select.columns(t2, "id as id"); // columns中允许写多个字段和字段的别名
		select.column(t3, EnumationTable.Field.desc).as("enumOfRoot");
		select.column(t4, EnumationTable.Field.desc).as("enumOfLeaf"); // 同一张表Join两次，因此需要通过指定查询实例才能确定选择那张表上的列
		// 查询2:
		Query<Person> union2 = QB.create(Person.class);
		QB.selectFrom(union2).column(union2, "name").as("rootName");
		QB.selectFrom(union2).columns(union2, "id ,cell as enumOfRoot,phone as enumOfLeaf");
		// 结果合并
		UnionQuery<ResultContainer> union = QB.unionAll(ResultContainer.class, join, union2);
		union.addOrderBy(true, new FBIField("enumOfLeaf")); // 指定union后的排序列

		List<ResultContainer> map = db.select(union, new PageLimit(1, 9	)); // 限定结果
		LogUtil.show(map.get(0));
	}

	/**
	 * 测试命名查询
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNamedQuery() throws SQLException {
		NativeQuery<Integer> query = db.createNamedQuery("testIn", Integer.class);
		query.setParameter("names", new int[] { 1, 2, 3 });
		int i = query.getSingleResult();
		System.out.println(i);
	}


	/**
	 * 测试命名查询的自动省略功能(不输入的条件对应的二元表达式将省略)
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNamedQueryComplex() throws SQLException {
		NativeQuery<TextValuePair> query = db.createNamedQuery("testComplex", TextValuePair.class);
		query.setParameter("column", new SqlExpression("code as value, the_name as text"));
		query.setParameter("id", new int[] { 1, 2, 3 });
		query.setParameter("code", 2);
		query.setParameter("name", "sa");
		try {
			query.setParameter("orderBy", new SqlExpression("id1"));
		} catch (Exception ex) {
			LogUtil.exception(ex);
		}

		List<TextValuePair> result = query.getResultList();
		System.out.println(result);
	}

	/**
	 * 测试命名查询
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNamedQueryComplex2() throws SQLException {
		NativeQuery query = db.createNamedQuery("testComplex2");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", new int[] { 1, 2 });
		map.put("code", "1");
		map.put("column", "id1, the_name, code");
		map.put("orderBy", "id1");
		query.setParameterMap(map);

		List<Map> result = query.getResultList();
		System.out.println("ok");
	}

	@Test
	public void testNativeQuery() throws SQLException {
		String sql = "select * from root where id1=?1 and code=?2";
		NativeQuery<Root> query;
		query = db.createNativeQuery(sql, Root.class);
		query.setParameter(2, "123");
		query.setParameter(1, 12);
		query.getResultList();
	}

	@Test
	public void testNamedQueryConfigedInDb() throws SQLException {
		NativeQuery<Integer> query = db.createNamedQuery("test_sql_in", Integer.class);
		query.setParameter("names", new int[] { 1, 2, 3 });
		int i = query.getSingleResult();
		System.out.println(i);
	}

	/**
	 * 测试命名查询
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn("sqlserver")
	public void testNamedQueryPaging() throws SQLException {
		NativeQuery<Root> query = db.createNamedQuery("testPage", Root.class);
		query.setFirstResult(3);

		PagingIterator<Root> m = db.pageSelect(query, 3);
		while (m.hasNext()) {
			LogUtil.show(m.next());
		}
	}

	/**
	 * 测试带表达式的更新语句
	 * 
	 * @throws SQLException
	 */
	@Test
	public void functionalUpdate() throws SQLException {
		Leaf leaf = new Leaf();
		leaf.setId(1);
		leaf.prepareUpdate(Leaf.Field.childId, new SqlExpression("childId+:aa"));
		leaf.getQuery().setAttribute("aa", 100);
		db.update(leaf);
	}

	/**
	 * 直接修改结果集 案例一、load出多一条记录，并进行修改/删除
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({"sqlite","sqlserver"})
	public void testLoadForUpdate() throws SQLException {
		RecordHolder<Root> holder = db.loadForUpdate(new Root(1)); //
		if (holder != null) {
			holder.get().setName("更新值");
			holder.get().setRange(2);
			holder.commit();
			holder.close();
		}
		Transaction trans=db.startTransaction();
		RecordHolder<Root> holder2 = trans.loadForUpdate(new Root(1));
		if (holder2 != null) {
			LogUtil.show(holder.get());
			holder2.delete();// 删除
			holder.close(); //此处的Holder写错了。应该为holder2，关闭holder造成锁表泄漏。
		}
		trans.close();		//好在上面的ResultSet是在事务里的，此处事务关闭，顺便将之前泄漏的结果集给关闭了
		//结论——悲观锁使用非常危险，一个小小的笔误就会引起严重的泄漏事故。
		System.out.println("=================");
	}

	/**
	 * 关于Oracle使用char类型时，绑定变量如不padding空格，则无法查出的问题
	 * 
	 * @throws SQLException
	 */
	@Test
	public void lastTest() throws SQLException {
		Root root = new Root(10);
		root.setName("123");
		db.insert(root);

		Query<Root> q = QB.create(Root.class);
		q.addCondition(Root.Field.name, "123                                     ");
		db.select(q, null);

	}

	/**
	 * @ * 直接修改结果集案例二、 elect出多条记录，并进行修改/删除/插入 的测试案例
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({"sqlite","sqlserver"})
	public void testSelectForUpdate() throws SQLException {
		RecordsHolder<Root> holder = db.selectForUpdate(QB.create(Root.class).getInstance());
		int size;
		try{
			int n = 0;
			for (Root r : holder.get()) {
				n++;
				r.setName("更新第" + n + "条。"); // 修改对象中的值
			}
			size = holder.size();
			assertTrue(size > 0);
			System.out.println("count:" + n);

			Root rootOld = holder.get().get(holder.size() - 1);
			System.out.println("To DELTET " + rootOld.getId());
			holder.delete(holder.size() - 1); // 删除结果集中的最后一条记录。
			size--;

			if (holder.supportsNewRecord()) {
				Root root = holder.newRecord(); // 创建一条新纪录
				root.setName("新插入的记录");
				size++;
			}

			holder.commit(); // 提交上述修改（更新、删除、添加）	
		}finally{
			holder.close();
		}
		

		List<Root> newResult = db.select(QB.create(Root.class));
		assertEquals(size, newResult.size());
	}

	/**
	 * 一个充分定制化的查询 实际产生的SQL和testSQL是一样的，然后拼装到一个自定义的对象中去
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectMultiObject() throws SQLException {
		System.out.println("===========testSelectObjectMap==============");
		Query<Root> t1 = QB.create(Root.class);
		Query<Leaf> t2 = QB.create(Leaf.class);
		Join join = QB.innerJoin(t1, t2, QB.on(Root.Field.id, Leaf.Field.childId));
		Query<EnumationTable> t3 = QB.create(EnumationTable.class);
		Query<EnumationTable> t4 = QB.create(EnumationTable.class);
		join.leftJoin(t3, QB.on(Root.Field.code, EnumationTable.Field.code), QB.on(EnumationTable.Field.type, "1"));
		join.leftJoin(t4, QB.on(Leaf.Field.code, EnumationTable.Field.code), QB.on(EnumationTable.Field.type, "2"));

		List<Object[]> map = db.selectAs(join, Object[].class, new IntRange(1, 10));
		if (!map.isEmpty()) {
			Object[] dm = map.get(0);
		}
	}

	/**
	 * 测试使用了Oreacle专用语句的命名查询。
	 * 命名查询支持方言，因此可以在写了方言SQL的数据库上使用
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn(allButExcept="sqlserver")
	public void testStartWith() throws Exception {
		try {
//			db.dropTable("sys_resource");
			if (!db.existsTable("sys_resource")) {
				db.createNamedQuery("testOracleTree_create").executeUpdate();
				TupleMetadata tuple=MetaHolder.initMetadata(db, "sys_resource");
				VarObject map=tuple.newInstance();
				map.set("id", 1);
				map.set("parentId", 0);
				map.set("name", "Root");
				db.insert(map);
				
				map=tuple.newInstance();
				map.set("id", 2);
				map.set("parentId", 1);
				map.set("name", "A22");
				db.insert(map);
				
				map=tuple.newInstance();
				map.set("id", 4);
				map.set("parentId", 2);
				map.set("name", "A444");
				db.insert(map);
				
				map=tuple.newInstance();
				map.set("id", 5);
				map.set("parentId", 1);
				map.set("name", "A555");
				db.insert(map);
				
				map=tuple.newInstance();
				map.set("id", 6);
				map.set("parentId", 2);
				map.set("name", "A33");
				db.insert(map);
			}
			
			ORMConfig.getInstance().setAllowRemoveStartWith(true);
			NativeQuery<Map> q = db.createNamedQuery("testOracleTree", Map.class);
			q.setParameter("value",2);
			for(Map ss:q.getResultList()){
				System.out.println(ss);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 在查询中使用一个自行编写的子查询表达式。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelectExpression() throws SQLException {
		Query<Root> query = QB.create(Root.class);
		Selects select = QB.selectFrom(query);
		select.sqlExpression("(select name as childname from child c where c.id=$1.id1)").as("sss");
		LogUtil.show(db.selectAs(query, Map.class));
	}

	/**
	 * @案例说明 测试存储过程
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn(allButExcept = { "oracle", "mysql" })
	public void testProcessdure() throws SQLException {
		boolean flag = db.getMetaData(null).exists(ObjectType.PROCEDURE, "INSERT_USER");
		if (!flag) {
			throw new IllegalArgumentException("请先创建存储过程再运行此案例。" + db.getProfile().getName());
		}
		// 案例1
		{
			NativeCall call1 = db.createNativeCall("INSERT_USER", String.class, Integer.class);
			call1.setParameters("张三你", RandomData.randomInteger(1, 1000000));
			call1.execute();
		}
		System.out.println("存储过程insert_user执行后");
		// List<D> result=db.selectAll(D.class);
		// LogUtil.show(result);
		System.out.println("=========================");

		// 案例2
		{
			NativeCall call2 = db.createNativeCall("Check_user", String.class, OutParam.typeOf(Integer.class));
			call2.setParameters("张三你");
			call2.execute();
			Object obj = call2.getOutParameter(2);
			LogUtil.show(obj);
		}

		flag = flag && db.getMetaData(null).exists(ObjectType.PROCEDURE, "GET_ALL_USER");
		if (flag) { // Oracle
			// 案例3
			{
				NativeCall call3 = db.createNativeCall("GET_ALL_USER", OutParam.listOf(Map.class));
				call3.execute();
				List<Map> obj = call3.getOutParameterAsList(1, Map.class);
				call3.close();
				org.junit.Assert.assertTrue(obj.size() > 0);
				// LogUtil.show(obj);
			}

			// 案例4
			{
				String sql = "declare " + "    l_line    varchar2(255); " + "    l_done    number; " + "    l_buffer long; " + "begin " + "  loop " + "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; " + "    dbms_output.get_line( l_line, l_done ); "
						+ "    l_buffer := l_buffer || l_line || chr(10); " + "  end loop; " + " :done := l_done; " + " :buffer := l_buffer; " + "end;";
				NativeCall call3 = db.createAnonymousNativeCall(sql, Integer.class, OutParam.typeOf(Integer.class), OutParam.typeOf(String.class));
				call3.execute(26000);
				System.out.println(call3.getOutParameter(2) + "   ||   " + call3.getOutParameter(3));
			}
		} else {
			if (db.getProfile().getName() == RDBMS.oracle) {
				throw new IllegalArgumentException();
			}
		}
	}

	/**
	 * 这个案例中使用了Oracle的rowid
	 * 
	 * @throws SQLException
	 */
	@Test
	@IgnoreOn({ "gbase", "mysql", "derby", "postgresql", "hsqldb" ,"sqlserver"})
	public void testExecuteSqlBatch() throws SQLException {
		List<String> rowids = db.selectBySql("select t.rowid from leaf t", String.class);
		List<?>[] rowArray = new List<?>[rowids.size()];
		List<Object> list = null;
		int i = 0;
		for (String rowid : rowids) {
			list = new ArrayList<Object>();
			list.add(rowid);
			list.add(RandomUtils.nextInt(0,9999999));

			rowArray[i++] = list;
		}

		int count = db.count(QB.create(Leaf.class));
		db.getSqlTemplate(null).executeSqlBatch("delete from leaf where rowid = ? and id = ?", rowArray);

		// 由于删除条件id的值均匹配不上，所以executeSqlBatch执行后，表中数据量应无变化
		org.junit.Assert.assertEquals(count, db.select(QB.create(Leaf.class)).size());
	}

	public static class ResultContainer {
		private int id;
		private int rootId;
		private String name;
		private String rootName;
		private String enumOfLeaf;
		private String enumOfRoot;
		private String code;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getRootId() {
			return rootId;
		}

		public void setRootId(int rootId) {
			this.rootId = rootId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getRootName() {
			return rootName;
		}

		public void setRootName(String rootName) {
			this.rootName = rootName;
		}

		public String getEnumOfLeaf() {
			return enumOfLeaf;
		}

		public void setEnumOfLeaf(String enumOfLeaf) {
			this.enumOfLeaf = enumOfLeaf;
		}

		public String getEnumOfRoot() {
			return enumOfRoot;
		}

		public void setEnumOfRoot(String enumOfRoot) {
			this.enumOfRoot = enumOfRoot;
		}
	}

}
