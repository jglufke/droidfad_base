/**
 * 
 */
package com.droidfad.persistency;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.droidfad.classloading.ClazzFinder;
import com.droidfad.data.ACategory;
import com.droidfad.data.ACategory.Category;
import com.droidfad.data.ADao;
import com.droidfad.util.FileUtil;
import com.droidfad.util.LogWrapper;
import com.droidfad.util.ReflectionUtil;

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
 * - all operations are synchroneous
 * - if a type is handled for the first time in this runtime, it is checked
 *   if the schema of the type has changed. If a change has been realized
 *   appropriate actions are taken
 * - it is checked how many files are open at the same time. If a threshold
 *   is exceeded the less 10th of threshold used files are closed.
 * - implementation as a singleton
 * - class is used by ObjectMgrTest which already grants synchronicity. No additional
 *   synchroneous clauses have thus to be used
 * 
 * This class should not be used directly by an application but through the interface
 * IData
 */
public class Persistency {

	/**
	 * in case that an ADao subclass does not have a single persistent attribute
	 * a DoNotCare attribute has been introduced to achieve that the ADao subclass
	 * is persisted anyway
	 */
	public static final String DUMMY_ATTRIBUTE_NAME = "___DUMMY___";
	private static final String SEPARATOR = "------------------------------" +
			"------------------------------";
	
	public enum TypeKey { _boolean, _byte, _byteA, _char, _double, _float, _int, 
		_long, _short } 

	private static final String ATTRIBUTE_FILENAME    = "Attributes";
	private static final String TYPEINFO_FILENAME     = "TypeInfo.txt";

	private static final String ATTRIBUTE_FP_FILENAME = "AttributeFp";
	private static final double DEFRAGMENT_THRESHOLD = 1.5;
	private static Persistency  instance = null;

	private static final String INSTANCE_FP_FILENAME  = "InstanceFp";
	private static final String LOGTAG = Persistency.class.getSimpleName();

	protected static String rootDir = "/data/data/com.droidfad/databases/pers";

	public static Persistency getInstance() {
		if(instance == null) {
			instance = new Persistency();
		}
		return instance;
	}
	private HashMap<Class<? extends ADao>, TreeMap<String, Integer>> attributeIndexMap;

	private HashSet<Class<? extends ADao>> daoSubTypeSet;
	private HashMap<Class<? extends ADao>, PersFiles> persFilesMap = new HashMap<Class<? extends ADao>, PersFiles>();

	private ReflectionUtil reflectionUtil = new ReflectionUtil();
	/**
	 * key    - class
	 * value  - type key
	 */
	private HashMap<Class<?>, TypeKey> typeKeyMap = new HashMap<Class<?>, Persistency.TypeKey>();

	private boolean isDefragmentationEnabled = true;

	protected Persistency() {
		long lStart = System.currentTimeMillis();
		init();
		LogWrapper.i(LOGTAG, "init took:" + (System.currentTimeMillis()-lStart) + " msec");
	}

	/**
	 *
	 * @param pPersFiles
	 *
	 */
	private void checkDefragmentation(Class<? extends ADao> pType, PersFiles pPersFiles) throws IOException {

		double lSizeToValid = pPersFiles.instanceFpInfo.getSizeToValid();
		if(isDefragmentationEnabled && (DEFRAGMENT_THRESHOLD < lSizeToValid)) {

			long lSizeBefore = getDirectorySize(pPersFiles.attributeFile.feFile.getParentFile().getAbsolutePath());
			long lStart      = System.currentTimeMillis();

			pPersFiles.instanceFpInfo.defragment();
			pPersFiles.attributeFpInfo.defragment(pPersFiles.instanceFpInfo);

			String                   lOldFilename      = pPersFiles.attributeFile.getFilename();
			String                   lDefFilename      = lOldFilename + ".df";
			AttributeFile            lDefragmentedFile = new AttributeFile(new File(lDefFilename));
			boolean                  lToBeBackCopied   = false;
			TreeMap<String, Integer> lIndexMap         = attributeIndexMap.get(pType);
			for(String lInstanceName : pPersFiles.instanceFpInfo.getInstanceNameSet()) {

				long                 lInstanceFp       = pPersFiles.instanceFpInfo.getInstanceFp(lInstanceName);
				if(defragmentAttributeFile(pType, lInstanceName, pPersFiles, lDefragmentedFile, lInstanceFp, lIndexMap)) {
					lToBeBackCopied = true;
				}
			}
			/**
			 * copy the defragmented file to the old location 
			 */
			pPersFiles.attributeFile.close();
			if(pPersFiles.attributeFile.feFile != null && pPersFiles.attributeFile.feFile.isFile()) {
				pPersFiles.attributeFile.feFile.delete();
			}
			lDefragmentedFile.close();
			File lNewAttributeFile = new File(lDefFilename);
			if(lToBeBackCopied) {
				if(!lNewAttributeFile.renameTo(new File(lOldFilename))) {
					FileUtil.copy(lDefFilename, lOldFilename);
				}
			}
			lNewAttributeFile = new File(lDefFilename);
			if(lNewAttributeFile.isFile()) {
				lNewAttributeFile.delete();
			}

			//			long lSizeAfter = getDirectorySize(pPersFiles.attributeFile.feFile.getParentFile().getAbsolutePath());
			//			System.err.println("- defragment:" + (System.currentTimeMillis()-lStart) + " " + lSizeBefore + "/" + lSizeAfter);
		}
	}

	protected void printStatistics(PrintStream pPS) {
		pPS.println("-------- statistics:" + new Date() + ":" + this);
		long lPersistencySize = getDirectorySize(rootDir);
		int  lObjectCount     = 0;
		for(PersFiles lPersFiles : persFilesMap.values()) {
			lObjectCount += lPersFiles.instanceFpInfo.getInstanceNameSet().size();
		}

		pPS.println("- bytes       :" + lPersistencySize + " objects:" + lObjectCount);
		if(lObjectCount > 0) {
			pPS.println("- bytes/object:" + (lPersistencySize / lObjectCount));
		} else {
			pPS.println("- bytes/object:" + lPersistencySize + " for 0 objects");
		}
	}

