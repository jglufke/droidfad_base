/**
 * 
 */
package com.droidfad.data;

import java.nio.ByteBuffer;

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
 */
class PersistencyUtil {

	private byte[][]      pathArray   = new byte[256][];
	private ByteBuffer    pathBuffer  = ByteBuffer.wrap(new byte[50]);

	public PersistencyUtil() {
		for(int i=0; i<256; i++) {
			pathArray[i] = ("" + i).getBytes();
		}
	}

	public String createFilename(String pRootDir, ADao pObject) {
		if(pObject == null) {
			throw new IllegalArgumentException("parameter pObject must not be null");
		}
		return createFilename(pRootDir, pObject.getType(), pObject.getName());
	}

	public String createFilename(String pRootDir, String pType, String pName) {
		if(pType == null) {
			throw new IllegalArgumentException("parameter pType must not be null");
		}
		if(pName == null) {
			throw new IllegalArgumentException("parameter pName must not be null");
		}
		StringBuilder lNameBuilder = new StringBuilder(50);
		lNameBuilder.append(pType);
		lNameBuilder.append('/');
		lNameBuilder.append(pName);
		lNameBuilder.append('/');
		String        lIdentifier  = lNameBuilder.toString();

		StringBuilder lPathBuilder = new StringBuilder(250);
		if(pRootDir != null) {
			// lPathBuilder.append('.').append('/');
			lPathBuilder.append(pRootDir);
			lPathBuilder.append('/');
		}
		lPathBuilder.append(createHashedFilepath(lIdentifier));
		lPathBuilder.append(lIdentifier);
		lNameBuilder.append('/');

		String lFileName = lPathBuilder.toString();
		return lFileName;
	}

	public String createHashedFilepath(String pIdentifier) {

		int           lHashCode = pIdentifier.hashCode();
		pathBuffer.position(0);
		pathBuffer.put(pathArray[0x00ff & (lHashCode >> 24)]);
		pathBuffer.put((byte) '/');
		pathBuffer.put(pathArray[0x00ff & (lHashCode >> 16)]);
		pathBuffer.put((byte) '/');
		pathBuffer.put(pathArray[0x00ff & (lHashCode >>  8)]);
		pathBuffer.put((byte) '/');
		pathBuffer.put(pathArray[0x00ff & lHashCode]);
		pathBuffer.put((byte) '/');

		return new String(pathBuffer.array(), 0, pathBuffer.position());
	}	


}