package jef.database.query.function;

import java.util.ArrayList;
import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.visitor.Expression;

/**
 * 可以直接将apache commons-lang中的一个函数定义为replace存储过程
 * CREATE FUNCTION REPLACE(STR VARCHAR(255), OLD VARCHAR(100), NEW VARCHAR(100)) RETURNS 
   VARCHAR(255) PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA EXTERNAL NAME 'org.apache.commons.lang.StringUtils.replace' 
 * @author jiyi
 */
public class EmuDerbyUserFunction extends BaseArgumentSqlFunction{
	private String name;
	private String localFuncName;
	
	private int mustParamCount;
	private Expression[] params;
	
	public void setPadParam(int count,Expression... param){
		this.mustParamCount=count;
		this.params=param;
	}
	

	public EmuDerbyUserFunction(String name,String nativeFuncName){
		this.name=name;
		this.localFuncName=nativeFuncName;
	}
	
	public String getName() {
		return name;
	}

	public Expression renderExpression(List<Expression> arguments) {
		if(mustParamCount>0 && arguments.size()<mustParamCount){
			ArrayList<Expression> newArgs=new ArrayList<Expression>(arguments);
			int left=mustParamCount-arguments.size();
			for(int i=0;i<left;i++){
				newArgs.add(params[i]);
			}
			return new Function(localFuncName,newArgs);
		}else{
			return new Function(localFuncName,arguments);
		}
	}

	@Override
	public String[] requiresUserFunction() {
		return new String[]{localFuncName.toUpperCase()};
	}
}
