package jef.database.innerpool;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import jef.database.DbMetaData;
import jef.database.DbUtils;
import jef.database.PartitionMetadata;
import jef.database.annotation.PartitionResult;
import jef.database.meta.DdlGenerator;
import jef.database.meta.DdlGeneratorImpl;
import jef.database.meta.ITableMetadata;
import jef.database.support.MetadataEventListener;
import jef.tools.Assert;

/**
 * 执行带路由功能的元数据操作
 * DbMetaData中封装不带路由的元数据操作，这里封装路由逻辑
 * @author jiyi
 *
 */
public abstract class AbstractMetaDataService implements IUserManagedPool{
	PartitionMetadata pm=new PartitionMetadata(this);
	private final Map<String,DdlGenerator>      ddls=new HashMap<String,DdlGenerator>();
	
	public DdlGenerator getDdlGenerator(String dbkey) {
		DdlGenerator ddl= ddls.get(dbkey);
		if(ddl==null){
			return createDdlGenerator(dbkey);
		}
		return ddl;
	}

	private synchronized DdlGenerator createDdlGenerator(String dbkey) {
		DdlGenerator ddl= ddls.get(dbkey);
		if(ddl==null){
			ddl=new DdlGeneratorImpl(this, getProfile(dbkey));			
			ddls.put(dbkey, ddl);
		}
		return ddl;
	}

	public PartitionSupport getPartitionSupport() {
		return pm;
	}
	
	public void tableRefresh(ITableMetadata meta,MetadataEventListener event) throws SQLException {
		Assert.notNull(meta,"The table definition which your want to resresh must not null.");
		PartitionResult[] results=DbUtils.toTableNames(meta, this.getPartitionSupport(),true);
		for(PartitionResult result:results){
			DbMetaData dbmeta=this.getMetadata(result.getDatabase());
			for(String table:result.getTables()){
				if(event==null || event.beforeTableRefresh(meta,table)){
					dbmeta.refreshTable(meta,table,event);
				}
			}
		}
	}
}
