package jef.database.dialect;

import jef.database.dialect.statement.LimitHandler;
import jef.database.dialect.statement.UnionJudgement;
import jef.database.dialect.statement.UnionJudgementDruidMySQLImpl;
import jef.database.wrapper.clause.BindSql;
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

	public BindSql toPageSQL(String sql, int[] range) {
		return toPageSQL(sql, range,unionJudge.isUnion(sql));
	}

	@Override
	public BindSql toPageSQL(String sql, int[] range, boolean isUnion) {
		String[] s = new String[] { Integer.toString(range[0]), Integer.toString(range[1]) };
		String limit = StringUtils.replaceEach(MYSQL_PAGE, new String[] { "%start%", "%next%" }, s);
		return new BindSql(isUnion ? StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit));
	}
}
