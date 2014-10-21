package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlSelectParser;

public class UnionJudgementDruidMySQLImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		MySqlSelectParser parser=new MySqlSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
