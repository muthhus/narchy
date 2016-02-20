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

import red.*;

/** A node in the line tree.
  * @author rli@chello.at
  * @invariant fUp == null || fUp.fSon == this
  * @invariant fLeft == null || fLeft.fSon.fUp.fFather == this
  * @invariant fRight == null || fRight.fSon.fUp.fFather == this
  * @tier system
  */ 
public class REDNode {
	public REDNode() {
	}

	/** Find node.
	  * @param find This parameter specifies the node to be found.
	  * @param compareFunction The comparison algorithm to be used. This allows to find nodes by different criteria.
	  * @param myData Must be initalized to 0/0 upon first call. Returns line/position.
	  * @pre myData != null
	  */
	public REDNode findNode(REDEdgeData find, REDEdgeDataComparison compareFunction, REDEdgeData myData) {
		REDNode retVal = null;
		if (compareFunction.leq(myData, find)) {
			if (fRight != null) {
				myData.add(fRight.fData);
				retVal = fRight.fSon.findNode(find, compareFunction, myData);
			}
			if (retVal == null) {
				if (fRight != null) {
					myData.sub(fRight.fData);
				}
				retVal = this;
			}
		}
		else {
			if (fLeft != null) {
				myData.sub(fLeft.fData);
				retVal = fLeft.fSon.findNode(find, compareFunction, myData);
			}
			if (retVal == null && fLeft != null) {
				myData.add(fLeft.fData);
			}
		}
		return retVal;
	}
	
	/** Iterate over the tree in order (i.e. left, this, right) and call iterator for each node 
	  * @param iterator Iterator to call for each visited node
	  * @param lowerBound A node with data d in the tree is only visited if d >= lowerBound. May be null in which case there is no lower limit to visited nodes.
	  * @param upperBound  A node with data d in the tree is only visited if d <= upperBound. May be null in which case there is no upper limit to visited nodes.
	  * @param comparison The comparison algorithm to be used when checking nodes against the given lower/upperBound. Must not be null, if either lowerBound or upperBound != null
	  * @pre iterator != null
	  */
	public void iterateInOrder(REDDistanceTreeIterator iterator, REDEdgeData data, int depth, REDEdgeData lowerBound, REDEdgeData upperBound, REDEdgeDataComparison comparison) {
		boolean lbCrit = lowerBound == null || comparison.leq(lowerBound, data);
		boolean ubCrit = upperBound == null || comparison.leq(data, upperBound);
		if (fLeft != null && lbCrit) {
			data.sub(fLeft.fData);
			fLeft.fSon.iterateInOrder(iterator, data, depth + 1, lowerBound, upperBound, comparison);
			data.add(fLeft.fData);
		}
		if (lbCrit && ubCrit) {
			iterator.processNode(this, data, depth);
		}
		if (fRight != null && ubCrit) {
			data.add(fRight.fData);
			fRight.fSon.iterateInOrder(iterator, data, depth + 1, lowerBound, upperBound, comparison);
			data.sub(fRight.fData);
		}
	}

	private static void rotate(REDNode a, REDNode b, REDEdge beta, REDEdge epsilon, int addOrSub) {
		REDNode t2root = null;
		REDEdge alpha = a.fUp;
		
		REDAssert.ensure(alpha != null);
		REDAssert.ensure(beta != null);
		REDAssert.ensure(epsilon != null);
		t2root = epsilon.fSon;	// t2root may be null
		
		// adapt values
		if (alpha.isLeftEdge()) {
			addOrSub *= -1;
		}
		
		if (addOrSub == 1) {
			alpha.fData.add(beta.fData);
		}
		else {
			alpha.fData.sub(beta.fData);
		}

		REDEdgeData oldBetaData = (REDEdgeData) beta.fData.clone();
		beta.fData.sub(epsilon.fData);
		epsilon.fData = oldBetaData;
		
		// adapt edges
		alpha.fSon = b; b.fUp = alpha;
		if (t2root != null) {
			beta.fSon = t2root; t2root.fUp = beta;
		}
		else {	// we artificially create epsilon => beta will be deleted
			if (beta.isLeftEdge()) {
				a.fLeft = null;
			}
			else {
				a.fRight = null;
			}
		}
		epsilon.fSon = a; a.fUp = epsilon;
	}
	
