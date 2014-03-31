package com.droidfad.data;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.droidfad.classloading.ClazzFinder;
import com.droidfad.data.AReferenceType.Cardinality;
import com.droidfad.iframework.event.IEvent;
import com.droidfad.iframework.event.IEvent.DataType;
import com.droidfad.iframework.event.IEvent.EventType;
import com.droidfad.iframework.event.IEventPublisher;
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
 */
class ReferenceManager {

	private static final String LOGTAG = ReferenceManager.class.getSimpleName();

	private static ReferenceManager instance = null;

	/**
	 * 
	 */
	private static volatile IEventPublisher eventBroadcaster = null;

	private HashMap<Class<? extends AReferenceType>, AReferenceType> 
	referenceMap = new HashMap<Class<? extends AReferenceType>, AReferenceType>();

	private HashMap<Class<? extends ADao>, List<Class<? extends AReferenceType>>> 
	referenceFromTypeMap = new HashMap<Class<? extends ADao>, List<Class<? extends AReferenceType>>>();

	private HashMap<Class<? extends ADao>, List<Class<? extends AReferenceType>>> 
	referenceToTypeMap = new HashMap<Class<? extends ADao>, List<Class<? extends AReferenceType>>>();

	private volatile boolean isInitialized;
	/**
	 * key     - reference type name, which is the simple name of the class
	 * key     - source object name
	 * key     - target object name
	 * value   - reference object
	 */
	private HashMap<String, HashMap<String, HashMap<String, Vector<_Reference>>>>
	persistentReferenceMap = new HashMap<String, HashMap<String,HashMap<String,Vector<_Reference>>>>();

	/**
	 * key     - reference class name
	 * value   - representing instance
	 */
	private static HashMap<String, AReferenceType> referenceClassToType;

	protected ReferenceManager() { 
	}

	public static synchronized ReferenceManager getImpl() {
		if(instance == null) {
			instance = new ReferenceManager();
		}
		return instance;
	}

	/**
	 * 
	 * ********************************************<br>
	 *
	 * @param pActivity 
	 *
	 * ********************************************<br>
	 */
	private synchronized void initializeReferenceMaps() {

		isInitialized = true;

		/**
		 * create a list of available AReferenceType sub classes
		 */
		HashSet<Class<? extends AReferenceType>> lReferenceTypeClassSet     = ClazzFinder.findSubclasses(AReferenceType.class);
		HashSet<String>                          lReferenceTypeClassNameSet = new HashSet<String>();
		for(Class<? extends AReferenceType> lClazz : lReferenceTypeClassSet) {
			lReferenceTypeClassNameSet.add(lClazz.getName());
		}

		/**
		 * read the persistently stored descriptors from persistency
		 */
		ObjectManager         lObjectManager    = ObjectManager.getImpl();
		Vector<_RefTypeDescr> lRefTypeDescrList = _RefTypeDescr.getInstances();
		/**
		 * delete the ones that do not reference a AreferenceType anymore
		 */
		deleteObsoleteRefTypeDescr(lObjectManager, lRefTypeDescrList,lReferenceTypeClassNameSet);
		HashMap<Integer, Integer> lOldNewIdMap = updateRefTypeDescs(lRefTypeDescrList, lReferenceTypeClassSet);

		/**
		 * next step, get the persistently stored instance of AReferenceType to
		 * be able to initialize the available typeIds
		 */
		HashSet<? extends AReferenceType> lReferenceTypeSet = initializeReferenceTypes(lReferenceTypeClassSet);

		for(AReferenceType lReferenceType : lReferenceTypeSet) {
			referenceMap.put(lReferenceType.getClass(), lReferenceType);
		}

		/**
		 * initialize the from type and to type maps
		 */
		initializeFromAndToTypeMaps();
		
		/**
		 * recover the persistent references
		 */
		referenceClassToType = new HashMap<String, AReferenceType>();
		for(AReferenceType lReferenceType : referenceMap.values()) {
			AReferenceType lExistingType = referenceClassToType.put(lReferenceType.getClass().getSimpleName(), lReferenceType);
			if(lExistingType != null) {
				LogWrapper.e(LOGTAG, "at least 2 AReference sub types exist with same simple name:" + 
						lReferenceType + " and " + lExistingType);
			}
		}
		/**
		 * register the references that are contained in lReferenceList in the hashmaps
		 * of referenceManager.
		 */
		registerReferences(lOldNewIdMap);

	}

