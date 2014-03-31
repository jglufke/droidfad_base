package com.droidfad.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import com.droidfad.data.Configurable;
import com.droidfad.data.EditableByUI;
import com.droidfad.data.Persistent;

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
 * class that implements a number of often used reflection calls
 */
public class ReflectionUtil {

	private static final String LOGTAG = ReflectionUtil.class.getSimpleName();

	/**
	 * definition of the caches ----------------------------------------------
	 */

	/**
	 * key    - class name
	 * key    - attribute name
	 * value  - Hashset of annotations
	 */
	private HashMap<Class<?>, HashMap<String, HashSet<Class<? extends Annotation>>>> annotationMap = 
		new HashMap<Class<?>, HashMap<String, HashSet<Class<? extends Annotation>>>>();

	/**
	 * key     - class
	 * key     - attributename
	 * value   - getter method 
	 */
	private static HashMap<Class<?>, HashMap<String, Method>> getterCache = 
		new HashMap<Class<?>, HashMap<String,Method>>();

	/**
	 * key     - class
	 * key     - attributename
	 * value   - setter method 
	 */
	private static HashMap<Class<?>, HashMap<String, Method>> setterCache = 
		new HashMap<Class<?>, HashMap<String,Method>>();

	/**
	 * key    - class 
	 * value  - list of getters
	 */
	private static HashMap<Class<?>, Vector<Method>> allGettersCache = new HashMap<Class<?>, Vector<Method>>();
	/**
	 * key    - class 
	 * value  - list of setters
	 */
	private static HashMap<Class<?>, Vector<Method>> allSettersCache = new HashMap<Class<?>, Vector<Method>>();

	/**
	 * key    - class
	 * key    - category, e.g. EditableByUI.class, Persistent.class
	 * 
	 */
	private static HashMap<Class<?>, HashMap<Class<?>, List<String>>> allCfgAttributesCache = new HashMap<Class<?>, HashMap<Class<?>,List<String>>>();

	/**
	 * key    - class
	 * 
	 */
	private static HashMap<Class<?>, Vector<String>> allGetAttributesCache = new HashMap<Class<?>, Vector<String>>();

	private static final boolean IS_DEBUG = false;
	/**
	 * 
	 *
	 * returns the list of attributes of pClazz for which a getter and a setter exists
	 * @param pClazz
	 * @param pPrefix
	 * @return the list of configurable attribute names without the prefix
	 *
	 */
	public synchronized Vector<String> getGetableAttributeNames(Class<?> pClazz, String pPrefix) {
		if(pClazz == null) {
			throw new IllegalArgumentException("parameter pClazz must not be null");
		}

		Vector<String> lAttributeNameList = allGetAttributesCache.get(pClazz);
		if(lAttributeNameList == null) {
			getEditableByUIAttributeNames(pClazz, pPrefix);
			lAttributeNameList = allGetAttributesCache.get(pClazz);
		}
		return lAttributeNameList;
	}
	/**
	 * 
	 * returns the list of attributes of pClazz for which a getter and a setter exists
	 * @param pClazz
	 * @param pPrefix
	 * @return the list of configurable attribute names without the prefix
	 *
	 */
	public synchronized List<String> getEditableByUIAttributeNames(Class<?> pClazz, String pPrefix) {
		return getConfigurableNames(pClazz, EditableByUI.class, pPrefix);
	}
	/**
	 * 
	 * get a list of all attribute of pClazz with prefix pPrefix that annotated
	 * with annotation {@link Persistent}
	 * @param pClazz
	 * @param pPrefix
	 * @return the list of attribute names without pPrefix
	 *
	 */
	public synchronized List<String> getPersistentAttributeNames(Class<?> pClazz, String pPrefix) {
		return getConfigurableNames(pClazz, Persistent.class, pPrefix);
	}
	/**
	 * 
	 * get a list of all attribute of pClazz with prefix pPrefix that annotated
	 * with annotation {@link Configurable}
	 * @param pClazz
	 * @param pPrefix
	 * @return the list of attribute names without pPrefix
	 *
	 */
	public synchronized List<String> getConfigurableAttributeNames(Class<?> pClazz, String pPrefix) {
		return getConfigurableNames(pClazz, Configurable.class, pPrefix);
	}

