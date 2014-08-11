package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.TemplateExpression;
import jef.database.jsqlparser.visitor.Expression;

public class EmuOracleCastTimestamp extends BaseArgumentSqlFunction{
	static EmuOracleCastTimestamp instance=new EmuOracleCastTimestamp();
	public static EmuOracleCastTimestamp getInstance(){
		return instance;
	}
	
	private EmuOracleCastTimestamp(){
	}
	public String getName() {
		return "timestamp";
	}

	public Expression renderExpression(List<Expression> arguments) {
		return convert(arguments.get(0));
	}
	
	public Expression convert(Expression ex){
		if(ex instanceof Function){
			String name=((Function) ex).getName();
			if("sysdate".equals(name)){
				ex=new Function("systimestamp");
			}else if("systimestamp".equals(name)){
			}
		}else{
			ex=new TemplateExpression("cast(%s as timestamp)",ex);
		}
		return ex;
	}

}
