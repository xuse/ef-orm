package jef.database.dialect;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.OperateTarget;

/**
 * 自动检查和适配SQLServer不同版本的方言
 * 
 * @author jiyi
 * 
 */
public class SQLServerDialect extends AbstractDelegatingDialect {

	@Override
	public String generateUrl(String host, int port, String pathOrName) {
		if (this.dialect == null) {
			DatabaseDialect dialect = new SQLServer2005Dialect();
			try {
				Class.forName(dialect.getDriverClass(""));
				this.dialect = dialect; // 暂时先作为2005处理，后续根据版本号再升级为2012和2014
			} catch (ClassNotFoundException e) {
				dialect = new SQLServer2000Dialect();
			}
		}
		return dialect.generateUrl(host, port, pathOrName);
	}

	@Override
	public void parseDbInfo(ConnectInfo connectInfo) {
		if (dialect == null) {
			if (connectInfo.getUrl().startsWith("jdbc:microsoft:")) {
				dialect = new SQLServer2000Dialect();
			} else {
				dialect = new SQLServer2005Dialect();
				dialect.parseDbInfo(connectInfo);
				return;
			}
		}
		super.parseDbInfo(connectInfo);
	}

	@Override
	public void init(OperateTarget asOperateTarget) {
		try{
			String version=asOperateTarget.getMetaData().getDatabaseVersion();
			if(version.startsWith("9.")){    //
				if(!(dialect instanceof SQLServer2005Dialect)){
					this.dialect=new SQLServer2005Dialect();
					LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
				}
			}else if(version.startsWith("10.")){//10.0=2008, 10.5=2008 R2 
				this.dialect=new SQLServer2008Dialect();
				LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
			}else if(version.startsWith("11.")){//2012
				this.dialect=new SQLServer2012Dialect();
				LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
			}else if(version.startsWith("12.")){//2014 
				this.dialect=new SQLServer2012Dialect();
				LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
			}
		}catch(SQLException e){
			throw new PersistenceException(e);
		}
		super.init(asOperateTarget);
	}
}
