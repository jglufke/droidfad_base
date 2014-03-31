/**
 * 
 */
package com.droidfad.persistency;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

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

 *
 * OpenfilesWatch takes care that only a predefined number of persistency
 * files are open. If the the threshold of number of open files is exceeded
 * a number of open files are closed.
 */
class OpenFilesWatch {

	/**
	 * key   - timestamp
	 * value - set of persfiles that were used at the respective timestamp
	 */
	private static TreeMap<Long, HashSet<PersFiles>> openFilesMap = new TreeMap<Long, HashSet<PersFiles>>(); 

	/**
	 * key   - Persfiles instance
	 * value - timestamp when it has been used the last time
	 */
	private static HashMap<PersFiles, Long> timestampMap = new HashMap<PersFiles, Long>(); 

	/**
	 * number of open files 
	 */
	private static int openFilesCount = 0;

	/**
	 * number of PersFiles instances that may be open at the same time
	 */
	private static final int MAX_OPEN_FILE_COUNT = 30;

	private static final String LOGTAG = OpenFilesWatch.class.getSimpleName();

	/**
	 *
	 * @param pPersFiles 
	 *
	 */
	public static void touch(PersFiles pPersFiles) {
		if(pPersFiles == null) {
			throw new IllegalArgumentException("parameter pPersFiles must not be null");
		}

		/**
		 * remove an potentially existing entry from the stack.
		 * yet
		 */
		boolean lIsNewEntry = removeFromStack(pPersFiles);
		/**
		 * add the entry to the stack
		 */
		addToStack(pPersFiles, lIsNewEntry);
		/**
		 * and cut the stack to the max number of allowed containedInstances
		 */
		cutStackAndCloseFiles();
	}

	/**
	 *
	 * returns true if pPersFile is a new entry
	 * @param pPersFiles
	 *
	 */
	private static boolean removeFromStack(PersFiles pPersFiles) {
		boolean lIsNewEntry = true;
		Long    lTimestamp  = timestampMap.remove(pPersFiles);
		if(lTimestamp != null) {
			/**
			 * a timestamp exists for pPersfiles which indicates that
			 * pPersFile is contained in the stack
			 */
			HashSet<PersFiles> lPersFilesSet = openFilesMap.get(lTimestamp);
			if(lPersFilesSet != null) {
				/**
				 * should not be null, but ad dit to make it more robust
				 */
				lIsNewEntry = !lPersFilesSet.remove(pPersFiles);
				if(lPersFilesSet.isEmpty()) {
					/**
					 * if the set is empty remove it
					 */
					openFilesMap.remove(lTimestamp);
				}
			}
		}
		return lIsNewEntry;
	}

	/**
	 *
	 * @param pPersFiles
	 * @param pIsNewEntry 
	 *
	 */
	private static void addToStack(PersFiles pPersFiles, boolean pIsNewEntry) {
		if(pIsNewEntry) {
			openFilesCount++;
		}
		Long lTimestamp = System.currentTimeMillis();
		/**
		 * add to the persFilesMap
		 */
		HashSet<PersFiles> lOpenFilesSet = openFilesMap.get(lTimestamp);
		if(lOpenFilesSet == null) {
			lOpenFilesSet = new HashSet<PersFiles>();
			openFilesMap.put(lTimestamp, lOpenFilesSet);
		}
		lOpenFilesSet.add(pPersFiles);
		timestampMap.put(pPersFiles, -lTimestamp);
	}

	/**
	 *
	 *
	 */
	private static void cutStackAndCloseFiles() {
		while(openFilesCount > MAX_OPEN_FILE_COUNT && !openFilesMap.isEmpty()) {
			Long               lLastTimeStamp    = openFilesMap.lastKey();
			HashSet<PersFiles> lLastFileEntrySet = openFilesMap.get(lLastTimeStamp);
			while(openFilesCount > MAX_OPEN_FILE_COUNT && !lLastFileEntrySet.isEmpty()) {
				PersFiles lPersFiles = lLastFileEntrySet.iterator().next();
				lLastFileEntrySet.remove(lPersFiles);
				close(lPersFiles);
				openFilesCount--;
			}
		}
	}

	/**
	 *
	 * @param pPersFiles
	 *
	 */
	private static void close(PersFiles pPersFiles) {
		try {
			pPersFiles.attributeFile.close();
			pPersFiles.attributeFpInfo.closeRandomAccessFile();
			pPersFiles.instanceFpInfo.closeRandomAccessFile();
		} catch(IOException e) {
			File lParentDir = new File(pPersFiles.attributeFile.getFilename()).getParentFile();
			LogWrapper.e(LOGTAG, "could close pPersFiles at:" + lParentDir.getAbsolutePath());
		}
	}
	
	protected static void reset() {
		openFilesMap   = new TreeMap<Long, HashSet<PersFiles>>();
		for(PersFiles lPersFiles : timestampMap.keySet()) {
			close(lPersFiles);
		}
		timestampMap   = new HashMap<PersFiles, Long>();
		openFilesCount = 0;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		for(HashSet<PersFiles> lPersFilesSet : openFilesMap.values()) {
			for(PersFiles lPersFiles : lPersFilesSet) {
				close(lPersFiles);
			}
		}
		super.finalize();
	}
}
