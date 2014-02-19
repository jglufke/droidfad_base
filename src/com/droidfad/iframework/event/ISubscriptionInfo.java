package com.droidfad.iframework.event;

import com.droidfad.iframework.event.IEvent.DataType;
import com.droidfad.iframework.event.IEvent.EventType;

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
 *   limitations under the License.<br><br><br>
 *  
 * -----------------------------------------------------------------------<br>----  
 *   
 * class that defines the information that has to be given to IEventPublisher
 * by a subscriber when it is registered. It contains a description of the 
 * events by which the subscriber wants to be notified.
 * An implementation should not create an instance of this interface directly
 * but should use IEventPublisher.createSubscriptionInfo() method  
 * 
 */
public interface ISubscriptionInfo {
	/**
	 * @return the data type for which should be subscriped
	 */
	public abstract DataType  getDataType();
	/**
	 * @return the event type for which should be subscriped
	 */
	public abstract EventType getEventType();
	/**
	 * @return the class name of the ADao subclass or ReferenceType subclass for 
	 * which should be subscribed. wildcard '*' might be returned for all
	 *
	 */
	public abstract String getType();
	/**
	 * @return the instance name of the ADao subclass for 
	 * which should be subscribed. wildcard '*' might be returned for all
	 */
	public abstract String getInstance();
	/**
	 * @return the attribute name of the ADao subclass for 
	 * which should be subscribed. wildcard '*' might be returned for all
	 */
	public abstract String getAttribute();

}