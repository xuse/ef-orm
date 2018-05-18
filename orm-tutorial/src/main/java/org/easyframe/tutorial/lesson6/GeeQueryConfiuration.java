package org.easyframe.tutorial.lesson6;

import java.sql.Connection;

import javax.inject.Provider;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.easyframe.enterprise.spring.SessionFactoryBean;
import org.easyframe.enterprise.spring.TransactionMode;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;

@Configuration
public class GeeQueryConfiuration {
	@Bean
	public DataSource dataSource() {
		// implementation omitted
		return null;
	}

	@Bean
	public SqlSessionFactory myBatis() throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource());
		return factoryBean.getObject();
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		return new DataSourceTransactionManager(dataSource());
	}

	@Bean()
	public EntityManagerFactory entityManagerFactory(DataSource dataSource, Environment env) {
		SessionFactoryBean bean = new org.easyframe.enterprise.spring.SessionFactoryBean();
		bean.setDataSource(dataSource);
		//bean.setPackagesToScan(...) //Your packages here
		bean.afterPropertiesSet();
		bean.setTransactionMode(TransactionMode.JDBC);
		return bean.getObject();
	}

	@Bean
	public com.querydsl.sql.Configuration querydslConfiguration() {
		SQLTemplates templates = H2Templates.builder().build(); 
		com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(templates);
		return configuration;
	}

	@Bean
	public SQLQueryFactory queryFactory() {
		Provider<Connection> provider = new SpringConnectionProvider(dataSource());
		return new SQLQueryFactory(querydslConfiguration(), provider);
	}
}
