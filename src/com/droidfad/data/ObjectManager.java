/**
 * 
 */
package com.droidfad.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import com.droidfad.classloading.ClazzFinder;
import com.droidfad.data.ACategory.Category;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.iframework.event.IEvent;
import com.droidfad.iframework.event.IEvent.DataType;
import com.droidfad.iframework.event.IEvent.EventType;
import com.droidfad.iframework.event.IEventPublisher;
import com.droidfad.persistency.Persistency;
import com.droidfad.util.LogWrapper;
import com.droidfad.util.ReflectionUtil;

/**
 * 
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com

   Licensed under the DROIDFAD license (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.droidfad.com/html/license/license.htm

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 *
 */
class ObjectManager {

	private static final Byte    BYTE0   = 0;

	private static final Double  DOUBLE0 = 0D;
	private volatile static IEventPublisher eventBroadcaster;
	private static final Float   FLOAT0  = 0F;

	private static ObjectManager instance;

	private static final Integer INT0    = 0;
	private static final int KEY_BOOL   = 7;

	private static final int KEY_BYTE   = 0;

	private static final int KEY_DOUBLE = 5;

	private static final int KEY_FLOAT  = 4;

	private static final int KEY_INT    = 2;
	private static final int KEY_LONG   = 3;

	private static final int KEY_SHORT  = 1;

	private static final int KEY_STRING = 6;

	private static final String LOGTAG = ObjectManager.class.getSimpleName();

	private static final Long    LONG0   = 0L;

	private static final Short   SHORT0  = 0;

	private static HashMap<Class<?>, Integer> typeKeyMap = null;

	private  static final String TYPENAME     = ObjectManager.class.getSimpleName();

	static {
		typeKeyMap = new HashMap<Class<?>, Integer>();
		typeKeyMap.put(byte.class, KEY_BYTE);
		typeKeyMap.put(Byte.class, KEY_BYTE);
		typeKeyMap.put(short.class, KEY_SHORT);
		typeKeyMap.put(Short.class, KEY_SHORT);
		typeKeyMap.put(int.class, KEY_INT);
		typeKeyMap.put(Integer.class, KEY_INT);
		typeKeyMap.put(long.class, KEY_LONG);
		typeKeyMap.put(Long.class, KEY_LONG);
		typeKeyMap.put(float.class, KEY_FLOAT);
		typeKeyMap.put(Float.class, KEY_FLOAT);
		typeKeyMap.put(double.class, KEY_DOUBLE);
		typeKeyMap.put(Double.class, KEY_DOUBLE);
		typeKeyMap.put(String.class, KEY_STRING);
		typeKeyMap.put(boolean.class, KEY_BOOL);
		typeKeyMap.put(Boolean.class, KEY_BOOL);
	}
	protected static synchronized ObjectManager getImpl() {
		if(instance == null) {
			instance = new ObjectManager();
		}
		return instance;
	}

	public static void main(String[] args) {
		String lFilename = "\\data\\data\\com.glufke.android\\databases\\pers\\153\\237\\133\\6\\TestReference_2\\TestReference_2\\SourceType\\val";
		File   lFile     = new File(lFilename);
		lFile.getParentFile().mkdirs();
	}

	protected static synchronized void setImpl(ObjectManager pImpl) {
		instance = pImpl;
	}

	/**
	 * key     - type
	 * key     - name
	 * key     - attributeName
	 * value   - attribute value
	 */
	private HashMap<String, HashMap<String, HashMap<String, Object>>> cache =
		new HashMap<String, HashMap<String,HashMap<String, Object>>>();

	/**
	 * key    - type name which is the simple name of the class
	 * value  - classname
	 */
	HashMap<String, String> classNameMap = null;

	/**
	 * key   - simple class name
	 * key   - instance name
	 * value - reference to instance
	 */
	HashMap<String, HashMap<String, ADao>> objectCache = null;

	private    ReflectionUtil    reflectionUtil = new ReflectionUtil();

	private HashMap<String, Class<? extends ADao>> typeNameToDaoMap;

