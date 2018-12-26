package jef.tools;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class DateUtilsTest extends org.junit.Assert {
	@Test
	public void testSameDayMonth() {
		Date d1 = DateUtils.get(2015, 1, 1, 0, 0, 0);
		Date d2 = DateUtils.get(2015, 1, 30, 12, 0, 0);
		Date d3 = DateUtils.get(2015, 2, 1, 0, 0, 0);
		Date d4 = DateUtils.get(2015, 1, 32, 23, 59, 59);
		assertFalse(DateUtils.isSameDay(d1, d2));
		assertFalse(DateUtils.isSameDay(d2, d3));
		assertTrue(DateUtils.isSameDay(d3, d4));

		assertTrue(DateUtils.isSameMonth(d1, d2));
		assertFalse(DateUtils.isSameMonth(d2, d3));
		assertTrue(DateUtils.isSameMonth(d3, d4));

	}

	@Test
	public void testTruncateZone() {
		Date d=new Date();
		System.out.println(DateUtils.truncateToDay(d,8));
		System.out.println(DateUtils.truncateToDay(d,TimeZone.getTimeZone("Asia/Shanghai")));
		
	}
	
	@Test
	public void testTruncate() {
		System.out.println("====================");
		showT( DateUtils.getUTC(2018, 2, 2, 10, 12, 12));
		
		System.out.println("====================");
		showT( DateUtils.getUTC(2017, 12, 31, 22, 12, 12));
	}

	private void showT(Date d) {
		showTime("原", d, 14);
		showTruncateTimes(d, 14);
		
		showTime("原", d, 8);
		showTruncateTimes(d, 8);
		
		showTime("原", d, 1);
		showTruncateTimes(d, 1);
		
		showTime("原", d, 0);
		showTruncateTimes(d, 0);
		
		showTime("原", d, -1);
		showTruncateTimes(d, -1);
		
		showTime("原", d, -5);
		showTruncateTimes(d, -5);

		
		showTime("原", d, -12);
		showTruncateTimes(d, -12);
	}

	private void showTruncateTimes(Date d, int zone) {
		showTime("分", DateUtils.truncateToMinute(d), zone);
		showTime("时", DateUtils.truncateToHour(d), zone);
		showTime("天", DateUtils.truncateToDay(d, zone), zone);
		showTime("月", DateUtils.truncateToMonth(d, zone), zone);
		showTime("年", DateUtils.truncateToYear(d, zone), zone);
		showTime("月底",DateUtils.monthEnd(d,TimeZones.getByUTCOffset(zone)),zone);
		System.out.println("----");
	}

	private void showTime(String type, Date truncateDay, int zone) {
		System.out.println(type + " " + DateFormats.DATE_TIME_CS_WITH_ZONE.format(truncateDay, zone));
	}

	@Test
	public void testTruncate2() {
		Date d = new Date();

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		c.setTime(d);
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		System.out.println(c.getTime());

	}
}
