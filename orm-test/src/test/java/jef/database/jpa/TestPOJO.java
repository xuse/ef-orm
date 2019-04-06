package jef.database.jpa;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import jef.common.wrapper.Page;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.meta.MetaHolder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPOJO {
	DbClient db;
	CommonDao dao;
	
	public TestPOJO() throws SQLException{
		db=new DbClientBuilder().build();
		dao=new CommonDaoImpl(db);
		dao.getNoTransactionSession().createTable(PojoEntity.class);
		dao.getNoTransactionSession().createTable(PojoFoo.class);
		
	}
	
	/**
	 * 实体类
	 * @author jiyi
	 *
	 */
	public static class PojoFoo{
		@Id
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
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
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
		dao.updateByPK(pojo);
		
		System.out.println("-=-==========================");
		
		
		PojoEntity cond=new PojoEntity();
		cond.setId(12);
		System.out.println(dao.find(cond));
		dao.remove(cond);
		
	}
	
	@Test
	public void test2() throws SQLException{
		dao.getNoTransactionSession().truncate(MetaHolder.getMeta(PojoFoo.class));
		List<PojoFoo> ps=new ArrayList<PojoFoo>();
		for(int i=0;i<50;i++){
			PojoFoo foo=new PojoFoo();
			foo.setId(i);
			foo.setName("四十九"+i);
			ps.add(foo);
		}
		dao.batchInsert(ps);
		
		PojoFoo foo=new PojoFoo();
		foo.setId(1);
		foo=dao.load(foo);
		Assert.assertNotNull(foo);
		
		
		List<PojoFoo> foos=dao.findByExample(foo);
		Assert.assertEquals(1, foos.size());
		
		Page<PojoFoo> result=dao.findAndPage(new PojoFoo(), 3, 8);
		System.out.println(result);
		Assert.assertEquals(8, result.getList().size());
		
		for(PojoFoo bean: result.getList()){
			bean.setName("修改"+bean.getName());
		}
		dao.batchUpdate(result.getList());
		
		List<PojoFoo> pojo=dao.findByField(PojoFoo.class, "name", "修改四十九6");
		System.out.println(pojo);
		
		dao.batchDelete(result.getList());
	}
	
	@Test
	public void test3() throws SQLException{
		DbClient session=dao.getNoTransactionSession();
		session.truncate(MetaHolder.getMeta(PojoFoo.class));
		
		List<PojoFoo> ps=new ArrayList<PojoFoo>();
		for(int i=0;i<50;i++){
			PojoFoo foo=new PojoFoo();
			foo.setId(i);
			foo.setName("四十九"+i);
			ps.add(foo);
		}
		session.batchInsert(ps);
		
		PojoFoo foo=new PojoFoo();
		foo.setId(1);
		foo=session.load(foo);
		Assert.assertNotNull(foo);
		
		List<PojoFoo> foos=session.selectByExample(foo);
		Assert.assertEquals(1, foos.size());
		
		Page<PojoFoo> result=session.selectPage(new PojoFoo(), 3, 8);
		System.out.println(result);
		Assert.assertEquals(8, result.getList().size());
		
		for(PojoFoo bean: result.getList()){
			bean.setName("修改"+bean.getName());
		}
		session.batchUpdate(result.getList());
		
		List<PojoFoo> pojo=session.selectByField(PojoFoo.class, "name", "修改四十九6");
		System.out.println(pojo);
		
		session.batchDelete(result.getList());
	}
}
