package org.easyframe.tutorial.lessonb;

import java.sql.SQLException;
import java.util.List;

import org.easyframe.enterprise.spring.BaseDao;
import org.easyframe.tutorial.lesson2.entity.Student;

import jef.database.DbUtils;

public class MyDao extends BaseDao {

	/**
	 * 使用标准JPA的方法来实现DAO
	 */
	public Student loadStudent(int id) {
		try {
			return getSession().load(Student.class, id);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	/**
	 * 使用EF-ORM的方法来实现DAO
	 * 
	 * @param name
	 * @return
	 */
	public List<Student> findStudentByName(String name) {
		Student st = new Student();
		st.getQuery().addCondition(Student.Field.name.matchAny(name));
		try {
			return getSession().select(st);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
}
