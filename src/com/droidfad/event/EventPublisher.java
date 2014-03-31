package com.droidfad.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.droidfad.data.ADao;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.iframework.event.IEvent;
import com.droidfad.iframework.event.IEvent.DataType;
import com.droidfad.iframework.event.IEvent.EventType;
import com.droidfad.iframework.event.IEventPublisher;
import com.droidfad.iframework.event.ISubscriber;
import com.droidfad.iframework.event.ISubscriptionInfo;
import com.droidfad.util.LogWrapper;

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
public class EventPublisher implements IEventPublisher {
	
	public static final String WILDCARD = "*";

	private static final String LOGTAG = EventPublisher.class.getSimpleName();

	/**
	 * the worker of eventing
	 */
	ExecutorService eventWorker = Executors.newSingleThreadExecutor();
	
	/**
	 * key    - dataType
	 * key    - eventType
	 * key    - type name
	 * key    - instance name
	 * key    - attribute name
	 * value  - list of subscribers
	 */
	private ConcurrentHashMap<String,             // dataType 
		ConcurrentHashMap<String,                 // eventType 
			ConcurrentHashMap<String,             // type name
				ConcurrentHashMap<String,         // instance name 
					ConcurrentHashMap<String,     // attribute name
					 ConcurrentLinkedQueue<ISubscriber>>>>>>
	
	subscriberMap = 
		new ConcurrentHashMap<String, ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>>>>>>();

	/**
	 * key   - subscriber
	 * value - list of lists which contain the subscriber
	 */
	private ConcurrentHashMap<ISubscriber, 
		ConcurrentLinkedQueue<
			ConcurrentLinkedQueue<ISubscriber>>>
	
	subscriberListMap = 
		new ConcurrentHashMap<ISubscriber, ConcurrentLinkedQueue<ConcurrentLinkedQueue<ISubscriber>>>();
	
