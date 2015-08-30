package com.dy.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class TimeUtil {
	private static final String[] WEEK = { "天", "一", "二", "三", "四", "五", "六" };
	public static final String XING_QI = "星期";
	public static final String ZHOU = "周";

	/*
	 * 返回今天以后的第num天是星期几
	 * 
	 */
	public static String getWeek(int num, String format) {
		final Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
		int weekNum = c.get(Calendar.DAY_OF_WEEK) + num;
		if (weekNum > 7)
			weekNum = weekNum - 7;
		return format + WEEK[weekNum - 1];
	}
	/**
	 * 返回:08/30 周日
	 * @return
	 */
	public static String getZhouWeek() {
		SimpleDateFormat format = new SimpleDateFormat("MM/dd");
		return format.format(new Date(System.currentTimeMillis())) + " "
				+ TimeUtil.getWeek(0, ZHOU);
	}
	
	
	/*
	 * 返回:今天13:32
	 */

	public static String getDay(long timesamp) {
		if(timesamp == 0L)
			return "未";
		String result = "未";
		SimpleDateFormat sdf = new SimpleDateFormat("dd");
//		当前时间
		Date today = new Date(System.currentTimeMillis());
//		传入的时间
		Date otherDay = new Date(timesamp);
//		两者时间的差值
		int temp = Integer.parseInt(sdf.format(today))
				- Integer.parseInt(sdf.format(otherDay));

		switch (temp) {
		case 0:
			result = "今天" + TimeUtil.getTime(timesamp);
			break;
		case 1:
			result = "昨天"+ TimeUtil.getTime(timesamp);
			break;
		case 2:
			result = "前天"+ TimeUtil.getTime(timesamp);
			break;

		default:
			result = temp + "天前"+ TimeUtil.getTime(timesamp);
			break;
		}

		return result;
	}

	/*
	 * 返回完整时间:yyyy-MM-dd'T'HH:mm:ss
	 */
	public static long getLongTime(String time) {
		try {
			time = time.substring(0, time.indexOf('.'));
			Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
					.parse(time);
			return date.getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}

	/**
	 * 返回14:23
	 * @param time
	 * @return
	 */
	public static String getTime(long time) {
		return new SimpleDateFormat("HH:mm").format(new Date(time));
	}
}