	/**
	 *
	 * @param pRootdir
	 * @return
	 *
	 */
	private long getDirectorySize(String pRootdir) {
		long lFileSize = 0;
		for(File lFile : FileUtil.find(new File(pRootdir), ".+")) {
			if(lFile.isFile()) {
				lFileSize += lFile.length();
			}
		}
		return lFileSize;
	}

	/**
	 *
	 * @param pType
	 * @param pPersFiles
	 * @param pDefragmentedFile
	 * @param pInstanceFp
	 * @param pIndexMap
	 * @throws IOException
	 *
	 */
	protected boolean defragmentAttributeFile(Class<? extends ADao> pType, String pName, PersFiles pPersFiles, 
			AttributeFile pDefragmentedFile, long pInstanceFp, TreeMap<String, Integer> pIndexMap)	
	throws IOException {

		boolean lToBeBackCopied = false;
		for(String lAttributeName : reflectionUtil.getPersistentAttributeNames(pType, null)) {
			Class<?> lAttributeType  = reflectionUtil.getAttributeType(pType, lAttributeName, null);
			int      lAttributeIndex = pIndexMap.get(lAttributeName);
			long     lAttributeFp    = pPersFiles.attributeFpInfo.getAttributeFp(pInstanceFp, lAttributeIndex);
			if(lAttributeFp >= 0) {
				/**
				 * only copy values that have set at least once in Persistency. Otherwise,
				 * lAttributeFp is -1
				 */
				TypeKey    lTypeKey        = typeKeyMap.get(lAttributeType);
				lToBeBackCopied            = true;

				if(false) {
					addDebugInfo(pType, pName, lAttributeName, lAttributeType, lTypeKey, lAttributeFp);
				}

				if(lTypeKey != null) {
					Object lAttributeValue = readPrimitiveValueFromFile(pPersFiles.attributeFile, lAttributeFp, lTypeKey);
					lAttributeFp           = writePrimitiveValueToFile(pDefragmentedFile, -1, lTypeKey, lAttributeValue);
					pPersFiles.attributeFpInfo.setAttributeFp(pInstanceFp, lAttributeIndex, lAttributeFp);
				} else {
					Object lAttributeValue = pPersFiles.attributeFile.readObject(lAttributeFp);
					lAttributeFp           = pDefragmentedFile.writeObject(pType.getSimpleName(), pName, lAttributeName, -1, lAttributeValue);
					pPersFiles.attributeFpInfo.setAttributeFp(pInstanceFp, lAttributeIndex, lAttributeFp);
				}
			}
		}
		return lToBeBackCopied;
	}

	/**
	 *
	 * @param pType
	 * @param pName
	 * @param pAttributeName
	 * @param pAttributeType
	 * @param lTypeKey
	 * @param pAttributeFp
	 *
	 */
	protected void addDebugInfo(Class<? extends ADao> pType, String pName,
			String pAttributeName, Class<?> pAttributeType, TypeKey lTypeKey,
			long pAttributeFp) {

		StringBuilder lBuilder = new StringBuilder("copy ");
		lBuilder.append(pType.getSimpleName());
		lBuilder.append('.');
		lBuilder.append(pName);
		lBuilder.append('.');
		lBuilder.append(pAttributeType.getSimpleName());
		lBuilder.append( ".");
		lBuilder.append( pAttributeName);
		lBuilder.append( "@");
		lBuilder.append( pAttributeFp);
		lBuilder.append( " key:");
		lBuilder.append( lTypeKey);
		LogWrapper.d(LOGTAG, lBuilder.toString());
		System.err.flush();
	}

	public void dump(String pFilename) {
		try {
			PrintWriter lPW = new PrintWriter(pFilename);
			dump(lPW);
			lPW.close();
		} catch(IOException e) {
			LogWrapper.e(LOGTAG, "can not write dump to file:" + pFilename + " e:" + e.getMessage());
		}
	}

	public void dump(PrintWriter pPS) {
		if(persFilesMap != null && !persFilesMap.isEmpty()) {
			TreeMap<String, Class<? extends ADao>> lSortedMap = new TreeMap<String, Class<? extends ADao>>();
			for(Class<? extends ADao> lType : persFilesMap.keySet()) {
				lSortedMap.put(lType.getName(), lType);
			}
			for(Class<? extends ADao> lType : lSortedMap.values()) {
				PersFiles lPersFiles = persFilesMap.get(lType);
				dump(pPS, lType, lPersFiles);
			}
		}
		pPS.flush();
	}

	static class DumpEntry {
		String instanceType   = "UNDEF IT";
		String instanceName   = "UNDEF IN";
		long   instanceFp     = Long.MIN_VALUE;
		String attributeName  = "UNDEF AN";
		long   attributeIndex = Long.MIN_VALUE;
		long   attributeFp    = Long.MAX_VALUE;
		String attributeType  = "UNDEF AT";
		String attributeValue = "UNDEF AV";
		String comment        = "";
	}
	/**
	 *
	 * @param pType
	 * @param pPersFiles
	 *
	 */
	private void dump(PrintWriter pPS, Class<? extends ADao> pType, PersFiles pPersFiles) {
		/**
		 * create a list sorting according to the file pointers in the attribute file
		 */
		pPS.println(SEPARATOR);
		pPS.println(pType.getSimpleName()+ " " + SEPARATOR.substring(0, Math.max(0, SEPARATOR.length()-pType.getSimpleName().length()-1)));
		pPS.println(SEPARATOR);
		pPS.print(pPersFiles.instanceFpInfo.toString());
		pPS.print(pPersFiles.attributeFpInfo.toString());
		/**
		 * key    - attributeFp
		 * value  - list of dump entries
		 */
		TreeMap<Long, Vector<DumpEntry>> lSortedEntryMap = new TreeMap<Long, Vector<DumpEntry>>();

		for(Map.Entry<String, InstanceFpEntry> lInstanceEntry : pPersFiles.instanceFpInfo.fpMap.entrySet()) {
			String          lInstanceName    = lInstanceEntry.getKey();
			InstanceFpEntry lInstanceFpEntry = lInstanceEntry.getValue();
			long            lInstanceFp      = lInstanceFpEntry.instanceFp;

			if(!lInstanceFpEntry.isValid || lInstanceFp < 0) {

				DumpEntry lDumpEntry      = new DumpEntry();
				lDumpEntry.attributeFp    = Long.MAX_VALUE;
				lDumpEntry.instanceType   = pType.getSimpleName();
				lDumpEntry.instanceName   = lInstanceName;
				lDumpEntry.instanceFp     = lInstanceFp;
				lDumpEntry.comment        = "instanceFp not valid:" + lInstanceFpEntry.isValid + "/" + lInstanceFp;
				addDumpEntry(lSortedEntryMap, lDumpEntry);

			} else {

				addDumpEntries(pType, pPersFiles, lSortedEntryMap, lInstanceName, lInstanceFp);

			}
		}

		/**
		 * write the sorted dump entries to PrintWriter
		 */
		for(Vector<DumpEntry> lDumpEntryList : lSortedEntryMap.values()) {
			for(DumpEntry lDE : lDumpEntryList) {

				pPS.print(lDE.attributeFp);
				pPS.print("-");
				pPS.print(lDE.instanceType);
				pPS.print('.');
				pPS.print(lDE.instanceName);
				pPS.print('@');
				pPS.print(lDE.instanceFp);
				pPS.print('.');
				pPS.print(lDE.attributeType);
				pPS.print('.');
				pPS.print(lDE.attributeName);
				pPS.print('@');
				pPS.print(lDE.attributeFp);
				pPS.print('=');
				pPS.print(lDE.attributeValue);
				pPS.print(' ');
				pPS.println(lDE.comment);

			}
		}
	}

