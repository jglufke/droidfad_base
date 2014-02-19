package com.droidfad.event;

import com.droidfad.data.ADao;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.iframework.event.IEvent;

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
 */
class DEvent implements IEvent {

	private Object context;
	private DataType dataType;
	private EventType eventType;
	private Class<? extends ADao> objectType;
	private String objectName;
	private Object source;
	private long   timestamp;
	private AttributesEnum attributeName;
	private Object oldValue;
	private Object newValue;

	public DEvent(Object pContext, DataType pDataType, EventType pEventType,
	Class<? extends ADao> pObjectType, String pObjectName, Object pSource,
	AttributesEnum pAttributeName, Object pOldValue, Object pNewValue) {
		super();
		
		context       = pContext;
		dataType      = pDataType;
		eventType     = pEventType;
		objectType    = pObjectType;
		objectName    = pObjectName;
		source        = pSource;
		attributeName = pAttributeName;
		oldValue      = pOldValue;
		newValue      = pNewValue;

		timestamp = System.currentTimeMillis();
	}

	public Class<? extends ADao> getObjectType() {
		return objectType;
	}

	public String getObjectName() {
		return objectName;
	}

	public Object getSource() {
		return source;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public AttributesEnum getAttributeEnum() {
		return attributeName;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	public DataType getDataType() {
		return dataType;
	}

	public EventType getEventType() {
		return eventType;
	}

	public Object getContext() {
		return context;
	}

	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder("Event:");
		lBuilder.append(dataType);
		lBuilder.append(':');
		lBuilder.append(eventType);
		lBuilder.append(':');
		lBuilder.append(objectType);
		lBuilder.append('.');
		lBuilder.append(objectName);
		lBuilder.append('.');
		lBuilder.append(attributeName);
		lBuilder.append(':');
		lBuilder.append(oldValue);
		lBuilder.append('-');
		lBuilder.append(newValue);
		return lBuilder.toString();
	}
}
