/**
 * 
 */
package com.droidfad.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;


/**
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com
 *
 *   Licensed under the DROIDFAD license (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.droidfad.com/html/license/license.htm
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * -----------------------------------------------------------------------
 *
 */
public class DateUtil {

	static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");

	/**
	 * key     - day name as reference in Android 'BYDAY' parameter
	 * value   - day of week according Calendar
	 */
	private static HashMap<String, Integer> dayName2DayOfWeek = new HashMap<String, Integer>();

	/**
	 * key     - day of week according Calendar
	 * value   - day name as reference in Android 'BYDAY' parameter
	 */
	private static String[] dayOfWeek2DayName = new String[8];

	/**
	 * key   - year
	 * value - day count
	 */
	private static HashMap<Integer, Integer> daysOfFebruary = new HashMap<Integer, Integer>();

	private final static Calendar gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

	public static final boolean IS_TEST = false;
	public static final long    MSEC_PER_DAY     = 24 * 3600 * 1000;
	public static final long    MSEC_PER_MINUTE  = 60 * 1000;
	public static final long    MSEC_PER_HOUR    = 60 * 60 * 1000;
	/**
	 * FIXME using the number of msecs per week is not appropriate because 
	 * number can vary according daylight saving time
	 */
	@Deprecated
	public static final long    MSEC_PER_WEEK    = 7  * 24 * 3600 * 1000;

	static {
		put2DayOfWeekMaps(CalendarWrapper.MONDAY,    "MO");
		put2DayOfWeekMaps(CalendarWrapper.TUESDAY,   "TU");
		put2DayOfWeekMaps(CalendarWrapper.WEDNESDAY, "WE");
		put2DayOfWeekMaps(CalendarWrapper.THURSDAY,  "TH");
		put2DayOfWeekMaps(CalendarWrapper.FRIDAY,    "FR");
		put2DayOfWeekMaps(CalendarWrapper.SATURDAY,  "SA");
		put2DayOfWeekMaps(CalendarWrapper.SUNDAY,    "SU");
	}
	/**
	 *
	 * @param pCalendar
	 * @param pDateCal
	 *
	 */
	private static void assertEquals2Day(Calendar pCalendar, CalendarWrapper pDateCal) {

		assert pDateCal.get(CalendarWrapper.YEAR)         == pCalendar.get(Calendar.YEAR);
		assert pDateCal.get(CalendarWrapper.MONTH)        == pCalendar.get(Calendar.MONTH);
		assert pDateCal.get(CalendarWrapper.DATE)         == pCalendar.get(Calendar.DATE);
	}
	/**
	 *
	 * @param pCalendar
	 * @param pDateCal
	 *
	 */
	private static void assertEquals2Second(Calendar pCalendar, CalendarWrapper pDateCal) {

		assert pDateCal.get(CalendarWrapper.YEAR)         == pCalendar.get(Calendar.YEAR);
		assert pDateCal.get(CalendarWrapper.MONTH)        == pCalendar.get(Calendar.MONTH);
		assert pDateCal.get(CalendarWrapper.DATE)         == pCalendar.get(Calendar.DATE);
		assert pDateCal.get(CalendarWrapper.HOUR_OF_DAY)  == pCalendar.get(Calendar.HOUR_OF_DAY);
		assert pDateCal.get(CalendarWrapper.MINUTE)       == pCalendar.get(Calendar.MINUTE);
		assert pDateCal.get(CalendarWrapper.SECOND)       == pCalendar.get(Calendar.SECOND);
	}