	protected ObjectManager(){}
	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#clear()
	 */
	protected synchronized void clear() {
		if(objectCache != null && !objectCache.isEmpty()) {
			for(Entry<String, HashMap<String, ADao>> lEntry : 
				new Vector<Entry<String, HashMap<String, ADao>>>(objectCache.entrySet())) {

				HashMap<String, ADao> lEntryMap = lEntry.getValue();
				if(lEntryMap != null && !lEntryMap.isEmpty()) {
					for(ADao lObject : new Vector<ADao>(lEntryMap.values())) {
						unregisterObject(lObject);
					}
				}
			}
		}
		Persistency.getInstance().delete();
		objectCache = null;
	}

	protected void clearCache() {
		clearCache(null, null);
	}
	
	/**
	 * 
	 * delete all entries in internal cache that have the category
	 * pCategory. If pCategory is null all entries in the cache are
	 * deleted. If pToBeClearedTypeSet is not null, only the types 
	 * that are containtedin pToBeClearedTypeSet are cleared from
	 * the cache. The conditions for pCategory and pToBeClearedTypeSet are
	 * AND connected.
	 * @param pCategory
	 * @param pToBeClearedTypeSet
	 *
	 */
	protected void clearCache(Category pCategory, HashSet<String> pToBeClearedTypeSet) {

		HashSet<String> lCategoryTypeSet = null;
		if(pCategory != null) {
			lCategoryTypeSet = new HashSet<String>();
			for(Class<?> lSubClass : ClazzFinder.findSubclasses(ADao.class)) {
				ACategory lCategoryAnnotation = lSubClass.getAnnotation(ACategory.class);
				if(lCategoryAnnotation != null) {
					if(pCategory.equals(lCategoryAnnotation.category())) {
						lCategoryTypeSet.add(lSubClass.getSimpleName());
					}
				}
			}
		}

		if(objectCache != null && !objectCache.isEmpty()) {
			for(Entry<String, HashMap<String, ADao>> lEntry : 
				new Vector<Entry<String, HashMap<String, ADao>>>(objectCache.entrySet())) {
				String                lDaoType  = lEntry.getKey();
				/**
				 * only delete the entry if pCategory was null or
				 * if lDaoType has the correct ACategory annotation
				 */
				if(lCategoryTypeSet == null || lCategoryTypeSet.contains(lDaoType)) {
					if(pToBeClearedTypeSet == null || pToBeClearedTypeSet.contains(lDaoType)) {
						HashMap<String, ADao> lEntryMap = lEntry.getValue();
						if(lEntryMap != null && !lEntryMap.isEmpty()) {
							for(ADao lObject : new Vector<ADao>(lEntryMap.values())) {
								removeObjectFromCache(lObject);
							}
						}
					}
				}
			}
		}

		if(pCategory == null && pToBeClearedTypeSet == null) {
			objectCache = null;
			cache.clear();
		}
	}
	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pName
	 * @return
	 *
	 * ********************************************<br>
	 */
	protected ADao createInstance(Class<? extends ADao> pType, String pName) {

		ADao lObject = null;
		/**
		 * get the class name of the pType
		 */
		if(pType != null && !AReferenceType.class.isAssignableFrom(pType)) {
			String lClassName = pType.getName();
			try {
				Constructor<? extends ADao> lConstructor = reflectionUtil.getConstructor(pType, new Class[]{ String.class });
				if(lConstructor != null) {
					lObject = lConstructor.newInstance(pName); 
				} else {
					LogWrapper.e(TYPENAME, "class:" + lClassName + " does not have a constructor with a single string as parameter");
				}
			} catch (IllegalArgumentException e) {
				LogWrapper.e(TYPENAME, "could not create:" + lClassName + " e:" + e.getMessage());
			} catch (InstantiationException e) {
				LogWrapper.e(TYPENAME, "could not create:" + lClassName + " e:" + e.getMessage());
			} catch (IllegalAccessException e) {
				LogWrapper.e(TYPENAME, "could not create:" + lClassName + " e:" + e.getMessage());
			} catch (InvocationTargetException e) {
				LogWrapper.e(TYPENAME, "could not create:" + lClassName + " e:" + e.getMessage(), e);
			}
		}
		return lObject;
	}

