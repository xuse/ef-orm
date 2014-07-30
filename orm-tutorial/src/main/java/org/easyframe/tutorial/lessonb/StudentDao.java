package org.easyframe.tutorial.lessonb;

import java.util.Collection;

import org.easyframe.enterprise.spring.GenericDao;
import org.easyframe.tutorial.lesson2.entity.Student;

public interface StudentDao extends GenericDao<Student>{
	/**
	 * 批量升级学生
	 * @param ids
	 */
	public void gradeUp(Collection<Integer> ids);
}
