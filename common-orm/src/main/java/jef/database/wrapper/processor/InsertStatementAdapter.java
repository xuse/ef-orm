package jef.database.wrapper.processor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InsertStatementAdapter implements InsertStep{
	private final List<InsertStep> calls= new ArrayList<InsertStep>(3);

	@Override
	public void callBefore(List<?> data) throws SQLException {
		for(InsertStep i:calls){
			i.callBefore(data);
		}
		
	}

	@Override
	public void callAfterBatch(List<?> data) throws SQLException {
		for(InsertStep i:calls){
			i.callAfterBatch(data);
		}
	}

	@Override
	public void callAfter(Object data) throws SQLException {
		for(InsertStep i:calls){
			i.callAfter(data);
		}
	}
}
