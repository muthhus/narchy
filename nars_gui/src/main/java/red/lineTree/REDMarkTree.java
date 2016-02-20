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
import red.*;

/** Mark tree data structure. 
  * @author rli@chello.at
  * @invariant fRoot instanceof REDMark
  * @tier API
  */
public class REDMarkTree extends REDDistanceTree implements REDTextEventListener {
	public REDMarkTree(REDEditor editor) {
		super(new REDMarkTreeFactory());
		fEditor = editor;
	}
	
	public REDMark createMark(int pos, Object value) {
		if (pos < 0) {
			pos = 0;
		}
		if (pos > fEditor.length()) {
			pos = fEditor.length();
		}
		return getRoot().insertNew(new REDMarkTreeData(pos), new REDMarkTreeData(), REDMarkTreeData.fgComparison, fFactory, value);
	}
	
	public void deleteMarks(int from, int to) {
		REDMarkTreeData fromData = new REDMarkTreeData(from);
		REDMarkTreeData toData = new REDMarkTreeData(to);
		REDMarkTreeData nullData = new REDMarkTreeData();
		while (fRoot.delete(fromData, toData, REDMarkTreeData.fgComparison, nullData, fFactory)) {
		}
	}
	
	public void deleteMark(REDMark m) {
		m.delete(fFactory);
	}
	
	/** Collect marks into a vector
	  * @param from Position to start the search for marks at
	  * @param to Position to end the search for marks at
	  * @param cl If this parameter is non-null, only those marks are collected whose value is an instanceof cl.
	  * @param reuse If this parameter is non-null, the passed ArrayList is reused. This can be used to increase performance. Existing elements in reuse are not removed!
	  * @return A ArrayList containing 0 to n REDMark objects.
	  */
	public ArrayList collectMarks(int from, int to, Class cl, ArrayList reuse) {
		if (reuse == null) {
			reuse = new ArrayList();
		}
		getRoot().collect(0, reuse, from, to, cl, true);
		return reuse;
	}
	
	/** Find a mark.
	  * This method tries to find the mark which is closest to a given position.
	  * @param pos Position to search for nearest mark
	  * @param left If this parameter is set to true, the mark is searched to the left of pos; otherwise it is searched to the right of pos.
	  * @param cl If this parameter is non-null, only those marks are considered whose value is an instanceof cl.
	  * @return The REDMark object closest to pos or null, if no such mark exists.
	  */
	public REDMark findMark(int pos, boolean left, Class cl) {
		return getRoot().find(0, pos, left, cl, true);
	}
	
//	REDMark findMarkUsingVisitor(int pos, boolean left, REDMarkCollectorVisitor visitor) {
//	}

	REDMark getRoot() {
		return (REDMark) fRoot;
	}

	
	// text event interface
	public int getListenerLevel() {
		return REDTextEventListener.RLL_VIEW;
	}

	public void afterInsert(int from, int to) {
		REDEdgeData data = new REDMarkTreeData(to-from);
		modifyEdgesAfterInsertion(new REDMarkTreeData(from), REDMarkTreeData.fgComparison, data);
	}
	
	public void afterDelete(int from, int to) {
		fRoot.moveDeleted(fFactory.createEdgeData(), new REDMarkTreeData(from), new REDMarkTreeData(to), REDMarkTreeData.fgComparison, fFactory);
		REDEdgeData data = new REDMarkTreeData(from - to);
		REDEdgeData start = modifyEdgesAfterInsertion(new REDMarkTreeData(from), REDMarkTreeData.fgComparison, data);
	}
	
	public void beforeInsert(int from, int to) {}
	public void beforeDelete(int from, int to) {}
	public void beforeStyleChange(int from, int to, REDStyle newStyle) {}
	public void afterStyleChange(int from, int to, REDStyle newStyle) {}
	public void beforeLoad() {}
	public void afterLoad() {}
	public void beforeSave() {}
	public void afterSave() {}	
	public void beforeSaveInto(String filename) {}
	public void afterSaveInto(String filename) {}	
	public void beforeStyleBatchNotification() {}
	public void afterStyleBatchNotification() {}
	REDEditor fEditor;
}

/** Distance tree factory for REDMarkTree */
class REDMarkTreeFactory implements REDDistanceTreeFactory {
	public REDNode createNode() {
		return new REDMark();
	}
	
	public REDEdgeData createEdgeData() {
		return new REDMarkTreeData();
	}		
}