	/**
	 *
	 * @param pPersFiles 
	 * @param pType 
	 * @param pSortedEntryMap
	 * @param pInstanceName
	 * @param pInstanceFp
	 *
	 */
	private void addDumpEntries(Class<? extends ADao> pType, PersFiles pPersFiles, TreeMap<Long, Vector<DumpEntry>> pSortedEntryMap, String pInstanceName, long pInstanceFp) {

		TreeMap<String, Integer> lIndexMap = attributeIndexMap.get(pType);
		for(String lAttributeName : new TreeSet<String>(lIndexMap.keySet())) {
			int  lAttributeIndex = lIndexMap.get(lAttributeName);
			long lAttributeFp    = pPersFiles.attributeFpInfo.getAttributeFp(pInstanceFp, lAttributeIndex);

			DumpEntry lDumpEntry      = new DumpEntry();
			lDumpEntry.attributeIndex = lAttributeIndex;
			lDumpEntry.attributeName  = lAttributeName;

			lDumpEntry.instanceFp     = pInstanceFp;
			lDumpEntry.instanceType   = pType.getSimpleName();
			lDumpEntry.instanceName   = pInstanceName;

			if(lAttributeFp >= 0 && !DUMMY_ATTRIBUTE_NAME.equals(lAttributeName)) {
				Class<?> lAttributeType = reflectionUtil.getAttributeType(pType, lAttributeName, null);
				TypeKey  lTypeKey       = typeKeyMap.get(lAttributeType);
				Object   lValue         = null;
				try { 
					if(lTypeKey != null) {
						lValue = readPrimitiveValueFromFile(pPersFiles.attributeFile, lAttributeFp, lTypeKey);
					} else {
						lValue = pPersFiles.attributeFile.readObject(lAttributeFp);
					}
				} catch(Exception e) {
					lValue = "XXXXX exception:" + e.getMessage();
				}
				lDumpEntry.attributeFp    = lAttributeFp;
				lDumpEntry.attributeType  = lAttributeType.getSimpleName();
				lDumpEntry.attributeValue = "" + lValue;
				if(lDumpEntry.attributeValue.length() > 200) {
					lDumpEntry.attributeValue = lDumpEntry.attributeValue.substring(0, 200);
				}
			} else {
				lDumpEntry.comment = "attributeFp<0";
			}
			addDumpEntry(pSortedEntryMap, lDumpEntry);
		}
	}

	/**
	 *
	 * @param pSortedEntryMap
	 * @param pDumpEntry
	 *
	 */
	private void addDumpEntry(TreeMap<Long, Vector<DumpEntry>> pSortedEntryMap, DumpEntry pDumpEntry) {
		Vector<DumpEntry> lEntryList = pSortedEntryMap.get(pDumpEntry.attributeFp);
		if(lEntryList == null) {
			lEntryList = new Vector<Persistency.DumpEntry>();
			pSortedEntryMap.put(pDumpEntry.attributeFp, lEntryList);
		}
		lEntryList.add(pDumpEntry);
	}


	/**
	 *
	 * @param pType
	 * @return
	 *
	 */
	private PersFiles createPersFiles(Class<? extends ADao> pType) {
		PersFiles     lPersFiles = new PersFiles();
		StringBuilder lBuilder   = new StringBuilder(50);
		lBuilder.append(rootDir);
		lBuilder.append('/');
		lBuilder.append(pType.getName());
		lBuilder.append('/');
		String lTypeRootPath = lBuilder.toString(); 
		lPersFiles.instanceFpInfo  = new InstanceFpInfo(new File(lTypeRootPath, INSTANCE_FP_FILENAME));
		int lAttributeCount        = attributeIndexMap.get(pType).size();
		lPersFiles.attributeFpInfo = new AttributeFpInfo(new File(lTypeRootPath, ATTRIBUTE_FP_FILENAME), lAttributeCount);
		lPersFiles.attributeFile   = new AttributeFile(new File(lTypeRootPath, ATTRIBUTE_FILENAME));

		return lPersFiles;
	}

	/**
	 * 
	 * returns the list of persisted instances of pType
	 * @param <T>
	 * @param pType
	 * @return
	 *
	 */
	public HashSet<String> getPersistedInstanceNameSet(Class<? extends ADao> pType) {

		HashSet<String> lInstanceNameSet  = new HashSet<String>();

		if(getPersFilesMap() == null) {
			init();
		}
		PersFiles lPersFiles = getPersFilesMap().get(pType);
		if(lPersFiles == null) {
			lPersFiles = createPersFiles(pType);
			getPersFilesMap().put(pType, lPersFiles);
		}
		lInstanceNameSet = lPersFiles.instanceFpInfo.getInstanceNameSet();
		return lInstanceNameSet;
	}

