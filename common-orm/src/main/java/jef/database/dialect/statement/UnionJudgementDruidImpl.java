package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.parser.SQLSelectParser;

/**
 * 使用Druid的SQL解析器判断是否为Union语句
 * @author jiyi
 *
 */
public final class UnionJudgementDruidImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		SQLSelectParser parser=new SQLSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
