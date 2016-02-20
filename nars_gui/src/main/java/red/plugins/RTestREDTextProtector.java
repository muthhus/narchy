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
 
package red.plugins;

import java.awt.event.*;
import red.*;
import junit.framework.*;

/** Regression test for REDTextProtector
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDTextProtector extends RTestControllerBase {
	public RTestREDTextProtector(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		fProtector = new REDTextProtector();
		fEditor.addPlugin(fProtector);
	}

	private void setNumericContent() {
		fEditor.replace("12345678901234567890123456789012345678901234567890", 0, fEditor.length(), null);
	}

	private void setLineContent() {
		fEditor.replace("AAA\nBBB\nCCC\nDDD\nEEE", 0, fEditor.length(), null);
	}

	public void testSpecialCaseBug1() {
		setLineContent();
		fProtector.protect(1, 4);
		fProtector.protect(7, 16);
		fProtector.protect(1, 8);
		assertEquals(1, fProtector.getNrProtectedAreas());
	}

	public void testSpecialCaseBug2() {
		setLineContent();
		fProtector.protect(0, 4);
		fProtector.protect(7, 16);
		fProtector.protect(3, 8);
		assertEquals(1, fProtector.getNrProtectedAreas());
	}

    public void testSpecialCaseBug3() {
        setNumericContent();
        fProtector.protect(10, 35);
		fProtector.protect(35, 40);
		assertEquals(1, fProtector.getNrProtectedAreas());
	}

	public void testProtection() {
		setNumericContent();
		fProtector.protect(10, 15);
		fProtector.protect(20, 30);
		assertEquals(false, fProtector.mayChange(12, 12));
		assertEquals(true, fProtector.mayChange(9, 9));
		assertEquals(true, fProtector.mayChange(10, 10));
		assertEquals(true, fProtector.mayChange(20, 20));
		assertEquals(false, fProtector.mayChange(11, 11));
		assertEquals(false, fProtector.mayChange(21, 21));
		assertEquals(true, fProtector.mayChange(5, 7));
		assertEquals(true, fProtector.mayChange(5, 10));
		assertEquals(false, fProtector.mayChange(5, 11));
		assertEquals(true, fProtector.mayChange(15, 17));
		assertEquals(true, fProtector.mayChange(15, 20));
		assertEquals(false, fProtector.mayChange(5, 19));
		assertEquals(false, fProtector.mayChange(5, 20));
		assertEquals(false, fProtector.mayChange(5, 50));
		assertEquals(false, fProtector.mayChange(5, 22));
		assertEquals(false, fProtector.mayChange(13, 18));
		assertEquals(false, fProtector.mayChange(13, 22));
		
		// some false values
		assertEquals(true, fProtector.mayChange(10, -10));		// must behave as if second parameter == first parameter
		assertEquals(false, fProtector.mayChange(11, -10));	// must behave as if second parameter == first parameter
		assertEquals(true, fProtector.mayChange(-5, -20));	// must normalize first parameter to 0
		assertEquals(true, fProtector.mayChange(-5, 10));		// must normalize first parameter to 0
		assertEquals(false, fProtector.mayChange(-5, 11));	// must normalize first parameter to 0
		assertEquals(true, fProtector.mayChange(9999, 13));	// must normalize first parameter to length & set second parameter == first parameter
	}
	
	public void testInterleaved() {
		setNumericContent();
		fProtector.protect(20, 25);
		assertEquals(1, fProtector.getNrProtectedAreas());
		fProtector.protect(23, 30);	// append 
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(20, 20));
		assertEquals(false, fProtector.mayChange(21, 21));
		assertEquals(false, fProtector.mayChange(29, 29));
		assertEquals(true, fProtector.mayChange(30, 30));
		fProtector.protect(15, 21);	// prepend
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(15, 15));
		assertEquals(false, fProtector.mayChange(16, 16));
		assertEquals(false, fProtector.mayChange(29, 29));
		assertEquals(true, fProtector.mayChange(30, 30));
		fProtector.protect(18, 27);	// within => no change
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(15, 15));
		assertEquals(false, fProtector.mayChange(16, 16));
		assertEquals(false, fProtector.mayChange(29, 29));
		assertEquals(true, fProtector.mayChange(30, 30));
		fProtector.protect(15, 30);	// exactly at current boundaries => no change
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(15, 15));
		assertEquals(false, fProtector.mayChange(16, 16));
		assertEquals(false, fProtector.mayChange(29, 29));
		assertEquals(true, fProtector.mayChange(30, 30));
		fProtector.protect(10, 35);	// without => completely new boundaries
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(10, 10));
		assertEquals(false, fProtector.mayChange(11, 11));
		assertEquals(false, fProtector.mayChange(34, 34));
		assertEquals(true, fProtector.mayChange(35, 35));
		fProtector.protect(35, 40);	// append at exact boundary
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(10, 10));
		assertEquals(false, fProtector.mayChange(11, 11));
		assertEquals(false, fProtector.mayChange(39, 39));
		assertEquals(true, fProtector.mayChange(40, 40));
		fProtector.protect(5, 10);		// prepend at exact boundary
		assertEquals(1, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(5, 5));
		assertEquals(false, fProtector.mayChange(6, 6));
		assertEquals(false, fProtector.mayChange(39, 39));
		assertEquals(true, fProtector.mayChange(40, 40));
		fProtector.protect(0, 2);
		assertEquals(2, fProtector.getNrProtectedAreas());	// new area in front 
		assertEquals(true, fProtector.mayChange(0, 0));
		assertEquals(false, fProtector.mayChange(1, 1));
		assertEquals(true, fProtector.mayChange(2, 2));
		assertEquals(true, fProtector.mayChange(3, 3));
		assertEquals(true, fProtector.mayChange(4, 4));
		assertEquals(true, fProtector.mayChange(5, 5));
		assertEquals(false, fProtector.mayChange(6, 6));
		fProtector.protect(45, 47);	// new area in rear
		assertEquals(3, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(5, 5));
		assertEquals(false, fProtector.mayChange(6, 6));
		assertEquals(false, fProtector.mayChange(39, 39));
		assertEquals(true, fProtector.mayChange(40, 40));
		assertEquals(true, fProtector.mayChange(45, 45));
		assertEquals(false, fProtector.mayChange(46, 46));
		assertEquals(true, fProtector.mayChange(47, 47));
		fProtector.protect(43, 44); 	// must be ignored
		assertEquals(3, fProtector.getNrProtectedAreas());
		fProtector.protect(44, 45);	// must be ignored
		assertEquals(3, fProtector.getNrProtectedAreas());
		fProtector.protect(41, 43);	// new area in front
		assertEquals(4, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(41, 41));
		assertEquals(false, fProtector.mayChange(42, 42));
		assertEquals(true, fProtector.mayChange(43, 43));
		assertEquals(true, fProtector.mayChange(44, 44));
		assertEquals(true, fProtector.mayChange(45, 45));
		assertEquals(false, fProtector.mayChange(46, 46));
		assertEquals(true, fProtector.mayChange(47, 47));
		fProtector.protect(43, 45);	// merge two areas
		assertEquals(3, fProtector.getNrProtectedAreas());
		assertEquals(true, fProtector.mayChange(41, 41));
		assertEquals(false, fProtector.mayChange(42, 42));
		assertEquals(false, fProtector.mayChange(43, 43));
		assertEquals(false, fProtector.mayChange(44, 44));
		assertEquals(false, fProtector.mayChange(45, 45));
		assertEquals(false, fProtector.mayChange(46, 46));
		assertEquals(true, fProtector.mayChange(47, 47));
	}

	public void testLineProtectionBug1() {
		setLineContent();
		fProtector.protectLines(0, 0);
		assertEquals(1, fProtector.getNrProtectedAreas());
		fProtector.protectLines(2, 3);
		assertEquals(2, fProtector.getNrProtectedAreas());
		fProtector.protectLines(0, 1);
		assertEquals(1, fProtector.getNrProtectedAreas());
	 }

	public void testLineProtectionBug2() {
		setLineContent();
		fProtector.protectLines(0, 0);
		assertEquals(1, fProtector.getNrProtectedAreas());
		fProtector.protectLines(2, 3);
		assertEquals(2, fProtector.getNrProtectedAreas());
		fProtector.protectLines(1, 1);
		assertEquals(1, fProtector.getNrProtectedAreas());
	 }

	public void testLineProtection() {
		setLineContent();

		// protect first line
		fProtector.protectLines(0, 0);
		assertTrue("Should always be able to change at pos: 0", fProtector.mayChange(0, 0));
		for (int x = 1; x <= 3; x++) {
			assertTrue("Should not be able to change at pos: " + x, !fProtector.mayChange(x, x));
		}
		for (int x = 4; x <= fEditor.length(); x++) {
			assertTrue(fProtector.mayChange(x, x));
		}
		assertEquals(1, fProtector.getNrProtectedAreas());

		// protect more lines
		fProtector.protectLines(2, 3);
		assertTrue("Should always be able to change at pos: 0", fProtector.mayChange(0, 0));
		for (int x = 1; x <= 3; x++) {
			assertTrue(!fProtector.mayChange(x, x));
		}
		for (int x = 4; x <= 7; x++) {
			assertTrue(fProtector.mayChange(x, x));
		}
		for (int x = 8; x <= 15; x++) {
			assertTrue(!fProtector.mayChange(x, x));
		}
		for (int x = 16; x <= fEditor.length(); x++) {
			assertTrue(fProtector.mayChange(x, x));
		}
		assertEquals(2, fProtector.getNrProtectedAreas());

		// connect protected lines areas
		fProtector.protectLines(1, 1);
		assertEquals(1, fProtector.getNrProtectedAreas());				
		assertTrue("Should always be able to change at pos: 0", fProtector.mayChange(0, 0));
		for (int x = 1; x <= 15; x++) {
			assertTrue("Should not be able to change at " + x, !fProtector.mayChange(x, x));
		}
		for (int x = 16; x <= fEditor.length(); x++) {
			assertTrue(fProtector.mayChange(x, x));
		}
	}
	
	public void testDeleteKey() {
		setLineContent();
		fProtector.protectLines(1, 2);
		
		for (int x = 3; x < 11; x++) {
			fEditor.setSelection(x);
			send(KeyEvent.VK_DELETE, '\0', 0);
			assertEquals(19, fEditor.length());
		}
		
		fEditor.setSelection(2, 5);
		send(KeyEvent.VK_DELETE, '\0', 0);
		assertEquals(19, fEditor.length());

		fEditor.setSelection(10, 13);
		send(KeyEvent.VK_DELETE, '\0', 0);
		assertEquals(19, fEditor.length());		
		
		fEditor.setSelection(2, 14);
		send(KeyEvent.VK_DELETE, '\0', 0);
		assertEquals(19, fEditor.length());
		
		fEditor.setSelection(12);
		send(KeyEvent.VK_DELETE, '\0', 0);
		assertEquals(18, fEditor.length());
		
		fEditor.setSelection(2);
		send(KeyEvent.VK_DELETE, '\0', 0);
		assertEquals(17, fEditor.length());
		
		assertEquals("AA\nBBB\nCCC\nDD\nEEE", fEditor.asString());
	}
	
	public void testBackspaceKey() {
		setLineContent();
		fProtector.protectLines(1, 2);
		
		for (int x = 4; x < 12; x++) {
			fEditor.setSelection(x);
			send(KeyEvent.VK_UNDEFINED, '\b', 0);
			assertEquals(19, fEditor.length());
		}
		
		fEditor.setSelection(2, 5);
		send(KeyEvent.VK_UNDEFINED, '\b', 0);
		assertEquals(19, fEditor.length());

		fEditor.setSelection(10, 13);
		send(KeyEvent.VK_UNDEFINED, '\b', 0);
		assertEquals(19, fEditor.length());		
		
		fEditor.setSelection(2, 14);
		send(KeyEvent.VK_UNDEFINED, '\b', 0);
		assertEquals(19, fEditor.length());
		
		fEditor.setSelection(13);
		send(KeyEvent.VK_UNDEFINED, '\b', 0);
		assertEquals(18, fEditor.length());
		
		fEditor.setSelection(3);
		send(KeyEvent.VK_UNDEFINED, '\b', 0);
		assertEquals(17, fEditor.length());
		
		assertEquals("AA\nBBB\nCCC\nDD\nEEE", fEditor.asString());
	}
	
	public void testKeys() {
		setLineContent();
		fProtector.protectLines(1, 2);
		
		for (int x = 4; x <= 11; x++) {
			fEditor.setSelection(x);
			send(KeyEvent.VK_UNDEFINED, 'a', 0);
			assertEquals(19, fEditor.length());
		}
		assertEquals("AAA\nBBB\nCCC\nDDD\nEEE", fEditor.asString());
		fEditor.setSelection(3);
		send(KeyEvent.VK_UNDEFINED, 'a', 0);
		assertEquals("AAAa\nBBB\nCCC\nDDD\nEEE", fEditor.asString());
		fEditor.setSelection(13);
		send(KeyEvent.VK_UNDEFINED, 'x', 0);
		assertEquals("AAAa\nBBB\nCCC\nxDDD\nEEE", fEditor.asString());
	}
	
	public void testLinebreak() {
		setLineContent();
		fProtector.protectLines(1, 2);
		
		for (int x = 4; x <= 11; x++) {
			fEditor.setSelection(x);
			send(KeyEvent.VK_UNDEFINED, '\n', 0);
			assertEquals(19, fEditor.length());
		}
		assertEquals("AAA\nBBB\nCCC\nDDD\nEEE", fEditor.asString());
		fEditor.setSelection(3);
		send(KeyEvent.VK_UNDEFINED, '\n', 0);
		assertEquals("AAA\n\nBBB\nCCC\nDDD\nEEE", fEditor.asString());
		fEditor.setSelection(13);
		send(KeyEvent.VK_UNDEFINED, '\n', 0);
		assertEquals("AAA\n\nBBB\nCCC\n\nDDD\nEEE", fEditor.asString());
	}

	public void testTab() {
		setLineContent();
		fProtector.protectLines(1, 2);
		
		for (int x = 4; x <= 11; x++) {
			fEditor.setSelection(x);
			send(KeyEvent.VK_TAB, '\0');
			assertEquals(19, fEditor.length());
		}
		assertEquals("AAA\nBBB\nCCC\nDDD\nEEE", fEditor.asString());
		fEditor.setSelection(3);
		send(KeyEvent.VK_TAB, '\0');
		assertEquals("AAA\t\nBBB\nCCC\nDDD\nEEE", fEditor.asString());
		fEditor.setSelection(13);
		send(KeyEvent.VK_TAB, '\0');
		assertEquals("AAA\t\nBBB\nCCC\n\tDDD\nEEE", fEditor.asString());
	}

	public static Test suite() {
		return new TestSuite(RTestREDTextProtector.class);
	}
	
	REDTextProtector fProtector;
}
