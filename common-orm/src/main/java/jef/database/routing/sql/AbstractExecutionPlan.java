package jef.database.routing.sql;

import jef.database.annotation.PartitionResult;

public abstract class AbstractExecutionPlan implements ExecutionPlan{
	private PartitionResult[] sites;
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
	
	@Override
	public boolean isSimple() {
		if(sites==null) return true;
		return sites.length==1 && sites[0].tableSize()==1;
	}

	public PartitionResult[] getSites() {
		return sites;
	}

	public String isChangeDatasource() {
		return changeDataSource;
	}
	
	
}
