package jef.database.jsqlparser.expression.operators.arithmetic;

import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

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
	
	@Override
	protected void acceptExp(ExpressionVisitor expressionVisitor) {
		expressionVisitor.visit(this);
	}

    public String getStringExpression() {
        return "%";
    }
    
	public ExpressionType getType0() {
		return ExpressionType.arithmetic;
	}
}
