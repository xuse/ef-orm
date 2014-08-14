package jef.database.query;

import jef.database.Condition.Operator;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.IReferenceColumn;
import jef.database.wrapper.clause.HavingEle;

public abstract class SingleColumnSelect implements IReferenceColumn{
	protected int projection;
	protected Operator havingCondOperator;//当为Having字句时的操作符
	protected Object havingCondValue;  //当为Having字句时的比较值
	
	public abstract HavingEle toHavingClause(DatabaseDialect profile,String tableAlias,SqlContext context);

	public final int getProjection() {
		return projection;
	}
}
