package org.easyframe.enterprise.spring;

/**
 * 在与Spring集成时的事务管理模式
 * <ul>
 * <li>{@link #JPA}——使用JPA事务管理器使用。</li>
 * <li>{@link #JTA}——使用JTA分布式事务时</li>
 * <li>{@link #JDBC}——共享事务。在与JDBC或者Hibernate方式混合使用时，共享事务。</li>
 * </ul>
 * @author jiyi
 *
 */
public enum TransactionMode {
	 
	/**
	 * 为默认的事务管理方式
	 * 使用JPA的方式管理事务，对应Spring的 {@linkplain org.springframework.orm.jpa.JpaTransactionManager JpaTransactionManager},
	 * 适用于ef-orm单独作为数据访问层时使用。
	 */
	JPA,
	/**
	 * 使用JTA事务管理器时候的模式。
	 * 使用JTA可以在多个数据源、内存数据库、JMS目标之间保持事务一致性。<br>推荐使用atomikos作为JTA管理器。
	 * 对应Spring的 {@linkplain org.springframework.transaction.jta.JtaTransactionManager JtaTransactionManager}。<br>
	 * 当需要在多个数据库之间保持事务一致性时酌情使用。
	 */
	JTA,
	/**
	 * 共享事务。通过Spring的JDBC事务管理器，或者Hibernate事务管理器暴露出JDBC事务时。<br>
	 * 使用此模式，可以共享Spring的JDBC事务。<br>
	 * 对应Spring的 {@linkplain org.springframework.orm.hibernate3.HibernateTransactionManager HibernateTransactionManager}
	 * 和{@linkplain org.springframework.jdbc.datasource.DataSourceTransactionManager DataSourceTransactionManager}。<br>
	 * 一般用于和Hibernate/Ibatis/MyBatis/JdbcTemplate等共享同一个事务。
	 */
	JDBC
}
