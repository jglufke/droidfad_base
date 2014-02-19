/**
 * 
 */
package com.droidfad.data;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import com.droidfad.util.ReflectionUtil;

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
 * base class of all data acces objects in the application
 * see  http://www.droidfad.com/html/model/model.htm for a detailled description
 * 
 */
public abstract class ADao {

	private          String name = null;
	/**
	 * indicate if this is valid or not. As soon as  the object is unregistered
	 * it is set to not valid
	 */
	protected volatile boolean isValid;
	protected static HashMap<Class<? extends ADao>, String> typeMap = new HashMap<Class<? extends ADao>, String>();
	
	/**
	 * if name is given as null as constructor parameter, 
	 * objIndex is used to create a unique runtime name
	 * of the instance
	 */
	private   static volatile int objIndex = 0;
	public static    Pattern      VALID_NAME_PATTERN = Pattern.compile("[_]?[a-zA-Z][_a-zA-Z0-9]*");
	public interface AttributesEnum { }	
	
	protected ADao(final String pName) {
		
		String lType = getType();

		/**
		 * find a correct not used name if pName == null
		 */
		if(pName != null) {
			name = pName;
		} else {
			Class<? extends ADao> lClass = getClass();
			while(ObjectManager.getImpl().getInstance(lClass, (lType + objIndex)) != null) {
				objIndex++;
			}
			name = lType + objIndex;
		}
		ObjectManager.getImpl().registerObject(this, name);
		isValid = true;
	}
	/**
	 * get the type name of the ADao subclass. In the context of this framework
	 * this means getClass().getSimpleName()
	 * @return
	 *
	 */
	public final String getType() {
		synchronized (typeMap) {
			Class<? extends ADao> lClazz = getClass();
			String lType = typeMap.get(lClazz);
			if(lType == null) {
				lType = lClazz.getSimpleName();
				typeMap.put(lClazz, lType);
			}
			return lType;
		}
	}
	/**
	 * all ADao instances have to have name that is unique per type.
	 * Return the name
	 *
	 * @return
	 *
	 */
	public final String getName() {
		return name;
	}
	
	/**
	 * 
	 *
	 * @param <T>
	 * @param pAttribute
	 * @return
	 *
	 */
	protected final <T> T getAttribute(AttributesEnum pAttribute) {
		T lValue = ObjectManager.getImpl().getValue(getClass(), name, pAttribute);
		return lValue;
	}
	/**
	 * 
	 * ********************************************<br>
	 *
	 * @param pAttribute
	 * @param pValue
	 *
	 * ********************************************<br>
	 */
	protected final void setAttribute(AttributesEnum pAttribute, Object pValue) {
		ObjectManager.getImpl().setValue(getClass(), name, pAttribute, pValue);
	}
	/**
	 * get all instances of ADao subclass T
	 *
	 * @param <T>
	 * @param pType
	 * @return
	 *
	 */
	public synchronized static <T extends ADao> Vector<T> getInstancesImpl(Class<? extends ADao> pType) {
		if(pType==null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		return ObjectManager.getImpl().getInstances(pType);
	}
	
	/**
	 * get all instances of ADao subclass T sorted according to value of attribute
	 * pAttribute. Take care that pAttribute implements Comparable
	 *
	 * @param <T>
	 * @param pType
	 * @param pAttribute
	 * @return
	 *
	 */
	public static <T extends ADao> Vector<T> getSortedInstancesImpl(Class<? extends ADao> pType, AttributesEnum pAttribute) {
		if(pType==null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		if(pAttribute == null) {
			throw new IllegalArgumentException("parameter pAttribute must not be null");
		}
		Vector<T>                  lInstanceList     = ObjectManager.getImpl().getInstances(pType);

		ReflectionUtil             lReflectionUtil   = new ReflectionUtil();
		Class<?>                   lAttributeType    = null; 
		String                     lAttributeName    = pAttribute.toString();
		TreeMap<Object, Vector<T>> lSortedMap        = new TreeMap<Object, Vector<T>>();
		for(T lInstance : lInstanceList) {
			if(lAttributeType == null) {
				lAttributeType = lReflectionUtil.getAttributeType(lInstance.getClass(), lAttributeName, null);
				if(lAttributeType == null || !Comparable.class.isAssignableFrom(lAttributeType)) {
					/**
					 * attribute does not exist or the attribute type is not comparable
					 */
					break;
				}
			}
			Object    lAttributeValue = lReflectionUtil.invokeGetter(lInstance, lAttributeName, null);
			Vector<T> lList           = lSortedMap.get(lAttributeValue);
			if(lList == null) {
				lList = new Vector<T>();
				lSortedMap.put(lAttributeValue, lList);
			}
			lList.add(lInstance);
		}
		if(!lSortedMap.isEmpty()) {
			/**
			 * aggregate the lSortedMap to one common list
			 */
			lInstanceList = new Vector<T>(lSortedMap.size()*5, 10);
			for(Entry<Object, Vector<T>> lEntry : lSortedMap.entrySet()) {
				lInstanceList.addAll(lEntry.getValue());
			}
		}
		
		return lInstanceList;
	}
	
	/**
	 * get an instance of ADao subclass T with name pName
	 *
	 * @param <T>
	 * @param pClass
	 * @param pName
	 * @return
	 *
	 */
	public static <T extends ADao> T getInstanceImpl(Class<? extends ADao> pClass, String pName) {
		T lInstance = null;
		if(pName != null) {
			lInstance = ObjectManager.getImpl().getInstance(pClass, pName);
		}
		return lInstance;
	}
	/**
	 * 
	 * checks first if an instance with pName of pType exists and if not
	 * it creates it
	 *
	 * @param <T>
	 * @param pType
	 * @param pName
	 * @return
	 *
	 */
	public synchronized static <T extends ADao> T getOrCreateImpl(Class<? extends ADao> pType, String pName) {
		T lInstance = ADao.<T>getInstanceImpl(pType, pName);
		if(lInstance == null) {
			lInstance = (T) ObjectManager.getImpl().createInstance(pType, pName);
		}
		return lInstance;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getType() + "." + name;
	}
	
	/**
	 * indicate if this instance is still valid or if it is not instantiated
	 * yet or if it has already been unregistered
	 *
	 * @return
	 *
	 */
	public boolean isValid() {
		return isValid;
	}
}
