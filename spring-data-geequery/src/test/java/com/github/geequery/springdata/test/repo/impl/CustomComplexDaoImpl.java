package com.github.geequery.springdata.test.repo.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.github.geequery.springdata.test.entity.ComplexFoo;
import com.github.geequery.springdata.test.entity.Foo;
import com.github.geequery.springdata.test.repo.ICustomComplexDao;

@Repository
public class CustomComplexDaoImpl implements ICustomComplexDao{
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
