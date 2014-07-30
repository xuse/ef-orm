package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Subtraction;


/**
 * 后来看了一下，timestampdiff和timestampadd是JDBC函数，貌似postgres的驱动已经实现了这个两个函数。
 * 
 * 但是我这边做不到的，Postgres的JDBC驱动一样做不到，该驱动会抛出异常。
 * 
 * 
 * 需要存储过程……
 * create or replace function get_months(interval , OUT result Integer) AS $$
      select cast(extract(year from $1)*12+extract(month from $1) as Integer) as result
    $$ language SQL;

 * 
    
 * @author jiyi
 *
 */
public class EmuPostgreTimestampDiff extends BaseArgumentSqlFunction{
	public String getName() {
		return "timestampdiff";
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression arg1=arguments.get(1);
		Expression arg2=arguments.get(2);
		
		String unit=arguments.get(0).toString().toLowerCase();
		if(unit.startsWith("sql_tsi_")){
			unit=unit.substring(8);
		}
		Expression subtraction;
		if("day".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Function("date_part",new StringValue("day",false),subtraction);
		}else if("hour".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Function("date_part",new StringValue("epoch",false),subtraction);//转为秒数
			subtraction=new Division(new Parenthesis(subtraction),new LongValue(3600));      //除以3600转为小时数
			subtraction=new Function("trunc",subtraction);									//截断
		}else if("minute".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Function("date_part",new StringValue("epoch",false),subtraction);//转为秒数
			subtraction=new Division(new Parenthesis(subtraction),new LongValue(60));      //除以60转为分钟数
			subtraction=new Function("trunc",subtraction);									//截断
		}else if("second".equals(unit)){
			subtraction=new Subtraction(arg2,arg1);
			subtraction=new Function("date_part",new StringValue("epoch",false),subtraction);//转为秒数
			subtraction=new Function("trunc",subtraction);									//截断
		}else if("month".equals(unit)){
			//FIXME 
			throw new UnsupportedOperationException("Since there's no function to convert interval to months, this function can't be supported untill we create a procedure in database");
			//用存储过程是能支持的，先不做
//			subtraction=new Function("age",arg2,arg1);
//			subtraction=new Function("date_part",new StringValue("month",false),subtraction);//
		}else if("quarter".equals(unit)){
			//FIXME 
			//用存储过程是能支持的，先不做
			throw new UnsupportedOperationException("Since there's no function to convert interval to months, this function can't be supported untill we create a procedure in database");
//			subtraction=new Function("age",arg2,arg1);
//			subtraction=new Function("date_part",new StringValue("quarter",false),subtraction);//转为秒数
		}else if("year".equals(unit)){
			subtraction=new Function("age",arg2,arg1);
			subtraction=new Function("date_part",new StringValue("year",false),subtraction);//无问题
		}else{
			throw new UnsupportedOperationException("The Oracle Dialect can't handle datetime unit ["+unit+"] for now.");
		}

		return subtraction;
	}

}
