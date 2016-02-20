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

/** Line Tree data structure. This class is not for the fainthearted. Read the concept paper (TWiki) to understand how it works
  * @author rli@chello.at
  * @invariant testClassInvariant()
  * @invariant fRoot != null
  * @invariant fRoot.fLeft == null
  * @tier system
  */
public class REDLineTree extends REDDistanceTree {
	public REDLineTree() {
		super(new REDLineTreeFactory());
	}
	
	private	void insertBalanced(int lb, int ub, ArrayList v, int lineOffset, REDLineTreeData nullData) {
		int i;
		if (lb >= ub) return;
		i = (lb + ub) / 2;
		REDLineTreeData data = (REDLineTreeData) v.get(i); data.fLine += lineOffset;
		fRoot.insertNew(data, nullData, REDLineTreeData.fgPositionComparison, fFactory);
		nullData.fPosition = 0; nullData.fLine = 0;
		insertBalanced(i+1, ub, v, lineOffset, nullData);
		insertBalanced(lb, i, v, lineOffset, nullData);
	}
		
	/** insertion notification routine - called by client upon changes 
	 * @param pos position where the insertion begins
	 * @param totalLength length of whole insertion
	 * @param v This vector contains an REDLineTreeData element for the distance of each line to its predecessor, starting with line nr. 1; may be null
	 * @pre v.size() > 0
	 */
	public void notifyInsert(int pos, int totalLength, ArrayList v) {
		int size = (v == null) ? 0 : v.size() - 1;
		REDLineTreeData data = new REDLineTreeData(totalLength, size);
		REDLineTreeData start = (REDLineTreeData) modifyEdgesAfterInsertion(new REDLineTreeData(pos, 0), REDLineTreeData.fgPositionComparison, data);
		if (v != null) {
			insertBalanced(0, size, v, start.fLine, new REDLineTreeData());
			fNrNodes += size;
		}
	}

	/** deletion notification routine - called by client upon changes 
	 * @param from position where the deletion begins
	 * @param to position where the deletion ends
	 * @param deleteNodes If this parameter is true, nodes within [from, to] are deleted. Otherwise they are just moved to from.
	 */
	public void notifyDelete(int from, int to) {
		int nrNodesDeleted = 0;
		REDLineTreeData fromData = new REDLineTreeData(from, 0);
		REDLineTreeData toData = new REDLineTreeData(to, 0);
		REDLineTreeData nullData = new REDLineTreeData();
		while (fRoot.delete(fromData, toData, REDLineTreeData.fgPositionComparison, nullData, fFactory)) {
			nrNodesDeleted++;
		}
		REDLineTreeData data = new REDLineTreeData(from - to, -1 * nrNodesDeleted);
		REDLineTreeData start = (REDLineTreeData) modifyEdgesAfterInsertion(new REDLineTreeData(from, 0), REDLineTreeData.fgPositionComparison, data);
		fNrNodes -= nrNodesDeleted;
	}

	
	/** Iterate over the tree in order (i.e. left, this, right) and call iterator for each node 
	  * @param iterator Iterator to call for each visited node.
	  * @pre iterator != null
	  */
	public void iterateInOrder(REDDistanceTreeIterator iterator) {
		iterateInOrder(iterator, null, null, null);
	}
	
	/** Iterate over the tree in order (i.e. left, this, right) and call iterator for each node 
	  * @param iterator Iterator to call for each visited node
	  * @param lowerBound A node with data d in the tree is only visited if d >= lowerBound. May be null in which case there is no lower limit to visited nodes.
	  * @param upperBound  A node with data d in the tree is only visited if d <= upperBound. May be null in which case there is no upper limit to visited nodes.
	  * @param comparison The comparison algorithm to be used when checking nodes against the given lower/upperBound. Must not be null, if either lowerBound or upperBound != null
	  * @pre iterator != null
	  */
	public void iterateInOrder(REDDistanceTreeIterator iterator, REDLineTreeData lowerBound, REDLineTreeData upperBound, REDEdgeDataComparison comparison) {
		REDLineTreeData nullData = new REDLineTreeData();
		fRoot.iterateInOrder(iterator, nullData, 0, lowerBound, upperBound, comparison);
	}

	public int getLineStart(int lineNr) {
		REDLineTreeData result = REDLineTreeData.acquireFromPool();
		REDLineTreeData needle = REDLineTreeData.acquireFromPool(0, lineNr);
		fRoot.findNode(needle, REDLineTreeData.fgLineComparison, result);
		int res = result.fPosition;
		REDLineTreeData.releaseIntoPool(result);
		REDLineTreeData.releaseIntoPool(needle);
		return res;
	}
	
	public int getLineForPosition(int pos) {
		REDLineTreeData result = REDLineTreeData.acquireFromPool();
		REDLineTreeData needle = REDLineTreeData.acquireFromPool(pos, 0);
		fRoot.findNode(needle, REDLineTreeData.fgPositionComparison, result);
		int res = result.fLine;
		REDLineTreeData.releaseIntoPool(result);
		REDLineTreeData.releaseIntoPool(needle);
		return res;
	}
	
//	// --- DEBUG CODE ---
//	/** tests node invariants */	
//	class NodeInvariantChecker implements REDDistanceTreeIterator {
//		public void processNode(REDNode node, REDEdgeData data, int depth) {
//			fInvariantOk = fInvariantOk && (node.fUp == null || node.fUp.fSon == node);
//			fInvariantOk = fInvariantOk && (node.fLeft == null || node.fLeft.isLeftEdge());
//			fInvariantOk = fInvariantOk && (node.fLeft == null || node.fLeft.fFather == node);
//			fInvariantOk = fInvariantOk && (node.fRight == null || !node.fRight.isLeftEdge());
//			fInvariantOk = fInvariantOk && (node.fRight == null || node.fRight.fFather == node);
//			fInvariantOk = fInvariantOk && (node.fUp == null || Math.abs(node.fBalance) <= 1);
//			if (node.fLeft == null && node.fRight == null) {
//				fInvariantOk = fInvariantOk && node.fBalance == 0;
//			}
//			fInvariantOk = fInvariantOk && (node.fUp == null || getDepth(node.fLeft) - getDepth(node.fRight) == node.fBalance);
//		}
//		
//		private int getDepth(REDEdge edge) {
//			if (edge == null) {
//				return 0;
//			}
//			else {
//				return edge.fSon.getDepth();
//			}
//		}
//		boolean fInvariantOk;
//		{
//			fInvariantOk = true;
//		}
//	}
//
//	// DEBUG CODE
//	/** test class invariant of line tree */
//	private boolean testClassInvariant() {
//		NodeInvariantChecker nic = new NodeInvariantChecker();
//		iterateInOrder(nic);
//		return nic.fInvariantOk;
//	}
}

/** Distance tree factory for REDLineTree */
class REDLineTreeFactory implements REDDistanceTreeFactory {
	public REDNode createNode() {
		return new REDNode();
	}
	
	public REDEdgeData createEdgeData() {
		return new REDLineTreeData();
	}		
}
