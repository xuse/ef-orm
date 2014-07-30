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
package jef.common;

import jef.tools.Assert;

/**
 * 描述一个连续的区间
 * @author Administrator
 *
 * @param <T>
 */
public abstract class ContinuedRange<T extends Comparable<T>> implements Range<T> {
	private static final long serialVersionUID = 1L;
	
	/*
	 * (non-Javadoc)
	 * @see jef.common.Range#isContinuous()
	 */
	final public boolean isContinuous(){
		return true;
	}
	
	/**
	 * 获得区间的结束点
	 * @return
	 */
	public abstract T getEnd();
	
	/**
	 * 获取区间的开始点
	 * @return
	 */
	public abstract T getStart();
	
	public boolean include(ContinuedRange<T> obj){
		return false;
	}
	
	public boolean nextTo(ContinuedRange<T> obj){
		return false;
	}
	
	/**
	 * 本区间与目标区间是否重合
	 * @param obj
	 * @return
	 */
	public boolean overlapping(ContinuedRange<T> obj){
		if(obj.getEnd().compareTo(this.getStart())<0 || obj.getStart().compareTo(this.getEnd())>0){
			return false;
		}
		
		if(obj.getEnd().compareTo(this.getStart())==0){
			if(obj.isEndIndexInclusive() && this.isBeginIndexInclusive()){
				return true;
			}else{
				return false;
			}
		}
		
		if(obj.getStart().compareTo(this.getEnd())==0){
			if(obj.isBeginIndexInclusive() && this.isEndIndexInclusive()){
				return true;
			}else{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 如果指定的值位于区间之外，则将区间扩展到那个点上
	 * @param obj
	 */
	public abstract void extendTo(T obj);
	/*
	 * (non-Javadoc)
	 * @see jef.common.Range#getGreatestValue()
	 */
	public final T getGreatestValue() {
		return getEnd();
	}
	/*
	 * (non-Javadoc)
	 * @see jef.common.Range#getLeastValue()
	 */
	public final T getLeastValue() {
		return getStart();
	}
	/*
	 * (non-Javadoc)
	 * @see jef.common.Range#contains(java.lang.Comparable)
	 */
	public final boolean contains(T obj) {
		Assert.notNull(obj);
		if(this.isBeginIndexInclusive()){
			if(obj.compareTo(this.getStart())<0){
				return true;
			}
		}else{
			if(obj.compareTo(this.getStart())<=0){
				return true;
			}
		}
		if(this.isEndIndexInclusive()){
			if(obj.compareTo(this.getEnd())>0){
				return true;
			}
		}else{
			if(obj.compareTo(this.getEnd())>=0){
				return true;
			}
		}
		return false;
	}

	/**
	 * 最小值是否包含在范围内？ 即是否开区间
	 * @return
	 */
	public abstract boolean isEndIndexInclusive();
	
	/**
	 * 最大值是否包含在范围内？ 即是否开区间
	 * @return
	 */
	public abstract boolean isBeginIndexInclusive();
	
	
}
