package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.jsqlparser.expression.operators.arithmetic.Subtraction;
import jef.database.jsqlparser.visitor.Expression;

/**
 * Oracle驱动并没有实现JDBC函数 timestampadd和timestampdiff，因此需要人工模拟。
 * 这个类用oracle运算和函数来模拟timestampdiff操作
 * @author jiyi
 *
 */
public class EmuOracleTimeStampDiff extends BaseArgumentSqlFunction{
	public String getName() {
		return "timestampdiff";
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression arg1=arguments.get(1);
		if(arg1 instanceof StringValue){
			arg1=EmuOracleToDate.getInstance().convert((StringValue)arg1);
		}
		Expression arg2=arguments.get(2);
		if(arg2 instanceof StringValue){
			arg2=EmuOracleToDate.getInstance().convert((StringValue)arg2);
		}
		
		String unit=arguments.get(0).toString().toLowerCase();
		if(unit.startsWith("sql_tsi_")){
			unit=unit.substring(8);
		}
		Expression subtraction;
		if("day".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
		}else if("hour".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Multiplication(new Parenthesis(subtraction),new LongValue(24));
		}else if("minute".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Multiplication(new Parenthesis(subtraction),new LongValue(1440));
		}else if("second".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Multiplication(new Parenthesis(subtraction),new LongValue(86400));
		}else if("month".equals(unit)){
			subtraction=new Function("months_between",arg2,arg1);
		}else if("quarter".equals(unit)){
			subtraction=new Function("months_between",arg2,arg1);
			subtraction=new Division(subtraction,new LongValue(3));
		}else if("year".equals(unit)){
			subtraction=new Function("months_between",arg2,arg1);
			subtraction=new Division(subtraction,new LongValue(12));
		}else{
			throw new UnsupportedOperationException("The Oracle Dialect can't handle datetime unit ["+unit+"] for now.");
		}
		return new Function("trunc",subtraction);
	}

}
