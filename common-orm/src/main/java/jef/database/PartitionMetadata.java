package jef.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.ConnectionAndMetadataProvider;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;
import jef.database.routing.PartitionResult;
import jef.tools.Exceptions;

/**
 * 描述分表后的实际存在表的情况
 * 
 * @author Administrator
 * 
 */
public class PartitionMetadata implements PartitionSupport{
	private ConnectionAndMetadataProvider parent;
	
	public PartitionMetadata(ConnectionAndMetadataProvider parent){
		this.parent=parent;
	}
	
	public Collection<String> getSubTableNames(String dbkey, ITableMetadata pTable) throws SQLException {
		return parent.getMetadata(dbkey).getSubTableNames(pTable);
	}
	
	public Collection<String> getDdcNames() {
		return parent.getAllDatasourceNames();
	}

	public DatabaseDialect getProfile(String dbkey) {
		return parent.getMetadata(dbkey).getProfile();
	}

	public void ensureTableExists(String db,String table,ITableMetadata tmeta) throws SQLException {
		DbMetaData meta=parent.getMetadata(db);
		Collection<String> tables=meta.getSubTableNames(tmeta);
		if(tables.contains(table.toUpperCase())){
			return;
		}
		LogUtil.info("Creating table: {}" , table);
		if(meta.createTable(tmeta, table)){
			tables.add(table.toUpperCase());
		}
	}
	

	public boolean isExist(String db, String table,ITableMetadata tmeta) {
		DbMetaData meta=parent.getMetadata(db);
		Collection<String> tables;
		try {
			tables = meta.getSubTableNames(tmeta);
			return tables.contains(table.toUpperCase());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	//FIXME 将子表缓存移到这个类来
	public PartitionResult[] getSubTableNames(ITableMetadata meta) {
		List<PartitionResult> ps = new ArrayList<PartitionResult>();
		Collection<String> dbs = getDdcNames();
		if (dbs.isEmpty()) {
			try {
				Collection<String> result = parent.getMetadata(null).getSubTableNames(meta);
				if (!result.isEmpty()) {
					PartitionResult p = new PartitionResult(result.toArray(new String[result.size()])).setDatabase(null);
					ps.add(p);
				}
			} catch (SQLException e) {
				Exceptions.log(e);
			}
		} else {
			for (String s : dbs) {
				try {
					Collection<String> result = parent.getMetadata(s).getSubTableNames(meta);
					if (!result.isEmpty()) {
						PartitionResult p = new PartitionResult(result.toArray(new String[result.size()])).setDatabase(s);
						ps.add(p);
					}
				} catch (SQLException e) {
					Exceptions.log(e);
				}
			}
		}
		return ps.toArray(new PartitionResult[ps.size()]);
	}

}
