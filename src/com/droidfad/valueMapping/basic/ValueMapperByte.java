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
 */
public class ValueMapperByte extends ValueMapperNumber implements
		IValueMapper<Byte> {

	public Byte mapString2Value(String pValueString) {
		return (Byte) super.mapString2Value(Byte.class, pValueString);
	}

	public String mapValue2String(Object pValue) {
		super.checkType(Byte.class, pValue);
		return super.mapValue2String((Number) pValue);
	}
}