	/**
	 *
	 * @param pTZone
	 * @param pTime
	 * @param pTargetCal
	 * @return
	 *
	 */
	public static long convertTimeToLocalTimezone(Long pTime, Calendar pTargetCal) {
		gmtCalendar.setTimeInMillis(pTime);

		pTargetCal.set(Calendar.YEAR, gmtCalendar.get(Calendar.YEAR));
		pTargetCal.set(Calendar.MONTH, gmtCalendar.get(Calendar.MONTH));
		pTargetCal.set(Calendar.DAY_OF_MONTH, gmtCalendar.get(Calendar.DAY_OF_MONTH));
		pTargetCal.set(Calendar.HOUR_OF_DAY, gmtCalendar.get(Calendar.HOUR_OF_DAY));
		pTargetCal.set(Calendar.MINUTE, gmtCalendar.get(Calendar.MINUTE));
		pTargetCal.set(Calendar.SECOND, gmtCalendar.get(Calendar.SECOND));
		pTargetCal.set(Calendar.MILLISECOND, gmtCalendar.get(Calendar.MILLISECOND));

		return pTargetCal.getTimeInMillis();
	}
	public static synchronized String format(long pGmtTime) {
		return DateUtil.dateFormat.format(new Date(pGmtTime));
	}

	public static String getDayOfWeek(int pWeekDayIndex) {
		if(pWeekDayIndex < 1 || pWeekDayIndex > 7) {
			throw new IllegalArgumentException("not valid pWeekDayIndex:" + pWeekDayIndex);
		}
		String lWeekDay = dayOfWeek2DayName[pWeekDayIndex];
		return lWeekDay;
	}

	/**
	 *
	 * @param pWeekDay
	 * @return
	 *
	 */
	public static int getDayOfWeekIndex(String pWeekDay) {
		int     lReturn    = -1;
		Integer lDayOfWeek = dayName2DayOfWeek.get(pWeekDay);
		if(lDayOfWeek != null) {
			lReturn = lDayOfWeek;
		} else {
			throw new IllegalArgumentException("not valid dayOfWeek:" + pWeekDay);
		}
		return lReturn;
	}
	public static int getLastDayInMonth(int year, int month) {

		int lLastDay = -1;
		switch(month) {
		case 0: // Jan
		case 2: // Mar
		case 4: // May
		case 6: // Jul
		case 7: // Aug
		case 9: // Oct
		case 11: // Dec
			lLastDay = 31;
			break;
		case 1: // Feb	
			Integer lDayCount = daysOfFebruary.get(year);
			if(lDayCount == null) {
				Calendar lCalendar = Calendar.getInstance();
				lCalendar.set(year, 2, 1);
				lCalendar.add(Calendar.DATE, -1);
				lDayCount = lCalendar.get(Calendar.DAY_OF_MONTH);
				daysOfFebruary.put(year, lDayCount);
			}
			lLastDay  = lDayCount;
			break;
		case 3: // Apr
		case 5: // Jun
		case 8: // Sep
		case 10: // Nov
			lLastDay = 30;
			break;
		default:
			throw new IllegalArgumentException("month out of range:" + month);
		}
		return lLastDay;
	}

	public static boolean isStartOfDay(Calendar pDateCal) {
		boolean lIsStartOfDay = 
				pDateCal.get(CalendarWrapper.HOUR_OF_DAY) == 0
				&& pDateCal.get(CalendarWrapper.MINUTE)      == 0
				&& pDateCal.get(CalendarWrapper.SECOND)      == 0;

		return lIsStartOfDay;		
	}

	public static boolean isStartOfDay(CalendarWrapper pWrapper) {
		boolean lIsStartOfDay = (
				pWrapper.get(Calendar.HOUR_OF_DAY) == 0
				&& pWrapper.get(Calendar.MINUTE)      == 0
				&& pWrapper.get(Calendar.SECOND)      == 0
				);		
		if(IS_TEST) {
			Calendar lCalendar = Calendar.getInstance();
			lCalendar.setTimeInMillis(pWrapper.getTimeInMillis());
			assert lIsStartOfDay == isStartOfDay(lCalendar);
		}
		return lIsStartOfDay;
	}

	public static synchronized void printDate(PrintStream out, String pPrefix, Date pDate) {
		out.print(pPrefix);
		out.print(DateUtil.dateFormat.format(pDate));
	}

	public static synchronized void printDate(PrintStream out, String pPrefix, long pDate) {
		printDate(out, pPrefix, new Date(pDate));
	}