	/**
	 *
	 *
	 */
	protected void initializeFromAndToTypeMaps() {
		for(AReferenceType lReferenceType : referenceMap.values()) {
			/**
			 * the from map
			 */
			Class<? extends ADao>                 lSourceType        = lReferenceType.getSourceType();
			List<Class<? extends AReferenceType>> lReferenceTypeList = referenceFromTypeMap.get(lSourceType);
			if(lReferenceTypeList == null) {
				lReferenceTypeList = new Vector<Class<? extends AReferenceType>>();
				referenceFromTypeMap.put(lSourceType, lReferenceTypeList);
			}
			lReferenceTypeList.add(lReferenceType.getClass());
			/**
			 * the to map
			 */
			Class<? extends ADao> lTargetType = lReferenceType.getTargetType();
			lReferenceTypeList                = referenceToTypeMap.get(lTargetType);
			if(lReferenceTypeList == null) {
				lReferenceTypeList = new Vector<Class<? extends AReferenceType>>();
				referenceToTypeMap.put(lTargetType, lReferenceTypeList);
			}
			lReferenceTypeList.add(lReferenceType.getClass());
		}
	}

	/**
	 * register the references that are contained in lReferenceList in the hashmaps
	 * of referenceManager.
	 * @param pOldNewIdMap 
	 */
	protected void registerReferences(HashMap<Integer, Integer> pOldNewIdMap) {
		ObjectManager      lObjectManager = ObjectManager.getImpl();
		Vector<_Reference> lReferenceList = lObjectManager.getInstances(_Reference.class);

		for(_Reference lReference : lReferenceList) {
			int            lReferenceTypeId  = lReference.getRefId();
			Integer        lNewId            = pOldNewIdMap.get(lReferenceTypeId);
			/**
			 * change the reference type id if it has been changed during initialization
			 */
			if(lNewId != null) {
				lReference.setRefId((short)(int) lNewId);
				lReferenceTypeId = lNewId;
			}
			_RefTypeDescr  lRefTypeDescr     = _RefTypeDescr.getInstance("" + lReferenceTypeId);
			boolean        lSuccess          = false;

			if(lRefTypeDescr != null) {
				AReferenceType lReferenceType = referenceClassToType.get(lRefTypeDescr.getRefType());
				if(lReferenceType != null) {
					String lSourceName    = lReference.getSrc();
					ADao   lSource        = lObjectManager.getInstance(lReferenceType.getSourceType(), lSourceName); 
					String lTargetName    = lReference.getTrg();
					ADao   lTarget        = lObjectManager.getInstance(lReferenceType.getTargetType(), lTargetName);

					if(lSource != null && lTarget != null) {
						/**
						 * put it to the internal hashmaps
						 */
						storePersistentReferenceObject(lReferenceType.getClass(), lSource, lTarget, lReference);
						createReference(lReferenceType.getClass(), lSource, lTarget, false);
						lSuccess = true;
					}
				}
			} 
			if(!lSuccess) {
				/**
				 * if the reference type is no longer available, delete
				 * the reference in the database
				 */
				lObjectManager.unregisterObject(lReference);
			}
		}
	}

