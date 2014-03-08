package com.droidfad.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
 *   limitations under the License.
 * -----------------------------------------------------------------------<br>
 * 
 * class that can be used to operations on files like copy, deleteDir etc 
 */
public class FileUtil {

	private static final String LOGTAG = FileUtil.class.getSimpleName();

	private static final String TEMP_DIR_NAME = "/TEMP/";

	private static String javaVersion;

	static {
		javaVersion = System.getProperty("java.specification.version");
	}

	/**
	 * 
	 * @param pSourceFile
	 * @param pTargetFile
	 * @throws IOException
	 */
	public static synchronized void copy(File pSourceFile, File pTargetFile) throws IOException {

		if(!pSourceFile.getCanonicalPath().equals(pTargetFile.getCanonicalPath())) {

			pTargetFile.getParentFile().mkdirs();

			FileInputStream  lFIS = new FileInputStream(pSourceFile);
			FileOutputStream lFOS = new FileOutputStream(pTargetFile.getCanonicalPath());
			copy(lFIS, lFOS);
			lFOS.close();
		}
	}

	/**
	 * destination is not closed on exit of method !
	 * @param pSourceStream
	 * @param pTargetStream
	 * @throws IOException
	 */
	public static synchronized void copy(InputStream pSourceStream, OutputStream pTargetStream) throws IOException {
		copy(pSourceStream, pTargetStream, true);
	}

	/**
	 * destination is not closed on exit of method !
	 * @param pSourceStream
	 * @param pTargetStream
	 * @param closeSource define if the source id closed after copying
	 * @throws IOException
	 */
	public static synchronized void copy(InputStream pSourceStream, OutputStream pTargetStream, boolean closeSource) throws IOException {

		final int    buffSize = 1024;
		final byte[] lBuf     = new byte[buffSize];

		int lBytesRead = 0;
		while((lBytesRead = pSourceStream.read(lBuf, 0, buffSize)) != -1) {
			pTargetStream.write(lBuf, 0, lBytesRead);
		}
		if(closeSource) {
			pSourceStream.close();
		}
	}

	/**
	 * 
	 * @param pSourceFile
	 * @param pTargetFile
	 * @throws IOException
	 */
	public static synchronized void copy(String pSourceFile, String pTargetFile) throws IOException {

		copy(new File(pSourceFile), new File(pTargetFile));
	}

	/**
	 * 
	 * @param pFilename
	 * @return
	 */
	public static synchronized File createTempFile(String pFilename) {
		String lPrefix   = TEMP_DIR_NAME + pFilename;
		int    lSuffix   = 1;
		String lFilename = lPrefix;
		File   lTempFile = null;
		while((lTempFile = new File(lFilename)).exists()) {
			lFilename = lPrefix + lSuffix++;
		}
		lTempFile.getParentFile().mkdirs();
		lTempFile.deleteOnExit();

		return lTempFile;
	}
	public static synchronized File createTempDir() {
		/**
		 * get the name
		 */
		String lFilename = "tempDir";
		File   lTempDir  = createTempFile(lFilename);
		if(!lTempDir.mkdir()) {
			throw new RuntimeException("could not create dir:" + lTempDir.getAbsolutePath());
		}
		return lTempDir;
	}
	/**
	 * 
	 * find a file in pDir and its sub directories that matches pFilter
	 * @param pFoundFiles
	 * @param pDir
	 * @param pFilter
	 * @return
	 *
	 */
	public static Vector<File> find(Vector<File> pFoundFiles, File pDir, FileFilter pFilter)
	{
		if(pDir.isDirectory()) {
			File[] lFoundFiles = pDir.listFiles(pFilter);
			if(lFoundFiles != null && lFoundFiles.length > 0) {
				pFoundFiles.addAll(Arrays.asList(lFoundFiles ));
			}
			for(File lFile : pDir.listFiles()) {
				find(pFoundFiles, lFile, pFilter);
			}
		}
		return pFoundFiles;
	}

	/**
	 * find all files in pDirectory and its sub directories whose absolute path
	 * matches pRegex
	 * @param pDirectory
	 * @param pRegex
	 * @return
	 */
	public static synchronized Vector<File> find(File pDirectory, String pRegex)
	{
		Vector<File> lReturn = new Vector<File>();
		lReturn = find(lReturn, pDirectory, new RegExpFileFilter(pRegex));
		return lReturn;
	}

