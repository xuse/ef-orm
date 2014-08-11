package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.TemplateExpression;
import jef.database.jsqlparser.visitor.Expression;

/**
 * 支持用模板来表示函数的改写
 * @author jiyi
 *
 */
public class TemplateFunction extends BaseArgumentSqlFunction{
	private String template;
	private String name;
	public String getName() {
		return name;
	}
	
	public TemplateFunction(String name,String template){
		this.name=name;
		this.template=template;
	}

	public Expression renderExpression(List<Expression> arguments) {
		return new TemplateExpression(template, arguments.toArray(new Expression[arguments.size()]));
	}
}
