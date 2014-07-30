package org.easyframe.enterprise.spring;

import jef.common.log.LogUtil;
import jef.database.DataObject;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.support.QuerableEntityScanner;

import org.springframework.beans.factory.InitializingBean;

/**
 * 当Spring初始化完成后扫描指定包下的所有类，如果是JEF实体那么就进行解析。
 * @author Administrator
 *
 */
public class EntityScanner implements InitializingBean{
	private String packages;
	private JefEntityManagerFactory entityManagerFactory;
	private boolean refreshTable=true;
	private boolean allowDropColumn;
	private boolean checkEnhance=false;
	
	public void afterPropertiesSet() throws Exception {
		QuerableEntityScanner qe=new QuerableEntityScanner();
		qe.setImplClasses(DataObject.class);
		LogUtil.info("Starting scan easyframe entity from package: "+ packages);
		if(packages!=null)qe.setPackageNames(packages);
		qe.setCheckEnhance(checkEnhance);
		if(refreshTable){
			qe.setEntityManagerFactory(entityManagerFactory);
		}
		qe.doScan();
	}
	
	/**
	 * 是否允许删除列
	 * @return true表示允许
	 */
	public boolean isAllowDropColumn() {
		return allowDropColumn;
	}
	/**
	 * 设置 是否允许删除列
	 * @param allowDropColumn true表示允许
	 */
	public void setAllowDropColumn(boolean allowDropColumn) {
		this.allowDropColumn = allowDropColumn;
	}


	/**
	 * 设置多个包名,用逗号分隔
	 * @param packages
	 */
	public void setPackages(String packages) {
		this.packages = packages;
	}


	public boolean isCheckEnhance() {
		return checkEnhance;
	}

	public void setCheckEnhance(boolean checkEnhance) {
		this.checkEnhance = checkEnhance;
	}

	/**
	 * 如果要自动创建表，则需要制定EMF
	 * @param entityManagerFactory
	 */
	public void setEntityManagerFactory(JefEntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
	

	/**
	 * true则在扫描时自动创建表
	 * @param refreshTable
	 */
	public void setRefreshTable(boolean refreshTable) {
		this.refreshTable = refreshTable;
	}
}
