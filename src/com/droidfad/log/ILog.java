/**
 * 
 */
package com.droidfad.log;

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
 *   limitations under the License.<br>
 * -----------------------------------------------------------------------<br>
 *
 * interface to define log functionality
 */
public interface ILog {

	public void e(String pLogTag, String pMessage);
	public void e(String pLogTag, String pMessage, Throwable pThrowable);

	public void w(String pLogTag, String pMessage);
	public void w(String pLogTag, String pMessage, Throwable pThrowable);

	public void i(String pLOGTAG, String pMessage);
	public void i(String pLOGTAG, String pMessage, Throwable pThrowable);

	public void d(String pLOGTAG, String pMessage);
	public void d(String pLOGTAG, String pMessage, Throwable pThrowable);
	
}
