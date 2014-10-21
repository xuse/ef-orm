package jef.database.dialect.statement;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;

public abstract class UnionJudgement {
	public abstract boolean isUnion(String sql); 
	
	public static final UnionJudgement DEFAULT=new UnionJudgement(){
		@Override
		public boolean isUnion(String sql) {
			try {
				Select select = DbUtils.parseNativeSelect(sql);
				return (select.getSelectBody() instanceof Union);  
			} catch (ParseException e) {
				LogUtil.exception("SqlParse Error:", e);
				return false;
			}
		}
	};
	
	/**
	 * Is Druid avaliable?
	 * @return
	 */
	public static boolean isDruid(){
		try{
			Class.forName("com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerStatementParser");
			return true;
		}catch(ClassNotFoundException e){
			return false;
		}
	}
}
