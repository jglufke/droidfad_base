package com.droidfad.valueMapping;

import java.io.File;
import java.io.IOException;

import com.droidfad.iframework.valuemapping.IValueMapper;
import com.droidfad.util.LogWrapper;

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
public class ValueMapperFile implements IValueMapper<File> {

	public File mapString2Value(String pValueString) {
		if(pValueString == null || "".equals(pValueString.trim())) {
			pValueString = ".";
		}
		return new File(pValueString);
	}

	public String mapValue2String(Object pValue) {
		if(pValue != null && !(pValue instanceof File)) {
			throw new IllegalArgumentException("pValue is not an instance of File");
		}
		String lResult = null;
		if(pValue != null) {
			try {
				lResult = ((File) pValue).getCanonicalPath();
			} catch (IOException e) {
				LogWrapper.e(getClass().getSimpleName(), "can not create canonical path for:" + pValue, e);
				lResult = ((File) pValue).getAbsolutePath();
			}
		}
		if(lResult == null) {
			lResult = ".";
		}
		return lResult;
	}
}
