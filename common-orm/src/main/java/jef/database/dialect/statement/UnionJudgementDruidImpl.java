package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.parser.SQLSelectParser;

public class UnionJudgementDruidImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		SQLSelectParser parser=new SQLSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