	/**
	 *
	 * @param pCategory
	 * @param pExportFile
	 * @throws IOException 
	 *
	 */
	public void exportData(Category pCategory, File pExportFile) throws IOException {
		Persistency.getInstance().exportData(pCategory, pExportFile);
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pName
	 * @param pAttributeName
	 * @param pValue
	 *
	 * ********************************************<br>
	 */
	private  void fireDataSet(final Class<? extends ADao> pType, final String pName,
			final AttributesEnum pAttribute, final Object pValue) {

		IEvent lEvent = eventBroadcaster.createEvent(
				this, 
				DataType.object, EventType.updated, pType, pName, 
				this, pAttribute, null, pValue);

		eventBroadcaster.publish(lEvent);
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pName
	 *
	 * ********************************************<br>
	 */
	private  void fireObjectCreated(final Class<? extends ADao> pType, final String pName) {
		IEvent lEvent = eventBroadcaster.createEvent(this, DataType.object, EventType.created, pType, pName, this, null, null, null);
		eventBroadcaster.publish(lEvent);
	}
	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pName
	 *
	 * ********************************************<br>
	 */
	private  void fireObjectDeleted(final Class<? extends ADao> pType, final String pName) {
		IEvent lEvent = eventBroadcaster.createEvent(this, DataType.object, EventType.deleted, pType, pName, this, null, null, null);
		eventBroadcaster.publish(lEvent);
	}
	/**
	 *
	 * @param pTypeName
	 * @return
	 *
	 */
	public synchronized Class<? extends ADao> getDaoClassFromTypeName(String pTypeName) {
		if(typeNameToDaoMap == null) {
			initObjectCache();
		}
		Class<? extends ADao> lDaoClass = typeNameToDaoMap.get(pTypeName);
		return lDaoClass;
	}
	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#getObject(java.lang.Class, java.lang.String)
	 */
	protected  synchronized  <T extends ADao> T getInstance(Class<? extends ADao> pType, String pName) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pObjectClass must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}

		T      lObject   = getObjectFromCache(pType, pName);
		if(lObject == null) {
			lObject      = getObjectFromDataBase(pType, pName);
		}
		return lObject;
	}

	/**
	 *
	 * @param pType
	 * @param pName
	 *
	 */
	private <T extends ADao> T getObjectFromDataBase(Class<? extends ADao> pType, String pName) {
		ADao lReturnObject = null;
		HashSet<String> lNameSet = Persistency.getInstance().getPersistedInstanceNameSet(pType);
		if(lNameSet.contains(pName)) {
			lReturnObject = createInstance(pType, pName);
		}
		return (T) lReturnObject;
	}

	/**
	 *
	 * @param pType
	 * @param pName
	 *
	 */
	@SuppressWarnings("unchecked")
	private <T extends ADao> Vector<T> getObjectsFromDataBase(Class<? extends ADao> pType) {
		Vector<T>  lReturnList = new Vector<T>();
		for(String lObjectName : Persistency.getInstance().getPersistedInstanceNameSet(pType)) {
			/**
			 * check if the object already exists
			 */
			T lObject   = getObjectFromCache(pType, lObjectName);
			if(lObject == null) {
				lObject = (T) createInstance(pType, lObjectName);
			}
			lReturnList.add(lObject);
		}
		return lReturnList;
	}

	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#getInstances(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	protected synchronized <T extends ADao> Vector<T> getInstances(Class<? extends ADao> pType) {

