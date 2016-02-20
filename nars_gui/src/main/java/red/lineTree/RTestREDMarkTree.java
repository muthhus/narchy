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
import junit.framework.*;
import java.util.*;

/** Test case for REDMarkTree.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDMarkTree extends TestCase {
	public RTestREDMarkTree(String name) {
		super(name);
	}
	
	public void testTree() {
		REDEditor e = new REDEditor();
		e.replace("12345678901234567890123456789012345678901234567890", 0, 0, "");
		REDMarkTree t = e.createMarkTree();
		String value1 = "Value1";
		String value2 = "Value2";
		String value3 = "Value3";
		String value4 = "Value4";
		String value5 = "Value5";
		REDMark m1 = t.createMark(10, value1);
		REDMark m2 = t.createMark(20, value2);
		REDMark m3 = t.createMark(-1, value3);
		REDMark m4 = t.createMark(0, value4);
		REDMark m5 = t.createMark(100, value5);
		REDMark m6 = t.createMark(30, null);
		assertEquals(10, m1.getPosition()); assertEquals(value1, m1.getValue());
		assertEquals(20, m2.getPosition()); assertEquals(value2, m2.getValue());
		assertEquals(0, m3.getPosition()); assertEquals(value3, m3.getValue());
		assertEquals(0, m4.getPosition()); assertEquals(value4, m4.getValue());
		assertEquals(50, m5.getPosition()); assertEquals(value5, m5.getValue());
		assertEquals(30, m6.getPosition()); assertEquals(null, m6.getValue());
		e.replace("ABCDE", 15, 15, "");
		assertEquals(10, m1.getPosition()); assertEquals(value1, m1.getValue());
		assertEquals(25, m2.getPosition()); assertEquals(value2, m2.getValue());
		assertEquals(0, m3.getPosition()); assertEquals(value3, m3.getValue());
		assertEquals(0, m4.getPosition()); assertEquals(value4, m4.getValue());
		assertEquals(55, m5.getPosition()); assertEquals(value5, m5.getValue());
		assertEquals(35, m6.getPosition()); assertEquals(null, m6.getValue());
		e.replace("ABCDE", 22, 27, "");
		assertEquals(10, m1.getPosition()); assertEquals(value1, m1.getValue());
		assertEquals(22, m2.getPosition()); assertEquals(value2, m2.getValue());
		assertEquals(0, m3.getPosition()); assertEquals(value3, m3.getValue());
		assertEquals(0, m4.getPosition()); assertEquals(value4, m4.getValue());
		assertEquals(55, m5.getPosition()); assertEquals(value5, m5.getValue());
		assertEquals(35, m6.getPosition()); assertEquals(null, m6.getValue());
		e.replace("ABCDE", 0, 0, "");
		assertEquals(15, m1.getPosition()); assertEquals(value1, m1.getValue());
		assertEquals(27, m2.getPosition()); assertEquals(value2, m2.getValue());
		assertEquals(0, m3.getPosition()); assertEquals(value3, m3.getValue());
		assertEquals(0, m4.getPosition()); assertEquals(value4, m4.getValue());
		assertEquals(60, m5.getPosition()); assertEquals(value5, m5.getValue());
		assertEquals(40, m6.getPosition()); assertEquals(null, m6.getValue());
		e.replace("ABCDE", 100, 100, "");
		assertEquals(15, m1.getPosition()); assertEquals(value1, m1.getValue());
		assertEquals(27, m2.getPosition()); assertEquals(value2, m2.getValue());
		assertEquals(0, m3.getPosition()); assertEquals(value3, m3.getValue());
		assertEquals(0, m4.getPosition()); assertEquals(value4, m4.getValue());
		assertEquals(60, m5.getPosition()); assertEquals(value5, m5.getValue());
		assertEquals(40, m6.getPosition()); assertEquals(null, m6.getValue());		
	}
	
	static boolean isAscending(ArrayList v) {
		int lastValue = 0;
		int pos;
		Iterator iter = v.iterator();
		while (iter.hasNext()) {
			pos = ((REDMark) iter.next()).getPosition();
			if (pos < lastValue) {
				return false;
			}
			lastValue = pos;
		}
		return true;
	}
	
	public void testCollect() {
		REDEditor e = new REDEditor();
		e.replace("12345678901234567890123456789012345678901234567890", 0, 0, "");
		REDMarkTree t = e.createMarkTree();
		String value1 = "Value1";
		Integer value2 = 17;
		String value3 = "Value3";
		Boolean value4 = Boolean.FALSE;
		String value5 = "Value5";
		REDMark m1 = t.createMark(10, value1);
		REDMark m2 = t.createMark(20, value2);
		REDMark m3 = t.createMark(-1, value3);
		REDMark m4 = t.createMark(0, value4);
		REDMark m5 = t.createMark(100, value5);
		REDMark m6 = t.createMark(30, null);
		ArrayList v = t.collectMarks(0, 30, null, null);
		assertEquals(5, v.size());
		assertEquals(true, isAscending(v));
		v.clear();
		ArrayList v2 = t.collectMarks(1, 600, value1.getClass(), v);
		assertSame(v, v2);
		assertEquals(2, v.size());
		assertEquals(true, isAscending(v));
		v.clear();
		v = t.collectMarks(-5, 50, value2.getClass(), v);
		assertEquals(1, v.size());
		v.clear();
		v = t.collectMarks(10, 5, null, v);
		assertEquals(0, v.size());
		v.clear();
		v = t.collectMarks(-10, -5, null, v);
		assertEquals(0, v.size());
		v.clear();
		t.collectMarks(10, 10, null, v);
		assertEquals(1, v.size());
	}		
		
	public void testFind() {
		REDEditor e = new REDEditor();
		e.replace("12345678901234567890123456789012345678901234567890", 0, 0, "");
		REDMarkTree t = e.createMarkTree();
		String value1 = "Value1";
		Integer value2 = 17;
		String value3 = "Value3";
		Boolean value4 = Boolean.FALSE;
		String value5 = "Value5";
		REDMark m1 = t.createMark(10, value1);
		REDMark m2 = t.createMark(20, value2);
		REDMark m3 = t.createMark(-1, value3);
		REDMark m4 = t.createMark(0, value4);
		REDMark m5 = t.createMark(100, value5);
		REDMark m6 = t.createMark(30, null);
		assertSame(m1, t.findMark(15, true, null));
		assertSame(m1, t.findMark(10, true, null));
		assertSame(m2, t.findMark(15, false, null));
		assertSame(m2, t.findMark(20, false, null));
		assertSame(m1, t.findMark(25, true, value1.getClass()));
		assertSame(m4, t.findMark(0, false, value4.getClass()));
		assertSame(null, t.findMark(e.length() +1, false, null));
		assertSame(m5, t.findMark(e.length(), false, null));
		assertSame(m5, t.findMark(e.length(), true, null));
		assertSame(m5, t.findMark(e.length() -1, false, null));
		assertSame(m6, t.findMark(e.length() -1, true, null));
		assertSame(null, t.findMark(-1, true, null));
		assertSame(m3, t.findMark(-1, false, value3.getClass()));
		assertSame(m4, t.findMark(-1, false, value4.getClass()));
		assertTrue(t.findMark(-1, false, null) == m3 || t.findMark(-1, false, null) == m4);
	}
	
	public void testAlignment() {
		REDEditor e = new REDEditor();
		e.replace("12345678901234567890123456789012345678901234567890", 0, 0, "");
		REDMarkTree t = e.createMarkTree();
		String value1 = "Value1";
		String value2 = "Value2";
		String value3 = "Value3";
		String value4 = "Value4";
		String value5 = "Value5";
		REDMark m1 = t.createMark(10, value1);
		REDMark m2 = t.createMark(20, value2);
		e.replace("Foo", 10, 10, null);
		assertEquals(m1.getPosition(), 10);	// we're left aligned
		assertEquals(m2.getPosition(), 23);	// happened right before us
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDMarkTree.class);
	}
}
