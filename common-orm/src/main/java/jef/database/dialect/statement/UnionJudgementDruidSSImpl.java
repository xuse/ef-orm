package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.postgresql.parser.PGSelectParser;

public class UnionJudgementDruidSSImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		PGSelectParser parser=new PGSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
