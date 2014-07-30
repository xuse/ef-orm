package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.operators.arithmetic.Subtraction;

/**
 * 截取oracle纯time
 * @author jiyi
 *
 */
public class EmuOracleTime extends BaseArgumentSqlFunction{

	public String getName() {
		return "time";
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression ex=arguments.get(0);
		ex=EmuOracleCastTimestamp.getInstance().convert(ex);
		Expression result=new Subtraction(ex,new Function("trunc",arguments.get(0)));
		result=new Parenthesis(result);
		return result;
	}
}