	/**
	 *
	 * @param pType
	 * @param pName
	 * @param pAttributeName
	 * @param pAttributeType
	 * @return
	 *
	 */
	public <T> T getValueFromFile(Class<? extends ADao> pType, String pName, String pAttributeName, Class<?> pAttributeType) throws IOException {

		T         lValue     = null;
		if(getPersFilesMap() == null) {
			init();
		}
		PersFiles lPersFiles = getPersFilesMap().get(pType);
		if(lPersFiles != null) {

			OpenFilesWatch.touch(lPersFiles);

			/**
			 * if the instance is not stored yet, the value is -1
			 */
			long lInstanceFp     = lPersFiles.instanceFpInfo.getInstanceFp(pName);
			if(lInstanceFp >= 0) {
				int  lAttributeIndex = attributeIndexMap.get(pType).get(pAttributeName);
				try {
					long lAttributeFp    = lPersFiles.attributeFpInfo.getAttributeFp(lInstanceFp, lAttributeIndex);
					if(lAttributeFp >= 0) {
						TypeKey lTypeKey = typeKeyMap.get(pAttributeType);
						if(lTypeKey != null) {
							lValue = readPrimitiveValueFromFile(lPersFiles.attributeFile, lAttributeFp, lTypeKey);
						} else {
							lValue = lPersFiles.attributeFile.readObject(lAttributeFp);
						}
					}
				} catch(Exception e) {
					LogWrapper.e(LOGTAG, "could not read:" + pType.getSimpleName() + "." + pName + "." + pAttributeName, e);
					close();
					removeInstance(pType, pName);
				}
			}
		}
		return lValue;
	}


	/**
	 * init checks if a schema migration took place and takes the respective
	 * actions
	 *
	 *
	 */
	protected void init() {

		LogWrapper.i(LOGTAG, "init:enter");

		typeKeyMap.put(byte.class, TypeKey._byte);
		typeKeyMap.put(Byte.class, TypeKey._byte);
		typeKeyMap.put(byte[].class, TypeKey._byteA);
		typeKeyMap.put(char.class, TypeKey._char);
		typeKeyMap.put(Character.class, TypeKey._char);
		typeKeyMap.put(short.class, TypeKey._short);
		typeKeyMap.put(Short.class, TypeKey._short);
		typeKeyMap.put(int.class, TypeKey._int);
		typeKeyMap.put(Integer.class, TypeKey._int);
		typeKeyMap.put(long.class, TypeKey._long);
		typeKeyMap.put(Long.class, TypeKey._long);
		typeKeyMap.put(float.class, TypeKey._float);
		typeKeyMap.put(Float.class, TypeKey._float);
		typeKeyMap.put(double.class, TypeKey._double);
		typeKeyMap.put(Double.class, TypeKey._double);
		typeKeyMap.put(boolean.class, TypeKey._boolean);
		typeKeyMap.put(Boolean.class, TypeKey._boolean);

		persFilesMap = new HashMap<Class<? extends ADao>, PersFiles>();

		/**
		 * get all the current subclasses of ADAO
		 */
		daoSubTypeSet = ClazzFinder.findSubclasses(ADao.class);

		/**
		 * load the type info file which describes the partialTopic of the 
		 * database.
		 * key   - type
		 * key   - attribute name
		 * key   - type
		 * 
		 * remove all types from the database that reference not existing 
		 * types
		 */
		File lTypeInfoFile      = new File(rootDir, TYPEINFO_FILENAME);
		HashMap<Class<? extends ADao>, HashMap<String, Class<?>>> lFileTypeAttributeMap = loadTypeInfoFile(lTypeInfoFile, daoSubTypeSet, true);

		String lErrorMessage = compareTypeInfoWithRuntimeTypes(lFileTypeAttributeMap, true, true);
		if(!"".equals(lErrorMessage)) {
			LogWrapper.e(LOGTAG, lErrorMessage);
		}
		/**
		 * create the type info files for all types that are handled by this 
		 */
		attributeIndexMap  = writeTypeInfoFile(lTypeInfoFile, daoSubTypeSet);

		/**
		 * appl indices to the attributeIndex map according to the sorting 
		 * order of the attributes
		 */ 
		for(TreeMap<String, Integer> lMap : attributeIndexMap.values()) {
			int i = 0;
			for(String lAttributeName : lMap.keySet()) {
				lMap.put(lAttributeName, i++);
			}
		}
	}
	/**
	 * 
	 *
	 * @param pFileTypeAttributeMap
	 * @param pRemoveFromPersistency
	 * @param pCheckAttributeCompleteness 
	 * @return empty string if no error occured, error message otherwise
	 *
	 */
	protected String compareTypeInfoWithRuntimeTypes(
			HashMap<Class<? extends ADao>, HashMap<String, Class<?>>> pFileTypeAttributeMap,
			boolean pRemoveFromPersistency, boolean pCheckAttributeCompleteness) {

		StringBuilder lErrorMessage = new StringBuilder(150);

		/**
		 * create a map that holds the same information for the current types
		 */
		HashMap<Class<? extends ADao>, HashMap<String,Class<?>>> lCurrentTypeAttributeMap =
			new HashMap<Class<? extends ADao>, HashMap<String,Class<?>>>();
		for(Class<? extends ADao> lSubClass : daoSubTypeSet) {
			HashMap<String,Class<?>> lAttributeMap = new HashMap<String, Class<?>>();
			lCurrentTypeAttributeMap.put(lSubClass, lAttributeMap);
			try {
				List<String> lAttributeNameList = reflectionUtil.getPersistentAttributeNames(lSubClass, null); 
				for(String lAttributeName : lAttributeNameList) {
					Class<?> lAttributeType = reflectionUtil.getAttributeType(lSubClass, lAttributeName, null);
					lAttributeMap.put(lAttributeName, lAttributeType);
				}
			} catch(Exception e) {
				LogWrapper.e(LOGTAG, "", e);
			}
		}

		/**
		 * check for changed attributes: attributes removed from current daoSubclass
		 */
		for(Class<? extends ADao> lPersistedDaoSubclass : pFileTypeAttributeMap.keySet()) {
			HashMap<String,Class<?>> lPersistedAttributeMap = pFileTypeAttributeMap.get(lPersistedDaoSubclass);
			HashMap<String,Class<?>> lCurrentAttributeMap   = lCurrentTypeAttributeMap.get(lPersistedDaoSubclass);
			if(lCurrentAttributeMap != null) {
				for(Entry<String, Class<?>> lPersistedAttributeEntry : lPersistedAttributeMap.entrySet()) {
					String   lPersistedAttributeName = lPersistedAttributeEntry.getKey();
					if(lCurrentAttributeMap.get(lPersistedAttributeName) == null && !DUMMY_ATTRIBUTE_NAME.equals(lPersistedAttributeName)) {
						LogWrapper.e(LOGTAG, "1 type:" + lPersistedDaoSubclass.getSimpleName() +" changed, persisted data will be deleted");
						removeTypeFromPersistency(lPersistedDaoSubclass.getName());
						break;
					}
				}
			} else {
				lErrorMessage.append("2 type:" + lPersistedDaoSubclass.getSimpleName() +" changed, persisted data will be deleted\n");
				if(pRemoveFromPersistency) {
					removeTypeFromPersistency(lPersistedDaoSubclass.getName());
				}
				break;
			}
		}

		/**
		 * check for changed attributes: attributes added to current daoSubclass
		 */
		for(Class<? extends ADao> lCurrentDaoSubclass : lCurrentTypeAttributeMap.keySet()) {
			HashMap<String,Class<?>> lCurrentAttributeMap   = lCurrentTypeAttributeMap.get(lCurrentDaoSubclass);
			HashMap<String,Class<?>> lPersistedAttributeMap = pFileTypeAttributeMap.get(lCurrentDaoSubclass);
			if(lPersistedAttributeMap != null) {
				for(Entry<String, Class<?>> lCurrentAttributeEntry : lCurrentAttributeMap.entrySet()) {
					String   lCurrentAttributeName = lCurrentAttributeEntry.getKey();
					if(lPersistedAttributeMap.get(lCurrentAttributeName) == null) {
						lErrorMessage.append("3 type:" + lCurrentDaoSubclass.getSimpleName() +" changed, persisted data will be deleted\n");
						if(pRemoveFromPersistency) {
							removeTypeFromPersistency(lCurrentDaoSubclass.getName());
						}
						break;
					}
				}
			} else {
				if(pCheckAttributeCompleteness) {
					lErrorMessage.append("4 no attribute info persisted for:" + lCurrentDaoSubclass.getSimpleName() +", persisted data will be deleted\n");
					if(pRemoveFromPersistency) {
						removeTypeFromPersistency(lCurrentDaoSubclass.getName());
					}
					break;
				}
			}
		}

		/**
		 * check for changed attributes: attribute type changed in current daoSubclass
		 */
		for(Class<? extends ADao> lCurrentDaoSubclass : lCurrentTypeAttributeMap.keySet()) {
			HashMap<String,Class<?>> lCurrentAttributeMap   = lCurrentTypeAttributeMap.get(lCurrentDaoSubclass);
			HashMap<String,Class<?>> lPersistedAttributeMap = pFileTypeAttributeMap.get(lCurrentDaoSubclass);
			if(lPersistedAttributeMap != null) {
				for(Entry<String, Class<?>> lCurrentAttributeEntry : lCurrentAttributeMap.entrySet()) {
					String   lCurrentAttributeName   = lCurrentAttributeEntry.getKey();
					Class<?> lCurrentAttributeType   = lCurrentAttributeEntry.getValue();
					Class<?> lPersistedAttributeType = lPersistedAttributeMap.get(lCurrentAttributeName); 
					if(lPersistedAttributeType != null && !lCurrentAttributeType.equals(lPersistedAttributeType)) {
						lErrorMessage.append("5 attribute type changed for attribute:" + lCurrentAttributeName + "." + lCurrentDaoSubclass.getSimpleName() +", persisted data will be deleted\n");
						if(pRemoveFromPersistency) {
							removeTypeFromPersistency(lCurrentDaoSubclass.getName());
						}
						break;
					}
				}
			} 
		}
		return lErrorMessage.toString();
	}

