package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.postgresql.parser.PGSelectParser;
import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerSelectParser;

public class UnionJudgementDruidPGImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		SQLServerSelectParser parser=new SQLServerSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
