package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.operators.arithmetic.Addition;
import jef.database.jsqlparser.visitor.Expression;

public class EmuPostgresAddDate extends BaseArgumentSqlFunction{
	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String getName() {
		return "adddate";
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression adjust=arguments.get(1);
		if(adjust instanceof Interval){
			Interval interval=(Interval)adjust;
			interval.toPostgresMode();
		}
		return new Addition(arguments.get(0), adjust);
	}
}
