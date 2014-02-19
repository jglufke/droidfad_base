/**
 * 
 */
package com.droidfad.util;

import java.util.HashMap;

/**
 *
 * @author John
 * copyright Jens Glufke, Germany mailto:jglufke@gmx.de
 *
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
