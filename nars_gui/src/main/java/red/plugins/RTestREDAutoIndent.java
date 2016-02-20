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

import red.*;
import red.util.*;
import junit.framework.*;
import java.awt.event.*;

/** Regression test for REDAutoIndent
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDAutoIndent extends TestCase {
	public RTestREDAutoIndent(String name) {
		super(name);
	}

	public void setUp() {
		fEditor = new REDEditor();
		fIndent = new REDAutoIndent();
		fEditor.addPlugin(fIndent);
	}
	
	private static long now() {
		return System.currentTimeMillis();
	}
	
	private void send(int keyCode, char keyChar) {
		send(keyCode, keyChar, 0);
	}

	private void send(int keyCode, char keyChar, int modifiers) {
		REDViewController c = fEditor.getController();
		if (keyCode == KeyEvent.VK_UNDEFINED) {
			c.keyTyped(new KeyEvent(fEditor.getView(), KeyEvent.KEY_TYPED, now(), modifiers, keyCode, keyChar));
		}			
		else {
			c.keyPressed(new KeyEvent(fEditor.getView(), KeyEvent.KEY_PRESSED, now(), modifiers, keyCode));
		}
	}
	
	public static void assertEQuote(String str1, String str2) {
		assertEquals(RTestAuxiliary.quote(str1), RTestAuxiliary.quote(str2));
		assertEquals(str1, str2);	// just in case ...
	}

	public void testAutoIndent() {
		send(0, 't');	send(0, 'e'); send(0, 's'); send(0, 't'); send(0, ' '); send(0, '{');
		assertEQuote("test {", fEditor.asString());
		send(0, '\n');
		assertEQuote("test {\n\t", fEditor.asString());
		send(0, '\n');
		assertEQuote("test {\n\t\n\t", fEditor.asString());		
		send(0, 'x');
		send(0, '\n');
		assertEQuote("test {\n\t\n\tx\n\t", fEditor.asString());
		send(0, '}');
		assertEQuote("test {\n\t\n\tx\n}", fEditor.asString());
		send(0, '\n');
		assertEQuote("test {\n\t\n\tx\n}\n", fEditor.asString());
	}
		
	// test against bug #12
	public void testFirstLineGlitch() {
		send(0, 't');	send(0, 'e'); send(0, 's'); send(0, 't'); send(0, ' '); send(0, '{');
		assertEQuote("test {", fEditor.asString());
		fEditor.setSelection(0, 0);
		send(0, '\n');
		assertEQuote("\ntest {", fEditor.asString());
	}
	
	// test against bug #12
	public void testFirstLineException() {
		send(0, '\n');
	}
	
	// test against bug 207
	public void testUnindentWithSpaces() {
		fEditor.replace("    if (foo) {", 0, 0, null);
		send(0, '\n');
		assertEQuote("    if (foo) {\n    \t", fEditor.asString());
		send(0, '}');
		assertEQuote("    if (foo) {\n    }", fEditor.asString());
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDAutoIndent.class);
	}
	REDEditor fEditor;
	REDAutoIndent fIndent;
}
