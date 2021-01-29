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
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import jef.common.log.LogUtil;
import jef.tools.support.TimeIterable;

/**
 * utils for date.
 * 
 */
public abstract class DateUtils {
    public static final int SECONDS_IN_DAY = 86400;
    public static final int SECONDS_IN_HOUR = 3600;
    public static final int SECONDS_IN_MINITE = 60;
    public static final int MILLISECONDS_IN_DAY = 86400000;
    public static final int MILLISECONDS_IN_HOUR = 3600000;

    private static String TODAY = "\u4eca\u5929";
    private static String YESTERDAY = "\u6628\u5929";
    private static String TOMORROW = "\u660e\u5929";
    private static String TOMORROW2 = "\u540e\u5929";

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
     * 格式化日期为中式格式
     * 
     * @param d
     * @return
     */
    public static String formatDate(Date d) {
        if (d == null)
            return "";
        return DateFormats.DATE_CS.get().format(d);
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
     * 格式化为日期+时间（中式）
     * 
     * @param d
     * @return
     */
    public static String formatDateTime(Date d) {
        if (d == null)
            return "";
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
     * 时间单位
     * 
     * @author Administrator
     */
    public static enum TimeUnit {
        DAY(86400000L), HOUR(3600000L), MINUTE(60000L), SECOND(1000L);

        private long ms;

        TimeUnit(long millseconds) {
            this.ms = millseconds;
        }

        public long getMs() {
            return ms;
        }
    }

    /**
     * 取得截断后日期/时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
     * 
     * @param d
     *            时间
     * @param field
     *            要保留到的field. Calendar类的常量
     * @return 截断后的时间
     */
    public final static void truncate(Date d, int field) {
        d.setTime(org.apache.commons.lang3.time.DateUtils.truncate(d, field).getTime());
    }

    /**
     * 取得截断后日期/时间。
     * 
     * @param d
     *            时间
     * @param field
     *            要保留到的field. Calendar类的常量
     * @return 截断后的时间
     */
    public final static Date getTruncated(Date d, int field) {
        return org.apache.commons.lang3.time.DateUtils.truncate(d, field);
    }

    /**
     * 取得截断后的日期/时间。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。
     * 
     * @param date
     *            时间
     * @param field
     *            要保留到的field. Calendar类的常量
     * @return 截断后的时间
     */
    public final static void truncate(Calendar date, int field) {
        date.setTimeInMillis(org.apache.commons.lang3.time.DateUtils.truncate(date, field).getTimeInMillis());
    }

    /**
     * 取得截断后的日期/时间。
     * 
     * @param date
     *            时间
     * @param field
     *            要保留到的field. Calendar类的常量
     * @return 截断后的时间
     */
    public final static Calendar getTruncated(Calendar date, int field) {
        return org.apache.commons.lang3.time.DateUtils.truncate(date, field);
    }

    /**
     * 取得截断后的日期。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。 和{@link #dayBegin(Date)}相同
     * 
     * @param d
     *            时间
     * @return 仅日期
     * @see #dayBegin(Date)
     */
    public final static void truncate(Date d) {
        d.setTime(org.apache.commons.lang3.time.DateUtils.truncate(d, Calendar.DATE).getTime());
    }

    /**
     * 取得截断后的日期。注意这个方法不会修改传入的日期时间值，而是创建一个新的对象并返回。 和{@link #dayBegin(Date)}相同
     * 
     * @param d
     *            时间
     * @return 仅日期
     * @see #dayBegin(Date)
     */
    public final static Date getTruncated(Date d) {
        return org.apache.commons.lang3.time.DateUtils.truncate(d, Calendar.DATE);
    }

    /**
     * 获取天开始的瞬间时间点
     * 
     * @param d
     *            时间点
     * @return 仅日期
     * @see #truncate(Date)
     */
    public static Date dayBegin(Date d) {
        return org.apache.commons.lang3.time.DateUtils.truncate(d, Calendar.DATE);
    }

    /**
     * 获得当天结束前最后一毫秒的时间点
     */
    public static Date dayEnd(Date d) {
        d = org.apache.commons.lang3.time.DateUtils.truncate(d, Calendar.DATE);
        return new Date(d.getTime() + MILLISECONDS_IN_DAY - 1);
    }

    /**
     * 去除 天以后的部分，仅保留年和月，实际上就是当月的开始时间
     * 
     * @param d
     *            时间
     * @return 时间的年和月部分，指向该月的开始
     */
    public static final Date monthBegin(Date date) {
        return org.apache.commons.lang3.time.DateUtils.truncate(date, Calendar.MONTH);
    }

    /**
     * 得到当月结束的时间点
     * 
     * @param date
     * @return 当月的最后1毫秒
     */
    public static final Date monthEnd(Date date) {
        if (date == null)
            return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 设置为当月的最后一天
        calendar = org.apache.commons.lang3.time.DateUtils.truncate(calendar, Calendar.DATE);// 去除时分秒
        Date d = calendar.getTime();
        d.setTime(d.getTime() + MILLISECONDS_IN_DAY - 1); // 调整到当天的最后1毫秒
        return d;
    }

    /**
     * 返回当月的最后一天
     * 
     * @param date
     * @return 当月的最后一天
     */
    public static final Date lastDayOfMonth(Date date) {
        if (date == null)
            return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 设置为当月的最后一天
        calendar = org.apache.commons.lang3.time.DateUtils.truncate(calendar, Calendar.DATE);// 去除时分秒
        return calendar.getTime();
    }

    /**
     * 判断是否为某个整时间（天、时、分、秒）
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
        long result = i.mod(BigInteger.valueOf(unit.ms)).longValue();
        return result == 0;
    }

    /**
     * 判断是否为某个整时间（天、时、分、秒） 取系统缺省时区
     * 
     * @param d
     * @param unit
     * @return
     */
    public static boolean isOnTime(Date d, TimeUnit unit) {
        return isOnTime(d, unit, TimeZone.getDefault());
    }

    /**
     * 格式化时间戳
     * 
     * @throws
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
     * @param d
     * @param format
     * @return
     */
    public static String format(Date d, ThreadLocal<DateFormat> format) {
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
     * @param s
     * @param format
     * @return
     * @throws ParseException
     */
    public static Date parse(String s, ThreadLocal<DateFormat> format) throws ParseException {
        if (StringUtils.isBlank(s))
            return null;
        return format.get().parse(s);
    }

    /**
     * 解析日期时间 非法则抛出异常
     * 
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
     * @return 如果输入为空白字符串，返回defaultValue 如果解析中出现异常，返回defaultValue
     * @throws不会抛出ParseException
     */
    public static Date parse(String s, DateFormat format, Date defaultValue) {
        if (StringUtils.isBlank(s))
            return defaultValue;
        try {
            return format.parse(s);
        } catch (ParseException e) {
            LogUtil.exception(e);
            return defaultValue;
        }
    }

    /**
     * 解析日期 非法返回指定缺省值
     * 
     * @return 如果输入为空白字符串，返回defaultValue 如果解析中出现异常，返回defaultValue
     * @throws不会抛出ParseException
     */
    public static Date parse(String s, ThreadLocal<DateFormat> format, Date defaultValue) {
        if (StringUtils.isBlank(s))
            return defaultValue;
        try {
            return format.get().parse(s);
        } catch (ParseException e) {
            LogUtil.exception(e);
            return defaultValue;
        }
    }

    /**
     * 解析日期 非法返回指定缺省值
     * 
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
        if (d1 == null && d2 == null)
            return true;
        if (d1 == null || d2 == null)
            return false;
        return org.apache.commons.lang3.time.DateUtils.isSameDay(d1, d2);
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
        return getTruncated(d1, Calendar.MONTH).getTime() == getTruncated(d2, Calendar.MONTH).getTime();
    }

    /**
     * 得到年份
     * 
     * @param d
     * @return
     */
    public static int getYear(Date d) {
        if (d == null)
            return 0;
        final Calendar c = new GregorianCalendar();
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
        if (d == null)
            return 0;
        final Calendar c = new GregorianCalendar();
        c.setTime(d);
        return c.get(Calendar.MONTH) + 1;
    }

    /**
     * 得到当月时的天数
     * 
     * @param d
     * @return
     */
    public static int getDay(Date d) {
        if (d == null)
            return 0;
        final Calendar c = new GregorianCalendar();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 得到当天是星期几
     * 
     * @param d
     * @return 0: 周日,1~6 周一到周六
     *         注意返回值和Calendar定义的sunday等常量不同，而是星期一返回数字1。这是为更符合中国人的习惯。
     *         如果传入null，那么返回-1表示无效。
     */
    public static int getWeekDay(Date d) {
        if (d == null)
            return -1;
        final Calendar c = new GregorianCalendar();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_WEEK) - 1;
    }

    /**
     * 返回传入日期所在周的第一天。<br>
     * 按天主教习惯，星期天 作为每周的第一天
     * <p>
     * A Week is Sunday ~ Saturday
     * <p>
     * 
     * @param date
     * @return The first day of the week. Note: only the date was adjusted. time
     *         is kept as original.
     */
    public static Date weekBeginUS(Date date) {
        return toWeekDayUS(date, 0);
    }

    /**
     * 返回传入日期所在周的最后一天。 按天主教习惯，星期六 作为每周的最后一天
     * <p>
     * A Week is Sunday ~ Saturday
     * <p>
     * 
     * @param date
     * @return The last day of the week. Note: only the date was adjusted. time
     *         is kept as original.
     */
    public static Date weekEndUS(Date date) {
        return toWeekDayUS(date, 6);
    }

    /**
     * 返回传入日期所在周的第一天。<br>
     * 按中国和大部分欧洲习惯，星期一 作为每周的第一天
     * <p>
     * A Week is Monday ~ Sunday
     * <p>
     * 
     * @param date
     * @return The first day of the week. Note: only the date was adjusted. time
     *         is kept as original.
     */
    public static Date weekBegin(Date date) {
        return toWeekDayCS(date, 1);
    }

    /**
     * 返回传入日期所在周的最后一天。<br>
     * 按中国和大部分欧洲习惯，星期天 作为每周的最后一天
     * <p>
     * A Week is Monday ~ Sunday
     * <p>
     * 
     * @param date
     * @return The last day of the week. Note: only the date was adjusted. time
     *         is kept as original.
     */
    public static Date weekEnd(Date date) {
        return toWeekDayCS(date, 7);
    }

    private static Date toWeekDayCS(Date date, int expect) {
        int day = getWeekDay(date);
        if (day == 0)
            day = 7;
        return adjustDate(date, 0, 0, expect - day);
    }

    private static Date toWeekDayUS(Date date, int expect) {
        int day = getWeekDay(date);
        return adjustDate(date, 0, 0, expect - day);
    }

    /**
     * 得到小时数：24小时制
     * 
     * @param d
     * @return 24小时制的小时数
     */
    public static int getHour(Date d) {
        if (d == null)
            return 0;
        final Calendar c = new GregorianCalendar();
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
        int[] ymd = new int[3];
        if (d == null)
            return ymd;
        final Calendar c = new GregorianCalendar();
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
        int[] hms = new int[3];
        if (d == null)
            return hms;
        final Calendar c = new GregorianCalendar();
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
    public static void addMillSec(Date d, long value) {
        d.setTime(d.getTime() + value);
    }

    /**
     * 加指定秒
     */
    public static void addSec(Date d, long value) {
        d.setTime(d.getTime() + value * TimeUnit.SECOND.ms);
    }

    /**
     * 加指定分
     */
    public static void addMinute(Date d, int value) {
        d.setTime(d.getTime() + value * TimeUnit.MINUTE.ms);
    }

    /**
     * 加指定小时
     */
    public static void addHour(Date d, int value) {
        d.setTime(d.getTime() + value * TimeUnit.HOUR.ms);
    }

    /**
     * 加指定天
     */
    public static void addDay(Date d, int value) {
        d.setTime(d.getTime() + value * TimeUnit.DAY.ms);
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
        int offset = TimeZone.getDefault().getRawOffset();
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
     * 得到当月包含的天数
     * 
     * @param date
     * @return 2月返回28或29，1月返回31
     */
    public static final int getDaysInMonth(Date date) {
        Assert.notNull(date);
        Calendar calendar = Calendar.getInstance();
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
            maxUnit = TimeUnit.DAY;
            if (second < SECONDS_IN_DAY)
                maxUnit = TimeUnit.HOUR;
            if (second < SECONDS_IN_HOUR)
                maxUnit = TimeUnit.MINUTE;
            if (second < SECONDS_IN_MINITE)
                maxUnit = TimeUnit.SECOND;
        }
        StringBuilder sb = new StringBuilder();
        if (maxUnit.ms >= TimeUnit.DAY.ms) {
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
        if (maxUnit.ms >= TimeUnit.HOUR.ms) {
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
        if (maxUnit.ms >= TimeUnit.MINUTE.ms) {
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
    public static String format(long millseconds) {
        Date d = new Date(millseconds);
        return formatDateTime(d);
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
        return org.apache.commons.lang3.time.DateUtils.truncate(new Date(), Calendar.DATE);
    }

    /**
     * 返回今天
     * 
     * @return
     */
    public static java.sql.Date sqlToday() {
        return new java.sql.Date(org.apache.commons.lang3.time.DateUtils.truncate(new Date(), Calendar.DATE).getTime());
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
    public static boolean isDayBegin(Date date) {
        Date d1 = org.apache.commons.lang3.time.DateUtils.truncate(date, Calendar.DATE);
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
     * @param LocalDateTime
     * @return java.util.Date
     */
    public static Date fromLocalDateTime(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts java.sql.Date to LocalDate (null safety)
     * @param java.sql.Date
     * @return LocalDate
     */
    public static LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }

    /**
     * Converts Time to LocalTime (null safety)
     * @param time
     * @return LocalTime
     */
    public static LocalTime toLocalTime(java.sql.Time time) {
        return time == null ? null : time.toLocalTime();
    }

    /**
     * Converts Timestamp to LocalTime (null safety)
     * @param ts Timestamp
     * @return LocalTime
     */
    public static LocalTime toLocalTime(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime().toLocalTime();
    }

    /**
     * Converts Timestamp to LocalDateTime (null safety)
     * @param ts Timestamp
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    /**
     * Converts LocalDate to Time (null safety)
     * @param date LocalDate
     * @return java.sql.Date
     */
    public static java.sql.Date toSqlDate(LocalDate date) {
        return date == null ? null : java.sql.Date.valueOf(date);
    }

    /**
     * Converts LocalTime to Time (null safety)
     * @param time LocalTime
     * @return java.sql.Time
     */
    public static java.sql.Time toSqlTime(LocalTime time) {
        return time == null ? null : java.sql.Time.valueOf(time);
    }

    /**
     * Converts LocalDateTime to Timestamp (null safety)
     * @param time LocalDateTime
     * @return Timestamp
     */
    public static java.sql.Timestamp toSqlTimeStamp(LocalDateTime time) {
        return time == null ? null : java.sql.Timestamp.valueOf(time);
    }

    /**
     * Converts instant to Timestamp (null safety)
     * @param instant Instant
     * @return java.sql.Timestamp
     */
    public static java.sql.Timestamp toSqlTimeStamp(Instant instant) {
        return instant == null ? null : java.sql.Timestamp.from(instant);
    }

    /**
     * Converts instant to JUD (null safety)
     * @param instant
     * @return java.util.Date
     */
    public static Date fromInstant(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    /**
     * Converts LocalTime to Timestamp (null safety)
     * @param localTime LocalTime
     * @return Timestamp
     */
    public static Timestamp toSqlTimeStamp(LocalTime localTime) {
        return localTime == null ? null : java.sql.Timestamp.valueOf(LocalDateTime.of(LocalDate.now(), localTime));
    }
    
	/**
	 * 返回两个日期中较大的那个 如果有null值，返回非null值
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static Date max(Date d1, Date d2) {
		if (d1 == null && d2 == null) {
			return null;
		} else if (d1 == null) {
			return d2;
		} else if (d2 == null) {
			return d1;
		}
		return d1.getTime() > d2.getTime() ? d1 : d2;
	}

	/**
	 * 返回两个日期中较小的哪个 如果有null值，返回非null值
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static Date min(Date d1, Date d2) {
		if (d1 == null && d2 == null) {
			return null;
		} else if (d1 == null) {
			return d2;
		} else if (d2 == null) {
			return d1;
		}
		return d1.getTime() > d2.getTime() ? d2 : d1;
	}

	/**
	 * 返回两个日期中较大的那个 如果有null值，返回非null值
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static Date maxOrNull(Date d1, Date d2) {
		if (d1 == null && d2 == null) {
			return null;
		} else if (d1 == null) {
			return d1;
		} else if (d2 == null) {
			return d2;
		}
		return d1.getTime() > d2.getTime() ? d1 : d2;
	}

	/**
	 * 返回两个日期中较小的哪个 如果有null值，返回非null值
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static Date minOrNull(Date d1, Date d2) {
		if (d1 == null && d2 == null) {
			return null;
		} else if (d1 == null) {
			return d1;
		} else if (d2 == null) {
			return d2;
		}
		return d1.getTime() > d2.getTime() ? d2 : d1;
	}
	

	public static Date adjustDay(Date date, int day) {
		if(date==null) {
			return null;
		}
		return new Date(date.getTime()+MILLISECONDS_IN_DAY * day);
	}
	

	/**
	 * 计算得到指定时区下一天的开始时间
	 * 
	 * @param d
	 * @param utcOffset
	 * @return
	 */
	public final static long truncateX(Date d, int utcOffset) {
		if (d == null) {
			return 0;
		}
		long time = d.getTime();
		if(time>=0) {
			time -= time % MILLISECONDS_IN_DAY;
			return time - utcOffset * MILLISECONDS_IN_HOUR;
		}
		return org.apache.commons.lang3.time.DateUtils.truncate(d, Calendar.DATE).getTime();
	}
}
