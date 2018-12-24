package jef.database.wrapper.processor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InsertWrapper implements StatementPreparer, InsertStep {
	private static final StatementPreparer P = new StatementPreparer() {
		public PreparedStatement doPrepareStatement(Connection conn, String sql) throws SQLException {
			return conn.prepareStatement(sql);
		}
	};

	private StatementPreparer preparer = P;

	@SuppressWarnings("unchecked")
	private List<InsertStep> processors = Collections.EMPTY_LIST;

	@Override
	public void callBefore(List<?> data) throws SQLException {
		for (InsertStep processor : processors) {
			processor.callBefore(data);
		}
	}

	@Override
	public void callAfterBatch(List<?> data) throws SQLException {
		for (InsertStep processor : processors) {
			processor.callAfterBatch(data);
		}
	}

	@Override
	public void callAfter(Object data) throws SQLException {
		for (InsertStep processor : processors) {
			processor.callAfter(data);
		}
	}

	@Override
	public PreparedStatement doPrepareStatement(Connection conn, String sql) throws SQLException {
		return preparer.doPrepareStatement(conn, sql);
	}

	public void addProcessor(InsertStep processor) {
		if (processors.isEmpty()) {
			processors = new ArrayList<InsertStep>();
		}
		processors.add(processor);
		if (processor instanceof StatementPreparer) {
			if(this.preparer!=P){
				throw new UnsupportedOperationException("两项特性均需要调整 conn.prepareStatement()的参数，无法同时满足。");
			}
			this.preparer = (StatementPreparer) processor;
		}
	}

}
