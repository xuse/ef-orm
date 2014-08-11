package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.visitor.Expression;

public class EmuOracleCast extends BaseArgumentSqlFunction {
	public String getName() {
		return "cast";
	}

	public Expression renderExpression(List<Expression> arguments) {
		assertParam(arguments,2);
		Expression value=arguments.get(0);
		String to=arguments.get(1).toString().toLowerCase();
		if("char".equals(to) || "varchar".equals(to)){
			return new Function("to_char",value);
		}else if(to.startsWith("int")|| "number".equals(to)){
			return new Function("to_number",value);
		}else if("date".equals(to) || to.startsWith("time")){
			return new Function("to_date",value);
		}
		throw new IllegalArgumentException("Unknown data type cast to "+ to);
	}
}
