package jef.database.query.function;

import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.InverseExpression;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.query.Func;
import jef.database.support.SQL_TSI;

/**
 * 
 * 举例、在derby上用timestampadd来模拟adddate subdate两个函数。
 * 原先的样式：
 * select adddate(dateExpr, interval n unit) from dual
 * 修改后
 * select {fn timestampadd(SQL_TSI_DAY,1,timestamp('2013-01-01 12:00:00'))} from dual;
 * 要点：1 必须用{fn }转义
 * 2 必须指定单位SQL_TSI_*
 * 3 偏差值单位，偏差值在中，时间值在后 （参数顺序变化）
 * 4 不会隐式将char转换为timestamp，需要手工转换
 * 
 * @author jiyi
 *
 */
public class EmuDateAddSubByTimesatmpadd extends BaseArgumentSqlFunction{
	private Func name;
	private boolean isSub;
	
	public EmuDateAddSubByTimesatmpadd(Func name){
		this.name=name;
		this.isSub=name==Func.subdate;
	}
	public String getName() {
		return name.name();
	}

	public Expression renderExpression(List<Expression> arguments) {
		Expression adjust=arguments.get(1);
		Expression unitExpr=SQL_TSI.DAY.get();
		if(adjust instanceof Interval){
			Interval interval=(Interval)adjust;
			interval.toMySqlMode();
			String unit=interval.getUnit().toLowerCase();
			Expression value=interval.getValue();
			
			if("day".equals(unit)){
				unitExpr=SQL_TSI.DAY.get();
			}else if("hour".equals(unit)){
				unitExpr=SQL_TSI.HOUR.get();
			}else if("minute".equals(unit)){
				unitExpr=SQL_TSI.MINUTE.get();
			}else if("second".equals(unit)){
				unitExpr=SQL_TSI.SECOND.get();
			}else if("month".equals(unit)){
				unitExpr=SQL_TSI.MONTH.get();
			}else if("quarter".equals(unit)){
				unitExpr=SQL_TSI.QUARTER.get();
			}else if("year".equals(unit)){
				unitExpr=SQL_TSI.YEAR.get();
			}else{
				throw new UnsupportedOperationException("The current Dialect can't handle datetime unit ["+unit+"] for now.");
			}
			adjust=isSub?InverseExpression.getInverse(value):value;
		}else{
			adjust=isSub?InverseExpression.getInverse(arguments.get(0)):arguments.get(0);
		}
		
		Expression dateExpr=arguments.get(0);
		if(dateExpr instanceof StringValue){
			dateExpr=new Function("timestamp",dateExpr);
		}
		Function func=new Function("timestampadd",unitExpr, adjust,dateExpr);
		func.setEscaped(true);
		return func;	
	}

}
