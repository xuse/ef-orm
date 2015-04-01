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
			createDefaultDialect();
		}
		return dialect.generateUrl(host, port, pathOrName);
	}

	private void createDefaultDialect() {
		DatabaseDialect dialect = new SQLServer2005Dialect();
		try {
			Class.forName(dialect.getDriverClass(""));
			this.dialect = dialect; // 暂时先作为2005处理，后续根据版本号再升级为2012和2014
		} catch (ClassNotFoundException e) {
			dialect = new SQLServer2000Dialect();
		}
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
	public String getDriverClass(String url) {
		if (this.dialect == null) {
			createDefaultDialect();
		}
		return super.getDriverClass(url);
	}

	/**
	 * 根据数据库版本信息判断当前数据库实际应该用哪个方言
	 */
	@Override
	public void init(OperateTarget asOperateTarget) {
		try{
			String version=asOperateTarget.getMetaData().getDatabaseVersion();
			int index=version.indexOf('.');
			if(index==-1){
				return;
			}
			int ver=Integer.parseInt(version.substring(0,index));
			switch(ver){
			case 9:
				if(!(dialect instanceof SQLServer2005Dialect)){
					this.dialect=new SQLServer2005Dialect();
					LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
				}
				break;
			case 10:
				//10.0=2008, 10.5=2008 R2
				this.dialect=new SQLServer2008Dialect();
				LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
				break;
			case 11:
				//version 11= SQLServer 2012
			case 12:
				//version 12= SQLServer 2014
			case 13:
				//???
			case 14:
				//???
			case 15:
				//???
			case 16:
				//???
			case 17:
				this.dialect=new SQLServer2012Dialect();
				LogUtil.info("Determin SQL-Server Dialect to [{}]",dialect.getClass());
				break;
			}
		}catch(SQLException e){
			throw new PersistenceException(e);
		}
		super.init(asOperateTarget);
	}
}