	/**
	 *
	 * if a class in the type info file can not be loaded the respective directory
	 * has to be deleted. Same applies if a class of an attribute of a type can not
	 * be loaded by the class loader. Also in this case the complete persisted information
	 * of the respective class is deleted
	 * @param pDaoSubTypeSet a set of ADao subclasses that are loaded with type
	 * info file. Any other type not contained in oDaoSubTypeSet will be deleted
	 * from persistency   
	 * @return
	 *
	 */
	private HashMap<Class<? extends ADao>, HashMap<String, Class<?>>> loadTypeInfoFile(
			File pTypeInfoFile, HashSet<Class<? extends ADao>> pDaoSubTypeSet,
			boolean pDeleteNotContainedTypes) {

		/**
		 * create a set with all available subtype names
		 */
		HashMap<String, Class<? extends ADao>> lTypenameMap = new HashMap<String, Class<? extends ADao>>();
		for(Class<? extends ADao> lType : pDaoSubTypeSet) {
			lTypenameMap.put(lType.getName(), lType);
		}

		/**
		 * key   - dao subclass
		 * key   - attribute name
		 * value - attributet type
		 */
		HashMap<Class<? extends ADao>, HashMap<String, Class<?>>> lReturnMap =
			new HashMap<Class<? extends ADao>, HashMap<String,Class<?>>>();

		try {
			BufferedReader lBR           = new BufferedReader(new FileReader(pTypeInfoFile));
			String         lLine         = null;
			while((lLine=lBR.readLine()) != null) {
				String[] lContent   = lLine.split(";");
				String   lClassname = lContent[0];

				Class<? extends ADao> lDaoSubClass = lTypenameMap.get(lClassname); 
				if(lDaoSubClass != null) {

					loadTypeInfo(lReturnMap, lDaoSubClass, lClassname, lContent);

				} else if(pDeleteNotContainedTypes) {
					/**
					 * the class name contained in the type info files does not exists as an ADao subclass.
					 * So, the respective directory has to be deleted from the persistency
					 */
					LogWrapper.e(LOGTAG, "type does not exist as ADao subclass anymore. Remove it:" + lClassname);
					removeTypeFromPersistency(lClassname);				
				}
			}
			lBR.close();
		} catch(IOException e) {
			LogWrapper.e(LOGTAG, "loadTypeInfoFile:could not load type info file");
		}

		return lReturnMap;
	}

