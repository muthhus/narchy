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
 
package red.plugins.synHi;

import java.awt.*;
import java.io.*;
import junit.framework.*;
import red.*;

/** Regression test for REDSyntaxHighlighter
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDSyntaxHighlighter extends TestCase {
	public RTestREDSyntaxHighlighter(String name) {
		super(name);
	}
	
	public void setUp() {
		fDef = new REDSyntaxHighlighterDefinition("foo");
		fDef.addRule(new REDSyntaxHighlighterKeyword("public", fKWStyle));
		fEditor = new REDEditor();
		fEditor.replace(fgContent, 0, 0, null);
	}
	
	public void assertStyle(int from, int to, REDStyle style) {
		for (int x = from; x <= to; x++) {
			assertEquals("Style at " + x + " not as expected", style, fEditor.getStyle(x));
		}
	}
	
	public void testKeyword() throws InterruptedException {
		REDSyntaxHighlighter hi = new REDSyntaxHighlighter(fDef);
		fEditor.addPlugin(hi); hi.waitForParser();
		assertStyle(0, 19, fDefStyle);
		assertStyle(20, 25, fKWStyle);
		assertStyle(26, 48, fDefStyle);
		assertStyle(49, 54, fKWStyle);
		assertStyle(55, 342, fDefStyle);
		assertStyle(343, 348, fKWStyle);
		assertStyle(349, fEditor.length(), fDefStyle);
	}

	public void testCaseSensitive() throws InterruptedException {
		REDSyntaxHighlighter hi = new REDSyntaxHighlighter(fDef);
		fEditor.replace(fgContent.toUpperCase(), 0, fEditor.length(), null);
		fEditor.addPlugin(hi); hi.waitForParser();

		assertStyle(0, fEditor.length(), fDefStyle);
		fEditor.removePlugin(hi);
		
		fDef.setCaseSensitive(false);
		hi = new REDSyntaxHighlighter(fDef);
		fEditor.addPlugin(hi); hi.waitForParser();
		assertStyle(0, 19, fDefStyle);
		assertStyle(20, 25, fKWStyle);
		assertStyle(26, 48, fDefStyle);
		assertStyle(49, 54, fKWStyle);
		assertStyle(55, 342, fDefStyle);
		assertStyle(343, 348, fKWStyle);
		assertStyle(349, fEditor.length(), fDefStyle);		
	}

	
	public void testRange() throws InterruptedException {
		fDef.addRule(new REDSyntaxHighlighterRange("\"", "\"", fRangeStyle1));
		REDSyntaxHighlighter hi = new REDSyntaxHighlighter(fDef);
		fEditor.addPlugin(hi); hi.waitForParser();		
		assertStyle(0, 19, fDefStyle);
		assertStyle(20, 25, fKWStyle);
		assertStyle(26, 48, fDefStyle);
		assertStyle(49, 54, fKWStyle);
		assertStyle(55, 341, fDefStyle);
		assertStyle(342, 349, fRangeStyle1);
		assertStyle(350, fEditor.length(), fDefStyle);
	}
	
	public void testSubparsers() throws InterruptedException {
		REDSyntaxHighlighterRange range = new REDSyntaxHighlighterRange("\"", "\"", fRangeStyle1);
		range.addSubParser(new REDSyntaxHighlighterKeyword("\\\\.", fQuotedStyle));
		range.addSubParser(new REDSyntaxHighlighterRange("'", "'", fRangeStyle2));
		fDef.addRule(range);
		REDSyntaxHighlighter hi = new REDSyntaxHighlighter(fDef);
		fEditor.replace("public String foo = \"'You cannot pass!', cried Gandalf. \\\"I am a wielder of the white flame of Anor\\\"\";", 0, fEditor.length(), null);
		fEditor.addPlugin(hi); hi.waitForParser();				
		assertStyle(0, 6, fKWStyle);
		assertStyle(7, 20, fDefStyle);
		assertStyle(21, 21, fRangeStyle1);
		assertStyle(22, 39, fRangeStyle2);
		assertStyle(40, 56, fRangeStyle1);
		assertStyle(57, 58, fQuotedStyle);
		assertStyle(59, 99, fRangeStyle1);
		assertStyle(100, 101, fQuotedStyle);
		assertStyle(102, 102, fRangeStyle1);
		assertStyle(103, fEditor.length(), fDefStyle);
	}
	
	public void testRewind() throws InterruptedException {
		REDSyntaxHighlighterRange range = new REDSyntaxHighlighterRange("\"", "\"", fRangeStyle1);
		REDSyntaxHighlighterRange subRange = new REDSyntaxHighlighterRange("'", "'|\"", fRangeStyle2);
		subRange.setRewind(true);
		range.addSubParser(subRange);
		fDef.addRule(range);
		REDSyntaxHighlighter hi = new REDSyntaxHighlighter(fDef);
		fEditor.replace("public String foo = \"'I am a wielder of the white flame of Anor\";public", 0, fEditor.length(), null);
		fEditor.addPlugin(hi); hi.waitForParser();				
		assertStyle(0, 6, fKWStyle);
		assertStyle(7, 20, fDefStyle);
		assertStyle(21, 21, fRangeStyle1);
		assertStyle(22, 64, fRangeStyle2);
		assertStyle(65, 65, fDefStyle);
		assertStyle(66, fEditor.length(), fKWStyle);
	}
	
	public void testSaveAs() throws InterruptedException {
		REDEditor editor1 = new REDEditor(); 
		REDSyntaxHighlighter hi1 = REDSyntaxHighlighterManager.createHighlighter("C++"); editor1.addPlugin(hi1);
		editor1.replace("/**************************************************************************\n*\n*\n*/", 0, 0, null);
		hi1.waitForParser();

		assertEquals(REDStyleManager.getStyle("Comment"), editor1.getStyle(editor1.length()-1));
		
		assertTrue(editor1.saveFileAs("RTestREDSyntaxHighlighter.1.tmp", false));
		
 		editor1.replace("/**************************************************************************\n*\n*\n*/", 0, editor1.length(), null);
		hi1.waitForParser();
		assertTrue(editor1.getStyle(editor1.length()-1) == REDStyleManager.getStyle("Comment"));
		editor1.close();
		assertTrue(new File("RTestREDSyntaxHighlighter.1.tmp").delete());
	}
	
	public void testJavaFloat() throws InterruptedException {
		REDSyntaxHighlighter hi = REDSyntaxHighlighterManager.createHighlighter("Java");
		fEditor.addPlugin(hi);
		fEditor.replace("f", 0, fEditor.length(), null);
		hi.waitForParser();
		assertEquals(fEditor.getDefaultStyle(), fEditor.getStyle(0));
		assertEquals(fEditor.getDefaultStyle(), fEditor.getStyle(1));
	}
					
	public static Test suite() {
		return new TestSuite(RTestREDSyntaxHighlighter.class);
	}
	
	REDSyntaxHighlighterDefinition fDef;
	REDEditor fEditor;
	REDStyle fKWStyle = new REDStyle(new Color(250, 100, 100), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 24, null);
	REDStyle fRangeStyle1 = new REDStyle(new Color(0, 100, 100), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 12, null);
	REDStyle fRangeStyle2 = new REDStyle(new Color(0, 100, 0), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 12, null);
	REDStyle fQuotedStyle = new REDStyle(new Color(255, 100, 0), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 12, null);
	REDStyle fDefStyle = REDStyleManager.getDefaultStyle();
	private static final String fgContent =
"import java.io.*;\n" +
		'\n' +
"public class Quoter {\n" +
"       public static void main(String [] args) throws IOException {\n" +
"               BufferedReader r = new BufferedReader(new InputStreamReader(System.in));\n" +
"               String publicStr;\n" +
		'\n' +
"               s = r.readLine();\n" +
"               while (s != null) {\n" +
"                       System.out.println(\"public\");\n" +
"                       s = r.readLine();\n" +
"               }\n" +
"       }\n" +
"}\n";

}
