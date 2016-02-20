//    RED - A Java Editor Library
//    Copyright (C) 2003  Robert Lichtenberger
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
package red.lineTree;

import java.util.*;

/** A mark in the line tree.
  * Marks float around in the text and can quickly give their position if needed.
  * For a mark with position x and insertions of length l at positions y the following results hold:
  * <UL>
  * <LI> y < x: x = x + l;
  * <LI> y >= x: x stays the same
  * </UL>
  * @author rli@chello.at
  * @tier API
  */ 
public class REDMark extends REDNode {
	public REDMark() {
		super();
	}
	
	public void setValue(Object value) {
		fValue= value;
	}
	
	public Object getValue() {
		return fValue;
	}

	public REDMark insertNew(REDMarkTreeData data, REDMarkTreeData myData, REDEdgeDataComparison comp, REDDistanceTreeFactory factory, Object value) {
		REDMark retVal = (REDMark) super.insertNew(data, myData, comp, factory);
		retVal.setValue(value);
		return retVal;
	}
	
	void collect(int myPos, ArrayList collection, int from, int to, Class cl, boolean excludeMyself) {
		if (myPos >= from && fLeft != null) {
			((REDMark) fLeft.fSon).collect(myPos - ((REDMarkTreeData) fLeft.fData).fPosition, collection, from, to, cl, false);
		}
		
		if (!excludeMyself && myPos >= from && myPos <= to && (cl == null || cl.isInstance(fValue))) {
			collection.add(this);
		}
		
		if (myPos <= to && fRight != null) {
			((REDMark) fRight.fSon).collect(myPos + ((REDMarkTreeData) fRight.fData).fPosition, collection, from, to, cl, false);
		}
	}
	
	REDMark find(int myPos, int pos, boolean left, Class cl, boolean excludeMyself) {
		REDMark retVal = null;
		
		if (left) {
			if (myPos <= pos && fRight != null) {
				retVal = ((REDMark) fRight.fSon).find(myPos + ((REDMarkTreeData) fRight.fData).fPosition, pos, left, cl, false);
			}
			if (!excludeMyself && retVal == null && myPos <= pos && (cl == null || cl.isInstance(fValue))) {
				retVal = this;
			}
			if (retVal == null && fLeft != null) {
				retVal = ((REDMark) fLeft.fSon).find(myPos - ((REDMarkTreeData) fLeft.fData).fPosition, pos, left, cl, false);
			}	
		}
		else {
			if (myPos >= pos && fLeft != null) {
				retVal = ((REDMark) fLeft.fSon).find(myPos - ((REDMarkTreeData) fLeft.fData).fPosition, pos, left, cl, false);
			}
			if (!excludeMyself && retVal == null && myPos >= pos && (cl == null || cl.isInstance(fValue))) {
				retVal = this;
			}
			if (retVal == null && fRight != null) {
				retVal = ((REDMark) fRight.fSon).find(myPos + ((REDMarkTreeData) fRight.fData).fPosition, pos, left, cl, false);	
			}
		}
		
		return retVal;
	}
	
	public int getPosition() {
		REDMarkTreeData data = new REDMarkTreeData();
		getPosition(data);
		return data.fPosition;
	}

	Object fValue;
}
