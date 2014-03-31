/**
 * 
 */
package com.droidfad.persistency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import com.droidfad.util.FileUtil;

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
 * maps the instance name to an offset in the instance info file.
 * The entry format in the fpInMyPersFile file is
 * [byte:isValid][String.instanceName][long:fpOfInstanceInfo]
 *
 * This class is not thread safe !!
 *
 * TODO implement caching algorithm to keep lastt 1000 instances
 * in memory
 *
 *
 */
class InstanceFpInfo extends AFpInfo<String, InstanceFpEntry> {

	private static final String LOGTAG = InstanceFpInfo.class.getSimpleName();

	/**
	 * 
	 * ************************************************************<br>
	 *
	 * @param pFpFile
	 *
	 * ************************************************************<br>
	 */
	protected InstanceFpInfo(File pFpFile) {
		super(pFpFile);
	}

	/**
	 * ************************************************************<br>
	 *
	 * @param pInstanceName
	 * @param pInstanceFp
	 * @throws FileNotFoundException 
	 *
	 * ************************************************************<br>
	 */
	protected long setInstanceFp(String pInstanceName, Long pInstanceFp) throws IOException {

		long lNewFp = pInstanceFp;
		if(pInstanceName != null) {
			boolean         lWriteToFile = false;
			InstanceFpEntry lEntry       = fpMap.get(pInstanceName);
			if(lEntry != null) {
				if(!lEntry.isValid) {
					/**
					 * if the entry has not been valid before we have a new 
					 * valid instance now
					 */
					validInstanceCount++;
				}

				lEntry.isValid    = true;
				lWriteToFile      = (lEntry.instanceFp != pInstanceFp); 
				lEntry.instanceFp = pInstanceFp;
				
			} else {
				lEntry = new InstanceFpEntry(-1, true, pInstanceFp, pInstanceName);
				fpMap.put(pInstanceName, lEntry);
				lWriteToFile = true;
				/**
				 * because we have a real new instance the count for valid instances
				 * has to be incremented
				 */
				validInstanceCount++;
			}
			/**
			 * write lEntry to the fpFile
			 */
			if(lWriteToFile) {
				if(lEntry.fpInMyPersFile < 0) {
					lEntry.fpInMyPersFile = getRandomAccessFile().length();
				}
				getRandomAccessFile().seek(lEntry.fpInMyPersFile);
				lEntry.writeToFile(getRandomAccessFile());
			}
			lNewFp = lEntry.fpInMyPersFile;
		}
		return lNewFp;
	}

	/**
	 * 
	 * ************************************************************<br>
	 *
	 * returns the file pointer in the instance description file.
	 *
	 * @return
	 *
	 * ************************************************************<br>
	 */
	protected long getInstanceFp(String pInstanceName) {
		long   lInstanceFp = -1;
		if(pInstanceName != null) {
			InstanceFpEntry lEntry = fpMap.get(pInstanceName);
			if(lEntry != null && lEntry.isValid) {
				lInstanceFp = lEntry.instanceFp;
			}
		}
		return lInstanceFp;
	}

	protected InstanceFpEntry createNewFpEntry() {
		return new InstanceFpEntry();
	}

	protected void defragment() throws IOException {

		closeRandomAccessFile();
		boolean lToBeBackCopied = false;
		/**
		 * create the backup file
		 */
		String         lDefragFilename = this.fpFile.getAbsolutePath() + ".df";
		InstanceFpInfo lDefragFile     = new InstanceFpInfo(new File(lDefragFilename));		

		for(Map.Entry<String, InstanceFpEntry> lMapEntry : fpMap.entrySet()) {
			InstanceFpEntry lFpEntry = lMapEntry.getValue();
			if(lFpEntry.isValid()) {
				lToBeBackCopied = true;
				lDefragFile.setInstanceFp(lFpEntry.getId(), lFpEntry.getTargetFp());
			}
		}

		lDefragFile.closeRandomAccessFile();
		if(lToBeBackCopied) {
			if(!lDefragFile.fpFile.renameTo(this.fpFile)) {
				FileUtil.copy(lDefragFile.fpFile, this.fpFile);
			}
			loadFpFile();
		}
		if(lDefragFile.fpFile.exists()) {
			lDefragFile.fpFile.delete();
		}
	}

	/**
	 *
	 * @return
	 *
	 */
	public HashSet<String> getInstanceNameSet() {
		HashSet<String> lInstanceNameSet = new HashSet<String>(fpMap.keySet().size());
		for(Map.Entry<String, InstanceFpEntry> lEntry: fpMap.entrySet()) {
			if(lEntry.getValue().isValid) {
				lInstanceNameSet.add(lEntry.getKey());
			}
		}
		return lInstanceNameSet;
	}
	
}

