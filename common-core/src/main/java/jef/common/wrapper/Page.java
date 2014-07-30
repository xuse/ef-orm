package jef.common.wrapper;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * 分页后的每页数据。一般用于数据存储层返回.<br>
 * 主要包括两部分信息。
 * <p>
 *  {@link #getTotalCount()} 返回总的记录条数
 * <p>
 *  {@link #getList()} 返回当前页的记录数据
 * @author jiyi
 * @param <T>
 */
@SuppressWarnings("restriction")
@XmlAccessorType(XmlAccessType.FIELD)
public class Page<T> implements Serializable{
	private static final long serialVersionUID = 1L;

	private int totalCount = 0;
	private int pageSize;
	
	private List<T> list ;
	public Page(){
		this(0,10);
	}
	
	/**
	 * 构造
	 * @param total 全部记录的总数
	 */
	public Page(int total){
		this(total,10);
	}
	
	/**
	 * 构造
	 * @param total 全部记录的总数
	 * @param pageSize 每页大小 
	 */
	public Page(int total,int pageSize){
		this.totalCount=total;
		this.pageSize=pageSize;
	}
	
	/**
	 * 构造
	 * @param total 记录总数
	 * @param data  当前页数据
	 */
	public Page(int total,List<T> data,int pageSize){
		this.totalCount=total;
		this.pageSize=pageSize;
		this.list=data;
	}
	/**
	 * 返回记录总数
	 * @return
	 */
	public int getTotalCount() {
		return totalCount;
	}
	/**
	 * 设置记录总数
	 * @param totalCount
	 */
	public void setTotalCount(long totalCount) {
		this.totalCount = (int)totalCount;
	}
	/**
	 * 返回记录数据
	 * @return
	 */
	public List<T> getList() {
		return list;
	}
	/**
	 * 设置记录数据
	 * @param list
	 */
	public Page<T> setList(List<T> list) {
		this.list = list;
		return this;
	}
	/**
	 * 设置记录数据
	 * @param list
	 */
	public Page<T> setList(T[] list) {
		this.list=Arrays.asList(list);
		return this;
	}
	/**
	 * 获得每页的记录数
	 * @return
	 */
	public int getPageSize() {
		return pageSize;
	}
	/**
	 * 获得总页数
	 * @return
	 */
	public int getTotalPage() {
		if(pageSize==0)return 0;
		return ((totalCount - 1) / pageSize) + 1;
	}

	@Override
	public String toString() {
		return "Total:"+totalCount+" page:"+pageSize+" "+list;
	}
}
