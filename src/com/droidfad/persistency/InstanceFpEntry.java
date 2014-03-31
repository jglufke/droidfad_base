/**
 * 
 */
package com.droidfad.persistency;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

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
 */
class InstanceFpEntry implements IFpEntry<String> {
	
	long    fpInMyPersFile; // the file pointer inside the persistency file of this
	String  instanceName;   // the name of the instance this is for
	boolean isValid;        // defines if the entry is valid
	long    instanceFp;     // the pointer to the position in the attributefp file

	public InstanceFpEntry() {}

	public InstanceFpEntry(int pFp, boolean pIsValid, Long pIndexInInstanceDescriptionFile, String pInstanceName) {
		fpInMyPersFile = pFp;
		isValid        = pIsValid;
		instanceFp     = pIndexInInstanceDescriptionFile;
		instanceName   = pInstanceName; 
	}

	public String getId() {
		return instanceName;
	}
	
	public Long getTargetFp() {
		return instanceFp;
	}
	public long read(long pFp, DataInputStream pDIS, byte[] pBuf) throws IOException {

		fpInMyPersFile = pFp;
		
		isValid        = pDIS.readByte() > 0;
		instanceFp     = pDIS.readLong();
		short lLength  = pDIS.readShort();
		if(lLength <0) {
			throw new IOException("length of instance name is < 0");
		}
		int         lReadByteCount = pDIS.read(pBuf, 0, lLength);
		if(lLength != lReadByteCount) {
			throw new IOException("inconsistent read");
		}
		instanceName   = new String(pBuf, 0, lReadByteCount);			

		pFp += 11 + lReadByteCount;
		return pFp;
	}

	/**
	 * ************************************************************<br>
	 *
	 * @param pRandomAccessFile
	 *
	 * ************************************************************<br>
	 */
	public void writeToFile(RandomAccessFile pRandomAccessFile) throws IOException {
		pRandomAccessFile.writeByte(isValid ? 1 : 0);
		pRandomAccessFile.writeLong(instanceFp);
		int lLength = instanceName.length();
		if(lLength > Short.MAX_VALUE){
			throw new IllegalArgumentException("instancename must not be longer than:" + Short.MAX_VALUE);
		}
		pRandomAccessFile.writeShort(lLength);
		pRandomAccessFile.write(instanceName.getBytes());
	}

	public boolean isValid() {
		return isValid;
	}
	public void setValid(boolean pB) {
		isValid = pB;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder(50);
		lBuilder.append(getClass().getSimpleName());
		lBuilder.append(':');
		lBuilder.append(instanceName);
		lBuilder.append(',');
		lBuilder.append(isValid);
		lBuilder.append(',');
		lBuilder.append(fpInMyPersFile);
		lBuilder.append(',');
		lBuilder.append(instanceFp);
		
		return lBuilder.toString();
	}

	/* (non-Javadoc)
	 * @see com.droidfad.persistency.IFpEntry#getFpInMyPersFile()
	 */
	@Override
	public long getFpInMyPersFile() {
		return fpInMyPersFile;
	}
}

