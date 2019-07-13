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

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.tools.support.TimeIterable;

/**
 * utils for date.
 * 
 * Revised 2018-12-26，全面支持多时区运算.
 */
public abstract class DateUtils {
	private static final Logger log=LoggerFactory.getLogger(DateUtils.class);
	
	public static final int SECONDS_IN_DAY = 86400;
	public static final int SECONDS_IN_HOUR = 3600;
	public static final int SECONDS_IN_MINITE = 60;

	public static final int MILLISECONDS_IN_DAY = 86400000;
	public static final int MILLISECONDS_IN_HOUR = 3600000;
	public static final int MILLISECONDS_IN_MINUTE = 60000;
	public static final int MILLISECONDS_IN_SECOND = 1000;

	private static String TODAY = "\u4eca\u5929";
	private static String YESTERDAY = "\u6628\u5929";
	private static String TOMORROW = "\u660e\u5929";
	private static String TOMORROW2 = "\u540e\u5929";

	/**
	 * 格式化日期为中式格式
	 * 
	 * @deprecated Use @{DateFormats}
	 * @param d
	 * @return
	 */
	public static String formatDate(Date d) {
		if (d == null)
			return "";
		return DateFormats.DATE_CS.get().format(d);
	}

	/**
	 * 格式化为日期+时间（中式）
	 * 
	 * @param d
	 * @return
	 */
	public static Optional<String> formatDateTime(Date d) {
		return DateFormats.DATE_TIME_CS.format2(d);
	}

	/**
	 * 格式化日期（中式）格式化后的日期带有“今天”等形式
	 * 
	 * @param d
	 * @return
	 */
	public static String formatDateWithToday(Date d) {
		if (d == null)
			return "";
		if (isSameDay(new Date(), d)) {
			return TODAY;
		} else if (isSameDay(futureDay(-1), d)) {
			return YESTERDAY;
		} else if (isSameDay(futureDay(1), d)) {
			return TOMORROW;
		} else if (isSameDay(futureDay(2), d)) {
			return TOMORROW2;
		}
		return DateFormats.DATE_CS.get().format(d);
	}

	/**
	 * 格式化日期+时间,格式化后的日期带有“今天”等形式
	 * 
	 * @param d
	 * @return
	 */
	public static String formatDateTimeWithToday(Date d) {
		if (d == null)
			return "";
		if (isSameDay(new Date(), d)) {
			return TODAY + " " + DateFormats.TIME_ONLY.get().format(d);
		} else if (isSameDay(yesterday(), d)) {
			return YESTERDAY + " " + DateFormats.TIME_ONLY.get().format(d);
		} else if (isSameDay(futureDay(1), d)) {
			return TOMORROW + " " + DateFormats.TIME_ONLY.get().format(d);
		} else if (isSameDay(futureDay(2), d)) {
			return TOMORROW2 + " " + DateFormats.TIME_ONLY.get().format(d);
		}
		return DateFormats.DATE_TIME_CS.get().format(d);
	}

	/**
	 * 从dos系统格式的时间数字转换到Java时间
	 * 
	 * @param dostime
	 * @return
	 */
	public static Date fromDosTime(long dostime) {
		int hiWord = (int) ((dostime & 0xFFFF0000) >>> 16);
		int loWord = (int) (dostime & 0xFFFF);

		Calendar date = Calendar.getInstance();
		int year = ((hiWord & 0xFE00) >>> 9) + 1980;
		int month = (hiWord & 0x01E0) >>> 5;
		int day = hiWord & 0x1F;
		int hour = (loWord & 0xF800) >>> 11;
		int minute = (loWord & 0x07E0) >>> 5;
		int second = (loWord & 0x1F) << 1;
		date.set(year, month - 1, day, hour, minute, second);
		return date.getTime();
	}

	/**
	 * 取得截去年以下单位的时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @return 截断后的时间
	 */
	public static Date truncateToYear(Date d) {
		return truncateToYear(d, TimeZone.getDefault());
	}

