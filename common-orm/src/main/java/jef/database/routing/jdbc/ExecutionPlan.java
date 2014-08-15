package jef.database.routing.jdbc;

import jef.database.Session;
import jef.database.annotation.PartitionResult;

public interface ExecutionPlan {

	boolean isMultiDatabase();

	PartitionResult[] getSites();

	int processUpdate(PartitionResult site, Session session);

}