	/**
	 *
	 * @param pReferenceType
	 * @param pSource
	 * @param pTarget
	 * @param pReference
	 *
	 */
	private void storePersistentReferenceObject(
			Class<? extends AReferenceType> pReferenceType, ADao pSource, ADao pTarget,
			_Reference pReference) {

		HashMap<String, HashMap<String, Vector<_Reference>>> lSourceMap = persistentReferenceMap.get(pReferenceType.getSimpleName());
		if(lSourceMap == null) {
			lSourceMap = new HashMap<String, HashMap<String,Vector<_Reference>>>();
			persistentReferenceMap.put(pReferenceType.getSimpleName(), lSourceMap);
		}
		HashMap<String, Vector<_Reference>> lTargetMap = lSourceMap.get(pSource.getName());
		if(lTargetMap == null) {
			lTargetMap = new HashMap<String, Vector<_Reference>>();
			lSourceMap.put(pSource.getName(), lTargetMap);
		}
		Vector<_Reference> lReferenceList = lTargetMap.get(pTarget.getName());
		if(lReferenceList == null) {
			lReferenceList = new Vector<_Reference>();
			lTargetMap.put(pTarget.getName(), lReferenceList);
		}
		lReferenceList.add(pReference);
	}

	/**
	 * 
	 *
	 * @param pReferenceType
	 * @param pSource
	 * @param pTarget
	 *
	 */
	private void deletePersistentReferenceObject(
			Class<? extends AReferenceType> pReferenceType, ADao pSource, ADao pTarget) {

		HashMap<String, HashMap<String, Vector<_Reference>>> lSourceMap = persistentReferenceMap.get(pReferenceType.getSimpleName());
		if(lSourceMap != null) {
			HashMap<String, Vector<_Reference>> lTargetMap = lSourceMap.get(pSource.getName());
			if(lTargetMap != null) {
				Vector<_Reference> lReferenceList = lTargetMap.get(pTarget.getName());
				if(lReferenceList != null) {
					if(!lReferenceList.isEmpty()) {
						_Reference lReference = lReferenceList.remove(0);
						ObjectManager.getImpl().unregisterObject(lReference);
					}
					if(lReferenceList.isEmpty()) {
						lTargetMap.remove(pTarget.getName());
					}
				}
				if(lTargetMap.isEmpty()) {
					lSourceMap.remove(pSource.getName());
				}
			}
		}
	}

	/**
	 *
	 * @param pObjectManager
	 * @param pRefTypeDescrList
	 * @param pReferenceTypeClassNameSet
	 *
	 */
	protected void deleteObsoleteRefTypeDescr(ObjectManager pObjectManager,
			Vector<_RefTypeDescr> pRefTypeDescrList,
			HashSet<String> pReferenceTypeClassNameSet) {
		for(_RefTypeDescr lDescriptor : pRefTypeDescrList) {

			String lRefClazzname = lDescriptor.getClazz(); 
			if(!pReferenceTypeClassNameSet.contains(lRefClazzname)) {
				/**
				 * class does not exist, so delete the respective descriptors
				 */
				pObjectManager.unregisterObject(lDescriptor);
				_RefTypeDescr lDeleteRefTypeDescr = _RefTypeDescr.getInstance(lDescriptor.getRefType());
				if(lDeleteRefTypeDescr != null) {
					pObjectManager.unregisterObject(lDeleteRefTypeDescr);
				}
				lDeleteRefTypeDescr = _RefTypeDescr.getInstance("" + lDescriptor.getId());
				if(lDeleteRefTypeDescr != null) {
					pObjectManager.unregisterObject(lDeleteRefTypeDescr);
				}
			}
		}
	}

