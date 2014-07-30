/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.common.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jef.common.Range;
import jef.tools.StringUtils;


/**
 * 描述由多个IntRange构成的拼合区间
 * @author Administrator
 *
 */
public class IntRangeGroup implements Range<Integer>,Iterable<Integer>{
	private static final long serialVersionUID = -348521088168327290L;
	IntRange[] ranges;

	public IntRangeGroup(IntRange... range){
		this.ranges=range;
	}
	
	
	public Integer getGreatestValue() {
		if(ranges.length==0){
			throw new IllegalArgumentException("the range is nothing.");
		}
		int max=ranges[0].getGreatestValue();
		for(int i=1;i<ranges.length;i++){
			IntRange r=ranges[i];
			if(r.getGreatestValue()>max){
				max=r.getGreatestValue();
			}
		}
		return max;
	}

	
	public Integer getLeastValue() {
		if(ranges.length==0){
			throw new IllegalArgumentException("the range is nothing.");
		}
		int min=ranges[0].getLeastValue();
		for(int i=1;i<ranges.length;i++){
			IntRange r=ranges[i];
			if(r.getGreatestValue()<min){
				min=r.getGreatestValue();
			}
		}
		return min;
	}

	
	public boolean isContinuous() {
		for(int i=1;i<ranges.length;i++){
			IntRange a=ranges[i];
			boolean isAConnectToAnyOhter=false;
			for(int j=0;j<i;j++){
				IntRange b=ranges[j];
				if(b.contains(a.getStart())|| b.contains(a.getEnd())){//说明区间A和区间B的相连的。
					isAConnectToAnyOhter=true;
					break;
				}
			}
			//只要发现任意一个区间和其他区间是不相连的，那整个区间组就是不连续的。
			if(!isAConnectToAnyOhter)return false;
		}
		return true;
	}

	
	public boolean contains(Integer obj) {
		for(IntRange r: ranges){//任意一个区间包含指定值，就是true
			if(r.contains(obj))return true;
		}
		return false;
	}

	
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>(){
			Iterator<IntRange> iter=Arrays.asList(ranges).iterator();
			Iterator<Integer> now=null;

			
			public boolean hasNext() {
				boolean flag=iter.hasNext();
				if(now==null)return flag;
				return flag||now.hasNext();
			}

			
			public Integer next() {
				if(now==null || !now.hasNext()){
					now=iter.next().iterator();	
				}
				return now.next();
			}

			
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	public static IntRangeGroup getInstance(String rangeStr){
		String[] args=StringUtils.split(rangeStr,',');
		List<IntRange> rangs=new ArrayList<IntRange>();
		for(String arg:args){
			arg=arg.trim();
			int start;
			int end;
			if(arg.indexOf('-')>-1){
				start=StringUtils.toInt(StringUtils.substringBefore(arg, "-"), 0);
				end=StringUtils.toInt(StringUtils.substringAfter(arg, "-"), 0);
			}else{
				start=StringUtils.toInt(arg, 0);
				end=start;
			}
			IntRange i=new IntRange(start,end);
			if(!tryMerge(rangs,i)){
				rangs.add(i);
			}
		}
		return new IntRangeGroup(rangs.toArray(new IntRange[rangs.size()]));
	}

	private static boolean tryMerge(List<IntRange> rangs, IntRange i) {
//		for(IntRange range:rangs){
//			
//		}
		return false;
	}
}
