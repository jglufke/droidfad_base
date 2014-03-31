/**
 * 
 */
package com.droidfad.persistency;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.TreeMap;

import com.droidfad.util.FileUtil;
import com.droidfad.util.LogWrapper;

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
 * this class is responsible for the access to the actual data on the filesystem.
 * A special handling is implemented for attributes that have a dynamic length.
 * The length information is stored in a 4 byte integer that is preceeding the actual
 * entry. This length information describes the length of the possible ! length
 * if the attribute. This is not identical with the actual length of the attribute.
 * To avoid early defragmentation a defined overhead is kept for every attribute.
 * If a new write operation to a flexible length attribute exceeds the possible length
 * the attribute is written to the lostLength file.
 * The following handling is implemented for writeObject:
 * - serialize the object
 * - if attributeFp > 0
 *   - check length integer at attributeFp
 *   - if length >= serializedObject.length
 *     - write serialized object to attributeFp+4
 *   - else length < serializedObject.length
 *     - add lost bytes count to lostBytesFile
 *     - append length information to file
 *     - append serialized object + n spare bytes to file
 *     - return new attributeFp 
 */ 
class AttributeFile {

	private static final String LOGTAG = AttributeFile.class.getSimpleName();
	private File file;
	private RandomAccessFile randomAccessFile;

	private static final boolean ISDEBUG = false;

	static class FreeEntry {
		long fp = -1; // fpInMyPersFile in the attribute file
		long freeEntryFp = -1; // fpInMyPersFile in the file that persists the free entries
		int  size = -1; // size of the entry
	}

	private HashMap<Long, FreeEntry> freeEntryMap = null;
	File feFile;

	/**
	 * defines how much headroom is reserved for an attribute if it is a complex
	 * one
	 */
	private static final double SERIALIZED_HEADROOM  = 1.25;

	private static final byte[] DEFAULT_SPAREBYTES   = new byte[10240];

	public AttributeFile(File pAttributeFile) {
		if(pAttributeFile == null) {
			throw new IllegalArgumentException("parameter pAttributeFile must not be null");
		}
		file      = pAttributeFile;
		feFile    = new File(file.getAbsolutePath() + ".fe");
		if(!file.exists() && feFile.exists()) {
			feFile.delete();
		}
		loadFreeEntries();
	}

	public String getFilename() {
		return file.getAbsolutePath();
	}

	/**
	 *
	 *
	 */
	private void loadFreeEntries() {
		freeEntryMap = new HashMap<Long, AttributeFile.FreeEntry>();
		if(feFile.exists()) {
			try {
				DataInputStream lDIS = new DataInputStream(new FileInputStream(file.getAbsolutePath() + ".fe"));
				long            lFp  = 0;
				while(lDIS.available()>0) {
					long      lReadFp      = lDIS.readLong();
					int       lSize        = lDIS.readInt();
					/**
					 * if a value must not be used anymore it is indicated by 
					 * an fpInMyPersFile < 0
					 */
					if(lReadFp >= 0) {
						FreeEntry lFreeEntry   = new FreeEntry();
						lFreeEntry.fp          = lReadFp;
						lFreeEntry.size        = lSize;
						lFreeEntry.freeEntryFp = lFp;
						freeEntryMap.put(lFreeEntry.fp, lFreeEntry);
					}
					lFp += 8 + 4;
				}
				lDIS.close();
			} catch(IOException e) {
				LogWrapper.e(LOGTAG, "loadFreeEntries could not load free entries:" + file.getAbsolutePath());
			}
		}
	}

