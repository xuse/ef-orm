package jef.database.routing.sql;

import jef.database.annotation.PartitionResult;

public abstract class AbstractExecutionPlan implements ExecutionPlan{
	PartitionResult[] sites;
	private String changeDataSource;
	
	protected AbstractExecutionPlan(PartitionResult[] sites){
		this.sites=sites;
	}

	public AbstractExecutionPlan(String bindDsName) {
		changeDataSource=bindDsName;
	}

	public boolean isMultiDatabase() {
		return sites.length>1;
	}

	public PartitionResult[] getSites() {
		return sites;
	}

	public String isChangeDatasource() {
		return changeDataSource;
	}
	
	
}
