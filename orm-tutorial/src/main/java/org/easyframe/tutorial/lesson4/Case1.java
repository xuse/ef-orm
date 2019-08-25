package org.easyframe.tutorial.lesson4;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.easyframe.tutorial.lesson4.entity.DataDict;
import org.easyframe.tutorial.lesson4.entity.Person;
import org.easyframe.tutorial.lesson4.entity.School;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.fastjson.JSON;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.query.Query;

public class Case1 extends org.junit.Assert{
	private static DbClient db ;
	private static int firstId;
	/**
	 * 准备
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException {
//		ORMConfig.getInstance().setCacheLevel2(500);
//		ORMConfig.getInstance().setDebugMode(true);
		db = new DbClientBuilder().setPackagesToScan(new String[] {"org.easyframe.tutorial"}).build();
		db.dropTable(Person.class, School.class,DataDict.class);
		db.createTable(Person.class, School.class,DataDict.class);

		// 准备数据
		School s1 = new School("天都中学");
		School s2 = new School("嘉海一中");
		School s3 = new School("天都大学附属中学");
		db.batchInsert(Arrays.asList(s1, s2, s3));
		ORMConfig.getInstance().setDebugMode(true);
		
		DataDict dict1=new DataDict("USER.GENDER","M","男人");
		DataDict dict2=new DataDict("USER.GENDER","F","女人");
		DataDict dict3=new DataDict("USER.GENDER","U","不确定");
		db.batchInsert(Arrays.asList(dict1,dict2,dict3));
		
		Person p=new Person();
		p.setName("孔明");
		p.setCurrentSchoolId(1);
		db.insert(p);
		firstId=p.getId();
	}


	/**
	 * 这个案例表示“级联不破单表”，即级联模型不会影响原有的单表操作。
	 * 级联描述是一种可以后续随时添加删除的扩展描述，原先的单表操作模型保持不变。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNonCascade() throws SQLException {
		db.truncate(Person.class);
		long t=System.currentTimeMillis();
		Person p = new Person();
		{
			p.setName("玄德");
			p.setCurrentSchoolId(1);
			p.setGender('F');

			// 虽然Person对象中配置了级联关系。
			// 但在EF-ORM中，级联关系是在保证了单表模型完整可用的基础上,补充上去的一种附属描述
			// 因此非级联操作一样可用。
			db.insert(p);
		}
		p.getQuery().setAttribute("dict_type", "USER.GENDER");
		//查出记录
		p = db.load(p);

		System.out.println(JSON.toJSONString(p,true));
		//单表更新
		p.setCurrentSchoolId(2);
		p.setName("云长");
		db.update(p);
		
		//单表删除
		db.delete(p);
		System.out.println(System.currentTimeMillis()-t);
	}
	
	/**
	 * 级联情况下简单的CRUD
	 * @throws SQLException
	 */
	@Test
	public void testCascade() throws SQLException{
		School school=db.load(new School("天都中学"));
		int personId;
		
		{
			Person p = new Person();		
			p.setName("翼德");
			p.setCurrentSchool(school);

			db.insertCascade(p);
			personId=p.getId();
		}
		{
			//查出记录
			Person p=db.load(new Person(personId));
			System.out.println(p.getCurrentSchoolId()+":"+p.getCurrentSchool().getName());	
			assertEquals("天都中学",p.getCurrentSchool().getName());
			
			//更新为另一学校
			p.setCurrentSchool(new School("天都外国语学校"));
			db.updateCascade(p);//外国语学校是新增的，在更新语句执行之前会先做插入School表操作。
			System.out.println("天都外国语学校 ID = "+p.getCurrentSchoolId()+" = "+p.getCurrentSchool().getId());
			
			//再使用单表更新方式，更新回原来的学校
			p.setCurrentSchoolId(school.getId());
			db.update(p);
			
			//删除该学生
			db.deleteCascade(p);
		}
	}
	
