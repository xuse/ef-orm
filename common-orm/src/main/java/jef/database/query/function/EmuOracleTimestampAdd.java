package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.operators.arithmetic.Addition;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.jsqlparser.visitor.Expression;

/**
 * Oracle驱动并没有实现JDBC函数 timestampadd和timestampdiff，因此需要人工模拟。
 * 这个类用oracle运算和函数来模拟timestampadd操作
 * @author jiyi
 *
 */
public class EmuOracleTimestampAdd extends BaseArgumentSqlFunction{
	public String getName() {
		return "timestampadd";
	}

	public Expression renderExpression(List<Expression> arguments) {
		//第一个参数为COlumn表示单位
		//第二个参数为偏差
		//第三个参数为时间
		String unit=arguments.get(0).toString().toLowerCase();
		if(unit.startsWith("sql_tsi_")){
			unit=unit.substring(8);
		}
		Expression adjust=arguments.get(1);
		Expression timeValue=arguments.get(2);
		if(timeValue instanceof StringValue){
			timeValue=EmuOracleToDate.getInstance().convert((StringValue)timeValue);
		}
		Expression result;
		if("day".equals(unit)){
			result=new Addition(timeValue, adjust);
		}else if("hour".equals(unit)){
			adjust=new Division(adjust,new LongValue(24));
			adjust=new Parenthesis(adjust);
			result=new Addition(timeValue, adjust);
		}else if("minute".equals(unit)){
			adjust=new Division(adjust,new LongValue(1440));
			adjust=new Parenthesis(adjust);
			result=new Addition(timeValue, adjust);
		}else if("second".equals(unit)){
			adjust=new Division(adjust,new LongValue(86400));
			adjust=new Parenthesis(adjust);
			result=new Addition(timeValue, adjust);
		}else if("month".equals(unit)){
			result=new Function("add_months",timeValue,adjust);
		}else if("quarter".equals(unit)){
			Expression right=new Multiplication(adjust,new LongValue(3));
			result=new Function("add_months",timeValue,right);
		}else if("year".equals(unit)){
			Expression right=new Multiplication(adjust,new LongValue(12));
			result=new Function("add_months",timeValue,right);
		}else{
			throw new UnsupportedOperationException("The Oracle Dialect can't handle datetime unit ["+unit+"] for now.");
		}
		return result;
	}
}
