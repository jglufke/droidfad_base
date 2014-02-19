/**
 * 
 */
package com.droidfad.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import com.droidfad.classloading.ClazzFinder;
import com.droidfad.data.ADao.AttributesEnum;
import com.droidfad.util.LogWrapper;
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
 *   limitations under the License.<br><br>
 * -----------------------------------------------------------------------<br><br>
 * this class checks during runtime for the correct
 * implementation of all DAO classes and throws an
 * exception if errors are found
 *
 */
public class DAOImplementationCheck {

	private static final String LOGTAG = DAOImplementationCheck.class.getSimpleName();

	private ReflectionUtil reflectionUtil = new ReflectionUtil(); 

	public DAOImplementationCheck() {
		checkModel();
	}
	
	/**
	 * ********************************************<br>
	 *
	 * @param pData
	 *
	 * ********************************************<br>
	 */
	private void checkModel() {
		/**
		 * get all the dao types
		 */
		Vector<String> lErrorMessageList = new Vector<String>();

		HashSet<Class<? extends ADao>> lDaoSubClassSet = ClazzFinder.findSubclasses(ADao.class);
		/**
		 * remove all not relevant classes
		 */
		for(Class<? extends ADao> lDaoSubClass : new Vector<Class<? extends ADao>>(lDaoSubClassSet)) {
			if((lDaoSubClass.getModifiers() & Modifier.ABSTRACT) != 0) {
				lDaoSubClassSet.remove(lDaoSubClass);
			}
		}

		/**
		 * check for unique single names of ADao subclasses 
		 */
		lErrorMessageList.addAll(checkForUniqueSimpleName(lDaoSubClassSet));

		for(Class<? extends ADao> lDaoSubClass : lDaoSubClassSet) {
			lErrorMessageList.addAll(checkClassImplementation(lDaoSubClass));
		}

		if(!lErrorMessageList.isEmpty()) {
			StringBuilder lBuilder = new StringBuilder();
			for(String lErrorMessage: lErrorMessageList) {
				LogWrapper.e(LOGTAG, lErrorMessage);
				lBuilder.append(lErrorMessage);
				lBuilder.append('\n');
			}
			LogWrapper.e(LOGTAG, lBuilder.toString());
		}
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pDaoSubClass
	 *
	 * ********************************************<br>
	 */
	private Vector<String> checkClassImplementation(Class<? extends ADao> pDaoSubClass) {

		Vector<String> lErrorMessageList = new Vector<String>();

		lErrorMessageList.addAll(checkConstructor(pDaoSubClass));
		if(!AReferenceType.class.isAssignableFrom(pDaoSubClass)) {
			/**
			 * reference types do not have to implement the get instances methods
			 */
			lErrorMessageList.addAll(checkGetInstanceMethods(pDaoSubClass));
		}
		lErrorMessageList.addAll(checkAttributesEnumExists(pDaoSubClass));

		return lErrorMessageList;
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pDaoSubClassSet
	 * @return
	 *
	 * ********************************************<br>
	 */
	private Collection<? extends String> checkForUniqueSimpleName(HashSet<Class<? extends ADao>> pDaoSubClassSet) {
		Vector<String> lErrorMessageList = new Vector<String>();
		return lErrorMessageList;
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pDaoSubClass
	 * @return
	 *
	 * ********************************************<br>
	 */
	private Collection<? extends String> checkAttributesEnumExists(Class<? extends ADao> pDaoSubClass) {
		Vector<String> lErrorMessageList = new Vector<String>();
		boolean        lFound            = false;
		for(Field lField : pDaoSubClass.getDeclaredFields()) {
			if("Attributes".equals(lField.getName())) {
				Class<?> lFieldType = lField.getType();
				if(Enum.class.isAssignableFrom(lFieldType)) {
					if(AttributesEnum.class.isAssignableFrom(lFieldType)) {
						/**
						 * check that the field is public
						 */
						if((lField.getModifiers() & Modifier.PUBLIC) != 0) {
							HashSet<String> lPersAttrSet = new HashSet<String>(reflectionUtil.getPersistentAttributeNames(pDaoSubClass, null));
							for(Field lEnumValue : lFieldType.getDeclaredFields()) {
								String lEnumValueName = lEnumValue.getName(); 
								lPersAttrSet.remove(lEnumValue);
							}
							if(!lPersAttrSet.isEmpty()) {
								lErrorMessageList.add("The enum 'Attributes' does not contain the following persistent attributes:" + lPersAttrSet);
							}
						} else {
							lErrorMessageList.add("The enum 'Attributes' is not public");
						}
						/**
						 * check that the enum contains the names of all persistent attributes
						 */
					} else {
						lErrorMessageList.add("The enum 'Attributes' does not implement:"+AttributesEnum.class.getName());
					}
				} else {
					lErrorMessageList.add("The field 'Attributes' is not an enum");
				}
				break;
			}
		}
		if(!lErrorMessageList.isEmpty()) {
			lErrorMessageList.add("ADao subclass:"+pDaoSubClass.getName()+" has to have 'public enum Attributes implements AttributesEnum {....};'\n");
		}
		return lErrorMessageList;
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pDaoSubClass
	 * @return
	 *
	 * ********************************************<br>
	 */
	private Collection<? extends String> checkGetInstanceMethods(Class<? extends ADao> pDaoSubClass) {
		Vector<String> lErrorMessageList = new Vector<String>();
		boolean        lFound            = false;
		/**
		 * check for ADao getInstance(String)
		 */
		Method[] lMethodArray = pDaoSubClass.getMethods(); 
		for(Method lMethod : lMethodArray) {
			if("getInstance".equals(lMethod.getName())) {
				Class[] lParameters = lMethod.getParameterTypes();
				if(lParameters.length==1 && String.class.equals(lParameters[0])) {
					Class<?> lReturnType = lMethod.getReturnType();
					if(pDaoSubClass.equals(lReturnType)) {
						/**
						 * check for static
						 */
						if((lMethod.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0) {
							lFound = true;
							break;
						} else {
							lErrorMessageList.add("method " + lMethod + " is not static and public");
						}
					} else {
						lErrorMessageList.add("method " + lMethod + " returnType is not " + pDaoSubClass.getName());
					}
				}
			}
		}
		if(!lFound) {
			lErrorMessageList.add("ADao subclass:"+pDaoSubClass.getName()+" has to implement static method getInstance with String as single parameter and "+pDaoSubClass.getSimpleName()+".class as return type\n");
		}

		lFound = false;
		for(Method lMethod : lMethodArray) {
			if("getInstances".equals(lMethod.getName())) {
				Class[] lParameters = lMethod.getParameterTypes();
				if(lParameters.length==0) {
					Class<?> lReturnType = lMethod.getReturnType();
					if(List.class.isAssignableFrom(lReturnType)) {
						/**
						 * check for static
						 */
						if((lMethod.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0) {
							lFound = true;
							break;
						}else {
							lErrorMessageList.add("method " + lMethod + " is not static and public");
						}
					} else {
						lErrorMessageList.add("method " + lMethod + " returnType is not ListReferenceEditor<" + pDaoSubClass.getSimpleName() +">");
					}
				}
			}
		}
		if(!lFound) {
			lErrorMessageList.add("ADao subclass:"+pDaoSubClass.getName()+" has to implement static method getInstances with String as single parameter\n");
		}
		return lErrorMessageList;
	}

	/**
	 * ********************************************<br>
	 *
	 * @param pDaoSubClass
	 * @return
	 *
	 * ********************************************<br>
	 */
	private Collection<? extends String> checkConstructor(Class<? extends ADao> pDaoSubClass) {
		Vector<String> lErrorMessageList = new Vector<String>();
		boolean        lFound            = false;
		if(        AReferenceType.class.isAssignableFrom(pDaoSubClass)
				|| _Reference.class.isAssignableFrom(pDaoSubClass)) {
			/**
			 * _Reference subclasses do not have to implement 
			 */
			lFound = true;
		} else {
		for(Constructor<?> lConstructor : pDaoSubClass.getDeclaredConstructors()) {
			Class<?>[] lParameters = lConstructor.getParameterTypes();
			if(lParameters.length == 1 && lParameters[0].equals(String.class)) {
				if((lConstructor.getModifiers() & Modifier.PUBLIC) != 0) {
					lFound = true;
					break;
				} else {
					lErrorMessageList.add("constructor has to be public:" + lConstructor);
					break;
				}
			}
		}
		}
		if(!lFound) {
			lErrorMessageList.add("ADao subclass:"+pDaoSubClass.getName()+" has to implement public 'naming' constructor with String as single parameter\n");
		}
		return lErrorMessageList;
	}
}