	/**
	 *
	 * @param pRefTypeDescrList
	 * @param pReferenceTypeClassList
	 * @throws Error
	 *
	 */
	protected HashMap<Integer, Integer> updateRefTypeDescs(Vector<_RefTypeDescr> pRefTypeDescrList,
			HashSet<Class<? extends AReferenceType>> pReferenceTypeClassList)
			throws Error {


		HashMap<Integer, Integer> lOldNewIdMap = new HashMap<Integer, Integer>();

		/**
		 * create new instances of _RefTypeDescr for the reference classes that exist
		 * but do not have a representation as _RefTypeDescr instance yet.
		 * If an inconsistent configuration is found all concerned RefTypeDesc instances
		 * are deleted
		 */
		for(Class<? extends AReferenceType> lReferenceClass : pReferenceTypeClassList) {
			/**
			 * create new instances for the not yet existing ones
			 */
			String       lClazzname    = lReferenceClass.getName();
			String       lType         = lReferenceClass.getSimpleName();

			_RefTypeDescr lTyDescriptor = _RefTypeDescr.getInstance(lType);
			boolean       lIsRepair     = lTyDescriptor == null;
			_RefTypeDescr lCNDescriptor = _RefTypeDescr.getInstance(lClazzname);
			lIsRepair                 |= lCNDescriptor == null;

			int          lId           = -1;
			if(lTyDescriptor != null && lCNDescriptor != null) {
				lId                    = lTyDescriptor.getId();                   
				lIsRepair             |= lId != lCNDescriptor.getId();
			}
			if(lId == -1) {
				if(lTyDescriptor != null ) {
					lId = lTyDescriptor.getId();
				} else if(lCNDescriptor != null) {
					lId = lCNDescriptor.getId();
				}
			}
			lIsRepair                 |= lId == -1;
			_RefTypeDescr lIdDescriptor = null;
			if(!lIsRepair) {
				lIdDescriptor = _RefTypeDescr.getInstance("" + lId);
			}
			lIsRepair                 |= lIdDescriptor == null;
			if(lIsRepair) {
				/**
				 * delete all old instances
				 */
				if(lTyDescriptor != null) {
					ObjectManager.getImpl().unregisterObject(lTyDescriptor);
				}
				if(lCNDescriptor != null) {
					ObjectManager.getImpl().unregisterObject(lCNDescriptor);
				}
				if(lIdDescriptor != null) {
					ObjectManager.getImpl().unregisterObject(lIdDescriptor);
				}
				/**
				 * get the next free id
				 */
				short lNewId = -1;
				for(short s=0; s<Short.MAX_VALUE; s++) {
					_RefTypeDescr lExistingDescriptor = _RefTypeDescr.getInstance("" + s);
					if(lExistingDescriptor == null) {
						lNewId = s;
						break;
					}
				}
				if(lNewId == -1) {
					throw new Error("class references run out of indices");
				}
				new _RefTypeDescr(lClazzname,  lClazzname, lType, lNewId);
				new _RefTypeDescr(lType,  lClazzname, lType, lNewId);
				new _RefTypeDescr("" + lNewId,  lClazzname, lType, lNewId);
				/**
				 * save the information of old and new id
				 */
				lOldNewIdMap.put(lId, (int) lNewId);
			}
		}

		return lOldNewIdMap;
	}

	/**
	 * ********************************************<br>
	 * @param pReferenceTypeClassSet 
	 *
	 * @return
	 *
	 * ********************************************<br>
	 */
	private HashSet<AReferenceType> initializeReferenceTypes(HashSet<Class<? extends AReferenceType>> pReferenceTypeClassSet) {

		HashSet<AReferenceType> lReferenceTypeSet = new HashSet<AReferenceType>();
		ObjectManager           lObjectManager    = ObjectManager.getImpl();
		for(Class<? extends AReferenceType> lReferenceTypeClass : pReferenceTypeClassSet) {
			Vector<AReferenceType> lInstanceList = lObjectManager.getInstances(lReferenceTypeClass);
			/**
			 * only one instance is necessary and must be stored
			 */
			AReferenceType lReferenceType = null;
			if(lInstanceList.isEmpty()) {
				/**
				 * no instance exists, create the necessary instance
				 */
				try {
					Constructor<? extends AReferenceType> lConstructor = lReferenceTypeClass.getConstructor();
					lReferenceType = lConstructor.newInstance();
				} catch (Exception e) {
					LogWrapper.e(LOGTAG, "initializePersistedReferenceTypes: could not create instance of:" + lReferenceTypeClass.getName()+ " e:" + e);
				} 
			} else {
				lReferenceType = lInstanceList.firstElement();
				if(lInstanceList.size() > 1) {
					/**
					 * delete the not necessary instances
					 */
					Vector<AReferenceType> lToBeDeletedList = new Vector<AReferenceType>(lInstanceList);
					lToBeDeletedList.remove(0);
					for(AReferenceType lToBeDeleted : lToBeDeletedList) {
						lObjectManager.unregisterObject(lToBeDeleted);
					}
				}
			}
			lReferenceTypeSet.add(lReferenceType);
		}
		return lReferenceTypeSet;
	}

