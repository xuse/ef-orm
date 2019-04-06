package com.github.geequery.springdata.test.repo;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.geequery.springdata.test.entity.ComplexFoo;

import jef.database.SessionFactory;

public class CustomComplexDaoImpl implements CustomComplexDao{
//	@PersistenceContext
	@Autowired
	private SessionFactory em;
	
	@Override
	public void someCustomMethod(ComplexFoo user) {
		System.out.println(user);
	}

	public ComplexFoo someOtherMethod() {
//		em.merge(user);
		ComplexFoo cf=new ComplexFoo();
//		cf.setUserId(user.getId());
		cf.setClassId(100);
		return cf;
	}

}
