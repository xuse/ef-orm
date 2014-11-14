package jef.database.wrapper.clause;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.wrapper.IntRange;
import jef.database.jdbc.rowset.CachedRowSetImpl;
import jef.database.jdbc.rowset.Row;

/**
 * 在内存中实现结果集分页
 * @author jiyi
 *
 */
public class InMemoryPaging implements InMemoryProcessor{
	private int start;
	private int end;
	
	public InMemoryPaging(int start,int end){
		this.start=start;
		this.end=end;
	}
	
	public InMemoryPaging(IntRange range) {
		int[] data=range.toStartLimitSpan();
		this.start=data[0];
		this.end=data[1]+start;
	}

	public void process(CachedRowSetImpl rows) throws SQLException {
		List<Row> list=rows.getRvh();
		if(start==0 && end>=list.size()){//不需要截取的场合
			return;
		}
		int end=this.end;
		if(end>list.size()){ //防止溢出
			end=list.size();
		}
		if(end<=start || start>=rows.size()){//防止空结果
			rows.setRvh(new ArrayList<Row>());
		}else{
			rows.setRvh(list.subList(start, end));
		}
		rows.refresh();	
	}

	public String getName() {
		return "PAGING";
	}

	
}
