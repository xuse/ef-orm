package jef.database.wrapper.clause;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.database.rowset.CachedRowSetImpl;
import jef.database.rowset.Row;
import jef.tools.StringUtils;

import org.apache.commons.lang.ObjectUtils;

public class InMemoryGroupByHaving implements InMemoryProcessor {
	GroupByItem[] keys;
	GroupByItem[] values;
	private List<HavingEle> having;

	public InMemoryGroupByHaving(List<GroupByItem> keys, List<GroupByItem> values) {
		this.keys = keys.toArray(new GroupByItem[keys.size()]);
		this.values = values.toArray(new GroupByItem[values.size()]);
	}

	public void process(CachedRowSetImpl rowset) throws SQLException {
		int keyLen = keys.length;
		List<Row> rows=rowset.getRvh();
		List<Row> newRows = new ArrayList<Row>(rows.size()/2+1);
		Map<Collection<?>, RowTask> mapTask = new HashMap<Collection<?>, RowTask>();
		for (Row row : rows) {
			Object[] keyValue = new Object[keyLen];
			for (int i = 0; i < keyLen; i++) {
				keyValue[i] = row.getArrayObject(keys[i].getIndex());
			}
			List<Object> keyObj = Arrays.asList(keyValue);
			RowTask exist = mapTask.get(keyObj);
			if (exist == null) {
				newRows.add(row);
				mapTask.put(keyObj, new RowTask(row));
			} else {
				exist.merge(row);
			}
		}
		for (RowTask task : mapTask.values()) {
			task.run();
		}
		mapTask.clear();
		if(having!=null && !having.isEmpty()){
			doHaving(newRows);
		}
		rowset.setRvh(newRows);
		rowset.refresh();
	}

	private void doHaving(List<Row> newRows) {
		for(Iterator<Row> iter=newRows.iterator();iter.hasNext();){
			Row row=iter.next();
			if(!checkRow(row)){
				iter.remove();
			}
		}
	}

