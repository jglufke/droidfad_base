/**
 * 
 */
package com.droidfad.util;

import java.util.Calendar;



/**
 *
 * @author John
 * copyright Jens Glufke, Germany mailto:jglufke@gmx.de
 *
 */
public class CalendarWrapper {

	public static final int DATE        = Calendar.DATE;
	public static final int DAY_OF_WEEK = Calendar.DAY_OF_WEEK;
	public static final int MONTH       = Calendar.MONTH;
	public static final int MILLISECOND = Calendar.MILLISECOND;
	public static final int SECOND      = Calendar.SECOND;
	public static final int YEAR        = Calendar.YEAR;
	public static final int HOUR_OF_DAY = Calendar.HOUR_OF_DAY;
	public static final int MINUTE      = Calendar.MINUTE;

	public static final int MONDAY      = Calendar.MONDAY;
	public static final int TUESDAY     = Calendar.TUESDAY;
	public static final int WEDNESDAY   = Calendar.WEDNESDAY;
	public static final int THURSDAY    = Calendar.THURSDAY;
	public static final int FRIDAY      = Calendar.FRIDAY;
	public static final int SATURDAY    = Calendar.SATURDAY;
	public static final int SUNDAY      = Calendar.SUNDAY;

	/**
	 *
	 * @return
	 *
	 */
	public static CalendarWrapper getInstance() {
		return new CalendarWrapper();
	}

	private long gmtTime;

	private CalendarWrapper() { }

	/**
	 *
	 * @param pField
	 * @param pCount
	 *
	 */
	public void add(long pGmtTime, int pField, int pCount) {

		switch(pField) {
		case MONTH:
			gmtTime = CalendarCache.addMonths(pGmtTime, pCount);
			break;
		case DATE:
			gmtTime = CalendarCache.addDays(pGmtTime, pCount);
			break;
		case SECOND:
			gmtTime += pCount * 1000;
			break;
		default:
			throw new IllegalArgumentException("not handled field");
		}
	}


	public boolean before(long pGmtTime) {
		return gmtTime < pGmtTime;
	}
	public boolean after(long pGmtTime) {
		return gmtTime > pGmtTime;
	}

	/**
	 *
	 * @param pYear
	 * @return
	 *
	 */
	public int get(final int pField) {

		Calendar lCalendar = null;

		int lValue = 0;
		switch(pField) {
		case YEAR:
			lValue = CalendarCache.getYear(gmtTime);
			break;
		case MONTH:
			lValue = CalendarCache.getMonth(gmtTime);
			break;
		case DAY_OF_WEEK:
			lValue = CalendarCache.getDayOfWeek(gmtTime);
			break;
		case DATE:
			lValue = CalendarCache.getDay(gmtTime);
			break;
		case HOUR_OF_DAY:
			lValue = CalendarCache.getHourOfDay(gmtTime);
			break;
		case MINUTE:
			lValue = CalendarCache.getMinute(gmtTime);
			break;
		case SECOND:
			lValue = CalendarCache.getSecond(gmtTime);
			break;
		case MILLISECOND:
			lValue = CalendarCache.getMilliSecond(gmtTime);
			break;
		default:
			throw new IllegalArgumentException("not handled field:" + pField);
		}
		return lValue; 
	}

	/**
	 *
	 * @return
	 *
	 */
	public long getTimeInMillis() {
		return gmtTime;
	}

	/**
	 * 
	 *
	 * @param pField
	 * @param pValue
	 *
	 */
	public void setDay(int pValue) {
		gmtTime = CalendarCache.setDay(gmtTime, pValue);
	}

	public void setStartOfDay(int year, int month, int day) {
		gmtTime = CalendarCache.getStartOfDay(year, month, day);
	}

	public void setEndOfDay(int year, int month, int day) {
		gmtTime = CalendarCache.getEndOfDay(year, month, day);
	}
	/**
	 *
	 * @param pEnd
	 *
	 */
	public void setTimeInMillis(long pGmtTime) {
		gmtTime = pGmtTime;
	}

	/**
	 *
	 * @param pField
	 * @param pAmount
	 *
	 */
	public void set(int pField, int pAmount) {

		switch(pField) {
		case YEAR:
			gmtTime = CalendarCache.setYear(gmtTime, pAmount);
			break;
		case MONTH:
			gmtTime = CalendarCache.setMonth(gmtTime, pAmount);
			break;
		case DATE:
			setDay(pAmount);
			break;
		default:
			throw new IllegalArgumentException("not handled field:" + pField);
		}
	}

	public long calcDaySpan(long pBegin, long pEnd) {
		return CalendarCache.calcDaySpan(pBegin, pEnd);
	}

	public void set(int pYear, int pMonth, int pDay, int pHourOfDay, int pMinute, int pSecond) {

		gmtTime = CalendarCache.getStartOfDay(pYear, pMonth, pDay);
		gmtTime += (pHourOfDay*3600000 + pMinute*60000 + pSecond*1000);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder(getClass().getSimpleName());
		lBuilder.append('@');
		lBuilder.append(hashCode());
		lBuilder.append(':');
		lBuilder.append(DateUtil.format(gmtTime));
		return  lBuilder.toString();
	}
}
