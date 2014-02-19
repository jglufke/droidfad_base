/**
 * 
 */
package com.droidfad.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * a simple file logger 
 * @author John
 * copyright Jens Glufke, Germany mailto:jglufke@gmx.de
 *
 */
public class SimpleFileLog {

	private File             logDir;
	private String           logFilename;
	private int              maxSize;
	private int              maxNrOfLogFiles;
	private PrintStream      logOutputStream = null;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS ");
	
	public SimpleFileLog(File pLogDir, String pLogFilename, int pMaxSize, int pMaxNrOfLogFiles) {
		super();

		if(pLogDir == null) {
			throw new IllegalArgumentException("parameter pLogDir must not be null");
		}
		if(pLogDir.exists() && !pLogDir.isDirectory()) {
			throw new IllegalArgumentException("parameter pDir must be a directory");
		}
		if(pLogFilename == null) {
			throw new IllegalArgumentException("parameter pLogFilename must not be null");
		}
		if(pMaxSize<1000 || pMaxSize>100000) {
			throw new IllegalArgumentException("pMaxSize must be between 1000 and 100000");
		}
		if(pMaxNrOfLogFiles < 1 || pMaxNrOfLogFiles>10) {
			throw new IllegalArgumentException("pMaxNrOfLogFiles must be between 1 and 10");
		}

		logDir          = pLogDir;
		logFilename     = pLogFilename;
		maxSize         = pMaxSize;
		maxNrOfLogFiles = pMaxNrOfLogFiles;
	}

	public void info(String pMessage) {
		info(pMessage, null);
	}
	public void info(String pMessage, Throwable pThrowable) {
		File lLogFile = checkLogFile();
		writeLog(lLogFile, pMessage, pThrowable);
	}

	/**
	 * @return 
	 *
	 *
	 */
	private File checkLogFile() {
		File lLogFile = new File(logDir, logFilename);
		if(lLogFile.exists() && lLogFile.length()>=maxSize) {
			swapLogFiles();
		}
		return lLogFile;
	}

	/**
	 *
	 *
	 */
	private void swapLogFiles() {

		if(logOutputStream != null) {
			logOutputStream.close();
			logOutputStream = null;
		}

		for(int i=maxNrOfLogFiles-1; i>0; i--) {
			File lFile = new File(logDir, logFilename + "." + i);
			if(lFile.exists() && lFile.isFile()) {
				File lNewFile = new File(logDir, logFilename + "." + (i+1));
				if(lNewFile.exists()) {
					lNewFile.delete();
				}
				if(!lFile.renameTo(lNewFile)) {
					try {
						FileUtil.copy(lFile, lNewFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				lFile.delete();
			}
		}
	}

	/**
	 *
	 * @param pLogFile 
	 * @param pMessage
	 * @param pThrowable
	 *
	 */
	private void writeLog(File pLogFile, String pMessage, Throwable pThrowable) {
		try {
			
			if(logOutputStream == null) {
				logOutputStream = new PrintStream(new FileOutputStream(pLogFile, true));
			}
			logOutputStream.print(dateFormat.format(new Date()));
			logOutputStream.println(pMessage);
			logOutputStream.flush();
			
			if(pThrowable != null) {
				pThrowable.printStackTrace(logOutputStream);
			}

		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		if(logOutputStream != null) {
			logOutputStream.close();
		}
	}
}
