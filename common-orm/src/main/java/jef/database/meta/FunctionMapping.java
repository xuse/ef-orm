package jef.database.meta;

import java.util.Collections;
import java.util.List;

import jef.database.DbFunction;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.query.function.NoArgSQLFunction;
import jef.database.query.function.SQLFunction;
import jef.tools.ArrayUtils;

/**
 * 描述一个数据库的函数与标准函数的匹配程度
 * @author jiyi
 *
 */
public class FunctionMapping {
	private DbFunction stardard;
	private SQLFunction function;
	private int match;
	
	
	/**
	 * 如果是内部定义的通用函数返回true
	 * @return return true if it is a standard function.
	 */
	public boolean isStandard(){
		return stardard!=null;
	}
	
	/**
	 * 返回参数个数。目前实际上此数据还未就绪，只能当无参数时返回0.
	 * @return return 0 if the function has no args.
	 */
	public int getArgCount(){
		if(function instanceof NoArgSQLFunction){
			return 0;
		}
		return 1;
	}
	
	public FunctionMapping(SQLFunction function, DbFunction stardard, int match) {
		this.function = function;
		this.stardard = stardard;
		this.match = match;
	}

	public SQLFunction getFunction() {
		return function;
	}

	public DbFunction getStardard() {
		return stardard;
	}

	/**
	 * 1、数据库的函数已经实现了所需要的标准函数。无需任何更改
	 * 2、数据库的函数和标准函数参数含义（基本）一样，仅需变化一下名称，如 nvl -> ifnull
	 * 3、数据库的函数和标准函数差别较大，通过多个其他函数模拟实现。（参数一致）
	 * @see #MATCH_FULL
	 * @see #MATCH_NAME_CHANGE
	 * @see #MATCH_EMULATION
	 * @return
	 */
	public int getMatch() {
		return match;
	}
//	public static final int MATCH_NONE=-1;
	/**
	 * 完全匹配，SQL不用做任何修改
	 */
	public static final int MATCH_FULL=0;
	/**
	 * 基本匹配，SQL的名称和 escape属性要修改
	 */
	public static final int MATCH_NAME_CHANGE=1; //
	/**
	 * 兼容实现，整个function要替换为新的对象
	 */
	public static final int MATCH_EMULATION=2;	//

	
	public Expression rewrite(String name,Expression... args) {
		if(getMatch()==FunctionMapping.MATCH_NAME_CHANGE){
			Function function=new Function(name,args);
			function.setName(getFunction().getName());//替换name
			function.setEscaped(getFunction().needEscape());							//替换名称后一般不需要转义
			return function;
		}else if(getMatch()==FunctionMapping.MATCH_EMULATION){					//整体替换
			SQLFunction sf=getFunction();
			return sf.renderExpression(ArrayUtils.asList(args));
		}else{
			Function function=new Function(name,args);
			function.setEscaped(getFunction().needEscape());
			return function;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void rewrite(Function function) {
		if(getMatch()==FunctionMapping.MATCH_NAME_CHANGE){
			function.setName(getFunction().getName());//替换name
			function.setEscaped(getFunction().needEscape());							//替换名称后一般不需要转义
		}else if(getMatch()==FunctionMapping.MATCH_EMULATION){					//整体替换
			List<Expression> exps;
			if(function.getParameters()!=null){
				exps=function.getParameters().getExpressions();
			}else{
				exps=Collections.EMPTY_LIST;
			}
			SQLFunction sf=getFunction();
			Expression s = sf.renderExpression(exps);
			function.rewrite=s;
		}else{
			function.setEscaped(getFunction().needEscape());
		}
	}

	public String[] requiresUserFunction() {
		return function.requiresUserFunction();
	}
}