	/**
	 * key   - reference type
	 * key   - source object
	 * value - target object list
	 */
	private ConcurrentHashMap<ADao, ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>>>
	forwardMap = new ConcurrentHashMap<ADao, ConcurrentHashMap<Class<? extends AReferenceType>,List<ADao>>>(); 

	/**
	 * key   - reference type
	 * key   - target object
	 * value - source object list
	 */
	private ConcurrentHashMap<ADao, ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>>>
	backwardMap = new ConcurrentHashMap<ADao, ConcurrentHashMap<Class<? extends AReferenceType>,List<ADao>>>();

	/**
	 * 
	 * @param pReference
	 * @param pTargetObject
	 * @return
	 */
	public ADao getSourceObject(Class<? extends AReferenceType> pReference, ADao pTargetObject) {
		if(!isInitialized) initializeReferenceMaps();

		ADao lReturn = null;

		List<ADao> lSourceList = getSourceObjectList(pReference, pTargetObject);
		if(lSourceList != null && !lSourceList.isEmpty()) {
			lReturn = lSourceList.get(0);
		}

		return lReturn;
	}

	/**
	 * 
	 * @param pReference
	 * @param pTargetObject
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends ADao> List<T> getSourceObjectList(Class<? extends AReferenceType> pReference, ADao pTargetObject) {
		if(pReference == null) {
			throw new IllegalArgumentException("parameter pReference must not be null");
		}
		if(pTargetObject == null) {
			throw new IllegalArgumentException("parameter pTargetObject must not be null");
		}
		if(!isInitialized) initializeReferenceMaps();

		ConcurrentHashMap<Class<? extends AReferenceType>,List<ADao>> lReferenceMap = backwardMap.get(pTargetObject);
		List<ADao> lSourceList = null;
		if(lReferenceMap != null) {
			lSourceList = lReferenceMap.get(pReference);
		}
		if(lSourceList != null) {
			lSourceList = new Vector<ADao>(lSourceList);
		} else {
			lSourceList = new Vector<ADao>(0);
		}
		return (List<T>) lSourceList;
	}

	/**
	 * 
	 * @param pReference
	 * @param pSourceObject
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends ADao> T getTargetObject(Class<? extends AReferenceType> pReference, ADao pSourceObject) {
		if(!isInitialized) initializeReferenceMaps();
		ADao lReturn = null;

		List<ADao> lTargetList = getTargetObjectList(pReference, pSourceObject);
		if(lTargetList != null && !lTargetList.isEmpty()) {
			lReturn = lTargetList.get(0);
		}

		return (T) lReturn;
	}

	/**
	 * 
	 * @param pReference
	 * @param pSourceObject
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends ADao> Vector<T> getTargetObjectList(Class<? extends AReferenceType> pReference, ADao pSourceObject) {

		if(pReference == null) {
			throw new IllegalArgumentException("parameter pReference must not be null");
		}
		if(pSourceObject == null) {
			throw new IllegalArgumentException("parameter pSourceObject must not be null");
		}
		if(!isInitialized) initializeReferenceMaps();

		ConcurrentHashMap<Class<? extends AReferenceType>,List<ADao>> lReferenceMap = forwardMap.get(pSourceObject);
		List<? extends ADao> lTargetList = null;
		if(lReferenceMap != null) {
			lTargetList = lReferenceMap.get(pReference);
		}
		if(lTargetList != null) {
			lTargetList = new Vector<ADao>(lTargetList); 
		} else {
			lTargetList = new Vector<ADao>(0);
		}
		return  (Vector<T>) lTargetList;
	}

	public void createReference(Class<? extends AReferenceType> pReference, ADao pSourceObject, ADao pTargetObject) {
		createReference(pReference, pSourceObject, pTargetObject, true);
	}
	private void createReference(Class<? extends AReferenceType> pReferenceType,
			ADao pSourceObject, ADao pTargetObject, boolean pCreatePersistentObject) {

		if(pReferenceType == null) {
			throw new IllegalArgumentException("parameter pReference must not be null");
		}
		if(pSourceObject == null) {
			throw new IllegalArgumentException("parameter pSourceObject must not be null");
		}
		if(pTargetObject == null) {
			throw new IllegalArgumentException("parameter pTargetObject must not be null");
		}
		if(!isInitialized) initializeReferenceMaps();
		AReferenceType lReferenceType = referenceMap.get(pReferenceType);
		if(lReferenceType == null) {
			throw new IllegalArgumentException("reference type not registered, maybe default constructor not implemented:" + pReferenceType.getName());
		}

		Class<? extends ADao> lSourceType = lReferenceType.getSourceType();
		Class<? extends ADao> lTargetType = lReferenceType.getTargetType();
		if(!lSourceType.isAssignableFrom(pSourceObject.getClass())) {
			throw new IllegalArgumentException("class of parameter pSourceObject:"+ pSourceObject
					+ " is not a subclass of class:"+lSourceType.getName()
					+" defined by parameter pReference:" + pReferenceType.getName()
					+ " as source type");
		}
		if(!lTargetType.isAssignableFrom(pTargetObject.getClass())) {
			throw new IllegalArgumentException("class of parameter pTargetObject:"+ pTargetObject.getClass().getName() 
					+ " is not a subclass of class:"+lTargetType.getClass().getName()
					+" defined by parameter pReference:" + pReferenceType.getName()
					+ " as target type");
		}

		boolean lSuccess  = createForwardReference(pReferenceType, pSourceObject, pTargetObject);
		lSuccess         &= createBackwardReference(pReferenceType, pSourceObject, pTargetObject);
		if(lSuccess) {
			if(pCreatePersistentObject) {
				String       lReferenceTypeName = pReferenceType.getSimpleName();
				_RefTypeDescr lRefTypeDescr      = _RefTypeDescr.getInstance(lReferenceTypeName);
				_Reference    lReference         = new _Reference(lRefTypeDescr.getId(), pSourceObject.getName(), pTargetObject.getName());
				storePersistentReferenceObject(pReferenceType, pSourceObject, pTargetObject, lReference);
			}
			if(eventBroadcaster != null) {
				IEvent lEvent = eventBroadcaster.createEvent(null, DataType.reference, EventType.created, 
						lReferenceType.getClass(), pSourceObject.getName() + "-" + pTargetObject.getName(), 
						this, null, null, null);
				eventBroadcaster.publish(lEvent);
			}
		}
	}

	/**
	 * 
	 * @param pReference
	 * @param pSourceObject
	 * @param pTargetObject
	 */
	private boolean createBackwardReference(
			Class<? extends AReferenceType> pReference, ADao pSourceObject,
			ADao pTargetObject) {

		boolean lSuccess = true;

		ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>> lReferenceMap = backwardMap.get(pTargetObject);
		if(lReferenceMap == null) {
			lReferenceMap = new ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>>();
			backwardMap.put(pTargetObject, lReferenceMap);
		}
		List<ADao> lSourceList = lReferenceMap.get(pReference);
		if(lSourceList == null) {
			lSourceList = new Vector<ADao>();
			lReferenceMap.put(pReference, lSourceList);
		}
		synchronized (lSourceList) {
			if(lSourceList.contains(pSourceObject)) {
				LogWrapper.w(LOGTAG, "reference of type:" + pReference.getName() + " from:" + pSourceObject
						+ " to:" + pTargetObject + " already exists!");
				lSuccess = false;
			} else {
				lSourceList.add(pSourceObject);
			}
		}

		return lSuccess;
	}

