package com.droidfad.persistency;

import java.io.IOException;

/**
 * 
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