package jef.database.wrapper.clause;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.database.rowset.CachedRowSetImpl;
import jef.database.rowset.Row;

import org.apache.commons.lang.ObjectUtils;

public class InMemoryGroupBy implements InMemoryProcessor {
	GroupByItem[] keys;
	GroupByItem[] values;

	public InMemoryGroupBy(List<GroupByItem> keys, List<GroupByItem> values) {
		this.keys = keys.toArray(new GroupByItem[keys.size()]);
		this.values = values.toArray(new GroupByItem[values.size()]);
		;
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
		rowset.setRvh(newRows);
		rowset.refresh();
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
				case ARRAY_TO_LIST:
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

	
}
