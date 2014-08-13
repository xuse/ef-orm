package jef.database.routing.jdbc;

import java.util.List;
import java.util.Map.Entry;

import jef.database.annotation.PartitionResult;

public interface SqlProvider {
	Entry<String,List<Object>>  getSql(PartitionResult site);
}
