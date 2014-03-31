package com.droidfad.valueMapping.basic;

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
public abstract class ValueMapperNumber  {

	/**
	 * 
	 * @param pType
	 * @param pValueString
	 * @return
	 */
	protected Number mapString2Value(Class<? extends Number> pType, String pValueString) {
		Number lValue = null;

		if(pValueString == null || "null".equals(pValueString.trim())) {
			pValueString = "0";
		}
		pValueString = pValueString.trim();

		try {
			if(byte.class.equals(pType)
					|| Byte.class.equals(pType)) {
				lValue = new Byte(pValueString);
			} else if(short.class.equals(pType)
					|| Short.class.equals(pType)) {
				lValue = new Short(pValueString);
			} else if(int.class.equals(pType)
					|| Integer.class.equals(pType)) {
				lValue = new Integer(Integer.parseInt(pValueString));
			} else if(long.class.equals(pType)
					|| Long.class.equals(pType)) {
				lValue = new Long(pValueString);
			} else if(float.class.equals(pType)
					|| Float.class.equals(pType)) {
				lValue = new Float(pValueString);
			} else if(double.class.equals(pType)
					|| Double.class.equals(pType)) {
				lValue = new Double(pValueString);
			} 
		} catch(NumberFormatException e) {

		}
		if(lValue == null) {
			LogWrapper.e(getClass().getSimpleName(),"could not map string:'" + pValueString + "' to number of type:" + pType);
		}

		return lValue;
	}

	/**
	 * 
	 * @param pValue
	 * @return
	 */
	protected String mapValue2String(Number pValue) {
		return "" + pValue;
	}

	protected void checkType(Class<? extends Number> pType, Object pValue) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pType must not be null");
		}
		if(pValue != null && !pType.isAssignableFrom(pValue.getClass())) {
			throw new IllegalArgumentException("parameter pValue is not a sub class of:" + pType.getName());
		}
	}
}
