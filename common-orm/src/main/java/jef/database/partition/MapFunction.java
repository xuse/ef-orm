package jef.database.partition;

import java.util.ArrayList;
import java.util.List;

import jef.common.ContinuedRange;
import jef.database.annotation.PartitionFunction;

import org.apache.commons.lang.StringUtils;

public class MapFunction implements PartitionFunction<String>{
	private final List<StringRange> ranges = new ArrayList<StringRange>();
	private List<String> allValues=new ArrayList<String>();
	public MapFunction(String expression){
		StringRange all = null;
		for(String s:StringUtils.split(expression,",")){
			int index=s.lastIndexOf(':');
			if(index<1){
				throw new IllegalArgumentException("Invalid config of map:"+s);
			}
			String value=s.substring(index+1);
			s=s.substring(0,index);
			if("*".equals(s)){
				all=new All().setTarget(value);
			}else{
				index=s.lastIndexOf('-');
				if(index>0){ //如果第一位就是-，认为是负号，不算分隔符
					ranges.add(new StringRange(s.substring(0,index),s.substring(index+1)).setTarget(value));
				}else{
					ranges.add(new Single(s).setTarget(value));
				}	
			}
		}
		if(all!=null){
			ranges.add(all);
		}
		for(StringRange range:ranges){
			allValues.add(range.getStart());	
		}
	}

	public String eval(String value) {
		for(StringRange range:ranges){
			if(range.contains(value)){
				return range.target;
			}
		}
		return "";
	}

	public List<String> iterator(String min, String max, boolean left,	boolean right) {
		return allValues;
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
	@SuppressWarnings("serial")
	private  static final class All extends StringRange{
		public All(String s) {
			super(s,s);
		}
		public All() {
			super("*","*");
		}
		public boolean contains(String obj) {
			return true;
		}
	}
}