	private void rotateLeft(REDDistanceTreeFactory factory) {
		REDEdge beta = fRight;
		REDNode a = this;
		REDNode b = beta.fSon;
		REDEdge epsilon = fRight.fSon.fLeft;
		if (epsilon == null) {
			epsilon = new REDEdge(factory.createEdgeData(), fRight.fSon, null, true);
			fRight.fSon.fLeft = epsilon;
		}
		rotate(a, b, beta, epsilon, 1);
		a.fBalance = a.fBalance + 1 - Math.min(0, b.fBalance);
		b.fBalance = b.fBalance + 1 + Math.max(a.fBalance, 0);
	}
	
	private void rotateRight(REDDistanceTreeFactory factory) {
		REDEdge beta = fLeft;
		REDNode a = this;
		REDNode b = beta.fSon;
		REDEdge epsilon = fLeft.fSon.fRight;
		if (epsilon == null) {
			epsilon = new REDEdge(factory.createEdgeData(), fLeft.fSon, null, false);
			fLeft.fSon.fRight = epsilon;
		}
		rotate(a, b, beta, epsilon, -1);
		a.fBalance = a.fBalance - 1 - Math.max(0, b.fBalance);
		b.fBalance = b.fBalance - 1 + Math.min(a.fBalance, 0);
	}
	
	/** called recursively to balance the tree after inserting
	 * @pre factor == 1 || factor == -1
	 */
	private void balanceAfterInsert(int factor, REDDistanceTreeFactory factory) {
		fBalance += factor;
		if (fUp == null) return;
		if (fBalance == 1 || fBalance == -1) {
			if (fUp.isLeftEdge()) {
				fUp.fFather.balanceAfterInsert(1, factory);
			}
			else {
				fUp.fFather.balanceAfterInsert(-1, factory);
			}
		}
		else if (fBalance == 2) {
			REDAssert.ensure(fLeft.fSon.fBalance != 0);
			if (fLeft.fSon.fBalance == 1) {
				this.rotateRight(factory);
			}
			else if (fLeft.fSon.fBalance == -1) {
				REDNode a = fLeft.fSon;
				a.rotateLeft(factory);
				this.rotateRight(factory);
			}
		}
		else if (fBalance == -2) {
			REDAssert.ensure(fRight.fSon.fBalance != 0);
			if (fRight.fSon.fBalance == -1) {
				this.rotateLeft(factory);
			}
			else if (fRight.fSon.fBalance == 1) {
				REDNode a = fRight.fSon;
				a.rotateRight(factory);
				this.rotateLeft(factory);
			}
		}
	}

	/** called recursively to balance the tree after deleting
	 * @pre factor == 1 || factor == -1
	 */
	private void balanceAfterDelete(int factor, REDDistanceTreeFactory factory) {
		REDNode fatherBackup = null;
		boolean fatherBackupOnLeftEdge = false;
		
		fBalance += factor;
		if (fUp == null) return;
		if (fBalance == 0) {
			if (fUp.isLeftEdge()) {
				fUp.fFather.balanceAfterDelete(-1, factory);
			}
			else {
				fUp.fFather.balanceAfterDelete(1, factory);
			}
		}
		else if (fBalance == 2) {
			if (fLeft.fSon.fBalance != 0) {
				fatherBackup = fUp.fFather;
				fatherBackupOnLeftEdge = fUp.isLeftEdge(); 
			}
			if (fLeft.fSon.fBalance >= 0) {
				this.rotateRight(factory);
			}
			else {
				REDNode a = fLeft.fSon;
				a.rotateLeft(factory);
				this.rotateRight(factory);
			}
		}
		else if (fBalance == -2) {
			if (fRight.fSon.fBalance != 0) {
				fatherBackup = fUp.fFather;
				fatherBackupOnLeftEdge = fUp.isLeftEdge(); 
			}
			if (fRight.fSon.fBalance <= 0) {
				this.rotateLeft(factory);
			}
			else {
				REDNode a = fRight.fSon;
				a.rotateRight(factory);
				this.rotateLeft(factory);
			}
		}
		if (fatherBackup != null) {	
			if (fatherBackupOnLeftEdge) {
				fatherBackup.balanceAfterDelete(-1, factory);	// balanced in the left subtree, thus its height was reduced
			}
			else {
				fatherBackup.balanceAfterDelete(1, factory);		// balanced in the right subtree, thus its height was reduced
			}
		}
	}
	
