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

/** This edge data is associated with REDEdges in mark trees.
  * @author rli@chello.at
  * @see REDEdge
  * @see REDNode
  * @see REDMarkTree
  * @tier system
  */
public class REDMarkTreeData extends REDEdgeData {
	public REDMarkTreeData() {
		super();
	}
	
	public REDMarkTreeData(int position) {
		super();
		fPosition = position;
	}
	
	/** add values of another data object
	  * @param op data to add
	  * @pre op != null
	  * @post fPosition == fPosition@pre + op.fPosition
	  * @post fLine == fLine@pre + op.fLine
	  */
	public void add(REDEdgeData op) {
		REDMarkTreeData ltop = (REDMarkTreeData) op;
		fPosition += ltop.fPosition;
	}
	
	/** subtract values of another data object
	  * @param op data to subtract
	  * @pre op != null
	  * @post fPosition == fPosition@pre - op.fPosition
	  * @post fLine == fLine@pre - op.fLine
	  */
	public void sub(REDEdgeData op) {
		REDMarkTreeData ltop = (REDMarkTreeData) op;
		fPosition -= ltop.fPosition;
	}
	
	/** clone data
	  * @post return.equals(this)
	  */
	protected Object clone() {
		return new REDMarkTreeData(fPosition);
	}

	int fPosition;
	
	public static REDMarkTreeComparison fgComparison = new REDMarkTreeComparison();
}

/** Comparison function for edge data in REDMarkTree */
class REDMarkTreeComparison implements REDEdgeDataComparison {
	public boolean lt(REDEdgeData d1, REDEdgeData d2) {
		if (d1 instanceof REDMarkTreeData && d2 instanceof REDMarkTreeData) {
			return ((REDMarkTreeData) d1).fPosition < ((REDMarkTreeData) d2).fPosition;
		}
		return false;
	}

	public boolean leq(REDEdgeData d1, REDEdgeData d2) {
		if (d1 instanceof REDMarkTreeData && d2 instanceof REDMarkTreeData) {
			return ((REDMarkTreeData) d1).fPosition <= ((REDMarkTreeData) d2).fPosition;
		}
		return false;
	}
}
