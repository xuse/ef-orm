package jef.database.jdbc.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerSelectParser;

/**
 * 使用Druid的SQL解析器判断是否为Union语句(SQLServer语法专用)
 * @author jiyi
 *
 */
public final class UnionJudgementDruidSSImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		SQLServerSelectParser parser=new SQLServerSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
