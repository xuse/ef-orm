package jef.database.jsqlparser;

import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.StartWithExpression;

public class RemovedDelayProcess {
	public StartWithExpression startWith;
	public Limit limit;
	public RemovedDelayProcess(Limit delayLimit, StartWithExpression delayStartWith) {
		// TODO Auto-generated constructor stub
	}
	public boolean isValid() {
		return startWith!=null || limit!=null;
	}
}
