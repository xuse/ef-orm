package org.easyframe.enterprise.spring;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DataObject;
import jef.database.DbClient;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.MetaHolder;
import jef.database.support.QuerableEntityScanner;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 供Spring上下文中初始化EF-ORM Session Factory使用
 * @author jiyi
 *
 */
public class SessionFactoryBean implements FactoryBean<JefEntityManagerFactory>, InitializingBean {
	/**
	 * 多数据源。分库分表时可以使用。
	 * 在Spring配置时，可以使用这样的格式来配置
	 * <pre><code>
	 * &lt;property name="dataSources"&gt;
	 * 	&lt;map&gt;
	 * 	 &lt;entry key="dsname1" value-ref="ds1" /&gt;
	 * 	 &lt;entry key="dsname2" value-ref="ds2" /&gt;
	 * 	&lt;/map&gt;
	 * &lt;/property&gt;
	 * </code></pre>
	 */
	private Map<String,DataSource> dataSources;
	/**
	 * 多数据源时的缺省数据源名称
	 */
	private String defaultDatasource;
	/**
	 * 单数据源。
	 */
	private DataSource dataSource;
	/**
	 * 指定对以下包内的实体做一次增强扫描。多个包名之间逗号分隔。<br>
	 * 如果不配置此项，默认将对packagesToScan包下的类进行增强。<br>
	 * 不过不想进行类增强扫描，可配置为"none"。
	 */
	private String enhancePackages;
	/**
	 * 指定扫描若干包,配置示例如下——
	 * <code><pre>
	 * &lt;list&gt;
	 *  &lt;value&gt;org.easyframe.test&lt;/value&gt;
	 *  &lt;value&gt;org.easyframe.entity&lt;/value&gt;
	 * &lt;/list&gt;
	 * </pre></code>
	 */
	private String[] packagesToScan;
	/**
	 * 扫描已知的若干注解实体类，配置示例如下——
	 * <code><pre>
	 * &lt;list&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Product&lt;/value&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Users&lt;/value&gt;
	 * &lt;/list&gt;
	 * </pre></code>
	 */
	private String[] annotatedClasses;
	
	/**
	 * 指定扫描若干表作为动态表，此处配置表名，名称之间逗号分隔
	 */
	private String dynamicTables;
	
	/**
	 * 是否将所有未并映射的表都当做动态表进行映射。
	 */
	private boolean registeNonMappingTableAsDynamic;
	
	/**
	 * 扫描到实体后，如果数据库中不存在，是否建表
	 * <br>
	 * 默认开启
	 */
	private boolean createTable= true;
	
	/**
	 * 扫描到实体后，如果数据库中存在，是否修改表
	 * <br>
	 * 默认开启
	 */
	private boolean alterTable = true;
	
	/**
	 * 扫描到实体后，如果准备修改表，如果数据库中的列更多，是否允许删除列
	 * <br>
	 * 默认关闭
	 */
	private boolean allowDropColumn;
	/**
	 * 事务支持类型，可配置为
	 * <ul>
	 * <li><strong>JPA</strong></li><br>
	 * 使用JPA的方式管理事务，对应Spring的 {@linkplain org.springframework.orm.jpa.JpaTransactionManager JpaTransactionManager},
	 * 适用于ef-orm单独作为数据访问层时使用。
	 * <li><strong>JTA</strong></li><br>
	 * 使用JTA的分布式事务管理。使用JTA可以在多个数据源、内存数据库、JMS目标之间保持事务一致性。<br>推荐使用atomikos作为JTA管理器。
	 * 对应Spring的 {@linkplain org.springframework.transaction.jta.JtaTransactionManager JtaTransactionManager}。<br>
	 * 当需要在多个数据库之间保持事务一致性时酌情使用。
	 * <li><strong>JDBC</strong></li><br>
	 * 使用JDBC事务管理。当和Hibernate一起使用时，可以利用Hibernate的连接共享Hibernate事务。当与JdbcTemplate共同使用时，
	 * 也可以获得DataSource所绑定的连接从而共享JDBC事务。
	 * 对应Spring的 {@linkplain org.springframework.orm.hibernate3.HibernateTransactionManager HibernateTransactionManager}
	 * 和{@linkplain org.springframework.jdbc.datasource.DataSourceTransactionManager DataSourceTransactionManager}。<br>
	 * 一般用于和Hibernate/Ibatis/MyBatis/JdbcTemplate等共享同一个事务。
	 * </ul>
	 * 默认为{@code JPA}
	 * @see TransactionMode
	 */
	private TransactionMode transactionMode;
	/**
	 * 对象实例
	 */
	private JefEntityManagerFactory instance;

