package com.droidfad.iframework.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import com.droidfad.data.ADao;
import com.droidfad.data.ACategory.Category;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.data.AReferenceType.Cardinality;
import com.droidfad.data.AReferenceType;
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
 *
 *   This interface defines the methods of the persistency mechanism of 
 *   DroidFad. see http://www.droidfad.com/html/persistency/persistency.htm
 */
public interface IData extends IService {

	/**
	 *
	 * initialize the data interface. This method is
	 * called during initialization of the framework
	 * by DroidFad or DroidFadJSE class. So there is 
	 * no need to call this method during regular 
	 * operation of the framework
	 *
	 */
	public void init();

	/**
	 *
	 * clear all data and persistent files
	 * 
	 */
	public void clear();

	/**
	 * 
	 * get all registered instances of ADao implementation
	 * classes. 
	 *
	 * @param <T>
	 * @param pType subclass of ADao
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	public <T extends ADao> Vector<T> getInstances(Class<? extends ADao> pType);

	/**
	 * 
	 * get the single instance of a ADao subclass that
	 * has the name pName
	 *
	 * @param <T>
	 * @param pType
	 * @param pName
	 * @return
	 *
	 */
	public <T extends ADao> T getObject(Class<? extends ADao> pType, String pName);

	/**
	 * 
	 * get the value of pAttributeName of an instance
	 * of pType with the name pName. This method should
	 * only be called by the ADao class itself. Probably,
	 * there is not need for any implementing class to 
	 * call this method because it is encapsulated in
	 * the getValue method of ADao.
	 *
	 * @param <T>
	 * @param pType
	 * @param pName
	 * @param pAttributeType
	 * @param pAttributeName
	 * @return
	 *
	 */
	public <T> T getValue(Class<? extends ADao> pClass, String pName, AttributesEnum pAttribute);

	/**
	 * 
	 * set the pValue to pAttribute for the instance of pType with 
	 * pName. Calling this method is encapsulated in the ADao implementation
	 * class and should not be called by other methods directly
	 *
	 * @param pType
	 * @param pName
	 * @param pAttribute
	 * @param pValue
	 *
	 */
	public void setValue(final Class<? extends ADao> pType,
			final String pName, final AttributesEnum pAttribute,
			final Object pValue);

	/**
	 * 
	 * method to unregister pObject from the framework. Calling
	 * this method will set pObject to invalid and any further call
	 * to a method of pObject will cause a RuntimeException to be
	 * thrown by DroidFad framework
	 * 
	 * @param <T>
	 * @param pObject
	 *
	 */
	@SuppressWarnings("static-access")
	public <T extends ADao> void unregisterObject(T pObject);
	
	/**
	 *
	 * get the list of reference types that point from
	 * pYType to another type
	 * @param pYType
	 * @return
	 *
	 */
	public List<Class<? extends AReferenceType>> getReferenceTypesFromADaoType(Class<? extends ADao> pYType);
	
	/**
	 * 
	 * get a list of reference types that point to
	 * pYType from another type
	 * @param pYType
	 * @return
	 *
	 */
	public List<Class<? extends AReferenceType>>  getReferenceTypesToADaoType(Class<? extends ADao> pYType);

	/**
	 * 
	 * Get one object to which pReference is pointing to from pSourceObject.
	 * If more than one target objects exist it is not defined which of these
	 * is returned 
	 *
	 * @param pReference
	 * @param pSourceObject
	 * @return
	 *
	 */
	public <T extends ADao> T getTargetObject(Class<? extends AReferenceType> pReference, ADao pSourceObject);
	
	/**
	 *  
	 *  get the ADao subclass to which pReferenceType is pointing
	 *
	 * @param pReferenceType
	 * @return
	 *
	 */
	public Class<? extends ADao> getTargetObjectType(Class<? extends AReferenceType> pReferenceType);
	
	/**
	 * 
	 * get the list of objects to which references of type pReference are
	 * pointing from pSourceObject
	 *
	 * @param pReference
	 * @param pSourceObject
	 * @return
	 *
	 */
	public <T extends ADao> List<T> getTargetObjectList(Class<? extends AReferenceType> pReference, ADao pSourceObject);
	
	/**
	 * 
	 * get a single object that is the source of the pReference pointing to
	 * pTargetObject
	 * 
	 * @param pReference
	 * @param pTargetObject
	 * @return
	 *
	 */
	public ADao             getSourceObject(Class<? extends AReferenceType> pReference, ADao pTargetObject);
	
	/**
	 *
	 *  get the ADao subclass from which pReferenceType is pointing to s
	 *  target object
	 *
	 * @param pReferenceType
	 * @return
	 *
	 */
	public Class<? extends ADao> getSourceObjectType(Class<? extends AReferenceType> pReferenceType);
	
	/**
	 *
	 * get the list of ADao instances that are pointing to pTargetObject with
	 * a reference of type pReference
	 *
	 * @param pReference
	 * @param pTargetObject
	 * @return
	 *
	 */
	public <T extends ADao> List<T> getSourceObjectList(Class<? extends AReferenceType> pReference, ADao pTargetObject);

	/**
	 * create a reference of type pReferenceType pointing from pSourceObject
	 * to pTargetObject
	 *
	 * @param pReferenceType
	 * @param pSourceObject
	 * @param pTargetObject
	 *
	 */
	public void createReference(Class<? extends AReferenceType> pReferenceType, ADao pSourceObject,	ADao pTargetObject);

	/**
	 * delete a reference of type pReferenceType pointing from pSourceObject
	 * to pTargetObject
	 *
	 * @param pReferenceType
	 * @param pSourceObject
	 * @param pTargetObject
	 *
	 */
	public void deleteReference(Class<? extends AReferenceType> pReferenceType, ADao pSourceObject,	ADao pTargetObject);

	/**
	 * 
	 * get all loaded subclasses of ADao
	 *
	 * @return
	 *
	 */
	public Vector<Class<? extends ADao>> getObjectTypeList();

	/**
	 * 
	 * create a instance of the subclass of ADao with pName.
	 * this method should not be used directly by any application
	 * but is called internally by the framework. Use the constructor
	 * of pType instead which will implicitely call this method
	 *
	 * @param pType
	 * @param pName
	 * @return 
	 *
	 */
	public ADao createInstance(Class<? extends ADao> pType, String pName);

	/**
	 * get an instance of pType with name pName.
	 * 
	 * @param pType
	 * @param pName
	 * @return
	 *
	 */
	public <T extends ADao> T getInstance(Class<? extends ADao> pType, String pName);

	/**
	 *
	 * get the Cardinality of pReferenceType
	 *
	 * @param pReferenceType
	 * @return
	 *
	 */
	public Cardinality getCardinality(Class<? extends AReferenceType> pReferenceType);

	/**
	 *
	 * @param pTypeName
	 * @return
	 *
	 */
	public Class<? extends ADao> getDaoClassFromTypeName(String pTypeName);

	/**
	 * 
	 * export data of pCategory to file pExportFile
	 *
	 * @param pCategory
	 * @param pExportFile
	 * @throws IOException
	 *
	 */
	public void exportData(Category pCategory, File pExportFile) throws IOException;
	/**
	 * import data contained in pImportFile.
	 *
	 * @param pImportFile
	 * @throws IOException
	 *
	 */
	public void importData(File pImportFile) throws IOException;

	/**
	 *
	 * @param pPrintWriter
	 *
	 */
	public void dumpPersistency(PrintWriter pPrintWriter);

}
