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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jef.tools.PageInfo;

/**
 * 对象分页器，将List对象分页
 * @author Administrator
 *
 * @param <T>
 */
public class Paginator<T> implements Iterable<List<T>>{
	PageInfo p;
	List<T> data;
	
	
	public Iterator<List<T>> iterator() {
		return new Iterator<List<T>>(){
			
			public boolean hasNext() {
				return p.hasNext();
			}

			
			public List<T> next() {
				IntRange r=p.getCurrentRecordRange();
				return data.subList(r.getStart()-1, r.getEnd());
			}

			
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public List<T> get(int pageNum){
		p.setCurPage(pageNum);
		IntRange r=p.getCurrentRecordRange();
		return data.subList(r.getStart()-1, r.getEnd());
	}
	
	public Paginator(T[] data,int rowsPerPage){
		this.data=Arrays.asList(data);
		this.p=new PageInfo(data.length,rowsPerPage);
	}
	
	public Paginator(List<T> data,int rowsPerPage){
		this.data=data;
		this.p=new PageInfo(data.size(),rowsPerPage);
	}
	
	public int getTotal(){
		return (int)p.getTotal();
	}
	
	public int getCurrentPage(){
		return p.getCurPage();
	}
	public int getTotalPage(){
		return p.getTotalPage();
	}
	public IntRange getRange(){
		return p.getCurrentRecordRange();
	}
	public void setPage(int page){
		p.setCurPage(page);
	}
	public void nextPage(){
		p.gotoNext();
	}
	public void prevPage(){
		p.gotoPrev();
	}
}
