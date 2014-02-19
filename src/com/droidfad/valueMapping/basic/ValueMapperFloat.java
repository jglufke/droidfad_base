package com.droidfad.valueMapping.basic;

import com.droidfad.iframework.valuemapping.IValueMapper;

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
 * -----------------------------------------------------------------------<br>
 *
 */
public class ValueMapperFloat extends ValueMapperNumber implements
		IValueMapper<Float> {

	public Float mapString2Value(String pValueString) {
		return (Float) super.mapString2Value(Float.class, pValueString);
	}

	public String mapValue2String(Object pValue) {
		super.checkType(Float.class, pValue);
		return super.mapValue2String((Number) pValue);
	}

}
