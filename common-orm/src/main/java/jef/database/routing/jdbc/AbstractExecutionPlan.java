package jef.database.routing.jdbc;

import jef.database.annotation.PartitionResult;

public abstract class AbstractExecutionPlan implements ExecutionPlan{
	PartitionResult[] sites;
	
	
	protected AbstractExecutionPlan(PartitionResult[] sites){
		this.sites=sites;
	}

	public boolean isMultiDatabase() {
		return sites.length>1;
	}

	public PartitionResult[] getSites() {
		return sites;
	}
}