	private List<String> getConfigurableNames(Class<?> pClazz, Class<? extends Annotation> pCategory, String pPrefix) {
		if(pClazz == null) {
			throw new IllegalArgumentException("parameter pClazz must not be null");
		}
		if(pCategory == null) {
			throw new IllegalArgumentException("parameter pCategory must not be null");
		}
		HashMap<Class<?>, List<String>> lCategoryMap = allCfgAttributesCache.get(pCategory);
		if(lCategoryMap == null) {
			lCategoryMap = new HashMap<Class<?>, List<String>>();
			allCfgAttributesCache.put(pCategory, lCategoryMap);
		}
		List<String> lAttributeNameList = lCategoryMap.get(pClazz);
		if(lAttributeNameList == null) {
			/**
			 * key   - sorting tag of EditableByUI annotation
			 * value - list of attribute names with the same sorting tag
			 */
			TreeMap<String, Vector<String>> lAttributeNameMap = new TreeMap<String, Vector<String>>();
			Vector<String> lGetAttributeNameList = new Vector<String>();
			allGetAttributesCache.put(pClazz, lGetAttributeNameList);

			Vector<Method> lGetterList = getGetterList(pClazz, pPrefix);
			for(Method lGetter : lGetterList) {
				getConfigurableAttributeNames(pClazz, pCategory, pPrefix, lAttributeNameMap, lGetAttributeNameList, lGetter);
			}
			/**
			 * create a sorted list of attribute names out of the treemap
			 */
			lAttributeNameList = new Vector<String>();
			for(Entry<String, Vector<String>> lEntry : lAttributeNameMap.entrySet()) {
				for(String lAttributeName : lEntry.getValue()) {
					lAttributeNameList.add(lAttributeName);
				}
			}
			lCategoryMap.put(pClazz, lAttributeNameList);
		}
		return new Vector<String>(lAttributeNameList);
	}

