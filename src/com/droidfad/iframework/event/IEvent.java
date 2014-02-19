package com.droidfad.iframework.event;

import com.droidfad.data.ADao;
import com.droidfad.data.ADao.AttributesEnum;

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
 *
 * ----------------------------------------------------------------------
 * 
 * this interface defines the methods of IEvent instances that are published
 * by IEventPublisher service. 
 */
public interface IEvent {
	
	/**
	 * defines if IEvent describes an object or a reference
	 */
	public enum DataType  { object, reference, all };
	/**
	 * defines which kind of event IEvent instance is
	 * - updated - a value of an existing object/reference changed
	 * - created - an object/reference has been created
	 * - deleted - an object/reference has been deleted
	 */
	public enum EventType { updated, created, deleted, all };
	
	/**
	 * context can be any object that is related to a specific
	 * event. It can e.g. be used to data from the event source
	 * to the eventhandler
	 * @return
	 */
	public Object                getContext();
	/**
	 * 
	 * get the source of the event
	 * @return
	 *
	 */
	public Object                getSource();
	/**
	 * 
	 * get the time when the event has been created
	 * @return
	 *
	 */
	public long                  getTimestamp();
	/**
	 * 
	 * get the datatype of the event (object/reference)
	 * @return
	 *
	 */
	public DataType              getDataType();
	/**
	 * 
	 * get the event type of the event (update/create/delete)
	 * @return
	 *
	 */
	public EventType             getEventType();
	/**
	 * 
	 * get the object/reference type of the object that
	 * has been updated/created/deleted
	 * @return
	 *
	 */
	public Class<? extends ADao> getObjectType();
	
	/**
	 * get the name of the object/reference
	 *
	 * @return
	 *
	 */
	public String                getObjectName();
	
	/**
	 * 
	 * in case of an object update this method returns the 
	 * attribute name of the updated attribute
	 * @return
	 *
	 */
	public AttributesEnum        getAttributeEnum();
	
	/**
	 * in case of an object update this method returns the old 
	 * value before the update
	 *
	 * @return
	 *
	 */
	public Object                getOldValue();
	/**
	 * in case of an object update this methods returns the new
	 * value after the update
	 *
	 * @return
	 *
	 */
	public Object                getNewValue();
}
