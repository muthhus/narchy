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
 
package red;	// this test case is in package red becaus it accesses REDText

import red.lineTree.*;
import java.util.*;
import junit.framework.*;

/** Regression test for line tree.
  * @tbd: move text-stuff into test case for text!
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDLineTree extends TestCase {
	static final int fcRandomIterations = 100;
	static final int fcRandomTextLength =  1000;
	String fStr;
	REDText fText;
	public RTestREDLineTree(String name) {
		super(name);
		fStr = "";
		fText = new REDText("");
	}

	public void dumpTree(String header) {
		dumpTree(header, false);
	}

	public void dumpTree(String header, boolean checkOnly) {
		if (!checkOnly) {
			System.out.println(header);
			fText.fLineTree.iterateInOrder(new PosPrinter());
//			fText.fLineTree.iterateInOrder(new TreePrinter());
//			fText.fLineTree.iterateInOrder(new RTestDottyPrinter());
		}
		fText.fLineTree.iterateInOrder(new PosCheckerPrinter(fStr));
	}
	
	public String treeToString(String header) {
		fText.fLineTree.iterateInOrder(new PosCheckerPrinter(fStr));
		ToStringPrinter pr = new ToStringPrinter();
		fText.fLineTree.iterateInOrder(pr);
		return header + ':' + pr.toString();
	}

	
	private void doTestInsert(int position, String str) {
		fStr = fStr.substring(0, position) + str + fStr.substring(position, fStr.length());
		fText.replace(position, position, str);
	}
	
	public void testInsert() {
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		assertEquals("Original: 0,0 1,3 2,6 3,9 4,12 5,15 6,18", treeToString("Original"));
		doTestInsert(10, "XX");
		assertEquals("Inserting XX at 10: 0,0 1,3 2,6 3,9 4,14 5,17 6,20", treeToString("Inserting XX at 10"));
		
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestInsert(10, "XX\nYY");
		assertEquals("Inserting XX\\nYY at 10: 0,0 1,3 2,6 3,9 4,13 5,17 6,20 7,23", treeToString("Inserting XX\\nYY at 10"));

		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestInsert(1, "\nXX");		
		doTestInsert(1, "\nXX");		
		doTestInsert(1, "\nXX");		
		doTestInsert(1, "\nXX");		
		assertEquals("Inserting at the left: 0,0 1,2 2,5 3,8 4,11 5,15 6,18 7,21 8,24 9,27 10,30", treeToString("Inserting at the left"));
		doTestInsert(28, "\nXX");		
		assertEquals("Provoke double rotation: 0,0 1,2 2,5 3,8 4,11 5,15 6,18 7,21 8,24 9,27 10,29 11,33", treeToString("Provoke double rotation"));
		
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestInsert(10, "XX\nSS\nTT\nYY");		
		assertEquals("Inserting XX\\nSS\\nTT\\nYY: 0,0 1,3 2,6 3,9 4,13 5,16 6,19 7,23 8,26 9,29", treeToString("Inserting XX\\nSS\\nTT\\nYY"));
		doTestInsert(21, "\nQQ");
		assertEquals("Inserting \\nQQ: 0,0 1,3 2,6 3,9 4,13 5,16 6,19 7,22 8,26 9,29 10,32", treeToString("Inserting \\nQQ"));
		doTestInsert(17, "\nQQ");
		assertEquals("Inserting \\nQQ again to provoke double rotation: 0,0 1,3 2,6 3,9 4,13 5,16 6,18 7,22 8,25 9,29 10,32 11,35", treeToString("Inserting \\nQQ again to provoke double rotation"));
	}
	
	public void testRandomInsert() {
		fText = new REDText("");
		doTestInsert(0, createRandomString(fcRandomTextLength));
		dumpTree("Test tree correctness: ", true);
		
		for (int x = 0; x <= fcRandomIterations; x++) {
			doTestInsert(fRnd.nextInt(fcRandomTextLength+1), createRandomString(fRnd.nextInt(fcRandomTextLength / 10)));
			dumpTree("Test tree correctness #" + x, true);
		}
	}
	
	private void doTestDelete(int from, int to) {
		fStr = fStr.substring(0, from) + fStr.substring(to, fStr.length());
		fText.replace(from, to, "");
	}

	
	public void testDelete() {
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		assertEquals("Original: 0,0 1,3 2,6 3,9 4,12 5,15 6,18", treeToString("Original"));
		doTestDelete(0, 1);
		assertEquals("Deleting in line: 0,0 1,2 2,5 3,8 4,11 5,14 6,17", treeToString("Deleting in line"));

		fText = new REDText(""); 
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestDelete(13, 14);
		assertEquals("Deleting in line: 0,0 1,3 2,6 3,9 4,12 5,14 6,17", treeToString("Deleting in line"));
		
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestDelete(8, 9);
		assertEquals("Deleting leaf line: 0,0 1,3 2,6 3,11 4,14 5,17", treeToString("Deleting leaf line"));
		doTestDelete(5, 6);
		assertEquals("Deleting node with only left son: 0,0 1,3 2,10 3,13 4,16", treeToString("Deleting node with only left son"));

		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestDelete(14, 15);
		assertEquals("Deleting node with only right son: 0,0 1,3 2,6 3,9 4,12 5,17", treeToString("Deleting node with only right son"));

		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestDelete(11, 12);
		assertEquals("Deleting node with two sons: 0,0 1,3 2,6 3,9 4,14 5,17", treeToString("Deleting node with two sons"));

		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestDelete(5, 6);
		assertEquals("Deleting node with two sons but no grandsons: 0,0 1,3 2,8 3,11 4,14 5,17", treeToString("Deleting node with two sons but no grandsons"));
		
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		doTestDelete(5, 12);
		assertEquals("Deleting more than one node: 0,0 1,3 2,8 3,11", treeToString("Deleting more than one node"));
		
		fText = new REDText("");
		doTestInsert(0, "AA\nBBB");
		doTestDelete(4, 6);
		assertEquals("Deleting in last line: 0,0 1,3", treeToString("Deleting in last line"));

		fText = new REDText("");
		doTestInsert(0, "AA\nBB\n");
		doTestDelete(4, 6);
		assertEquals("Deleting last line which is empty: 0,0 1,3", treeToString("Deleting last line which is empty"));
		
		fText = new REDText("");
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nFF\nGG\nHH");
		doTestDelete(14, 15);
		assertEquals("Deleting for balancing check: 0,0 1,3 2,6 3,9 4,12 5,17 6,20 7,23", treeToString("Deleting for balancing check"));
	}

	public void testRandomDelete() {
		fText = new REDText("");
		doTestInsert(0, createRandomString(fcRandomTextLength));
		dumpTree("Test tree correctness: ", true);
		
		for (int x = 0; x <= fcRandomIterations; x++) {
			int start = fRnd.nextInt(fStr.length());
			int end = start + fRnd.nextInt(fcRandomTextLength / fcRandomIterations);
			if (end > fStr.length()) end = fStr.length();
//			System.out.println("Delete from " + start + " to " + end);
			doTestDelete(start, end);
			dumpTree("Test tree correctness #" + x, true);
		}
	}
	
	public void testIterator() {
		ToStringPrinter iter = new ToStringPrinter();
		doTestInsert(0, "AA\nBB\nCC\nDD\nEE\nFF\nGG");
		fText.fLineTree.iterateInOrder(iter);
		assertEquals(" 0,0 1,3 2,6 3,9 4,12 5,15 6,18", iter.toString());
		REDLineTreeData lb = new REDLineTreeData(5, 3);
		REDLineTreeData ub = new REDLineTreeData(16, 5);
		REDLineTreeData lb2 = new REDLineTreeData(6, 3);
		REDLineTreeData ub2 = new REDLineTreeData(15, 5);
		
		// lb & ub for lines
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, lb, ub, REDLineTreeData.fgLineComparison);
		assertEquals(" 3,9 4,12 5,15", iter.toString());
		
		// lb & ub for positions
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, lb, ub, REDLineTreeData.fgPositionComparison);
		assertEquals(" 2,6 3,9 4,12 5,15", iter.toString());

		// ditto, with boundaries exact
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, lb2, ub2, REDLineTreeData.fgPositionComparison);
		assertEquals(" 2,6 3,9 4,12 5,15", iter.toString());
		
		// no lb for lines
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, null, ub, REDLineTreeData.fgLineComparison);
		assertEquals(" 0,0 1,3 2,6 3,9 4,12 5,15", iter.toString());
		
		// no hb for lines
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, lb, null, REDLineTreeData.fgLineComparison);
		assertEquals(" 3,9 4,12 5,15 6,18", iter.toString());

		// no lb for positions
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, null, ub, REDLineTreeData.fgPositionComparison);
		assertEquals(" 0,0 1,3 2,6 3,9 4,12 5,15", iter.toString());
		
		// no hb for position
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, lb, null, REDLineTreeData.fgPositionComparison);
		assertEquals(" 2,6 3,9 4,12 5,15 6,18", iter.toString());

		// no bounds for lines
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, null, null, REDLineTreeData.fgLineComparison);
		assertEquals(" 0,0 1,3 2,6 3,9 4,12 5,15 6,18", iter.toString());

		// no bounds for position
		iter.clear();
		fText.fLineTree.iterateInOrder(iter, null, null, REDLineTreeData.fgPositionComparison);
		assertEquals(" 0,0 1,3 2,6 3,9 4,12 5,15 6,18", iter.toString());
	}

	static class PosPrinter implements REDDistanceTreeIterator {
		public void processNode(REDNode node, REDEdgeData data, int depth) {
			REDLineTreeData myData = (REDLineTreeData) data;
			System.out.println("Line: " + myData.getLine() + ", Position: " + myData.getPosition());
		}		
	}

	static class ToStringPrinter implements REDDistanceTreeIterator {
		public ToStringPrinter() {
			fBuffer = new StringBuffer();
		}
		
		public void processNode(REDNode node, REDEdgeData data, int depth) {
			REDLineTreeData myData = (REDLineTreeData) data;
			fBuffer.append(" ").append(myData.getLine()).append(',').append(myData.getPosition());
		}		
		
		public String toString() {
			return fBuffer.toString();
		}
		
		public void clear() {
			fBuffer = new StringBuffer();
		}
		
		StringBuffer fBuffer;
	}
	
	static class TreePrinter implements REDDistanceTreeIterator {
		public void processNode(REDNode node, REDEdgeData data, int depth) {
			REDLineTreeData myData = (REDLineTreeData) data;
			StringBuilder buf = new StringBuilder();
			while (depth > 0) {
				buf.append('|');
				depth--;
			}
			buf.append("-(").append(myData.getLine()).append(", ").append(myData.getPosition()).append(')');
			System.out.println(buf.toString());
		}		
	}
	
	static class PosCheckerPrinter implements REDDistanceTreeIterator {
		public PosCheckerPrinter (String text) {
			fLineStart = new ArrayList(); fLineStart.add(0);
			int idx = text.indexOf('\n');
			while (idx != -1) {
				fLineStart.add(idx + 1);
				idx = text.indexOf('\n', idx+1);
			}
			fCurLine = 0;
		}
		ArrayList fLineStart;
		int fCurLine;
		
		public void processNode(REDNode node, REDEdgeData data, int depth) {
			REDLineTreeData myData = (REDLineTreeData) data;
			assertTrue("Wrong linenumber. Expected: " + fCurLine + ", got " + myData.getLine(), myData.getLine() == fCurLine);
			assertTrue("Node: " + node + " is wrong. Expected its position to be " + (Integer) fLineStart.get(fCurLine) + " but it was " + myData.getPosition(), myData.getPosition() == (Integer) fLineStart.get(fCurLine));
			fCurLine++;
		}		
		
//		private int getDepth(REDEdge edge) {
//			if (edge == null) {
//				return 0;
//			}
//			else {
//				return edge.getSon().getDepth();
//			}
//		}

	}
	
	public String createRandomString(int length) {
		String charTable = "\n\tABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder buf = new StringBuilder(length+1);
		for (int x = 0; x < length; x++) {
			buf.append(charTable.charAt(fRnd.nextInt(charTable.length())));
		}
		return buf.toString();
	}

	Random fRnd; {
		fRnd = new Random(System.currentTimeMillis());
	}

	
	public static Test suite() {
		return new TestSuite(RTestREDLineTree.class);
	}
}
