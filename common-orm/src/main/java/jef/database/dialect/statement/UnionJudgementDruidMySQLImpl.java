package jef.database.dialect.statement;

import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlSelectParser;

/**
 * 使用Druid的SQL解析器判断是否为Union语句(MySQL语法专用)
 * @author jiyi
 *
 */
public final class UnionJudgementDruidMySQLImpl extends UnionJudgement{
	
	@Override
	public boolean isUnion(String sql) {
		MySqlSelectParser parser=new MySqlSelectParser(sql);
		return parser.select().getQuery() instanceof SQLUnionQuery;
	}
}