	public static synchronized void printInMin(PrintStream out, String pPrefix, long pMSec) {
		printInSec(out, pPrefix, pMSec / 60);
		out.print('.');
		out.print(pMSec % 60000);
	}

	public static synchronized void printInSec(PrintStream out, String pPrefix, long pMSec) {
		out.print(pPrefix);
		out.print(pMSec / 1000);
	}

	public static synchronized void printlnDate(PrintStream out, String pPrefix, Date pDate) {
		printDate(out, pPrefix, pDate);
		out.println();
	}

	public static synchronized void printlnDate(PrintStream out, String pPrefix, long pDate) {
		printlnDate(out, pPrefix, new Date(pDate));
	}

	public static synchronized void printlnInHour(PrintStream out, String pPrefix, long pMSec) {
		long lHours    = pMSec / DateUtil.MSEC_PER_HOUR;
		long lHoursMod = pMSec % DateUtil.MSEC_PER_HOUR;
		long lMins     = lHoursMod / DateUtil.MSEC_PER_MINUTE;
		long lMinMods  = lHoursMod % DateUtil.MSEC_PER_MINUTE;
		long lSecs     = lMinMods /  1000;
		long lMSecs    = lMinMods % 1000;
				
		out.print(pPrefix);
		out.print(lHours);
		out.print(':');
		out.print(lMins);
		out.print(':');
		out.print(lSecs);
		out.print('.');
		out.println(lMSecs);
	}

	public static synchronized void printlnInMin(PrintStream out, String pPrefix, long pMSec) {
		printInMin(out, pPrefix, pMSec);
		out.println();
	}

	public static synchronized void printlnInSec(PrintStream out, String pPrefix, long pMSec) {
		printInSec(out, pPrefix, pMSec);
		out.println();
	}

	/**
	 *
	 * @param pDayOfWeek
	 * @param pDayName
	 *
	 */
	private static void put2DayOfWeekMaps(int pDayOfWeek, String pDayName) {
		dayOfWeek2DayName[pDayOfWeek] = pDayName;
		dayName2DayOfWeek.put(pDayName, pDayOfWeek);
	}

	/**
	 *
	 * @param pCalendar
	 * @param pYear
	 * @param pMonth
	 *
	 */
	static void setCalToLastDayOfMonth(Calendar pCalendar, int pYear, int pMonth) {
		
		pCalendar.set(Calendar.YEAR,  pYear);
		pCalendar.set(Calendar.MONTH, pMonth);
		pCalendar.set(Calendar.DATE,  1);

		pCalendar.add(Calendar.MONTH, 1);
		pCalendar.add(Calendar.DATE, -1);
		
	}

	public static void setCalToLastDayOfMonth(CalendarWrapper pWrapper, int pYear, int pMonth) {

		pWrapper.set(Calendar.YEAR,  pYear);
		pWrapper.set(Calendar.MONTH, pMonth);
		pWrapper.set(Calendar.DATE,  getLastDayInMonth(pYear, pMonth));
		
		if(IS_TEST) {
			Calendar lCalendar = Calendar.getInstance();
			setCalToLastDayOfMonth(lCalendar, pYear, pMonth);
			assertEquals2Day(lCalendar, pWrapper);
		}
	}

