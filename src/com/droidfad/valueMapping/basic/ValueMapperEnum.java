package com.droidfad.valueMapping.basic;

import com.droidfad.iframework.valuemapping.IValueMapper;

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
 * -----------------------------------------------------------------------<br>
 *
 * because the implementation of the enum mapping depends heavily on
 * the concrete sub class of Enum.class, the generic approach can
 * not be used for mapping enums
 */
public class ValueMapperEnum implements IValueMapper<Enum> {

	private Class<? extends Enum> enumType = null;
	
	public void setEnumType(Class<? extends Enum> pEnumType) {
		enumType = pEnumType;
	}
	
	/**
	 * 
	 * @param pType
	 * @param pValue
	 * @return
	 */
	public String mapValue2String(Class<?> pType, Object pValue) {
		return pValue.toString();
	}
	public Enum mapString2Value(String pValueString) {
		Enum lEnumValue = null;
		if(pValueString != null) {
			lEnumValue = Enum.valueOf(enumType, pValueString);
		}
		return lEnumValue;
	}

	public String mapValue2String(Object pValue) {
		return pValue != null ? pValue.toString() : null;
	}

}
