package jef.database.jdbc.statement;

public class ResultSetLaterProcess {
	private long skipResults;

	public ResultSetLaterProcess(long i) {
		this.skipResults = i;
	}

	public long getSkipResults() {
		return skipResults;
	}
}