	/**
	 * 
	 * get the free entry which size is closest to pSize
	 * @param pRequiredSize
	 * @return
	 *
	 */
	private FreeEntry getFreeEntry(int pRequiredSize) {
		FreeEntry                   lFreeEntry      = null;
		TreeMap<Integer, FreeEntry> lSortedEntryMap = new TreeMap<Integer, AttributeFile.FreeEntry>();
		for(FreeEntry lEntry : freeEntryMap.values()) {
			if(lEntry.fp >= 0 && lEntry.size >= pRequiredSize) {
				int lDiff = pRequiredSize - lEntry.size;
				if(lDiff < 0) {
					lDiff = -lDiff;
				}
				lSortedEntryMap.put(lDiff, lEntry);
			}
		}
		if(!lSortedEntryMap.isEmpty()) {
			lFreeEntry = lSortedEntryMap.values().iterator().next();
		}
		return lFreeEntry;
	}
	/**
	 * remove the entry from the list of free entries and set the 
	 * respective negative value in the free entry file
	 *
	 * @param pEntry
	 *
	 */
	private void removeFreeEntry(long pFp) {
		FreeEntry lEntry = freeEntryMap.remove(pFp);
		if(lEntry != null) {
			try {
				RandomAccessFile lRAF = new RandomAccessFile(feFile, "rws");
				lRAF.seek(lEntry.freeEntryFp);
				lRAF.writeLong(-1);
				lRAF.close();
			} catch(IOException e) {
				LogWrapper.e(LOGTAG, "removeFreeEntry could not write to file:" + feFile.getAbsolutePath());
			}
		}
	}

	/**
	 * 
	 *
	 * @param pFp
	 * @param pSize
	 *
	 */
	private void createFreeEntry(long pFp, int pSize) {
		FreeEntry lEntry    = new FreeEntry();
		lEntry.fp           = pFp;
		lEntry.size         = pSize;
		lEntry.freeEntryFp  = feFile.length();
		try {
			RandomAccessFile lRAF = new RandomAccessFile(feFile, "rws");
			lEntry.freeEntryFp = lRAF.length();
			lRAF.seek(lEntry.freeEntryFp);
			lRAF.writeLong(pFp);
			lRAF.writeInt(pSize);
			freeEntryMap.put(lEntry.fp, lEntry);
			lRAF.close();
		} catch(IOException e) {
			LogWrapper.e(LOGTAG, "createFreeEntry could not write to file:" + feFile.getAbsolutePath());
		}
	}

	/**
	 * 
	 *
	 *
	 */
	protected void defragmentFreeEntriesFile() {
		try {

			DataInputStream  lDIS      = new DataInputStream(new FileInputStream(feFile));
			File             lOutfile  = new File(feFile.getAbsolutePath() + ".tmp");
			DataOutputStream lDOS      = new DataOutputStream(new FileOutputStream(lOutfile));
			while(lDIS.available() > 0) {
				long lFp   = lDIS.readLong();
				int  lSize = lDIS.readInt();
				if(lFp >= 0) {
					lDOS.writeLong(lFp);
					lDOS.writeInt(lSize);
				}
			}
			lDIS.close();
			lDOS.close();

			if(!lOutfile.renameTo(feFile)) {
				FileUtil.copy(lOutfile, feFile);
				lOutfile.delete();
			}

		} catch(IOException e) {
			LogWrapper.e(LOGTAG, "defragmentFreeEntriesFile file operation failed:" + e.getMessage());
		}
	}

	public void close() throws IOException {
		if(randomAccessFile != null) {
			randomAccessFile.close();
		}
		randomAccessFile = null;
	}

	private long checkFp(long pFp) throws IOException {
		if(pFp < 0) {
			pFp = getRandomAccessFile().length();
		}
		return pFp;
	}

	public long write(long pFp, byte pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().write(pVal);
		return pFp;
	}
	public long write(long pFp, byte[] pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().write(pVal.length);
		getRandomAccessFile().write(pVal);
		return pFp;
	}
	public long write(long pFp, char pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().writeChar(pVal);
		return pFp;
	}
	public long write(long pFp, short pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().writeShort(pVal);
		return pFp;
	}
	public long write(long pFp, int pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().writeInt(pVal);
		return pFp;
	}
	public long write(long pFp, long pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().writeLong(pVal);
		return pFp;
	}
	public long write(long pFp, float pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().writeFloat(pVal);
		return pFp;
	}
	public long write(long pFp, double pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().writeDouble(pVal);
		return pFp;
	}
	public long write(long pFp, boolean pVal) throws IOException {
		pFp = checkFp(pFp); 
		getRandomAccessFile().seek(pFp);
		getRandomAccessFile().write(pVal ? 1 : 0);
		return pFp;
	}

