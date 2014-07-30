package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.StringValue;

public class EmuTranslateByReplace extends BaseArgumentSqlFunction{
	public String getName() {
		return "translate";
	}

	public Expression renderExpression(List<Expression> arguments) {
		if(!(arguments.get(1) instanceof StringValue)){
			throw new IllegalArgumentException("Emulator of translate can only process literal search string.");
		}
		if(!(arguments.get(2) instanceof StringValue)){
			throw new IllegalArgumentException("Emulator of translate can only process literal replace string.");
		}
		
		String from=((StringValue)arguments.get(1)).getValue();
		String to=((StringValue)arguments.get(2)).getValue();
		Function f=null;
		for(int i=0;i<from.length();i++){
			String s=from.substring(i,i+1);
			String x=i<to.length()?to.substring(i,i+1):"";
			if(f==null){
				f=new Function("replace",arguments.get(0),new StringValue(String.valueOf(s),false),new StringValue(x,false));
			}else{
				f=new Function("replace",f,new StringValue(String.valueOf(s),false),new StringValue(x,false));
			}
		}
		return f;
	}
	

}
