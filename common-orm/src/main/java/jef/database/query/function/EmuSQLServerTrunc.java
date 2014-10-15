package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.query.SqlExpression;
import jef.tools.StringUtils;

public class EmuSQLServerTrunc extends BaseArgumentSqlFunction{
	private SqlExpression INTEGER=new SqlExpression("integer");
	
	private SqlExpression FLOAT=new SqlExpression("float");
	
	@Override
	public String getName() {
		return "trunc";
	}

	@Override
	public Expression renderExpression(List<Expression> arguments) {
		Expression input=arguments.get(0);
		Expression digital=null;
		if(arguments.size()>1){
			digital=arguments.get(1);
		}else{
			digital=LongValue.L0;
		}
		int numeric=-1;
		String numricStr=digital.toString();//截断后保留的位数
		if(StringUtils.isNumeric(numricStr)){
			numeric=Integer.parseInt(numricStr);
		}
		Function roundFunc=new Function("round",input,digital,LongValue.L1);
		if(numeric==0){
			return new Function("convert",INTEGER, roundFunc);			
		}else{
			return new Function("convert",FLOAT, roundFunc);
		}
	}

}