	/**
	 * 
	 * The following handling is implemented for writeObject:
	 * - serialize the object
	 * - if attributeFp > 0
	 *   - check length integer at attributeFp
	 *   - if length >= serializedObject.length
	 *     - write serialized object to attributeFp+4
	 *   - else length < serializedObject.length
	 *     - add lost bytes count to lostBytesFile
	 *     - append length information to file
	 *     - append serialized object + n spare bytes to file
	 *     - return new attributeFp 
	 * - else attributeFp <= 0
	 *   - append length information + overhead to file
	 *   - append serialized object + n spare bytes to file
	 *
	 * @param pAttributeFp
	 * @param pObject
	 * @return
	 *
	 */
	public long writeObject(String pType, String pName, String pAttributeName, 
			long pAttributeFp, Object pObject) throws IOException {


		long   lNewAttributeFp   = pAttributeFp;
		byte[] lSerializedObject = serializeObject(pObject);

		if(pAttributeFp >= 0) {
			if(ISDEBUG) System.out.print(" 1 ");
			/**
			 * - if attributeFp > 0
			 *   - check length integer at attributeFp
			 *   - if length >= serializedObject.length
			 *     - write serialized object to attributeFp+4
			 *   - else length < serializedObject.length
			 *     - create a new freeEntry for the old fpInMyPersFile location in the file
			 *     - try to get a freeEntry
			 *     - freeEntry == null
			 *       - append length information to file
			 *       - append serialized object + n spare bytes to file
			 *     - freeEntry != null
			 *       - write serialized object to free entry
			 *       - remove free entry
			 *     - return new attributeFp 
			 */
			getRandomAccessFile().seek(pAttributeFp);
			int  lLength = -1;
			if(getRandomAccessFile().length() >= 4) {
				lLength         = getRandomAccessFile().readInt();
			}
			if(lLength >= lSerializedObject.length) {
				if(ISDEBUG) System.out.print(" 2 len:" + lLength + " obj.len:" + lSerializedObject.length + " " + pAttributeFp + "-" + (pAttributeFp+4+lLength));
				writeObjectImpl(pAttributeFp, lLength, lSerializedObject);
			} else {

				if(ISDEBUG) System.out.print(" 3 ");
				createFreeEntry(pAttributeFp, lLength);
				FreeEntry lEntry = getFreeEntry(lSerializedObject.length);
				if(lEntry != null) {
					if(ISDEBUG) System.out.print(" 4 so.len:"+ lSerializedObject.length + " fe.len:" + lEntry.size);
					lNewAttributeFp = lEntry.fp;
					writeObjectImpl(lNewAttributeFp, lEntry.size, lSerializedObject);
				} else {
					if(ISDEBUG) System.out.print(" 5 ");
					lLength         = (int) (lSerializedObject.length * SERIALIZED_HEADROOM);
					lNewAttributeFp = getRandomAccessFile().length();
					writeObjectImpl(lNewAttributeFp, lLength, lSerializedObject);
				}
			}
		} else {
			if(ISDEBUG) System.out.print(" 6 ");
			/**
			 * - else attributeFp <= 0
			 *   - append length information + overhead to file
			 *   - append serialized object + n spare bytes to file
			 */
			int lLength     = (int) (lSerializedObject.length * SERIALIZED_HEADROOM);
			lNewAttributeFp = getRandomAccessFile().length(); 
			writeObjectImpl(lNewAttributeFp, lLength, lSerializedObject);
		}
		// LogWrapper.d(LOGTAG, "wrote:" + pType + "." + pName + "." + pAttributeName + "@" + lNewAttributeFp + "=" + pObject);
		return lNewAttributeFp;
	}

	/**
	 *
	 * @param pAttributeFp
	 * @param pLength
	 * @param pSerializedObject
	 *
	 */
	private void writeObjectImpl(long pAttributeFp, int pLength, byte[] pSerializedObject) throws IOException {

		removeFreeEntry(pAttributeFp);

		if(pLength > 10000) {
			System.err.println();
		}

		RandomAccessFile lFile = getRandomAccessFile();
		lFile.seek(pAttributeFp);
		lFile.writeInt(pLength);
		lFile.write(pSerializedObject);
		/**
		 * determine the number of spare bytes
		 */
		int lSpareBytesCount = pLength - pSerializedObject.length;
		if(ISDEBUG) System.out.print(" sb:" + lSpareBytesCount);
		if(lSpareBytesCount > 0) {
			if(lSpareBytesCount <= DEFAULT_SPAREBYTES.length) {
				lFile.write(DEFAULT_SPAREBYTES, 0, lSpareBytesCount);
			} else {
				lFile.write(new byte[lSpareBytesCount]);
			}
		}
	}

