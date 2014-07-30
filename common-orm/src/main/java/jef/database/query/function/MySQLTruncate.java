package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;

public class MySQLTruncate extends BaseArgumentSqlFunction{

	public String getName() {
		return "trunc";
	}

	public Expression renderExpression(List<Expression> arguments) {
		if(arguments.size()==1){
			return new Function("truncate",arguments.get(0),LongValue.L0);
		}else{
			return new Function("truncate",arguments);
		}
	}
	
	

}
