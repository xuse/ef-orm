package jef.database.query;

import java.util.List;

import jef.database.Condition.Operator;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.IReferenceColumn;
import jef.database.wrapper.clause.HavingEle;

public abstract class SingleColumnSelect implements IReferenceColumn {
	protected int projection;
	protected Operator havingCondOperator;// 当为Having字句时的操作符
	protected Object havingCondValue; // 当为Having字句时的比较值

	public abstract HavingEle toHavingClause(DatabaseDialect dialect, String tableAlias, SqlContext context);

	public final int getProjection() {
		return projection;
	}

	protected String applyProjection(String name, List<DbFunctionCall> functionDef,DatabaseDialect dialect) {
		if ((projection & PROJECTION_CUST_FUNC) > 0) {
			String start = name;
			for (DbFunctionCall func : functionDef) {
				start = func.apply(start, dialect);
			}
			name = start;
		}

		int key = projection & 0xFF;
		if (key > 0) {
			// if(StringUtils.isEmpty(alias)){//强行取Alias(无必要)
			// alias="C".concat(RandomStringUtils.randomAlphanumeric(12));
			// }
			switch (key) {
			case PROJECTION_COUNT:
				return "count(".concat(name).concat(")");
			case PROJECTION_COUNT_DISTINCT:
				return "count(distinct ".concat(name).concat(")");
			case PROJECTION_SUM:
				return "sum(".concat(name).concat(")");
			case PROJECTION_AVG:
				return "avg(".concat(name).concat(")");
			case PROJECTION_MAX:
				return "max(".concat(name).concat(")");
			case PROJECTION_MIN:
				return "min(".concat(name).concat(")");
			default:
				throw new IllegalArgumentException("Unknown projection " + key);
			}
		} else {
			return name;
		}
	}
}