	/**
	 * 
	 * @param pReference
	 * @param pSourceObject
	 * @param pTargetObject
	 */
	private boolean createForwardReference(Class<? extends AReferenceType> pReference,
			ADao pSourceObject, ADao pTargetObject) {

		boolean lSuccess = true;

		ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>> lReferenceMap = forwardMap.get(pSourceObject);
		if(lReferenceMap == null) {
			lReferenceMap = new ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>>();
			forwardMap.put(pSourceObject, lReferenceMap);
		}
		List<ADao> lTargetList = lReferenceMap.get(pReference);
		if(lTargetList == null) {
			lTargetList = new Vector<ADao>();
			lReferenceMap.put(pReference, lTargetList);
		}
		synchronized (lTargetList) {
			if(lTargetList.contains(pTargetObject)) {
				LogWrapper.w(LOGTAG, "reference of type:" + pReference.getName() + " from:" + pSourceObject
						+ " to:" + pTargetObject + " already exists!");
				lSuccess = false;
			} else {
				lTargetList.add(pTargetObject);
			}
		}

		return lSuccess;
	}

	public void removeReferences(ADao pRemovedObject) {
		if(pRemovedObject != null) {
			/**
			 * remove reference from pRemovedObject to other object instances
			 */
			ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>> lForwardRefMap = forwardMap.get(pRemovedObject);
			if(lForwardRefMap != null) {
				Set<Class<? extends AReferenceType>> lKeySet = new HashSet<Class<? extends AReferenceType>>(lForwardRefMap.keySet()); 
				for(Class<? extends AReferenceType> lReferenceType : lKeySet) {
					List<ADao> lInstanceList = lForwardRefMap.get(lReferenceType);
					if(lInstanceList != null) {
						for(ADao lInstance : new Vector<ADao>(lInstanceList)) {
							deleteReference(lReferenceType, pRemovedObject, lInstance);
						}
					}
				}
			}
		}
	}

