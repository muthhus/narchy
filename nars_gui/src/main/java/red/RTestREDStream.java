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

import java.awt.*;
import junit.framework.*;
import red.plugins.synHi.*;

/** Regression test for REDStream
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDStream extends TestCase {
	public RTestREDStream(String name) {
		super(name);
	}
	
	public void setUp() throws Exception {
		fEditor = new REDEditor();
		fHighlighter = REDSyntaxHighlighterManager.createHighlighter("Java");
		fEditor.addPlugin(fHighlighter);
		fHighlighter.waitForParser();
		fEditor.replace(fcContent, 0, 0, null);
		fBuf = new StringBuffer();
	}
	
	public void tearDown() {
		fEditor.close();
	}
	
	public void testReadForward() {
		REDStream stream = fEditor.createStream(0, REDStreamDirection.FORWARD);
		char c = stream.readChar();
		while (!stream.eof()) {
			fBuf.append(c);
			c = stream.readChar();
		}
		assertEquals(fcContent, String.valueOf(fBuf));
		stream.close();
	}
	
	public void testReadBackward() {
		REDStream stream = fEditor.createStream(6, REDStreamDirection.BACKWARD);		
		char c = stream.readChar();
		while (!stream.eof()) {
			fBuf.append(c);
			c = stream.readChar();
		}
		assertEquals("tropmi", String.valueOf(fBuf));
		stream.close();
	}
	
	public void testRobustnessForward() {
		REDStream stream = fEditor.createStream(0, REDStreamDirection.FORWARD);
		for (int x = 0; x < 3; x++) {
			stream.readChar();
		}
		fEditor.replace("", 2, 5, null);
		assertEquals('t', stream.readChar());		
		fEditor.replace("x", 2, 2, null);
		assertEquals(' ', stream.readChar());
		fEditor.replace("z", 5, 5, null);
		assertEquals('z', stream.readChar());
		fEditor.replace("yy", 4, 7, null);
		assertEquals('y', stream.readChar());
		assertEquals('y', stream.readChar());
		assertEquals('a', stream.readChar());
		stream.close();
	}
	
	public void testRobustnessBackward() {
		REDStream stream = fEditor.createStream(6, REDStreamDirection.BACKWARD);
		for (int x = 0; x < 3; x++) {
			stream.readChar();
		}		
		fEditor.replace("", 2, 5, null);
		assertEquals('m', stream.readChar());
		fEditor.replace("abcd", 1, 1, null);
		assertEquals('d', stream.readChar());
		fEditor.replace("xxx", 7, 7, null);
		assertEquals('c', stream.readChar());
		fEditor.replace("yy", 2, 5, null);
		assertEquals('y', stream.readChar());
		assertEquals('y', stream.readChar());
		assertEquals('a', stream.readChar());		
		stream.close();
	}
	
	public void testSetPositionAndEof() {
		REDStream stream = fEditor.createStream(0, REDStreamDirection.FORWARD);
		assertEquals('i', stream.readChar());
		stream.setPosition(0);
		assertEquals('i', stream.readChar());
		stream.setPosition(-1);
		assertEquals('i', stream.readChar());
		stream.setPosition(-100);
		assertEquals('i', stream.readChar());
		stream.setPosition(6);
		assertEquals(' ', stream.readChar());
		stream.setPosition(fEditor.length()-1);
		assertEquals('\n', stream.readChar());
		assertEquals(false, stream.eof());
		assertEquals('\0', stream.readChar());
		assertEquals(true, stream.eof());
		stream.setPosition(0);
		assertEquals('i', stream.readChar());
		assertEquals(false, stream.eof());
		
		
		fEditor.replace("X", fEditor.length(), fEditor.length(), null);
		stream = fEditor.createStream(fEditor.length(), REDStreamDirection.BACKWARD);
		assertEquals('X', stream.readChar());
		stream.setPosition(fEditor.length());
		assertEquals('X', stream.readChar());
		stream.setPosition(fEditor.length() + 1);
		assertEquals('X', stream.readChar());
		stream.setPosition(fEditor.length() + 100);
		assertEquals('X', stream.readChar());
		stream.setPosition(1);
		assertEquals('i', stream.readChar());
		assertEquals(false, stream.eof());
		assertEquals('\0', stream.readChar());
		assertEquals(true, stream.eof());
		stream.setPosition(1);
		assertEquals('i', stream.readChar());
		assertEquals(false, stream.eof());		
		stream.close();
	}
	
	private static String readFile(REDStream stream) {
		StringBuilder buf = new StringBuilder();
		stream.setPosition(0);
		char c = stream.readChar();
		while (!stream.eof()) {
			buf.append(c);
			c = stream.readChar();
		}
		return String.valueOf(buf);
	}
	
	public void testSelectedStylesRecursive() {
		REDStream stream = fEditor.createStream(0, REDStreamDirection.FORWARD);
		stream.excludeStyle("Default", true);
		stream.includeStyle("Comment", true);
		assertEquals("/** Javadoc comment */// Read file\n", readFile(stream));

		stream.includeStyle("String", true);
		assertEquals("/** Javadoc comment */// Read file\n\"public\"", readFile(stream));
		
		stream.excludeStyle("Comment", true);
		assertEquals("\"public\"", readFile(stream));

		stream.excludeStyle("String", true);
		assertEquals("", readFile(stream));
		stream.close();
	}
	
	public void testSelectedStylesNonRecursive() {
		REDStream stream = fEditor.createStream(0, REDStreamDirection.FORWARD);
		stream.excludeStyle("Default", true);
		stream.includeStyle("Comment", false);
		assertEquals("// Read file\n", readFile(stream));
		stream.includeStyle("Comment", true);
		assertEquals("/** Javadoc comment */// Read file\n", readFile(stream));

		stream.excludeStyle("Comment", false);
		assertEquals("/** Javadoc comment */", readFile(stream));
		stream.close();
	}
	
	
	public void testSelectedStylesBackwardExtremeCase() {
		REDStyle style = new REDStyle(new Color(250, 100, 100), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 24, null);
		REDStyleManager.addStyle("testSelectedStylesBackwardExtremeCaseStyle", style);
		REDEditor editor = new REDEditor();
		editor.replace("X-----", 0, 0, null);
		editor.setStyle(0, 1, style);
		REDStream stream = editor.createStream(editor.length(), REDStreamDirection.BACKWARD);
		stream.excludeStyle("Default", true);
		stream.includeStyle("testSelectedStylesBackwardExtremeCaseStyle", false);
		assertEquals('X', stream.readChar());
		assertEquals(false, stream.eof());
		assertEquals('\0', stream.readChar());
		assertEquals(true, stream.eof());
		stream.close();
	}
	
	public void testSelectedStylesNoMatch() {
		REDStream stream = fEditor.createStream(0, REDStreamDirection.FORWARD);
		stream.excludeStyle("Default", true);
		assertEquals("", readFile(stream));
		stream.close();
	}
	
	public void testGetPosition() {
		REDStream stream = fEditor.createStream(5, REDStreamDirection.FORWARD);
		int expPos = 5;
		while (!stream.eof()) {
			assertEquals(expPos, stream.getPosition());
			stream.readChar();
			expPos++;
		}
		assertEquals(fEditor.length() + 1, expPos);
		assertEquals(fEditor.length(), stream.getPosition());

		stream = fEditor.createStream(5, REDStreamDirection.BACKWARD);
		expPos = 5;
		while (!stream.eof()) {
			assertEquals(expPos, stream.getPosition());
			stream.readChar();
			expPos--;
		}
		assertEquals(-1, expPos);
		assertEquals(0, stream.getPosition());
		stream.close();
	}
		
	public static Test suite() {
		return new TestSuite(RTestREDStream.class);
	}	

	StringBuffer fBuf;
	REDEditor fEditor;
	REDSyntaxHighlighter fHighlighter;
	private final static String fcContent = 
"import java.io.*;\n" +
		'\n' +
"/** Javadoc comment */\n" +
"public class Quoter {\n" +
"       public static void main(String [] args) throws IOException {\n" +
"               BufferedReader r = new BufferedReader(new InputStreamReader(System.in));\n" +
"               String publicStr;\n" +
		'\n' +
"				// Read file\n" +
"               s = r.readLine();\n" +
"               while (s != null) {\n" +
"                       System.out.println(\"public\");\n" +
"                       s = r.readLine();\n" +
"               }\n" +
"       }\n" +
"}\n";	
}
