package com.droidfad.valueMapping;

import java.util.HashMap;
import java.util.Vector;

import com.droidfad.iframework.service.IService;
import com.droidfad.iframework.service.IServiceUser;
import com.droidfad.iframework.valuemapping.IValueMapper;
import com.droidfad.iframework.valuemapping.IValueMapperService;
import com.droidfad.util.LogWrapper;
import com.droidfad.util.ReflectionUtil;
import com.droidfad.valueMapping.basic.ValueMapperEnum;

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
 * implementation of the IValueMapperService
 */
public class ValueMapperService implements IValueMapperService, IServiceUser {

	private static final String LOGTAG = ValueMapperService.class.getSimpleName();
	/**
	 * key    - class to be mapped to string and vice versa
	 * value  - instance of value mapper which is doing the actual mapping
	 */
	private HashMap<Class<?>, IValueMapper<?>> valueMapperMap = new HashMap<Class<?>, IValueMapper<?>>();

	/**
	 * 
	 *
	 *
	 */
	public ValueMapperService() {}

	/*
	 * (non-Javadoc)
	 * @see org.yaffa.service.IServiceUser#registerService(org.yaffa.service.IService)
	 */
	@Override
	public synchronized void registerService(IService pService) {

		if(pService instanceof IValueMapper<?>) {
			IValueMapper<?>  lValueMapper = (IValueMapper<?>) pService;
			Class<?>         lClazz       = lValueMapper.getClass();
			Vector<Class<?>> lGenTypeList = new ReflectionUtil().getGenericType(lClazz, IValueMapper.class);
			if(!lGenTypeList.isEmpty()) {
				Class<?>        lMappedType     = lGenTypeList.firstElement();
				IValueMapper<?> lExistingMapper = putToValueMapperMap(lMappedType, lValueMapper);
				if(lExistingMapper != null) {
					LogWrapper.w(getClass().getSimpleName(), "mapper already registered for type:" + lMappedType + " valueMapper:" + lValueMapper);
				}
			}
		}
	}
	/**
	 * 
	 *
	 * @param pMappedType
	 * @param pValueMapper
	 * @return
	 *
	 */
	private IValueMapper<?> putToValueMapperMap(Class<?> pMappedType, IValueMapper<?> pValueMapper) {
		IValueMapper<?> lOldValueMapper = valueMapperMap.put(pMappedType, pValueMapper);
		/**
		 * take care of the primitives
		 */
		if(Short.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(short.class, pValueMapper);
		} else if(Integer.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(int.class, pValueMapper);
		} else if(Long.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(long.class, pValueMapper);
		} else if(Float.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(float.class, pValueMapper);
		} else if(Double.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(double.class, pValueMapper);
		} else if(Boolean.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(boolean.class, pValueMapper);
		} else if(Byte.class.equals(pMappedType)) {
			lOldValueMapper = valueMapperMap.put(byte.class, pValueMapper);
		} 
		return lOldValueMapper;
	}

	/*
	 * (non-Javadoc)
	 * @see com.droidfad.iframework.valuemapping.IValueMapperService#mapString2Value(java.lang.Class, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T mapString2Value(Class<?> pType, String pValueString) {

		if(pType == null) {
			throw new IllegalArgumentException("parameter pType must not be null");
		}

		IValueMapper<T> lValueMapper = (IValueMapper<T>) valueMapperMap.get(pType);
		if(lValueMapper == null && Enum.class.isAssignableFrom(pType)) {
			lValueMapper = (IValueMapper<T>) valueMapperMap.get(Enum.class);
			if(lValueMapper instanceof ValueMapperEnum) {
				((ValueMapperEnum) lValueMapper).setEnumType((Class<? extends Enum>) pType);
			}
		}
		if(lValueMapper != null) {
			return lValueMapper.mapString2Value(pValueString);
		}

		throw new NoValueMapperDefinedException("no IValueMapper instance registered for type:" + pType);
	}
	/*
	 * (non-Javadoc)
	 * @see com.droidfad.iframework.valuemapping.IValueMapperService#mapValue2String(java.lang.Class, java.lang.Object)
	 */
	@Override
	public String mapValue2String(Class<?> pType, Object pValue) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pType must not be null");
		}

		IValueMapper<?> lValueMapper = valueMapperMap.get(pType);
		if(lValueMapper == null && Enum.class.isAssignableFrom(pType)) {
			lValueMapper = valueMapperMap.get(Enum.class);
			if(lValueMapper instanceof ValueMapperEnum) {
				((ValueMapperEnum) lValueMapper).setEnumType((Class<? extends Enum>) pType);
			}
		}
		if(lValueMapper != null) {
			return lValueMapper.mapValue2String(pValue);
		} else {
			LogWrapper.w(getClass().getSimpleName(), "no IValueMapper instance registered for type:" + pType);
		}
		return null;
	}
}
