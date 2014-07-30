package jef.database.support;

import jef.database.query.SqlExpression;

/**
 * 描述标准SQL函数timestampadd timestampdiff 中的辅助单位
 * @author jiyi
 *
 */
public enum SQL_TSI {
	FRAC_SECOND,
	SECOND,
	MINUTE,
	HOUR,
	DAY,
	WEEK,
	MONTH,
	QUARTER,
	YEAR;
	private SqlExpression exp;
	SQL_TSI(){
		this.exp=new SqlExpression("SQL_TSI_"+name());
	}
	public SqlExpression get(){
		return exp;
	}
}
