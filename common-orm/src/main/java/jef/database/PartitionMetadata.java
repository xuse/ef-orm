package jef.database;

import java.sql.SQLException;
import java.util.Collection;

import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;

/**
 * 描述分表后的实际存在表的情况
 * 
 * @author Administrator
 * 
 */
public class PartitionMetadata implements PartitionSupport{
	/**
	 * 所属的数据库
	 */
	private IUserManagedPool parent;
	
	public PartitionMetadata(IUserManagedPool parent){
		this.parent=parent;
	}
	
	public Collection<String> getSubTableNames(String dbkey, ITableMetadata pTable) throws SQLException {
		return parent.getMetadata(dbkey).getSubTableNames(pTable);
	}
	
	public Collection<String> getDdcNames() {
		return parent.getAllDatasourceNames();
	}
	public boolean isSingleSite() {
		return ORMConfig.getInstance().isSingleSite();
	}

	public DatabaseDialect getProfile(String dbkey) {
		return parent.getProfile(dbkey);
	}

}
