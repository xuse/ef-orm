package jef.database.jsqlparser;

import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.StartWithExpression;

public class RemovedDelayProcess {
	public StartWithExpression startWith;
	public Limit limit;
	public boolean isValid() {
		return startWith!=null || limit!=null;
	}
}
