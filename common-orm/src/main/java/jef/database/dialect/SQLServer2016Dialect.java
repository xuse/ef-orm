package jef.database.dialect;

import jef.database.dialect.handler.DerbyLimitHandler;
import jef.database.dialect.handler.LimitHandler;
import jef.database.dialect.handler.SQL2005LimitHandler;
import jef.database.dialect.handler.SQL2005LimitHandlerSlowImpl;
import jef.database.jdbc.statement.UnionJudgement;
import jef.database.meta.Feature;


/**
 * SQL Server 2016
 * @author jiyi
 *
 */
public class SQLServer2016Dialect extends SQLServer2012Dialect{
	
	public SQLServer2016Dialect(){
		super();
		//"COMMENT ON TABLE Xxx" is unsupported under SQL Server 2016. I don't know why... 
		features.remove(Feature.SUPPORT_COMMENT);
	}
	

	@Override
	protected LimitHandler generateLimitHander() {
		//FIXME 似乎2012中支持的offset /fetch next rows 在2016中又不支持了，不知道为什么这么反复 
		if(UnionJudgement.isDruid()){
			return new SQL2005LimitHandler();
		}else{
			return new SQL2005LimitHandlerSlowImpl();
		}
	}
	
}