	/**
	 * 这个案例演示级联选项是可以关闭的
	 * @throws SQLException
	 */
	@Test
	public void testNonCasecadeLoad() throws SQLException{
		int personId;
		{
			Person p = new Person();
			p.setName("玄德");
			p.setCurrentSchoolId(1);
			db.insert(p);//插入一条记录
			personId=p.getId();
		}
		
		
		Person p=db.load(Person.class, personId);
		System.out.println(p.getCurrentSchool());
		assertNotNull(p.getCurrentSchool());
		//默认情况下， select()和load()方法都是级联操作，因此能查出关联的School对象
		//不过级联的开关可以自由控制——
		
		{
			Person query=new Person();
			query.setId(personId);
			//设置为非级联查询
			query.getQuery().setCascade(false);
			//等效于...
			//query.getQuery().getResultTransformer().setLoadVsMany(false);
			//query.getQuery().getResultTransformer().setLoadVsOne(false);
			
			p= db.load(query);
			System.out.println(p.getCurrentSchool());
			assertNull(p.getCurrentSchool()); 			//关闭级联开关不做级联查询了，所以School对象得不到了
			assertEquals(1, p.getCurrentSchoolId());
		}
	}
	
	/**
	 * 这个案例演示，仅引用关联对象中一个字段的场景
	 * 
	 * 用Person的性别为例
	 * @throws SQLException
	 */
	@Test
	public void testGetFieldFromManyToOne() throws SQLException{
		//FIXME ，当不使用外连接时，FileCondition无效的问题。
		ORMConfig.getInstance().setCacheDebug(true);
		
		//准备数据
		Person p1=new Person();
		p1.setName("孟德");
		p1.setGender('M'); //男性为M 女性为F
	//	p1.setDictType("USER");
		
		Person p2=new Person();
		p2.setName("貂蝉");
		p2.setGender('F');
	//	p2.setDictType("USER");
		
		db.insert(p1);
		db.insert(p2);
		
		{
			//查出数据
			Query<Person> query=QB.create(Person.class);
			query.setCascadeViaOuterJoin(true);
			query.addCondition(QB.notNull(Person.Field.gender));
			query.orderByAsc(Person.Field.gender);
			List<Person> p=db.select(query);
			
			assertEquals("女人", p.get(0).getGenderName());
			assertEquals("男人", p.get(1).getGenderName());			
		}
		{
			Query<Person> query=QB.create(Person.class);
			query.setCacheable(false);
			query.setCascadeViaOuterJoin(true);
			query.addCondition(QB.notNull(Person.Field.gender));
			query.orderByAsc(Person.Field.gender);
			List<Person> p=db.select(query);

			assertEquals("女人", p.get(0).getGenderName());
			assertEquals("男人", p.get(1).getGenderName());
		}

	}
	


	/**
	 * 这个案例演示，可以将级联关系设置为延迟加载的。
	 * 运行此案例前，请先修改Person.java的代码，设置
	 * <pre><code>
	 * @ManyToOne(targetEntity = School.class,fetch=FetchType.LAZY)  
	 * </code></pre>
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testLazyLoad() throws SQLException{
		Person query=new Person();
		query.setId(firstId);
		Person p=db.load(query);
		System.out.println("接下来观察调用get方法后，才会输出加载School的SQL语句。");
		p.getCurrentSchool();
		//请观察输出的SQL语句，
	}
	
	
	/**
	 * 这个案例演示，ManyToOne的关系的两种实现区别
	 * 1、通过外连接一次加载
	 * 2、分次加载，当我们setCascadeViaOuterJoin(false)的时候，即采用分次加载
	 * 。（默认情况下，使用单次访问数据库的外连接加载方式。）
	 * 
	 * 用Person的性别为例
	 * @throws SQLException
	 */
	@Test
	public void testNonOuterJoin() throws SQLException{
		Person query=new Person();
		query.setId(firstId);
		query.getQuery().setCascadeViaOuterJoin(false); //改变默认行为，不使用外连接。
		Person p=db.load(query);
		p.getCurrentSchool();
		p.getGenderName();
		//请观察输出的SQL语句，
	}
	

	/**
	 * 可以动态的对级联对象设置过滤条件。
	 * 通过addCascadeCondition方法。
	 * @throws SQLException
	 */
	@Test
	public void testCascadeCondition() throws SQLException{
		Query<Person> query=QB.create(Person.class);
		query.setCascadeViaOuterJoin(true);
		query.addCondition(QB.eq(Person.Field.id,firstId));
		query.addCascadeCondition(QB.matchAny(School.Field.name, "清华"));
		db.select(query); 
	}
}
