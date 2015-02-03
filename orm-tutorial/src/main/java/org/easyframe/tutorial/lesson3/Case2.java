package org.easyframe.tutorial.lesson3;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.query.Func;
import jef.database.query.Join;
import jef.database.query.Query;
import jef.database.query.QueryBuilder;
import jef.database.query.Selects;
import jef.http.client.support.CommentEntry;
import jef.tools.string.RandomData;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson2.entity.StudentToLession;
import org.junit.Test;

/**
 * 之前看了查询条件的用法。现在看看更多灵活的查询用法。
 * @author geequery
 *
 */
public class Case2 extends org.junit.Assert {
	DbClient db;

	public Case2() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial");
		db = new DbClient();
		// 准备数据时关闭调试，减少控制台信息
		ORMConfig.getInstance().setDebugMode(true);
		db.dropTable(Student.class, StudentToLession.class);
		db.createTable(Student.class, StudentToLession.class);
		prepareData(15);
		ORMConfig.getInstance().setDebugMode(true);
	}

	/**
	 * 使用selectFrom方法，来指定要查询的字段 。未指定要查询的字段不会被查出。
	 * @throws SQLException
	 */
	@Test
	public void testSelect_selectFrom() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);
		query.addCondition(QueryBuilder.eq(Student.Field.gender, "F"));

		//指定：仅查询id,name两个字段
		Selects selects = QueryBuilder.selectFrom(query);
		selects.column(Student.Field.id);
		selects.column(Student.Field.name);

		List<Student> students = db.select(query);

		// 验证数据
		if (students.size() > 0) {
			Student st = students.get(0);
			assertNotNull(st.getName());
			assertNull(st.getGender());
		}
	}

	/**
	 * 查出所有学生姓名，使用distinct语句过滤掉重复的。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_selectDistinct() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);
		Selects selects = QueryBuilder.selectFrom(query);
		selects.setDistinct(true);
		selects.column(Student.Field.name);

		// 相当于 select distinct t.NAME from STUDENT t
		List<Student> students = db.select(query);
	}

	/**
	 * group by的查询该怎么用？
	 * 下面的例子演示了将学生按照男生、女生分组进行统计。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_selectGroup() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);
		Selects selects = QueryBuilder.selectFrom(query);

		selects.column(Student.Field.gender).group(); // 按性别分组
		selects.column(Student.Field.id).count(); // 统计人数
		selects.column(Student.Field.id).max(); // 最大的学号
		selects.column(Student.Field.id).min(); // 最小的学号

		// 上述查询的结果。无法再转换为Student对象返回了，这里将各个列按顺序形成数组返回。
		List<String[]> stat = db.selectAs(query, String[].class);
		for (Object[] result : stat) {
			System.out.print("M".equals(result[0]) ? "男生" : "女生");
			System.out.print(" 总数:" + result[1] + " 最大学号:" + result[2] + " 最小学号" + result[3]);
			System.out.println();
		}
	}

	/**
	 * 带有Having子句的查询该怎么使用？
	 * 我们对学生按年级进行分组统计，同时使用having子句过滤掉人数大于2的年级。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_groupHaving() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);
		Selects selects = QueryBuilder.selectFrom(query);

		selects.column(Student.Field.grade).group(); // 按年级分组
		selects.column(Student.Field.grade).count().having(Operator.GREAT, 2); // 查出人数，同时添加一个Having条件，count(id)>2

		List<String[]> stat = db.selectAs(query, String[].class);
		for (Object[] result : stat) {
			System.out.print("年级:" + result[0] + " 人数:" + result[1]);
			System.out.println();
		}
	}

	/**
	 * 查询学生数量的语句中，使用distinct……
	 * @throws SQLException
	 */
	@Test
	public void testSelect_countDistinct() throws SQLException {
		Query<Student> q = QB.create(Student.class);

		Selects items = QB.selectFrom(q);
		items.column(Student.Field.name);
		items.setDistinct(true);
		//q.setMaxResult(1);

		long total = db.count(q);// 取总数
		List<String> result=db.selectAs(q,String.class);
		System.out.println("总数为:"+ total +" 查出"+ result.size()+"条");
	}
	
	/**
	 * 查询学生数量的语句中，使用distinct——的另一种写法
	 */
	@Test
	public void testSelect_countDistinct2() throws SQLException {
		Query<Student> q = QB.create(Student.class);

		Selects items = QB.selectFrom(q);
		items.column(Student.Field.name).countDistinct();

		Integer total=db.loadAs(q,Integer.class);
		System.out.println("Count:"+  total);
	}

	/**
	 * 分组查询 group by以后，常用的统计函数——
	 * @throws SQLException
	 */
	@Test
	public void testSelect_function1() throws SQLException {
		Query<Student> q = QB.create(Student.class);

		Selects items = QB.selectFrom(q);
		items.column(Student.Field.id).min().as("min_id");
		items.column(Student.Field.id).max().as("max_id");
		items.column(Student.Field.id).sum().as("sub_id");
		items.column(Student.Field.id).avg().as("avg_id");
		
		for(Map<String,Object> result:db.selectAs(q,Map.class)){
			System.out.println(result);
		}
	}
	
	/**
	 * 在查询中，除了常用统计函数，还可以使用标准的系统函数，这些系统函数几乎囊括了大部分数据库上的常用函数。
	 * 例如下面的 decode函数和upper函数。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_function2() throws SQLException {
		Query<Student> q = QB.create(Student.class);

		Selects items = QB.selectFrom(q);
		//对姓名统一转大写
		items.column(Student.Field.name).func(Func.upper);
		//性别进行函数转换，decode是Oracle下的函数，注意观察其在Derby下的处理。有兴趣的可以换成MySQL试一下。
		items.column(Student.Field.gender).func(Func.decode, "?", "'M'" ,"'男'","'F'" ,"'女'");
		//先对日期转文本，然后截取前面的部分
		items.column(Student.Field.dateOfBirth).func(Func.str).func(Func.substring,"?","1","10");
		
		for(String[] result:db.selectAs(q,String[].class)){
			System.out.println(Arrays.toString(result));
		}
	}
	
	/**
	 * 当然如果不愿意用API编写select 部分的，直接写SQL 片段也是允许的。
	 * 
	 * 下面的例子直接用SQL片段来描述查询的内容。同时对片段也是支持各种数据库兼容的。（用到了SQL改写技术）
	 * @throws SQLException
	 */
	@Test
	public void testSelect_sqlExpression() throws SQLException {
		Query<Student> q = QB.create(Student.class);
		
		Selects select = QB.selectFrom(q);
		select.columns("name,decode(gender,'F','女','M','男') as gender");;
		for(Student result:db.select(q)){
			System.out.println(result.getName()+" "+result.getGender());
		}
	}
	
	/**
	 * 同样的日期时间也有相关的标准函数支持。
	 * @throws SQLException
	 */
	@Test
	public void testSelect_sqlExpression2() throws SQLException {
		Query<Student> q = QB.create(Student.class);
		
		Selects select = QB.selectFrom(q);
		select.column(Student.Field.name);
		select.sqlExpression("str(add_months(Date_Of_Birth,24))").as("BIRTH_ADD_24");
		for(String[] result:db.selectAs(q,String[].class)){
			System.out.println(Arrays.toString(result));
		}
	}
	
	/**
	 * 在自定义Join以后，也可以进行count distinct操作。
	 * @throws SQLException
	 */
	@Test
	public void testSelect_JoinCountDistinct() throws SQLException {
		Query<Student> q = QB.create(Student.class);
		Query<StudentToLession> q2 = QB.create(StudentToLession.class);

		Join join = QueryBuilder.innerJoin(q, q2, QB.on(Student.Field.id, StudentToLession.Field.studentId));

		Selects items = QB.selectFrom(join);
		items.noColums(q2);
		items.column(Student.Field.name);
		items.setDistinct(true);
		join.setMaxResult(1);

		long total = db.count(join);// 取总数
		System.out.println("总数" + total);

		LogUtil.show(db.selectAs(join, String.class)); // 查询，由于总数被限制为1，因此只会显示第一条。
	}

	/**
	 * 自定义列的别名，然后用自定义对象容器作为返回结果 
	 * @throws SQLException
	 */
	@Test
	public void testSelect_selectCustome() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);
		Selects selects = QueryBuilder.selectFrom(query);
		ORMConfig.getInstance().setFormatSQL(false);
		selects.column(Student.Field.id).as("key");
		selects.column(Student.Field.name).as("value");
		List<CommentEntry> stat = db.selectAs(query, jef.http.client.support.CommentEntry.class);
		System.out.println(stat);
	}

	/**
	 * 自定义Join，每条记录映射为多个对象
	 * @throws SQLException
	 */
	@Test
	public void testSelect_selectMulti() throws SQLException {
		Query<Student> query = QueryBuilder.create(Student.class);
		Join join = QB.innerJoin(query, QB.create(StudentToLession.class), QB.on(Student.Field.id, StudentToLession.Field.studentId));

		List<Object[]> results = db.selectAs(join, Object[].class);

		for (Object[] result : results) {
			Student st = (Student) result[0];
			StudentToLession sl = (StudentToLession) result[1];
			System.out.println(st + " - " + sl);
		}
	}

	private void prepareData(int num) throws SQLException {
		List<Student> data = new ArrayList<Student>();
		Date old = new Date(System.currentTimeMillis() - 864000000000L);
		for (int i = 0; i < num; i++) {
			// 用随机数生成一些学生信息
			Student st = new Student();
			st.setGender(i % 2 == 0 ? "M" : "F");
			st.setName(RandomData.randomChineseName());
			st.setDateOfBirth(RandomData.randomDate(old, new Date()));
			st.setGrade(String.valueOf(RandomData.randomInteger(1, 6)));
			data.add(st);
		}
		db.batchInsert(data);

		List<StudentToLession> data2 = new ArrayList<StudentToLession>();
		for (int i = 0; i < num; i++) {
			StudentToLession sl = new StudentToLession();
			sl.setStudentId(data.get(i).getId());
			sl.setLessionId(100);
			data2.add(sl);
		}
		db.batchInsert(data2);

	}
}
