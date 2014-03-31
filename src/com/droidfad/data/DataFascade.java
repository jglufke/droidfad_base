/**
 * 
 */
package com.droidfad.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import com.droidfad.data.ACategory.Category;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.data.AReferenceType.Cardinality;
import com.droidfad.iframework.data.IData;
import com.droidfad.iframework.event.IEventPublisher;
import com.droidfad.iframework.event.IEventPublisherUser;
import com.droidfad.iframework.service.IService;

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
public class DataFascade implements IData, IEventPublisherUser {

	private ObjectManager     objectManager;
	private ReferenceManager  referenceManager;

	public DataFascade() {
		objectManager    = ObjectManager.getImpl();
		referenceManager = ReferenceManager.getImpl();
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#init()
	 */
	@Override
	public synchronized void init() {
		objectManager.init();
		referenceManager.init();
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#clear()
	 */
	@Override
	public synchronized void clear() {
		objectManager.clear();
		referenceManager.clear();
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getInstances(java.lang.String)
	 */
	@Override
	public synchronized <T extends ADao> Vector<T> getInstances(Class<? extends ADao> pType) {
		return objectManager.getInstances(pType);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getObject(java.lang.Class, java.lang.String)
	 */
	@Override
	public synchronized <T extends ADao> T getInstance(Class<? extends ADao> pType, String pName) {
		return objectManager.getInstance(pType, pName);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getObject(java.lang.String, java.lang.String)
	 */
	@Override
	public synchronized <T extends ADao> T getObject(Class<? extends ADao> pType, String pName) {
		return objectManager.getObject(pType, pName);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getValue(java.lang.Class, java.lang.String, com.droidfad.data.ADao.AttributesEnum)
	 */
	@Override
	public synchronized <T> T getValue(Class<? extends ADao> pClass, String pName, AttributesEnum pAttribute) {
		return objectManager.getValue(pClass, pName, pAttribute);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#setValue(java.lang.Class, java.lang.String, com.droidfad.data.ADao.AttributesEnum, java.lang.Object)
	 */
	@Override
	public synchronized void setValue(Class<? extends ADao> pClass, String pName, AttributesEnum pAttribute, Object pValue) {
		objectManager.setValue(pClass, pName, pAttribute, pValue);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#unregisterObject(com.droidfad.data.ADao)
	 */
	@Override
	public synchronized <T extends ADao> void unregisterObject(T pObject) {
		objectManager.unregisterObject(pObject);
		referenceManager.removeReferences(pObject);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getReferenceTypesFromYType(java.lang.Class)
	 */
	@Override
	public synchronized List<Class<? extends AReferenceType>> getReferenceTypesFromADaoType(Class<? extends ADao> pYType) {
		return referenceManager.getReferenceTypesFromADaoType(pYType);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getReferenceTypesToYType(java.lang.Class)
	 */
	@Override
	public synchronized List<Class<? extends AReferenceType>> getReferenceTypesToADaoType(Class<? extends ADao> pType) {
		return referenceManager.getReferenceTypesToADaoType(pType);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getTargetObject(java.lang.Class, com.droidfad.data.ADao)
	 */
	@Override
	public synchronized <T extends ADao> T getTargetObject(Class<? extends AReferenceType> pReference, ADao pSourceObject) {
		return referenceManager.getTargetObject(pReference, pSourceObject);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getTargetObjectList(java.lang.Class, com.droidfad.data.ADao)
	 */
	@Override
	public synchronized <T extends ADao> List<T> getTargetObjectList(Class<? extends AReferenceType> pReference, ADao pSourceObject) {
		return referenceManager.getTargetObjectList(pReference, pSourceObject);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getSourceObject(java.lang.Class, com.droidfad.data.ADao)
	 */
	@Override
	public synchronized ADao getSourceObject(Class<? extends AReferenceType> pReference,	ADao pTargetObject) {
		return referenceManager.getSourceObject(pReference, pTargetObject);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getSourceObjectList(java.lang.Class, com.droidfad.data.ADao)
	 */
	@Override
	public synchronized <T extends ADao> List<T> getSourceObjectList(Class<? extends AReferenceType> pReference, ADao pTargetObject) {
		return referenceManager.getSourceObjectList(pReference, pTargetObject);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.service.IServiceUser#registerService(com.droidfad.iframework.service.IService)
	 */
	@Override
	public synchronized void registerService(IService pService) {
		if(pService instanceof IEventPublisher) {
			objectManager.setEventBroadcaster((IEventPublisher) pService);
			referenceManager.setEventBroadcaster((IEventPublisher) pService);
		}
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#createReference(java.lang.Class, com.droidfad.data.ADao, com.droidfad.data.ADao)
	 */
	@Override
	public synchronized void createReference(Class<? extends AReferenceType> pReferenceType, ADao pSourceObject, ADao pTargetObject) {
		referenceManager.createReference(pReferenceType, pSourceObject, pTargetObject);
	}
	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#createReference(java.lang.Class, com.droidfad.data.ADao, com.droidfad.data.ADao)
	 */
	@Override
	public synchronized void deleteReference(Class<? extends AReferenceType> pReferenceType, ADao pSourceObject, ADao pTargetObject) {
		referenceManager.deleteReference(pReferenceType, pSourceObject, pTargetObject);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getObjectTypeNameList()
	 */
	@Override
	public synchronized Vector<Class<? extends ADao>> getObjectTypeList() {
		return objectManager.getObjectTypeList();
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#createObject(java.lang.Class, java.lang.String)
	 */
	@Override
	public synchronized ADao createInstance(Class<? extends ADao> pType, String pName) {
		return objectManager.createInstance(pType, pName);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getTargetObjectType(java.lang.Class)
	 */
	@Override
	public synchronized Class<? extends ADao> getTargetObjectType(Class<? extends AReferenceType> pReferenceType) {
		return referenceManager.getTargetObjectType(pReferenceType);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getSourceObjectType(java.lang.Class)
	 */
	@Override
	public synchronized Class<? extends ADao> getSourceObjectType(Class<? extends AReferenceType> pReferenceType) {
		return referenceManager.getSourceObjectType(pReferenceType);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getCardinality(java.lang.Class)
	 */
	@Override
	public synchronized Cardinality getCardinality(Class<? extends AReferenceType> pReferenceType) {
		return referenceManager.getCardinality(pReferenceType);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#getDaoClassFromTypeName(java.lang.String)
	 */
	@Override
	public synchronized Class<? extends ADao> getDaoClassFromTypeName(String pTypeName) {
		return objectManager.getDaoClassFromTypeName(pTypeName);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#exportData(com.droidfad.data.ACategory.Category, java.io.File)
	 */
	@Override
	public synchronized void exportData(Category pCategory, File pExportFile) throws IOException {
		objectManager.exportData(pCategory, pExportFile);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#importData(java.io.File)
	 */
	@Override
	public synchronized void importData(File pImportFile) throws IOException {
		referenceManager.clear();
		objectManager.importData(pImportFile);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.iframework.data.IData#dumpPersistency(java.io.PrintWriter)
	 */
	@Override
	public void dumpPersistency(PrintWriter pPrintWriter) {
		objectManager.dumpPersistency(pPrintWriter);
	}
}
