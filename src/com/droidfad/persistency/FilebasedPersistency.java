/**
 * 
 */
package com.droidfad.persistency;

import java.io.File;

import com.droidfad.data.ADao;

/**
 *
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
 * implementation of a file based persistency
 * 
 * - all not primitive attribute types are stored using ObjectStream
 * - structure of the types is contained in TypeInfoMap
 * 
 */
class FilebasedPersistency {
	
	private String rootPath = null;

	public final static String TYPESTRUCTFILE = "TypeStruct";
	
	public FilebasedPersistency(File pRootPath) {
		if(pRootPath == null) {
			throw new IllegalArgumentException("parameter pRootPath must not be null");
		}
		if(pRootPath.isFile()) {
			throw new IllegalArgumentException("parameter pRootPath must not be an existing file");
		}
		pRootPath.mkdirs();
		rootPath = pRootPath.getAbsolutePath();
	}
	
	public void init() {
		/**
		 * handle change in type structure -------------------------------------------
		 */
		/**
		 * get stored types from TypeInfoMap
		 */
		/**
		 * get respective classes from ClazzManager
		 */
		/**
		 * replace all instances with default values
		 */
		
		/**
		 * remove types which are not existing anymore from file system -----------------
		 */
		
	}
	
	public void setValue(Class<? extends ADao> pType, String pName, String pAttribute, Object pValue) {
		/**
		 * check if type is contained in type list
		 */
		
		/**
		 * check if instance is in internal cache
		 * - yes, do nothing
		 * - no, add instance to cache after appending instance to pers file 
		 */
		
		/**
		 * get the offset from the cache
		 */

		/**
		 * get the offset of attribute from InstanceEntryInfo
		 */
		
		/**
		 * get the lenght information of the attribute.
		 * - if length is exceeded by new entry append the complete instance
		 *   to the file and set old entry to not used
		 * - if length is ok write attribute at defined place, set the length info
		 *   if necessary
		 */
		
	}
	/**
	 * 
	 *
	 *
	 */
	public void defragment() {
		
	}
}
