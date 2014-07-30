package org.easyframe.tutorial.lessonb;

import java.sql.SQLException;
import java.util.List;

import jef.database.DbUtils;
import jef.database.QB;

import org.easyframe.enterprise.spring.BaseDao;
import org.easyframe.tutorial.lesson2.entity.Student;

public class MyDao extends BaseDao{
	
	/**
	 * 使用标准JPA的方法来实现DAO
	 */
	public Student loadStudent(int id){
		return getEntityManager().find(Student.class, id);
	}
	
	/**
	 * 使用EF-ORM的方法来实现DAO
	 * @param name
	 * @return
	 */
	public List<Student> findStudentByName(String name){
		Student st=new Student();
		st.getQuery().addCondition(QB.matchAny(Student.Field.name, name));
		try {
			return getSession().select(st);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
}
