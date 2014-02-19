/**
 * 
 */
package com.droidfad.persistency;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

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
interface IFpEntry<K> {

	public K getId();

	public long getFpInMyPersFile();
	
	public boolean isValid();
	public void setValid(boolean pB);
		
	public long read(long pFp, DataInputStream pDIS, byte[] pBuf) throws IOException;
	
	public void writeToFile(RandomAccessFile pRandomAccessFile) throws IOException;
}