	/**
	 *
	 * @param pReturnMap
	 * @param pDaoSubClass
	 * @param pClassname
	 * @param pContent
	 *
	 */
	protected void loadTypeInfo(
			HashMap<Class<? extends ADao>, HashMap<String, Class<?>>> pReturnMap,
			Class<? extends ADao> pDaoSubClass, String pClassname,
			String[] pContent) {

		HashMap<String,Class<?>> lAttributeMap = new HashMap<String, Class<?>>();
		pReturnMap.put(pDaoSubClass, lAttributeMap);
		for(int i=1; i<pContent.length; i++) {
			String[] lAttributeDefinition = pContent[i].split("=");
			if(lAttributeDefinition.length == 2) {
				String lAttributeName     = lAttributeDefinition[0];
				String lAttributeTypeName = lAttributeDefinition[1];
				/**
				 * try to load the attribute type class. If it does not exist
				 * invalidate the complete type by deleting the type's directory
				 */
				try {

					Class<?> lAttributeType = reflectionUtil.getPrimitiveType(lAttributeTypeName);
					if(lAttributeType == null) {
						lAttributeType = Class.forName(lAttributeTypeName);
					}
					lAttributeMap.put(lAttributeName, lAttributeType);

				} catch (ClassNotFoundException e) {

					LogWrapper.e(LOGTAG, "loadTypeInfo:could not load class:" + lAttributeTypeName);
					removeTypeFromPersistency(pClassname);				
					pReturnMap.remove(pDaoSubClass);
					break;

				}
			} else {
				LogWrapper.e(LOGTAG, "error reading type info file. wrong attribute format:" + pClassname + "." + pContent);
			}
		}
	}

	/**
	 *
	 * @param pAttributeFile
	 * @param pAttributeFp
	 * @param pTypeKey
	 * @return
	 *
	 */
	@SuppressWarnings("unchecked")
	private <T> T readPrimitiveValueFromFile(AttributeFile pAttributeFile, long pAttributeFp, TypeKey pTypeKey) {

		T lValue = null;
		try {
			switch(pTypeKey) {
			case _boolean: {
				lValue =  (T) (Boolean) pAttributeFile.readBoolean(pAttributeFp);
				break;
			}
			case _byte: {
				lValue =  (T) (Byte) pAttributeFile.readByte(pAttributeFp);
				break;
			}
			case _byteA: {
				lValue =  (T) pAttributeFile.readBytes(pAttributeFp);
				break;
			}
			case _char: {
				lValue =  (T) (Character) pAttributeFile.readChar(pAttributeFp);
				break;
			}
			case _double: { 
				lValue =  (T) (Double) pAttributeFile.readDouble(pAttributeFp);
				break;
			}
			case _float: {
				lValue =  (T) (Float) pAttributeFile.readFloat(pAttributeFp);
				break;
			}
			case _int: {
				lValue =  (T) (Integer) pAttributeFile.readInt(pAttributeFp);
				break;
			}
			case _long: {
				lValue =  (T) (Long) pAttributeFile.readLong(pAttributeFp);
				break;
			}
			case _short: { 
				lValue =  (T) (Short) pAttributeFile.readShort(pAttributeFp);
				break;
			}
			default: 
				throw new IllegalArgumentException("unhandled type:" + pTypeKey);
			}
		} catch(IOException e) {
			e.printStackTrace();
			LogWrapper.e(LOGTAG, "can not read value for:" + pAttributeFile);
		}
		return lValue;
	}

	/**
	 *
	 * @param pClassname
	 *
	 */
	protected void removeTypeFromPersistency(String pClassname) {
		FileUtil.deleteDir(new File(rootDir, pClassname));
	}

	public void removeInstance(Class<? extends ADao> pType, String pName) {
		PersFiles lPersFiles = getPersFilesMap().get(pType);

		if(lPersFiles != null) {
			OpenFilesWatch.touch(lPersFiles);
			long lInstanceFp = lPersFiles.instanceFpInfo.getInstanceFp(pName);
			if(lInstanceFp >= 0) {
				try {
					lPersFiles.instanceFpInfo.removeFp(pName);
					lPersFiles.attributeFpInfo.removeFp(lInstanceFp);
					// LogWrapper.d(LOGTAG, "remove 4: " + pName + " " + lPersFiles.instanceFpInfo.getInstanceFp(pName));

				} catch(Exception e) {
					LogWrapper.e(LOGTAG, "removeInstance: could not remove:" + pType.getSimpleName() + "." + pName);
				}
				try {
					checkDefragmentation(pType, lPersFiles);
				} catch(IOException e) {
					LogWrapper.e(LOGTAG, "removeInstance: error defragmentation, removed:" + pType.getSimpleName() + "." + pName);
				}
			}
		}
	}