	public String getTransactionMode() {
		return transactionMode==null?null:transactionMode.name();
	}

	public void setTransactionMode(String txType) {
		this.transactionMode =TransactionMode.valueOf(StringUtils.upperCase(txType));
	}

	public void afterPropertiesSet() throws Exception {
		if(dataSource==null && dataSources==null){
			throw new IllegalArgumentException("No datasource found.");
		}
		instance=buildSessionFactory();
	}

	private JefEntityManagerFactory buildSessionFactory() {
		//try enahcen entity if theres 'enhancePackages'.
		if(enhancePackages!=null){
			if(!enhancePackages.equals("none")){
				new EntityEnhancer().enhance(StringUtils.split(enhancePackages,","));
			}
		}else if(packagesToScan!=null){
			//if there is no enhances packages, try enhance 'package to Scan'
			new EntityEnhancer().enhance(packagesToScan);
		}
		//check data sources.
		if(dataSource!=null && dataSources!=null){
			throw new IllegalArgumentException("You must config either 'datasource' or 'datasources' but not both.");
		}
		JefEntityManagerFactory sf;
		if(dataSource!=null){
			sf=new JefEntityManagerFactory(dataSource,transactionMode);
		}else{
			RoutingDataSource rs=new RoutingDataSource(new MapDataSourceLookup(dataSources));
			sf=new JefEntityManagerFactory(rs,transactionMode);
		}
		if(packagesToScan!=null || annotatedClasses!=null){
			QuerableEntityScanner qe=new QuerableEntityScanner();
			if(transactionMode==TransactionMode.JTA){
				//JTA事务下，DDL语句必须在已启动后立刻就做，迟了就被套进JTA是事务中，出错。
				qe.setCheckSequence(false);
			}
			qe.setImplClasses(DataObject.class);
			qe.setAllowDropColumn(allowDropColumn);
			qe.setAlterTable(alterTable);
			qe.setCreateTable(createTable);
			qe.setEntityManagerFactory(sf);
			if(annotatedClasses!=null){
				for(String s:annotatedClasses){
					if(!qe.registeEntity(StringUtils.trim(s))){
						throw new IllegalArgumentException("Register entity class ["+s+"] error.");
					}
				}
			}
			if(packagesToScan!=null){
				qe.setPackageNames(StringUtils.join(packagesToScan,','));
				LogUtil.info("Starting scan easyframe entity from package: "+ packagesToScan);
				qe.doScan();	
			}
		}
		if(dynamicTables!=null){
			DbClient client=sf.getDefault();
			for(String s:StringUtils.split(dynamicTables,",")){
				String table=s.trim();
				registe(client,table);
			}
		}
		if(registeNonMappingTableAsDynamic){
			DbClient client=sf.getDefault();
			try{
				for(String tableName:client.getMetaData(null).getTableNames()){
					if(MetaHolder.lookup(null, tableName)!=null){
						registe(client,tableName);
					}
				}	
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
			
		}
		return sf;
	}

	private void registe(DbClient client, String table) {
		if(MetaHolder.getDynamicMeta(table)==null){
			try {
				MetaHolder.initMetadata(client, table);
				LogUtil.show("DynamicEntity: ["+table+"] registed.");
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
		}
	}

	public JefEntityManagerFactory getObject() throws Exception {
		return instance;
	}
	
	public void close(){
		if(instance!=null){
			instance.close();
		}
	}

	public Class<?> getObjectType() {
		return JefEntityManagerFactory.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * 设置数据源
	 * @param dataSource 数据源
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置要扫描的包
	 * @return 要扫描的包，逗号分隔
	 */	
	public void setPackagesToScan(String[] scanPackages) {
		this.packagesToScan = scanPackages;
	}

	public boolean isAlterTable() {
		return alterTable;
	}

	/**
	 * 扫描到实体后，是否修改数据库中与实体定义不同的表
	 * @param alterTable 'true' , EF-ORM will alter tables in database.
	 */
	public void setAlterTable(boolean alterTable) {
		this.alterTable = alterTable;
	}

	public boolean isCreateTable() {
		return createTable;
	}
	
	/**
	 * 扫描到实体后，是否在数据库中创建不存在的表
	 * @param createTable true将会创建表
	 */
	public void setCreateTable(boolean createTable) {
		this.createTable = createTable;
	}

	public boolean isAllowDropColumn() {
		return allowDropColumn;
	}

	/**
	 * 扫描到实体后，在Alter数据表时，是否允许删除列。
	 * @param allowDropColumn true允许删除列
	 */
	public void setAllowDropColumn(boolean allowDropColumn) {
		this.allowDropColumn = allowDropColumn;
	}

	public String getEnhancePackages() {
		return enhancePackages;
	}

	/**
	 * 是否检查并增强实体。
	 * 注意，增强实体仅对目录中的class文件生效，对jar包中的class无效。
	 * @param enhancePackages 要扫描的包 
	 */
	public void setEnhancePackages(String enhancePackages) {
		this.enhancePackages = enhancePackages;
	}

	public String getDynamicTables() {
		return dynamicTables;
	}

	/**
	 * 扫描数据库中存在的表作为动态表模型
	 * @param dynamicTables 表名，逗号分隔
	 */
	public void setDynamicTables(String dynamicTables) {
		this.dynamicTables = dynamicTables;
	}

	public boolean isRegisteNonMappingTableAsDynamic() {
		return registeNonMappingTableAsDynamic;
	}

	public String[] getAnnotatedClasses() {
		return annotatedClasses;
	}
	/**
	 * 扫描已知的若干注解实体类，配置示例如下——
	 * <code><pre>
	 * &lt;list&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Product&lt;/value&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Users&lt;/value&gt;
	 * &lt;/list&gt;
	 * </pre></code>
	 */
	public void setAnnotatedClasses(String[] annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	/**
	 * 扫描数据库中当前schema下的所有表，如果尚未有实体与该表对应，那么就将该表作为动态表建模。
	 * @param registeNonMappingTableAsDynamic
	 */
	public void setRegisteNonMappingTableAsDynamic(boolean registeNonMappingTableAsDynamic) {
		this.registeNonMappingTableAsDynamic = registeNonMappingTableAsDynamic;
	}
	
	public Map<String, DataSource> getDataSources() {
		return dataSources;
	}
	
	public String getDefaultDatasource() {
		return defaultDatasource;
	}
	/**
	 * 设置多数据源时的缺省数据源名称
	 * @param defaultDatasource name of the datasource.
	 */
	public void setDefaultDatasource(String defaultDatasource) {
		this.defaultDatasource = defaultDatasource;
	}
	/**
	 * 多数据源。分库分表时可以使用。
	 * 在Spring配置时，可以使用这样的格式来配置
	 * <pre><code>
	 * &lt;property name="dataSources"&gt;
	 * 	&lt;map&gt;
	 * 	 &lt;entry key="dsname1" value-ref="ds1" /&gt;
	 * 	 &lt;entry key="dsname2" value-ref="ds2" /&gt;
	 * 	&lt;/map&gt;
	 * &lt;/property&gt;
	 * </code></pre>
	 */
	public void setDataSources(Map<String, DataSource> datasources) {
		this.dataSources = datasources;
	}
}
