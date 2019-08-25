package jef.database.wrapper.clause;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import jef.database.Condition.Operator;
import jef.database.jdbc.rowset.CachedRowSetImpl;
import jef.database.jdbc.rowset.Row;
import jef.tools.StringUtils;

/**
 * 在内存中实现结果集的递归查询
 * 
 * 对于Oracle或Postgres的递归查询，在不支持的数据库上或者在分库分表后，只能通过人工在内存中连接来支持。
 * 
 *  start with connectBy如果去掉，可以理解是查出了整个树的所有节点。本方法的目的就是过滤掉那些挂不到树上去的节点。
 *  
 * @author jiyi
 * TODO implement it
 */
public class InMemoryStartWithConnectBy implements InMemoryProcessor{
	
	//要连接的当前ID
	public int connectPrior;
	//要连接的父ID
	public int connectParent;
	
	public int startWithColumn;
	public Operator startWithOperator;
	public Object startWithValue;
	
	public void process(CachedRowSetImpl rows) throws SQLException {
		Set<Row> result=new LinkedHashSet<Row>();
		Multimap<Object,Row> index=index(rows.getRvh(),result);
		List<Row> newComing=Arrays.asList(result.toArray(new Row[result.size()]));
		while(!newComing.isEmpty()){ //找不到新的孩子就结束循环
			newComing=appendResults(result,newComing,index);	
		}
		if(result.isEmpty()){
			rows.getRvh().clear();
			rows.refresh();
		}else{
			rows.setRvh(Arrays.asList(result.toArray(new Row[result.size()])));
			rows.refresh();
		}
	}
	
	private List<Row> appendResults(Set<Row> result, List<Row> toScan,Multimap<Object,Row> index) {
		List<Row> newComing=new ArrayList<Row>();
		for(Row row:toScan){
			Collection<Row> children=index.get(row.getColumnObject(connectPrior));
			for(Row child:children){
				if(result.add(child)){
					newComing.add(child);
				}
			}
		}
		return newComing;
	}

	//根据父ID索引
	private Multimap<Object, Row> index(List<Row> rvh,Set<Row> result) {
		Multimap<Object, Row> index=ArrayListMultimap.create();
		for(Row row:rvh){
			index.put(row.getColumnObject(connectParent), row);
			if(matchStart(row)){
				result.add(row);
			}
		}
		return index;	
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean matchStart(Row row) {
		Object obj=row.getColumnObject(startWithColumn);
		if(obj instanceof Integer){
			obj=Long.valueOf(((Integer) obj).longValue());
		}
		if(obj instanceof Float){
			obj=Double.valueOf(((Float) obj).doubleValue());
		}
		switch(startWithOperator){
		case EQUALS:
			return Objects.equal(obj, startWithValue);
		case GREAT:
			return ObjectUtils.compare((Comparable)obj, (Comparable)startWithValue)>0;
		case GREAT_EQUALS:
			return ObjectUtils.compare((Comparable)obj, (Comparable)startWithValue)>=0;
		case IS_NOT_NULL:
			return obj!=null;
		case IS_NULL:
			return obj==null;
		case LESS:
			return ObjectUtils.compare((Comparable)obj, (Comparable)startWithValue)<0; 
		case LESS_EQUALS:
			return ObjectUtils.compare((Comparable)obj, (Comparable)startWithValue)<=0;
		case MATCH_ANY:{
			//当startWithOperator是基于SQL时，要注意其实现
			String s1=StringUtils.toString(obj);
			String s2=String.valueOf(startWithValue);
			s2=StringUtils.replaceChars(s2, "%_", "*?");
			return StringUtils.matches(s1, s2, false);
		}
		case MATCH_END:{
			String s1=StringUtils.toString(obj);
			String s2=String.valueOf(startWithValue);
			return s1.endsWith(s2);
		}
		case MATCH_START:{
			String s1=StringUtils.toString(obj);
			String s2=String.valueOf(startWithValue);
			return s1.startsWith(s2);
		}
		case NOT_EQUALS:
			return !Objects.equal(obj, startWithValue);
		case NOT_IN:{
			List<Object> values=(List<Object>)startWithValue;
			return !values.contains(obj);
		}
		case IN:{
			List<Object> values=(List<Object>)startWithValue;
			return values.contains(obj);
		}
		case BETWEEN_L_L:{
			List<Object> values=(List<Object>)startWithValue;
			return ObjectUtils.compare((Comparable)obj, (Comparable)values.get(0))>=0 && ObjectUtils.compare((Comparable)obj, (Comparable)values.get(1))<=0;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	public String getName() {
		return "CONNECT_BY";
	}
	
	

}
