package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.InverseExpression;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.operators.arithmetic.Addition;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.jsqlparser.expression.operators.arithmetic.Subtraction;
import jef.database.jsqlparser.visitor.Expression;

/**
 * 在oracle上模拟datesub函数
 * @author jiyi
 *
 */
public class EmuOracleDateSub extends BaseArgumentSqlFunction{
	public String getName() {
		return "dateadd";
	}

	/*
	 * dataadd(xxx interval xxx unit)
	 * dataadd(xxx, xxx)
	 * @see jef.database.query.function.SQLFunction#renderExpression(java.util.List)
	 */
	public Expression renderExpression(List<Expression> arguments) {
		Expression adjust=arguments.get(1);
		Expression timeValue=arguments.get(0);
		if(timeValue instanceof StringValue){
			timeValue=EmuOracleToDate.getInstance().convert((StringValue)timeValue);
		}
		if(adjust instanceof Interval){
			Interval interval=(Interval)adjust;
			String unit=interval.getUnit().toLowerCase();
			interval.toMySqlMode();
			Expression value=interval.getValue();
			Expression add;
			if("day".equals(unit)){
				add=new Addition(timeValue, value);
			}else if("hour".equals(unit)){
				value=new Division(value,new LongValue(24));
				value=new Parenthesis(value);
				add=new Subtraction(timeValue, value);
			}else if("minute".equals(unit)){
				value=new Division(value,new LongValue(1440));
				value=new Parenthesis(value);
				add=new Subtraction(timeValue, value);
			}else if("second".equals(unit)){
				value=new Division(value,new LongValue(86400));
				value=new Parenthesis(value);
				add=new Subtraction(timeValue, value);
			}else if("month".equals(value)){
				add=new Function("add_months",timeValue,InverseExpression.getInverse(value));
			}else if("quarter".equals(adjust)){
				Expression right=new InverseExpression(new Multiplication(value,new LongValue(3)));
				add=new Function("add_months",timeValue,right);
			}else if("year".equals(value)){
				Expression right=new InverseExpression(new Multiplication(value,new LongValue(12)));
				add=new Function("add_months",timeValue,right);
			}else{
				throw new UnsupportedOperationException("The Oracle Dialect can't handle datetime unit "+unit+" for now.");
			}
			return add;
		}else{
			return new Subtraction(timeValue, arguments.get(1));
		}
	}
	
	

}
