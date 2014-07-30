package org.easyframe.tutorial.lesson5;

import java.sql.SQLException;
import java.util.Arrays;

import jef.codegen.EntityEnhancer;
import jef.database.DbClient;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.Transaction;
import jef.database.query.Query;

import org.easyframe.tutorial.lesson5.entity.Catalogy;
import org.easyframe.tutorial.lesson5.entity.Item;
import org.easyframe.tutorial.lesson5.entity.ItemExtendInfo;
import org.easyframe.tutorial.lesson5.entity.Student;
import org.easyframe.tutorial.lesson5.entity.TeacherLesson;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 继续演示级联操作
 * 
 * @author jiyi
 * 
 */
public class Case1 extends org.junit.Assert {
	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial.lesson5");
		ORMConfig.getInstance().setDebugMode(false);
		db = new DbClient();
		db.dropTable(Catalogy.class, Item.class, Student.class, TeacherLesson.class, ItemExtendInfo.class);
		db.createTable(Catalogy.class, Item.class, Student.class, TeacherLesson.class, ItemExtendInfo.class);
		Catalogy c = new Catalogy();
		c.setName("类别1");
		// c.setParentId(1);
		c.setItems(Arrays.asList(new Item("条目1").setExtendInfo("1000", "1:30:00", "China"), new Item("条目2").setExtendInfo("2000", "0:30:00", "Japan"), new Item("条目3").setExtendInfo("3000", "1:00:00", "USA"), new Item("条目4").setExtendInfo("4000", "0:20:00", "Morocco")));
		db.insertCascade(c);

		TeacherLesson t1 = new TeacherLesson(1, 1, "语文");
		TeacherLesson t2 = new TeacherLesson(2, 1, "数学");
		TeacherLesson t3 = new TeacherLesson(3, 1, "英语");
		TeacherLesson t4 = new TeacherLesson(4, 1, "化学");
		db.batchInsert(Arrays.asList(t1, t2, t3, t4));

		t1 = new TeacherLesson(1, 2, "语文");
		t2 = new TeacherLesson(2, 2, "数学");
		t3 = new TeacherLesson(3, 2, "英语");
		t4 = new TeacherLesson(4, 2, "化学");
		db.batchInsert(Arrays.asList(t1, t2, t3, t4));

		t1 = new TeacherLesson(1, 3, "语文");
		t2 = new TeacherLesson(2, 3, "数学");
		t3 = new TeacherLesson(3, 3, "英语");
		t4 = new TeacherLesson(4, 3, "化学");
		db.batchInsert(Arrays.asList(t1, t2, t3, t4));

		Student s1 = new Student(1, 1, "张一");
		Student s2 = new Student(2, 1, "李二");
		Student s3 = new Student(3, 1, "王三");
		Student s4 = new Student(4, 2, "赵四");
		Student s5 = new Student(5, 2, "钱五");
		Student s6 = new Student(6, 2, "孙六");
		Student s7 = new Student(7, 3, "周七");
		Student s8 = new Student(8, 3, "吴八");
		Student s9 = new Student(9, 3, "郑九");
		db.insertCascade(s1);
		db.batchInsert(Arrays.asList(s2, s3, s4, s5, s6, s7, s8, s9));
		ORMConfig.getInstance().setDebugMode(true);
	}

	/**
	 * 演示:一对多情况下，将关联字段设置为null以后再去update，会删除关联的记录.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCascade() throws SQLException {
		Transaction tx = db.startTransaction();
		Catalogy c = new Catalogy(); // Catalogy表是父表
		c.setId(1);
		c = tx.load(c);

		assertNotNull(c);
		assertEquals(4, c.getItems().size()); // Item表是子表

		c.setItems(null);
		System.out.println("设置为null，调用级联update，@OneToMany下会删除子表记录");

		tx.updateCascade(c);
		tx.commit();
	}
	
	/**
	 * 演示：多对多情况下，将关联字段设置为null以后再去update，不删除关联的记录.
	 * ManyToMany用于较弱的关联，使用更为保守的更新策略，因此不会删除关联记录。
	 * @throws SQLException
	 */
	@Test
	public void testManyToMany() throws SQLException {
		Transaction tx = db.startTransaction();
		Student s1 = new Student();
		s1.setId(1);
		s1 = tx.load(s1);

		s1.getLessons().clear();
		System.out.println("设置为null，调用级联update，@ManyToMany下不会删除子表记录");
		// ManyToMany用来描述弱关联。因此级联操作会插入/更新传入的数据，但不会删除级联数据。
		tx.updateCascade(s1); 
		tx.rollback();
	}
	
	
	/**
	 * 级联过滤条件的效果
	 * @throws SQLException
	 */
	@Test
	public void testCascadeCondition() throws SQLException {
		{//无级联条件时
			Student s1 = db.load(Student.class,1);
			for(TeacherLesson t:s1.getLessons()){
				System.out.println(t);
			}	
		}
		{
			Student st=new Student();
			st.setId(1);
			st.getQuery().addCascadeCondition(QB.in(TeacherLesson.Field.lessonName, new String[]{"语文","化学"}));
			for(TeacherLesson t:db.load(st).getLessons()){
				System.out.println(t);
			}	
		}
		
	}

	/**
	 * 级联过滤条件也可以用于间接的引用中，
	 * 比如本例中， Cacalogy引用Item、Item引用ItemExtendIndo，通过指定引用字段，可以精控制过滤条件要用于那个对象上。
	 */
	@Test
	public void testFilterCondition2() throws SQLException {
		Query<Catalogy> q = QB.create(Catalogy.class);
		q.addCondition(QB.eq(Catalogy.Field.id, 1));
		q.addCascadeCondition("items.itemExtInfos", QB.eq(ItemExtendInfo.Field.key, "拍摄地点"));// 作为Filter能生效
		Catalogy c = db.load(q);
		for (Item item : c.getItems()) {
			System.out.print(item.getItemExtInfos());
		}
	}

	/**
	 * 如果不是级联过滤条件的情况下，传入一个别的对象的字段，会在校验时抛出异常。
	 * @throws SQLException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testFilterCondition3() throws SQLException {
		Query<Catalogy> q = QB.create(Catalogy.class);
		q.addCondition(QB.eq(Item.Field.id, 1));
		Catalogy c = db.load(q);
		System.out.println(c);
	}




}