	private boolean checkRow(Row row) {
		for(HavingEle ele:having){
			if(!check(ele,row)){
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("rawtypes")
	private boolean check(HavingEle ele, Row row) {
		Object obj=row.getArrayObject(ele.getIndex());
		switch(ele.havingCondOperator){
		case EQUALS:
			return ObjectUtils.equals(obj, ele.havingCondValue);
		case GREAT:
			return ObjectUtils.compare((Comparable)obj, (Comparable)ele.havingCondValue)>0;
		case GREAT_EQUALS:
			return ObjectUtils.compare((Comparable)obj, (Comparable)ele.havingCondValue)>=0;
		case IS_NOT_NULL:
			return obj!=null;
		case IS_NULL:
			return obj==null;
		case LESS:
			return ObjectUtils.compare((Comparable)obj, (Comparable)ele.havingCondValue)<0; 
		case LESS_EQUALS:
			return ObjectUtils.compare((Comparable)obj, (Comparable)ele.havingCondValue)<=0;
		case MATCH_ANY:{
			String s1=StringUtils.toString(obj);
			String s2=String.valueOf(ele.havingCondValue);
			return s1.contains(s2);
		}
		case MATCH_END:{
			String s1=StringUtils.toString(obj);
			String s2=String.valueOf(ele.havingCondValue);
			return s1.endsWith(s2);
		}
		case MATCH_START:{
			String s1=StringUtils.toString(obj);
			String s2=String.valueOf(ele.havingCondValue);
			return s1.startsWith(s2);
		}
		case NOT_EQUALS:
			return !ObjectUtils.equals(obj, ele.havingCondValue);
		case NOT_IN:
		case IN:
		case BETWEEN_L_L:
		default:
			throw new UnsupportedOperationException();
		}
	}

	class RowTask {
		private Row baseRow;
		private List<Row> rows = new ArrayList<Row>(64);

		public RowTask(Row row) {
			this.baseRow = row;
			rows.add(row);
		}

		public void merge(Row row) {
			rows.add(row);
		}

		void run() throws SQLFeatureNotSupportedException {
			for (GroupByItem g : values) {
				int index = g.getIndex();
				switch (g.getType()) {
				case ARRAY_TO_STRING:
					baseRow.setArrayObject(index, arrayToString(rows, index));
					break;
				case AVG:
					baseRow.setArrayObject(index, avg(rows, index));
					break;
				case COUNT:
					baseRow.setArrayObject(index, count(rows, index));
					break;
				case MAX:
					baseRow.setArrayObject(index, max(rows, index));
					break;
				case MIN:
					baseRow.setArrayObject(index, min(rows, index));
					break;
				case SUM:
					baseRow.setArrayObject(index, sum(rows, index));
					break;
				default:
					throw new SQLFeatureNotSupportedException("the " + g.getType() + " function was not supported in Memory operate.");
				}
			}
		}

		private Object sum(List<Row> rows, int index) {
			Number value=(Number) rows.get(0).getArrayObject(index);
			if(value instanceof Double){
				return doubleAdd(rows,index);
			}else if(value instanceof Long){
				return longAdd(rows,index);
			}else if(value instanceof Integer){
				return intAdd(rows,index);
			}else if(value instanceof Float){
				return floatAdd(rows,index);
			}else{
				return decmialAdd(rows,index);
			}
		}
	
		@SuppressWarnings("rawtypes")
		private Object min(List<Row> rows2, int index) {
			Comparable min=(Comparable)rows2.get(0).getArrayObject(index);
			for(int i=1;i>rows2.size();i++){
				Comparable current=(Comparable)rows2.get(i).getArrayObject(index);
				if(ObjectUtils.compare(min,current) >0){
					min=current;
				}
			}
			return min;
		}
		@SuppressWarnings("rawtypes")
		private Object max(List<Row> rows2, int index) {
			Comparable max=(Comparable)rows2.get(0).getArrayObject(index);
			for(int i=1;i>rows2.size();i++){
				Comparable current=(Comparable)rows2.get(i).getArrayObject(index);
				if(ObjectUtils.compare(max,current) <0){
					max=current;
				}
			}
			return max;
		}

		private Object count(List<Row> rows2, int index) {
			return intAdd(rows,index);
		}

		//FIXME 这个算法是不对的。将各个结果集平均数全部相加再相除，和原先的所有样本取平均数是不同的。那些样本数较多的平均值权重被忽略了……
		private Object avg(List<Row> rows2, int index) {
			double d = 0;
			for(Row row:rows2){
				Number num=(Number)row.getArrayObject(index);
				d+=num.doubleValue();
			}
			return d/rows2.size();
		}
		
		private Object arrayToString(List<Row> rows2, int index) {
			//TODO support the function array_to_string of postgres
			throw new UnsupportedOperationException();
		}
	}
	
	
	private Object decmialAdd(List<Row> rows2, int index) {
		double d = 0;
		for(Row row:rows2){
			Number num=(Number)row.getArrayObject(index);
			d+=num.doubleValue();
		}
		return new BigDecimal(d);
	}

	private Object floatAdd(List<Row> rows2, int index) {
		float d=0;
		for(Row row:rows2){
			Number num=(Number)row.getArrayObject(index);
			d+=num.floatValue();
		}
		return d;
	}

	private Object longAdd(List<Row> rows2, int index) {
		long d=0;
		for(Row row:rows2){
			Number num=(Number)row.getArrayObject(index);
			d+=num.longValue();
		}
		return d;
	}

	private Object intAdd(List<Row> rows2, int index) {
		int d=0;
		for(Row row:rows2){
			Number num=(Number)row.getArrayObject(index);
			d+=num.intValue();
		}
		return d;
	}

	private Object doubleAdd(List<Row> rows2, int index) {
		double d = 0;
		for(Row row:rows2){
			Number num=(Number)row.getArrayObject(index);
			d+=num.doubleValue();
		}
		return d;
	}


	public void setHaving(List<HavingEle> having2) {
		this.having=having2;
	}

	public String getName() {
		return "group,having";
	}

	
}
