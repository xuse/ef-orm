package jef.database.routing.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jef.common.ContinuedRange;
import jef.database.annotation.PartitionFunction;
import jef.database.query.RegexpDimension;
import jef.tools.StringUtils;

public class MapFunction implements PartitionFunction<String>{
	private final List<StringRange> ranges = new ArrayList<StringRange>();
	private String defaultTarget="";
	private int maxLen;
	private List<String> allValues=new ArrayList<String>();
	
	public MapFunction(String expression,int digit){
		this.maxLen=digit>0?digit:20;
		for(String s:StringUtils.split(expression,",")){
			int index=s.lastIndexOf(':');
			if(index<1){
				throw new IllegalArgumentException("Invalid config of map:"+s);
			}
			String value=s.substring(index+1);
			s=s.substring(0,index);
			if("*".equals(s)){
				defaultTarget=value;
			}else{
				index=s.lastIndexOf('-');
				if(index>0){ //如果第一位就是-，认为是负号，不算分隔符
					ranges.add(new StringRange(s.substring(0,index),s.substring(index+1)).setTarget(value));
				}else{
					ranges.add(new Single(s).setTarget(value));
				}	
			}
		}
		Collections.sort(ranges,new Comparator<StringRange>() {//必须按小到大排序
			public int compare(StringRange o1, StringRange o2) {
				return o1.getStart().compareTo(o2.getStart());
			}
		});
		for(StringRange range:ranges){
			allValues.add(range.getStart());	
		}
	}

	public String eval(String value) {
		if(value.length()>maxLen){
			value=value.substring(0,maxLen);
		}
		for(StringRange range:ranges){
			if(range.contains(value)){
				return range.target;
			}
		}
		return defaultTarget;
	}
	//TODO 这个方法要单元测试一下
	public List<String> iterator(String min, String max, boolean left,	boolean right) {
		if(min==null && max==null){
			return allValues;
		}
		List<String> result=new ArrayList<String>();
		boolean open=false;
		if(min==null){
			open=true;
		}else if(left){
			result.add(min);
		}
		for(StringRange range:ranges){
			if(open){
				if(range.contains(max)){
					open=false;
					break;
				}else{
					result.add(range.start);
				}
			}else{
				if(range.contains(min)){
					open=true;
					if(range.contains(max)){
						open=false;
						break;
					}
				}
			}
		}
		if(max!=null && !max.equals(min) && right){
			result.add(max);
		}
		return result;
	}
	@SuppressWarnings("serial")
	private static class StringRange extends ContinuedRange<String>{
		protected String start;
		private String end;
		private String target;
		public StringRange setTarget(String target) {
			this.target = target;
			return this;
		}

		public StringRange(String start, String end) {
			this.start=start;
			this.end=end;
		}

		@Override
		public String getEnd() {
			return end;
		}

		@Override
		public String getStart() {
			return start;
		}

		@Override
		public void extendTo(String obj) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEndIndexInclusive() {
			return true;
		}

		@Override
		public boolean isBeginIndexInclusive() {
			return true;
		}
	}
	@SuppressWarnings("serial")
	private  static final class Single extends StringRange{
		public Single(String s) {
			super(s,s);
		}

		public boolean contains(String obj) {
			return start.equals(obj);
		}
	}
	public boolean acceptRegexp() {
		return false;
	}

	public Collection<String> iterator(RegexpDimension regexp) {
		throw new UnsupportedOperationException();
	}
}
