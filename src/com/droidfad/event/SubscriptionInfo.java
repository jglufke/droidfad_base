package com.droidfad.event;

import com.droidfad.iframework.event.IEvent.DataType;
import com.droidfad.iframework.event.IEvent.EventType;
import com.droidfad.iframework.event.ISubscriptionInfo;

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
class SubscriptionInfo implements ISubscriptionInfo {

	private DataType  dataType  = DataType.all;
	private EventType eventType = EventType.all;
	private String type      = "*";
	private String instance  = "*"; 
	private String attribute = "*";

	public SubscriptionInfo() {
		super();
	}

	public SubscriptionInfo(DataType pDataType, EventType pEventType, String pType,
			String pInstance, String pAttribute) {
		super();
		dataType = pDataType;
		eventType = pEventType;
		type = pType;
		instance = pInstance;
		attribute = pAttribute;
	}
	/* (non-Javadoc)
	 * @see org.yaffa.event.ISubscriptionInfo#getDataType()
	 */
	@Override
	public DataType getDataType() {
		return dataType;
	}
	public void setDataType(DataType pDataType) {
		dataType = pDataType;
	}
	/* (non-Javadoc)
	 * @see org.yaffa.event.ISubscriptionInfo#getEventType()
	 */
	@Override
	public EventType getEventType() {
		return eventType;
	}
	public void setEventType(EventType pEventType) {
		eventType = pEventType;
	}
	/* (non-Javadoc)
	 * @see org.yaffa.event.ISubscriptionInfo#getType()
	 */
	@Override
	public String getType() {
		return type;
	}
	public void setType(String pType) {
		type = pType;
	}
	/* (non-Javadoc)
	 * @see org.yaffa.event.ISubscriptionInfo#getInstance()
	 */
	@Override
	public String getInstance() {
		return instance;
	}
	public void setInstance(String pInstance) {
		instance = pInstance;
	}
	/* (non-Javadoc)
	 * @see org.yaffa.event.ISubscriptionInfo#getAttribute()
	 */
	@Override
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String pAttribute) {
		attribute = pAttribute;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result
		+ ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result
		+ ((eventType == null) ? 0 : eventType.hashCode());
		result = prime * result
		+ ((instance == null) ? 0 : instance.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubscriptionInfo other = (SubscriptionInfo) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (dataType == null) {
			if (other.dataType != null)
				return false;
		} else if (!dataType.equals(other.dataType))
			return false;
		if (eventType == null) {
			if (other.eventType != null)
				return false;
		} else if (!eventType.equals(other.eventType))
			return false;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder(super.toString());
		lBuilder.append(':');
		lBuilder.append(dataType);
		lBuilder.append(':');
		lBuilder.append(eventType);
		lBuilder.append(':');
		lBuilder.append(type);
		lBuilder.append('.');
		lBuilder.append(instance);
		lBuilder.append('.');
		lBuilder.append(attribute);
		return lBuilder.toString();
	}
}