	/**
	 * set the Calendar pCalendar to the n-th pWeekday. The method returns false if
	 * the dayOfWeek could not be found. If n is set to -1 it means that the last
	 * day of month with pWeekDay is to be set  
	 *
	 * @param pCalendar
	 * @param pYear
	 * @param pMonth
	 * @param pWeekday can be MO,TU,WE,TH,FR,SA,SO
	 * @param n
	 * @return true if the date could be set. 
	 *
	 */	
	static boolean setCalToNthWeekdayOfMonth(Calendar pCalendar, int pYear, int pMonth, String pWeekday, int n) {
		if(pCalendar == null) {
			throw new IllegalArgumentException("parameter pCalendar must not be null");
		}
		if(pWeekday == null) {
			throw new IllegalArgumentException("parameter pWeekday must not be null");
		}
		if(n <= 0 && n != -1) {
			throw new IllegalArgumentException("parameter n must not be greater than 0");
		}
		
		boolean lResult    = false;
		/**
		 * get the dayOfWeek and calculate the difference to the requested dayOfWeek
		 */
		Integer lDayOfWeek = dayName2DayOfWeek.get(pWeekday);
		if(lDayOfWeek != null) {
			/**
			 * set the calendar to the first day of month
			 */
			pCalendar.set(Calendar.YEAR, pYear);
			pCalendar.set(Calendar.MONTH, pMonth);

			int lLastDay  = getLastDayInMonth(pYear, pMonth);
			if(n == -1) {
				pCalendar.set(Calendar.DATE, lLastDay);
			} else {
				pCalendar.set(Calendar.DATE, 1);
			}

			int lCalDayOfWeek = pCalendar.get(Calendar.DAY_OF_WEEK);
			int lIncrement    = lDayOfWeek  - lCalDayOfWeek;; 
			if(n == -1) {
				/**
				 *   1  2  3  4  5  6  7   1  2  3  4
				 *  MO DI WE TH FR SA SU  MO DI WE TH
				 *  case 1
				 *        WD                       LD
				 *  incr  -1
				 *  case 2
				 *                 WD              LD
				 *  incr  -5
				 *  case 3
				 *                                 LD
				 *                                 WD
				 *  incr   0
				 */
				if(lIncrement > 0) {
					lIncrement += -7;
				}
			} else {
				if(lIncrement < 0) {
					lIncrement += 7;
				} 
			} 
			pCalendar.add(Calendar.DATE, lIncrement);

			/**
			 * now pCalendar should have the first pWeekDay in pMonth.
			 * set a control value for debug purposes
			 */
			lCalDayOfWeek = pCalendar.get(Calendar.DAY_OF_WEEK);

			if(n == -1) {
				lResult = true;
			} else {
				/**
				 * increment the date 
				 */
				int lDay      = pCalendar.get(Calendar.DATE) + (n-1) * 7;
				if(lDay <= lLastDay) {
					pCalendar.set(Calendar.DATE, lDay);
					lResult = true;
				} else {
					lResult = false;
				}
			}
		} else {
			throw new IllegalArgumentException("unknown dayOfWeek:" + pWeekday);
		}

		return  lResult;
	}

