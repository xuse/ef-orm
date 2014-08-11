package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.visitor.Expression;

public class EmuDerbyCast extends BaseArgumentSqlFunction {

	public String getName() {
		return "cast";
	}

	public Expression renderExpression(List<Expression> arguments) {
		assertParam(arguments,2);
		String to=arguments.get(1).toString().toLowerCase();
		return new Function(to,arguments.get(0));
	}
}
