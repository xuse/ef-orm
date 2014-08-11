package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.TemplateExpression;
import jef.database.jsqlparser.visitor.Expression;

public class EmuLRpadOnSqlite extends BaseArgumentSqlFunction {

	boolean isLeft;

	private final String lpad = "replace(substr(hex(zeroblob(%2$s-length(%1$s))),1,%2$s-length('a')),'0',%3$s)||%1$s";
	private final String rpad = "%1$s||replace(substr(hex(zeroblob(%2$s-length(%1$s))),1,%2$s-length('a')),'0',%3$s)";

	public EmuLRpadOnSqlite(boolean isLpad) {
		this.isLeft = isLpad;
	}

	public String getName() {
		return isLeft ? "lpad" : "rpad";
	}

	public Expression renderExpression(List<Expression> arguments) {
		if (arguments.size() == 3) {
			/*
			 * 三参数的pad功能不支持，要用复杂的手段进行。
			 */
			return new TemplateExpression(isLeft ? lpad : rpad, arguments.toArray(new Expression[arguments.size()]));
		} else {
			/*
			 * 双参数情况下,换一个函数名称即可.
			 */
			return new Function(isLeft ? "padl" : "padr", arguments);
		}
	}

}
