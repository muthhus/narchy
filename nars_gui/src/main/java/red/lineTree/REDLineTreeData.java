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

/** Edge data associated with REDEdges.
  * @author rli@chello.at
  * @see REDEdge
  * @see REDNode
  * @see REDLineTree
  * @tier system
  */
public class REDLineTreeData extends REDEdgeData {
	public REDLineTreeData() {
		super();
		fPosition = 0;
		fLine = 0;
	}
	
	public REDLineTreeData(int position, int line) {
		super();
		fPosition = position;
		fLine = line;
	}
	
	/** add values of another data object
	  * @param op data to add
	  * @pre op != null
	  * @post fPosition == fPosition@pre + op.fPosition
	  * @post fLine == fLine@pre + op.fLine
	  */
	public void add(REDEdgeData op) {
		REDLineTreeData ltop = (REDLineTreeData) op;
		fPosition += ltop.fPosition;
		fLine += ltop.fLine;
	}
	
	/** subtract values of another data object
	  * @param op data to subtract
	  * @pre op != null
	  * @post fPosition == fPosition@pre - op.fPosition
	  * @post fLine == fLine@pre - op.fLine
	  */
	public void sub(REDEdgeData op) {
		REDLineTreeData ltop = (REDLineTreeData) op;
		fPosition -= ltop.fPosition;
		fLine -= ltop.fLine;
	}
	
	public int getPosition() {
		return fPosition;
	}
	
	public int getLine() {
		return fLine;
	}
	
	/** clone data
	  * @post return.equals(this)
	  */
	protected Object clone() {
		return new REDLineTreeData(fPosition, fLine);
	}
	
	/** Acquire data object from pool.
	  * If pool is empty, a new object will be created
	  * @return A REDLineTreeData object, initialized to 0/0
	  */
	public static REDLineTreeData acquireFromPool() {
		try {
			return (REDLineTreeData) fgPool.pop();
		}
		catch (EmptyStackException ese) {
			return new REDLineTreeData();
		}
	}
	
	/** Acquire data object from pool.
	  * If pool is empty, a new object will be created
	  * @param position The position the acquired data should have.
	  * @param line The line the acquired data should have.
	  * @return A REDLineTreeData object, initialized to position/line
	  */
	public static REDLineTreeData acquireFromPool(int position, int line) {
		REDLineTreeData data = acquireFromPool();
		data.fPosition = position;
		data.fLine = line;
		return data;
	}

	/** Release data object into pool.
	  * The object does not necessarily have to be acquired from the pool.
	  * @param The REDLineTreeData object to be released into the pool for future usage. Will be set to 0/0 by this method.
	  */
	public static void releaseIntoPool(REDLineTreeData data) {
		data.fPosition = 0; data.fLine = 0;
		fgPool.push(data);
	}	


	public static PositionComparison fgPositionComparison = new PositionComparison();
	public static LineComparison fgLineComparison = new LineComparison();
	
	int fPosition;
	int fLine;
	static Stack fgPool;		// LineTreeData pool for reduced memory usage fluctuation	
	static {
		fgPool = new Stack();
	}
}

/** Comparison function for edge data in REDLineTree, using positions */
class PositionComparison implements REDEdgeDataComparison {
	public boolean lt(REDEdgeData d1, REDEdgeData d2) {
		if (d1 instanceof REDLineTreeData && d2 instanceof REDLineTreeData) {
			return ((REDLineTreeData) d1).fPosition < ((REDLineTreeData) d2).fPosition;
		}
		return false;
	}

	public boolean leq(REDEdgeData d1, REDEdgeData d2) {
		if (d1 instanceof REDLineTreeData && d2 instanceof REDLineTreeData) {
			return ((REDLineTreeData) d1).fPosition <= ((REDLineTreeData) d2).fPosition;
		}
		return false;
	}
}

/** Comparison function for edge data in REDLineTree, using lines */
class LineComparison implements REDEdgeDataComparison {
	public boolean lt(REDEdgeData d1, REDEdgeData d2) {
		if (d1 instanceof REDLineTreeData && d2 instanceof REDLineTreeData) {
			return ((REDLineTreeData) d1).fLine < ((REDLineTreeData) d2).fLine;
		}
		return false;
	}

	public boolean leq(REDEdgeData d1, REDEdgeData d2) {
		if (d1 instanceof REDLineTreeData && d2 instanceof REDLineTreeData) {
			return ((REDLineTreeData) d1).fLine <= ((REDLineTreeData) d2).fLine;
		}
		return false;
	}
}
