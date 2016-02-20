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

/**
  * JUnit TestCase class for all kinds of modifications of red.REDText. This
  * class has it's own TestSuite. The methods
  * {@link REDText#replace(int from, int to, String s) replace()},
  * {@link REDText#isModified() isModified()},
  * {@link REDText#length() length()} and {@link REDText#asString() asString()}
  * are used for testing. Primarily the replace() method is tested, as it is the
  * only interface for inserting, deleting and replacing text in a REDText
  * object. The kind of the operation (which implies the kind of generated
  * event) depends on the range (i.e. the from and to parameters) of the
  * replacement. The test plan below is mainly a variation of these parameters
  * in conjunction with the String parameter.<P>
  * Each test method makes one or more replace operations and tests the String
  * representation of the text, the length, if it was modified and if the right
  * events occured in the supposed order.<P>
  * <H1>Testplan</H1><P>
  * <TABLE BORDER>
  * <TR><TH>Test Method</TH><TH>from</TH><TH>to</TH><TH>String</TH><TH>Kind of Operation</TH></TR>
  * <TR><TD>{@link #testInsertFirstChar() testInsertFirstChar()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>0</TD><TD>"1"</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertBegin() testInsertBegin()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>0</TD><TD>"Text at the Begin: "</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertMiddle() testInsertMiddle()}</TD><TD ALIGN=RIGHT>length() / 2</TD><TD ALIGN=RIGHT>length() / 2</TD><TD>" Text in the middle "</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertEnd() testInsertEnd()}</TD><TD ALIGN=RIGHT>length()</TD><TD ALIGN=RIGHT>length()</TD><TD>" Text at the end."</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertLastChar() testInsertLastChar()}</TD><TD ALIGN=RIGHT>length()</TD><TD ALIGN=RIGHT>length()</TD><TD>"1"</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertInEmptyText() testInsertInEmptyText()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length()</TD><TD>" Text in the middle "</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertBeginNegIdx() testInsertBeginNegIdx()}</TD><TD ALIGN=RIGHT>-1</TD><TD ALIGN=RIGHT>0</TD><TD>"Text at the Begin: "</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertBeforeBegin() testInsertBeforeBegin()}</TD><TD ALIGN=RIGHT>-1</TD><TD ALIGN=RIGHT>-1</TD><TD>"Text at the Begin: "</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertEndGreaterIdx() testInsertEndGreaterIdx()}</TD><TD ALIGN=RIGHT>length()</TD><TD ALIGN=RIGHT>length() + 1</TD><TD>" Text at the end."</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testInsertAfterEnd() testInsertAfterEnd()}</TD><TD ALIGN=RIGHT>length() + 1</TD><TD ALIGN=RIGHT>length() + 1</TD><TD>" Text at the end."</TD><TD>Insert</TD></TR>
  * <TR><TD>{@link #testDeleteFirstChar() testDeleteFirstChar()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>1</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteBegin() testDeleteBegin()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length() / 2</TD><TD>""</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteMiddle() testDeleteMiddle()}</TD><TD ALIGN=RIGHT>length() / 3 </TD><TD ALIGN=RIGHT>length() / 3 * 2</TD><TD>""</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteEnd() testDeleteEnd()}</TD><TD ALIGN=RIGHT>length() / 2</TD><TD ALIGN=RIGHT>length()</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteLastChar() testDeleteLastChar()}</TD><TD ALIGN=RIGHT>length() - 1</TD><TD ALIGN=RIGHT>length()</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteAll() testDeleteAll()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length()</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD 	ROWSPAN=2>{@link #testDeleteAllStepwise() testDeleteAllStepwise()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length() / 2</TD><TD>""</TD><TD>Delete</TD></TR>
  * <TR><TD ALIGN=RIGHT>length / 2</TD><TD ALIGN=RIGHT>length()</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteFromEmptyText() testDeleteFromEmptyText()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length()</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteZeroIdx() testDeleteZeroIdx()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>0</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteBeginNegIdx() testDeleteBeginNegIdx()}</TD><TD ALIGN=RIGHT>-1</TD><TD ALIGN=RIGHT>0</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteBeforeBegin() testDeleteBeforeBegin()}</TD><TD ALIGN=RIGHT>-1</TD><TD ALIGN=RIGHT>-1</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteEndLengthIdx() testDeleteEndLengthIdx()}</TD><TD ALIGN=RIGHT>length()</TD><TD ALIGN=RIGHT>length()</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteEndGreaterIdx() testDeleteEndGreaterIdx()}</TD><TD ALIGN=RIGHT>length()</TD><TD ALIGN=RIGHT>length() + 1</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testDeleteAfterEnd() testDeleteAfterEnd()}</TD><TD ALIGN=RIGHT>length() + 1</TD><TD ALIGN=RIGHT>length() + 1</TD><TD>null</TD><TD>Delete</TD></TR>
  * <TR><TD>{@link #testReplaceFirstChar() testReplaceFirstChar()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>1</TD><TD>"1"</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceBegin() testReplaceBegin()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>1</TD><TD>"Text at the Begin: "</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceBeginExactLength() testReplaceBeginExactLength()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>19</TD><TD>"Text at the Begin: "</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceMiddle() testReplaceMiddle()}</TD><TD ALIGN=RIGHT>length() / 2</TD><TD ALIGN=RIGHT>length() / 2 + 1</TD><TD>" Text in the middle "</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceMiddleExactLength() testReplaceMiddleExactLength()}</TD><TD ALIGN=RIGHT>length() / 2</TD><TD ALIGN=RIGHT>length() / 2 + 20</TD><TD>" Text in the middle "</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceEnd() testReplaceEnd()}</TD><TD ALIGN=RIGHT>length() - 1</TD><TD ALIGN=RIGHT>length()</TD><TD>" Text at the end."</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceEndExactLength() testReplaceEndExactLength()}</TD><TD ALIGN=RIGHT>length() - 17</TD><TD ALIGN=RIGHT>length()</TD><TD>" Text at the end."</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceLastChar() testReplaceLastChar()}</TD><TD ALIGN=RIGHT>length() - 1</TD><TD ALIGN=RIGHT>length()</TD><TD>"1"</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceAll() testReplaceAll()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length()</TD><TD>LongText</TD><TD>Replace</TD></TR>
  * <TR><TD 	ROWSPAN=2>{@link #testReplaceAllStepwise() testReplaceAllStepwise()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length() / 2</TD><TD>LongText.FirstHalf</TD><TD>Replace</TD></TR>
  * <TR><TD ALIGN=RIGHT>length / 2</TD><TD ALIGN=RIGHT>length()</TD><TD>LongText.SecondHalf</TD><TD>Replace</TD></TR>
  * <TR><TD>{@link #testReplaceAllCharwise() testReplaceAllCharwise()}</TD><TD ALIGN=RIGHT>0</TD><TD ALIGN=RIGHT>length()</TD><TD>LongText</TD><TD>Replace</TD></TR>
  * </TABLE>
  *
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDTextModification extends RTestREDText {
	/** Constant for a short (length() == 1) text for replace operations. */
	private static final String SHORT_TEXT = "1";

	/** Constant for a begin text for replace operations. */
	private static final String BEGIN_TEXT = "Text at the Begin: ";

	/** Constant for a middel text for replace operations. */
	private static final String MIDDLE_TEXT = " Text in the middle ";

	/** Constant for an end text for replace operations. */
	private static final String END_TEXT = " Text at the end.";	
	
	/** Constant for a long text for replace operations. */
	private static final String LONG_TEXT = "The Balrog reached the bridge."
			+ "\n\n" + " Gandalf stood in the middle of the span, leaning on the staff in hand."
			+ "\n";
			
	/**
	 * Static method to construct the TestSuite of RTestREDTextFileIO.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDTextModification.class);
	}
	
	/**
	 * Constructor.
	 * Constructs a new RTestREDTextModification object.
	 */
	public RTestREDTextModification(String name) {
		super(name);
	}
	
	/**
	 * Initializes test data. Here a REDTextEventListener for each level is
	 * attached to the test text.
	 */
	public void setUp() throws Exception {
		super.setUp();
		logEventClass(REDTextEventListener.class);
		ignoreEventMethod("getListenerLevel");
	}
	
	/**
	 * Do some clean up. This is removing the REDTextEventListeners.
	 */
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Tests the insertion of a single character at the begin of a REDText
	 * object.
	 */
	public void testInsertFirstChar() {
		REDText text = getTestText();
		String result = SHORT_TEXT + TEXT_CONTENT;
		text.replace(0, 0, SHORT_TEXT);
		assertEquals("Insertion of a single character at the begin failed.",
						result, text.asString());
		assertEquals("Wrong length after insertion of a single character at the begin.",
						TEXT_CONTENT.length() + 1, text.length());
		checkEvents("beforeInsert(0, 1)\n" +
			"afterInsert(0, 1)");
	}

	/**
	 * Tests the insertion of text at the begin of a REDText object.
	 */
	public void testInsertBegin() {
		REDText text = getTestText();
		String result = BEGIN_TEXT + TEXT_CONTENT;
		text.replace(0, 0, BEGIN_TEXT);
		assertEquals("Insertion at the begin failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion at the begin.",
						TEXT_CONTENT.length() + BEGIN_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text in the middle of a REDText object.
	 */
	public void testInsertMiddle() {
		REDText text = getTestText();
		int midIdx = text.length() / 2;
		String result = TEXT_CONTENT.substring(0, midIdx)  + MIDDLE_TEXT
						+ TEXT_CONTENT.substring(midIdx, TEXT_CONTENT.length());
		text.replace(midIdx, midIdx, MIDDLE_TEXT);
		assertEquals("Insertion in the middle failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion in the middle.",
						TEXT_CONTENT.length() + MIDDLE_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(" + midIdx + ", " + (midIdx + MIDDLE_TEXT.length()) + ")\n" +
			"afterInsert(" + midIdx + ", " + (midIdx + MIDDLE_TEXT.length()) + ')');
	}
	
	/**
	 * Tests the insertion of text at the end of a REDText object.
	 */
	public void testInsertEnd() {
		REDText text = getTestText();
		String result = TEXT_CONTENT + END_TEXT;
		text.replace(text.length(), text.length(), END_TEXT);
		assertEquals("Insertion at the end failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion at the end.",
						TEXT_CONTENT.length() + END_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the insertion of a single character at the end of a REDText
	 * object.
	 */
	public void testInsertLastChar() {
		REDText text = getTestText();
		String result = TEXT_CONTENT + SHORT_TEXT;
		text.replace(text.length(), text.length(), SHORT_TEXT);
		assertEquals("Insertion of a single character at the end failed.",
						result, text.asString());
		assertEquals("Wrong length after insertion of a single character at the end.",
						TEXT_CONTENT.length() + 1, text.length());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() +", " + (TEXT_CONTENT.length() + SHORT_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() +", " + (TEXT_CONTENT.length() + SHORT_TEXT.length()) + ')');
	}

	/**
	 * Tests the insertion of text in an empty REDText object. Further the
	 * length of an empty REDText object is tested.
	 */
	public void testInsertInEmptyText() {
		REDText text = getEmptyTestText();
		String result = MIDDLE_TEXT;
		assertEquals("Length of empty text greater zero.", 0, text.length());
		text.replace(0, text.length(), MIDDLE_TEXT);
		assertEquals("Insertion in empty text failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion in empty text.",
						MIDDLE_TEXT.length(), text.length());
		checkEvents("beforeInsert(0, " + MIDDLE_TEXT.length() + ")\n" +
			"afterInsert(0, " + MIDDLE_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text at the begin of a REDText object, but with
	 * the first index out of range (i.e. from < 0).
	 */
	public void testInsertBeginNegIdx() {
		REDText text = getTestText();
		String result = BEGIN_TEXT + TEXT_CONTENT;
		text.replace(-1, 0, BEGIN_TEXT);
		assertEquals("Insertion at the begin (one neg. index) failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion at the begin  (one neg. index).",
						TEXT_CONTENT.length() + BEGIN_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text at the begin of a REDText object, but with
	 * both indexes out of range (i.e. from < 0 && to < 0 ).
	 */
	public void testInsertBeforeBegin() {
		REDText text = getTestText();
		String result = BEGIN_TEXT + TEXT_CONTENT;
		text.replace(-1, -1, BEGIN_TEXT);
		assertEquals("Insertion before the begin failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion before the begin.",
						TEXT_CONTENT.length() + BEGIN_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text at the end of a REDText object, but with
	 * the second index out of range (i.e. to > length()).
	 */
	public void testInsertEndGreaterIdx() {
		REDText text = getTestText();
		String result = TEXT_CONTENT + END_TEXT;
		text.replace(text.length(), text.length() + 1, END_TEXT);
		assertEquals("Insertion at the end failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion at the end.",
						TEXT_CONTENT.length() + END_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the insertion of text at the end of a REDText object, but with
	 * both indexes out of range (i.e. from > length() && to > length()).
	 */
	public void testInsertAfterEnd() {
		REDText text = getTestText();
		String result = TEXT_CONTENT + END_TEXT;
		text.replace(text.length() + 1, text.length() + 1, END_TEXT);
		assertEquals("Insertion at the end failed.", result,
						text.asString());
		assertEquals("Wrong length after insertion at the end.",
						TEXT_CONTENT.length() + END_TEXT.length(),
						text.length());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the deletion of the first character of a REDText object.
	 */
	public void testDeleteFirstChar() {
		REDText text = getTestText();
		String result = TEXT_CONTENT.substring(1);		// cut first char
		text.replace(0, 1, null);
		assertEquals("Deletion of first character failed.", result,
				text.asString());
		assertEquals("Wrong length after deletion of first character.",
				TEXT_CONTENT.length() - 1, text.length());
		checkEvents("beforeDelete(0, 1)\n" +
			"afterDelete(0, 1)");
	}

	/**
	 * Tests the deletion of the begin of a REDText object.
	 */
	public void testDeleteBegin() {
		REDText text = getTestText();
		int len = TEXT_CONTENT.length() / 2;
		String result = TEXT_CONTENT.substring(len);		// cut first half
		text.replace(0, len, "");
		assertEquals("Deletion at begin failed.", result, text.asString());
		assertEquals("Wrong length after deletion at begin.", len,
				text.length());
		checkEvents("beforeDelete(0, " + len + ")\n" +
			"afterDelete(0, " + len + ')');
	}

	/**
	 * Tests the deletion of the middle part of a REDText object.
	 */
	public void testDeleteMiddle() {
		REDText text = getTestText();
		int from = TEXT_CONTENT.length() / 3;
		int to = TEXT_CONTENT.length() / 3 * 2;
		String result = TEXT_CONTENT.substring(0, from)
				+ TEXT_CONTENT.substring(to, TEXT_CONTENT.length());
		text.replace(from, to, null);
		assertEquals("Deletion in the middle failed.", result, text.asString());
		assertEquals("Wrong length after deletion in the middle.",
				result.length(), text.length());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" + 
			"afterDelete(" + from + ", " + to + ')');
	}

	/**
	 * Tests the deletion of the end of a REDText object.
	 */
	public void testDeleteEnd() {
		REDText text = getTestText();
		int midIdx = TEXT_CONTENT.length() / 2;
		// cut second half		
		String result = TEXT_CONTENT.substring(0, midIdx);
		text.replace(midIdx, text.length(), null);
		assertEquals("Deletion at the end failed.", result, text.asString());
		assertEquals("Wrong length after deletion at the end.",
				result.length(), text.length());
		checkEvents("beforeDelete(" + midIdx + ", " + TEXT_CONTENT.length() + ")\n" +
			"afterDelete(" + midIdx + ", " + TEXT_CONTENT.length() + ')');
	}

	/**
	 * Tests the deletion of the last character of a REDText object.
	 */
	public void testDeleteLastChar() {
		REDText text = getTestText();
		// cut last char
		String result = TEXT_CONTENT.substring(0, TEXT_CONTENT.length() - 1);
		text.replace(text.length() - 1, text.length(), null);
		assertEquals("Deletion of last character failed.", result,
				text.asString());
		assertEquals("Wrong length after deletion of last character.",
				result.length(), text.length());
		int len = TEXT_CONTENT.length();
		checkEvents("beforeDelete(" + (len - 1) +", " + len + ")\n" +
			"afterDelete("  + (len - 1) + ", " + len + ')');
	}

	/**
	 * Tests the deletion of the whole text of a REDText object.
	 */
	public void testDeleteAll() {
		REDText text = getTestText();
		text.replace(0, text.length(), null);		// delete whole text
		assertEquals("Deletion of whole text failed.", "", text.asString());
		assertEquals("Wrong length after deletion of whole text.", 0,
				text.length());
		checkEvents("beforeDelete(0, " + TEXT_CONTENT.length() + ")\n" +
			"afterDelete(0, " + TEXT_CONTENT.length() + ')');
	}

	/**
	 * Tests the deletion of the whole text of a REDText object in more replace
	 * operations. Further the length of the listener queues is tested, as it
	 * should grow after each operation.
	 */
	public void testDeleteAllStepwise() {
		REDText text;
		testDeleteBegin();		// first delete the begin (first half)
		text = getTestText();

		// cut second half		
		text.replace(0, text.length(), null);
		assertEquals("Deletion of second half failed.", "", text.asString());
		assertEquals("Wrong length after deletion of whole text.",0,
				text.length());
		int firstHalf = TEXT_CONTENT.length() / 2;
		int secondHalf = TEXT_CONTENT.length() - firstHalf;
		checkEvents("beforeDelete(0, " + firstHalf + ")\n" +
			"afterDelete(0, " + firstHalf + ")\n" +
			"beforeDelete(0, " + secondHalf + ")\n" +
			"afterDelete(0, " + secondHalf + ')');
	}

	/**
	 * Tests the deletion of text on an empty REDText object.
	 */
	public void testDeleteFromEmptyText() {
		REDText text = getEmptyTestText();
		text.replace(0, TEXT_CONTENT.length(), null);
		assertEquals("Deletion from empty text changed text.", "",
				text.asString());
		assertEquals("Deletion from empty text changed length.", 0,
				text.length());
		checkNoEvent();
	}

	/**
	 * Tests the deletion before the first character of a REDText object. So
	 * the object should not change.
	 */
	public void testDeleteZeroIdx() {
		REDText text = getTestText();
		text.replace(0, 0, null);
		assertEquals("Deletion before first character changed text.",
				TEXT_CONTENT,	text.asString());
		assertEquals("Deletion before first character changed length.",
				TEXT_CONTENT.length(), text.length());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the begin of a REDText object, but with
	 * the first index out of range (i.e. from < 0).
	 */
	public void testDeleteBeginNegIdx() {
		REDText text = getTestText();
		text.replace(-1, 0, null);
		assertEquals("Deletion before first character  (one neg. index) changed text.",
				TEXT_CONTENT,	text.asString());
		assertEquals("Deletion before first character (one neg. index) changed length.",
				TEXT_CONTENT.length(), text.length());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the begin of a REDText object, but with
	 * both indexes out of range (i.e. from < 0 && to < 0).
	 */
	public void testDeleteBeforeBegin() {
		REDText text = getTestText();
		text.replace(-1, -1, null);
		assertEquals("Deletion before begin changed text.",	TEXT_CONTENT,
				text.asString());
		assertEquals("Deletion before begin changed length.",
				TEXT_CONTENT.length(), text.length());
		checkNoEvent();
	}
	
	/**
	 * Tests the deletion of text at the end of a REDText object, but with
	 * the both indexes equal length()).
	 */
	public void testDeleteEndLengthIdx() {
		REDText text = getTestText();
		text.replace(text.length(), text.length(), null);
		assertEquals("Deletion at length() index changed text.", TEXT_CONTENT,
				text.asString());
		assertEquals("Deletion at length() index changed length.",
				TEXT_CONTENT.length(), text.length());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the end of a REDText object, but with
	 * the second index out of range (i.e. to > length()).
	 */
	public void testDeleteEndGreaterIdx() {
		REDText text = getTestText();
		text.replace(text.length(), text.length() + 1, null);
		assertEquals("Deletion at length() + 1 index changed text.", TEXT_CONTENT,
				text.asString());
		assertEquals("Deletion at length() + 1 index changed length.",
				TEXT_CONTENT.length(), text.length());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the end of a REDText object, but with
	 * both indexes out of range (i.e. from > length() && to > length()).
	 */
	public void testDeleteAfterEnd() {
		REDText text = getTestText();
		text.replace(text.length() + 1, text.length() + 1, null);
		assertEquals("Deletion after end changed text.", TEXT_CONTENT,
				text.asString());
		assertEquals("Deletion after end changed length.", TEXT_CONTENT.length(),
				text.length());
		checkNoEvent();
	}

	/**
	 * Tests the replacement of the first character in a REDText object.
	 */
	public void testReplaceFirstChar() {
		REDText text = getTestText();
		String result = SHORT_TEXT + TEXT_CONTENT.substring(1);
		text.replace(0, 1, SHORT_TEXT);
		assertEquals("Replacement of first character failed.", result,
				text.asString());
		assertEquals("Replacement of first character changed length.",
				TEXT_CONTENT.length(), text.length());
		checkEvents("beforeDelete(0, 1)\n" +
			"afterDelete(0, 1)\n" +
			"beforeInsert(0, " + SHORT_TEXT.length() + ")\n" +
			"afterInsert(0, " + SHORT_TEXT.length() + ')');
	}

	/**
	 * Tests the replacement of of the first character in a REDText object by a
	 * longer string.
	 */
	public void testReplaceBegin() {
		REDText text = getTestText();
		String result = BEGIN_TEXT + TEXT_CONTENT.substring(1);
		text.replace(0, 1, BEGIN_TEXT);
		assertEquals("Replacement of first character by longer string failed.", result,
				text.asString());
		assertEquals("Replacement of first character by longer string changed length.",
				result.length(), text.length());
		checkEvents("beforeDelete(0, 1)\n" +
			"afterDelete(0, 1)\n" +
			"beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the replacement of the first character in a REDText object by a
	 * longer string.
	 */
	public void testReplaceBeginExactLength() {
		REDText text = getTestText();
		String result = BEGIN_TEXT + TEXT_CONTENT.substring(BEGIN_TEXT.length());
		text.replace(0, BEGIN_TEXT.length(), BEGIN_TEXT);
		assertEquals("Replacement of begin failed.", result, text.asString());
		assertEquals("Replacement of begin changed length.", result.length(),
				text.length());
		checkEvents("beforeDelete(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterDelete(0, " + BEGIN_TEXT.length() + ")\n" +
			"beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the replacement of a single character in the middle of a REDText
	 * object by a longer string.
	 */
	public void testReplaceMiddle() {
		REDText text = getTestText();
		int from = TEXT_CONTENT.length() / 2;
		int to = TEXT_CONTENT.length() / 2 + 1;
		String result = TEXT_CONTENT.substring(0, from) + MIDDLE_TEXT
				+ TEXT_CONTENT.substring(to, TEXT_CONTENT.length());
		text.replace(from, to, MIDDLE_TEXT);
		assertEquals("Replacement of a single character by a longer string in the middle failed.",
				result,	text.asString());
		assertEquals("Replacement of a single character by a longer string  in the middle changed length.",
				result.length(), text.length());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ')');
	}

	/**
	 * Tests the replacement of a string in the middle a REDText object.
	 */
	public void testReplaceMiddleExactLength() {
		REDText text = getTestText();
		int from = TEXT_CONTENT.length() / 2;
		int to = TEXT_CONTENT.length() / 2 + MIDDLE_TEXT.length();
		String result = TEXT_CONTENT.substring(0, from) + MIDDLE_TEXT
				+ TEXT_CONTENT.substring(to, TEXT_CONTENT.length());
		text.replace(from, to, MIDDLE_TEXT);
		assertEquals("Replacement of text in the middle failed.", result,
				text.asString());
		assertEquals("Replacement of text in the middle changed length.",
				result.length(), text.length());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ')');
	}

	/**
	 * Tests the replacement of a single character at the end of a REDText
	 * object by a longer string.
	 */
	public void testReplaceEnd() {
		REDText text = getTestText();
		int from = TEXT_CONTENT.length() - 1;
		int to = TEXT_CONTENT.length();
		String result = TEXT_CONTENT.substring(0, from) + END_TEXT;
		text.replace(from, to, END_TEXT);
		assertEquals("Replacement of the last character by a longer string in the middle failed.",
				result,	text.asString());
		assertEquals("Replacement of the last character by a longer string  in the middle changed length.",
				result.length(), text.length());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + END_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the replacement of a string at the End a REDText object.
	 */
	public void testReplaceEndExactLength() {
		REDText text = getTestText();
		int from = TEXT_CONTENT.length() - END_TEXT.length();
		int to = TEXT_CONTENT.length();
		String result = TEXT_CONTENT.substring(0, from) + END_TEXT;
		text.replace(from, to, END_TEXT);
		assertEquals("Replacement of text at the end failed.", result,
				text.asString());
		assertEquals("Replacement of text at the end changed length.",
				result.length(), text.length());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + END_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + END_TEXT.length()) + ')');
	}


	/**
	 * Tests the replacement of the last character in a REDText object.
	 */
	public void testReplaceLastChar() {
		REDText text = getTestText();
		int len = TEXT_CONTENT.length();
		String result = TEXT_CONTENT.substring(0, len - 1) + SHORT_TEXT;
		text.replace(len - 1, len, SHORT_TEXT);
		assertEquals("Replacement of last character failed.", result,
				text.asString());
		assertEquals("Replacement of last character changed length.",
				TEXT_CONTENT.length(), text.length());
		checkEvents("beforeDelete(" + (len-1) + ", " + len +")\n" +
			"afterDelete(" + (len-1) + ", " + len +")\n" +
			"beforeInsert(" + (len -1) + ", " + (len -1 + SHORT_TEXT.length()) + ")\n" +
			"afterInsert(" + (len -1) + ", " + (len -1 + SHORT_TEXT.length()) + ')');
	}
	
	/**
	 * Tests the replacement of the whole text in a REDText object by another
	 * string with the same length .
	 */
	public void testReplaceAll() {
		REDText text = getTestText();
		text.replace(0, text.length(), LONG_TEXT);
		assertEquals("Replacement of whole text failed.", LONG_TEXT,
				text.asString());
		assertEquals("Replacement of the whole text changed length.",
				TEXT_CONTENT.length(), text.asString().length());
		checkEvents("beforeDelete(0, " + TEXT_CONTENT.length() + ")\n" +
			"afterDelete(0, " + TEXT_CONTENT.length() + ")\n" +
			"beforeInsert(0, " + LONG_TEXT.length() + ")\n" +
			"afterInsert(0, " + LONG_TEXT.length() + ')');
	}
	
	/**
	 * Tests the replacement of the whole text of a REDText object in more
	 * replace operations. Further the length of the listener queues is tested,
	 * as it should grow after each operation.
	 */
	public void testReplaceAllStepwise() {
		REDText text = getTestText();
		int halfLen = text.length() / 2;
		int fullLen = text.length();
		String firstHalf = LONG_TEXT.substring(0, halfLen);
		String secHalf = LONG_TEXT.substring(halfLen);
		
		// replace first half
		text.replace(0, halfLen, firstHalf);
		// check events
		assertEquals("Replacement of first half failed.", firstHalf
				+ TEXT_CONTENT.substring(halfLen), text.asString());
		assertEquals("Wrong length after replacement of first half.",
				LONG_TEXT.length(), text.length());
		// cut second half		
		text.replace(halfLen, LONG_TEXT.length(), secHalf);
		assertEquals("Replacement of whole text stepwise failed.", LONG_TEXT,
				text.asString());
		assertEquals("Wrong length after replacement of second half.",
				LONG_TEXT.length(), text.length());
		// check events
		checkEvents("beforeDelete(0, " + halfLen + ")\n" +
			"afterDelete(0, " + halfLen + ")\n" +
			"beforeInsert(0, " + firstHalf.length() + ")\n" +
			"afterInsert(0, " + firstHalf.length() + ")\n" +
			"beforeDelete(" + halfLen + ", " + fullLen + ")\n" +
			"afterDelete(" + halfLen + ", " + fullLen + ")\n" +
			"beforeInsert(" + halfLen + ", " + text.length() + ")\n" +
			"afterInsert(" + halfLen + ", " + text.length() + ')');
	}
	
	/**
	 * Tests the replacement of the whole text of a REDText object character by
	 * character. Further the length of the listener queues is tested, as it
	 * should grow after each operation.
	 */
	public void testReplaceAllCharwise() {
		REDText text = getTestText();
		String result;		// supposed result
		String expLog = "";
		
		for (int i = 0; i < TEXT_CONTENT.length(); i++) {
			result = LONG_TEXT.substring(0, i + 1) + TEXT_CONTENT.substring(i + 1);
			text.replace(i, i + 1, LONG_TEXT.substring(i, i + 1));
			assertEquals("Replacement of whole text characterwise failed at char: "+ i,
					 result,	text.asString());
			assertEquals("Wrong length after replacement of second half.",
					LONG_TEXT.length(), text.length());
			expLog += "\nbeforeDelete(" + i + ", " + (i+1) + ")\n" +
				"afterDelete(" + i + ", " + (i+1) + ")\n" +
				"beforeInsert(" + i + ", " + (i+1) + ")\n" +
				"afterInsert(" + i + ", " + (i+1) + ')';
			checkEvents(expLog.substring(1));			
		}
	}
}