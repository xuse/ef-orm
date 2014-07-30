package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.StringValue;

/**
 * 大多数数据库都可以直接将标准格式的字符串当作日期时间处理（隐式转换），Oracle需要用to_date模拟
 * @author jiyi
 *
 */
public class EmuOracleToDate extends BaseArgumentSqlFunction{
	static EmuOracleToDate instance=new EmuOracleToDate();
	public static EmuOracleToDate getInstance(){
		return instance;
	}
	private EmuOracleToDate(){}
	
	public String getName() {
		return "timestamp";
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression value=arguments.get(0);
		return convert(value);
	}
	public Expression convert(Expression value) {
		if((value instanceof StringValue) && ((StringValue)value).length()<=10){
			return new Function("to_date",value,new StringValue("yyyy-mm-dd", false));
		}else{
			return new Function("to_date",value,new StringValue("yyyy-mm-dd hh24:mi:ss", false));
		}
	}
	public Expression convert(StringValue value) {
		if(value.length()<=10){
			return new Function("to_date",value,new StringValue("yyyy-mm-dd", false));
		}else{
			return new Function("to_date",value,new StringValue("yyyy-mm-dd hh24:mi:ss", false));
		}
	}
}
