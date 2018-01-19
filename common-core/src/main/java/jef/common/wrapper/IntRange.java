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

import java.util.Iterator;

import jef.common.ContinuedRange;

/**
 * 描述一个含头含尾的数字区间
 * @author Administrator
 *
 */
public class IntRange extends ContinuedRange<Integer> implements Iterable<Integer>{
	private static final long serialVersionUID = 8871503409414219286L;
	private int start;
	private int end;
	public IntRange(int start,int end){
		this.start=start;
		this.end=end;
	}
	
	public String toString() {
		return start + ".." + end;
	}
	
	
	public final boolean isBeginIndexInclusive() {
		return true;
	}
	
	public final boolean isEndIndexInclusive() {
		return true;
	}
	
	
	public Integer getEnd() {
		return end;
	}

	
	public Integer getStart() {
		return start;
	}
	
	public boolean contains(double num) {
		return num>=start && num<=end;
	}
	
	public boolean contains(float num) {
		return num>=start && num<=end;
	}
	
	public boolean contains(long num) {
		return num>=start && num<=end;
	}
	
	public Iterator<Integer> iterator(){
		return new Iterator<Integer>(){
			private int now=start;
			
			public boolean hasNext() {
				return now<=end;
			}

			
			public Integer next() {
				return now++;
			}

			
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public void extendTo(Integer date) {
		if(date<start){
			start=date;
		}else if(date>end){
			end=date;
		}
	}

	public int size() {
		return end-start+1;
	}
}