	/**
	 * 
	 * adds to pAttributeNameMap the attributes
	 * of pClazz that are annotated with pCategroy.
	 * The annotation has to be added to the getter
	 * or setter of the attribute.
	 * if pCategory Configurable.class the annotation 
	 * does not matter and attributes are added that
	 * follow the beans convention concerning getter
	 * and setter method
	 * 
	 * @param pClazz
	 * @param pCategory
	 * @param pPrefix
	 * @param pAttributeNameMap
	 * @param pGetAttributeNameList
	 * @param pGetter
	 *
	 */
	private void getConfigurableAttributeNames(Class<?> pClazz, Class<? extends Annotation> pCategory, 
			String pPrefix,
			TreeMap<String, Vector<String>> pAttributeNameMap,
			Vector<String> pGetAttributeNameList, Method pGetter) {

		String lAttributeName = pGetter.getName();
		if(lAttributeName.startsWith("get")) {
			lAttributeName = lAttributeName.substring(3);
		} else {
			lAttributeName = lAttributeName.substring(2);
		}
		if(pPrefix != null) {
			lAttributeName = lAttributeName.substring(pPrefix.length());
		}
		pGetAttributeNameList.add(lAttributeName);

		Annotation   lAnnotation = null;
		boolean      lIsRelevant = (pCategory == Configurable.class);
		Method       lSetter     = getSetter(pClazz, lAttributeName, pPrefix);
		if(!lIsRelevant) {
			lAnnotation = pGetter.getAnnotation(pCategory);
			if(lSetter != null && lAnnotation == null) {
				lAnnotation = lSetter.getAnnotation(pCategory);
			}
			lIsRelevant = (lAnnotation != null);
		}
		if(IS_DEBUG) LogWrapper.e("ReflectionUtil", "setter:" + lSetter + " an:" + lAnnotation);
		if(lIsRelevant && lSetter != null) {

			Class<?> lGetterReturnType = pGetter.getReturnType();
			Class<?> lSetterParType    = lSetter.getParameterTypes()[0];
			if(lGetterReturnType.equals(lSetterParType)) {
				String         lSortingTag = getSortingTag(pCategory, lAnnotation);
				if(lSortingTag == null) {
					lSortingTag = "";
				}
				Vector<String> lNameList = pAttributeNameMap.get(lSortingTag);
				if(lNameList == null) {
					lNameList = new Vector<String>();
					pAttributeNameMap.put(lSortingTag, lNameList);
				}
				lNameList.add(lAttributeName);
			}
		}
	}
	/**
	 * 
	 *
	 * @param pClass
	 * @param pAnnotation
	 * @return
	 *
	 */
	private String getSortingTag(Class<? extends Annotation> pClass, Annotation pAnnotation) {
		String lTag    = null;
		try {
			for(Method lMethod : pClass.getMethods()) {
				if("sortingTag".equals(lMethod.getName())) {
					lTag = (String) lMethod.invoke(pAnnotation);
				}
			}
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (ClassCastException e) {
		}
		return lTag;
	}
	/**
	 * 
	 * return the getter for attribute pAttributeName, after the get
	 * prefix pPrefix is appended to determine the method name. If not 
	 * method is found an IllegalArgumentException is thrown
	 * 
	 * @param pClazz
	 * @param pAttributeName
	 * @param pPrefix
	 * @return
	 *
	 */
	public synchronized Method getGetter(Class<?> pClazz, String pAttributeName, String pPrefix) {
		if(pClazz == null) {
			throw new IllegalArgumentException("parameter pClazz must not be null");
		}
		if(pAttributeName == null) {
			throw new IllegalArgumentException("parameter pAttributeName must not be null");
		}
		HashMap<String,Method> lGetterMap = getterCache.get(pClazz);
		if(lGetterMap == null) {
			lGetterMap = new HashMap<String, Method>();
			getterCache.put(pClazz, lGetterMap);
		}
		Method lGetter = lGetterMap.get(pAttributeName);
		if(lGetter == null) {
			String lPostFix    = pPrefix != null ? pPrefix + pAttributeName : pAttributeName; 
			String lMethodName = "get" + lPostFix;
			try {
				lGetter = pClazz.getMethod(lMethodName, (Class[]) null);
				lGetterMap.put(pAttributeName, lGetter);
			} catch (SecurityException e) {
				throw new RuntimeException("could not get getter:" + lMethodName + " for:" + pClazz, e);
			} catch (NoSuchMethodException e) {
				/**
				 * check if a boolean getter exists
				 */
			}
			if(lGetter == null) {
				lMethodName = "is" + lPostFix;
				try {
					Method[] lMethods = pClazz.getMethods();
					lGetter = pClazz.getMethod(lMethodName, (Class[]) null);
					lGetterMap.put(pAttributeName, lGetter);
				} catch (SecurityException e) {
					throw new RuntimeException("could not get getter:" + lMethodName + " for:" + pClazz, e);
				} catch (NoSuchMethodException e) {
					/**
					 * it is ok, return null in this case. Calling method has 
					 * to handle this
					 */
				}
			}
		}
		return lGetter;
	}

	/**
	 * 
	 * @param pClazz
	 * @param pAttributeName
	 * @param pPrefix
	 * @return
	 */
	public synchronized Method getSetter(Class<?> pClazz, String pAttributeName, String pPrefix) {
		if(pClazz == null) {
			throw new IllegalArgumentException("parameter pClazz must not be null");
		}
		if(pAttributeName == null || "".equals(pAttributeName)) {
			throw new IllegalArgumentException("paramter pAttributeName must not be null or emtpy");
		}

		HashMap<String,Method> lSetterMap = setterCache.get(pClazz);
		if(lSetterMap == null) {
			lSetterMap = new HashMap<String, Method>();
			setterCache.put(pClazz, lSetterMap);
		}
		Method lSetter = lSetterMap.get(pAttributeName);
		if(lSetter == null) {
			String lPostFix    = pAttributeName;
			if(pPrefix != null) {
				lPostFix = pPrefix + pAttributeName; 
			}
			String lMethodName = "set" + lPostFix;
			Method lGetter     = getGetter(pClazz, pAttributeName, pPrefix);
			if(lGetter != null) {
				Class<?> lAttributeType = lGetter.getReturnType();
				if(lAttributeType != null) {
					try {
						lSetter = pClazz.getMethod(lMethodName, new Class<?>[] { lAttributeType });
						lSetterMap.put(pAttributeName, lSetter);
					} catch (SecurityException e) {
						throw new RuntimeException("could not get getter:" + lMethodName + " for:" + pClazz, e);
					} catch (NoSuchMethodException e) {
					}
				}
			}
		}
		return lSetter;
	}

	/**
	 * retrieve all the getters that are implemented by pClazz
	 * @param pClazz
	 * @param pPrefix
	 * @return
	 */
	public synchronized Vector<Method> getGetterList(Class<?> pClazz, String pPrefix) {
		if(pClazz == null) {
			throw new IllegalArgumentException("parameter pClazz must not be null");
		}
		Vector<Method> lGetterList = allGettersCache.get(pClazz);
		if(lGetterList == null) {
			lGetterList = new Vector<Method>();
			allGettersCache.put(pClazz, lGetterList);
			String lGetPrefix = pPrefix != null ? "get" + pPrefix : "get";
			String lIsPrefix  = pPrefix != null ? "is"  + pPrefix : "is";

			for(Method lMethod : pClazz.getMethods()) {
				String lMethodName = lMethod.getName();
				if(lMethodName.startsWith(lGetPrefix) || lMethodName.startsWith(lIsPrefix)) {
					/**
					 * check that it is no void method
					 */
					Class<?> lReturnType = lMethod.getReturnType();
					if(lReturnType != null && !void.class.equals(lReturnType)) {
						/**
						 * only methods with no parameters
						 */
						Class<?>[] lParameterTypes = lMethod.getParameterTypes();
						if(lParameterTypes != null && lParameterTypes.length == 0) {
							lGetterList.add(lMethod);
						}
					}
				}
			}
		}
		lGetterList = new Vector<Method>(lGetterList);
		return lGetterList;
	}

	/**
	 * retrieve all the getters that are implemented by pClazz
	 * @param pClazz
	 * @param pPrefix
	 * @return
	 */
	public synchronized Vector<Method> getSetterList(Class<?> pClazz, String pPrefix) {
		if(pClazz == null) {
			throw new IllegalArgumentException("parameter pClazz must not be null");
		}
		Vector<Method> lSetterList = allSettersCache.get(pClazz);
		if(lSetterList == null) {
			lSetterList = new Vector<Method>();
			allSettersCache.put(pClazz, lSetterList);
			for(Method lMethod : pClazz.getMethods()) {
				String lMethodName = lMethod.getName();
				if(lMethodName.startsWith("set")) {
					/**
					 * check that it is no void method
					 */
					Class<?> lReturnType = lMethod.getReturnType();
					if(lReturnType == null || void.class.equals(lReturnType)) {
						/**
						 * only methods with a single parameter
						 */
						Class<?>[] lParameterTypes = lMethod.getParameterTypes();
						if(lParameterTypes != null && lParameterTypes.length == 1) {
							lSetterList.add(lMethod);
						}
					}
				}
			}
		}
		lSetterList = new Vector<Method>(lSetterList);
		return lSetterList;
	}

	/**
	 * returns the generic type for which a class has defined an interface. If e.g.
	 * pGenericClass aTestClass implements aTestInterface<Integer, String> the method will
	 * return { Integer, String } if called with the parameters aTestClass, aTestInterface.class
	 * @param pGenericClazz class that has generic interfaces
	 * @param pGenericInterface
	 * @return
	 */
	public synchronized Vector<Class<?>> getGenericType(Class<?> pGenericClazz, Class<?> pGenericInterface) {
		if(pGenericClazz == null) {
			throw new IllegalArgumentException("parameter pGenericClazz must not be null");
		}
		if(pGenericInterface == null) {
			throw new IllegalArgumentException("parameter pGenericInterface must not be null");
		}

		Vector<Class<?>> lResult     = new Vector<Class<?>>();
		Type[]           lGenIfArray = pGenericClazz.getGenericInterfaces();
		if(lGenIfArray != null) {
			for(Type lType : lGenIfArray) {
				if(lType instanceof ParameterizedType) {
					ParameterizedType lGenericInterface = (ParameterizedType) lType;
					Type              lRawType          = lGenericInterface.getRawType();
					if(lRawType instanceof Class) {
						Class<?> lRawClazz = (Class<?>) lRawType;
						if(pGenericInterface.isAssignableFrom(lRawClazz)) {
							for(Type lGenericType : lGenericInterface.getActualTypeArguments()) {
								if(lGenericType instanceof Class) {
									lResult.add((Class<?>) lGenericType);
								}
							}
						}
					}
				}
			}
		}
		return lResult;
	}
	/**
	 * 
	 *
	 * @param pGenericClazz
	 * @return
	 *
	 */
	public synchronized Vector<Class<?>> getGenericType(Class<?> pGenericClazz) {
		if(pGenericClazz == null) {
			throw new IllegalArgumentException("parameter pGenericClazz must not be null");
		}

		Vector<Class<?>>  lResult     = new Vector<Class<?>>();
		TypeVariable<?>[] lGenIfArray = pGenericClazz.getTypeParameters();
		TypeVariable<?>   lVariable   = lGenIfArray[0];
		if(lGenIfArray != null) {
			for(Type lType : lGenIfArray) {
				if(lType instanceof ParameterizedType) {
					ParameterizedType lGenericInterface = (ParameterizedType) lType;
					Type              lRawType          = lGenericInterface.getRawType();
					if(lRawType instanceof Class) {
						Class<?> lRawClazz = (Class<?>) lRawType;
						if(false) {
							for(Type lGenericType : lGenericInterface.getActualTypeArguments()) {
								if(lGenericType instanceof Class) {
									lResult.add((Class<?>) lGenericType);
								}
							}
						}
					}
				}
			}
		}
		return lResult;
	}
	/**
	 * 
	 * @param pClass
	 * @param pAttributeName
	 * @param pPrefix
	 * @param pValue
	 * @return true if the invoke was successful
	 */
	public synchronized boolean invokeSetter(Object pObject,	String pAttributeName, String pPrefix, Object pValue) {

		if(pObject == null) {
			throw new IllegalArgumentException("parameter pObject must not be null");
		}
		if(pAttributeName == null) {
			throw new IllegalArgumentException("parameter pAttributeName must not be null");
		}
		boolean lSuccess = false;
		Method lSetter = getSetter(pObject.getClass(), pAttributeName, pPrefix);
		if(lSetter != null) {
			try {
				lSetter.invoke(pObject, pValue);
				lSuccess = true;
			} catch (IllegalArgumentException e) {
				LogWrapper.e(LOGTAG, e.getMessage());
			} catch (IllegalAccessException e) {
				LogWrapper.e(LOGTAG, e.getMessage());
			} catch (InvocationTargetException e) {
				LogWrapper.e(LOGTAG, e.getMessage());
			}
		} else {
//			LogWrapper.w(getClass().getSimpleName(), "setter for attribute:" + pAttributeName + " does not exist" +
//					" for class:" + pObject.getClass());
		}
		return  lSuccess;
	}

	/**
	 * 
	 * @param pClass
	 * @param pAttributeName
	 * @param pPrefix
	 * @return true if the invoke was successful
	 */
	public synchronized Object invokeGetter(Object pObject,	String pAttributeName, String pPrefix) {
		if(pObject == null) {
			throw new IllegalArgumentException("parameter pObject must not be null");
		}
		if(pAttributeName == null) {
			throw new IllegalArgumentException("parameter pAttributeName must not be null");
		}
		Object lValue     = null;
		Method    lGetter = getGetter(pObject.getClass(), pAttributeName, pPrefix);
		Exception lE      = null;
		if(lGetter != null) {
			try {
				// LogWrapper.e(LOGTAG, "iGet:" + pObject + " g:" + lGetter);
				lValue = lGetter.invoke(pObject, new Object[0]);
			} catch (IllegalArgumentException e) {
				lE = e;
			} catch (IllegalAccessException e) {
				lE = e;
			} catch (InvocationTargetException e) {
				lE = e;
			} catch (Exception e) {
				lE = e;
			} finally {
				if(lE != null) {
					throw new RuntimeException("could not invoke:" + lGetter + " e:" + lE + " msg:" + lE.getMessage());
				}
			}
		} else {
			LogWrapper.w(getClass().getSimpleName(), "getter for attribute:" + pAttributeName + " does not exist" +
					" for class:" + pObject.getClass());
		}
		return  lValue;
	}
	/**
	 * 
	 *
	 * @param <T>
	 * @param pClass
	 * @param pParameterTypes
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> Constructor<T> getConstructor(Class<?> pClass, Class<?>[] pParameterTypes) {
		if(pClass == null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		Constructor<T> lConstructor = null;
		try {
			lConstructor = (Constructor<T>) pClass.getConstructor(pParameterTypes);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		return lConstructor;
	}
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
	 *   limitations under th-----------------------------------------------------------------------<br>----<br>----<br>----<br>----<br>
	 * @param <T>
	 *
	 */
	public static class Mapper<T> {
		public Class<? extends T> getClass(String pClassName) {
			Class<? extends T> lClass = null;
			try {
				lClass = (Class<? extends T>) Class.forName(pClassName);
			} catch (ClassNotFoundException e) {
			}
			return lClass;
		}
	}
	/**
	 * 
	 *
	 * @param pObjectType
	 * @param pAttributeName
	 * @param pPrefix
	 * @return
	 *
	 */
	public synchronized Class<?> getAttributeType(Class<?> pObjectType, String pAttributeName, String pPrefix) {
		if(pObjectType == null) {
			throw new IllegalArgumentException("parameter pObjectType must not be null");
		}
		if(pAttributeName == null) {
			throw new IllegalArgumentException("parameter pAttributeName must not be null");
		}
		Class<?> lAttributeType = null;
		Method   lGetter        = getGetter(pObjectType, pAttributeName, pPrefix);
		if(lGetter != null) {
			lAttributeType = lGetter.getReturnType();
		}
		return lAttributeType;
	}
	/**
	 * 
	 *
	 * @param pClass
	 * @param pAttributeName
	 * @param pAnnotation
	 * @return
	 *
	 */
	public synchronized boolean hasAnnotation(Class<?> pClass, String pAttributeName, Class<? extends Annotation> pAnnotation) {
		if(pClass == null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		if(pAttributeName == null) {
			throw new IllegalArgumentException("parameter pAttributeName must not be null");
		}
		if(pAnnotation == null) {
			throw new IllegalArgumentException("parameter pAnnotation must not be null");
		}

		boolean lHasAnnotation = false;

		HashMap<String, HashSet<Class<? extends Annotation>>> lAttributeMap = annotationMap.get(pClass);
		if(lAttributeMap == null) {
			lAttributeMap = createAttributeAnnotationMap(pClass);
			annotationMap.put(pClass, lAttributeMap);
		}
		HashSet<Class<? extends Annotation>> lAnnotationSet = lAttributeMap.get(pAttributeName);
		if(lAnnotationSet != null && !lAnnotationSet.isEmpty()) {
			lHasAnnotation = lAnnotationSet.contains(pAnnotation);
		}
		return lHasAnnotation;
	}
	/**
	 * 
	 *
	 * @param pClass
	 * @param pAnnotation
	 * @return
	 *
	 */
	public synchronized boolean hasAnnotation(Class<?> pClass, Class<? extends Annotation> pAnnotation) {
		if(pClass == null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		if(pAnnotation == null) {
			throw new IllegalArgumentException("parameter pAnnotation must not be null");
		}
		boolean lHasAnnotation = pClass.getAnnotation(pAnnotation) != null;
		return  lHasAnnotation;
	}
	/**
	 * 
	 *
	 * @param pClass
	 * @return
	 *
	 */
	private HashMap<String, HashSet<Class<? extends Annotation>>> createAttributeAnnotationMap(final Class<?> pClass) {

		HashMap<String, HashSet<Class<? extends Annotation>>> lAttributeAnnotationMap = new HashMap<String, HashSet<Class<? extends Annotation>>>();

		for(String lAttributeName : getConfigurableAttributeNames(pClass, null)) {
			HashSet<Class<? extends Annotation>> lAnnotationSet = new HashSet<Class<? extends Annotation>>();
			Method lMethod = getGetter(pClass, lAttributeName, null);
			for(Annotation lAnnotation : lMethod.getAnnotations()) {
				lAnnotationSet.add(lAnnotation.annotationType());
			}
			lMethod        = getSetter(pClass, lAttributeName, null);
			for(Annotation lAnnotation : lMethod.getAnnotations()) {
				lAnnotationSet.add(lAnnotation.annotationType());
			}
			if(!lAnnotationSet.isEmpty()) {
				lAttributeAnnotationMap.put(lAttributeName, lAnnotationSet);
			}
		}
		return lAttributeAnnotationMap;
	}
	/**
	 *
	 * @param pAttributeTypeName
	 * @return
	 *
	 */
	public synchronized Class<?> getPrimitiveType(String pAttributeTypeName) {

		Class<?> lPrimitiveType = null;
		if("byte".equals(pAttributeTypeName)) {
			lPrimitiveType = byte.class;
		} else if("char".equals(pAttributeTypeName)) {
			lPrimitiveType = char.class;
		} else if("short".equals(pAttributeTypeName)) {
			lPrimitiveType = short.class;
		} else if("int".equals(pAttributeTypeName)) {
			lPrimitiveType = int.class;
		} else if("long".equals(pAttributeTypeName)) {
			lPrimitiveType = long.class;
		} else if("float".equals(pAttributeTypeName)) {
			lPrimitiveType = float.class;
		} else if("double".equals(pAttributeTypeName)) {
			lPrimitiveType = double.class;
		} else if("boolean".equals(pAttributeTypeName)) {
			lPrimitiveType = boolean.class;
		} 
		return lPrimitiveType;
	}
}
