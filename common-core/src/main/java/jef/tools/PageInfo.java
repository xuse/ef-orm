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
package jef.tools;

import jef.common.wrapper.IntRange;

/**
 * 分页计算工具
 * @author Administrator
 */
public class PageInfo {
	private static final int UNKNOWN = -1;
	private int total = UNKNOWN;// 总条数
	private int rowsPerPage = 10;// 每页面显示的条目数
	private int curPage = 1; // 当前页面
	private int totalPage = UNKNOWN; // 总页数
	
	private int offset=0;

	public PageInfo() {}

	/**
	 * 设置Offset。即采用偏移模式，即PageInfo的当前页数总是为0。
	 * @param offset
	 */
	public void setOffset(int offset){
		this.offset=offset;
	}
	
	
	/**
	 * 根据记录所在的页数来设置页。记录从0开始计数。
	 * 例如每页10条时。
	 * <pre>setCurrentPageByOffset(0) == 1
	 * setCurrentPageByOffset(9) == 1
	 * setCurrentPageByOffset(10) == 2
	 * setCurrentPageByOffset(19) == 2
	 * setCurrentPageByOffset(20) == 3</pre>
	 * @param offset
	 * @return 设置后当前的页数
	 */
	public int setCurrentPageByOffset(int offset){
		if(offset<0)return getCurPage();
		int num=offset/rowsPerPage;
		this.curPage=num+1;
		this.offset=0;
		return curPage;
	}
	
	/**
	 * 设置 curPage 的值 类型 为 int
	 * @param curPage 当面页面从1开始
	 */
	public void setCurPage(int curPage) {
		if (this.curPage < 1) {
			this.curPage = 1;
		} else {
			this.curPage = curPage;
		}
		this.offset=0;
		
	}
	/**
	 * 获取 curPage 的信息
	 */
	public int getCurPage() {
		if(offset>0)return 0;
		return curPage;
	}

	
	/**
	 * 构造
	 * @param total  总记录数
	 * @param rowsPerPage 每页记录数
	 */
	public PageInfo(int total, int rowsPerPage) {
		this.total = total;
		this.rowsPerPage = rowsPerPage;
		sumPage();
	}


	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Records: " + total);
		sb.append(" \tPage: ").append(getCurPage()).append('/').append(totalPage);
		sb.append(" \tCurrentShows: ").append(this.getCurrentRecordRange().toString());
		return sb.toString();
	}



	/**
	 * 获取 总的行数
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * 获取每页行数
	 * 
	 * @return
	 */
	public int getRowsPerPage() {
		return rowsPerPage;
	}

	/**
	 * 设置每页行数
	 */
	public void setRowsPerPage(int pageRow) {
		if(pageRow<1){
			throw new IllegalArgumentException("The Page size must greater than zero!, you page size "+pageRow+" is invalid!");
		}
		if (pageRow == this.rowsPerPage)
			return;
		rowsPerPage = pageRow;
		sumPage();
	}

	/**
	 * 设置总行数
	 */
	public void setTotal(long total) {
		if (total == this.total)
			return;
		this.total = (int)total;
		sumPage();
	}

	public boolean hasNext() {
		if (total == UNKNOWN)
			return true;
		return curPage <= totalPage;
	}

	/**
	 * 下一页
	 * 
	 * @return int nextPage
	 */
	public boolean gotoNext() {
		if (totalPage!=UNKNOWN && curPage >= totalPage)
			return false;
		curPage++;
		return true;
	}

	/**
	 * 得到上一页
	 * 
	 * @return int prePage
	 */
	public boolean gotoPrev() {
		if (curPage <= 1)
			return false;
		curPage--;
		return true;
	}

	/**
	 * 去第一页
	 * 
	 * @return int firstPage
	 */
	public void gotoFirst() {
		curPage = 1;
	}

	public void gotoLast() {
		curPage = totalPage;
	}

	public int getTotalPage(){
		return totalPage;
	}
	

	// 返回当前页记录的序号(含头含尾)
	public IntRange getCurrentRecordRange() {
		int start;
		if(offset>0){
			start= offset+ 1;	
		}else{
			start=rowsPerPage * (curPage - 1)+1;
		}
		if (curPage == totalPage && totalPage != UNKNOWN) {
			return new IntRange(start, total);
		} else {
			return new IntRange(start, start + rowsPerPage - 1);
		}
	}

	/**
	 * 分页计算
	 */
	private void sumPage() {
		if (total > UNKNOWN) {
			totalPage = ((total - 1) / rowsPerPage) + 1; // 计算总页面
		} else {
			totalPage = UNKNOWN;
		}
	}
}
