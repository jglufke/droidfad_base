package com.droidfad.classloading;

import java.util.HashSet;

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
 * -----------------------------------------------------------------------<br>--
 * with the help of the ClazzFinder class it is possible for an 
 * application to determine which classes are contained in the 
 * classpath or which jar files contain which classes etc.
 * A very helpful class if an plugin environment has to be implemented.
 * The naming convention for the methods is as follows. 
 * 
 */
public class ClazzFinder {

	public static final boolean ISDEBUG = true;

	public static final String LOGTAG = ClazzFinder.class.getSimpleName();

	private static HashSet<ClazzDescriptor> classEntries = null;

	private static IClazzFinder clazzFinderImpl;

	/**
	 * 
	 *
	 *
	 */
	public ClazzFinder() {}
	public static void setClazzFinder(IClazzFinder pClazzFinder) {
		clazzFinderImpl = pClazzFinder;
	}

	/**
	 * 
	 * ************************************************************<br>
	 *
	 * find all the classes in the System.properties classpath 
	 * "java.class.path"
	 *
	 * @author glufkeje
	 * @return
	 * 
	 * <br>************************************************************<br>
	 */
	public static HashSet<ClazzDescriptor> findClasses() {

		synchronized (ClazzFinder.class) {
			if(classEntries == null) {
				classEntries = clazzFinderImpl.findClasses();
			}
		}
		return classEntries;
	}
	
	/**
	 * ********************************************<br>
	 *
	 * @param pParentClass
	 * @return
	 *
	 * ********************************************<br>
	 */
	public static <T> HashSet<Class<? extends T>> findSubclasses(Class<?> pParentClass) {
		if(pParentClass == null) {
			throw new IllegalArgumentException("parameter pClass must not be null");
		}
		return clazzFinderImpl.findSubclasses(pParentClass);
	}
}
