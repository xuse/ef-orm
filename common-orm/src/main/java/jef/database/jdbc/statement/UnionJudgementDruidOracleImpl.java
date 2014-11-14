package jef.database.jdbc.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleSelectParser;

/**
 * 使用Druid的SQL解析器判断是否为Union语句(Oracle语法专用)
 * @author jiyi
 *
 */
public final class UnionJudgementDruidOracleImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		OracleSelectParser parser=new OracleSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
