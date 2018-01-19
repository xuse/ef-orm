package jef.database.dialect;

import jef.database.meta.Feature;


/**
 * SQL Server 2012
 * @author jiyi
 *
 */
public class SQLServer2016Dialect extends SQLServer2012Dialect{
	
	public SQLServer2016Dialect(){
		super();
		//"COMMENT ON TABLE Xxx" is unsupported under SQL Server 2016.. 
		features.remove(Feature.SUPPORT_COMMENT);
	}
	

	
}
