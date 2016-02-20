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
 
package red;	

import junit.framework.*;
import java.awt.event.*;
import java.awt.*;

/** Test case for REDViewReadonlyController.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDViewController extends RTestControllerBase {
	
	public RTestREDViewController(String name) {
		super(name);
	}

	public void testKeys() {
		fEditor.replace("", 0, fEditor.length(), null);	
		send(0, 'a');
		assertEquals("a", fEditor.asString());
		send(0, 'A');
		assertEquals("aA", fEditor.asString());
		send(KeyEvent.VK_TAB, '\0');
		assertEquals("aA\t", fEditor.asString());
	}

	public void testSpecialKeys() {
		fEditor.replace("You cannot pass!", 0, fEditor.length(), null);

		send(KeyEvent.VK_ESCAPE, '\0');
		assertEquals("You cannot pass!", fEditor.asString());

		send(KeyEvent.VK_INSERT, '\0');
		assertEquals("You cannot pass!", fEditor.asString());
		
		fEditor.setSelection(0, 0);
		send(0, '\n');
		assertEquals("\nYou cannot pass!", fEditor.asString());
	}

	public void testBackspaceAndDelete() {
		fEditor.replace("You cannot pass!", 0, fEditor.length(), null);
		fEditor.setSelection(12, 12);

		send(0, '\b');
		assertEquals("You cannot ass!", fEditor.asString());

		send(0, '\b');
		send(KeyEvent.VK_DELETE, '\0');
		send(KeyEvent.VK_DELETE, '\0');
		send(KeyEvent.VK_DELETE, '\0');
		assertEquals("You cannot!", fEditor.asString());

		fEditor.setSelection(0, 0);
		send(0, '\b');
		assertEquals("You cannot!", fEditor.asString());
		
		fEditor.setSelection(fEditor.length(), fEditor.length());
		send(KeyEvent.VK_DELETE, '\0');
		assertEquals("You cannot!", fEditor.asString());		
	}

	public void testCursorKeys() {
		fEditor.replace("XXX XXX\nXXX XXX\nXXX XXX\nXXX XXX", 0, fEditor.length(), null);
		fEditor.setSelection(0, 0);
		send(KeyEvent.VK_LEFT, '\0');
		send(KeyEvent.VK_RIGHT, '\0');
		assertEquals(1, fEditor.getSelectionStart());
		for (int x = 0; x < 7; x++) {
			send(KeyEvent.VK_RIGHT, '\0');
		}
		assertEquals(8, fEditor.getSelectionStart());
		send(KeyEvent.VK_RIGHT, '\0');
		send(KeyEvent.VK_UP, '\0');
		assertEquals(1, fEditor.getSelectionStart());
		send(KeyEvent.VK_RIGHT, '\0');
		send(KeyEvent.VK_DOWN, '\0');
		assertEquals(10, fEditor.getSelectionStart());
		
		send(KeyEvent.VK_END, '\0');
		assertEquals(fEditor.getLineEnd(1), fEditor.getSelectionStart());

		send(KeyEvent.VK_HOME, '\0');
		assertEquals(fEditor.getLineStart(1), fEditor.getSelectionStart());

		send(KeyEvent.VK_END, '\0', InputEvent.CTRL_MASK);
		assertEquals(fEditor.length(), fEditor.getSelectionStart());

		send(KeyEvent.VK_HOME, '\0', InputEvent.CTRL_MASK);
		assertEquals(0, fEditor.getSelectionStart());

		send(KeyEvent.VK_RIGHT, '\0', InputEvent.CTRL_MASK);
		assertEquals(3, fEditor.getSelectionStart());

		send(KeyEvent.VK_RIGHT, '\0', InputEvent.CTRL_MASK);
		send(KeyEvent.VK_LEFT, '\0', InputEvent.CTRL_MASK);
		assertEquals(4, fEditor.getSelectionStart());

		fEditor.getView().setSize(new Dimension(1000, 1000));		
		send(KeyEvent.VK_DOWN, '\0', InputEvent.CTRL_MASK);
		assertEquals(28, fEditor.getSelectionStart());

		send(KeyEvent.VK_UP, '\0', InputEvent.CTRL_MASK);
		assertEquals(4, fEditor.getSelectionStart());		

		send(KeyEvent.VK_PAGE_DOWN, '\0', 0);
		assertEquals(28, fEditor.getSelectionStart());

		send(KeyEvent.VK_PAGE_UP, '\0', 0);
		assertEquals(4, fEditor.getSelectionStart());		
	} 
	
	public void testClicks() {
		for (int x = 0; x <= fEditor.length(); x++) {
			mouseClick(x, LEFT, 1);
			assertEquals(x, fEditor.getSelectionStart());
		}
		
		// double click on first word
		mouseClick(0, LEFT, 2);
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(5, fEditor.getSelectionEnd());
		
		// triple click => whole line selected
		mouseClick(5, LEFT, 3);
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(fEditor.getLineStart(1), fEditor.getSelectionEnd());		
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDViewController.class);
	}
}