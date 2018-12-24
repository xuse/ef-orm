package com.github.geequery.spring.case2;

import java.sql.SQLException;
import java.util.List;

import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.easyframe.enterprise.spring.GenericDaoSupport;
import com.github.geequery.spring.entity.Tt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.geequery.test.spring.JefTransactionTest.DbCall;

import jef.database.DbUtils;

@Transactional(propagation=Propagation.REQUIRES_NEW)
@Repository("TableManager")
@SuppressWarnings("unused")
public class ServiceRequiresNew extends GenericDaoSupport<Tt>{
	@Autowired
	private ServiceMandatory serviceMandatory;
	@Autowired
	private ServiceNested serviceNested;
	@Autowired
	private ServiceNever serviceNever;
	@Autowired
	private ServiceNotSupported serviceNotSupported;
	@Autowired
	private ServiceRequired serviceRequired;
	private ServiceRequiresNew serviceRequiresNew=this;
	@Autowired
	private ServiceSupports serviceSupports;
	@Autowired
	private CommonDaoImpl commondao;
	
	public void xmerge(Tt t) {
		try {
			super.getSession().merge(t);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	//测试嵌套事务
	public Tt executeMethod1(List<Propagation> tasks,List<DbCall> calls) {
		if(!calls.isEmpty()){
			calls.remove(0).call(super.getSession());
		}
		if(!tasks.isEmpty()){
			switch(tasks.remove(0)){
			case MANDATORY:
				serviceMandatory.executeMethod1(tasks,calls);
				break;
			case NESTED:
				serviceNested.executeMethod1(tasks,calls);
				break;
			case NEVER:
				serviceNever.executeMethod1(tasks,calls);
				break;
			case NOT_SUPPORTED:
				serviceNotSupported.executeMethod1(tasks,calls);
				break;
			case REQUIRED:
				serviceRequired.executeMethod1(tasks,calls);
				break;
			case REQUIRES_NEW:
				serviceRequiresNew.executeMethod1(tasks,calls);
				break;
			case SUPPORTS:
				serviceSupports.executeMethod1(tasks,calls);
				break;
			default:
				throw new RuntimeException();
					
			}
		}
		return null;
	}
}
