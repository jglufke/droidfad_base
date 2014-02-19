/**
 * 
 */
package com.droidfad.data;

import java.util.Vector;

import com.droidfad.data.ACategory.Category;

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
 *   limitations under the License.<br>
 * -----------------------------------------------------------------------<br>
 *
 * class that is internally used to persist References. This class should not be 
 * used by the Application developer directly.
 */
@ACategory(category=Category.model)
public final class _Reference extends ADao {

	public enum Attributes implements AttributesEnum { RefId, Src, Trg }

	public _Reference(String pName) {
		super(pName);
	}
	
	/**
	 * 
	 *
	 * @param pReferenceId a referenceId
	 * @param pSourceName name of the source ADao object
	 * @param pTargetName name of the target ADao object
	 *
	 */
	public _Reference(short pReferenceId, String pSourceName, String pTargetName) {
		super(null);
		setRefId(pReferenceId);
		setSrc(pSourceName);
		setTrg(pTargetName);
	}
	
	public static _Reference getInstance(String pName) {
		return getInstanceImpl(_Reference.class, pName);
	}
	public static Vector<_Reference> getInstances() {
		return getInstancesImpl(_Reference.class);
	}
	/**
	 * get the reference id which is a descriptor for the ReferenceType
	 *
	 * @return
	 *
	 */
	@Persistent
	public short getRefId() {
		return super.getAttribute(Attributes.RefId);
	}
	public void setRefId(short pRefId) {
		super.setAttribute(Attributes.RefId, pRefId);
	}
	/**
	 * get the name of the source ADao object
	 *
	 * @return
	 *
	 */
	@Persistent
	public String getSrc() {
		return super.getAttribute(Attributes.Src);
	}
	/**
	 * set the name of the source ADao object
	 *
	 * @return
	 *
	 */
	public void setSrc(String pSrc) {
		super.setAttribute(Attributes.Src, pSrc);
	}
	/**
	 * get the name of the target ADao object
	 *
	 * @return
	 *
	 */
	@Persistent
	public String getTrg() {
		return super.getAttribute(Attributes.Trg);
	}
	/**
	 * set the name of the target ADao object
	 *
	 * @return
	 *
	 */
	public void setTrg(String pTrg) {
		super.setAttribute(Attributes.Trg, pTrg);
	}
	
	/* (non-Javadoc)
	 * @see com.droidfad.data.ADao#toString()
	 */
	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder(super.toString());
		lBuilder.append("id:").append(getRefId());
		lBuilder.append(" src:").append(getSrc());
		lBuilder.append(" trg:").append(getTrg());
		return lBuilder.toString();
	}
}
