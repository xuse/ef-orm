package jef.database.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import jef.database.annotation.PartitionFunction;

import  org.apache.commons.lang3.StringUtils;

/**
 * 描述一个非连续区间
 * @author jiyi
 *
 */
public class ComplexDimension implements Dimension {
	private final List<Dimension> ranges=new ArrayList<Dimension>();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ComplexDimension create(Comparable[] objs){
		if(objs.length==0)return null;
		
		ComplexDimension result=new ComplexDimension(new RangeDimension(objs[0]));
		for(int i=1;i<objs.length;i++){
			result.mergeOr(new RangeDimension(objs[i]));
		}
		return result;
	}
	
	public ComplexDimension(Dimension dimensions){
		ranges.add(dimensions);
	}
	
	public Dimension mergeAnd(Dimension d) {
		for(int i=0;i<ranges.size();i++){
			ranges.set(i,ranges.get(i).mergeAnd(d));
		}
		check();
		return this;
	}

	public Dimension mergeOr(Dimension d) {
		ranges.add(d);
		check();
		return this;
	}

	public Dimension mergeNot() {
		Dimension result=null;
		for(int i=0;i<ranges.size();i++){
			if(result==null){
				result=ranges.get(i).mergeNot();
			}else{
				result=result.mergeAnd(ranges.get(i).mergeNot());
			}
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<?> toEnumationValue(Collection<PartitionFunction> function) {
		Collection set=new TreeSet();
		for(Dimension d: ranges){
			Collection<?> objs=d.toEnumationValue(function);
//			if(objs.length==2 && (objs[0]==null || objs[1]==null)){//出现正负无穷的枚举就不处理
//				break;
//			}
			set.addAll(objs);	
		}
		return set;
	}
	

	@Override
	public String toString() {
		if(ranges.size()>0)return StringUtils.join(ranges," || "); 
		return "invalid!";
	}

	/**
	 * 合并出现重叠的区间.清除无效的区间
	 */
	private void check() {
		for(Iterator<Dimension> iter=ranges.iterator();iter.hasNext();){
			Dimension d=iter.next();
			if(!d.isValid()){
				iter.remove();
			}
		}
	}

	public boolean isValid() {
		return ranges.size()>0;
	}

}