	/** 
	  * @pre !data.equals(myData)
	  * @post return != null
	  */
	public REDNode insertNew(REDEdgeData data, REDEdgeData myData, REDEdgeDataComparison comp, REDDistanceTreeFactory factory) {
		REDNode n = null;
		REDNode n2 = null;
		REDEdge e = null;
		int factor = 0;
		
		if (comp.lt(myData, data)) {	// go or append right
			if (fRight != null) {
				myData.add(fRight.fData);
				n2 = fRight.fSon.insertNew(data, myData, comp, factory);
			}
			else {
				data.sub(myData);
				try {
					n = factory.createNode();	
				}
				catch (Exception ie) {
					throw new Error(String.valueOf(ie));
				}
				e = new REDEdge(data, this, n, false);
				factor = -1;
				fRight = e;
			}
		}
		else {	// go or append left
			if (fLeft != null) {
				myData.sub(fLeft.fData);
				n2 = fLeft.fSon.insertNew(data, myData, comp, factory);
			} 
			else {
				myData.sub(data);
				try {
					n = factory.createNode();
				}
				catch (Exception ie) {
					throw new Error(String.valueOf(ie));
				}
				e = new REDEdge((REDEdgeData) myData.clone(), this, n, true);
				factor = 1;
				fLeft = e;
			}
		}
		if (n != null) {
			REDAssert.ensure(e != null);
			REDAssert.ensure(factor != 0);
			n.fUp = e;
			balanceAfterInsert(factor, factory);
		}
		else {
			n = n2;
		}
		return n;
	}
	
	/** Delete this node. */
	public void delete(REDDistanceTreeFactory factory) {
		if (fLeft == null && fRight == null) {	// no sons, simple case
			if (fUp.isLeftEdge()) {
				fUp.fFather.fLeft = null;
				fUp.fFather.balanceAfterDelete(-1, factory);
			}
			else {
				fUp.fFather.fRight = null;
				fUp.fFather.balanceAfterDelete(1, factory);
			}
		}
		else if (fRight == null) {	// just a left son
			REDAssert.ensure(fLeft != null && fLeft.fSon.fBalance == 0 && fLeft.fSon.fLeft == null && fLeft.fSon.fRight == null);
			if (fUp.isLeftEdge()) {
				fUp.fData.add(fLeft.fData);
				fUp.fFather.balanceAfterDelete(-1, factory);
			}
			else {
				fUp.fData.sub(fLeft.fData);
				fUp.fFather.balanceAfterDelete(1, factory);
			}
			fUp.fSon = fLeft.fSon;
			fLeft.fSon.fUp = fUp;
		}
		else if (fLeft == null) {	// just a right son
			REDAssert.ensure(fRight != null && fRight.fSon.fBalance == 0 && fRight.fSon.fLeft == null && fRight.fSon.fRight == null);
			if (fUp.isLeftEdge()) {
				fUp.fData.sub(fRight.fData);
				fUp.fFather.balanceAfterDelete(-1, factory);
			}
			else {
				fUp.fData.add(fRight.fData);
				fUp.fFather.balanceAfterDelete(1, factory);
			}
			fUp.fSon = fRight.fSon;
			fRight.fSon.fUp = fUp;
		}
		else {	// two sons, most complicated case
			// find a substitute node
			REDNode subst = fRight.fSon;
			REDEdgeData diff = (REDEdgeData) fRight.fData.clone();
			while (subst.fLeft != null) {
				diff.sub(subst.fLeft.fData);
				subst = subst.fLeft.fSon;
			}
			if (subst.fRight != null) {
				subst.fRight.fSon.fUp = subst.fUp;
				subst.fUp.fSon = subst.fRight.fSon;
				if (subst.fUp.isLeftEdge()) {
					subst.fUp.fData.sub(subst.fRight.fData);
				}
				else {
					subst.fUp.fData.add(subst.fRight.fData);
				}
			}
			else {
				if (subst.fUp.isLeftEdge()) {
					subst.fUp.fFather.fLeft = null;
				}
				else {
					subst.fUp.fFather.fRight = null;
				}
			}
			REDEdge balanceEdge = subst.fUp;	// remembered for later usage
			// balance now
			if (balanceEdge.isLeftEdge()) {
				balanceEdge.fFather.balanceAfterDelete(-1, factory);
			}
			else {
				balanceEdge.fFather.balanceAfterDelete(1, factory);
			}
			
			// replace this by subst
//				REDAssert.ensure(diff.fPosition > 0); REDAssert.ensure(diff.fLine > 0);
			REDEdge alpha = fUp;
			REDEdge beta = fLeft;
			REDEdge gamma = fRight;
			if (alpha.isLeftEdge()) {
				alpha.fData.sub(diff);
			}
			else {
				alpha.fData.add(diff);
			}
			alpha.fSon = subst; subst.fUp = alpha;
			if (beta != null) {
				beta.fFather = subst; 
				beta.fData.add(diff);
			}
			subst.fLeft = beta;
			if (gamma != null) {
				gamma.fData.sub(diff);
				gamma.fFather = subst; 
			}
			subst.fRight = gamma;
			subst.fBalance = fBalance;
		}
		fUp = null; fLeft = null; fRight = null;
	}
	
