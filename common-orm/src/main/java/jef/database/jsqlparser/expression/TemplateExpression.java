package jef.database.jsqlparser.expression;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

/**
 * 支持用标准的String.format语法进行修饰的表达式。
 * 
 *  %[argument_index$][flags][width][.precision]conversion
 *  
 *  常见的如 % 1$ s
 *  或者 %s
 * 
 * @author jiyi
 *
 */
public class TemplateExpression implements Expression{
	private Expression[] exprs;
	private String template;
	
	public TemplateExpression(String format,Expression... exprs){
		this.template=format;
		this.exprs=exprs;
	}
	
	public void accept(ExpressionVisitor expressionVisitor) {
		for(Expression ex:exprs){
			if(ex!=null){
				ex.accept(expressionVisitor);
			}
		}
	}
	@Override
	public String toString() {
		return String.format(template, (Object[])exprs);
	}

	public void appendTo(StringBuilder sb) {
		sb.append(String.format(template, (Object[])exprs));
	}
	
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}
