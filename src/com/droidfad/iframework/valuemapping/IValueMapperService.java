package com.droidfad.iframework.valuemapping;

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
 * -----------------------------------------------------------------------<br>---
 * This service has to be used to map instances of classes to strings and 
 * vice versa. this can be used e.g. for gui editors or persistency mechanisms.
 * If the appropriate mapper is not available for a class an implementation
 * class of IValueMapper can be implemented to provide the  correct mapping.
 * This instance is automatically registered at startup and can be invoked by
 * calling the service IValueMapperService
 *
 */
public interface IValueMapperService extends IService {
	
	public static class NoValueMapperDefinedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public NoValueMapperDefinedException(String pMessage) {
			super(pMessage);
		}
	}
	
	public String mapValue2String(Class<?> pType, Object pValue);

	public <T> T mapString2Value(Class<?> pType, String pValueString);
	
}
