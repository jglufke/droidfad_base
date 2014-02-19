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
 *   limitations under the License.<br>
 * -----------------------------------------------------------------------<br>
 *
 */
public class ValueMapperLong extends ValueMapperNumber implements
		IValueMapper<Long> {

	public Long mapString2Value(String pValueString) {
		return (Long) super.mapString2Value(Long.class, pValueString);
	}

	public String mapValue2String(Object pValue) {
		super.checkType(Long.class, pValue);
		return super.mapValue2String((Number) pValue);
	}

}
