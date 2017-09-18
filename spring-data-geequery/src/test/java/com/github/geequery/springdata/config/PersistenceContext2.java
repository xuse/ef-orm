package com.github.geequery.springdata.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import jef.database.datasource.SimpleDataSource;

import org.easyframe.enterprise.spring.JefJpaDialect;
import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.geequery.springdata.repository.config.EnableGqRepositories;

@Configuration
@EnableTransactionManagement
@EnableGqRepositories(basePackages = { "com.github.geequery.springdata.test2.repo" }, transactionManagerRef = "tx2",entityManagerFactoryRef="emf2")
public class PersistenceContext2 {
    @Bean(name="ds2")
    public DataSource dataSource(Environment env) {
        SimpleDataSource ds = new SimpleDataSource("jdbc:derby:./db2;create=true", null, null);
        return ds;
    }

    @Bean(name = "emf2")
   public  EntityManagerFactory entityManagerFactory(@Qualifier("ds2")DataSource dataSource, Environment env) {
        SessionFactoryBean bean = new org.easyframe.enterprise.spring.SessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPackagesToScan(new String[] { "com.github.geequery.springdata.test.entity" });
        bean.afterPropertiesSet();
        return bean.getObject();
    }

    @Bean(name = "tx2")
    public  JpaTransactionManager transactionManager(@Qualifier("emf2")EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        transactionManager.setJpaDialect(new JefJpaDialect());
        return transactionManager;
    }
}