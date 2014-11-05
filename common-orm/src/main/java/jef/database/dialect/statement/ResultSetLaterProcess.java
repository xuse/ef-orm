package jef.database.dialect.statement;

public class ResultSetLaterProcess {
	private int skipResults;

	public ResultSetLaterProcess(int i) {
		this.skipResults = i;
	}

	public int getSkipResults() {
		return skipResults;
	}
}