	/**
	 *
	 * @param pAttributeFile
	 * @param pAttributeFp
	 * @param pTypeKey
	 * @param pValue
	 * @throws IOException
	 *
	 */
	protected long writePrimitiveValueToFile(AttributeFile pAttributeFile, 
			long pAttributeFp, TypeKey pTypeKey, Object pValue)
	throws IOException {


		switch(pTypeKey) {
		case _boolean: {
			boolean lDBValue = pValue != null ? (Boolean) pValue : false;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _byte: {
			byte lDBValue = pValue != null ? (Byte) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _byteA: {
			byte[] lDBValue = pValue != null ? (byte[]) pValue : null;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _char: {
			char lDBValue = pValue != null ? (Character) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _double: { 
			double lDBValue = pValue != null ? (Double) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _float: {
			float lDBValue = pValue != null ? (Float) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _int: {
			int lDBValue = pValue != null ? (Integer) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _long: {
			long lDBValue = pValue != null ? (Long) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		case _short: { 
			short lDBValue = pValue != null ? (Short) pValue : 0;
			pAttributeFp = pAttributeFile.write(pAttributeFp, lDBValue);
			break;
		}
		default: 
			throw new IllegalArgumentException("unhandled type:" + pTypeKey);
		}

		return pAttributeFp;
	}

	/**
	 * write the information about the currently available subclasses of 
	 * ADAO to file. The relevant information is 
	 * typeName;persAttribute=type;.....
	 * @return 
	 *
	 */
	private HashMap<Class<? extends ADao>, TreeMap<String, Integer>> writeTypeInfoFile(
			File pTypeInfoFile, HashSet<Class<? extends ADao>> pTypeSet) {

		/**
		 * key     - type
		 * key     - attribute name
		 * value   - index
		 */
		HashMap<Class<? extends ADao>, TreeMap<String, Integer>> lAttributeMap = 
			new HashMap<Class<? extends ADao>, TreeMap<String, Integer>>();

		pTypeInfoFile.getParentFile().mkdirs();

		ReflectionUtil lReflectionUtil = new ReflectionUtil();
		try {
			BufferedWriter lBW = new BufferedWriter(new FileWriter(pTypeInfoFile));
			for(Class<? extends ADao> lType : pTypeSet) {				
				lBW.write(lType.getName());

				TreeMap<String, Integer> lSortedAttributeMap = new TreeMap<String, Integer>();
				lAttributeMap.put(lType, lSortedAttributeMap);
				List<String> lAttributeNameList = lReflectionUtil.getPersistentAttributeNames(lType, null);
				if(lAttributeNameList.isEmpty()) {
					lAttributeNameList.add(DUMMY_ATTRIBUTE_NAME);
				}
				for(String lAttributeName : lAttributeNameList) {

					lSortedAttributeMap.put(lAttributeName, 0);

					lBW.write(';');
					lBW.write(lAttributeName);
					lBW.write('=');
					Class<?> lAttributeType = null;
					if(DUMMY_ATTRIBUTE_NAME.equals(lAttributeName)) {
						lAttributeType = String.class;
					} else {
						lAttributeType = lReflectionUtil.getAttributeType(lType, lAttributeName, null);
					}
					lBW.write(lAttributeType.getName());
				}
				lBW.write('\n');
			}
			lBW.close();
		} catch(IOException e) {
			LogWrapper.e(LOGTAG, "writeTypeInfoFile:could not write type info file:" + e.getMessage());
		}

		return lAttributeMap;
	}
	/**
	 *
	 * @param pType
	 * @param pInstanceName
	 * @param pAttributeName
	 * @param pValue
	 *
	 */
	public void writeValueToFile(Class<? extends ADao> pType, String pInstanceName, String pAttributeName, Object pValue) 
	throws IOException {

		/**
		 * get the instance fpInMyPersFile which is the fpInMyPersFile to the instance in the 
		 * attribute fpInMyPersFile file
		 */
		if(getPersFilesMap() == null) {
			init();
		}
		PersFiles lPersFiles = getPersFilesMap().get(pType);
		if(lPersFiles == null) {
			lPersFiles = createPersFiles(pType);
			getPersFilesMap().put(pType, lPersFiles);
		}

		/**
		 * trigger the open files watch. This class will close files if too many
		 * files are open at a time
		 */
		OpenFilesWatch.touch(lPersFiles);

		long                     lInstanceFp     = lPersFiles.instanceFpInfo.getInstanceFp(pInstanceName);
		long                     lNewInstanceFp  = lInstanceFp;
		TreeMap<String, Integer> lAttributeMap   = attributeIndexMap.get(pType);
		Integer                  lIndexObject    = lAttributeMap.get(pAttributeName);
		if(lIndexObject != null) {
			int                  lAttributeIndex = lIndexObject; 
			long                 lAttributeFp    = lPersFiles.attributeFpInfo.getAttributeFp(lInstanceFp, lAttributeIndex);
			if(lAttributeFp < 0) {
				lNewInstanceFp   = lPersFiles.attributeFpInfo.setAttributeFp(lInstanceFp, lAttributeIndex, lAttributeFp);
			}

			if(lInstanceFp != lNewInstanceFp) {
				lPersFiles.instanceFpInfo.setInstanceFp(pInstanceName, lNewInstanceFp);
			}

			/**
			 * determine the attribute type by evaluating the object class
			 */
			Class<?> lAttributeType = null;
			if(DUMMY_ATTRIBUTE_NAME.equals(pAttributeName)) {
				lAttributeType = String.class;
			} else {
				lAttributeType = reflectionUtil.getAttributeType(pType, pAttributeName, null);
			}
			if(lAttributeType != null) {
				TypeKey lTypeKey        = typeKeyMap.get(lAttributeType);

				long lNewAttributeFp = lAttributeFp;
				if(lTypeKey != null) {
					lNewAttributeFp = writePrimitiveValueToFile(lPersFiles.attributeFile, lAttributeFp, lTypeKey, pValue);
				} else {
					lNewAttributeFp = lPersFiles.attributeFile.writeObject(pType.getSimpleName(), pInstanceName, pAttributeName, lAttributeFp, pValue);
				}

				/**
				 * it is necessary to reset the attribute fpInMyPersFile in the respective file
				 */
				lPersFiles.attributeFpInfo.setAttributeFp(lNewInstanceFp, lAttributeIndex, lNewAttributeFp);

			} else {
				LogWrapper.e(LOGTAG, "writeValueToFile: could not determine type of attribute for:" + pType.getName()
						+ "." + pAttributeName);
			}
		}
	}

	/**
	 *
	 *
	 */
	public void delete() {
		close();
		if(getPersFilesMap() != null) {
			for(PersFiles lPersFiles : getPersFilesMap().values()) {
				try {
					lPersFiles.close();
				} catch (IOException e) {
					LogWrapper.e(LOGTAG, "delete: could not close:" + lPersFiles);
				}
			}
			persFilesMap = null;
			OpenFilesWatch.reset();
		}
		/**
		 * delete all the subdirs of root dir
		 */
		Vector<File> lFileList = FileUtil.find(new File(rootDir), ".+"); 
		for(File lSubDir : lFileList) {
			if(lSubDir.isDirectory()) {
				FileUtil.deleteDir(lSubDir);
			} else if(lSubDir.isFile()) {
				lSubDir.delete();
			}
		}
	}

	protected static synchronized String getRootDir() {
		return rootDir;
	}

	protected static synchronized void setRootDir(String pROOTDIR) {
		rootDir = pROOTDIR;
	}

	public HashMap<Class<? extends ADao>, PersFiles> getPersFilesMap() {
		return persFilesMap;
	}

	/**
	 *
	 * @param pB
	 *
	 */
	protected synchronized void setDefragmentationEnabled(boolean pB) {
		isDefragmentationEnabled = pB;
	}

	/**
	 *
	 * accessible for test purposes
	 */
	protected void close() {
		if(getPersFilesMap() != null) {
			for(PersFiles lPersFiles : getPersFilesMap().values()) {
				try {
					lPersFiles.close();
				} catch (IOException e) {
					LogWrapper.e(LOGTAG, "could not close pers files for:"+lPersFiles.attributeFile.getFilename() + " e:" + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 *
	 * @param pCategory
	 * @param pExportFile
	 *
	 */
	public void exportData(Category pCategory, File pExportFile) throws IOException {
		/**
		 * create a list of all ADao subclasses that have pCategory
		 */
		HashSet<Class<? extends ADao>> lCategoryTypeSet = new HashSet<Class<? extends ADao>>();
		for(Class<? extends ADao> lSubType : daoSubTypeSet) {
			ACategory lCategory = lSubType.getAnnotation(ACategory.class);
			if(lCategory != null) {
				Category lCategoryValue = lCategory.category();
				if(pCategory.equals(lCategoryValue)) {
					lCategoryTypeSet.add(lSubType);
				}
			}
		}

		/**
		 * create a temp file in which the data is stored
		 */
		File            lTempFile = File.createTempFile("droidFad.", ".zip");
		lTempFile.deleteOnExit();
		ZipOutputStream lZOS      = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(lTempFile)));
		/**
		 * create the type info file and store it in the zip file
		 */
		File                lTypeInfoFile = File.createTempFile("droidFad.", ".typeinfo.txt");
		lTypeInfoFile.deleteOnExit();
		writeTypeInfoFile(lTypeInfoFile, lCategoryTypeSet);
		BufferedInputStream lBIS          = new BufferedInputStream(new FileInputStream(lTypeInfoFile));
		String              lEntryName    = TYPEINFO_FILENAME;
		ZipEntry            lZipEntry     = new ZipEntry(lEntryName);
		lZOS.putNextEntry(lZipEntry);
		FileUtil.copy(lBIS, lZOS);
		lZOS.closeEntry();

		/**
		 * create an entry for every dao subtype that has to be exported
		 */
		for(Class<? extends ADao> lSubType : lCategoryTypeSet) {
			/**
			 * get the directory for the respective persistency files
			 */
			File   lTypeDir  = new File(rootDir, lSubType.getName());
			File[] lFileList = lTypeDir.listFiles();
			if(lFileList != null) {
				for(File lPersFile : lFileList) {
					lEntryName = lSubType.getName() + "/" + lPersFile.getName();
					lZipEntry  = new ZipEntry(lEntryName);
					lZOS.putNextEntry(lZipEntry);
					/**
					 * write the file to the zip out put stream
					 */
					lBIS = new BufferedInputStream(new FileInputStream(lPersFile));
					FileUtil.copy(lBIS, lZOS);
					lZOS.closeEntry();
				}
			}
		}
		lZOS.close();
		FileUtil.copy(lTempFile, pExportFile);
	}
	/**
	 * 
	 *
	 * @param pCategory
	 * @param pImportFile
	 * @return s set of simple names of ADao subclasses contained in pImportFile
	 * @throws IOException
	 *
	 */
	public HashSet<String> importData(File pImportFile) throws IOException {
		if(pImportFile == null) {
			throw new IOException("parameter pImportFile must not be null");
		}
		if(!pImportFile.exists() || !pImportFile.isFile()) {
			throw new IOException("parameter pImportFile has to be an existing file");
		}

		close();

		HashSet<String> lLoadedTypeSet = new HashSet<String>();
		ZipFile         lZipFile       = new ZipFile(pImportFile);
		/**
		 * check the type info file if type information is consistent with 
		 * loaded ADao subclasses
		 */
		String   lTypeInfoFileName = TYPEINFO_FILENAME;
		ZipEntry lEntry            = lZipFile.getEntry(lTypeInfoFileName);
		if(lEntry == null) {
			throw new IllegalArgumentException("pImportFile:" + pImportFile.getAbsolutePath() + 
					" does not contain type info file with name:" + lTypeInfoFileName);
		}
		InputStream          lIS       = lZipFile.getInputStream(lEntry);
		File                 lTempFile = File.createTempFile("droidFad.", "typeinfo.txt");
		lTempFile.deleteOnExit();
		BufferedOutputStream lBOS      = new BufferedOutputStream(new FileOutputStream(lTempFile));
		FileUtil.copy(lIS, lBOS);
		lBOS.close();	

		HashSet<Class<? extends ADao>> lSubClassSet = ClazzFinder.findSubclasses(ADao.class); 
		HashMap<Class<? extends ADao>, HashMap<String, Class<?>>> lInputTypeInfo = loadTypeInfoFile(lTempFile, lSubClassSet, false);

		String lErrorMessage = compareTypeInfoWithRuntimeTypes(lInputTypeInfo, false, false);
		if(!"".equals(lErrorMessage)) {
			throw new IOException(lErrorMessage);
		}
		for(Class<? extends ADao> lImportClass : lInputTypeInfo.keySet()) {
			lLoadedTypeSet.add(lImportClass.getSimpleName());
		}
		Enumeration<? extends ZipEntry> lEntries = lZipFile.entries();
		while(lEntries.hasMoreElements()) {
			lEntry            = lEntries.nextElement();
			String lEntryName = lEntry.getName();
			if(!lTypeInfoFileName.equals(lEntryName)) {
				File                lOutFile = new File(rootDir, lEntryName);
				lBOS                         = new BufferedOutputStream(new FileOutputStream(lOutFile));
				BufferedInputStream lBIS     = new BufferedInputStream(lZipFile.getInputStream(lEntry));
				FileUtil.copy(lBIS, lBOS);				
				lBOS.close();
			}
		}

		persFilesMap = null;

		return lLoadedTypeSet;
	}
}