	/**
	 * set the Calendar pCalendar to the n-th pWeekday. The method returns Long.MIN_VALUE if
	 * the dayOfWeek could not be found. If n is set to -1 it means that the last
	 * day of month with pWeekDay is to be set  
	 *
	 * @param lCalendar
	 * @param pYear
	 * @param pMonth
	 * @param pWeekday can be MO,TU,WE,TH,FR,SA,SO
	 * @param n
	 * @return 		true if the date could be set, false otherwise 
	 * FIXME test me :-)
	 */	
	public static boolean setCalToNthWeekdayOfMonth(CalendarWrapper pWrapper, int pYear, int pMonth, String pWeekday, int n) {

		if(pWeekday == null) {
			throw new IllegalArgumentException("parameter pWeekday must not be null");
		}
		if(n <= 0 && n != -1) {
			throw new IllegalArgumentException("parameter n must not be greater than 0");
		}
		boolean lResult    = false;

		Calendar lCalendar = null;
		if(IS_TEST) {
			lCalendar = Calendar.getInstance();
			lCalendar.setTimeInMillis(pWrapper.getTimeInMillis());
		}
		/**
		 * get the dayOfWeek and calculate the difference to the requested dayOfWeek
		 */
		Integer lDayOfWeek = dayName2DayOfWeek.get(pWeekday);
		if(lDayOfWeek != null) {
			/**
			 * set the calendar to the first day of month
			 */
			pWrapper.set(Calendar.YEAR, pYear);
			pWrapper.set(Calendar.MONTH, pMonth);

			int lLastDay  = getLastDayInMonth(pYear, pMonth);
			if(n == -1) {
				pWrapper.set(Calendar.DATE, lLastDay);
			} else {
				pWrapper.set(Calendar.DATE, 1);
			}

			int lCalDayOfWeek = pWrapper.get(Calendar.DAY_OF_WEEK);
			int lIncrement    = lDayOfWeek  - lCalDayOfWeek;; 
			if(n == -1) {
				/**
				 *   1  2  3  4  5  6  7   1  2  3  4
				 *  MO DI WE TH FR SA SU  MO DI WE TH
				 *  case 1
				 *        WD                       LD
				 *  incr  -1
				 *  case 2
				 *                 WD              LD
				 *  incr  -5
				 *  case 3
				 *                                 LD
				 *                                 WD
				 *  incr   0
				 */
				if(lIncrement > 0) {
					lIncrement += -7;
				}
			} else {
				if(lIncrement < 0) {
					lIncrement += 7;
				} 
			} 
			pWrapper.add(pWrapper.getTimeInMillis(), Calendar.DATE, lIncrement);

			/**
			 * now pCalendar should have the first pWeekDay in pMonth.
			 * set a control value for debug purposes
			 */
			lCalDayOfWeek = pWrapper.get(Calendar.DAY_OF_WEEK);

			if(n == -1) {
				lResult = true;
			} else {
				/**
				 * increment the date 
				 */
				int lDay      = pWrapper.get(Calendar.DATE) + (n-1) * 7;
				if(lDay <= lLastDay) {
					pWrapper.set(Calendar.DATE, lDay);
					lResult = true;
				} else {
					lResult = false;
				}
			}
		} else {
			throw new IllegalArgumentException("unknown dayOfWeek:" + pWeekday);
		}

		if(IS_TEST) {
			boolean lCompareResult = setCalToNthWeekdayOfMonth(lCalendar, pYear, pMonth, pWeekday, n);
			assertEquals2Day(lCalendar, pWrapper);			
		}
		
		return  lResult;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	static void setEndOfDay(Calendar pDateCal) {
		pDateCal.set(Calendar.HOUR_OF_DAY, 23);
		pDateCal.set(Calendar.MINUTE,      59);
		pDateCal.set(Calendar.SECOND,      59);
		pDateCal.set(Calendar.MILLISECOND,  0);
	}

	public static void setEndOfDay(CalendarWrapper pWrapper) {
		Calendar lCalendar = null;
		if(IS_TEST) {
			lCalendar = Calendar.getInstance();
			lCalendar.setTimeInMillis(pWrapper.getTimeInMillis());
		}
		pWrapper.setEndOfDay(
				pWrapper.get(CalendarWrapper.YEAR), 
				pWrapper.get(CalendarWrapper.MONTH), 
				pWrapper.get(CalendarWrapper.DATE));

		if(IS_TEST) {
			setEndOfDay(lCalendar);
			assertEquals2Second(lCalendar, pWrapper);
		}
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	static void setStartOfDay(Calendar pDateCal) {
		pDateCal.set(Calendar.HOUR_OF_DAY, 0);
		pDateCal.set(Calendar.MINUTE,      0);
		pDateCal.set(Calendar.SECOND,      0);
		pDateCal.set(Calendar.MILLISECOND, 0);

	}

	public static void setStartOfDay(CalendarWrapper pWrapper) {
		
		Calendar lCalendar = null;
		if(IS_TEST) {
			lCalendar = Calendar.getInstance();
			lCalendar.setTimeInMillis(pWrapper.getTimeInMillis());
		}
		
		pWrapper.setStartOfDay(
				pWrapper.get(CalendarWrapper.YEAR), 
				pWrapper.get(CalendarWrapper.MONTH), 
				pWrapper.get(CalendarWrapper.DATE));
				
		if(IS_TEST) {
			setStartOfDay(lCalendar);
			assertEquals2Second(lCalendar, pWrapper);
		}
	}
}
