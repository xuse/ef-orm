package org.easyframe.tutorial.lesson5_a;

import java.sql.SQLException;
import java.util.Arrays;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.QB;
import jef.database.meta.MetaHolder;
import jef.database.meta.TupleMetadata;

import org.easyframe.tutorial.lesson5_a.entity.Role;
import org.easyframe.tutorial.lesson5_a.entity.User;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManyToManyTest extends org.junit.Assert {
	private static DbClient db;

	@BeforeClass
	public static void setup() throws SQLException {
		db = new DbClientBuilder().setEnhancePackages("org.easyframe.tutorial.lesson5_a").build();
		db.dropTable(User.class,Role.class);
		db.createTable(User.class, Role.class);
	}

	@Test
	public void empty() throws SQLException {
	}
	
	@Test
	public void test1() throws SQLException {
		User user = new User();
		user.setName("admin");

		Role role = new Role();
		role.setName("一般用户");

		user.setRoles(Arrays.asList(role));
		db.insertCascade(user);
		int userId=user.getId();
		int roleId=role.getId();
		System.out.println("=========================================111");
		User u = db.load(User.class, userId);
		System.out.println("=========================================222");
		Role r = db.load(Role.class, roleId);
		{
			//检查
			assertNotNull(u);
			assertNotNull(r);
			assertEquals(1,u.getRoles().size());
			System.out.println("====1");
			System.out.println(u.getRoleNames());
			System.out.println("====xxx");
			System.out.println(u.getRoles().get(0).getUsers());
		}
		
		Role role2=new Role();	
		role2.setName("超级用户");
		u.getRoles().add(role2);
		db.updateCascade(u);
		
		{
			//检查2
			assertEquals(2, db.count(QB.create(Role.class)));
			TupleMetadata meta=MetaHolder.getDynamicMeta("user_roles");
			assertEquals(2, db.count(QB.create(meta)));
			
			u = db.load(User.class, userId);
			assertEquals(2, u.getRoles().size());
			System.out.println("====2");
			System.out.println(u.getRoleNames());
		}
		
		u=new User();
		u.setId(userId);
		db.deleteCascade(u);
		{
			assertEquals(2, db.count(QB.create(Role.class)));
			assertEquals(0, db.count(QB.create(User.class)));
			
			TupleMetadata meta=MetaHolder.getDynamicMeta("user_roles");
			assertEquals(0, db.count(QB.create(meta)));
		}
	}
}
