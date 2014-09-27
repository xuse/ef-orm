package org.easyframe.enterprise.spring;

/**
 * 在与Spring集成时的事务管理模式
 * <ul>
 * <li>JPA——使用JPA事务管理器使用。</li>
 * <li>JTA——使用JTA分布式事务时</li>
 * <li>JDBC——共享事务。在与JDBC或者Hibernate方式混合使用时，共享事务。</li>
 * </ul>
 * @author jiyi
 *
 */
public enum TransactionMode {
	/**
	 * 使用Spring的JPA事务管理器时的模式。
	 * 为默认的事务管理方式
	 */
	JPA,
	/**
	 * 使用JTA事务管理器时候的模式。
	 */
	JTA,
	/**
	 * 共享事务。通过Spring的JDBC事务管理器，或者Hibernate事务管理器暴露出JDBC事务时。
	 * 使用此模式，可以共享Spring的JDBC事务。
	 */
	JDBC
}