	public void deleteReference(Class<? extends AReferenceType> pReferenceType,
			ADao pSourceObject, ADao pTargetObject) {

		if(!isInitialized) initializeReferenceMaps();
		boolean lSuccess  = deleteForwardReference(pReferenceType, pSourceObject, pTargetObject);
		lSuccess         &= deleteBackwardReference(pReferenceType, pSourceObject, pTargetObject);

		if(lSuccess && eventBroadcaster != null) {

			deletePersistentReferenceObject(pReferenceType, pSourceObject, pTargetObject);

			ADao lReferenceType = referenceMap.get(pReferenceType);
			IEvent lEvent = eventBroadcaster.createEvent(null, DataType.reference, EventType.deleted, 
					lReferenceType.getClass(), pSourceObject.getName() + "-" + pTargetObject.getName(), 
					this, null, null, null);
			eventBroadcaster.publish(lEvent);
		}
	}

	private boolean deleteBackwardReference(Class<? extends AReferenceType> pReference, 
			ADao pSourceObject, ADao pTargetObject) {

		boolean lSuccess = false;

		ConcurrentHashMap<Class<? extends AReferenceType>, List<ADao>> lReferenceMap = backwardMap.get(pTargetObject);
		if(lReferenceMap != null) {
			List<ADao> lSourceList = lReferenceMap.get(pReference);
			if(lSourceList != null) {
				lSuccess = lSourceList.remove(pSourceObject);
			}
		}

		return lSuccess;
	}

	private boolean deleteForwardReference(Class<? extends AReferenceType> pReference,
			ADao pSourceObject, ADao pTargetObject) {

		boolean lSuccess = false;

		ConcurrentHashMap<Class<? extends AReferenceType>,List<ADao>> lSourceMap = forwardMap.get(pSourceObject);
		if(lSourceMap != null) {
			List<ADao> lTargetList = lSourceMap.get(pReference);
			if(lTargetList != null) {
				lSuccess = lTargetList.remove(pTargetObject);
			}
		}		
		return lSuccess;
	}