		Vector<T> lInstanceList = null;
		if(objectCache == null) {
			initObjectCache();
		}
		HashMap<String,ADao> lInstanceMap = null;
		if(pType != null) {
			lInstanceMap = objectCache.get(pType.getSimpleName());
			if(lInstanceMap != null) {
				Collection<ADao> lObjectList = lInstanceMap.values();
				lInstanceList = new Vector<T>(lObjectList.size());
				for(ADao lObject : lInstanceMap.values()) {
					lInstanceList.add((T) lObject);
				}
			} else {
				System.out.println();
			}
		} 
		if(lInstanceList == null) {
			lInstanceList = new Vector<T>(0);
		}
		return lInstanceList;
	}

	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#getObject(java.lang.String, java.lang.String)
	 */
	protected  synchronized  <T extends ADao> T getObject(Class<? extends ADao> pType, String pName) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pObjectType must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}
		T      lObject   = getObjectFromCache(pType, pName);

		return lObject;
	}
	/**
	 * ********************************************<br>
	 * @param pType 
	 * @param lTypeName
	 * @param pName
	 * @return
	 *
	 * ********************************************<br>
	 */
	@SuppressWarnings("unchecked")
	private  <T extends ADao> T getObjectFromCache(final Class<? extends ADao> pType, final String pName) {
		if(objectCache == null) {
			/**
			 * if the object cache is not yet instantiated, read all
			 * available information from database
			 */
			initObjectCache();
		}

		T    lObject       = null;
		ADao lCachedObject = null;

		String lTypeName = pType.getSimpleName();
		HashMap<String, ADao> lInstanceMap = objectCache.get(lTypeName);
		if(lInstanceMap != null) {
			lCachedObject = lInstanceMap.get(pName);
		}
		if(lCachedObject != null) {
			if(pType == null || pType.equals(lCachedObject.getClass())) {
				lObject = (T) lCachedObject;
			} else {
				throw new IllegalArgumentException("request object class: "+pType+" is different to stored:" + 
						pType.getName() + " stored:" + lCachedObject.getClass().getName());
			}
		} 
		return lObject;
	}

	/**
	 * ********************************************<br>
	 *
	 * @return
	 *
	 * ********************************************<br>
	 */
	public Vector<Class<? extends ADao>> getObjectTypeList() {
		HashSet<Class<? extends ADao>> lSubClassSet = ClazzFinder.findSubclasses(ADao.class);
		return new Vector<Class<? extends ADao>>(lSubClassSet);
	}
	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#getValue(java.lang.Class, java.lang.String, com.droidfad.data.ADao.AttributesEnum)
	 */
	protected synchronized <T> T getValue(Class<? extends ADao> pType, String pName, AttributesEnum pAttribute) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}
		if(pAttribute == null) {
			throw new IllegalArgumentException("parameter pAttribute must not be null");
		}
		ADao     lObject        = getObject(pType, pName);
		Class<?> lAttributeType = reflectionUtil.getAttributeType(pType, pAttribute.toString(), null);
		if(lAttributeType == null) {
			throw new IllegalArgumentException("not getter implemented for attribute:" + pAttribute);
		}
		T        lValue         = null;
		if(lObject != null) {

			String lAttributeName = pAttribute.toString();
			lValue                = getValueFromCache(cache, pType, pName, lAttributeName);
			if(lValue == null && isPersistent(pType, lAttributeName)) {
				lValue = getValueFromDataBase(pType, pName, lAttributeType, lAttributeName);
				setValueToCache(cache, pType, pName, lAttributeName, lValue);
			} 
		} else {
			LogWrapper.w(LOGTAG, "getValue:" + pType + "." + pName + " object is not registered");
		}
		lValue = handleNullValue(lAttributeType, lValue);

		return lValue;
	}
	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pName
	 * @param pAttributeName
	 * @return
	 *
	 * ********************************************<br>
	 */
	@SuppressWarnings("unchecked")
	private <T> T getValueFromCache(final HashMap<String, HashMap<String, HashMap<String, Object>>> pCache,
			final Class<? extends ADao> pType, final String pName, final String pAttributeName) {
		T lValue = null;
		HashMap<String,HashMap<String,Object>> lInstanceMap = pCache.get(pType.getSimpleName());
		if(lInstanceMap != null) {
			HashMap<String, Object> lAttributeMap = lInstanceMap.get(pName);
			if(lAttributeMap != null) {
				lValue = (T) lAttributeMap.get(pAttributeName);
			}
		}
		return lValue;
	}
	/**
	 * 
	 * ********************************************<br>
	 *
	 * @param <T>
	 * @param pType
	 * @param pName
	 * @param pAttributeType
	 * @param pAttributeName
	 * @return
	 *
	 * ********************************************<br>
	 */
	private <T> T getValueFromDataBase(final Class<? extends ADao> pType, final String pName, 
			final Class<?> pAttributeType, final String pAttributeName) {

		T lValue = null;
		try {
			lValue = Persistency.getInstance().getValueFromFile(pType, pName, pAttributeName, pAttributeType);
		} catch (IOException e) {
			String lMessage = "could not get value to attribute:" + pType + "." + pName + "." + pAttributeName + " e:" + e.getMessage();
			LogWrapper.e(LOGTAG, lMessage);
			throw new RuntimeException(lMessage);
		}

		return lValue;
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pAttributeType
	 * @param pValue
	 * @return
	 *
	 * ********************************************<br>
	 */
	@SuppressWarnings("unchecked")
	private <T> T handleNullValue(final Class<?> pAttributeType, final T pValue) {
		T lValue = pValue;
		if(pValue == null) {
			Integer lKey = typeKeyMap.get(pAttributeType);
			if(lKey == null) {
				lKey = Integer.MAX_VALUE;
			}
			switch(lKey) {
			case KEY_BYTE:
				lValue = (T) BYTE0;
				break;
			case KEY_SHORT:
				lValue = (T) SHORT0;
				break;
			case KEY_INT:
				lValue = (T) INT0;
				break;
			case KEY_LONG:
				lValue = (T) LONG0;
				break;
			case KEY_FLOAT:
				lValue = (T) FLOAT0;
				break;
			case KEY_DOUBLE:
				lValue = (T) DOUBLE0;
				break;
			case KEY_BOOL:
				lValue = (T) Boolean.FALSE;
				break;
			case KEY_STRING:
				break;
			default:
				// LogWrapper.w(LOGTAG, "unhandled key:" + lKey + " for type:" + pAttributeType.getSimpleName());
			}
		}
		return lValue;
	}

	/**
	 *
	 * @param pImportFile
	 * @throws IOException 
	 *
	 */
	public void importData(File pImportFile) throws IOException {
		/**
		 * objectManager has to know which ADao subclasses have been 
		 * imported to delete the cache for the respective classes
		 */
		HashSet<String> lLoadedTypeSet = Persistency.getInstance().importData(pImportFile);
		clearCache(null, lLoadedTypeSet);
		/**
		 * load the information of the loaded data into the object cache
		 */
		for(String lTypeName : lLoadedTypeSet) {
			Class<? extends ADao> lType             = typeNameToDaoMap.get(lTypeName);
			Vector<ADao>          lLoadedObjectList = getObjectsFromDataBase(lType);
		}
	}

	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#init()
	 */
	protected void init() {
		eventBroadcaster.removeAllSubscribers();
		clearCache(null, null);
	}
	/**
	 * 
	 * ********************************************<br>
	 *
	 * read the type information and the instance names
	 * from the database and fill objectCache with it
	 *
	 * ********************************************<br>
	 */
	private  void initObjectCache() {

		objectCache      = new HashMap<String, HashMap<String,ADao>>();
		typeNameToDaoMap = new HashMap<String, Class<? extends ADao>>();
		HashSet<Class<? extends ADao>> lDaoTypeList = ClazzFinder.findSubclasses(ADao.class);
		for(Class<? extends ADao> lDaoType : lDaoTypeList) {

			String                lDaoTypeName      = lDaoType.getSimpleName();
			HashSet<String>       lInstanceNameSet  = Persistency.getInstance().getPersistedInstanceNameSet(lDaoType);
			HashMap<String, ADao> lInstanceMap      = new HashMap<String, ADao>();
			objectCache.put(lDaoTypeName, lInstanceMap);
			typeNameToDaoMap.put(lDaoTypeName, lDaoType);

			for(String lName : lInstanceNameSet) {
				ADao lObject = createInstance(lDaoType, lName);
				if(lObject != null) {
					lInstanceMap.put(lName, lObject);
				}
			}
		}
	}
	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pAttribute
	 * @return
	 *
	 * ********************************************<br>
	 */
	protected boolean isPersistent(Class<? extends ADao> pClass, String pAttributeName) {
		boolean lIsPersistent = reflectionUtil.hasAnnotation(pClass, pAttributeName, Persistent.class); 
		return  lIsPersistent;
	}

	/**
	 * 
	 * ********************************************<br>
	 *
	 * @param <T>
	 * @param pObject
	 * @param pObjectClass
	 * @param pName
	 *
	 * ********************************************<br>
	 */
	private  <T extends ADao> void putObjectToCache(T pObject, Class<? extends ADao> pObjectClass, String pName) {
		if(objectCache == null) {
			initObjectCache();
		}

		String lTypeName = pObjectClass.getSimpleName();

		HashMap<String, ADao> lInstanceMap = objectCache.get(lTypeName);
		if(lInstanceMap == null) {
			lInstanceMap = new HashMap<String, ADao>();
			objectCache.put(lTypeName, lInstanceMap);
		}
		ADao lOldObject = lInstanceMap.put(pName, pObject);

		if(lOldObject == null) {
			/**
			 * put the new object to persistency
			 */
			writeInstanceToDataBase(pName, pObject);

		} else {
			/**
			 * change back the partialTopic of lInstanceMap
			 */
			lInstanceMap.put(pName, pObject);
			throw new IllegalArgumentException("object of type:" + lTypeName + " with name:" + pName + " already exists");
		}
	}
	/**
	 * ********************************************<br>
	 *
	 * @param pAObject
	 * @param pName
	 *
	 * ********************************************<br>
	 */
	protected synchronized  void registerObject(ADao pAObject, String pName) {
		if(pAObject == null) {
			throw new IllegalArgumentException("parameter pAObject must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}

		Class<? extends ADao> lClass = pAObject.getClass();
		putObjectToCache(pAObject, lClass, pName);	
		if(!(pAObject instanceof AReferenceType)) {
			fireObjectCreated(pAObject.getClass(), pName);
		}
	};

	private  <T extends ADao> void removeObjectFromCache(T pObject) {
		String lType = pObject.getType();
		String lName = pObject.getName();
		if(objectCache != null) {
			HashMap<String, ADao> lInstanceMap = objectCache.get(lType);
			if(lInstanceMap != null) {
				lInstanceMap.remove(lName);
				if(lInstanceMap.isEmpty()) {
					objectCache.remove(lType);
				}
			}
		}
		if(cache != null) {
			HashMap<String, HashMap<String, Object>> lInstanceMap = cache.get(lType);
			if(lInstanceMap != null) {
				lInstanceMap.remove(lName);
			}
		}
	}
	/**
	 * ********************************************<br>
	 *
	 * @param pService
	 *
	 * ********************************************<br>
	 */
	protected synchronized void setEventBroadcaster(IEventPublisher pService) {
		eventBroadcaster = pService;
	}

	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#setValue(java.lang.Class, java.lang.String, com.droidfad.data.ADao.AttributesEnum, java.lang.Object)
	 */
	protected synchronized void setValue(final Class<? extends ADao> pType, final String pName, 
			final AttributesEnum pAttribute, final Object pValue) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}
		if(pAttribute == null) {
			throw new IllegalArgumentException("parameter pAttribute must not be null");
		}
		/**
		 * only set a value if the object is registered and available.
		 * 
		 */
		ADao lObject        = getObject(pType, pName);
		if(lObject != null) {
			String  lAttributeName = pAttribute.toString();
			boolean lValueChanged  = setValueToCache(cache, pType, pName, lAttributeName, pValue);
			if(lValueChanged && isPersistent(pType, lAttributeName)) {
				/**
				 * only write the data to database if the value actually changed
				 */
				writeValueToDatabase(pType, pName, lAttributeName, pValue);
			} 
			fireDataSet(pType, pName, pAttribute, pValue);
		} else {
			LogWrapper.w(LOGTAG, "setValue:" + pType.getSimpleName() + "." + pName + " object does not exist");
		}
	}
	/**
	 * ********************************************<br>
	 * @param pCache 
	 *
	 * @param pType
	 * @param pName
	 * @param pAttributeName
	 *
	 * ********************************************<br>
	 */
	private boolean setValueToCache(HashMap<String, HashMap<String, HashMap<String, Object>>> pCache, 
			final Class<? extends ADao> pType, final String pName, final String pAttributeName, final Object pValue) {

		HashMap<String,HashMap<String,Object>> lInstanceMap = pCache.get(pType.getSimpleName());
		if(lInstanceMap == null) {
			lInstanceMap = new HashMap<String, HashMap<String,Object>>();
			pCache.put(pType.getSimpleName(), lInstanceMap);
		}
		HashMap<String, Object> lAttributeMap = lInstanceMap.get(pName);
		if(lAttributeMap == null) {
			lAttributeMap = new HashMap<String, Object>();
			lInstanceMap.put(pName, lAttributeMap);
		}
		boolean lValueChanged = false;
		Object lOldValue = lAttributeMap.put(pAttributeName, pValue);
		if(pValue == null) {
			lValueChanged = lOldValue != null;
		} else {
			lValueChanged = !pValue.equals(lOldValue);
		}
		return lValueChanged;
	}
	/* (non-Javadoc)
	 * @see com.droidfad.data.IObjectManager#unregisterObject(T)
	 */
	protected synchronized <T extends ADao> void unregisterObject(T pObject) {
		if(pObject != null) {

			Persistency.getInstance().removeInstance(pObject.getClass(), pObject.getName());

			/**
			 * delete the attribute cache
			 */
			removeObjectFromCache(pObject);
			if(!(pObject instanceof AReferenceType)) {
				fireObjectDeleted(pObject.getClass(), pObject.getName());
			}
			pObject.isValid = false;
		}
	}
	/**
	 * 
	 *
	 * @param pName
	 * @param pObject
	 *
	 */
	private void writeInstanceToDataBase(String pName, ADao pObject) {
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}
		if(pObject == null) {
			throw new IllegalArgumentException("parameter pObject must not be null");
		}
		Class<? extends ADao> lType              = pObject.getClass();
		List<String>          lAttributeNameList = reflectionUtil.getPersistentAttributeNames(pObject.getClass(), null);
		String                lAttributeName     = null;
		Object                lValue             = null;
		if(lAttributeNameList.isEmpty()) {
			lAttributeName = Persistency.DUMMY_ATTRIBUTE_NAME;
			lValue = "";
		} else {
			lAttributeName = lAttributeNameList.get(0);
			lValue         = reflectionUtil.invokeGetter(pObject, lAttributeName, null);
		}

		try {

			Persistency.getInstance().writeValueToFile(lType, pName, lAttributeName, lValue);

		} catch (IOException e) {
			LogWrapper.e(LOGTAG, "could not write attribute:" + lType.getSimpleName() + "." + pName +
					"." + lAttributeName);
		}

	}
	/**
	 * ********************************************<br>
	 *
	 * @param pType
	 * @param pName
	 * @param pAttributeName
	 * @throws IOException 
	 *
	 * ********************************************<br>
	 */
	private void writeValueToDatabase(Class<? extends ADao> pType, String pName, String pAttributeName, Object pValue)  {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pType must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}
		if(pAttributeName == null) {
			throw new IllegalArgumentException("parameter pAttributeName must not be null");
		}
		if(pValue != null) {

			try {

				Persistency.getInstance().writeValueToFile(pType, pName, pAttributeName, pValue);

			} catch (IOException e) {
				LogWrapper.e(LOGTAG, ("could not set value to file for:" + pType + "." + pName
						+ "." + pAttributeName + "=" + pValue));
			}

		} 
	}

	/**
	 *
	 * @param pPrintWriter
	 *
	 */
	public void dumpPersistency(PrintWriter pPrintWriter) {
		Persistency.getInstance().dump(pPrintWriter);
	}
}
