package jef.database.dialect;

import jef.database.jdbc.statement.UnionJudgement;
import jef.database.jdbc.statement.UnionJudgementDruidPGImpl;
import jef.database.wrapper.clause.BindSql;
import jef.tools.StringUtils;

public class LimitOffsetLimitHandler implements LimitHandler {
	protected static final String PG_PAGE = " limit %next% offset %start%";
	private UnionJudgement unionJudge;
	
	public LimitOffsetLimitHandler(){
		if(UnionJudgement.isDruid()){
			unionJudge=new UnionJudgementDruidPGImpl();
		}else{
			unionJudge=UnionJudgement.DEFAULT;
		}
	}
	
	public BindSql toPageSQL(String sql, int[] range) {
		return toPageSQL(sql, range,unionJudge.isUnion(sql));

	}

	public BindSql toPageSQL(String sql, int[] range, boolean isUnion) {
		String[] s=new String[]{Integer.toString(range[0]),Integer.toString(range[1])};
		String limit = StringUtils.replaceEach(PG_PAGE, new String[] { "%start%", "%next%" }, s);
		return new BindSql(isUnion ? StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit));
	}
}