	/**
	 * 
	 * @param pFile
	 * @return
	 */
	public  static String getCanonicalPath(File pFile) {

		String lReturnPath = pFile.getAbsolutePath();
		lReturnPath = lReturnPath.replaceAll("\\\\", "/");

		final String  regExp1       = "()[^/]+/(\\.\\.)"; 
		final Pattern pattern1      = Pattern.compile(regExp1);
		final String  regExp2       = "([^/]+/)(\\.)"; 
		final Pattern pattern2      = Pattern.compile(regExp2);
		final String  regExp3       = "(/)(/)"; 
		final Pattern pattern3      = Pattern.compile(regExp3);

		for(Pattern lPattern : new Pattern[]{ pattern1, pattern2, pattern3 }) {
			Matcher lMatcher = lPattern.matcher(lReturnPath);
			while(lMatcher.find()) {
				lReturnPath   = lMatcher.replaceFirst(lMatcher.group(1));
				lMatcher = lPattern.matcher(lReturnPath);
			}
		}
		if(lReturnPath.endsWith("/")) {
			lReturnPath = lReturnPath.substring(0, lReturnPath.length()-1);
		}
		return lReturnPath;
	}

	/**
	 * check if the canonical path of pFile1 equals the one
	 * of pFile2
	 * @param pFile1
	 * @param pFile2
	 * @return
	 */
	public static boolean isEqual(File pFile1, File pFile2) {
		return getCanonicalPath(pFile1).equals(getCanonicalPath(pFile2));
	}
	/**
	 * checks if the string representation of the Java version of 
	 * the current runtime equals pVersion
	 *
	 * @param pVersion
	 * @return
	 *
	 */
	public static boolean isJavaVersion(String pVersion) {
		return pVersion.trim().equalsIgnoreCase(javaVersion);
	}

