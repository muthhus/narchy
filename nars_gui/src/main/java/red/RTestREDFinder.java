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
import red.rexParser.*;
import java.util.*;
import javax.swing.*;

/** Test case for REDFinder.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDFinder extends TestCase {
	public RTestREDFinder(String name) {
		super(name);
	}
	
	public void setUp() {
		fEditor = new REDEditor();
		fFinder = new REDFinder();
		fFinder.setEditor(fEditor);
		fEditor.replace(fcFileContent, 0, fEditor.length(), null);
	}
	
	public void testQuoteWithBackslash() {
		assertEquals("C\\:\\\\Foo", REDFinder.quoteWithBackslash("C:\\Foo", "\\:"));
		assertEquals("3 \\* 4", REDFinder.quoteWithBackslash("3 * 4", "*"));
	}
	
	public void testFind() throws REDRexMalformedPatternException {
//	public void find(String pattern, int from, REDFinderDirection direction, boolean caseSensitive, 
//  boolean wholeWord, boolean useRegExp, boolean addToHistory, boolean all, REDRexAction action) throws REDRexMalformedPatternException { 		
		TestAction act = new TestAction();
		
		// Test forward search
		fFinder.find("the", fEditor.getLineStart(2), REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("1/12/15, 4/12/15, 5/5/8, 6/16/19, 7/4/7, ", act.getLog()); act.clearLog();
		fFinder.find("the", fEditor.getLineStart(2), REDFinderDirection.FORWARD, true, false, true, true, false, act);
		assertEquals("1/12/15, ", act.getLog()); act.clearLog();
		
		// Test backward search
		fFinder.find("the", fEditor.getLineStart(3) + 15, REDFinderDirection.BACKWARD, true, false, true, true, true, act);
		assertEquals("0/12/15, 2/4/7, 3/16/19, 4/5/8, 5/12/15, ", act.getLog()); act.clearLog();
		fFinder.find("the", fEditor.getLineStart(3) + 15, REDFinderDirection.BACKWARD, true, false, true, true, false, act);
		assertEquals("0/12/15, ", act.getLog()); act.clearLog();

		// case insensitive
		fFinder.find("the", fEditor.getLineStart(2), REDFinderDirection.FORWARD, false, false, true, true, true, act);
		assertEquals("1/0/3, 1/12/15, 4/12/15, 5/5/8, 6/16/19, 7/4/7, ", act.getLog()); act.clearLog();

		// whole word
		fFinder.find("the", fEditor.getLineStart(2), REDFinderDirection.FORWARD, false, true, true, true, true, act);
		assertEquals("1/0/3, 1/12/15, 4/12/15, 6/16/19, ", act.getLog()); act.clearLog();
		
		// without regexp
		fFinder.find("s.", fEditor.getLineStart(2), REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("1/20/22, ", act.getLog()); act.clearLog();
		
		fEditor.replace("$^*[]+?.\\", 0, fEditor.length(), null);
		fFinder.find("$", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/0/1, ", act.getLog()); act.clearLog();
		fFinder.find("^", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/1/2, ", act.getLog()); act.clearLog();
		fFinder.find("*", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/2/3, ", act.getLog()); act.clearLog();
		fFinder.find("[", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/3/4, ", act.getLog()); act.clearLog();
		fFinder.find("]", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/4/5, ", act.getLog()); act.clearLog();
		fFinder.find("+", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/5/6, ", act.getLog()); act.clearLog();
		fFinder.find("?", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/6/7, ", act.getLog()); act.clearLog();
		fFinder.find(".", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/7/8, ", act.getLog()); act.clearLog();
		fFinder.find("\\", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		assertEquals("0/8/9, ", act.getLog()); act.clearLog();
	}
	
	public void testEmpty() throws REDRexMalformedPatternException {
		TestAction act = new TestAction();
		fFinder.find("", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		fFinder.find("", 0, REDFinderDirection.FORWARD, false, true, true, true, true, act);
		fEditor.replace("", 0, fEditor.length(), null);
		fFinder.find("the", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		fFinder.find("the", 0, REDFinderDirection.FORWARD, false, true, true, true, true, act);
		fFinder.find("", 0, REDFinderDirection.FORWARD, true, false, false, true, true, act);
		fFinder.find("", 0, REDFinderDirection.FORWARD, false, true, true, true, true, act);
	}

	private String findHistoryString() {
		StringBuilder buf = new StringBuilder();
		ListModel m = fFinder.getFindHistory();
		for (int x = 0; x < m.getSize(); x++) {
			buf.append("").append(m.getElementAt(x));
		}
		return String.valueOf(buf);
	}
	
	private String findHistoryCurrent() {
		return String.valueOf(fFinder.getFindHistory().getSelectedItem());
	}
	
	public void testFindHistory() throws REDRexMalformedPatternException {
		TestAction act = new TestAction();
		
		fFinder.setHistorySize(5);
		assertEquals("", findHistoryString());
		fFinder.find("A", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("A", findHistoryString());
		assertEquals("A", findHistoryCurrent());
		fFinder.find("B", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("BA", findHistoryString());
		assertEquals("B", findHistoryCurrent());
		fFinder.find("C", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("CBA", findHistoryString());
		assertEquals("C", findHistoryCurrent());
		fFinder.find("A", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("ACB", findHistoryString());
		assertEquals("A", findHistoryCurrent());
		fFinder.find("C", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("CAB", findHistoryString());
		assertEquals("C", findHistoryCurrent());
		fFinder.find("D", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		fFinder.find("E", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("EDCAB", findHistoryString());
		assertEquals("E", findHistoryCurrent());
		fFinder.find("F", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("FEDCA", findHistoryString());
		assertEquals("F", findHistoryCurrent());
		fFinder.find("A", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("AFEDC", findHistoryString());
		assertEquals("A", findHistoryCurrent());
		fFinder.find("E", 0, REDFinderDirection.FORWARD, true, false, true, true, true, act);
		assertEquals("EAFDC", findHistoryString());
		assertEquals("E", findHistoryCurrent());
		fFinder.find("G", 0, REDFinderDirection.FORWARD, true, false, true, false, true, act);
		assertEquals("EAFDC", findHistoryString());
		assertEquals("E", findHistoryCurrent());
	}
	
	private String replaceHistoryString() {
		StringBuilder buf = new StringBuilder();
		ListModel m = fFinder.getReplaceHistory();
		for (int x = 0; x < m.getSize(); x++) {
			buf.append("").append(m.getElementAt(x));
		}
		return String.valueOf(buf);
	}

	public void testReplaceHistory() throws REDRexMalformedPatternException {
		TestAction act = new TestAction();
		
		fFinder.setHistorySize(5);
		assertEquals("", replaceHistoryString());
		fFinder.replace("A", "A", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("A", replaceHistoryString());
		fFinder.replace("B", "B", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("BA", replaceHistoryString());
		fFinder.replace("C", "C", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("CBA", replaceHistoryString());
		fFinder.replace("A", "A", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("ACB", replaceHistoryString());
		fFinder.replace("C", "C", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("CAB", replaceHistoryString());
		fFinder.replace("D", "D", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		fFinder.replace("E", "E", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("EDCAB", replaceHistoryString());
		fFinder.replace("F", "F", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("FEDCA", replaceHistoryString());
		fFinder.replace("A", "A", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("AFEDC", replaceHistoryString());
		fFinder.replace("E", "E", 0, REDFinderDirection.FORWARD, true, false, true, true, REDFinderReplaceAllDirective.FILE);
		assertEquals("EAFDC", replaceHistoryString());
		fFinder.replace("G", "G", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals("EAFDC", replaceHistoryString());
	}

	
	public void testHistorySize() {
		assertEquals(REDFinder.fcDefaultHistorySize, fFinder.getHistorySize());
		fFinder.setHistorySize(20);
		assertEquals(20, fFinder.getHistorySize());
	}
	
	private static String strReplace(String in, String pattern, String replacement) {
		StringBuilder buf = new StringBuilder();
		int idx = in.indexOf(pattern);
		int lastPos = 0;
		while (idx != -1) {
			buf.append(in.substring(lastPos, idx));
			buf.append(replacement);
			idx += pattern.length();
			lastPos = idx;
			idx = in.indexOf(pattern, idx);
		}
		buf.append(in.substring(lastPos));
		return String.valueOf(buf);
	}
	
	public void testReplaceSelection() throws REDRexMalformedPatternException {
		fFinder.replace("on", "off", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.OFF);
		assertEquals(1, fEditor.getSelectionStart());
		assertEquals(4, fEditor.getSelectionEnd());
	}

	public void testReplace() throws REDRexMalformedPatternException {
		fFinder.replace("o", "X", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals(strReplace(fcFileContent, "o", "X"), fEditor.asString());
		fEditor.replace(fcFileContent, 0, fEditor.length(), null);

		fFinder.replace("o", "", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals(strReplace(fcFileContent, "o", ""), fEditor.asString());
		fEditor.replace(fcFileContent, 0, fEditor.length(), null);

		fFinder.replace("e", "XXX", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals(strReplace(fcFileContent, "e", "XXX"), fEditor.asString());
		fEditor.replace(fcFileContent, 0, fEditor.length(), null);

		fFinder.replace("the", "foo", 0, REDFinderDirection.FORWARD, false, false, false, false, REDFinderReplaceAllDirective.FILE);
		String expected = strReplace(fcFileContent, "the", "foo");
		expected = strReplace(expected, "The", "foo");
		assertEquals(expected, fEditor.asString());
		fEditor.replace(fcFileContent, 0, fEditor.length(), null);
		
		int oriLines = fEditor.getNrOfLines();
		int oriLength = fEditor.length();
		fFinder.replace("\n", "\r\n", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals(strReplace(fcFileContent, "\n", "\r\n"), fEditor.asString());
		assertEquals(oriLines, fEditor.getNrOfLines());
		assertEquals(oriLength + oriLines - 1, fEditor.length());
		fFinder.replace("\r\n", "\n", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals(fcFileContent, fEditor.asString());
		assertEquals(oriLines, fEditor.getNrOfLines());
		assertEquals(oriLength, fEditor.length());
		
		fEditor.replace(fcFileContent, 0, fEditor.length(), null);
	}
	
	public void testBackrefs() throws REDRexMalformedPatternException {
		fEditor.replace("The road goes ever on and on", 0, fEditor.length(), null);
		fFinder.replace("(.)e", "e\\1", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals("Teh road geose evr on and on", fEditor.asString());
		
		fEditor.replace("Regen", 0, fEditor.length(), null);
		fFinder.replace("(.)(.)(.)(.)(.)", "\\5\\4\\3\\2\\1", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals("negeR", fEditor.asString());
		
		fEditor.replace("aaabaaa", 0, fEditor.length(), null);
		fFinder.replace("(.)b", "\\", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals("aa\\aaa", fEditor.asString());
		
		fEditor.replace("aaabaaa", 0, fEditor.length(), null);
		fFinder.replace("(.)b", "\\\\1", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals("aa\\aaaa", fEditor.asString());
		
		fEditor.replace("aaabaaa", 0, fEditor.length(), null);
		fFinder.replace("(.)b", "\\2", 0, REDFinderDirection.FORWARD, true, false, true, false, REDFinderReplaceAllDirective.FILE);
		assertEquals("aaaaa", fEditor.asString());
	}
	
	public void testMultipleMatchesOnOneLine() throws REDRexMalformedPatternException {
		TestAction act = new TestAction();
		
		fFinder.find("e", 0, REDFinderDirection.FORWARD, true, false, true, true, false, act);
		assertEquals("0/7/8, ", act.getLog()); act.clearLog();
		fFinder.find("e", 8, REDFinderDirection.FORWARD, true, false, true, true, false, act);
		assertEquals("0/3/4, ", act.getLog()); act.clearLog();
		fFinder.find("e", 12, REDFinderDirection.FORWARD, true, false, true, true, false, act);
		assertEquals("0/6/7, ", act.getLog()); act.clearLog();
	}


	/** 
	 * Tests, whether the object returned by <CODE>getIntance</CODE> is always
	 * the same.
	 */
	public void testGetInstance() {
		REDFinder  finder = REDFinder.getInstance();
		assertSame("getInstance returned not the same object.", finder,
				REDFinder.getInstance());
	}
	
	/**
	 * Tests the getter for the editor property of the <CODE>REDFinder</CODE>.
	 */
	public void testGetEditor() {
		assertEquals("Wrong REDEditor returned.", fEditor, fFinder.getEditor());
	}
			
	public static Test suite() {
		return new TestSuite(RTestREDFinder.class);
	}

	REDEditor fEditor;
	REDFinder fFinder;
	static final String fcFileContent = 
"Don't meddle in the affairs of wizards,\n" +
"for they are subtle and quick to anger.\n" +
		'\n' +
"The Lord of the Rings.\n" +
		'\n' +
"And it is also said: \n" +
"\t\"Don't ask the elves for counsel,\n" +
"\tfor they will both say yes and no\".";

	static class LogEntry {
		int line, from, to;
		Object emitObj;
	}

	public class TestAction extends REDRexAction {
		public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
			LogEntry entry = new LogEntry(); entry.line = line; entry.from = match.getStart(0); entry.to = match.getEnd(0); entry.emitObj = match.getEmitObj();
			fLog.add(entry);
		}
		
		String getLog() {
			StringBuffer buf = new StringBuffer();
			Iterator iter = fLog.iterator();
			while (iter.hasNext()) {
				LogEntry e = (LogEntry) iter.next();
				buf.append(e.line).append("/").append(e.from).append('/').append(e.to).append(", ");
			}
			return new String(buf);
		}
		
		void clearLog() {
			fLog = new ArrayList();
		}
		
		ArrayList fLog = new ArrayList();
	}

}
