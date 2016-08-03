package com.github.geequery.springdata.test.repo;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.github.geequery.springdata.test.entity.ComplexFoo;

public class CustomComplexDaoImpl implements CustomComplexDao{
	@PersistenceContext
	private EntityManager em;
	
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
