package jef.database.jsqlparser.statement.select;

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

public class StartWithExpression implements Expression{
	private Expression startExpression;
	private Expression connectExpression;
	
	public StartWithExpression(Expression stExpression,Expression connectBy){
		this.startExpression=stExpression;
		this.connectExpression=connectBy;
	}
	
	public void accept(ExpressionVisitor expressionVisitor) {
		expressionVisitor.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(128);
		appendTo(sb);
		return sb.toString();
	}

	public void appendTo(StringBuilder sb) {
		if(startExpression==null)return;
		sb.append(" START WITH ");
		startExpression.appendTo(sb);
		if(connectExpression!=null){
			sb.append(" CONNECT BY ");
			connectExpression.appendTo(sb);
		}
	}
	
	public Expression getStartExpression() {
		return startExpression;
	}

	public Expression getConnectExpression() {
		return connectExpression;
	}

	public void setStartExpression(Expression startExpression) {
		this.startExpression = startExpression;
	}

	public void setConnectExpression(Expression connectExpression) {
		this.connectExpression = connectExpression;
	}

	public ExpressionType getType() {
		return ExpressionType.complex;
	}

}
