package jef.database.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CountSqlResult implements SqlResult{
	private Map<String,List<BindSql>> sqls;
	private String need;
	
	public void addSql(String dbName,String... sql){
		if(sqls==null){
			sqls=new LinkedHashMap<String,List<BindSql>>();
		}
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
		if(sqls==null){
			sqls=new LinkedHashMap<String,List<BindSql>>();
		}
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

//	/**
//	 * 获得SQL语句，忽略绑定变量。（如果SQL是绑定变量的，那么抛出异常）
//	 * @return
//	 */
//	public String[] getAsNoBindSql() {
//		String[] result=new String[sqls.size()];
//		int n=0;
//		for(BindSql sql:sqls){
//			if(sql.isBind()){
//				throw new IllegalStateException("Try to get sql without bind variables.");
//			}
//			result[n++]=sql.getSql();
//		}
//		return result;
//	}
}
