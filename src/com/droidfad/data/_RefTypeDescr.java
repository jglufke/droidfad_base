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
 *   limitations under the License.
 * -----------------------------------------------------------------------<br>
 * this class implements a mapping from _Reference
 * Class to type name and id. This is done by instantiating
 * for every AReferenceType class an instance with name
 * - reference class name
 * - reference type name
 * - reference id
 * 
 * Should not be used by the developer directly to avoid unwanted side effects
 */
@ACategory(category=Category.model)
public class _RefTypeDescr extends ADao {

	public enum Attributes implements AttributesEnum { Clazz, RefType, Id }
	
	public _RefTypeDescr(String pName, String pClazz, String pType, short pId) {
		super(pName);
		setClazz(pClazz);
		setRefType(pType);
		setId(pId);
	}

	public _RefTypeDescr(String pName) {
		super(pName);
	}

	public static _RefTypeDescr getInstance(String pName) {
		return getInstanceImpl(_RefTypeDescr.class, pName);
	}
	public static Vector<_RefTypeDescr> getInstances() {
		return getInstancesImpl(_RefTypeDescr.class);
	}
	
	@Persistent
	public String getClazz() {
		return super.getAttribute(Attributes.Clazz);
	}
	public void setClazz(String pClazz) {
		super.setAttribute(Attributes.Clazz, pClazz);
	}

	@Persistent
	public String getRefType() {
		return super.getAttribute(Attributes.RefType);
	}
	public void setRefType(String pType) {
		super.setAttribute(Attributes.RefType, pType);
	}

	@Persistent
	public short getId() {
		return super.getAttribute(Attributes.Id);
	}
	public void setId(short pId) {
		super.setAttribute(Attributes.Id, pId);
	}
	
	/* (non-Javadoc)
	 * @see com.droidfad.data.ADao#toString()
	 */
	@Override
	public String toString() {
		StringBuilder lBuilder = new StringBuilder(super.toString());
		lBuilder.append(" tp:");
		lBuilder.append(getType());
		lBuilder.append(" id:");
		lBuilder.append(getId());
		return lBuilder.toString();
	}
}
