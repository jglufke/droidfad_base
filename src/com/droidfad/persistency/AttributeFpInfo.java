/**
 * 
 */
package com.droidfad.persistency;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.droidfad.util.FileUtil;
import com.droidfad.util.LogWrapper;

/**
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com
 *
 *   Licensed under the DROIDFAD license (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.droidfad.com/html/license/license.htm
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 * This class maps the instance and attribute index to a filepointer in 
 * the attributes file which contains the actual data. The order of 
 * the file pointer for one entry is defined by the alphabetical sorted
 * list of attributes of a Dao subclass. This order is managed by the 
 * class that uses AttributeFpInfo. The current structure is stored
 * in the [TypeName]_Info.bin file. If this class realizes that the 
 * structure of the sub class changed the changed/removed attributes can
 * be retrieved by the method "getChangedAttributeNameList"
 * The format of a single instance entry is
 * [valid][fpInMyPersFile per attribute]
 *
 */
class AttributeFpInfo extends AFpInfo<Long, AttributeFpEntry>{

	private static final String LOGTAG = AttributeFpInfo.class.getSimpleName();

	/**
	 * 
	 * ************************************************************<br>
	 *
	 * @param pFpFile
	 *
	 * ************************************************************<br>
	 */
	protected AttributeFpInfo(File pFpFile, int pAttributeCount) {
		super(pFpFile, pAttributeCount);
	}

	/* (non-Javadoc)
	 * @see com.droidfad.persistency.AFpInfo#createNewFpEntry()
	 */
	@Override
	protected AttributeFpEntry createNewFpEntry() {
		return new AttributeFpEntry(typeAttributeCount);
	}

	/**
	 * 
	 * if an instance with this fpInMyPersFile is contained in the file 
	 * the method returns the fpInMyPersFile of the attribute. If the instance
	 * is not contained -1 is returned
	 * 
	 * @param pInstanceFp
	 * @param pAttributeIndex
	 * @return fpInMyPersFile of the attribute or -1 if instance does not exist or is not valid
	 *
	 */
	protected long getAttributeFp(long pInstanceFp, int pAttributeIndex) {
		long lFp = -1;
		if(pAttributeIndex < typeAttributeCount) {
			AttributeFpEntry lEntry = fpMap.get(pInstanceFp);
			if(lEntry != null) {
				lFp = lEntry.getAttributeFp(pAttributeIndex);
			} else if(pInstanceFp >= 0) {
				throw new IllegalArgumentException("could not get an attributeFp for instanceFp:" + pInstanceFp + "\n" + this);
			}
		} else {
			throw new IllegalArgumentException("parameter attribute index:" + pAttributeIndex + " exceeds attribute count:" + typeAttributeCount);
		}
		return lFp;
	}

	/**
	 * if pInstanceFp is >= 0 
	 *    the method tries to locate an attributeFpEntry for the given
	 *    instanceFp.
	 *    if the instance exists. 
	 *       The attributeFpEntry is set to valid
	 *    if the instance does not exist
	 *    	 A new entry is appended to the file and the respective fpInMyPersFile is returned
	 * if pInstanceFp is < 0
	 *    a new entry is appended to the file and the respective fpInMyPersFile is returned
	 *
	 * @param pInstanceFp
	 * @param pAttributeIndex
	 * @param pNewAttributeFp
	 * @return
	 * @throws IOException 
	 *
	 */
	protected long setAttributeFp(long pInstanceFp, int pAttributeIndex, long pNewAttributeFp) throws IOException {

		long             lNewInstanceFp  = pInstanceFp;
		boolean          lWriteToFile    = false;
		AttributeFpEntry lAttributeEntry = null;
		if(pInstanceFp >= 0) {
			lAttributeEntry = fpMap.get(pInstanceFp);
			if(lAttributeEntry != null) {
				
				if(lAttributeEntry.fpInMyPersFile != pInstanceFp) {
					throw new Error("fpMap inconsistent! instanceFp must be equal to attributeEntry.fp ifp:" + pInstanceFp + " aefp:" + lAttributeEntry.fpInMyPersFile);
				}				
				long lOldAttributeFp = lAttributeEntry.getAttributeFp(pAttributeIndex); 
				if(pNewAttributeFp != lOldAttributeFp) {
					lAttributeEntry.setAttributeFp(pAttributeIndex, pNewAttributeFp);
					lWriteToFile = true;
				}
			} else {
				pInstanceFp = -1;
			}
		}
		if(pInstanceFp < 0) {
			lAttributeEntry = new AttributeFpEntry(typeAttributeCount);
			lAttributeEntry.setAttributeFp(pAttributeIndex, pNewAttributeFp);
			/**
			 * append the entry to the end of the existing file
			 */
			lNewInstanceFp       = fpFile.length();
			lAttributeEntry.fpInMyPersFile   = lNewInstanceFp;
			lWriteToFile         = true;
			fpMap.put(lNewInstanceFp, lAttributeEntry);
		}
		if(lWriteToFile) {
			getRandomAccessFile().seek(lNewInstanceFp);
			lAttributeEntry.writeToFile(getRandomAccessFile());
		}
		return lNewInstanceFp;
	}

	/**
	 * 
	 *
	 * @param pInstanceFpInfo
	 * @throws IOException
	 *
	 */
	protected void defragment(InstanceFpInfo pInstanceFpInfo) throws IOException {

		/**
		 * create the backup file
		 */
		String          lDefragFilename = fpFile.getAbsolutePath() + ".df";
		AttributeFpInfo lDefragFile     = new AttributeFpInfo(new File(lDefragFilename), typeAttributeCount);		
		boolean         lToBeBackCopied = false;
		/**
		 * iterate over all instances in pInstanceFpInfo 
		 */
		for(Map.Entry<String, InstanceFpEntry> lInstanceMapEntry : pInstanceFpInfo.fpMap.entrySet()) {
			String          lInstanceName    = lInstanceMapEntry.getKey();
			InstanceFpEntry lInstancefpEntry = lInstanceMapEntry.getValue();
			if(lInstancefpEntry.isValid()) {
				lToBeBackCopied       = true;
				long   lInstanceFp    = lInstancefpEntry.instanceFp;
				/**
				 * get the respective attributeFpEntry and copy it to the new file
				 */
				AttributeFpEntry lAttributeFpEntry = fpMap.get(lInstanceFp);
				if(lAttributeFpEntry == null) {
					lAttributeFpEntry = new AttributeFpEntry(typeAttributeCount);
					LogWrapper.e(LOGTAG, "problem fixed: could not find an attribute entry for:" + lInstanceName);
				}
				long lNewInstanceFp = -1;
				for(int i=0; i<typeAttributeCount; i++) {
					lNewInstanceFp = lDefragFile.setAttributeFp(lNewInstanceFp, i, lAttributeFpEntry.getAttributeFp(i));
				}
				/**
				 * the instanceFp changed, so set it to the new value in instanceFpInfo
				 */
				pInstanceFpInfo.setInstanceFp(lInstanceName, lNewInstanceFp);
			}
		}

		closeRandomAccessFile();
		lDefragFile.closeRandomAccessFile();
		if(lToBeBackCopied) {
			if(!lDefragFile.fpFile.renameTo(fpFile)) {
				FileUtil.copy(lDefragFile.fpFile, fpFile);
			}
		}
		if(lDefragFile.fpFile.exists()) {
			lDefragFile.fpFile.delete();
		}
		loadFpFile();
	}	
}