	/** Delete first nodes within range. Recursive method.
	  * To delete all nodes within a range use <CODE>while (root.delete(from, to, comparison, new REDEdgeData());
	  * @param from Lower bound of range to find node to delete
	  * @return true, if a node has been deleted between from and to
	  */
	public boolean delete(REDEdgeData from, REDEdgeData to, REDEdgeDataComparison comp, REDEdgeData myData, REDDistanceTreeFactory factory) {
		if (comp.lt(from, myData) && fLeft != null) {
			REDEdgeData sonData = (REDEdgeData) myData.clone();
			sonData.sub(fLeft.fData);
			if (fLeft.fSon.delete(from, to, comp, sonData, factory)) {
				return true;
			}
		}
		if (comp.leq(myData, to) && fRight  != null) {
			REDEdgeData sonData = (REDEdgeData) myData.clone();
			sonData.add(fRight.fData);
			if (fRight.fSon.delete(from, to, comp, sonData, factory)) {
				return true;
			}
		}
		if (comp.lt(from, myData) && comp.leq(myData, to)) {	
			delete(factory);
			return true;
		}
		return false;
	}
	
	/** get position of node */
	public REDEdgeData getPosition(REDEdgeData data) {
		REDEdge up = fUp;
		while (up != null) {
			if (up.isLeftEdge()) {
				data.sub(up.fData);
			}
			else {
				data.add(up.fData);
			}
			up = up.fFather.fUp;
		}
		return data;
	}

	/** 
	  * @pre fUp != null
	  * @pre fUp.fFather != null
	  */	
	void move(REDEdgeData len) {
		if (fUp.isLeftEdge()) {
			fUp.fData.sub(len);
		}
		else {
			fUp.fData.add(len);
		}
		if (fLeft != null) {
			fLeft.fData.add(len);
		}
		if (fRight != null) {
			fRight.fData.sub(len);
		}
	}
	
	void moveDeleted(REDEdgeData myPos, REDEdgeData from, REDEdgeData to, REDEdgeDataComparison comp, REDDistanceTreeFactory factory) {
		REDEdgeData lPos = factory.createEdgeData();
		REDEdgeData rPos = factory.createEdgeData();	
		
		if (fRight != null) {	
			rPos.add(myPos); rPos.add(fRight.fData);
		}
		
		if (fLeft != null) {
			lPos.add(myPos); lPos.sub(fLeft.fData);
		}
	
		if (comp.lt(from, myPos) && comp.leq(myPos, to)) {
			REDEdgeData dist = (REDEdgeData) from.clone(); dist.sub(myPos); 
			move(dist);
		}
	
		if (comp.leq(from, myPos) && fLeft != null) {
			fLeft.fSon.moveDeleted(lPos, from, to, comp, factory);
		}
	
		if (comp.leq(myPos, to) && fRight != null) {	
			fRight.fSon.moveDeleted(rPos, from, to, comp, factory);
		}
	}

	
	protected int fBalance;
	protected REDEdge fLeft;
	protected REDEdge fRight;
	protected REDEdge fUp;
	{
		fLeft = null;
		fRight = null;
		fUp = null;
		fBalance = 0;
	}
}
