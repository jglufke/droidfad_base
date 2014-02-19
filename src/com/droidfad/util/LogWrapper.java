/**
 * 
 */
package com.droidfad.util;

import com.droidfad.log.ILog;

/**
 * 
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
 * -----------------------------------------------------------------------<br>
 * class to wrap the used ILog interface. Introduced because DroidFad and
 * DroidFadJSE might need different implementations 
 */
public class LogWrapper {

	private static ILog log;
	public static void setLogImpl(ILog pLog) {
		log = pLog;
	}
	
	public void setLogLevel() {
		
	}
	
	public static void e(String pLogTag, String pMessage) {
		log.e(pLogTag, pMessage);
	}
	public static void e(String pLogTag, String pMessage, Throwable pThrowable) {
		log.e(pLogTag, pMessage, pThrowable);
	}
	public static void w(String pLogTag, String pMessage) {
		log.w(pLogTag, pMessage);
	}
	public static void w(String pLogTag, String pMessage, Throwable pThrowable) {
		log.w(pLogTag, pMessage, pThrowable);
	}
	public static void i(String pLogTag, String pMessage) {
		log.i(pLogTag, pMessage);
	}
	public static void i(String pLogTag, String pMessage, Throwable pThrowable) {
		log.i(pLogTag, pMessage, pThrowable);
	}
	public static void d(String pLogTag, String pMessage) {
		log.d(pLogTag, pMessage);
	}
	public static void d(String pLogTag, String pMessage, Throwable pThrowable) {
		log.d(pLogTag, pMessage, pThrowable);
	}
}
