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

/** JUnit test class for the line handling methods of REDText. 
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDTextLineHandling extends RTestREDText {
	/**
	 * Returns a test suite holding all the line handling tests.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDTextLineHandling.class);
	}
	
	/**
	 * Constructs a new RTestREDTextLineHandling object.
	 *
	 * @param name the name of the method to test, when for adding to a suite.
	 */
	public RTestREDTextLineHandling(String name) {
		super(name);
	}
	
	/**
	 * Returns the number of line breaks in the given string. Valid line
	 * breaks are: '\n', '\r' and "\r\n".
	 *
	 * @param testString the string in which to count the line breaks.
	 */
	private static int getNrOfLineBreaks(String testString) {
		byte[] byteArray = testString.getBytes();
		int nrOfBreaks = 0;
		
		for (int i = 0; i < byteArray.length; i++) {
			if (byteArray[i] == '\r') {
				nrOfBreaks++;
			}
			// carriage return followed by line feed is one break
			if (byteArray[i] == '\n' && byteArray[i -1] != '\r') {
				nrOfBreaks++;
			}
		}
		return nrOfBreaks;
	}
	
	/**
	 * Modifies the line breaks of the given REDText object. The first line
	 * feed character ('\n') is replaced by a carriage return ('\r'). Before
	 * the second line feed a carriage return is inserted.
	 *
	 * @param text the REDText object, whose line breaks should be modified.
	 */
	private static void modifyLineBreaks(REDText text) {
		String textString = text.asString();
		int firstLB = textString.indexOf('\n');
		int secLB = textString.indexOf('\n', firstLB + 1);
		text.replace(firstLB, firstLB + 1, "\r"); // replace \n by \r
		assertEquals("Wrong length after replace of line feed.",
				TEXT_CONTENT.length(), text.length());
		text.replace(secLB, secLB, "\r"); // add \r before \n
		assertEquals("Wrong length after adding one carriage return.",
				TEXT_CONTENT.length() + 1, text.length());
	}
	
	/**
	 * Tests the method getNrOfLines() on a normal text.
	 */
	public void testGetNrOfLines() {
		REDText text = getTestText();
		String textString = text.asString();
		int proposedLines = getNrOfLineBreaks(TEXT_CONTENT) + 1;
		int firstLB = textString.indexOf('\n');
		int secLB = textString.indexOf('\n', firstLB + 1);
		
		// modify text with different line break characters
		// it's the same, what modifyLineBreaks does, but with some asserts
		// in it simplify to find the errors
		text.replace(firstLB, firstLB + 1, "\r"); // replace \n by \r
		assertEquals("Wrong length after replace of line feed.",
				TEXT_CONTENT.length(), text.length());
		assertEquals("REDText has wrong number of lines after replace of line feed.",
				proposedLines, text.getNrOfLines());
		text.replace(secLB, secLB, "\r"); // add \r before \n
		assertEquals("Wrong length after adding one carriage return.",
				TEXT_CONTENT.length() + 1, text.length());
		assertEquals("REDText has wrong number of lines after adding one carriage return.",
				proposedLines, text.getNrOfLines());
	}

	/**
	 * Tests the method getNrOfLines() on an empty text.
	 */
	public void testGetNrOfLinesEmptyText() {
		REDText text = getEmptyTestText();
		assertEquals("REDText has wrong number of lines.", 1,text.getNrOfLines());
	}
	
	/**
	 * Tests the method getLineForPosition() on a normal text at the begin of
	 * the text.
	 */
	public void testGetLineForPositionBegion() {
		REDText text = getTestText();
		assertEquals("Wrong position for line at the begin.", 0,
				text.getLineForPosition(0));
	}
	
	/**
	 * Tests the method getLineForPosition() on a normal text in the middle of
	 * the text.
	 */
	public void testGetLineForPositionMiddle() {
		REDText text = getTestText();
		assertEquals("Wrong position for line in the middle.", 1,
				text.getLineForPosition(text.length() / 2));
	}
	
	/**
	 * Tests the method getLineForPosition() on a normal text at the end of
	 * the text.
	 */
	public void testGetLineForPositionEnd() {
		REDText text = getTestText();
		assertEquals("Wrong position for line at the end.", 4,
				text.getLineForPosition(text.length()));
	}
	
	/**
	 * Tests the method getLineForPosition() on a normal text after the end of
	 * the text.
	 */
	public void testGetLineForPositionGreaterLenght() {
		REDText text = getTestText();
		assertEquals("Wrong position for line after the end.", 4,
				text.getLineForPosition(text.length() + 1));
	}
	
	/**
	 * Tests the method getLineForPosition() on a normal text before the begin
	 * of the text.
	 */
	public void testGetLineForPositionBeforeBegin() {
		REDText text = getTestText();
		assertEquals("Wrong position for line before the begin.", 0,
				text.getLineForPosition(-1));
	}

	/**
	 * Tests the method getLineForPosition() on a normal text exactly on the
	 * position of a line break character.
	 */
	public void testGetLineForPositionLineBreak() {
		REDText text = getTestText();
		int pos = text.toString().indexOf('\n');
		assertEquals("Wrong position for line at line break.", 0,
				text.getLineForPosition(pos));
	}

	/**
	 * Tests the method getLineLength() on normal lines with different line
	 * break characters.
	 */
	public void testGetLineLength() {
		REDText text = getTestText();
		int lb1 = TEXT_CONTENT.indexOf('\n');
		// length of line 0 incl. line break
		int len0 = TEXT_CONTENT.substring(0, lb1).length() + 1; 
		// length of line 1 incl. line break
		int len1 = TEXT_CONTENT.substring(lb1 + 1,
				TEXT_CONTENT.indexOf('\n', lb1 + 1)).length() + 1;
		assertEquals("Line number 0 has wrong length incl. linebreak character \\n",
				len0, text.getLineLength(0, true));
		assertEquals("Line number 0 has wrong length excl. linebreak characters \\n",
				len0 - 1, text.getLineLength(0, false));
		assertEquals("Line number 1 has wrong length incl. linebreak character \\n",
				len1, text.getLineLength(1, true));
		assertEquals("Line number 1 has wrong length excl. linebreak characters \\n",
				len1 - 1, text.getLineLength(1, false));
		modifyLineBreaks(text);
		len1++;		// line one ends with \r\n now
		assertEquals("Line number 0 has wrong length incl. linebreak character \\r.",
				len0, text.getLineLength(0, true));
		assertEquals("Line number 0 has wrong length excl. linebreak characters \\r",
				len0 - 1, text.getLineLength(0, false));
		assertEquals("Line number 1 has wrong length incl. linebreak characters \\r\\n",
				len1, text.getLineLength(1, true));
		assertEquals("Line number 1 has wrong length excl. linebreak characterss \\r\\n",
				len1 - 2, text.getLineLength(1, false));
	}

	/**
	 * Tests the method getLineLength() on empty lines with different line
	 * break characters.
	 */
	public void testGetLineLengthEmptyLine() {
		REDText text = getTestText();
		
		assertEquals("Empty line has wrong length incl. linebreak character \\n",
				1, text.getLineLength(2, true));
		assertEquals("Empty line has wrong length excl. linebreak character \\n",
				0, text.getLineLength(2, false));
		text = getEmptyTestText();
		text.replace(0, 0, "\r");		// text with two lines (one carriage return)
		assertEquals("Empty line has wrong length incl. linebreak character \\r",
				1, text.getLineLength(0, true));
		assertEquals("Empty line has wrong length excl. linebreak character \\r",
				0, text.getLineLength(0, false));
		text.replace(0, 2, "\r\n");		// text with two lines (one \r and one \n)
		assertEquals("Empty line has wrong length incl. linebreak character \\r\\n",
				2, text.getLineLength(0, true));
		assertEquals("Empty line has wrong length excl. linebreak character \\r\\n",
				0, text.getLineLength(0, false));		
	}
	
	/**
	 * Tests the method getLineLength() with wrong line numbers. These are
	 * numbers lower 0, equal nr of lines and greater nr of lines.
	 */
	public void testGetLineLengthWrongLineNumber() {
		REDText text = getTestText();
		int lb1 = TEXT_CONTENT.indexOf('\n');
		// length of line 0 incl. line break
		int len0 = TEXT_CONTENT.substring(0, lb1).length() + 1;
		
		assertEquals("Line with negative number has wrong length incl. linebreak character \\n",
				len0, text.getLineLength(-1, true));
		assertEquals("Line with negative number has wrong length excl. linebreak character \\n",
				len0 - 1, text.getLineLength(-1, false));
		assertEquals("Line at getNrOfLines() has wrong length incl. linebreak character \\n",
				0, text.getLineLength(text.getNrOfLines(), true));
		assertEquals("Line at getNrOfLines() has wrong length excl. linebreak character \\n",
				0, text.getLineLength(text.getNrOfLines(), false));
		assertEquals("Line at getNrOfLines() has wrong length incl. linebreak character \\n",
				0, text.getLineLength(text.getNrOfLines()-1, true));
		assertEquals("Line at getNrOfLines() has wrong length excl. linebreak character \\n",
				0, text.getLineLength(text.getNrOfLines()-1, false));
		text.replace(text.length(), text.length(), "ABC");
		assertEquals("Line at getNrOfLines() has wrong length incl. linebreak character \\n",
				0, text.getLineLength(text.getNrOfLines(), true));
		assertEquals("Line at getNrOfLines() has wrong length excl. linebreak character \\n",
				0, text.getLineLength(text.getNrOfLines(), false));
		assertEquals("Line at getNrOfLines() has wrong length incl. linebreak character \\n",
				3, text.getLineLength(text.getNrOfLines()-1, true));
		assertEquals("Line at getNrOfLines() has wrong length excl. linebreak character \\n",
				3, text.getLineLength(text.getNrOfLines()-1, false));
	}

	/**
	 * Tests the method getLineStart() at the begin of the text.
	 */
	public void testGetLineStartBegin() {
		REDText text = getTestText();
		assertEquals("First line doesn't start at position 0.", 0,
				text.getLineStart(0));
	}

	/**
	 * Tests the method getLineStart() in the middle of the text.
	 */
	public void testGetLineStartMiddle() {
		REDText text = getTestText();
		int nrLb = getNrOfLineBreaks(TEXT_CONTENT) + 1;
		int lineNr = (nrLb) / 2;
		int startPos;
		int idx = 0;
		for (int i = 0; i < lineNr; i++) {
			idx = TEXT_CONTENT.indexOf('\n', idx + 1);
		}
		startPos = idx + 1;
		assertEquals("The number of lines is wrong.", nrLb,
				text.getNrOfLines());
		assertEquals("Middle line doesn't start at rigth position.",
				startPos, text.getLineStart(text.getNrOfLines() / 2));
	}
	
	/**
	 * Returns the supposed start index of the last line. This index is
	 * calculated from TEXT_CONTENT.
	 */
	private int getLastLineStart() {
		int nrLb = getNrOfLineBreaks(TEXT_CONTENT) + 1;
		int idx = 0;
		int oldIdx = 0;

		assertEquals("The number of lines is wrong.", nrLb,
				getTestText().getNrOfLines());
		// calculate index of last line start (startPos)
		for (int i = 0; i < nrLb; i++) {
			oldIdx = idx;
			idx = TEXT_CONTENT.indexOf('\n', idx + 1);
		}
		return oldIdx + 1;
	}

	/**
	 * Tests the method getLineStart() at the end of the text.
	 */
	public void testGetLineStartEnd() {
		REDText text = getTestText();
		// last line nr of lines - 1 
		assertEquals("End line doesn't start at right position", getLastLineStart(),
				text.getLineStart(text.getNrOfLines() - 1));
	}
	
	/**
	 * Tests the method getLineStart() after the end of the text.
	 */
	public void testGetLineStartAfterEnd() {
		REDText text = getTestText();
		// nr of lines is greater, than the index of the last line
		assertEquals("Line after end doesn't start at right position", getLastLineStart(),
				text.getLineStart(text.getNrOfLines()));
	}

	/**
	 * Tests the method getLineStart() before the begin of the text.
	 */
	public void testGetLineStartBeforeBegin() {
		REDText text = getTestText();
		assertEquals("Line before first line doesn't start at position 0.", 0,
				text.getLineStart(-1));
	}

	/**
	 * Tests the method getLineEnd() at the begin of the text.
	 */
	public void testGetLineEndBegin() {
		REDText text = getTestText();
		assertEquals("First line doesn't end at supposed position.",
				TEXT_CONTENT.indexOf('\n'), text.getLineEnd(0));
	}

	/**
	 * Tests the method getLineEnd() in the middle of the text.
	 */
	public void testGetLineEndMiddle() {
		REDText text = getTestText();
		int nrLb = getNrOfLineBreaks(TEXT_CONTENT) + 1;
		int lineNr = (nrLb) / 2;
		int endPos;
		int idx = 0;
		for (int i = 0; i <= lineNr; i++) {
			idx = TEXT_CONTENT.indexOf('\n', idx + 1);
		}
		endPos = idx;
		assertEquals("The number of lines is wrong.", nrLb,
				text.getNrOfLines());
		assertEquals("Middle line doesn't end at the supposed position.",
				endPos, text.getLineEnd(text.getNrOfLines() / 2));
	}

	/**
	 * Tests the method getLineEnd() at the end of the text.
	 */
	public void testGetLineEndEnd() {
		REDText text = getTestText();
		// last line nr of lines - 1 
		assertEquals("End line doesn't end at the supposed position",
				TEXT_CONTENT.length(),
				text.getLineEnd(text.getNrOfLines() - 1));
	}
	
	/**
	 * Tests the method getLineEnd() after the end of the text.
	 */
	public void testGetLineEndAfterEnd() {
		REDText text = getTestText();
		// nr of lines is greater, than the index of the last line
		assertEquals("Line after end doesn't end at the supposed position",
				TEXT_CONTENT.length(), text.getLineEnd(text.getNrOfLines()));
	}

	/**
	 * Tests the method getLineEnd() before the begin of the text.
	 */
	public void testGetLineEndBeforeBegin() {
		REDText text = getTestText();
		assertEquals("Line before first line doesn't end at supposed position.",
				0, text.getLineEnd(-1));
	}
}
