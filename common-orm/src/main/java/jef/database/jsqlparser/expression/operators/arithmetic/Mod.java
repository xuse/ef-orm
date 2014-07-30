package jef.database.jsqlparser.expression.operators.arithmetic;

import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.ExpressionVisitor;

/**
 * 求余数
 * @author jiyi
 *
 */
public class Mod  extends BinaryExpression {

	public Mod(){
	}
	public Mod(Expression left,Expression right){
		this.setLeftExpression(left);
		this.setRightExpression(right);
	}
	
    public void accept(ExpressionVisitor expressionVisitor) {
    	if(rewrite==null){
    		expressionVisitor.visit(this);
    	}else{
    		rewrite.accept(expressionVisitor);
    	}
    }

    public String getStringExpression() {
        return "%";
    }
}