	/**
	 * 
	 * @param pSourceDirName
	 * @param pTargetDirName
	 */
	public synchronized void copyDir(String pSourceDirName, String pTargetDirName) {
		Vector<File> lFiles = find(new File(pSourceDirName), ".+");
		if(!lFiles.isEmpty()) {

			/**
			 * create the target directory
			 */
			File lTargetDir = new File(pTargetDirName);
			lTargetDir.mkdirs();
			pTargetDirName = pTargetDirName.replaceAll("\\\\", "/");

			for(File lFile : lFiles) {
				String lTargetFilename = lFile.getPath().replaceAll("\\\\", "/");
				/**
				 * remove sourceDirname 
				 */
				lTargetFilename = lTargetFilename.substring(pSourceDirName.length());
				lTargetFilename = pTargetDirName + "/" + lTargetFilename;
				// System.out.println("copy " + lFile.getPath() + " to " + lTargetFilename);
				try {
					//  System.out.println("copy " + lFile.getPath() + " to " + lTargetFilename);
					copy(lFile, new File(lTargetFilename));
				} catch (IOException e) {
					System.err.println("FAILED to copy " + lFile.getPath() + " to " + lTargetFilename);
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * 
	 * @param pDirectory
	 */
	public static void deleteDir(File pDirectory) {
		if(pDirectory.exists()) {
			if(pDirectory.isDirectory()) {
				for(File lFile : pDirectory.listFiles()) {
					if(!lFile.equals(pDirectory.getParentFile())) {
						deleteDir(lFile);
					}
				}
			}
			pDirectory.delete();
		}
	}

	/**
	 * 
	 * @return
	 */
	public File getTempDir() {
		String lTempDir = System.getProperty("java.io.tmpdir");
		if(lTempDir == null) {
			lTempDir = "./tmp/";

		}
		File lReturn = new File(lTempDir);
		return lReturn;
	}

	/**
	 * 
	 * @param pIS
	 * @param pSize
	 * @return
	 */
	public byte[] readInputStream(InputStream pIS, int pSize) {

		ByteArrayOutputStream lBOS = new ByteArrayOutputStream((pSize > 0) ? pSize + 1024 : 10240);
		int    lBufSize   = 1024;
		byte[] lBuffer    = new byte[lBufSize];
		int    lReadBytes = 0;

		try {
			while((lReadBytes = pIS.read(lBuffer, 0, lBufSize)) != -1) {
				lBOS.write(lBuffer, 0, lReadBytes);
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return lBOS.toByteArray();
	}

	public static void zipDirectory(File p2BeZippedDir, File pZipFile) {
		if(p2BeZippedDir == null) {
			throw new IllegalArgumentException("parameter p2BeZippedDir must not be null");
		}
		if(!p2BeZippedDir.isDirectory()) {
			throw new IllegalArgumentException("parameter p2BeZippedDir has to be a directory");
		}
		if(pZipFile == null) {
			throw new IllegalArgumentException("parameter pZipFile must not be null");
		}
		if(pZipFile.isDirectory()) {
			throw new IllegalArgumentException("parameter pZipFile has to be a file and not a directory");
		}
		Vector<File> lFileList = find(p2BeZippedDir, ".+");
		String lDirName   = p2BeZippedDir.getAbsolutePath();
		int    lBegin     = lDirName.length()+1;
		String lEntryName = null;
		try {
			boolean lIsEntryWritten = false;
			pZipFile.delete();
			FileOutputStream     lFOS = new FileOutputStream(pZipFile);
			BufferedOutputStream lBOS = new BufferedOutputStream(lFOS);
			ZipOutputStream      lZOS = new ZipOutputStream(lBOS);

			for(File lFile : lFileList) {
				if(lFile.isFile()) {
					lIsEntryWritten    = true;
					lEntryName         = lFile.getAbsolutePath();
					lEntryName         = "/" + lEntryName.substring(lBegin);
					lEntryName         = lEntryName.replaceAll("\\\\", "/");
					ZipEntry lZipEntry = new ZipEntry(lEntryName);
					lZOS.putNextEntry(lZipEntry);

					FileInputStream     lFIS = new FileInputStream(lFile);
					BufferedInputStream lBIS = new BufferedInputStream(lFIS);

					copy(lBIS, lZOS);
					lZOS.closeEntry();
				}
			}
			if(!lIsEntryWritten) {
				lZOS.putNextEntry(new ZipEntry("ZipFileIsEmpty"));
				lZOS.closeEntry();
			}
			lZOS.close();

		} catch(ZipException e) {
			throw new RuntimeException("could not zip entry:" + lEntryName, e);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 
	 *
	 * @param pZipFile
	 * @return the list of unzipped files which where partialTopic of pZipFile
	 *
	 */
	public static Vector<String> unzip(File pZipFile) {
		/**
		 * create a temp directory
		 */
		File lTempDir = createTempDir();
		return unzipDirectory(lTempDir, pZipFile);
	}

	/**
	 *
	 *
	 * @param p2BeUnzippedDir
	 * @param pZipFile
	 * @return the list of unzipped file names
	 *
	 */
	public static Vector<String> unzipDirectory(File p2BeUnzippedDir, File pZipFile) {
		if(p2BeUnzippedDir == null) {
			throw new IllegalArgumentException("parameter p2BeUnzippedDir must not be null");
		}
		if(!p2BeUnzippedDir.isDirectory()) {
			throw new IllegalArgumentException("parameter p2BeUnzippedDir has to be a directory");
		}
		if(pZipFile == null) {
			throw new IllegalArgumentException("parameter pZipFile must not be null");
		}
		if(pZipFile.isDirectory()) {
			throw new IllegalArgumentException("parameter pZipFile has to be a file and not a directory");
		}
		Vector<String> lFilenameList = new Vector<String>();
		try {
			ZipFile lZipFile = new ZipFile(pZipFile);
			Enumeration<? extends ZipEntry> lEntryEnum = lZipFile.entries();
			while(lEntryEnum.hasMoreElements()) {
				ZipEntry             lEntry    = lEntryEnum.nextElement();
				InputStream          lIS       = lZipFile.getInputStream(lEntry);
				BufferedInputStream  lBIS      = new BufferedInputStream(lIS);
				String               lFilename = p2BeUnzippedDir.getAbsolutePath() + "/" + lEntry.getName();
				new File(lFilename).getParentFile().mkdirs();
				FileOutputStream     lFOS      = new FileOutputStream(lFilename);
				BufferedOutputStream lBOS      = new BufferedOutputStream(lFOS);
				FileUtil.copy(lBIS, lBOS);
				lBOS.close();
				lFilenameList.add(lFilename);
			}
			lZipFile.close();
		} catch (IOException lE) {
			LogWrapper.e(LOGTAG, "can not unzip file:" + pZipFile.getAbsolutePath()+ " to:" + p2BeUnzippedDir);
		}
		return lFilenameList;
	}
}


/**
 * 
 * ************************************************************<br>
 *
 * @author glufkeje
 *
 * 
 * <br>************************************************************<br>
 */
class RegExpFileFilter implements FileFilter {
	private Pattern pattern = null;
	public RegExpFileFilter(String regex) {
		pattern = Pattern.compile(regex);
	}
	public boolean accept(File pathname)
	{
		return pattern.matcher(pathname.getAbsolutePath()).matches();
	}

};
