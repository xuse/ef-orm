package com.github.geequery.springdata.config;

import javax.sql.DataSource;

import org.easyframe.enterprise.spring.CommonDao;
import org.easyframe.enterprise.spring.CommonDaoImpl;
import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.geequery.springdata.repository.config.EnableGqRepositories;

import jef.database.SessionFactory;
import jef.database.datasource.SimpleDataSource;

@Configuration
@EnableTransactionManagement
@EnableGqRepositories(basePackages = { "com.github.geequery.springdata.test.repo" }, transactionManagerRef = "tx1", sessionFactoryRef = "emf1")
public class PersistenceContext {
    @Bean(name="ds1")
    DataSource dataSource(Environment env) {
        SimpleDataSource ds = new SimpleDataSource("jdbc:derby:./db;create=true", null, null);
        return ds;
    }

    @Bean(name = {"emf1"})
    @Primary
    SessionFactory entityManagerFactory(@Qualifier("ds1") DataSource dataSource, Environment env) {
        SessionFactoryBean bean = new org.easyframe.enterprise.spring.SessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPackagesToScan(new String[] { "com.github.geequery.springdata.test.entity" });
        bean.afterPropertiesSet();
        return bean.getObject();
    }

    @Bean(name = "tx1")
    PlatformTransactionManager transactionManager(@Qualifier("ds1")DataSource ds1) {
        return new DataSourceTransactionManager(ds1);
    }

    @Bean()
    CommonDao commonDao(@Qualifier("emf1")SessionFactory sf1) {
        return new CommonDaoImpl(sf1);
    }
}