	/**
	 * 
	 * ********************************************<br>
	 *
	 * @param pType
	 * @return
	 *
	 * ********************************************<br>
	 */
	public List<Class<? extends AReferenceType>> getReferenceTypesFromADaoType(Class<? extends ADao> pType) {
		if(!isInitialized) initializeReferenceMaps();
		return getReferenceTypeList(referenceFromTypeMap, pType);
	}
	/**
	 * 
	 * ********************************************<br>
	 *
	 * take care that only copies of the internal list 
	 * are returned to avoid that the internal lists
	 * are damaged
	 * 
	 * @param pReferenceTypeMap
	 * @param pType
	 * @return
	 *
	 * ********************************************<br>
	 */
	private List<Class<? extends AReferenceType>> getReferenceTypeList(
			HashMap<Class<? extends ADao>, List<Class<? extends AReferenceType>>> pReferenceTypeMap,
			Class<? extends ADao> pType) {

		if(pType == null) {
			throw new IllegalArgumentException("parameter pType must not be null");
		}
		List<Class<? extends AReferenceType>> lReferenceList =	pReferenceTypeMap.get(pType);
		if(lReferenceList != null) {
			lReferenceList = new Vector<Class<? extends AReferenceType>>(lReferenceList);
		} else {
			lReferenceList = new Vector<Class<? extends AReferenceType>>(0);
		}
		return lReferenceList;
	}
	/**
	 * 
	 * ********************************************<br>
	 *
	 * @param pType
	 * @return
	 *
	 * ********************************************<br>
	 */
	public List<Class<? extends AReferenceType>> getReferenceTypesToADaoType(Class<? extends ADao> pType) {
		if(!isInitialized) initializeReferenceMaps();
		return getReferenceTypeList(referenceToTypeMap, pType);
	}

	public synchronized void setEventBroadcaster(IEventPublisher pService) {
		eventBroadcaster = pService; 
	}

	/**
	 * ********************************************<br>
	 *
	 *
	 * ********************************************<br>
	 */
	public void init() {
		clear();
	}

	/**
	 * ********************************************<br>
	 *
	 *
	 * ********************************************<br>
	 */
	public void clear() {
		isInitialized = false;
		referenceMap.clear();
		referenceFromTypeMap.clear();  
		referenceToTypeMap.clear();
		persistentReferenceMap.clear();
		if(referenceClassToType != null) {
			referenceClassToType.clear(); ;
		}
	}

	/**
	 *
	 * @param pReferenceType
	 * @return
	 *
	 */
	public Class<? extends ADao> getTargetObjectType(Class<? extends AReferenceType> pReferenceType) {
		if(!isInitialized) initializeReferenceMaps();
		AReferenceType lReferenceType = referenceClassToType.get(pReferenceType.getSimpleName());
		return         lReferenceType.getTargetType();
	}

	/**
	 *
	 * @param pReferenceType
	 * @return
	 *
	 */
	public Class<? extends ADao> getSourceObjectType(Class<? extends AReferenceType> pReferenceType) {

		if(!isInitialized) initializeReferenceMaps();
		AReferenceType lReferenceType = referenceClassToType.get(pReferenceType.getSimpleName());
		return         lReferenceType.getSourceType();
	}

	/**
	 *
	 * @param pReferenceType
	 * @return
	 *
	 */
	public Cardinality getCardinality(Class<? extends AReferenceType> pReferenceType) {
		if(pReferenceType == null) {
			throw new IllegalArgumentException("parameter preferenceType must not be null");
		}
		if(!isInitialized) initializeReferenceMaps();
		AReferenceType lReferenceType = referenceClassToType.get(pReferenceType.getSimpleName());
		return         lReferenceType.getCardinality();
	}
}
