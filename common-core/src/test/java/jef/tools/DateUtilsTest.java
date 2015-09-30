package jef.tools;

import java.util.Date;

import org.junit.Test;

public class DateUtilsTest extends org.junit.Assert{
	@Test
	public void testSameDayMonth() {
		Date d1=DateUtils.get(2015, 1, 1, 0, 0, 0);
		Date d2=DateUtils.get(2015, 1, 30, 12, 0, 0);
		Date d3=DateUtils.get(2015, 2, 1, 0, 0, 0);
		Date d4=DateUtils.get(2015, 1, 32, 23, 59, 59);
		assertFalse(DateUtils.isSameDay(d1, d2));
		assertFalse(DateUtils.isSameDay(d2, d3));
		assertTrue(DateUtils.isSameDay(d3, d4));
		
		assertTrue(DateUtils.isSameMonth(d1, d2));
		assertFalse(DateUtils.isSameMonth(d2, d3));
		assertTrue(DateUtils.isSameMonth(d3, d4));
		
	}

}
