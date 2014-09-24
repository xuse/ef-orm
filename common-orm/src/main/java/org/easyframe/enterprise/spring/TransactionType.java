package org.easyframe.enterprise.spring;

/**
 * 在与Spring集成时的事务管理方式
 * 
 * JPA——使用JPA事务管理器使用。
 * DATASOURCE——在与JDBC或者Hibernate方式混合使用时，共享事务。
 * MANAGED——待定
 * 
 * @author jiyi
 *
 */
public enum TransactionType {
	JPA,
	DATASOURCE,
	MANAGED,
}
