package com.droidfad.valueMapping.basic;

import com.droidfad.iframework.valuemapping.IValueMapper;
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
 * -----------------------------------------------------------------------<br>
 *
 */
public class ValueMapperClass implements IValueMapper<Class<?>> {

	private static final String LOGTAG = ValueMapperClass.class.getSimpleName();

	public Class<?> mapString2Value(String pValueString) {
		
		Class<?> lClazz = null;
		if(pValueString != null && !"".equals(pValueString.trim())) {
			try {
				Class.forName(pValueString);
			} catch (ClassNotFoundException e) {
				LogWrapper.e(LOGTAG, "could not load class:" + pValueString + " e:" + e.getMessage());
			}
		}
		return lClazz;
	}

	public String mapValue2String(Object pValue) {
		String lValueString = null;
		if(pValue != null) {
			lValueString = ((Class<?>) pValue).getSimpleName();
		}
		return lValueString;
	}
}
