package jef.database.jsqlparser.expression;

import jef.database.query.SqlExpression;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 这个是专门为了解析MYSQL日期时间操作中的INTERVAL关键字的
 * @author jiyi
 *
 */
public class Interval implements Expression,Child  {
	private Object parent; //可能是ExpressionList，也可能是Addition 或者 Subtraction等运算
	private String unit;
	private Expression value;
	private static final String[] MYSQL_ALL_DATEUNIT={"MICROSECOND","SECOND","MINUTE","HOUR","DAY","WEEK","MONTH","QUARTER","YEAR","SECOND_MICROSECOND", 
		"MINUTE_MICROSECOND","MINUTE_SECOND","HOUR_MICROSECOND","HOUR_SECOND","HOUR_MINUTE","DAY_MICROSECOND","DAY_SECOND","DAY_MINUTE","DAY_HOUR","YEAR_MONTH"};
		
	public Interval(){}
	
	/**
	 * postgres模式下，没有value，value和unit都被引号变为常量
	 * @return
	 */
	public boolean isPostgreMode(){
		return value==null && unit.charAt(0)=='\'';
	}
	
	public boolean isMySQLMode(){
		return ArrayUtils.contains(MYSQL_ALL_DATEUNIT, unit.toUpperCase());
	}
	
	public boolean toMySqlMode(){
		if(isPostgreMode()){
			unit=StringUtils.substringBetween(unit, "'", "'");
			int space=unit.indexOf(' ');
			if(space>-1){
				value=new SqlExpression(unit.substring(0,space));
				unit=unit.substring(space+1);
			}
			return true;
		}else{
			return isMySQLMode();
		}
	}
	
	public boolean toPostgresMode(){
		if(isMySQLMode()){
			unit="'"+value.toString()+" "+unit+"'";
			value=null;
			return true;
		}else{
			return isPostgreMode();
		}
	}
	
	
	
	public Interval(String valueAndUnit){
		this.unit=valueAndUnit;
	}
	
	public Interval(Expression value,String unit){
		this.value=value;
		this.unit=unit;
	}
	
	public void accept(ExpressionVisitor expressionVisitor) {
		expressionVisitor.visit(this);
	}
	
	public Object getParent() {
		return parent;
	}

	public void setParent(Object parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(64);
		appendTo(sb);
		return sb.toString();
	}
	
	public void appendTo(StringBuilder sb) {
		sb.append("INTERVAL ");
		if(value!=null){
			value.appendTo(sb);
			sb.append(' ');
		}
		sb.append(unit);
	}
	
	

	public String getUnit() {
		return unit;
	}

	public Expression getValue() {
		return value;
	}
	
	/**
MICROSECOND MICROSECONDS  毫秒
SECOND SECONDS            秒
MINUTE MINUTES            分
HOUR HOURS                时
DAY DAYS                  天
WEEK WEEKS                周
MONTH MONTHS              月
QUARTER QUARTERS          季
YEAR YEARS                年
SECOND_MICROSECOND 'SECONDS.MICROSECONDS' 
MINUTE_MICROSECOND 'MINUTES:SECONDS.MICROSECONDS' 
MINUTE_SECOND 'MINUTES:SECONDS' 
HOUR_MICROSECOND 'HOURS:MINUTES:SECONDS.MICROSECONDS' 
HOUR_SECOND 'HOURS:MINUTES:SECONDS' 
HOUR_MINUTE 'HOURS:MINUTES' 
DAY_MICROSECOND 'DAYS HOURS:MINUTES:SECONDS.MICROSECONDS' 
DAY_SECOND 'DAYS HOURS:MINUTES:SECONDS' 
DAY_MINUTE 'DAYS HOURS:MINUTES' 
DAY_HOUR 'DAYS HOURS' 
YEAR_MONTH 'YEARS-MONTHS' 

	 * @return
	 */
	public long toMills(){
		return 0;
	}
}
