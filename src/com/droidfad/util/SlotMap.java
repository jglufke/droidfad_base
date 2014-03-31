/**
 * 
 */
package com.droidfad.util;

import java.util.HashMap;

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
public class SlotMap<V> extends HashMap<Long, V> {

	private static final long serialVersionUID = 1L;
	private static final boolean DEBUG = false;
	private long slotSize;

	public SlotMap(long pSlotSize) {
		slotSize = pSlotSize;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(Long pKey, V pValue) {
		if(DEBUG) System.err.print("put:" + pKey + ":");
		return super.put(calcSlot(pKey), pValue);
	}

	public long calcSlot(Object pKey) {
		long lSlot = 0;
		if(pKey != null && (pKey instanceof Long)) {
			long    lLongKey  = (Long) pKey;
			boolean lNegative = lLongKey < 0;

			lLongKey      -= (lLongKey % slotSize);
			lSlot          = lLongKey;
			
			if(lNegative) {
				lSlot -= slotSize;
			}
		}
		if(DEBUG) System.err.println("slot:" + lSlot);
		return lSlot;
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object pKey) {
		return super.remove(calcSlot(pKey));
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
	@Override
	public V get(Object pKey) {
		if(DEBUG) System.err.print("get:" + pKey + ":");
		return super.get(calcSlot(pKey));
	}
}
