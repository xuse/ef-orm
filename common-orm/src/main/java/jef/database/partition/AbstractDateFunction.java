package jef.database.partition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jef.database.annotation.PartitionFunction;
import jef.tools.DateFormats;
import jef.tools.DateUtils;

/**
 * 抽象类，一般用于进行分表取特征值的日志处理函数
 * @author Administrator
 *
 */
public abstract class AbstractDateFunction implements PartitionFunction<Date>{
	//四位数年
	public static final AbstractDateFunction YEAR=new AbstractDateFunction(){
		public String eval(Date value) {
			return String.valueOf(DateUtils.getYear(value));
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.YEAR);
			while (gval.getTime().getTime() <= endSec) {
				ret.add(gval.getTime());
				gval.add(Calendar.YEAR, 1);
			}
			return ret;
		}
		@Override
		public int getTimeLevel() {
			return 5;
		}
		@Override
		List<Date> iterateAll() {
			Date date1=new Date();
			Date date2=new Date();
			DateUtils.addYear(date1, -1);
			DateUtils.addYear(date2, 9);
			return innerIterator(date1, date2, true, false);
		}
	};
	
	public static final AbstractDateFunction YEAR_MONTH=new AbstractDateFunction(){
		public String eval(Date value) {
			return DateUtils.format(value, DateFormats.YAER_MONTH);
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.MONTH);
			while (gval.getTime().getTime() <= endSec) {
				ret.add(gval.getTime());
				gval.add(Calendar.MONTH, 1);
			}
			return ret;
		}
		@Override
		public int getTimeLevel() {
			return 4;
		}
		@Override
		List<Date> iterateAll() {//三年内的所有月份
			int year=DateUtils.getYear(new Date());
			int month=DateUtils.getMonth(new Date());
			month=month-6;
			if(month<1){
				month+=12;
				year-=1;
			}
			Date date1=DateUtils.getDate(year, month, 1);
			Date date2 = DateUtils.getDate(year + 3, month, 1);
			return innerIterator(date1, date2, true, true);
		}
	};
	
	public static final AbstractDateFunction MONTH=new AbstractDateFunction(){
		public String eval(Date value) {
			return String.valueOf(DateUtils.getMonth(value));
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.MONTH);
			while (gval.getTime().getTime() <= endSec) {
				ret.add(gval.getTime());
				gval.add(Calendar.MONTH, 1);
			}
			return ret;
		}
		public int getTimeLevel() {
			return 4;
		}
		@Override
		List<Date> iterateAll() {
			Date date1=DateUtils.getDate(1970, 1, 1);
			Date date2=DateUtils.getDate(1970, 12, 1);
			return innerIterator(date1, date2, true, true);
		}
	};
	
	public static final AbstractDateFunction DAY=new AbstractDateFunction(){
		public String eval(Date value) {
			return String.valueOf(DateUtils.getDay(value));
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.DATE);
			long date=gval.getTimeInMillis();
			while (date <= endSec) {
				ret.add(new Date(date));
				date=date+86400000L;//直接加一天的毫秒数.直接计算性能最好，之所以+1月要借助Calendar对象是因为加一个月太复杂了。
			}
			return ret;
		}
		@Override
		public int getTimeLevel() {
			return 3;
		}
		@Override
		List<Date> iterateAll() {
			Date date1=DateUtils.getDate(1970, 1, 1);
			Date date2=DateUtils.getDate(1970, 1, 31);
			return innerIterator(date1, date2, true, true);
		}
	};
	
	public static final AbstractDateFunction WEEKDAY=new AbstractDateFunction(){
		public String eval(Date value) {
			return String.valueOf(DateUtils.getWeekDay(value));
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.DATE);
			long date=gval.getTimeInMillis();
			while (date <= endSec) {
				ret.add(new Date(date));
				date=date+86400000L;//直接加一天的毫秒数.直接计算性能最好，之所以+1月要借助Calendar对象是因为加一个月太复杂了。
			}
			return ret;
		}
		@Override
		public int getTimeLevel() {
			return 3;
		}
		@Override
		List<Date> iterateAll() {
			Date date1=new Date();
			Date date2=new Date();
			DateUtils.addDay(date2, 7);
			return innerIterator(date1,date2,true,true);
		}
	};
	
	public static final AbstractDateFunction HH24=new AbstractDateFunction(){
		public String eval(Date value) {
			return String.valueOf(DateUtils.getHour(value));
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.HOUR_OF_DAY);
			long date=gval.getTimeInMillis();
			while (date <= endSec) {
				ret.add(new Date(date));
				date=date+3600000L;//直接加一小时的毫秒数
			}
			return ret;
		}
		@Override
		public int getTimeLevel() {
			return 2;
		}
		@Override
		List<Date> iterateAll() {//从去年向后数10年
			Date date1=new Date();
			date1=DateUtils.dayBegin(date1);
			Date date2=DateUtils.dayEnd(date1);;
			return innerIterator(date1, date2, true, true);
		}
	};
	public static final AbstractDateFunction YEAR_LAST2=new AbstractDateFunction(){
		public String eval(Date value) {
			return String.valueOf(DateUtils.getYear(value)).substring(2);
		}
		public List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
			List<Date> ret=new ArrayList<Date>();
			Calendar gval = Calendar.getInstance();
			gval.setTime(sDate);
			long endSec=eDate.getTime();
			if(!rightInclude)endSec--;
			DateUtils.truncate(gval,  Calendar.MONTH);
			while (gval.getTime().getTime() <= endSec) {
				ret.add(gval.getTime());
				gval.add(Calendar.MONTH, 1);
			}
			return ret;
		}
		@Override
		public int getTimeLevel() {
			return 5;
		}
		@Override
		List<Date> iterateAll() {//从去年向后数10年
			Date date1=new Date();
			Date date2=new Date();
			DateUtils.addYear(date1, -1);
			DateUtils.addYear(date2, 9);
			return innerIterator(date1, date2, true, false);
		}
	};

	public List<Date> iterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude) {
		if (sDate == null && eDate==null) {//表示全区间
			return iterateAll();
		}else if(sDate==null){//负无穷区间，直接报错
			throw new UnsupportedOperationException("The min date of this query can not be detimed.");
		}else if(eDate==null){
			eDate=new Date();
		}
		return innerIterator(sDate,eDate,leftInclude,rightInclude);
	}
	
	
	//得到时间段的范围：年5/月4/日3/小时2.这是为了当用户在一个字段上设置了多个分表维度时，取最小的单位来度量值的分布情况
	abstract public int getTimeLevel();
	abstract List<Date> iterateAll();
	abstract protected List<Date> innerIterator(Date sDate,Date eDate,boolean leftInclude,boolean rightInclude);
}
