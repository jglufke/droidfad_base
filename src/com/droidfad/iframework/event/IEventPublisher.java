package com.droidfad.iframework.event;

import com.droidfad.data.ADao;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.iframework.service.IService;

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
 * -----------------------------------------------------------------------<br>-----
 * 
 * this service interface defines the methods that are used to manage events. 
 */
public interface IEventPublisher extends IService {

	/**
	 * to publish/broadcast an event to all registered ISubscribers
	 * call this method
	 *
	 * @param event
	 *
	 */
	public void publish(IEvent event);
	
	/**
	 * an ISubscriber that should be notified by events has to 
	 * be registered with this method. If the subscriber is already
	 * registered it will be added another time.
	 *
	 * @param subscriber
	 *
	 */
	public void addSubscriber(ISubscriber subscriber);
	
	/**
	 * remove all registrations of subscriber from the service. If 
	 * subscriber was added multiple times all registrations will
	 * be removed
	 *
	 * @param subscriber
	 *
	 */
	public void removeSubscriber(ISubscriber subscriber);

	/**
	 * create an event instance which can then be published by
	 * method publish 
	 *
	 * @param pContext
	 * @param pDataType
	 * @param pEventType
	 * @param pObjectType
	 * @param pObjectName
	 * @param pSource
	 * @param pAttributeName
	 * @param pOldValue
	 * @param pNewValue
	 * @return
	 *
	 */
	public IEvent createEvent(Object pContext, IEvent.DataType pDataType, IEvent.EventType pEventType,
			Class<? extends ADao> pObjectType, String pObjectName,
			Object pSource, AttributesEnum pAttributeName, Object pOldValue,
			Object pNewValue);

	/**
	 * create a subscription info that causes a subscription to the events
	 * defined by the given parameters 
	 * 
	 * @param pDataType
	 * @param pEventType
	 * @param pType
	 * @param pInstance
	 * @param pAttribute
	 * @return
	 */
	public ISubscriptionInfo createSubscriptionInfo(IEvent.DataType pDataType, IEvent.EventType pEventType, String pType,
			String pInstance, String pAttribute);
	
	/**
	 * create an subscription info that causes an subscription to all events
	 * @return
	 */
	public ISubscriptionInfo createSubscriptionInfo();

	/**
	 * remove all registered subscriber instance from the service
	 */
	public void removeAllSubscribers();
}
