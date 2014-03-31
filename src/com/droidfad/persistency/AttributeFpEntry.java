/**
 * 
 */
package com.droidfad.persistency;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

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
 * The format of a single instance entry is
 * [valid][fpInMyPersFile per attribute]
 *  
 */
class AttributeFpEntry implements IFpEntry<Long> {

	/**
	 * The fpInMyPersFile where this is located in the persistency file.
	 * This is also the fpInMyPersFile which is used by instanceFpInfo to locate the correct entry
	 */
	protected long   fpInMyPersFile;
	protected long[] attributeFps;

	public AttributeFpEntry(int pAttributeCount) {
		attributeFps = new long[pAttributeCount];
		for(int i=0;i<pAttributeCount; i++) {
			attributeFps[i] = -1;
		}
	}

	/* (non-Javadoc)
	 * @see com.droidfad.persistency.IFpEntry#getId()
	 */
	@Override
	public Long getId() {
		return fpInMyPersFile;
	}

	@Override
	public long getFpInMyPersFile() {
		return fpInMyPersFile;
	}
	
	public long getAttributeFp(int pIndex) {
		return attributeFps[pIndex];
	}
	public void setAttributeFp(int pIndex, long pAttributeFp) {
		attributeFps[pIndex] = pAttributeFp;
	}

	public long read(long pFp, DataInputStream pDIS, byte[] pBuf) throws IOException {
		fpInMyPersFile      = pFp;
		/**
		 * read the valid byte which is not used for this but it is contained because
		 * of super class
		 */
		pDIS.read();
		for(int i=0;i<attributeFps.length; i++) {
			attributeFps[i] = pDIS.readLong();
		}
		return pFp + 1 + attributeFps.length*8;
	}

	/* (non-Javadoc)
	 * @see com.droidfad.persistency.IFpEntry#writeToFile(java.io.RandomAccessFile)
	 */
	@Override
	public void writeToFile(RandomAccessFile pRandomAccessFile) throws IOException {
		pRandomAccessFile.write(1);
		for(int i=0;i<attributeFps.length; i++) {
			pRandomAccessFile.writeLong(attributeFps[i]);
		}
	}

	public String toString() {
		StringBuilder lBuilder = new StringBuilder(50);
		lBuilder.append(getClass().getSimpleName());
		lBuilder.append(':');
		lBuilder.append(fpInMyPersFile);
		lBuilder.append(':');
		for(long lFp : attributeFps) {
			lBuilder.append(lFp);
			lBuilder.append(',');
		}
		return lBuilder.toString();
	}

	/* (non-Javadoc)
	 * @see com.droidfad.persistency.IFpEntry#isValid()
	 */
	@Override
	public boolean isValid() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.droidfad.persistency.IFpEntry#setValid(boolean)
	 */
	@Override
	public void setValid(boolean pB) {
	}
}
