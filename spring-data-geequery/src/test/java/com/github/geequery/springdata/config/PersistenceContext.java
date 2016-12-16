package com.github.geequery.springdata.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import jef.database.datasource.SimpleDataSource;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.easyframe.enterprise.spring.JefJpaDialect;
import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.geequery.springdata.repository.config.EnableGqRepositories;

@Configuration
@EnableTransactionManagement
@EnableGqRepositories(basePackages = {
        "com.github.geequery.springdata.test.repo"
})
public class PersistenceContext {
	@Bean()
	DataSource dataSource(Environment env) {
		SimpleDataSource ds = new SimpleDataSource("jdbc:derby:./db;create=true", null, null);
		return ds;
	}

	@Bean
	EntityManagerFactory entityManagerFactory(DataSource dataSource, Environment env) {
		SessionFactoryBean bean = new org.easyframe.enterprise.spring.SessionFactoryBean();
		bean.setDataSource(dataSource);
		bean.setPackagesToScan(new String[]{"com.github.geequery.springdata.test.entity"});
		bean.afterPropertiesSet();
		return bean.getObject();
	}


	@Bean
	JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory);
		transactionManager.setJpaDialect(new JefJpaDialect());
		return transactionManager;
	}
	
	@Bean
	CommonDao commonDao(EntityManagerFactory entityManagerFactory){
		return new CommonDaoImpl(entityManagerFactory);
	}
}