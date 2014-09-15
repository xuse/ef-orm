package jef.database.routing.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import jef.common.IntList;
import jef.database.DbUtils;
import jef.database.rowset.CachedRowSetImpl;

public class BatchReturn extends UpdateReturn{
	private final IntList batchResult=new IntList();
	
	public BatchReturn(int[] batchCount) {
		super(-2);
		batchResult.addAll(batchCount);
	}
	public BatchReturn(){
		super(-2);
	}

	@Override
	public boolean isBatch() {
		return true;
	}

	public int[] getBatchResult() {
		return batchResult.toArray();
	}
	
	public boolean isSuccessNoInfo(){
		return batchResult.size()==1 && batchResult.get(0)==-2;
	}
	
	public boolean isExecuteFail(){
		return batchResult.size()==1 && batchResult.get(0)==-3;
	}

	public void merge(int[] batchResult2, ResultSet generatedKeys2) throws SQLException {
		if(batchResult.isEmpty()){
			batchResult.addAll(batchResult2);
		}else if(isSuccessNoInfo()){
			if(batchResult2.length==1 && batchResult2[0]==-3){
				batchResult.set(0, -3);
			}
		}else if(isExecuteFail()){
		}else{
			batchResult.addAll(batchResult2);
		}
		if(generatedKeys2!=null){
			if(this.generatedKeys==null){
				this.generatedKeys=new CachedRowSetImpl();
			}
			generatedKeys.populate(generatedKeys2);
			DbUtils.close(generatedKeys2);
		}
	}
}
