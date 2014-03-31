package com.droidfad.iframework.valuemapping;

import com.droidfad.iframework.service.IService;

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
 * ---------------------------------------------------------------
 * service that is responsible for mapping an instance 
 * of an specific type to a String and vice versa
 * @param <T>
 *
 */
public interface IValueMapper<T> extends IService {
	/**
	 * 
	 * map the object pValue to a string representation
	 * @param pValue
	 * @return
	 *
	 */
	public String   mapValue2String(Object pValue);
	/**
	 * map an string representation pValueString to
	 * an object of type T
	 *
	 * @param pValueString
	 * @return
	 *
	 */
	public T        mapString2Value(String pValueString);
	
}
