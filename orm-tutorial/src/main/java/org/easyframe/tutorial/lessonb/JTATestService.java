package org.easyframe.tutorial.lessonb;

import java.sql.SQLException;

import jef.database.SqlTemplate;
import jef.tools.DateUtils;

import org.easyframe.enterprise.spring.CommonDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JTATestService {
	@Autowired
	private CommonDao dao;
	
	/**
	 * 成功的事务
	 * @throws SQLException
	 */
	

	public void success1(){
		SqlTemplate ds1=dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2=dao.getSession().getSqlTemplate("ds2");
		try{
			Integer val=ds1.loadBySql("select max(id) from foo2", Integer.class);
			ds1.executeSql("insert into foo2 (id,name,created) values(?,?,?)", val==null?1:val+1,"卡卡西",DateUtils.sqlToday());
			ds2.executeSql("insert into Student(id,name) values(default,?)", "陆逊");	
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}
	
	public void failure1(){
		SqlTemplate ds1=dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2=dao.getSession().getSqlTemplate("ds2");
		try{
			Integer val=ds1.loadBySql("select max(id) from foo2", Integer.class);
			ds1.executeSql("insert into foo2 (id,name,created) values(?,?,?)", val==null?1:val,"卡卡西",DateUtils.sqlToday());
			
			ds2.executeSql("insert into Student(id,name) values(default,?)", "陆逊");	
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}
	
	public void failure2(){
		SqlTemplate ds1=dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2=dao.getSession().getSqlTemplate("ds2");
		try{
			Integer val=ds1.loadBySql("select max(id) from foo2", Integer.class);
			ds1.executeSql("insert into foo2 (id,name,created) values(?,?,?)", val==null?1:val+1,"卡卡西",DateUtils.sqlToday());
			
			ds2.executeSql("insert into Student(id,name) values(error,?)", "陆逊");	
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}
	
	public void failure3(){
		SqlTemplate ds1=dao.getSession().getSqlTemplate("ds1");
		SqlTemplate ds2=dao.getSession().getSqlTemplate("ds2");
		try{
			Integer val=ds1.loadBySql("select max(id) from foo2", Integer.class);
			ds1.executeSql("insert into foo2 (id,name,created) values(?,?,?)", val==null?1:val+1,"卡卡西",DateUtils.sqlToday());
			ds2.executeSql("insert into Student(id,name) values(default,?)", "陆逊");	
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
		throw new NullPointerException();
	}
}
