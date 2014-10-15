package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.query.SqlExpression;

public class EmuSQLServerTimestamp extends BaseArgumentSqlFunction {
	private String enumFunction;
	private String nativeFunction;
	
	public EmuSQLServerTimestamp(String emuFunction,String nativeFunction){
		this.enumFunction=emuFunction;
		this.nativeFunction=nativeFunction;
	}
	
	@Override
	public String getName() {
		return enumFunction;
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression datepart  = arguments.get(0);
		String unit=String.valueOf(datepart);
		if(unit.startsWith("tsi_")){
			datepart=new SqlExpression(unit.substring(4));
		}
		Expression number  = arguments.get(1);
		Expression timeExpr  = arguments.get(2);
		Function func = new Function(nativeFunction, datepart, number, timeExpr);
		return func;
	}

}
