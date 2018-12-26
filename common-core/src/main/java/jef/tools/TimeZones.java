package jef.tools;

import java.util.TimeZone;

/**
 * 记录了所有27个时区的常量，用于在编码时直接引用。
 * 
 * @author jiyi
 *
 */
public class TimeZones {
	static final String[] TIME_ZONES = new String[] { "Etc/GMT+12", "Pacific/Midway", "US/Hawaii", "US/Alaska", "US/Pacific", "US/Arizona", "US/Central", "America/New_York", "PRT", "America/Araguaina", "Atlantic/South_Georgia", "Atlantic/Azores", "GMT", "Etc/GMT-1", "Etc/GMT-2",
			"Europe/Moscow", "Etc/GMT-4", "IST", "Etc/GMT-6", "Etc/GMT-7", "Asia/Shanghai", "Asia/Tokyo", "Australia/ACT", "Etc/GMT-11", "Etc/GMT-12", "Pacific/Apia", "Pacific/Kiritimati" };

	public static final TimeZone UTC_M12 = TimeZone.getTimeZone(TIME_ZONES[0]);

	public static final TimeZone UTC_M11 = TimeZone.getTimeZone(TIME_ZONES[1]);

	public static final TimeZone UTC_M10 = TimeZone.getTimeZone(TIME_ZONES[2]);

	public static final TimeZone UTC_M9 = TimeZone.getTimeZone(TIME_ZONES[3]);

	public static final TimeZone UTC_M8 = TimeZone.getTimeZone(TIME_ZONES[4]);

	public static final TimeZone UTC_M7 = TimeZone.getTimeZone(TIME_ZONES[5]);

	public static final TimeZone UTC_M6 = TimeZone.getTimeZone(TIME_ZONES[6]);

	public static final TimeZone UTC_M5 = TimeZone.getTimeZone(TIME_ZONES[7]);

	public static final TimeZone UTC_M4 = TimeZone.getTimeZone(TIME_ZONES[8]);

	public static final TimeZone UTC_M3 = TimeZone.getTimeZone(TIME_ZONES[9]);

	public static final TimeZone UTC_M2 = TimeZone.getTimeZone(TIME_ZONES[10]);

	public static final TimeZone UTC_M1 = TimeZone.getTimeZone(TIME_ZONES[11]);

	public static final TimeZone UTC = TimeZone.getTimeZone(TIME_ZONES[12]);

	public static final TimeZone UTC_1 = TimeZone.getTimeZone(TIME_ZONES[13]);

	public static final TimeZone UTC_2 = TimeZone.getTimeZone(TIME_ZONES[14]);

	public static final TimeZone UTC_3 = TimeZone.getTimeZone(TIME_ZONES[15]);

	public static final TimeZone UTC_4 = TimeZone.getTimeZone(TIME_ZONES[16]);

	public static final TimeZone UTC_5 = TimeZone.getTimeZone(TIME_ZONES[17]);

	public static final TimeZone UTC_6 = TimeZone.getTimeZone(TIME_ZONES[18]);

	public static final TimeZone UTC_7 = TimeZone.getTimeZone(TIME_ZONES[19]);

	public static final TimeZone UTC_8 = TimeZone.getTimeZone(TIME_ZONES[20]);

	public static final TimeZone UTC_9 = TimeZone.getTimeZone(TIME_ZONES[21]);

	public static final TimeZone UTC_10 = TimeZone.getTimeZone(TIME_ZONES[22]);

	public static final TimeZone UTC_11 = TimeZone.getTimeZone(TIME_ZONES[23]);

	public static final TimeZone UTC_12 = TimeZone.getTimeZone(TIME_ZONES[24]);

	public static final TimeZone UTC_13 = TimeZone.getTimeZone(TIME_ZONES[25]);

	public static final TimeZone UTC_14 = TimeZone.getTimeZone(TIME_ZONES[26]);

	public static final TimeZone Asia_Shanghai = TimeZone.getTimeZone("Asia/Shanghai");

	// public static void main(String[] args) throws IllegalArgumentException,
	// IllegalAccessException {
	// for(Field field:TimeZones.class.getDeclaredFields()) {
	// Object b=field.get(null);
	// if(b instanceof TimeZone) {
	// TimeZone tz=(TimeZone)b;
	// System.out.println(field.getName()+" "+tz.getRawOffset()/3600000);
	// }
	// }
	// }

	public static final TimeZone getByUTCOffset(int utcOffset) {
		if (utcOffset > 14 || utcOffset < -12) {
			throw new IllegalArgumentException("Invalid UTC time offset,must beetween UTC-12 to UTC+14.");
		}
		return TimeZone.getTimeZone(TIME_ZONES[utcOffset + 12]);
	}
}
