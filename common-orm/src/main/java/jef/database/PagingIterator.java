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
package jef.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import jef.common.wrapper.IntRange;
import jef.common.wrapper.Page;
import jef.database.support.MultipleDatabaseOperateException;
import jef.database.wrapper.populator.Transformer;
import jef.tools.PageInfo;

/**
 * 用于分页查询的遍历器,用于使用分页来查询结果<br>
 * 实现了Iterator<List<T>>接口，即每次调用next()方法可以得到下一页的数据<br>
 * 
 * <ul>
 * <li> {@link #getTotal()}<br>
 * 得到总记录条数（JEF会自动将你的查询语句改写为等效的count语句进行查询）</li>
 * <li>getTotalPage()<br>
 * 得到总页数</li>
 * <li>hasNext()可以判断是否可以继续向下翻页。</li>
 * <li>setOffset(int) 可以设置当前记录读取从第几个开始，例如0表示从第一条记录开始，1表示从第二条开始.<br>
 * <li>getCurrentPage()可以获得当前的页号</li>
 * 每页的size可以在构造的时候传入。<br>
 * </ul>
 * 
 * @author Administrator
 * 
 * @param <T>
 */
public abstract class PagingIterator<T> implements Iterator<List<T>> {
	/**
	 * 分页信息
	 */
	protected PageInfo page;
	/**
	 * 记录转换结果类型
	 */
	protected Transformer transformer;
	
	/**
	 * 最后一页
	 * FIXME 这个设计有点冗余
	 */
	protected int lastPage = -1;

	/*
	 * TODO 用于报表或导出时，使用Oracle共享锁，阻止记录被插入和更改。
	 * 关于数据导出时分页的一致性问题。 一个方法是加上共享锁，这样所有的写操作都会挂起。 * 还有一个方式是使用create table as
	 * 获得一个记录快照，然后根据快照来分页获取数据，保证 整个处理过程中，不会因为有数据被插入或者顺序变化，造成记录的错漏和重复。
	 */

	/**
	 * 获取总记录数
	 * 
	 * @return 总记录数, int类型
	 */
	public int getTotalAsInt() {
		long total = getTotal();
		if (total > Integer.MAX_VALUE) {
			throw new IllegalStateException();
		}
		return (int) total;
	}

	/**
	 * 获取总记录数
	 * 
	 * @return
	 */
	public long getTotal() {
		calcPage();
		return page.getTotal();
	}

	/**
	 * 获取总页数
	 * 
	 * @return 总页数。0表示没有记录，有一条记录时也是1页。
	 */
	public int getTotalPage() {
		calcPage();
		return page.getTotalPage();
	}

	/**
	 * 获得当前页数
	 * 
	 * @return
	 */
	public int getCurrentPage() {
		return page.getCurPage();
	}

	/**
	 * 获得每页记录条数
	 * 
	 * @return
	 */
	public int getRowsPerPage() {
		return page.getRowsPerPage();
	}

	/**
	 * 设置查询结果偏移
	 * 
	 * @param offset 从0开始。如要跳过前10条记录则输入10，查询结果将从第11条记录开始
	 * @return PagingIterator对象本身
	 */
	public PagingIterator<T> setOffset(int offset) {
		page.setOffset(offset);
		return this;
	}

	/**
	 * 设置下一次调用 {@link #next()}方法要读取的页数。
	 * 
	 * @param pageNum
	 *            页数，从1开始。如果大于总页数会下调至最后一页。
	 * @return PagingIterator对象本身
	 */
	public PagingIterator<T> setCurrentPage(int pageNum) {
		int totalPage = this.getTotalPage();
		if (pageNum > totalPage) {
			pageNum = totalPage;
		}
		page.setCurPage(pageNum);
		return this;
	}

	/**
	 * 得到在指定页面的所有数据
	 * 
	 * @param pageNum 页号，从1开始
	 * @return 查询结果
	 */
	public List<T> getRecordsInPage(int pageNum) {
		calcPage();
		int old=page.getCurPage();
		page.setCurPage(pageNum);
		try {
			return doQuery(true);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}finally{
			page.setCurPage(old);
		}
	}

	/**
	 * 下一页
	 */
	@SuppressWarnings("unchecked")
	public List<T> next() {
		calcPage();
		try {
			if (page.getCurrentRecordRange().size() <= 0) {
				return Collections.EMPTY_LIST;
			}
			return doQuery(true);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			if (!page.gotoNext()) {
				lastPage = page.getCurPage() - 1;
			}
		}
	}

	/**
	 * 子类实现，完成查询
	 * 
	 * @param b 是否执行分页逻辑
	 * @return
	 * @throws SQLException
	 */
	protected abstract List<T> doQuery(boolean b) throws SQLException;

	/**
	 * 子类实现，计算记录条数
	 * 
	 * @return 总记录条数
	 */
	protected abstract long doCount() throws SQLException;

	public boolean hasNext() {
		calcPage();
		if (lastPage > -1) {
			return page.getCurPage() < lastPage;
		}
		return page.hasNext();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 获得当前页的全部数据
	 * 
	 * @return
	 */
	public Page<T> getPageData() {
		return new Page<T>((int) getTotal(), next(),page.getRowsPerPage());
	}

	// 获取总数，用来计算分页
	protected void calcPage() {
		if (page.getTotal() == -1) {
			recalculatePages();
		}
	}

	/**
	 * 强制重新计算一次页数<br>
	 * 默认情况下，页数只计算一次。 调用此方法可以即时再计算一次页数。
	 */
	public void recalculatePages() {
		try {
			page.setTotal(doCount());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	// 如果在不获取总数的情况下任意（顺序）翻页，当翻到全空的一页时，就知道前一页就是最后一页，从而能够计算出总数了，防止继续翻页
	protected void recordEmpty() {
		if (lastPage < 0 || page.getCurPage() < lastPage) {
			lastPage = page.getCurPage() - 1;// 记录
		}
	}
}
