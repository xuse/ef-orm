package jef.database.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CountSqlResult implements SqlResult{
	private Map<String,List<BindSql>> sqls=new LinkedHashMap<String,List<BindSql>>();
	
	public void addSql(String dbName,String... sql){
		List<BindSql> list=sqls.get(dbName);
		if(list==null){
			list=new ArrayList<BindSql>();
			sqls.put(dbName, list);
		}
		for(String s:sql){
			list.add(new BindSql(s));
		}
	}
	
	
	public void addSql(String dbName,List<BindSql> value){
		List<BindSql> list=sqls.get(dbName);
		if(list==null){
			list=new ArrayList<BindSql>();
			sqls.put(dbName, list);
		}
		list.addAll(value);
	}
	
	public void addSql(String dbName,BindSql... sqls){
		addSql(dbName,Arrays.asList(sqls));
	}

	public Map<String,List<BindSql>> getSqls() {
		return sqls;
	}
}
