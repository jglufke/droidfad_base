/**
 * 
 */
package com.droidfad.util;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;


/**
Copyright 2014 Jens Glufke

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
public class CalendarCache {

	public final static boolean DEBUG = false;

	private static CacheEntry lastEntry = null;
	private static long       lastBegin = Long.MAX_VALUE;
	private static long       lastEnd   = Long.MIN_VALUE;

	private static class CacheEntry {

		int  year;
		int  dayOfMonth;
		int  day         = -1;
		int  dayOfWeek   = -1;
		int  month       = -1;
		int  utcOffset   = -1;
		long nextMonth   = Long.MIN_VALUE;
		/**
		 * gmtTime is always the begin time of the day
		 */
		long gmtTime     = Long.MIN_VALUE;
		long midTime     = Long.MIN_VALUE;
		long endTime     = Long.MIN_VALUE;

		public CacheEntry(long pGmtTime) {			
			synchronized(calendar) {

				calendar.setTimeInMillis(pGmtTime);
				day        = calendar.get(Calendar.DATE);
				month      = calendar.get(Calendar.MONTH);
				year       = calendar.get(Calendar.YEAR);
				dayOfWeek  = calendar.get(Calendar.DAY_OF_WEEK);
				dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
				/**
				 * set the day begin time
				 */
				calendar.set(year, month, day, 0, 0, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				gmtTime    =  calendar.getTimeInMillis();
				/**
				 * set the day end time
				 */
				calendar.set(year, month, day, 23, 59, 59);
				calendar.set(Calendar.MILLISECOND, 999);
				endTime    =  calendar.getTimeInMillis();

				midTime    = gmtTime + (endTime - gmtTime) / 2;

				calendar.add(Calendar.MONTH, 1);
				nextMonth  = calendar.getTimeInMillis();
				utcOffset  = timeZone.getOffset(pGmtTime);
			}
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder lBuilder = new StringBuilder(100);
			lBuilder.append(CalendarCache.CacheEntry.class.getSimpleName());
			lBuilder.append(':');
			lBuilder.append(gmtTime);
			lBuilder.append(':');
			lBuilder.append(endTime);
			lBuilder.append(':');
			lBuilder.append(year);
			lBuilder.append('.');
			lBuilder.append(month+1);
			lBuilder.append('.');
			lBuilder.append(day);
			lBuilder.append(' ');
			lBuilder.append(dayOfWeek);
			lBuilder.append(' ');
			lBuilder.append(dayOfMonth);
			return lBuilder.toString();
		}
	}

	static class CacheValue {
		long       begin       = Long.MIN_VALUE;
		CacheEntry firstDay    = null;
		long       firstSplit  = Long.MIN_VALUE; 
		CacheEntry secondDay   = null;
	}

	public final static long MSEC_PER_SECOND = 1000;
	public final static long MSEC_PER_MINUTE = 60 * MSEC_PER_SECOND;
	public final static long MSEC_PER_HOUR   = 60 * MSEC_PER_MINUTE;
	public final static long MSEC_PER_DAY    = 24 * MSEC_PER_HOUR;
	
	private static SlotMap<CacheValue> cache = new SlotMap<CacheValue>(MSEC_PER_DAY);

	private static Calendar calendar = Calendar.getInstance();

	private static TimeZone timeZone = TimeZone.getDefault();

	/**
	 * key   - local year
	 * key   - local month
	 * key   - local day
	 * value - CalendarEntry
	 */
	private static HashMap<Integer, HashMap<Integer, HashMap<Integer, CacheEntry>>> dateMap =
			new HashMap<Integer, HashMap<Integer,HashMap<Integer,CacheEntry>>>();


	/**
	 *
	 * @param pGmtTime
	 * @return
	 *
	 */
	private static CacheEntry getCacheEntry(final long pGmtTime) {

		if(DEBUG) System.err.println("getCacheEntry -------------------------------------");
		if(lastEntry == null || pGmtTime < lastBegin || pGmtTime > lastEnd) {

			if(cache.size() > 750) {
				cache = new SlotMap<CacheValue>(MSEC_PER_DAY);
				dateMap.clear();
			}
			/**
			 * the cached value contains all local days that are
			 * covered by the gmt day
			 */
			CacheValue lCacheValue  = cache.get(pGmtTime);
			if(lCacheValue == null) {
				lCacheValue       = new CacheValue();
				lCacheValue.begin = cache.calcSlot(pGmtTime);
				cache.put(pGmtTime, lCacheValue);
			}

			lastEntry = getCacheEntry(pGmtTime, lCacheValue);
			lastBegin = lastEntry.gmtTime;
			lastEnd   = lastEntry.endTime;
		} 
		return lastEntry;
	}

	private static CacheEntry getCacheEntry(int year, int month, int day) {

		CacheEntry lCacheEntry = null;
		HashMap<Integer, HashMap<Integer, CacheEntry>> lMonthMap = dateMap.get(year);
		if(lMonthMap != null) {
			HashMap<Integer, CacheEntry> lDayMap = lMonthMap.get(month);
			if(lDayMap != null) {
				lCacheEntry = lDayMap.get(day);
			}
		}
		if(lCacheEntry == null) {
			Calendar lCalendar = Calendar.getInstance();
			lCalendar.set(year, month, day);
			/**
			 * getCacheEntry will also put the created CacheEntry in dateMap
			 */
			lCacheEntry = getCacheEntry(lCalendar.getTimeInMillis());
		}
		return lCacheEntry;
	}

	private static CacheEntry putEntryToDateMap(CacheEntry pCacheEntry) {

		HashMap<Integer, HashMap<Integer, CacheEntry>> lMonthMap = dateMap.get(pCacheEntry.year);
		if(lMonthMap == null) {
			lMonthMap = new HashMap<Integer, HashMap<Integer,CacheEntry>>();
			dateMap.put(pCacheEntry.year, lMonthMap);
		}
		HashMap<Integer, CacheEntry> lDayMap = lMonthMap.get(pCacheEntry.month);
		if(lDayMap == null) {
			lDayMap = new HashMap<Integer, CacheEntry>();
			lMonthMap.put(pCacheEntry.month, lDayMap);
		}

		return lDayMap.put(pCacheEntry.day, pCacheEntry);
	}

	/**
	 *
	 * @param pGmtTime
	 * @param pCacheValue
	 * @return
	 *
	 */
	private static CacheEntry getCacheEntry(long pGmtTime, CacheValue pCacheValue) {

		CacheEntry lCacheEntry      = null;
		long       lGmtDayBegin     = cache.calcSlot(pGmtTime);
		/**
		 * check if there is alreay an appropriate entry
		 */
		if(pCacheValue.firstDay != null && pGmtTime <= pCacheValue.firstDay.endTime) {
			lCacheEntry = pCacheValue.firstDay;
			if(DEBUG) System.err.println("getCacheEntry:firstDay:" + lCacheEntry);
		} else if(pCacheValue.secondDay != null && pGmtTime >= pCacheValue.secondDay.gmtTime && pGmtTime <= pCacheValue.secondDay.endTime) {
			lCacheEntry = pCacheValue.secondDay;
			if(DEBUG) System.err.println("getCacheEntry:scndDay:" + lCacheEntry);
		} 
		/**
		 * if nothing has been found create a new entry
		 */
		if(lCacheEntry == null) {
			lCacheEntry = new CacheEntry(pGmtTime);
			putEntryToCacheValue(lGmtDayBegin, pCacheValue, lCacheEntry, true);
			putEntryToDateMap(lCacheEntry);
		}

		return lCacheEntry;
	}

	/**
	 *
	 * @param pGmtDayBegin
	 * @param pCacheValue
	 * @param pCacheEntry
	 *
	 */
	private static void putEntryToCacheValue(long pGmtDayBegin, CacheValue pCacheValue, 
			CacheEntry pCacheEntry, boolean pSetNeighbours) {


		if(pCacheEntry.gmtTime <= pGmtDayBegin) {
			/**
			 * the day starts in the previous gmt day interval, so it is a firstday entry
			 */
			pCacheValue.firstDay   = pCacheEntry;
			if(DEBUG) System.err.println("putEntryToCacheValue firstDay:"+pCacheValue.begin+":" + pCacheEntry);
			if(pSetNeighbours && pCacheEntry.gmtTime < pGmtDayBegin) {
				/**
				 * set lCacheEntry to the previous interval
				 */
				CacheValue lPrevValue  = cache.get(pCacheEntry.gmtTime);
				if(lPrevValue == null) {
					lPrevValue       = new CacheValue();
					lPrevValue.begin = cache.calcSlot(cache.calcSlot(pCacheEntry.gmtTime));
					cache.put(pCacheEntry.gmtTime, lPrevValue);
				}
				if(DEBUG) System.err.println("putEntryToCacheValue firstDay put2Prev:"+pCacheValue.begin+":" + pCacheEntry);
				putEntryToCacheValue(pGmtDayBegin-MSEC_PER_DAY, lPrevValue, pCacheEntry, false);
			}
		} else {
			/**
			 * at this point pCacheEntry might describe the second or the 
			 * third day of the gmt day interval.
			 * 
			 */

			/**
			 * check for a second entry
			 */
			if(pCacheValue.firstSplit == Long.MIN_VALUE) {
				if(DEBUG) System.err.println("putEntryToCacheValue scndDay:"+pCacheValue.begin+":" + pCacheEntry);
				pCacheValue.firstSplit  = pCacheEntry.gmtTime;
				pCacheValue.secondDay   = pCacheEntry;
			} else {
				/**
				 * check if there is already a second entry set
				 */
				if(pCacheValue.secondDay == null) {
					/**
					 * if no second entry set, set it now 
					 */
					if(DEBUG) System.err.println("putEntryToCacheValue scndDay ok:"+pCacheValue.begin+":" + pCacheEntry);
					pCacheValue.firstSplit = pCacheEntry.gmtTime;
					pCacheValue.secondDay  = pCacheEntry;
				} 
			}

			long lGmtNextDayBegin = pGmtDayBegin + MSEC_PER_DAY;
			if(pSetNeighbours && pCacheEntry.endTime > lGmtNextDayBegin) {
				CacheValue lNextValue  = cache.get(pCacheEntry.endTime);
				if(lNextValue == null) {
					lNextValue = new CacheValue();
					lNextValue.begin = cache.calcSlot(cache.calcSlot(pCacheEntry.endTime));
					cache.put(pCacheEntry.endTime, lNextValue);
				}
				putEntryToCacheValue(lGmtNextDayBegin, lNextValue, pCacheEntry, false);
			}
		}
	}

	public static int getDay(long pGmtTime) {
		return getCacheEntry(pGmtTime).day;
	}
	public static int  getDayOfWeek(long pGmtTime) {
		return getCacheEntry(pGmtTime).dayOfWeek;
	}
	public static int  getMonth(long pGmtTime) {
		return getCacheEntry(pGmtTime).month;
	}
	public static int  getYear(long pGmtTime) {
		return getCacheEntry(pGmtTime).year;
	}


	public static long getUtcOffset(long pGmtTime) {
		return getCacheEntry(pGmtTime).utcOffset;
	}
	/**
	 *
	 * @return
	 *
	 */
	public static int getDayOfMonth(long pGmtTime) {
		return getCacheEntry(pGmtTime).dayOfMonth;
	}
	/**
	 *
	 * @param pGmtTime
	 * @param pInterval
	 * @return
	 *
	 */
	public static long addMonths(long pGmtTime, int pInterval) {

		/**
		 * set the correct offset to be able to return the correct
		 * time of day
		 */
		CacheEntry lCacheEntry = getCacheEntry(pGmtTime);
		long       lOffset     = pGmtTime - lCacheEntry.gmtTime;

		int  lYearCount  = pInterval / 12;
		int  lMonthCount = pInterval % 12; 

		if(lYearCount != 0) {
			Calendar lCalendar = Calendar.getInstance();
			lCalendar.setTimeInMillis(pGmtTime);
			lCalendar.add(Calendar.YEAR, lYearCount);
			pGmtTime = lCalendar.getTimeInMillis();
		}

		int  lDay     = getDay(pGmtTime);
		long lNewTime = pGmtTime;
		if(lMonthCount >= 0) {
			for(int i=0; i<lMonthCount; i++) {
				lNewTime = addMonth(lNewTime);
			}
		} else {
			throw new IllegalArgumentException("parameter pInterval must be >=0");
		}

		lCacheEntry = getCacheEntry(lNewTime);
		switch(lCacheEntry.month) {
		case 0: // January
		case 2: // Mar
		case 4: // May
		case 7: // Aug
		case 9: // Oct
		case 6:  // Jul
		case 11: // Dec
			break;
		case 1: // Feb
			if(lDay >= 28) {
				/**
				 * check if the feb has 28 or 29 days
				 */
				long lDayInFeb  = get1stDayInMonth(lNewTime);
				lDayInFeb      += 28 * MSEC_PER_DAY;
				lDayInFeb       = getMonth(lDayInFeb); 
				if(lDayInFeb == 1) {
					lDay = Math.min(lDay, 29);
				} else {
					lDay = 28;
				}
			}
			break;
		case 3: // Apr
		case 5: // June
		case 8: // Sep
		case 10: // Nov
			if(lDay == 31) {
				lDay = 30;
			}
			break;
		default:
			throw new IllegalArgumentException("not valid month:" + lCacheEntry.month);
		}

		lNewTime    = get1stDayInMonth(lNewTime) + MSEC_PER_DAY * (lDay-1);
		lCacheEntry = getCacheEntry(lNewTime);
		lNewTime    = Math.min(lCacheEntry.endTime, lCacheEntry.gmtTime + lOffset);

		return lNewTime;
	}

	public static long get1stDayInMonth(long pGmtTime) {

		CacheEntry lEntry  = getCacheEntry(pGmtTime);
		return lEntry.midTime - MSEC_PER_DAY * (lEntry.day - 1);
	}

	/**
	 *
	 * @param pGmtTime
	 * @return
	 *
	 */
	public static long addMonth(long pGmtTime) {

		if(DEBUG) System.err.println(">>>>>> addMonth  in:" + new Date(pGmtTime));

		long       l1stDayInMonth = get1stDayInMonth(pGmtTime);
		CacheEntry l1stDayEntry   = getCacheEntry(l1stDayInMonth);

		if(l1stDayEntry.nextMonth == Long.MIN_VALUE) {
			switch(l1stDayEntry.month) {
			case 0: // January
			case 2: // Mar
			case 4: // May
			case 7: // Aug
			case 9: // Oct
			case 6:  // Jul
			case 11: // Dec
				l1stDayEntry.nextMonth = l1stDayInMonth + 31 * MSEC_PER_DAY;
				break;
			case 1: // Feb, that's tricky
				long lNextMonthTime    = l1stDayInMonth + 30 * MSEC_PER_DAY;
				l1stDayEntry.nextMonth = get1stDayInMonth(lNextMonthTime);
				break;
			case 3: // Apr
			case 5: // June
			case 8: // Sep
			case 10: // Nov
				l1stDayEntry.nextMonth = l1stDayInMonth + 30 * MSEC_PER_DAY;
				break;
			default:
				throw new IllegalArgumentException("not valid month:" + l1stDayEntry.month);
			}
		}

		int lDay  = getDay(pGmtTime);
		switch(l1stDayEntry.month) {
		case 0: // January
			if(lDay >= 28) {
				Calendar lCalendar = Calendar.getInstance();
				lCalendar.setTimeInMillis(pGmtTime);
				lCalendar.add(Calendar.MONTH, 1);
				lDay               = lCalendar.get(Calendar.DATE);
			}
		case 2: // Mar
		case 4: // May
		case 7: // Aug
		case 9: // Oct
			if(lDay == 31) {
				lDay = 30;
			}
		case 6:  // Jul
		case 11: // Dec
		case 1: // Feb
		case 3: // Apr
		case 5: // June
		case 8: // Sep
		case 10: // Nov
			break;
		default:
			throw new IllegalArgumentException("not valid month:" + l1stDayEntry.month);
		}

		long   lOutTime = l1stDayEntry.nextMonth + (MSEC_PER_DAY/2) + MSEC_PER_DAY * Math.max(0, lDay -2); 
		if(DEBUG) System.err.println("<<<<<< addMonth out:" + new Date(lOutTime));
		return lOutTime;
	}
	/**
	 * 
	 *
	 * @param pGmtTime defines year and month
	 * @param pDay defines the day of month
	 * @return
	 *
	 */
	public static long setDay(long pGmtTime, int pDay) {

		/**
		 * get the offset to the day begin to set the correct time afterwards
		 */
		CacheEntry lCacheEntry    = getCacheEntry(pGmtTime);
		long       lOffset        = pGmtTime - lCacheEntry.gmtTime;

		long       l1stDayInMonth = get1stDayInMonth(pGmtTime);
		if(DEBUG) System.err.println("setDay 1stDayInMonth:" + DateUtil.format(l1stDayInMonth)); 
		lCacheEntry               = getCacheEntry(l1stDayInMonth); 
		if(DEBUG) System.err.println("setDay cachedEntry:" + lCacheEntry); 
		int        lDay           = pDay;
		switch(lCacheEntry.month) {
		case 0: // January
		case 2: // Mar
		case 4: // May
		case 7: // Aug
		case 9: // Oct
		case 6:  // Jul
		case 11: // Dec
			break;
		case 1: // Feb
			if(pDay >= 28) {
				/**
				 * check if the feb has 28 or 29 days
				 */
				long lDayInFeb  = get1stDayInMonth(pGmtTime);
				lDayInFeb      += 28 * MSEC_PER_DAY;
				lDayInFeb       = getMonth(lDayInFeb); 
				if(lDayInFeb == 1) {
					pDay = Math.min(pDay, 29);
				} else {
					pDay = 28;
				}
			}
			break;
		case 3: // Apr
		case 5: // June
		case 8: // Sep
		case 10: // Nov
			if(pDay == 31) {
				pDay = 30;
			}
			break;
		default:
			throw new IllegalArgumentException("not valid month:" + lCacheEntry.month);
		}

		if(DEBUG) System.err.println("setDay lDay:" + lDay); 
		long   lOutTime = l1stDayInMonth + MSEC_PER_DAY * (lDay - 1);
		lCacheEntry     = getCacheEntry(lOutTime);
		lOutTime        = Math.min(lCacheEntry.endTime, lCacheEntry.gmtTime + lOffset);
		if(DEBUG) System.err.println("setDay lOuttime:" + DateUtil.format(lOutTime)); 
		return lOutTime;
	}

	/**
	 * erstelle eine liste mit Testdaten/tagen
    TimeZone:Eire start:1916.5.22 0:35:38.999 1st:20 3rd:22
    TimeZone:Europe/Dublin start:1916.5.22 0:35:38.999 1st:20 3rd:22
	 */
	public static void main(String[] args) throws Exception {

		PrintStream lPW = new PrintStream("./testData.txt");

		Calendar lGMTCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		for(String lTimeZoneID : TimeZone.getAvailableIDs()) {
			TimeZone lTimeZone = TimeZone.getTimeZone(lTimeZoneID);

			Calendar lCalendar = Calendar.getInstance(lTimeZone);

			/**
			 * start around 1860 and end in 2070
			 */
			long     lStart    = 0 - 100 * 365 * MSEC_PER_DAY;
			long     lEnd      = 0 + 100 * 365 * MSEC_PER_DAY;
			/**
			 * iterate day wise
			 */
			if(DEBUG) System.err.println("TimeZone:" + lTimeZoneID);
			for(long lGmtDay=lStart; lGmtDay<lEnd; lGmtDay += MSEC_PER_DAY) {

				/**
				 * just doublecheck my assumptions
				 */
				lGMTCalendar.setTimeInMillis(lGmtDay);
				if(        lGMTCalendar.get(Calendar.HOUR) != 0
						|| lGMTCalendar.get(Calendar.MINUTE) != 0
						|| lGMTCalendar.get(Calendar.SECOND) != 0
						|| lGMTCalendar.get(Calendar.MILLISECOND) != 0) {

					printCalendar(System.err, lCalendar);
					throw new IllegalArgumentException("failure for:" + lGMTCalendar.getTime());
				}
				lGMTCalendar.setTimeInMillis(lGmtDay+MSEC_PER_DAY-1);
				if(        lGMTCalendar.get(Calendar.HOUR_OF_DAY) != 23
						|| lGMTCalendar.get(Calendar.MINUTE) != 59
						|| lGMTCalendar.get(Calendar.SECOND) != 59
						|| lGMTCalendar.get(Calendar.MILLISECOND) != 999) {
					throw new IllegalArgumentException("failure for:" + lGMTCalendar.getTime());
				}

				lCalendar.setTimeInMillis(lGmtDay);
				int lFirstDate  = lCalendar.get(Calendar.DATE);
				lCalendar.setTimeInMillis(lGmtDay + MSEC_PER_DAY/2);
				int lSecondDate = lCalendar.get(Calendar.DATE);
				lCalendar.setTimeInMillis(lGmtDay + MSEC_PER_DAY-1);
				int lThirdDate  = lCalendar.get(Calendar.DATE);

				if(lFirstDate != lThirdDate && lFirstDate != lSecondDate && lSecondDate != lThirdDate) {
					lPW.print("    TimeZone:" + lTimeZoneID + " start:");
					printCalendar(lPW, lCalendar);
					lPW.println(" 1st:" + lFirstDate + " 3rd:" + lThirdDate);
				}
			}

		}

		lPW.close();
	}

	/**
	 *
	 * @param pPS
	 * @param pCalendar
	 *
	 */
	private static void printCalendar(PrintStream pPS, Calendar pCalendar) {
		pPS.print(pCalendar.get(Calendar.YEAR));
		pPS.print('.');
		pPS.print(pCalendar.get(Calendar.MONTH)+1);
		pPS.print('.');
		pPS.print(pCalendar.get(Calendar.DATE));
		pPS.print(' ');
		pPS.print(pCalendar.get(Calendar.HOUR_OF_DAY));
		pPS.print(':');
		pPS.print(pCalendar.get(Calendar.MINUTE)+1);
		pPS.print(':');
		pPS.print(pCalendar.get(Calendar.SECOND));
		pPS.print('.');
		pPS.print(pCalendar.get(Calendar.MILLISECOND));
	}

	/**
	 *
	 * @param pGmtTime
	 * @param pCount
	 * @return
	 *
	 */
	public static long addDays(long pGmtTime, int pCount) {
		CacheEntry lEntry    = getCacheEntry(pGmtTime);
		long       lOffset   = pGmtTime-lEntry.gmtTime;
		long       lEnd      = lEntry.midTime + pCount * MSEC_PER_DAY;
		CacheEntry lEndEntry = getCacheEntry(lEnd); 
		return lEndEntry.gmtTime + lOffset;
	}

	/**
	 *
	 * @param year
	 * @param month
	 * @param day
	 * @return
	 *
	 */
	public static long getStartOfDay(int year, int month, int day) {
		CacheEntry lCacheEntry = getCacheEntry(year, month, day);		
		return lCacheEntry.gmtTime;
	}
	public static long getEndOfDay(int year, int month, int day) {
		CacheEntry lCacheEntry = getCacheEntry(year, month, day);		
		return lCacheEntry.endTime;
	}

	/**
	 *
	 * @param pBegin
	 * @param pEnd
	 * @return
	 *
	 */
	public static long calcDaySpan(long pBegin, long pEnd) {
		CacheEntry lBeginEntry = getCacheEntry(pBegin);
		/**
		 * end dates are often given as 1974.01.31 00:00:00.000
		 * to make calculation correctly subtract 10 msec if appropriate
		 */
		CacheEntry lEndEntry   = getCacheEntry(pEnd);
		if(pEnd == lEndEntry.gmtTime) {
			lEndEntry = getCacheEntry(pEnd - 10);
		}
		long        lDaySpan   = 1 + (lEndEntry.midTime-lBeginEntry.midTime) / MSEC_PER_DAY;
		return lDaySpan;
	}

	/**
	 *
	 * @param pGmtTime
	 * @param pMonth
	 * @return
	 *
	 */
	public static long setMonth(long pGmtTime, int pMonth) {

		if(pMonth<0||pMonth>11) {
			throw new IllegalArgumentException("parameter pMonth has to be between 0 and 11");
		}
		
		CacheEntry lEntry  = getCacheEntry(pGmtTime);
		long       lOffset = pGmtTime - lEntry.gmtTime;
		lEntry             = getCacheEntry(lEntry.year, pMonth, lEntry.day);
		long       lTime   = Math.min(lEntry.endTime, lEntry.gmtTime+lOffset);

		return lTime;
	}

	/**
	 *
	 * @param pGmtTime
	 * @param pYear
	 * @return
	 *
	 */
	public static long setYear(long pGmtTime, int pYear) {

		CacheEntry lEntry  = getCacheEntry(pGmtTime);
		long       lOffset = pGmtTime - lEntry.gmtTime;
		lEntry             = getCacheEntry(pYear, lEntry.month, lEntry.day);
		long       lTime   = Math.min(lEntry.endTime, lEntry.gmtTime+lOffset);

		return lTime;
	}

	/**
	 *
	 * @param pGmtTime
	 * @return
	 *
	 */
	public static int getMilliSecond(long pGmtTime) {
		CacheEntry lEntry  = getCacheEntry(pGmtTime);
		long       lValue  = (pGmtTime - lEntry.gmtTime) % MSEC_PER_HOUR;
		lValue            %= MSEC_PER_MINUTE;
		lValue            %= MSEC_PER_SECOND;
		return (int) lValue;
	}

	/**
	 *
	 * @param pGmtTime
	 * @return
	 *
	 */
	public static int getSecond(long pGmtTime) {
		CacheEntry lEntry  = getCacheEntry(pGmtTime);
		long       lValue  = (pGmtTime - lEntry.gmtTime) % MSEC_PER_HOUR;
		lValue            %= MSEC_PER_MINUTE;
		lValue            /= MSEC_PER_SECOND;
		return (int) lValue;
	}

	/**
	 *
	 * @param pGmtTime
	 * @return
	 *
	 */
	public static int getMinute(long pGmtTime) {
		CacheEntry lEntry  = getCacheEntry(pGmtTime);
		long       lValue  = (pGmtTime - lEntry.gmtTime) % MSEC_PER_HOUR;
		lValue            /= MSEC_PER_MINUTE;
		return (int) lValue;
	}

	/**
	 *
	 * @param pGmtTime
	 * @return
	 *
	 */
	public static int getHourOfDay(long pGmtTime) {
		CacheEntry lEntry = getCacheEntry(pGmtTime);
		long       lValue = Long.MIN_VALUE;
		/**
		 * if the difference between day start and end is not 24 hours
		 * it means that a day light savong time change or other has taken
		 * place at this date. Thus, use Calendar to calc hour of day
		 */
		if(lEntry.endTime-lEntry.gmtTime == (MSEC_PER_DAY-1)) {
			lValue = (pGmtTime - lEntry.gmtTime) / MSEC_PER_HOUR;
		} else {
			Calendar lCalendar = Calendar.getInstance();
			lCalendar.setTimeInMillis(pGmtTime);
			lValue = lCalendar.get(Calendar.HOUR_OF_DAY);
		}
		return (int) lValue;
	}
}
