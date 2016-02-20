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
 
package red.plugins.brMatcher;

import java.io.*;
import junit.framework.*;
import red.*;
import red.util.*;
import red.plugins.synHi.*;

/** Regression test for REDBracketMatcher
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDBracketMatcher extends RTestControllerBase {
	public RTestREDBracketMatcher(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		readAdditionalMatcherDefs("ok.xml");
	}
		
	private void assertHighlit(int pos) {
		assertEquals(REDStyleManager.getStyle("KeywordControl"), fEditor.getStyle(pos + 1));
	}
	
	private void assertNotHighlit() {
		for (int x = 0; x <= fEditor.length(); x++) {
			assertEquals("Found wrong style before gap: " + x, REDStyleManager.getDefaultStyle(), fEditor.getStyle(x));
		}
	}
	
	public void testHighlighting() throws Exception {
		fEditor.replace("if (foo()(()) == 0)", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));
		
		// before left
		fEditor.setSelection(3); assertHighlit(18);
		fEditor.setSelection(0); assertNotHighlit();

		// after left
		fEditor.setSelection(4); assertHighlit(18);
		
		// before right 
		fEditor.setSelection(18); assertHighlit(3);

		// after right 
		fEditor.setSelection(19); assertHighlit(3);
		
		// could highlight after left and before right => will take before right
		fEditor.setSelection(8); assertHighlit(7);
		
		// could highlight after right and before left => will take after right
		fEditor.setSelection(9); assertHighlit(7);
		
		// could highlight before or after left => will take before left
		fEditor.setSelection(10); assertHighlit(11);
		
		// could highlight before or after right => will take after right
		fEditor.setSelection(12); assertHighlit(10);
		
		// normal, nested case
		fEditor.setSelection(13); assertHighlit(9);
	}	
	
	private void assertNotHighlitWithBracketMatcherStyle() {
		for (int x = 0; x <= fEditor.length(); x++) {
			assertTrue("Unexpected highlighting at " + x, fEditor.getStyle(x) != REDStyleManager.getStyle("KeywordControl"));
		}
	}
	
	
	public void testIgnoredStyles() throws Exception {
		REDSyntaxHighlighter hi = REDSyntaxHighlighterManager.createHighlighter("Java");
		fEditor.addPlugin(hi);
		hi.waitForParser();
		fEditor.replace("if (foo() == ')')", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));
		
		// Test, whether closing bracket in single quotes is ignored.
		fEditor.setSelection(3); assertHighlit(16);
		fEditor.setSelection(4); assertHighlit(16);
		fEditor.setSelection(16); assertHighlit(3);
		fEditor.setSelection(17); assertHighlit(3);
		
		// These should be totally unaffected
		fEditor.setSelection(7); assertHighlit(8);
		fEditor.setSelection(8); assertHighlit(7);
		fEditor.setSelection(9); assertHighlit(7);
		
		// There should be no highlighting at all for the closing bracket in single quotes.
		fEditor.setSelection(14); assertNotHighlitWithBracketMatcherStyle();
		fEditor.setSelection(15); assertNotHighlitWithBracketMatcherStyle();
	}
	
	public void testMaxParameters() {
		fEditor.replace("if (foo == ')') {\n\tdoIt();\n\tdoIt();\n\tdoIt();\n}", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));

		// too many lines and chars
		assertEquals("}", fEditor.copy(45, 46));
		fEditor.setSelection(16); assertNotHighlit();
		fEditor.setSelection(17); assertNotHighlit();
		fEditor.setSelection(45); assertNotHighlit();
		fEditor.setSelection(46); assertNotHighlit();
		
		// remove some chars, still too many lines
		fEditor.replace("", 39, 43, null);
		assertEquals("}", fEditor.copy(41, 42));
		fEditor.setSelection(16); assertNotHighlit();
		fEditor.setSelection(17); assertNotHighlit();
		fEditor.setSelection(41); assertNotHighlit();
		fEditor.setSelection(42); assertNotHighlit();

		// remove one linebreak => must work then
		fEditor.replace("", 35, 36, null);
		assertEquals("}", fEditor.copy(40, 41));
		fEditor.setSelection(16); assertHighlit(40);
		fEditor.setSelection(17); assertHighlit(40);
		fEditor.setSelection(40); assertHighlit(16);
		fEditor.setSelection(41); assertHighlit(16);
		
		// pump in characters => lines would be ok, but characters are too many
		fEditor.replace("XXXXXXX", 40, 40, null);
		assertEquals("}", fEditor.copy(47, 48));
		fEditor.setSelection(16); assertNotHighlit();
		fEditor.setSelection(17); assertNotHighlit();
		fEditor.setSelection(47); assertNotHighlit();
		fEditor.setSelection(48); assertNotHighlit();
	}
	
	public void testMaxLineWithDOSStyle() {
		fEditor.replace("{\r\nxxx\r\nxxx\r\n}", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));
		fEditor.setSelection(0); assertHighlit(13);
		fEditor.setSelection(1); assertHighlit(13);
		fEditor.setSelection(13); assertHighlit(0);
		fEditor.setSelection(14); assertHighlit(0);
	}
	
	public void testDoubleClickSelection() {
		fEditor.replace("if (foo()(()) == 0)", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));
		
		// double clicking should only work within brackets, here we have no word either
		mouseClick(3, LEFT, 2);
		assertEquals(3, fEditor.getSelectionStart()); assertEquals(3, fEditor.getSelectionEnd());
		
		// now we're within the first bracket.
		mouseClick(4, LEFT, 2);
		assertEquals(4, fEditor.getSelectionStart()); assertEquals(18, fEditor.getSelectionEnd());
		mouseClick(18, LEFT, 2);
		assertEquals(4, fEditor.getSelectionStart()); assertEquals(18, fEditor.getSelectionEnd());
		
		// double clicking foo after the 'f' should still result in normal word selection
		mouseClick(5, LEFT, 2);
		assertEquals(4, fEditor.getSelectionStart()); assertEquals(7, fEditor.getSelectionEnd());
		
		// this pair of brackets is empty, thus this should behave like a simple click
		mouseClick(8, LEFT, 2);
		assertEquals(8, fEditor.getSelectionStart()); assertEquals(8, fEditor.getSelectionEnd());
		
		// now test the outer brackets of the nested case
		mouseClick(10, LEFT, 2);
		assertEquals(10, fEditor.getSelectionStart()); assertEquals(12, fEditor.getSelectionEnd());
		mouseClick(12, LEFT, 2);
		assertEquals(10, fEditor.getSelectionStart()); assertEquals(12, fEditor.getSelectionEnd());
		
		// the inner brackets of the nested case are empty, should behave like a simple click
		mouseClick(11, LEFT, 2);
		assertEquals(11, fEditor.getSelectionStart()); assertEquals(11, fEditor.getSelectionEnd());
		
		// finally, ensure that triple click still has it's normal behaviour
		for (int x = 0; x <= fEditor.length(); x++) {
			mouseClick(x, LEFT, 3);
			assertEquals(0, fEditor.getSelectionStart()); assertEquals(fEditor.length(), fEditor.getSelectionEnd());
		}
	}
	
	public void testCommentHighlighting() throws Exception {
		REDSyntaxHighlighter hi = REDSyntaxHighlighterManager.createHighlighter("Java");
		fEditor.addPlugin(hi);
		hi.waitForParser();
		fEditor.replace("if /**Nice comment */ foo", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));
		
		// double clicking should only work within brackets, here we have no word either
		mouseClick(5, LEFT, 2);
		assertEquals(5, fEditor.getSelectionStart()); assertEquals(19, fEditor.getSelectionEnd());
		
		mouseClick(8, LEFT, 1);
		assertEquals(8, fEditor.getSelectionStart()); assertEquals(8, fEditor.getSelectionEnd());
		
		mouseClick(19, LEFT, 2);
		assertEquals(5, fEditor.getSelectionStart()); assertEquals(19, fEditor.getSelectionEnd());
		
		mouseClick(3, LEFT, 1);
		assertHighlit(19); 
		assertHighlit(20);
		
		mouseClick(5, LEFT, 1);
		assertHighlit(19); 
		assertHighlit(20);

		mouseClick(19, LEFT, 1);
		assertHighlit(3); 
		assertHighlit(4);

		mouseClick(21, LEFT, 1);
		assertHighlit(3); 
		assertHighlit(4);
	}
	
	public void testStringHighlighting() throws Exception {
		REDSyntaxHighlighter hi = REDSyntaxHighlighterManager.createHighlighter("Java");
		fEditor.addPlugin(hi);
		hi.waitForParser();
		fEditor.replace("x = \"X X\\\"\" + x", 0, fEditor.length(), null);
		fEditor.addPlugin(REDBracketMatcherManager.createMatcher("Test1"));
		
		// double clicking should only work within brackets, here we have no word either
		mouseClick(5, LEFT, 2);
		assertEquals(5, fEditor.getSelectionStart()); assertEquals(10, fEditor.getSelectionEnd());
		
		mouseClick(10, LEFT, 2);
		assertEquals(5, fEditor.getSelectionStart()); assertEquals(10, fEditor.getSelectionEnd());
		
		mouseClick(9, LEFT, 2);
		assertEquals(9, fEditor.getSelectionStart()); assertEquals(9, fEditor.getSelectionEnd());

		mouseClick(4, LEFT, 1);
		assertHighlit(10); 
		
		mouseClick(5, LEFT, 1);
		assertHighlit(10); 
		
		mouseClick(9, LEFT, 1);
		assertNotHighlitWithBracketMatcherStyle(); 

		mouseClick(10, LEFT, 1);
		assertHighlit(4); 
		
		mouseClick(11, LEFT, 1);
		assertHighlit(4); 
	}
	
	
	private static void readAdditionalMatcherDefs(String pattern) {
		REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("red/plugins/brMatcher", pattern);
		while (iter.hasNext()) {
			InputStream is = (InputStream) iter.next();
			REDBracketMatcherManager.readMatcherDefinition(is, String.valueOf(iter.curName()));
		}
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDBracketMatcher.class);
	}
}