	private Object subscribeSync   = new Object();
	private Object unsubscribeSync = new Object();
	private Object broadcastSync   = new Object();

	
	public EventPublisher(){ 
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.droidfad.iframework.event.IEventPublisher#publish(com.droidfad.iframework.event.IEvent)
	 */
	@Override
	public void publish(IEvent pEvent) {

		if(pEvent == null) {
			throw new IllegalArgumentException("parameter pEvent must not be null");
		}
		
		synchronized (broadcastSync) {
			class EventRunner implements Runnable {
				private IEvent event = null;
				public EventRunner(IEvent pEvent) {
					event = pEvent;
				}
				public void run() {
					broadcastImpl(event);
				}
			}
			eventWorker.execute(new EventRunner(pEvent));
		}
	}

	private void broadcastImpl(IEvent pEvent) {
		/**
		 * key    - dataType
		 * key    - eventType
		 * key    - type name
		 * key    - instance name
		 * key    - attribute name
		 * value  - list of subscribers
		 */
		ArrayList<ISubscriber> lSubscriberList = new ArrayList<ISubscriber>();
		for(String lDataType : new String[]{ WILDCARD, "" + pEvent.getDataType()}) {
			ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>>>>> 
			lDataTypeMap = subscriberMap.get(lDataType);
			if(lDataTypeMap != null) {
				for(String lEventType : new String[]{ WILDCARD, "" + pEvent.getEventType()}) {
					ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>>>> 
					lEventTypeMap = lDataTypeMap.get(lEventType);
					if(lEventTypeMap != null) {
						for(String lTypeName : new String[]{ WILDCARD, pEvent.getObjectType().getSimpleName()}) {
							ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>>> 
							lTypeMap = lEventTypeMap.get(lTypeName);
							if(lTypeMap != null) {
								for(String lInstanceName : new String[]{ WILDCARD, pEvent.getObjectName()}) {
									ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>> 
									lInstanceMap = lTypeMap.get(lInstanceName);
									switch(pEvent.getEventType()) {
									case created:
									case deleted:
										if(lInstanceMap != null) {
										for(ConcurrentLinkedQueue<ISubscriber> lSubscriberQueue : lInstanceMap.values()) {
											lSubscriberList.addAll(lSubscriberQueue);
										}
										}
										break;
									case updated:
										String lEventAttributeName = "" + pEvent.getAttributeEnum();
										if(lInstanceMap != null && lEventAttributeName != null) {
											for(String lAttributeName : new String[]{ WILDCARD, lEventAttributeName }) {
												ConcurrentLinkedQueue<ISubscriber> lAttributeList = lInstanceMap.get(lAttributeName);
												if(lAttributeList != null) {
													lSubscriberList.addAll(lAttributeList);
												}
											} // for(String lAttributeName : new String[]{ WILDCARD, lEventAttributeName }) {
										} // if(lInstanceMap != null && lEventAttributeName != null) {
										break;
									default:
										throw new IllegalArgumentException("not handled event type:" + pEvent.getEventType());
									}
								} // for(String lInstanceName : new String[]{ WILDCARD, pEvent.getObjectName()}) {
							} // if(lTypeMap != null) {
						} // for(String lTypeName : new String[]{ WILDCARD, pEvent.getObjectType()}) {
					} // if(lEventTypeMap != null) {
				} // for(String lEventType : new String[]{ WILDCARD, "" + pEvent.getEventType()}) {
			} // if(lDataTypeMap != null) {
		}
		for(ISubscriber lSubscriber : lSubscriberList) {
			try {
				lSubscriber.handleEvent(pEvent);
			} catch(Exception e) {
				LogWrapper.e(LOGTAG, "exception for handle event:" + lSubscriber + ":" + pEvent, e);
			}
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.yaffa.iframework.event.IEventBroadcaster#addSubscriber(org.yaffa.iframework.event.ISubscriber)
	 */
	@Override
	public void addSubscriber(ISubscriber pSubscriber) {
		synchronized (subscribeSync) {
			if(pSubscriber == null) {
				throw new IllegalArgumentException("parameter pSubscriber must not be null");
			}
			Collection<ISubscriptionInfo> lSubscriptionInfo = pSubscriber.getSubscriptionInfo();
			if(lSubscriptionInfo != null) {
				for(ISubscriptionInfo lInfo : lSubscriptionInfo) {
					addSubscriber(pSubscriber, lInfo);
				}
			}
		}
	}

	private void addSubscriber(ISubscriber pSubscriber, ISubscriptionInfo pInfo) {
		/**
		 * key    - dataType
		 * key    - eventType
		 * key    - type name
		 * key    - instance name
		 * key    - attribute name
		 * value  - list of subscribers
		 */
		/**
		 * ---------------------------------------------------------------------
		 * key    - dataType
		 */
		String   lDataType     = null;
		DataType lDataTypeEnum = pInfo.getDataType();
		if(lDataTypeEnum == null || lDataTypeEnum == DataType.all) {
			lDataType = WILDCARD;
		} else {
			lDataType = lDataTypeEnum.toString();
		}

		ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>>>>> 
		lDataTypeMap = subscriberMap.get(lDataType);
		if(lDataTypeMap == null) {
			lDataTypeMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>>>>>();
			subscriberMap.put(lDataType, lDataTypeMap);
		}
		/**
		 * ---------------------------------------------------------------------
		 * key    - eventType
		 */
		String    lEventType     = null; 
		EventType lEventTypeEnum = pInfo.getEventType(); 
		if(lEventTypeEnum == null || lEventTypeEnum == EventType.all) {
			lEventType = WILDCARD;
		} else {
			lEventType = lEventTypeEnum.toString();
		}
		ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>>>> 
		lEventTypeMap = lDataTypeMap.get(lEventType);
		if(lEventTypeMap == null) {
			lEventTypeMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>>>>();
			lDataTypeMap.put(lEventType, lEventTypeMap);
		}
		/**
		 * ---------------------------------------------------------------------
		 * key    - type name
		 */
		String lTypeName = pInfo.getType();
		if(lTypeName == null) {
			lTypeName = WILDCARD;
		}
		ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>>> 
		lTypeMap = lEventTypeMap.get(lTypeName);
		if(lTypeMap == null) {
			lTypeMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>>>();
			lEventTypeMap.put(lTypeName, lTypeMap);
		}
		/**
		 * ---------------------------------------------------------------------
		 * key    - instance name
		 */
		String lInstanceName = pInfo.getInstance();
		if(lInstanceName == null) {
			lInstanceName = WILDCARD;
		}
		ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>> 
		lInstanceMap = lTypeMap.get(lInstanceName);
		if(lInstanceMap == null) {
			lInstanceMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<ISubscriber>>();
			lTypeMap.put(lInstanceName, lInstanceMap);
		}
		/**
		 * ---------------------------------------------------------------------
		 * key    - attribute name
		 */
		String lAttributeName = pInfo.getAttribute();
		if(lAttributeName == null) {
			lAttributeName = WILDCARD;
		}
		ConcurrentLinkedQueue<ISubscriber> 
		lSubscriberList = lInstanceMap.get(lAttributeName);
		if(lSubscriberList == null) {
			lSubscriberList = new ConcurrentLinkedQueue<ISubscriber>();
			lInstanceMap.put(lAttributeName, lSubscriberList);
		}
		/**
		 * ---------------------------------------------------------------------
		 * value  - list of subscribers
		 */
		lSubscriberList.add(pSubscriber);

		/**
		 * ---------------------------------------------------------------------
		 * enter the subscriber to the map which contains a reference to all
		 * 
		 */
		ConcurrentLinkedQueue<ConcurrentLinkedQueue<ISubscriber>> 
		lSubscriberListList = subscriberListMap.get(pSubscriber);
		if(lSubscriberListList == null) {
			lSubscriberListList = new ConcurrentLinkedQueue<ConcurrentLinkedQueue<ISubscriber>>();
			subscriberListMap.put(pSubscriber, lSubscriberListList);
		}
		lSubscriberListList.add(lSubscriberList);
	}
	/*
	 * (non-Javadoc)
	 * @see org.yaffa.iframework.event.IEventBroadcaster#removeSubscriber(org.yaffa.iframework.event.ISubscriber)
	 */
	@Override
	public void removeSubscriber(ISubscriber pSubscriber) {
		synchronized (unsubscribeSync) {
			/**
			 * key    - dataType
			 * key    - eventType
			 * key    - type name
			 * key    - instance name
			 * key    - attribute name
			 * value  - list of subscribers
			 */
			ConcurrentLinkedQueue<ConcurrentLinkedQueue<ISubscriber>> 
			lSubscriberListList = subscriberListMap.get(pSubscriber);
			if(lSubscriberListList != null) {
				for(ConcurrentLinkedQueue<ISubscriber> lSubscriberList : lSubscriberListList) {
					while(lSubscriberList.remove(pSubscriber)) {
					}
				}
			}
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.yaffa.iframework.event.IEventBroadcaster#createEvent(java.lang.Object, org.yaffa.iframework.event.IEvent.DataType, org.yaffa.iframework.event.IEvent.EventType, java.lang.String, java.lang.String, java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public IEvent createEvent(Object pContext, DataType pDataType, EventType pEventType,
			Class<? extends ADao> pObjectType, String pObjectName, Object pSource,
			AttributesEnum pAttributeName, Object pOldValue, Object pNewValue) {

		return new DEvent(pContext, pDataType, pEventType,
				pObjectType, pObjectName, pSource,
				pAttributeName, pOldValue, pNewValue);
	}
	/*
	 * (non-Javadoc)
	 * @see org.yaffa.iframework.event.IEventBroadcaster#createSubscriptionInfo(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public ISubscriptionInfo createSubscriptionInfo(DataType pDataType,
			EventType pEventType, String pType, String pInstance, String pAttribute) {

		return new SubscriptionInfo(pDataType, pEventType, pType, pInstance, pAttribute);
	}
	/*
	 * (non-Javadoc)
	 * @see org.yaffa.iframework.event.IEventBroadcaster#createSubscriptionInfo()
	 */
	@Override
	public ISubscriptionInfo createSubscriptionInfo() {

		return new SubscriptionInfo();
	}
	/* (non-Javadoc)
	 * @see com.droidfad.iframework.event.IEventPublisher#removeAllSubscribers()
	 */
	@Override
	public void removeAllSubscribers() {
		synchronized (subscribeSync) {
			subscriberListMap = new ConcurrentHashMap<ISubscriber, ConcurrentLinkedQueue<ConcurrentLinkedQueue<ISubscriber>>>();
			subscriberMap     = new ConcurrentHashMap<String, ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentLinkedQueue<ISubscriber>>>>>>();
		}
	}	
}
