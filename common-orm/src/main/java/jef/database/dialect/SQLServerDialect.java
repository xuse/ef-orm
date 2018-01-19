package jef.database.dialect;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;

/**
 * 自动检查和适配SQLServer不同版本的方言
 * 
 * @author jiyi
 * 
 */
public class SQLServerDialect extends AbstractDelegatingDialect {
	private DatabaseDialect defaultDialect;

	protected DatabaseDialect createDefaultDialect() {
		DatabaseDialect dialect = new SQLServer2005Dialect();
		try {
			Class.forName(dialect.getDriverClass(""));
			// 暂时先作为2005处理，后续根据版本号再升级为2012和2014
		} catch (ClassNotFoundException e) {
			dialect = new SQLServer2000Dialect();
		}
		this.defaultDialect = dialect;
		return dialect;
	}

	@Override
	public void parseDbInfo(ConnectInfo connectInfo) {
		if (dialect == defaultDialect) {
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

	/**
	 * 根据数据库版本信息判断当前数据库实际应该用哪个方言
	 */
	@Override
	protected DatabaseDialect decideDialect(DbMetaData meta) {
		DatabaseDialect dialect = this.dialect;
		try {
			String version = meta.getDatabaseVersion();
			int index = version.indexOf('.');
			if (index == -1) {
				return dialect;
			}
			int ver = Integer.parseInt(version.substring(0, index));
			switch (ver) {
			case 9:
				if (!(dialect instanceof SQLServer2005Dialect)) {
					LogUtil.info("Determin SQL-Server Dialect to [{}]", dialect.getClass());
					return new SQLServer2005Dialect();
				}
				break;
			case 10:
				// 10.0=2008, 10.5=2008 R2
				LogUtil.info("Determin SQL-Server Dialect to [{}]", dialect.getClass());
				return new SQLServer2008Dialect();
			case 11:
				// version 11= SQLServer 2012
			case 12:
				// version 12= SQLServer 2014
			case 13:
				return new SQLServer2012Dialect();
			case 14:
				// version 14= SQLServer 2016
			case 15:
			case 16:
			case 17:
				return new SQLServer2016Dialect();
			default:
				LogUtil.info("Determin SQL-Server Dialect to [{}]", dialect.getClass());
				return new SQLServer2012Dialect();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		return dialect;
	}
}
