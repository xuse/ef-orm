package jef.database.wrapper.clause;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jef.database.rowset.CachedRowSetImpl;
import jef.database.rowset.Row;

public class InMemoryDistinct implements InMemoryProcessor{
	public static InMemoryDistinct instance=new InMemoryDistinct();
	
	private InMemoryDistinct(){
	}
	
	public void process(CachedRowSetImpl rows) throws SQLException {
		Set<Row> newRows=new LinkedHashSet<Row>();
		for(Row row: rows.getRvh()){
			newRows.add(row);
		}
		rows.setRvh(null);//先释放掉
		List<Row> result=Arrays.asList(newRows.toArray(new Row[newRows.size()]));
		rows.setRvh(result);
		rows.refresh();
	}
}