	/**
	 * 取得截去年以下单位的时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @param zone
	 *            时区
	 * @return 截断后的时间
	 */
	public static Date truncateToYear(Date d, TimeZone zone) {
		long l = d.getTime();
		long left = (l + zone.getRawOffset()) % MILLISECONDS_IN_DAY;
		l = l - left;

		Calendar c = Calendar.getInstance(zone);
		c.setTimeInMillis(l);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.MONTH, 0);
		return c.getTime();
	}

	/**
	 * 取得截去年以下单位的时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @param int
	 *            utcOffset 时区，-12 ~ +14
	 * @return 截断后的时间
	 */
	public static Date truncateToYear(Date d, int utcOffset) {
		return truncateToYear(d, TimeZone.getTimeZone(TimeZones.TIME_ZONES[utcOffset + 12]));
	}

	/**
	 * 取得截去月以下单位的时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @return 截断后的时间
	 */
	public static Date truncateToMonth(Date d) {
		return truncateToMonth(d, TimeZone.getDefault());
	}

	/**
	 * 取得截去月以下单位的时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @param zone
	 *            时区
	 * @return 截断后的时间
	 */
	public static Date truncateToMonth(Date d, TimeZone zone) {
		long l = d.getTime();
		long left = (l + zone.getRawOffset()) % MILLISECONDS_IN_DAY;
		l = l - left;
		Calendar c = Calendar.getInstance(zone);
		c.setTimeInMillis(l);
		c.set(Calendar.DAY_OF_MONTH, 1);
		return c.getTime();
	}

	/**
	 * 取得截去天以下单位的时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @param int
	 *            utcOffset 时区，-12 ~ +14
	 * @return 截断后的时间
	 */
	public static Date truncateToMonth(Date d, int utcOffset) {
		return truncateToMonth(d, TimeZone.getTimeZone(TimeZones.TIME_ZONES[utcOffset + 12]));
	}

	/**
	 * 取得截断后日期。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @return 截断后的时间
	 */
	public static Date truncateToDay(Date d) {
		return truncateToDay(d, TimeZone.getDefault());
	}

	/**
	 * 取得截断后日期。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @param int
	 *            utcOffset 时区，-12 ~ +14
	 * @return 截断后的时间
	 */
	public static Date truncateToDay(Date d, int utcOffset) {
		if (utcOffset > 14 || utcOffset < -12) {
			throw new IllegalArgumentException("Invalid UTC time offset,must beetween UTC-12 to UTC+14.");
		}
		if (d == null) {
			return null;
		}
		long l = d.getTime();
		long left = (l + utcOffset * 3600000) % MILLISECONDS_IN_DAY;
		return new Date(l - left);
	}

	/**
	 * 取得截断后日期。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param d
	 *            时间，如果传入null本方法将返回null.
	 * @param zone
	 *            时区
	 * @return 截断后的时间
	 */
	public static Date truncateToDay(Date d, TimeZone zone) {
		if (d == null) {
			return null;
		}
		long l = d.getTime();
		long left = (l + zone.getRawOffset()) % MILLISECONDS_IN_DAY;
		return new Date(l - left);
	}

	/**
	 * 取得截去分钟以下单位的时间。得到整点
	 * 
	 * @param d
	 *            时间，，如果传入null本方法将返回null.
	 * @return 截去分钟以下单位的时间。
	 */
	public static Date truncateToHour(Date d) {
		if (d == null) {
			return null;
		}
		long l = d.getTime();
		long left = l % MILLISECONDS_IN_HOUR;
		return new Date(l - left);
	}

	/**
	 * 取得截去秒以下单位的时间。得到整分钟时间点
	 * 
	 * @param d
	 *            时间，，如果传入null本方法将返回null.
	 * @return 截去秒以下单位的时间。
	 */
	public static Date truncateToMinute(Date d) {
		if (d == null) {
			return null;
		}
		long l = d.getTime();
		long left = l % MILLISECONDS_IN_MINUTE;
		return new Date(l - left);
	}

	/**
	 * 取得截去毫秒以下单位的时间。得到整秒时间点
	 * 
	 * @param d
	 *            时间，，如果传入null本方法将返回null.
	 * @return 截去毫秒以下单位的时间。
	 */
	public static Date truncateToSecond(Date d) {
		if (d == null) {
			return null;
		}
		long l = d.getTime();
		long left = l % MILLISECONDS_IN_SECOND;
		return new Date(l - left);
	}

	/**
	 * 获取天开始的瞬间时间点<br/>
	 * 此方法不支持传入时区，需要支持多时区时请使用 {@link #truncateToDay(Date, TimeZone)}
	 * 
	 * @param d
	 *            时间点
	 * @return 仅日期
	 * @see #truncateToDay(Date)
	 * 
	 */
	public static Date dayBegin(Date d) {
		return truncateToDay(d);
	}

	/**
	 * 获得当天结束前最后一毫秒的时间点<br/>
	 * 
	 * @param d
	 *            时间
	 * @return 当天最后一毫秒对应的时间
	 */
	public static Date dayEnd(Date d) {
		return dayEnd(d, TimeZone.getDefault());
	}

	/**
	 * 获得当天结束前最后一毫秒的时间点<br/>
	 * 
	 * @param d
	 *            时间
	 * @param zone
	 *            时区，不同时区下“当天”的范围是不同的
	 * @return 当天最后一毫秒对应的时间
	 */
	public static Date dayEnd(Date d, TimeZone zone) {
		d = truncateToDay(d, zone);
		return new Date(d.getTime() + MILLISECONDS_IN_DAY - 1);
	}

	/**
	 * 去除 天以后的部分，仅保留年和月，实际上就是当月的开始时间 此方法不支持传入时区，需要支持多时区时请使用
	 * {@link #truncateToMonth(Date, TimeZone)}
	 * 
	 * @param defaultUtc
	 *            时间
	 * @return 时间的年和月部分，指向该月的开始
	 */
	public static final Date monthBegin(Date date) {
		return  org.apache.commons.lang3.time.DateUtils.truncate(date, Calendar.MONTH);
	}

	/**
	 * 得到该月结束的时间点
	 * 
	 * @param date
	 * @return 当月的最后1毫秒对应的时间
	 */
	public static final Date monthEnd(Date date) {
		return monthEnd(date, TimeZone.getDefault());
	}

	/**
	 * 得到该月结束的时间点
	 * 
	 * @param date
	 *            时间
	 * @param zone
	 *            时区
	 * @return 当月的最后1毫秒对应的时间
	 */
	public static final Date monthEnd(Date date, TimeZone zone) {
		if (date == null)
			return null;
		Calendar calendar = Calendar.getInstance(zone);
		calendar.setTime(date);
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 设置为当月的最后一天
		long l = calendar.getTimeInMillis();
		long left = (l + zone.getRawOffset()) % MILLISECONDS_IN_DAY;
		return new Date(l - left + MILLISECONDS_IN_DAY - 1);
	}

	/**
	 * 返回当月的最后一天
	 * 
	 * @param date
	 * @return 当月的最后一天开始的时间
	 */
	public static final Date lastDayOfMonth(Date date) {
		return lastDayOfMonth(date, TimeZone.getDefault());
	}

	/**
	 * 返回当月的最后一天
	 * 
	 * @param date
	 * @param zone
	 * @return 当月的最后一天开始的时间
	 */
	public static final Date lastDayOfMonth(Date date, TimeZone zone) {
		if (date == null)
			return null;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 设置为当月的最后一天
		long l = calendar.getTimeInMillis();
		long left = (l + zone.getRawOffset()) % MILLISECONDS_IN_DAY;
		return new Date(l - left);
	}

	/**
	 * 判断是否为某个整时间（天、时、分、秒） 取系统缺省时区
	 * <p>
	 * <tt>
	 * <li>比如08:00:00.000 就是8点整，符合TimeUnit.HOURS</li>
	 * <li>比如某天的00:00:00.000 就是0点整，符合TimeUnit.DAYS</li>
	 * <li>比如某天的01:15:00.000 就是1点15分整，符合TimeUnit.MINUTES</li>
	 * <li>比如某天的01:15:01.000 就是1点15分零1秒整，符合TimeUnit.SECONDS</li> 本方法使用系统默认时区。
	 * 
	 * @param d
	 * @param unit
	 * @return
	 */
	public static boolean isOnTime(Date d, TimeUnit unit) {
		return isOnTime(d, unit, TimeZone.getDefault());
	}

	/**
	 * 判断是否为某个整点 时间（天、时、分、秒）
	 * <p>
	 * <tt>
	 * <li>比如08:00:00.000 就是8点整，符合TimeUnit.HOURS</li>
	 * <li>比如某天的00:00:00.000 就是0点整，符合TimeUnit.DAYS</li>
	 * <li>比如某天的01:15:00.000 就是1点15分整，符合TimeUnit.MINUTES</li>
	 * <li>比如某天的01:15:01.000 就是1点15分零1秒整，符合TimeUnit.SECONDS</li>
	 * </tt>
	 * 
	 * @param d
	 * @param unit
	 *            单位
	 * @param zone
	 *            时区：不同时区的 整“天”是不一样的（即当地的零时）
	 * @return
	 */
	public static boolean isOnTime(Date d, TimeUnit unit, TimeZone zone) {
		BigInteger i = BigInteger.valueOf(d.getTime() + zone.getRawOffset());
		long result = i.mod(BigInteger.valueOf(unit.toMillis(1))).longValue();
		return result == 0;
	}

	/**
	 * 格式化时间戳 @deprecated Use @{DateFormats} @throws
	 */
	public static String formatTimeStamp(Date d) {
		if (d == null)
			return "";
		return DateFormats.TIME_STAMP_CS.get().format(d);
	}

	/**
	 * 用指定的模板格式化日期时间 这里不知道传入的Format是否线程安全，因此还是同步一次
	 * 
	 * @param d
	 * @param format
	 * @return
	 */
	public static String format(Date d, DateFormat format) {
		if (d == null)
			return "";
		synchronized (format) {
			return format.format(d);
		}
	}

	/**
	 * 用指定的模板格式化日期时间
	 * 
	 * @deprecated Use {@link jef.tools.DateFormats.TLDateFormat#format(Date)}
	 */
	public static String format(Date d, String format) {
		if (d == null)
			return "";
		DateFormat f = new SimpleDateFormat(format);
		return f.format(d);
	}

	/**
	 * 用指定的模板格式化日期时间 直接传入ThreadLocal对象，确保了线程安全
	 * 
	 * @deprecated Use {@link jef.tools.DateFormats.TLDateFormat#format(Date)}
	 *             please.
	 * @param d
	 * @param format
	 * @return
	 */
	public static String format(Date d, ThreadLocal<? extends DateFormat> format) {
		if (d == null)
			return "";
		return format.get().format(d);
	}

	/**
	 * 用指定的模板格式化到“当天”
	 * 
	 * @param d
	 * @param dateF
	 * @param timeF
	 * @return
	 * @deprecated 不推荐使用。以前为解析某个RSS站点的日期显示而设计，并不通用。
	 */
	public static String formatWithToday(Date d, DateFormat dateF, DateFormat timeF) {
		if (d == null)
			return "";
		synchronized (timeF) {
			if (isSameDay(new Date(), d)) {
				return TODAY + " " + timeF.format(d);
			} else if (isSameDay(yesterday(), d)) {
				return YESTERDAY + " " + timeF.format(d);
			} else if (isSameDay(futureDay(1), d)) {
				return TOMORROW + " " + timeF.format(d);
			} else if (isSameDay(futureDay(2), d)) {
				return TOMORROW2 + " " + timeF.format(d);
			} else {
				synchronized (dateF) {
					return dateF.format(d) + " " + timeF.format(d);
				}
			}
		}
	}

	/**
	 * 用默认的格式（中式日期）解析
	 * 
	 * @deprecated use {@code DateFormats.DATE_CS.parse(String)}
	 * @param s
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDate(String s) throws ParseException {
		if (StringUtils.isBlank(s))
			return null;
		return DateFormats.DATE_CS.get().parse(s);
	}

	/**
	 * 用默认的格式（中式日期时间）解析
	 * 
	 * @deprecated use {@code DateFormats.DATE_TIME_CS.parse(String)}
	 * @param s
	 * @return
	 * @throws ParseException
	 *             解析失败抛出
	 */
	public static Date parseDateTime(String s) throws ParseException {
		if (StringUtils.isBlank(s))
			return null;
		return DateFormats.DATE_TIME_CS.get().parse(s);
	}

	/**
	 * 用默认的格式（中式日期时间）解析
	 * 
	 * @deprecated use {@code DateFormats.DATE_TIME_CS.parse(String,Date)}
	 * @param s
	 * @param defaultValue
	 * @return
	 * @throws ParseException
	 *             如果未指定缺省值，解析失败时抛出
	 */

	public static Date parseDateTime(String s, Date defaultValue) {
		if (StringUtils.isBlank(s))
			return null;
		try {
			return DateFormats.DATE_TIME_CS.get().parse(s);
		} catch (ParseException e) {
			return defaultValue;
		}
	}

	/**
	 * 解析日期时间 非法则抛出异常
	 * 
	 * @deprecated Use @{DateFormats}
	 * @param s
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	public static Date parse(String s, ThreadLocal<? extends DateFormat> format) throws ParseException {
		if (StringUtils.isBlank(s))
			return null;
		return format.get().parse(s);
	}

	/**
	 * 解析日期时间 非法则抛出异常
	 * 
	 * @deprecated Use @{DateFormats}
	 * @Title: parse
	 */
	public static Date parse(String s, DateFormat format) throws ParseException {
		if (StringUtils.isBlank(s))
			return null;
		return format.parse(s);
	}

	/**
	 * 自动解析(猜测)日期格式 (某些特殊场景下可能解释错误) 支持:
	 * <ol>
	 * <li>中式日期(yyyy-MM-dd)</li>
	 * <li>中式日期时间 yyyy-mm-dd</li>
	 * <li>美式日期(MM/dd/yyyy)</li>
	 * <li>美式日期时间(MM/dd/yyyy HH:mm:ss)</li>
	 * <li>8位数字日期 (yyyyMMdd)</li>
	 * <li>14位数字日期时间(yyyyMMddHHmmss)</li>
	 * <li>12位数字时间(yyyyMMddHHmm)</li>
	 * </ol>
	 * 
	 * @param dateStr
	 * @return 尽可能的猜测并解析时间。如果无法解析则返回null。
	 */
	public static Date autoParse(String dateStr) {
		try {
			int indexOfDash = dateStr.indexOf('-');
			if (indexOfDash > 0) {// 按中式日期格式化(注意，首位为‘-’可能是负数日期，此处不应处理)
				if (indexOfDash == 2) {// 尝试修复仅有两位数的年
					int year = StringUtils.toInt(dateStr.substring(0, indexOfDash), -1);
					if (year >= 50) {// 当年份只有两位数时，只能猜测是19xx年还是20xx年。
						dateStr = "19" + dateStr;
					} else if (year >= 0) {
						dateStr = "20" + dateStr;
					}
				}
				if (dateStr.indexOf(':') > -1) {// 带时间
					return DateFormats.DATE_TIME_CS.get().parse(dateStr);
				} else {
					return DateFormats.DATE_CS.get().parse(dateStr);
				}
			} else if (dateStr.indexOf('/') > -1) {// 按美式日期格式化
				if (dateStr.indexOf(':') > -1) {// 带时间
					return DateFormats.DATE_TIME_US.get().parse(dateStr);
				} else {
					return DateFormats.DATE_US.get().parse(dateStr);
				}
			} else if (dateStr.length() == 8 && StringUtils.isNumeric(dateStr) && (dateStr.startsWith("19") || dateStr.startsWith("20"))) {// 按8位数字格式化
				return DateFormats.DATE_SHORT.get().parse(dateStr);
			} else if (dateStr.length() == 14 && StringUtils.isNumeric(dateStr) && (dateStr.startsWith("19") || dateStr.startsWith("20"))) {// 按14位数字格式化yyyyMMDDHHMMSS
				return DateFormats.DATE_TIME_SHORT_14.get().parse(dateStr);
			} else if (dateStr.length() == 12 && StringUtils.isNumeric(dateStr) && (dateStr.startsWith("19") || dateStr.startsWith("20"))) {// 按12位数字格式化yyyyMMDDHHMM
				return DateFormats.DATE_TIME_SHORT_12.get().parse(dateStr);
			} else if (StringUtils.isNumericOrMinus(dateStr)) {
				long value = Long.valueOf(dateStr).longValue();
				return new Date(value);
			} else {
				return null;
			}
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * 解析日期 非法返回指定缺省值
	 * 
	 * @deprecated Use @{DateFormats}
	 * @return 如果输入为空白字符串，返回defaultValue 如果解析中出现异常，返回defaultValue
	 * @throws不会抛出ParseException
	 */
	public static Date parse(String s, DateFormat format, Date defaultValue) {
		if (StringUtils.isBlank(s))
			return defaultValue;
		try {
			return format.parse(s);
		} catch (ParseException e) {
			log.error("error",e);
			return defaultValue;
		}
	}

	/**
	 * 解析日期 非法返回指定缺省值
	 * 
	 * @deprecated Use @{DateFormats}
	 * @return 如果输入为空白字符串，返回defaultValue 如果解析中出现异常，返回defaultValue
	 * @throws不会抛出ParseException
	 */
	public static Date parse(String s, ThreadLocal<? extends DateFormat> format, Date defaultValue) {
		if (StringUtils.isBlank(s))
			return defaultValue;
		try {
			return format.get().parse(s);
		} catch (ParseException e) {
			log.error("error",e);
			return defaultValue;
		}
	}

	/**
	 * 解析日期 非法返回指定缺省值
	 * 
	 * @deprecated Use @{DateFormats}
	 * @return 如果输入为空白字符串，返回defaultValue 如果解析中出现异常，返回defaultValue
	 * @throws不会抛出ParseException
	 */
	public static Date parse(String s, String format, Date defaultValue) throws ParseException {
		if (StringUtils.isBlank(s))
			return null;
		try {
			return new SimpleDateFormat(format).parse(s);
		} catch (ParseException e) {
			return defaultValue;
		}
	}

	/**
	 * return true if the date 1 and date 2 is on the same day
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static boolean isSameDay(Date d1, Date d2) {
		return isSameDay(d1, d2, TimeZone.getDefault());
	}

	/**
	 * return true if the date 1 and date 2 is on the same day
	 * 
	 * @param d1
	 * @param d2
	 * @param zone
	 *            时区，不同地区对“当天”的范围是不一样的
	 * @return
	 */
	public static boolean isSameDay(Date d1, Date d2, TimeZone zone) {
		if (d1 == null && d2 == null)
			return true;
		if (d1 == null || d2 == null)
			return false;
		return truncateToDay(d1, zone).getTime() == truncateToDay(d2, zone).getTime();
	}

	/**
	 * 是否同一个月内
	 * 
	 * @param d1
	 *            日期1
	 * @param d2
	 *            日期2
	 * @return
	 */
	public static boolean isSameMonth(Date d1, Date d2) {
		return truncateToMonth(d1).getTime() == truncateToMonth(d2).getTime();
	}

	/**
	 * 是否同一个月内
	 * 
	 * @param d1
	 * @param d2
	 * @param zone
	 *            时区，不同地区对“当天”的范围是不一样的
	 * @return
	 */
	public static boolean isSameMonth(Date d1, Date d2, TimeZone zone) {
		return truncateToMonth(d1, zone).getTime() == truncateToMonth(d2, zone).getTime();
	}

	/**
	 * 得到年份
	 * @param d 时间
	 * @return 年份
	 */
	public static int getYear(Date d) {
		return getYear(d, TimeZone.getDefault());
	}
	
	/**
	 * 得到年份
	 * 
	 * @param d 时间
	 * @param zone 时区
	 * @return
	 */
	public static int getYear(Date d, TimeZone zone) {
		if (d == null)
			return 0;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}

	/**
	 * 得到月份 (1~12)
	 * 
	 * @param d
	 * @return 月份，范围 [1,12]。
	 */
	public static int getMonth(Date d) {
		return getMonth(d, TimeZone.getDefault());
	}
	
	/**
	 * 得到月份 (1~12)
	 * 
	 * @param d
	 * @param zone
	 * @return 月份，范围 [1,12]。
	 */
	public static int getMonth(Date d, TimeZone zone) {
		if (d == null)
			return 0;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		return c.get(Calendar.MONTH) + 1;
	}

	/**
	 * 得到当月时的天数
	 * 
	 * @param d
	 * @return 当月内的日期：天
	 */
	public static int getDay(Date d) {
		return getDay(d, TimeZone.getDefault());
	}
	
	/**
	 * 得到当月时的天数
	 * 
	 * @param d
	 * @param zone
	 * @return
	 */
	public static int getDay(Date d, TimeZone zone) {
		if (d == null)
			return 0;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		return c.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 得到该时间是星期几
	 * @param d 日期
	 * @return  0: 周日,1~6 周一到周六<br>
	 *         注意返回值和Calendar定义的sunday等常量不同，而是星期一返回数字1，这是为更符合中国人的习惯。
	 *         如果传入null，那么返回-1表示无效。
	 */
	public static int getWeekDay(Date d) {
		return getWeekDay(d, TimeZone.getDefault());
	}
	
	/**
	 * 得到该时间是星期几
	 * 
	 * @param d 日期
	 * @param zone 时区
	 * @return 0: 周日,1~6 周一到周六<br>
	 *         注意返回值和Calendar定义的sunday等常量不同，而是星期一返回数字1，这是为更符合中国人的习惯。
	 *         如果传入null，那么返回-1表示无效。
	 */
	public static int getWeekDay(Date d, TimeZone zone) {
		if (d == null)
			return -1;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		return c.get(Calendar.DAY_OF_WEEK) - 1;
	}

	/**
	 * 返回传入日期所在周的第一天。<br>
	 * 按中国和大部分欧洲习惯，<strong>星期一 作为每周的第一天</strong>
	 * <p>
	 * A Week is Monday to Sunday
	 * <p>
	 * @param date
	 * @return The first day of the week. Note: only the date was adjusted. time
	 *         is kept as original.
	 */
	public static Date weekBegin(Date date, TimeZone zone) {
		return toWeekDayCS(date, 1, zone);
	}

	/**
	 * 返回传入日期所在周的最后一天。<br>
	 * 按中国和大部分欧洲习惯，星期天 作为每周的最后一天
	 * <p>
	 * A Week is Monday to Sunday
	 * <p>
	 * 
	 * @param date
	 * @return The last day of the week. Note: only the date was adjusted. time
	 *         is kept as original.
	 */
	public static Date weekEnd(Date date, TimeZone zone) {
		return toWeekDayCS(date, 7, zone);
	}
	

	/**
	 * 返回传入日期所在周的第一天。<br>
	 * 按天主教习惯，星期天 作为每周的第一天
	 * <p>
	 * A Week is Sunday to Saturday
	 * <p>
	 * 
	 * @param date
	 * @return The first day of the week. Note: only the date was adjusted. time
	 *         is kept as original.
	 */
	public static Date weekBeginUS(Date date, TimeZone zone) {
		return toWeekDayUS(date, 0, zone);
	}

	/**
	 * 返回传入日期所在周的最后一天。 按天主教习惯，星期六 作为每周的最后一天
	 * <p>
	 * A Week is Sunday to Saturday
	 * <p>
	 * 
	 * @param date
	 * @return The last day of the week. Note: only the date was adjusted. time
	 *         is kept as original.
	 */
	public static Date weekEndUS(Date date, TimeZone zone) {
		return toWeekDayUS(date, 6, zone);
	}

	private static Date toWeekDayCS(Date date, int expect, TimeZone zone) {
		int day = getWeekDay(date, zone);
		if (day == 0)
			day = 7;
		return adjustDate(date, 0, 0, expect - day);
	}

	private static Date toWeekDayUS(Date date, int expect, TimeZone zone) {
		int day = getWeekDay(date, zone);
		return adjustDate(date, 0, 0, expect - day);
	}


	/**
	 * 得到小时数：24小时制
	 * 
	 * @param d
	 * @return 24小时制的小时数
	 */
	public static int getHour(Date d) {
		return getHour(d, TimeZone.getDefault());
	}
	
	/**
	 * 得到小时数：24小时制
	 * 
	 * @param d
	 * @param zone  时区
	 * 
	 * @return 24小时制的小时数
	 */
	public static int getHour(Date d,TimeZone zone) {
		if (d == null)
			return 0;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		return c.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 获得该时间的分数
	 * 
	 * @param d
	 * @return
	 */
	public static int getMinute(Date d) {
		if (d == null)
			return 0;
		final Calendar c = new GregorianCalendar();
		c.setTime(d);
		return c.get(Calendar.MINUTE);
	}

	/**
	 * 获得该时间的秒数
	 * 
	 * @param d
	 * @return
	 */
	public static int getSecond(Date d) {
		if (d == null)
			return 0;
		final Calendar c = new GregorianCalendar();
		c.setTime(d);
		return c.get(Calendar.SECOND);
	}

	/**
	 * 以数组的形式，返回年、月、日三个值
	 * 
	 * @param d
	 * @return int[]{year, month, day}，其中month的范围是1~12。
	 * 
	 */
	public static int[] getYMD(Date d) {
		return getYMD(d, TimeZone.getDefault());
	}
	/**
	 * 以数组的形式，返回年、月、日三个值
	 * 
	 * @param d
	 * @param zone 时区
	 * @return int[]{year, month, day}，其中month的范围是1~12。
	 * 
	 */
	public static int[] getYMD(Date d, TimeZone zone) {
		int[] ymd = new int[3];
		if (d == null)
			return ymd;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		ymd[0] = c.get(Calendar.YEAR);
		ymd[1] = c.get(Calendar.MONTH) + 1;
		ymd[2] = c.get(Calendar.DAY_OF_MONTH);
		return ymd;
	}

	/**
	 * 以数组的形式，返回时、分、秒 三个值
	 * 
	 * @param d
	 * @return
	 */
	public static int[] getHMS(Date d) {
		return getHMS(d, TimeZone.getDefault());
	}
	
	/**
	 * 以数组的形式，返回时、分、秒 三个值
	 * 
	 * @param d
	 * @param zone 时区
	 * @return
	 */
	public static int[] getHMS(Date d, TimeZone zone) {
		int[] hms = new int[3];
		if (d == null)
			return hms;
		final Calendar c = new GregorianCalendar(zone);
		c.setTime(d);
		hms[0] = c.get(Calendar.HOUR_OF_DAY);
		hms[1] = c.get(Calendar.MINUTE);
		hms[2] = c.get(Calendar.SECOND);
		return hms;
	}

	/**
	 * 在指定日期上减去1毫秒
	 * 
	 * @throws
	 */
	public static void prevMillis(Date d) {
		d.setTime(d.getTime() - 1);
	}

	/**
	 * 加指定毫秒
	 */
	public static void addMillis(Date d, long value) {
		d.setTime(d.getTime() + value);
	}

	/**
	 * 加指定秒
	 */
	public static void addSec(Date d, long value) {
		d.setTime(d.getTime() + TimeUnit.SECONDS.toMillis(value));
	}

	/**
	 * 加指定分
	 */
	public static void addMinute(Date d, int value) {
		d.setTime(d.getTime() + TimeUnit.MINUTES.toMillis(value));
	}

	/**
	 * 加指定小时
	 */
	public static void addHour(Date d, int value) {
		d.setTime(d.getTime() + TimeUnit.HOURS.toMillis(value));
	}

	/**
	 * 加指定天
	 */
	public static void addDay(Date d, int value) {
		d.setTime(d.getTime() + TimeUnit.DAYS.toMillis(value));
	}

	/**
	 * 加指定月
	 */
	public static void addMonth(Date d, int value) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.MONTH, value);
		d.setTime(c.getTime().getTime());
	}

	/**
	 * 加指定年
	 */
	public static void addYear(Date d, int value) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.YEAR, value);
		d.setTime(c.getTime().getTime());
	}

	/**
	 * 
	 * 在原日期上增加指定的 年、月、日数 。这个方法不会修改传入的Date对象，而是一个新的Date对象
	 * 
	 * @param date
	 *            原日期时间
	 * @param year
	 *            增加的年（可为负数）
	 * @param month
	 *            增加的月（可为负数）
	 * @param day
	 *            增加的日（可为负数）
	 * @return 调整后的日期（新的日期对象）
	 */
	public static Date adjustDate(Date date, int year, int month, int day) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.YEAR, year);
		c.add(Calendar.MONTH, month);
		c.add(Calendar.DAY_OF_YEAR, day);
		return c.getTime();
	}

	/**
	 * 在原日期上增加指定的 时、分、秒数 。这个方法不会修改传入的Date对象，而是一个新的Date对象
	 * 
	 * @param date
	 *            原日期时间
	 * @param hour
	 *            增加的时（可为负数）
	 * @param minute
	 *            增加的分（可为负数）
	 * @param second
	 *            增加的秒（可为负数）
	 * @return 调整后的日期时间（新的日期对象）
	 */
	public static Date adjustTime(Date date, int hour, int minute, int second) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.HOUR, hour);
		c.add(Calendar.MINUTE, minute);
		c.add(Calendar.SECOND, second);
		return c.getTime();
	}

	/**
	 * 在原日期上调整指定的毫秒并返回新对象。这个方法不会修改传入的Date对象，而是一个新的Date对象。
	 * 
	 * @param date
	 *            原日期时间
	 * @param mills
	 *            毫秒数（可为负数）
	 * @return 调整后的日期时间（新的日期对象）
	 * 
	 */
	public static Date adjust(Date date, long mills) {
		return new Date(date.getTime() + mills);
	}

	/**
	 * 获取一个日期对象(java.util.Date)
	 * 
	 * @param year
	 *            格式为：2004
	 * @param month
	 *            从1开始
	 * @param date
	 *            从1开始
	 * @return 要求的日期
	 * @deprecated use {@link #get(int, int, int)} instead.
	 */
	public static final Date getDate(int year, int month, int date) {
		return get(year, month, date);
	}

	/**
	 * 获取一个日期对象(java.util.Date)
	 * 
	 * @param year
	 *            格式为：2004
	 * @param month
	 *            从1开始
	 * @param date
	 *            从1开始
	 * @return 要求的日期
	 */
	public static final Date get(int year, int month, int date) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month - 1, date, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取一个日期对象(java.sql.Date)
	 * 
	 * @param year
	 * @param month
	 * @param date
	 * @return
	 */
	public static final java.sql.Date getSqlDate(int year, int month, int date) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month - 1, date, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return new java.sql.Date(calendar.getTime().getTime());
	}

	/**
	 * 获得一个UTC时间
	 * 
	 * @param year
	 * @param month
	 * @param date
	 * @param hour
	 * @param minute
	 * @param second
	 * @return
	 */
	public static final Date getUTC(int year, int month, int date, int hour, int minute, int second) {
		Calendar calendar = Calendar.getInstance(TimeZones.UTC);
		calendar.set(year, month - 1, date, hour, minute, second);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取一个时间对象
	 * 
	 * @param year
	 *            格式为：2004
	 * @param month
	 *            从1开始
	 * @param date
	 *            从1开始
	 * @param hour
	 *            小时(0-24)
	 * @param minute
	 *            分(0-59)
	 * @param second
	 *            秒(0-59)
	 * @return Date
	 */
	public static final Date get(int year, int month, int date, int hour, int minute, int second) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month - 1, date, hour, minute, second);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取一个时间对象
	 * 
	 * @param year
	 *            格式为：2004
	 * @param month
	 *            从1开始
	 * @param date
	 *            从1开始
	 * @param hour
	 *            小时(0-24)
	 * @param minute
	 *            分(0-59)
	 * @param second
	 *            秒(0-59)
	 * @return Date
	 * @deprecated Use {@link #get(int, int, int, int, int, int)} instead.
	 */
	public static final Date getDate(int year, int month, int date, int hour, int minute, int second) {
		return get(year, month, date, hour, minute, second);
	}

	/**
	 * 返回两个时间相差的天数
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static final int daySubtract(Date a, Date b) {
		return daySubtract(a, b, TimeZone.getDefault());
	}
	
	/**
	 * 返回在指定时区下，两个时间之间相差的天数
	 * @param a
	 * @param b
	 * @param zone
	 * @return
	 */
	public static final int daySubtract(Date a, Date b, TimeZone zone) {
		int offset =zone.getRawOffset();
		int date = (int) (((a.getTime() + offset) / MILLISECONDS_IN_DAY - (b.getTime() + offset) / MILLISECONDS_IN_DAY));
		return date;
	}

	/**
	 * 返回两个时间相差多少秒
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static final long secondSubtract(Date a, Date b) {
		return ((a.getTime() - b.getTime()) / 1000);
	}
	

	/**
	 * 得到该日期所在月包含的天数
	 * 
	 * @param date
	 * @return 2月返回28或29，1月返回31
	 */
	public static final int getDaysInMonth(Date date) {
		return getDaysInMonth(date, TimeZone.getDefault());
	}

	/**
	 * 得到该日期所在月包含的天数
	 * 
	 * @param date
	 * @param zone
	 * @return 2月返回28或29，1月返回31
	 */
	public static final int getDaysInMonth(Date date, TimeZone zone) {
		Assert.notNull(date);
		Calendar calendar = Calendar.getInstance(zone);
		calendar.setTime(date);
		int day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		return day;
	}

	/**
	 * 格式化时间段
	 * 
	 * @param second
	 * @return
	 */
	public static String formatTimePeriod(long second) {
		return formatTimePeriod(second, null, Locale.getDefault());
	}

	/**
	 * 将秒数转换为时长描述
	 * 
	 * @param second
	 * @param maxUnit
	 *            :最大单位 0自动 3天4小时 5分钟
	 * @return
	 */
	public static String formatTimePeriod(long second, TimeUnit maxUnit, Locale locale) {
		if (locale == null)
			locale = Locale.getDefault();
		if (maxUnit == null) {
			maxUnit = TimeUnit.DAYS;
			if (second < SECONDS_IN_DAY)
				maxUnit = TimeUnit.HOURS;
			if (second < SECONDS_IN_HOUR)
				maxUnit = TimeUnit.MINUTES;
			if (second < SECONDS_IN_MINITE)
				maxUnit = TimeUnit.SECONDS;
		}
		StringBuilder sb = new StringBuilder();
		if (maxUnit.ordinal() >= TimeUnit.DAYS.ordinal()) {
			int days = (int) (second / SECONDS_IN_DAY);
			if (days > 0) {
				sb.append(days);
				if (Locale.US == locale) {
					sb.append("days ");
				} else {
					sb.append("天");
				}
				second = second - SECONDS_IN_DAY * days;
			}
		}
		if (maxUnit.ordinal() >= TimeUnit.HOURS.ordinal()) {
			int hours = (int) (second / SECONDS_IN_HOUR);
			if (hours > 0) {
				sb.append(hours);
				if (Locale.US == locale) {
					sb.append("hours ");
				} else {
					sb.append("小时");
				}
				second = second - SECONDS_IN_HOUR * hours;
			}
		}
		if (maxUnit.ordinal() >= TimeUnit.MINUTES.ordinal()) {
			int min = (int) (second / SECONDS_IN_MINITE);
			if (min > 0) {
				sb.append(min);
				if (Locale.US == locale) {
					sb.append("minutes ");
				} else {
					sb.append("分");
				}
				second = second - SECONDS_IN_MINITE * min;
			}
		}
		if (second > 0) {
			if (Locale.US == locale) {
				sb.append(second).append("seconds");
			} else {
				sb.append(second).append("秒");
			}
		}
		return sb.toString();
	}

	/**
	 * 返回“昨天”的同一时间
	 * 
	 * @return
	 */
	public static Date yesterday() {
		return futureDay(-1);
	}

	/**
	 * 返回未来多少天的同一时间
	 * 
	 * @param i
	 * @return
	 */
	public static Date futureDay(int i) {
		return new Date(System.currentTimeMillis() + (long) MILLISECONDS_IN_DAY * i);
	}

	/**
	 * 将系统格式时间(毫秒)转换为文本格式
	 * 
	 * @param millseconds
	 * @return
	 */
	public static Optional<String> format(long millseconds) {
		return DateFormats.DATE_TIME_CS.format2(new Date(millseconds));
	}

	/**
	 * 月份遍历器 指定两个日期，遍历两个日期间的所有月份。（包含开始时间和结束时间所在的月份）
	 * 
	 * @param includeStart
	 * @param includeEnd
	 * @return
	 */
	public static Iterable<Date> monthIterator(Date includeStart, Date includeEnd) {
		return new TimeIterable(includeStart, includeEnd, Calendar.MONTH).setIncludeEndDate(true);
	}

	/**
	 * 日遍历器 指定两个时间，遍历两个日期间的所有天。（包含开始时间和结束时间所在的天）
	 * 
	 * @param includeStart
	 *            the begin date.(include)
	 * @param excludeEnd
	 *            the end date(include)
	 * @return A Iterable object that can iterate the date.
	 */
	public static Iterable<Date> dayIterator(final Date includeStart, final Date includeEnd) {
		return new TimeIterable(includeStart, includeEnd, Calendar.DATE).setIncludeEndDate(true);
	}

	/**
	 * 返回今天
	 * 
	 * @return the begin of today.
	 */
	public static Date today() {
		return  org.apache.commons.lang3.time.DateUtils.truncate(new Date(), Calendar.DATE);
	}

	/**
	 * 返回今天
	 * 
	 * @return
	 */
	public static java.sql.Date sqlToday() {
		return new java.sql.Date( org.apache.commons.lang3.time.DateUtils.truncate(new Date(), Calendar.DATE).getTime());
	}

	/**
	 * 返回现在
	 * 
	 * @return
	 */
	public static java.sql.Timestamp sqlNow() {
		return new java.sql.Timestamp(System.currentTimeMillis());
	}

	/**
	 * 返回现在时间
	 * 
	 * @return the current date time.
	 */
	public static Date now() {
		return new Date();
	}

	/**
	 * 指定时间是否为一天的开始
	 * 
	 * @param date
	 * @return true if the date is the begin of day.
	 */
	public static boolean isDayBegin(Date date, TimeZone zone) {
		Date d1 = truncateToDay(date, zone);
		return d1.getTime() == date.getTime();
	}

	/**
	 * Convert to Instance
	 * 
	 * @see Instant
	 * @param date
	 *            java.util.Date
	 * @return instant
	 */
	public static Instant toInstant(Date date) {
		return date == null ? null : date.toInstant();
	}

	/**
	 * Convert LocalDate to jud
	 * 
	 * @param date
	 *            LocalDate
	 * @return java.util.Date
	 */
	public static Date fromLocalDate(LocalDate date) {
		return date == null ? null : Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Convert LocalTime to jud.
	 * 
	 * @param time
	 *            LocalTime
	 * @return java.util.Date
	 */
	public static Date fromLocalTime(LocalTime time) {
		return time == null ? null : Date.from(LocalDateTime.of(LocalDate.now(), time).atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts LocalDateTime to java.util.Date (null safety)
	 * 
	 * @param LocalDateTime
	 * @return java.util.Date
	 */
	public static Date fromLocalDateTime(LocalDateTime value) {
		return value == null ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts java.sql.Date to LocalDate (null safety)
	 * 
	 * @param java.sql.Date
	 * @return LocalDate
	 */
	public static LocalDate toLocalDate(java.sql.Date date) {
		return date == null ? null : date.toLocalDate();
	}

	/**
	 * Converts java.util.Date to LocalDate (null safety)
	 * 
	 * @param date
	 *            java.util.Date
	 * @return LocalDate
	 */
	public static LocalDate toLocalDate(java.util.Date date) {
		return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
	}

	/**
	 * Converts Time to LocalTime (null safety)
	 * 
	 * @param time
	 * @return LocalTime
	 */
	public static LocalTime toLocalTime(java.sql.Time time) {
		return time == null ? null : time.toLocalTime();
	}

	/**
	 * Converts Timestamp to LocalTime (null safety)
	 * 
	 * @param ts
	 *            Timestamp
	 * @return LocalTime
	 */
	public static LocalTime toLocalTime(java.sql.Timestamp ts) {
		return ts == null ? null : ts.toLocalDateTime().toLocalTime();
	}

	/**
	 * Converts java.util.Date to LocalTime (null safety)
	 * 
	 * @param defaultUtc
	 * @return
	 */
	public static LocalTime toLocalTime(java.util.Date date) {
		return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalTime();
	}

	/**
	 * Convert Timestamp to LocalDateTime (null safety)
	 * 
	 * @param ts
	 *            Timestamp
	 * @return LocalDateTime
	 */
	public static LocalDateTime toLocalDateTime(java.sql.Timestamp ts) {
		return ts == null ? null : ts.toLocalDateTime();
	}

	/**
	 * Convert java.util.Date to LocalDateTime (null safety)
	 * 
	 * @param date
	 *            date
	 * @return
	 */
	public static LocalDateTime toLocalDateTime(java.util.Date date) {
		return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	}

	/**
	 * Convert java.util.Date to YearMonth (null safety)
	 * 
	 * @param date
	 * @return
	 */
	public static YearMonth toYearMonth(java.util.Date date) {
		return date == null ? null : YearMonth.from(date.toInstant());
	}

	/**
	 * Convert java.util.Date to MonthDay (null safety)
	 * 
	 * @param date
	 * @return
	 */
	public static MonthDay toMonthDay(java.util.Date date) {
		return date == null ? null : MonthDay.from(date.toInstant());
	}

	/**
	 * Converts LocalDate to Time (null safety)
	 * 
	 * @param date
	 *            LocalDate
	 * @return java.sql.Date
	 */
	public static java.sql.Date toSqlDate(LocalDate date) {
		return date == null ? null : java.sql.Date.valueOf(date);
	}

	/**
	 * Converts LocalTime to Time (null safety)
	 * 
	 * @param time
	 *            LocalTime
	 * @return java.sql.Time
	 */
	public static java.sql.Time toSqlTime(LocalTime time) {
		return time == null ? null : java.sql.Time.valueOf(time);
	}

	/**
	 * 转换为java.sql.timestamp
	 * 
	 * @param d
	 * @return
	 */
	public static Timestamp toSqlTimeStamp(Date d) {
		if (d == null)
			return null;
		return new java.sql.Timestamp(d.getTime());
	}

	/**
	 * Converts LocalDateTime to Timestamp (null safety)
	 * 
	 * @param time
	 *            LocalDateTime
	 * @return Timestamp
	 */
	public static java.sql.Timestamp toSqlTimeStamp(LocalDateTime time) {
		return time == null ? null : java.sql.Timestamp.valueOf(time);
	}

	/**
	 * Converts instant to Timestamp (null safety)
	 * 
	 * @param instant
	 *            Instant
	 * @return java.sql.Timestamp
	 */
	public static java.sql.Timestamp toSqlTimeStamp(Instant instant) {
		return instant == null ? null : java.sql.Timestamp.from(instant);
	}

	/**
	 * Converts instant to JUD (null safety)
	 * 
	 * @param instant
	 * @return java.util.Date
	 */
	public static Date fromInstant(Instant instant) {
		return instant == null ? null : Date.from(instant);
	}

	/**
	 * Converts LocalTime to Timestamp (null safety)
	 * 
	 * @param localTime
	 *            LocalTime
	 * @return Timestamp
	 */
	public static Timestamp toSqlTimeStamp(LocalTime localTime) {
		return localTime == null ? null : java.sql.Timestamp.valueOf(LocalDateTime.of(LocalDate.now(), localTime));
	}

	/**
	 * 转换为java.sql.Date
	 * 
	 * @param d
	 * @return
	 */
	public static java.sql.Date toSqlDate(Date d) {
		if (d == null)
			return null;
		return new java.sql.Date(d.getTime());
	}

	/**
	 * 转换为Sql的Time对象（不含日期）
	 * 
	 * @param date
	 * @return
	 */
	public static java.sql.Time toSqlTime(Date date) {
		if (date == null)
			return null;
		return new java.sql.Time(date.getTime());
	}

	/**
	 * 从java.sql.Date转换到java.util.Date
	 * 
	 * @param d
	 * @return
	 */
	public static Date fromSqlDate(java.sql.Date d) {
		if (d == null)
			return null;
		return new Date(d.getTime());
	}

	/**
	 * 取得截断后的日期/时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
	 * 
	 * @param start
	 * @param unit
	 *            时间单位，使用Calendar中的Field常量。
	 * @return 截断后的日期/时间
	 * @see Calendar
	 */
	public static Date getTruncated(Date start, int unit) {
		switch (unit) {
		case Calendar.SECOND:
			return truncateToSecond(start);
		case Calendar.MINUTE:
			return truncateToMinute(start);
		case Calendar.HOUR:
			return truncateToHour(start);
		case Calendar.DATE:
		case Calendar.DAY_OF_YEAR:
			return truncateToDay(start);
		case Calendar.MONTH:
			return truncateToMonth(start);
		case Calendar.YEAR:
			return truncateToYear(start);
		default:
			throw new UnsupportedOperationException("Unsupported unit:" + unit);
		}
	}
}
