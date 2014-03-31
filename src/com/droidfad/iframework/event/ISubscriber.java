package com.droidfad.iframework.event;

import java.util.Collection;

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
 * ---------------------------------------------------------------------
 * 
 * Any class that wants to subscribe to IEventPublisher has to implement
 * this interface
 *
 */
public interface ISubscriber {
	/**
	 * 
	 * @return the information to which events ISubscriber wants to subscribe
	 *
	 */
	public Collection<ISubscriptionInfo> getSubscriptionInfo();
	/**
	 * handling the event. This method is called by the IPublisher service
	 * @param pEvent
	 */
	public void handleEvent(IEvent pEvent);
}
