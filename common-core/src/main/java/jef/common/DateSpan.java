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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import jef.tools.DateUtils;

/**
 * 描述一个时间段
 * 用于以毫秒为单位进行日期时间段的计算
 *
 * @author jiyi
 */
public class DateSpan extends ContinuedRange<Date> {
	private static final long serialVersionUID = 4996949998416390336L;
	
	private long start;
	private long end;
	
	/*
	 * (non-Javadoc)
	 * @see jef.common.ContinuedRange#getEnd()
	 */
	public Date getEnd() {
		return new Date(end);
	}
	public void setEnd(Date end) {
		this.end = end.getTime();
	}
	
	public Date getStart() {
		return new Date(start);
	}
	
	public void setStart(Date start) {
		this.start = start.getTime();
	}
	public DateSpan(){};
	public DateSpan(long start,long end){
		this.start=start;
		this.end=end;
	}
	
	public DateSpan(Date start,Date end){
		this.start=start.getTime();
		this.end=end.getTime();
		if(this.end<this.start)throw new IllegalArgumentException("start:"+ DateUtils.formatDateTime(start)+"end:" + DateUtils.formatDateTime(end));
	}
	
	/*
	 * (non-Javadoc)
	 * @see jef.common.ContinuedRange#extendTo(java.lang.Comparable)
	 */
	public void extendTo(Date date) {
		if(date.getTime()<start){
			start=date.getTime();
		}else if(date.getTime()>end){
			end=date.getTime();
		}
	};
	
	
	public String toString() {
		return DateUtils.formatDateTimeWithToday(new Date(start))+" - " + DateUtils.formatDateTimeWithToday(new Date(end));
	}
	
	/**
	 * 将是时间段提前或推迟若干时间，长度不变，其中年，月的变化值和开始时间相关，和结束时间无关
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 */
	public void move(int year,int month,int day,int hour,int minute,int second){
		Calendar c1=new GregorianCalendar();
		c1.setTimeInMillis(start);
		c1.add(Calendar.YEAR, year);
		c1.add(Calendar.MONTH, month);
		c1.add(Calendar.DAY_OF_YEAR, day);
		c1.add(Calendar.HOUR_OF_DAY, hour);
		c1.add(Calendar.MINUTE, minute);
		c1.add(Calendar.SECOND, second);
		this.end+=c1.getTimeInMillis()-start;
		this.start=c1.getTimeInMillis();
	}
	
	/**
	 * 返回一个或两个DateSpan对象，从原来的时间段中除去指定的时间段，如果时间段被拆成两端，则返回两个对象
	 * @param ds
	 * @return
	 */
	public DateSpan[] removePiece(DateSpan ds){
		
		boolean headFlag=(this.start<ds.start);
		boolean tailFlag=(this.end>ds.end);
		DateSpan new1=null;
		DateSpan new2=null;
		if(headFlag){
			new1=new DateSpan(this.start,ds.start);
			if(new1.end>this.end)new1.end=this.end;
		}
		if(tailFlag){
			new2=new DateSpan(ds.end,this.end);
			if(new2.start<this.start)new2.start=this.start;
		}
		if(new1==null && new2==null){
			return null;
		}else if(new1==null && new2!=null){
			return new DateSpan[]{new2};
		}else if(new1==null && new2!=null){
			return new DateSpan[]{new1};
		}else{
			return new DateSpan[]{new1,new2};
		}
	}
	
	/**
	 * 返回一个表示今天的日期时间范围。（本时区）
	 * @return
	 */
	public static DateSpan Today(){
		TimeZone tz=TimeZone.getDefault();
		long now=System.currentTimeMillis();
		long pa=now % DateUtils.MILLISECONDS_IN_DAY;
		long start=now-pa-tz.getRawOffset();
		return new DateSpan(start,start+DateUtils.MILLISECONDS_IN_DAY);
	}
	/**
	 * 返回一个表示当前小时
	 * @return
	 */
	public static DateSpan currentHour() {
		long now=System.currentTimeMillis();
		long pa=now % DateUtils.MILLISECONDS_IN_HOUR;
		long start=now-pa;
		return new DateSpan(start,start+DateUtils.MILLISECONDS_IN_HOUR);
	}
	
	
	public boolean isBeginIndexInclusive() {
		return true;
	}
	
	public boolean isEndIndexInclusive() {
		return true;
	}
}
