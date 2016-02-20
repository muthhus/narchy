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

/** Edges in the line tree.
  * @author rli@chello.at
  * @see REDEdge
  * @see REDNode
  * @see REDLineTree
  * @tier system
  */
public class REDEdge {
	/**
	  * @param data the data to associate with this edge
	  * @param father the father node
	  * @param son the son node
	  * @param left <BR>&nbsp; true: son is the left son of father
	    <BR>&nbsp; false: son is the right son of father
	  * @pre data != null
	  * @pre father != null
	  * @pre son != null
	  */
	public REDEdge(REDEdgeData data, REDNode father, REDNode son, boolean left) {
		fData = data;
		fFather = father;
		fSon = son;
		fIsLeft = left;
	}

	/** determine whether this is a left edge
	  * @return <BR>&nbsp; true: son is the left son of father
	    <BR>&nbsp; false: son is the right son of father
	  */
	public boolean isLeftEdge() {
		return fIsLeft;
	}
	
	protected REDEdgeData fData;
	protected boolean fIsLeft;
	protected REDNode fFather;
	protected REDNode fSon;
}
