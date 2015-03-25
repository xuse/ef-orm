package jef.database.routing.sql;

import jef.database.routing.PartitionResult;

/**
 * 抽象执行计划
 * @author jiyi
 *
 */
public abstract class AbstractExecutionPlan{
	protected PartitionResult[] sites;
	
	
	protected AbstractExecutionPlan(PartitionResult[] sites){
		this.sites=sites;
	}


	public boolean isMultiDatabase() {
		return sites.length>1;
	}
	
	public boolean isSimple() {
		if(sites==null) return true;
		return sites.length==1 && sites[0].tableSize()==1;
	}

	public boolean isEmpty() {
		return sites==null ||  sites.length==0;
	}

	public PartitionResult[] getSites() {
		return sites;
	}
}
