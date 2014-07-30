package org.easyframe.tutorial.lessonb;

import java.sql.SQLException;
import java.util.Collection;

import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.query.JpqlExpression;

import org.easyframe.enterprise.spring.GenericDaoSupport;
import org.easyframe.tutorial.lesson2.entity.Student;

/**
 * 这个类实现了GenericDao<T>接口
 */
public class StudentDaoImpl extends GenericDaoSupport<Student> implements StudentDao{

	public void gradeUp(Collection<Integer> ids) {
		Student st=new Student();
		st.getQuery().addCondition(Student.Field.id, Operator.IN, ids);
		st.prepareUpdate(Student.Field.grade, new JpqlExpression("grade+1"));
		try {
			getSession().update(st);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
}
