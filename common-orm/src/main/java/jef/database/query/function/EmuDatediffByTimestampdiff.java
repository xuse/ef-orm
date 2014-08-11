package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.support.SQL_TSI;

/**
 * 在derby上用timestampdiff来模拟datediff参数
 * 
 * 注意：
 * 1、 datediff是按DAY为单位的
 * 2、 datediff是  arg1-arg2，而timestampdiff是 arg2-arg1，两者刚好相反
 * 
 * @author jiyi
 *
 */
public class EmuDatediffByTimestampdiff extends BaseArgumentSqlFunction{
	public String getName() {
		return "datediff";
	}

	public Expression renderExpression(List<Expression> arguments) {
		Function func=new Function("timestampdiff",SQL_TSI.DAY.get(),arguments.get(1),arguments.get(0));
		func.setEscaped(true);
		return func;
	}
}
