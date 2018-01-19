package jef.database.dialect.handler;

import jef.database.jdbc.statement.UnionJudgement;
import jef.database.jdbc.statement.UnionJudgementDruidPGImpl;
import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;
import jef.tools.StringUtils;

public class LimitOffsetLimitHandler implements LimitHandler {
	private UnionJudgement unionJudge;

	public LimitOffsetLimitHandler() {
		if (UnionJudgement.isDruid()) {
			unionJudge = new UnionJudgementDruidPGImpl();
		} else {
			unionJudge = UnionJudgement.DEFAULT;
		}
	}

	public BindSql toPageSQL(String sql, PageLimit range) {
		return toPageSQL(sql, range, unionJudge.isUnion(sql));

	}

	public BindSql toPageSQL(String sql, PageLimit range, boolean isUnion) {
		String limit;
		if (range.getOffset() == 0) {
			limit = " limit " + range.getLimit();
		} else {
			limit = " limit " + range.getLimit() + " offset " + range.getOffset();
		}
		return new BindSql(isUnion ? StringUtils.concat("select * from (", sql, ") tb__", limit) : sql.concat(limit));
	}
}
