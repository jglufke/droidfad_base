/**
 * 
 */
package com.droidfad.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

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
 */
public class SystemCall {

	private static final String          LOGTAG = SystemCall.class.getSimpleName();
	private static final Timer           timer  = new Timer();

	public static boolean execute(final String pCommand) {
		return execute(pCommand, -1, true);
	}
	/**
	 * 
	 * executes a system call. If the call is not finished within pTimeoutMSec
	 * an IOException is thrown. An IOException is also thrown if the system
	 * call is finieshed with an error
	 * 
	 * @param pCommand
	 * @param pTimeoutMSec
	 * @param pIsSynchroneous 
	 * @return
	 * @throws IOException
	 *
	 */
	public static boolean execute(final String pCommand, final long pTimeoutMSec, boolean pIsSynchroneous) {
		boolean lSuccess = false;

		Process lProcess = null;
		try {
			lProcess = Runtime.getRuntime().exec(pCommand);
		} catch (IOException e) {
			LogWrapper.e(LOGTAG, "could not execute:" + pCommand, e);
		}
		if(lProcess != null) {
			class ProcessDestroyer extends TimerTask {
				Process process;
				public ProcessDestroyer(Process pProcess) {
					process = pProcess;
				}
				@Override
				public void run() {
					process.destroy();
					LogWrapper.e(LOGTAG, "process timedout:" + pCommand);
				}
			}
			ProcessDestroyer lDestroyer = null;

			if(pTimeoutMSec > 0) {
				lDestroyer = new ProcessDestroyer(lProcess);
				timer.schedule(lDestroyer, pTimeoutMSec);
			}
			if(pTimeoutMSec > 0 || pIsSynchroneous) {
				try {
					int lWaitFor = lProcess.waitFor();
					lSuccess     = lWaitFor == 0;
					if(lDestroyer != null) {
						lDestroyer.cancel();
					}
					if(!lSuccess) {
						InputStream           lErrorStream = lProcess.getErrorStream();
						ByteArrayOutputStream lOutStream   = new ByteArrayOutputStream();

						FileUtil.copy(lErrorStream, lOutStream);
						LogWrapper.e(LOGTAG, "command execution failed:" + pCommand + " failure:" + lOutStream.toString("UTF-8"));
					}
				} catch (InterruptedException e) {
					LogWrapper.e(LOGTAG, "could not execute:" + pCommand, e);
				} catch (IOException e) {
					LogWrapper.e(LOGTAG, "could not execute:" + pCommand, e);
				}
			}
		}
		return lSuccess;
	}
}
