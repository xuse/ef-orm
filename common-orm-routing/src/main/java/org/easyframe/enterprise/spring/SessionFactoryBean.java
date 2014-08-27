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
	
	private JefEntityManagerFactory instance;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource);
		instance=buildSessionFactory();
	}

	private JefEntityManagerFactory buildSessionFactory() {
		if(enhancePackages!=null){
			new EntityEnhancer().enhance(StringUtils.split(enhancePackages,","));
		}
		JefEntityManagerFactory sf=new JefEntityManagerFactory(dataSource);
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

	public Class<?> getObjectType() {
		return JefEntityManagerFactory.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getScanPackages() {
		return scanPackages;
	}

	public void setScanPackages(String scanPackages) {
		this.scanPackages = scanPackages;
	}

	public boolean isAlterTable() {
		return alterTable;
	}

	public void setAlterTable(boolean alterTable) {
		this.alterTable = alterTable;
	}

	public boolean isCreateTable() {
		return createTable;
	}

	public void setCreateTable(boolean createTable) {
		this.createTable = createTable;
	}

	public boolean isAllowDropColumn() {
		return allowDropColumn;
	}

	public void setAllowDropColumn(boolean allowDropColumn) {
		this.allowDropColumn = allowDropColumn;
	}

	public String getEnhancePackages() {
		return enhancePackages;
	}

	public void setEnhancePackages(String enhancePackages) {
		this.enhancePackages = enhancePackages;
	}

	public String getDynamicTables() {
		return dynamicTables;
	}

	public void setDynamicTables(String dynamicTables) {
		this.dynamicTables = dynamicTables;
	}

	public boolean isRegisteNonMappingTableAsDynamic() {
		return registeNonMappingTableAsDynamic;
	}

	public void setRegisteNonMappingTableAsDynamic(boolean registeNonMappingTableAsDynamic) {
		this.registeNonMappingTableAsDynamic = registeNonMappingTableAsDynamic;
	}
}
