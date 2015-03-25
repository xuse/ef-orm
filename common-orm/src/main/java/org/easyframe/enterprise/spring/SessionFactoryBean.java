package org.easyframe.enterprise.spring;

import jef.database.DbClientBuilder;
import jef.database.DbUtils;
import jef.database.jpa.JefEntityManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 供Spring上下文中初始化EF-ORM Session Factory使用
 * 
 * @author jiyi
 * 
 */
public class SessionFactoryBean extends DbClientBuilder implements FactoryBean<JefEntityManagerFactory>, InitializingBean {
	public void afterPropertiesSet() throws Exception {
		instance = buildSessionFactory();
	}

	public JefEntityManagerFactory getObject(){
		return instance;
	}

	public void close() {
		if (instance != null) {
			instance.close();
		}
	}

	public Class<?> getObjectType() {
		return JefEntityManagerFactory.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public SessionFactoryBean setDataSource(String url,String user,String password) {
		this.dataSource=DbUtils.createSimpleDataSource(url, user, password);
		return this;
	}
}
