package jef.database.jpa;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.wrapper.Page;
import jef.database.DbClient;
import jef.tools.reflect.BeanUtils;

import org.easyframe.enterprise.spring.BaseDao;
import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.junit.Test;

public class TestPOJO {
	DbClient db;
	CommonDao dao;
	
	public TestPOJO() throws SQLException{
		db=new DbClient();
		dao=new CommonDaoImpl();
		((BaseDao)dao).setEntityManagerFactory(new JefEntityManagerFactory(db));
		dao.getNoTransactionSession().createTable(PojoEntity.class);
		dao.getNoTransactionSession().createTable(PojoFoo.class);
		
	}
	
	
	public static class PojoFoo{
		private int id;
		private String name;
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	
	@Test
	public void testpojo() throws SQLException{
		PojoEntity p=new PojoEntity();
		p.setName("fsdfsfs");
		
		dao.insert(p);
		System.out.println(p.getId());
		dao.insert(p);
		System.out.println(p.getId());
		dao.insert(p);
		System.out.println(p.getId());
		
		
		PojoEntity pojo=dao.load(p);
		System.out.println(pojo);
		
		pojo.setName("35677");
		dao.update(pojo);
		
		System.out.println("-=-==========================");
		
		
		PojoEntity cond=new PojoEntity();
		cond.setId(12);
		System.out.println(dao.find(cond));
		dao.remove(cond);
		
	}
	
	@Test
	public void test2(){
		List<PojoFoo> ps=new ArrayList<PojoFoo>();
		for(int i=0;i<50;i++){
			PojoFoo foo=new PojoFoo();
			foo.setId(i);
			foo.setName("四十九"+i);
			ps.add(foo);
		}
		dao.batchInsert(ps);
		
		PojoFoo foo=new PojoFoo();
		dao.load(foo);
		
		dao.findByExample(foo);
		
		Page<PojoFoo> result=dao.findAndPage(foo, 3, 8);
		System.out.println(result);
	}
	
}
