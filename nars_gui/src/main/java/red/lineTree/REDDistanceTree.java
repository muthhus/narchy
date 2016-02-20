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

/** Generic distance tree data structure. This class is not for the fainthearted. Read the concept paper (TWiki) to understand how it works
  * @author rli@chello.at
  * @invariant testClassInvariant()
  * @invariant fRoot != null
  * @invariant fRoot.fLeft == null
  * @tier system
  */
public class REDDistanceTree {
	public REDDistanceTree(REDDistanceTreeFactory factory) {
		fFactory = factory;
		fRoot = fFactory.createNode();
		fNrNodes = 1;
	}
	
	public int getNrNodes() {
		return fNrNodes;
	}
	
	protected REDEdgeData modifyEdgesAfterInsertion(REDEdgeData pos, REDEdgeDataComparison comp, REDEdgeData data) {
		REDEdgeData modData = fFactory.createEdgeData();
		REDNode node = fRoot.findNode(pos, comp, modData);
		boolean dirLeft = false;
		if (node.fRight != null) {
			node.fRight.fData.add(data);
		}
		while (node.fUp != null) {
			if (node.fUp.isLeftEdge() != dirLeft) {
				node.fUp.fData.add(data);
				dirLeft = node.fUp.isLeftEdge();
			}
			node = node.fUp.fFather;
		}
		return modData;
	}

	REDNode fRoot;
	REDDistanceTreeFactory fFactory;
	int fNrNodes;
}
