package jef.database.query.function;

import java.util.LinkedList;
import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.NullValue;
import jef.database.jsqlparser.expression.operators.relational.EqualsTo;

/**
 * 用 if(expr1, expr2, expr3)的样式来模拟Oracle的decode函数。
 * 该函数只在MySQL中支持
 * @author jiyi
 *
 */
public final class EmuDecodeWithIf extends BaseArgumentSqlFunction{

	public String getName() {
		return "decode";
	}

	public Expression renderExpression(List<Expression> arguments) {
		LinkedList<Expression> copy=new LinkedList<Expression>(arguments);
		Expression root=copy.removeFirst();
		Expression ifFunc=wrapWithIf(root,copy);
		return ifFunc;
	}

	private Expression wrapWithIf(Expression root, LinkedList<Expression> copy) {
		if(copy.size()==1){
			return copy.removeFirst();
		}else if(copy.isEmpty()){
			return NullValue.getInstance();
		}
		return new Function("if",toEqual(root,copy.removeFirst()),copy.removeFirst(),wrapWithIf(root,copy));
	}
	
	private Expression toEqual(Expression left, Expression right) {
		return new EqualsTo(left,right);
	}
}
