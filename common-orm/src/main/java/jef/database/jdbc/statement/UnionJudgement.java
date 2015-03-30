package jef.database.jdbc.statement;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;

/**
 * 判断一个SELECT SQL语句是否为union语句
 * @author jiyi
 *
 */
public abstract class UnionJudgement {
	public abstract boolean isUnion(String sql); 
	
	/**
	 * 缺省实现，使用内置解析器（较慢）
	 */
	public static final UnionJudgement DEFAULT=new UnionJudgement(){
		@Override
		public boolean isUnion(String sql) {
			try {
				Select select = DbUtils.parseNativeSelect(sql);
				return (select.getSelectBody() instanceof Union);  
			} catch (ParseException e) {
				LogUtil.warn("Druid parser Error:", e);
				return false;
			}
		}
	};
	
	/**
	 * Is Druid available?
	 * @return true if druid parser is available.
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
