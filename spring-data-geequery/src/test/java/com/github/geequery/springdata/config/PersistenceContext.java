package com.github.geequery.springdata.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import jef.database.datasource.SimpleDataSource;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.easyframe.enterprise.spring.JefJpaDialect;
import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.geequery.springdata.repository.config.EnableGqRepositories;

@Configuration
@EnableTransactionManagement
@EnableGqRepositories(basePackages = { "com.github.geequery.springdata.test.repo" }, transactionManagerRef = "tx1", entityManagerFactoryRef = "emf1")
public class PersistenceContext {
    @Bean(name="ds1")
    DataSource dataSource(Environment env) {
        SimpleDataSource ds = new SimpleDataSource("jdbc:derby:./db;create=true", null, null);
        return ds;
    }

    @Bean(name = {"emf1"})
    EntityManagerFactory entityManagerFactory(@Qualifier("ds1") DataSource dataSource, Environment env) {
        SessionFactoryBean bean = new org.easyframe.enterprise.spring.SessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPackagesToScan(new String[] { "com.github.geequery.springdata.test.entity" });
        bean.afterPropertiesSet();
        return bean.getObject();
    }

    @Bean(name = "tx1")
    JpaTransactionManager transactionManager(@Qualifier("emf1")EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        transactionManager.setJpaDialect(new JefJpaDialect());
        return transactionManager;
    }

    @Bean()
    CommonDao commonDao(@Qualifier("emf1")EntityManagerFactory entityManagerFactory) {
        return new CommonDaoImpl(entityManagerFactory);
    }
}