package jef.database.query.function;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jef.database.jsqlparser.expression.CaseExpression;
import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.WhenClause;


/**
 * 用 case expr1
 *  when expr2 then expr3
 *  when ... then.
 *  else ...
 *  end 来模拟oracle的decode函数
 * @author jiyi
 *
 */
public class EmuDecodeWithCase extends BaseArgumentSqlFunction{
	public String getName() {
		return "decode";
	}

	public Expression renderExpression(List<Expression> arguments) {
		LinkedList<Expression> copy=new LinkedList<Expression>(arguments);
		CaseExpression result=new CaseExpression();
		result.setSwitchExpression(copy.removeFirst());
		
		List<WhenClause> whens=new ArrayList<WhenClause>();
		while(copy.size()>1){
			WhenClause when=new WhenClause();
			when.setWhenExpression(copy.removeFirst());
			when.setThenExpression(copy.removeFirst());
			whens.add(when);
		}
		result.setWhenClauses(whens);
		if(!copy.isEmpty()){
			result.setElseExpression(copy.removeFirst());
		}
		return result;
	}
//	public static void main(String[] args) throws ParseException {
//		Function func=(Function)DbUtils.parseExpression("decode('abc','a',1,'b',2,'abc1',3)");
//		EmuDecodeWithCase tt=new EmuDecodeWithCase();
//		Expression result=tt.renderExpression(func.getParameters().getExpressions());
//		System.out.println(result);
//	}
}
