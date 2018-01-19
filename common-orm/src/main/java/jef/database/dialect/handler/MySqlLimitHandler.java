package jef.database.dialect.handler;

import jef.database.jdbc.statement.UnionJudgement;
import jef.database.jdbc.statement.UnionJudgementDruidMySQLImpl;
import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;
import jef.tools.StringUtils;

public class MySqlLimitHandler implements LimitHandler {
	private final static String MYSQL_PAGE = " limit %start%,%next%";
	private UnionJudgement unionJudge;

	public MySqlLimitHandler() {
		if(UnionJudgement.isDruid()){
			unionJudge=new UnionJudgementDruidMySQLImpl();
		}else{
			unionJudge=UnionJudgement.DEFAULT;
		}
	}

	public BindSql toPageSQL(String sql, PageLimit range) {
		return toPageSQL(sql, range,unionJudge.isUnion(sql));
	}

	@Override
	public BindSql toPageSQL(String sql, PageLimit range, boolean isUnion) {
		String[] s = new String[] { Long.toString(range.getOffset()), Integer.toString(range.getLimit()) };
		String limit = StringUtils.replaceEach(MYSQL_PAGE, new String[] { "%start%", "%next%" }, s);
		return new BindSql(isUnion ? StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit));
	}
}
