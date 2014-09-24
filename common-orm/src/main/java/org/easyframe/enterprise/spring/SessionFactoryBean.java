package org.easyframe.enterprise.spring;

import java.sql.SQLException;

import javax.sql.DataSource;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DataObject;
import jef.database.DbClient;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.MetaHolder;
import jef.database.support.QuerableEntityScanner;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 供Spring上下文中初始化EF-ORM Session Factory使用
 * @author jiyi
 *
 */
public class SessionFactoryBean implements FactoryBean<JefEntityManagerFactory>, InitializingBean {
	private DataSource dataSource;
	/**
	 * 指定对以下包内的实体做一次增强扫描
	 */
	private String enhancePackages;
	/**
	 * 指定扫描若干包，逗号分隔
	 */
	private String scanPackages;
	/**
	 * 指定扫描若干表作为动态表，逗号分隔
	 */
	private String dynamicTables;
	/**
	 * 是否将所有未并映射的表都当做动态表进行映射
	 */
	private boolean registeNonMappingTableAsDynamic;
	/**
	 * 扫描到实体后，如果数据库中不存在，是否建表
	 */
	private boolean createTable= true;
	
	/**
	 * 扫描到实体后，如果数据库中存在，是否修改表
	 */
	private boolean alterTable = true;
	
	/**
	 * 扫描到实体后，如果准备修改表，如果数据库中的列更多，是否允许删除列
	 */
	private boolean allowDropColumn;
	
	
	/**
	 * 事务支持类型
	 */
	private TransactionType txType;
	
	private JefEntityManagerFactory instance;


	public String getTxType() {
		return txType==null?null:txType.name();
	}

	public void setTxType(String txType) {
		this.txType =TransactionType.valueOf(StringUtils.upperCase(txType));
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource);
		instance=buildSessionFactory();
	}

	private JefEntityManagerFactory buildSessionFactory() {
		if(enhancePackages!=null){
			new EntityEnhancer().enhance(StringUtils.split(enhancePackages,","));
		}
		JefEntityManagerFactory sf=new JefEntityManagerFactory(dataSource,txType);
		if(scanPackages!=null){
			QuerableEntityScanner qe=new QuerableEntityScanner();
			qe.setImplClasses(DataObject.class);
			LogUtil.info("Starting scan easyframe entity from package: "+ scanPackages);
			qe.setPackageNames(scanPackages);
			
			qe.setAllowDropColumn(allowDropColumn);
			qe.setAlterTable(alterTable);
			qe.setCreateTable(createTable);
			qe.setEntityManagerFactory(sf);
			qe.doScan();
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

	public String getScanPackages() {
		return scanPackages;
	}

	/**
	 * 设置要扫描的包
	 * @return 要扫描的包，逗号分隔
	 */	
	public void setScanPackages(String scanPackages) {
		this.scanPackages = scanPackages;
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

	/**
	 * 扫描数据库中当前schema下的所有表，如果尚未有实体与该表对应，那么就将该表作为动态表建模。
	 * @param registeNonMappingTableAsDynamic
	 */
	public void setRegisteNonMappingTableAsDynamic(boolean registeNonMappingTableAsDynamic) {
		this.registeNonMappingTableAsDynamic = registeNonMappingTableAsDynamic;
	}
}
