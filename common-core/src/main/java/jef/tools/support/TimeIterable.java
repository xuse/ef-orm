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
package jef.tools.support;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import jef.tools.DateUtils;

/**
 * 用来遍历两个时间之前的每一个整点时间单位。 (如年/月/天/时。。。）
 * <br>
 * 默认不含结束时间所在的时间单位
 * 使用{@link #setIncludeEndDate(boolean)}来指定包含结束日期所在的时间单位。 
 * @author jiyi
 *
 */
public final class TimeIterable implements Iterable<Date> {
	private Date start;	
	private long last;
	private int unit;
	private boolean includeEndDate;
	
	/**
	 * @return true if the iterator will include the last date.
	 */
	public boolean isIncludeEndDate() {
		return includeEndDate;
	}

	/**
	 * set true if the iterator will include the last date.
	 * @param includeEndDate
	 */
	public TimeIterable setIncludeEndDate(boolean includeEndDate) {
		this.includeEndDate = includeEndDate;
		return this;
	}

	public TimeIterable(Date start, Date end, int unit) {
		this.start = DateUtils.getTruncated(start,unit);
		this.last = DateUtils.getTruncated(end,unit).getTime();
		this.unit = unit;
	}

	public Iterator<Date> iterator() {
		return new Iterator<Date>() {
			private Date now=start;
			public boolean hasNext() {
				return now.getTime() < last || (includeEndDate && now.getTime()==last);
			}

			public Date next() {
				Calendar c = new GregorianCalendar();
				c.setTime(now);
				c.add(unit, 1);
				Date result = now;
				now = c.getTime();
				return result;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
