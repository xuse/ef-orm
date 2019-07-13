package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.NullValue;
import jef.database.jsqlparser.visitor.Expression;

import  org.apache.commons.lang3.ArrayUtils;

/**
 * 在函数的名称和参数顺序上稍作变化实现兼容的数据库函数渲染器
 * @author jiyi
 *
 */
public class TransformFunction implements SQLFunction {
	private String name;
	
	private String functionName;
	
	private String between; //
	
	private int[] paramIndex;
	
	private boolean escape;

	public TransformFunction(String name,String funname,int[] paramIndex,boolean escape){
		this.name=name;
		this.functionName=funname;
		this.paramIndex=paramIndex;
		this.escape=escape;
	}
	public TransformFunction(String name,String funname,int[] paramIndex){
		this(name,funname,paramIndex,false);
	}
	

	public TransformFunction setBetween(String between) {
		this.between = between;
		return this;
	}



	public boolean hasArguments() {
		return paramIndex.length>0;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String getName() {
		return name;
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression[] newarg=new Expression[paramIndex.length];
		for(int i=0;i<paramIndex.length;i++){
			int index=paramIndex[i];
			if(index<arguments.size()){
				newarg[i]=arguments.get(index);
			}else{
				newarg[i]=NullValue.getInstance();
			}
		}
		Function func=new Function(functionName,newarg);
		if(between!=null)func.getParameters().setBetween(between);
		if(escape)func.setEscaped(escape);
		return func;
	}
	public boolean needEscape() {
		return false;
	}
	public String[] requiresUserFunction() {
		return ArrayUtils.EMPTY_STRING_ARRAY;
	}
}
