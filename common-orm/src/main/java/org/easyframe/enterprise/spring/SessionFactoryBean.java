package org.easyframe.enterprise.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import jef.database.DbClientBuilder;
import jef.database.DbUtils;
import jef.database.SessionFactory;

/**
 * 供Spring上下文中初始化EF-ORM Session Factory使用
 * 
 * @author jiyi
 * 
 */
public class SessionFactoryBean extends DbClientBuilder implements FactoryBean<SessionFactory>, InitializingBean {
	public void afterPropertiesSet(){
		instance = buildSessionFactory();
	}

	public SessionFactory getObject(){
		return instance;
	}

	public void close() {
		if (instance != null) {
			instance.shutdown();
		}
	}

	public Class<?> getObjectType() {
		return SessionFactory.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public SessionFactoryBean setDataSource(String url,String user,String password) {
		this.dataSource=DbUtils.createSimpleDataSource(url, user, password);
		return this;
	}
}
