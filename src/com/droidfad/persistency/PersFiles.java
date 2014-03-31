package com.droidfad.persistency;

import java.io.IOException;

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

 * members and types to organize the fpInMyPersFile info files which hold the actual
 * persistent data
 *
 */
class PersFiles {
	AttributeFile   attributeFile;
	AttributeFpInfo attributeFpInfo;
	InstanceFpInfo  instanceFpInfo;

	private static long instanceCount = 0;
	private        long id            = -1;

	public PersFiles() {
		id = instanceCount++;
	}

	public AttributeFile getAttributeFile() {
		return attributeFile;
	}
	public AttributeFpInfo getAttributeFpInfo() {
		return attributeFpInfo;
	}
	public InstanceFpInfo getInstanceFpInfo() {
		return instanceFpInfo;
	}
	
	public void close() throws IOException {
		attributeFile.close();
		attributeFpInfo.closeRandomAccessFile();
		instanceFpInfo.closeRandomAccessFile();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PersFiles other = (PersFiles) obj;
		if (id != other.id)
			return false;
		return true;
	}
}