	/**
	 *
	 * @param pObject
	 * @return
	 * @throws IOException
	 *
	 */
	protected byte[] serializeObject(Object pObject) throws IOException {
		byte[] lSerializedObject;
		if(pObject != null) {
			ByteArrayOutputStream lBOS = new ByteArrayOutputStream(1024);
			ObjectOutputStream    lOOS = new ObjectOutputStream(lBOS);
			lOOS.writeObject(pObject);
			lOOS.close();

			lSerializedObject          = lBOS.toByteArray();

		} else {
			lSerializedObject = new byte[0];
		}
		return lSerializedObject;
	}

	/**
	 * --------------------------------------------------------------------------- 
	 * --------------------------------------------------------------------------- 
	 */
	public byte readByte(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		byte lReturn = getRandomAccessFile().readByte();
		return lReturn;
	}
	public byte[] readBytes(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		int    lLength = getRandomAccessFile().readInt();
		byte[] lBuffer = new byte[lLength];
		getRandomAccessFile().read(lBuffer);
		return lBuffer;
	}
	public char readChar(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		char lReturn = getRandomAccessFile().readChar();
		return lReturn;
	}
	public short readShort(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		short lReturn = getRandomAccessFile().readShort();
		return lReturn;
	}
	public int readInt(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		int lReturn = getRandomAccessFile().readInt();
		return lReturn;
	}
	public long readLong(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		long lReturn = getRandomAccessFile().readLong();
		return lReturn;

	}
	public float readFloat(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		float lReturn = getRandomAccessFile().readFloat();
		return lReturn;
	}
	public double readDouble(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		double lReturn = getRandomAccessFile().readDouble();
		return lReturn;
	}
	public boolean readBoolean(long pFp) throws IOException {
		getRandomAccessFile().seek(pFp);
		byte lReturn = getRandomAccessFile().readByte();
		return lReturn > 0;
	}

	public <T> T readObject(long pFp) throws IOException {
		RandomAccessFile lFile   = getRandomAccessFile();
		lFile.seek(pFp);
		int              lSize   = lFile.readInt();
		byte[]           lBuffer = null; 
		if(lSize <= DEFAULT_SPAREBYTES.length) {
			lBuffer = DEFAULT_SPAREBYTES;
		} else {
			if(lSize > 1000000) {
				throw new IOException("read size@"+pFp+" too big:" + lSize);
			}
			lBuffer = new byte[lSize];
		}
		T                    lObject = null;
		if(lSize > 0) {
			lFile.read(lBuffer, 0, lSize);
			ByteArrayInputStream lBIS    = new ByteArrayInputStream(lBuffer);

			try {
				ObjectInputStream lOIS   = new ObjectInputStream(lBIS);
				lObject                  = (T) lOIS.readObject();
				lOIS.close();
			} catch(ClassNotFoundException e) {
				LogWrapper.e(LOGTAG, "readObject: could not read object fpInMyPersFile:"+pFp+" size:" + lSize + " e:" + e.getMessage());
			} catch(IOException e) {
				LogWrapper.e(LOGTAG, "readObject: could not read object fpInMyPersFile:"+pFp+" size:" + lSize + " e:" + e.getMessage());
				throw new IOException("readObject: could not read object fpInMyPersFile:"+pFp+" size:" + lSize + " e:" + e.getMessage());
			}
		}
		return lObject;
	}

	/**
	 * --------------------------------------------------------------------------- 
	 * --------------------------------------------------------------------------- 
	 */
	protected RandomAccessFile getRandomAccessFile() throws IOException {
		if(randomAccessFile == null) {
			randomAccessFile = new RandomAccessFile(file, "rws");
		}
		return randomAccessFile;
	}

	/**
	 *
	 * @return
	 * @throws IOException 
	 *
	 */
	public long length() throws IOException {
		return getRandomAccessFile().length();
	}

}
