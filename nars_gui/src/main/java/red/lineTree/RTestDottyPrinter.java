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

/** This class prints nodes in a format that can be processed by dotty, a freely available graph layout system from AT&T Labs
  * @author rli@chello.at
  * @tier test
  */
public class RTestDottyPrinter implements REDDistanceTreeIterator {
	public void processNode(REDNode node, REDEdgeData theData, int depth) {
		REDLineTreeData myData = (REDLineTreeData) theData;
		REDLineTreeData data;
		REDNode son;
		System.out.println("line" + myData.getLine() + '_' + Math.abs(node.hashCode()) + " [label = \"Line " + myData.getLine() + " ("+ myData.getPosition() + ')'
			+ ", b=" + node.fBalance 
			+	", " + node.toString().substring(21) + "\"]");
		if (node.fLeft != null) {
			data = (REDLineTreeData) node.fLeft.fData; son = node.fLeft.fSon;
			System.out.println("line" + myData.getLine() + '_' + Math.abs(node.hashCode()) + " -> line" + (myData.getLine() - data.getLine()) + '_' + Math.abs(son.hashCode()) + " [label = \"(" +data.getLine() + '/' + data.getPosition() + ")\"]");
		}				
		if (node.fRight != null) {
			data = (REDLineTreeData) node.fRight.fData; son = node.fRight.fSon;
			System.out.println("line" + myData.getLine() + '_' + Math.abs(node.hashCode()) + " -> line" + (myData.getLine() + data.getLine()) + '_' + Math.abs(son.hashCode()) + " [label = \"(" +data.getLine() + '/' + data.getPosition() + ")\"]");
		}								
	}		
}
