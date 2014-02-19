/**
 * 
 */
package com.droidfad.persistency;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.droidfad.util.LogWrapper;

/**
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com

   Licensed under the DROIDFAD license (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.droidfad.com/html/license/license.htm

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 *
 */
abstract class AFpInfo<K, V extends IFpEntry> {

	protected File fpFile;

	protected long             validInstanceCount = 0;    
	private   RandomAccessFile randomAccessFile   = null;
	protected int              typeAttributeCount = -1;

	protected HashMap<K, V> fpMap = new HashMap<K, V>();

	protected AFpInfo(File pFpFile) {
		this(pFpFile, -1);
		
	}
	protected AFpInfo(File pFpFile, int pAttributeCount) {
		if(pFpFile == null) {
			throw new IllegalArgumentException("parameter pInstanceIndexFile must not be null");
		}
		if(pFpFile.isDirectory()) {
			throw new IllegalArgumentException("parameter pInstanceIndexFile must not be an existing directory");
		}
		pFpFile.getParentFile().mkdirs();
		fpFile         = pFpFile;
		typeAttributeCount = pAttributeCount;
		loadFpFile();
	}

	protected double getSizeToValid() {
		double lSize  = fpMap.size();
		double lCount = validInstanceCount;
		return (lSize / lCount);
	}

	/**
	 * ************************************************************<br>
	 *
	 *
	 * ************************************************************<br>
	 */
	protected void loadFpFile() {
		if(fpFile.exists()) {
			fpMap = new HashMap<K, V>();
			try {
				byte[]          lBuf      = new byte[Short.MAX_VALUE]; 
				DataInputStream lDIS      = new DataInputStream(new FileInputStream(fpFile));
				long            lFp       = 0;
				validInstanceCount        = 0;
				while(lDIS.available() > 0) {

					V  lNewEntry      = createNewFpEntry();
					lFp               = lNewEntry.read(lFp, lDIS, lBuf);
					fpMap.put((K) lNewEntry.getId(), lNewEntry);
					if(lNewEntry.isValid()) {
						validInstanceCount++;
					}
				}
				lDIS.close();
				
			} catch(IOException e) {
				fpMap = new HashMap<K, V>();
				fpFile.delete();
				throw new RuntimeException(e);
			}
		}
	}

	abstract protected V createNewFpEntry();

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		if(randomAccessFile != null) {
			randomAccessFile.close();
		}
	}

	/**
	 *
	 * @return
	 *
	 */
	protected long getSize() {
		return fpFile != null ? fpFile.length() : -1;
	}

	protected synchronized void removeFp(K pId)  throws IOException {

		V lEntry = fpMap.get(pId);
		/**
		 * only do something if the entry is valid and does exist
		 */
		if(lEntry != null && lEntry.isValid()) {

			lEntry.setValid(false);
			if(fpFile.exists()) {
				long             lFp   = lEntry.getFpInMyPersFile();
				RandomAccessFile lFile = getRandomAccessFile(); 
				LogWrapper.i(getClass().getSimpleName(), "removeFp:" + fpFile.getParentFile().getName() + "." + fpFile.getName() + "." + lFp);
				lFile.seek(lFp);
				lFile.writeByte(0);
			}
			validInstanceCount--;
		}
	}
	
	public void closeRandomAccessFile() throws IOException {
		if(randomAccessFile != null) {
			randomAccessFile.close();
		}
		randomAccessFile = null;
	}
	
	protected RandomAccessFile getRandomAccessFile() throws IOException {
		if(randomAccessFile == null) {
			randomAccessFile = new RandomAccessFile(fpFile, "rws");
		}
		return randomAccessFile;
	}
	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder(50);
		lBuilder.append(getClass().getSimpleName());
		lBuilder.append(':');
		lBuilder.append('\n');
		TreeMap<K, V> lSortedMap = new TreeMap<K, V>(fpMap); 
		for(Entry<K, V> lEntry : lSortedMap.entrySet()) {
			lBuilder.append('\t');
			lBuilder.append(lEntry.getKey());
			lBuilder.append("\t");
			lBuilder.append("\t");
			lBuilder.append(lEntry.getValue().isValid());
			lBuilder.append("\t");
			lBuilder.append("\t");
			lBuilder.append(lEntry.getValue().toString());
			lBuilder.append('\n');
		}
		return lBuilder.toString();
	}
}
