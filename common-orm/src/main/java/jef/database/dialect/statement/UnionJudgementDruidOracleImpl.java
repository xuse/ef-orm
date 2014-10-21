package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleSelectParser;

public class UnionJudgementDruidOracleImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		OracleSelectParser parser=new OracleSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
