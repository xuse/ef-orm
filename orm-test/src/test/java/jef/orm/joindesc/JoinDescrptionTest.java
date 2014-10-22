package jef.orm.joindesc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.Session;
import jef.database.test.DataSource;
import jef.database.test.DataSourceContext;
import jef.database.test.DatabaseInit;
import jef.database.test.JefJUnit4DatabaseTestRunner;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JefJUnit4DatabaseTestRunner.class)
@DataSourceContext({
	@DataSource(name = "hsqldb", url = "jdbc:hsqldb:mem:testhsqldb", user = "sa", password = ""),
	@DataSource(name = "sqlserver", url = "${sqlserver.url}",user="${sqlserver.user}",password="${sqlserver.password}")
})
public class JoinDescrptionTest {

	@BeforeClass
	public static void test() {
		EntityEnhancer en = new EntityEnhancer();
		en.enhance();
	}

	@DatabaseInit
	public void init() throws SQLException {
		db.refreshTable(Student.class);
		db.refreshTable(Lesson.class);
		db.refreshTable(UserToLession.class);
	}

	private DbClient db;

	public void testPrepareData() throws SQLException {
		db.truncate(Student.class);
		db.truncate(UserToLession.class);
		{
			Student user=new Student("张三");
			db.insert(user);
		}
		{
			Student user=new Student("李思");
			db.insert(user);
		}
		{
			Student user=new Student("王五");
			db.insert(user);
		}
		
		List<Lesson> lessions= new ArrayList<Lesson>();
		lessions.add(new Lesson("语文",10));
		lessions.add(new Lesson("数学",10));
		lessions.add(new Lesson("英语",9));
		lessions.add(new Lesson("物理",8));
		lessions.add(new Lesson("化学",8));
		lessions.add(new Lesson("生物",6));
		lessions.add(new Lesson("体育",4));
		lessions.add(new Lesson("美术",3));
		lessions.add(new Lesson("音乐",2));
		db.batchInsert(lessions);
		{
			db.batchInsert(Arrays.asList(new UserToLession(1,1,40),
			new UserToLession(1,2,56),
			new UserToLession(1,3,90),
			new UserToLession(1,4,70),
			new UserToLession(1,5,76),
			new UserToLession(1,6,55),
			new UserToLession(1,7,99),
			new UserToLession(1,8,92)));
		}
	}

	@Test
	public void testSelect() throws SQLException {
		Session db=this.db.startTransaction();
		testPrepareData();
		Student user = db.load(new Student(1));
		// 延迟加载
		List<UserToLession> userTests = user.getToLession();
		// 打印出
		UserToLession maxScore=user.getMaxScoreLession();
		maxScore.getLession();
		maxScore.getUser();
		System.out.println(userTests.size());
		System.out.println(maxScore);
		db.close();
		//事务被关闭了，但是还是坚持从数据库连接获取
		System.out.println(maxScore.getLession().getTests());
		
		
		

	}
}
