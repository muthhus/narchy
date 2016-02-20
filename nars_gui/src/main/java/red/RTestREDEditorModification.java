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
import java.awt.*;

/**
  * JUnit TestCase class for all kinds of modifications of red.REDEditor. This
  * class has it's own TestSuite. The methods
  * {@link REDEditor#replace(int from, int to, String s) replace()},
  * {@link REDEditor#isModified() isModified()},
  * {@link REDEditor#length() length()} and {@link REDEditor#asString() asString()}
  * are used for testing. Primarily the replace() method is tested, as it is the
  * only interface for inserting, deleting and replacing text in a REDEditor
  * object. The kind of the operation (which implies the kind of generated
  * event) depends on the range (i.e. the from and to parameters) of the
  * replacement. The test plan below is mainly a variation of these parameters
  * in conjunction with the String parameter.<P>
  * Each test method makes one or more replace operations and tests the String
  * representation of the text, the length and if it was modified..<P>
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
  * @author rli@chello.at taken from a unit test from Gerald Czech - gerald.czech@scch.at
  * @tier test
  */
public class RTestREDEditorModification extends RTestREDEditor {
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
	 * Static method to construct the TestSuite of RTestREDEditorFileIO.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDEditorModification.class);
	}
	
	/**
	 * Constructor.
	 * Constructs a new RTestREDEditorModification object.
	 */
	public RTestREDEditorModification(String name) {
		super(name);
	}
		
	/**
	 * Tests the insertion of a single character at the begin of a REDEditor
	 * object.
	 */
	public void testInsertFirstChar() {
		REDEditor editor = getTestEditor();
		String result = SHORT_TEXT + TEXT_CONTENT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(SHORT_TEXT, 0, 0, null);
		assertEquals("Insertion of a single character at the begin failed",
				result, editor.asString());
		assertEquals("Wrong length after insertion of a single character at the begin",
				TEXT_CONTENT.length() + 1, editor.length());
		assertTrue("Text was not modified after insertion of a single character at the begin",
				editor.isModified());
		checkEvents("beforeInsert(0, 1)\n" +
			"afterInsert(0, 1)");
	}

	/**
	 * Tests the insertion of text at the begin of a REDEditor object.
	 */
	public void testInsertBegin() {
		REDEditor editor = getTestEditor();
		String result = BEGIN_TEXT + TEXT_CONTENT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(BEGIN_TEXT, 0, 0, null);
		assertEquals("Insertion at the begin failed", result, editor.asString());
		assertEquals("Wrong length after insertion at the begin",
				TEXT_CONTENT.length() + BEGIN_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion at the begin",
				editor.isModified());
		checkEvents("beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text in the middle of a REDEditor object.
	 */
	public void testInsertMiddle() {
		REDEditor editor = getTestEditor();
		int midIdx = editor.length() / 2;
		String result = TEXT_CONTENT.substring(0, midIdx)  + MIDDLE_TEXT
						+ TEXT_CONTENT.substring(midIdx, TEXT_CONTENT.length());
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(MIDDLE_TEXT, midIdx, midIdx, null);
		assertEquals("Insertion in the middle failed", result,
				editor.asString());
		assertEquals("Wrong length after insertion in the middle",
				TEXT_CONTENT.length() + MIDDLE_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion in the middle",
				editor.isModified());
		checkEvents("beforeInsert(" + midIdx + ", " + (midIdx + MIDDLE_TEXT.length()) + ")\n" +
			"afterInsert(" + midIdx + ", " + (midIdx + MIDDLE_TEXT.length()) + ')');
	}
	
	/**
	 * Tests the insertion of text at the end of a REDEditor object.
	 */
	public void testInsertEnd() {
		REDEditor editor = getTestEditor();
		String result = TEXT_CONTENT + END_TEXT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(END_TEXT, editor.length(), editor.length(), null);
		assertEquals("Insertion at the end failed", result, editor.asString());
		assertEquals("Wrong length after insertion at the end",
				TEXT_CONTENT.length() + END_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion at the end",
				editor.isModified());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the insertion of a single character at the end of a REDEditor
	 * object.
	 */
	public void testInsertLastChar() {
		REDEditor editor = getTestEditor();
		String result = TEXT_CONTENT + SHORT_TEXT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(SHORT_TEXT, editor.length(), editor.length(), null);
		assertEquals("Insertion of a single character at the end failed", result,
				editor.asString());
		assertEquals("Wrong length after insertion of a single character at the end",
				TEXT_CONTENT.length() + 1, editor.length());
		assertTrue("Text was not modified after insertion of a single character at the end",
				editor.isModified());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() +", " + (TEXT_CONTENT.length() + SHORT_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() +", " + (TEXT_CONTENT.length() + SHORT_TEXT.length()) + ')');
	}

	/**
	 * Tests the insertion of text in an empty REDEditor object. Further the
	 * length of an empty REDEditor object is tested.
	 */
	public void testInsertInEmptyText() {
		REDEditor editor = getEmptyTestEditor();
		String result = MIDDLE_TEXT;
			
		assertEquals("Length of empty editor greater zero", 0, editor.length());
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(MIDDLE_TEXT, 0, editor.length(), null);
		assertEquals("Insertion in empty text failed", result,
				editor.asString());
		assertEquals("Wrong length after insertion in empty text",
				MIDDLE_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion in empty text",
				editor.isModified());
		checkEvents("beforeInsert(0, " + MIDDLE_TEXT.length() + ")\n" +
			"afterInsert(0, " + MIDDLE_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text at the begin of a REDEditor object, but with
	 * the first index out of range (i.e. from < 0).
	 */
	public void testInsertBeginNegIdx() {
		REDEditor editor = getTestEditor();
		String result = BEGIN_TEXT + TEXT_CONTENT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(BEGIN_TEXT, -1, 0, null);
		assertEquals("Insertion at the begin (one neg. index) failed", result,
				editor.asString());
		assertEquals("Wrong length after insertion at the begin  (one neg. index)",
				TEXT_CONTENT.length() + BEGIN_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion at the begin (one neg. index)",
				editor.isModified());
		checkEvents("beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text at the begin of a REDEditor object, but with
	 * both indexes out of range (i.e. from < 0 && to < 0 ).
	 */
	public void testInsertBeforeBegin() {
		REDEditor editor = getTestEditor();
		String result = BEGIN_TEXT + TEXT_CONTENT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(BEGIN_TEXT, -1, -1, null);
		assertEquals("Insertion before the begin failed", result,
				editor.asString());
		assertEquals("Wrong length after insertion before the begin",
				TEXT_CONTENT.length() + BEGIN_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion before the begin",
				editor.isModified());
		checkEvents("beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the insertion of text at the end of a REDEditor object, but with
	 * the second index out of range (i.e. to > length()).
	 */
	public void testInsertEndGreaterIdx() {
		REDEditor editor = getTestEditor();
		String result = TEXT_CONTENT + END_TEXT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(END_TEXT, editor.length(), editor.length() + 1, null);
		assertEquals("Insertion at the end failed", result, editor.asString());
		assertEquals("Wrong length after insertion at the end",
				TEXT_CONTENT.length() + END_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion at the end",
				editor.isModified());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the insertion of text at the end of a REDEditor object, but with
	 * both indexes out of range (i.e. from > length() && to > length()).
	 */
	public void testInsertAfterEnd() {
		REDEditor editor = getTestEditor();
		String result = TEXT_CONTENT + END_TEXT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(END_TEXT, editor.length() + 1, editor.length() + 1, null);
		assertEquals("Insertion at the end failed", result, editor.asString());
		assertEquals("Wrong length after insertion at the end",
				TEXT_CONTENT.length() + END_TEXT.length(), editor.length());
		assertTrue("Text was not modified after insertion at the end",
				editor.isModified());
		checkEvents("beforeInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ")\n" +
			"afterInsert(" + TEXT_CONTENT.length() + ", " + (TEXT_CONTENT.length() + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the deletion of the first character of a REDEditor object.
	 */
	public void testDeleteFirstChar() {
		REDEditor editor = getTestEditor();
		String result = TEXT_CONTENT.substring(1);		// cut first char
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, 0, 1, null);
		assertEquals("Deletion of first character failed", result,
				editor.asString());
		assertEquals("Wrong length after deletion of first character",
				TEXT_CONTENT.length() - 1, editor.length());
		assertTrue("Text was not modified after deletion of first character",
				editor.isModified());
		checkEvents("beforeDelete(0, 1)\n" +
			"afterDelete(0, 1)");
	}

	/**
	 * Tests the deletion of the begin of a REDEditor object.
	 */
	public void testDeleteBegin() {
		REDEditor editor = getTestEditor();
		int len = TEXT_CONTENT.length() / 2;
		String result = TEXT_CONTENT.substring(len);		// cut first half
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace("", 0, len, null);
		assertEquals("Deletion at begin failed", result, editor.asString());
		assertEquals("Wrong length after deletion at begin", len,
				editor.length());
		assertTrue("Text was not modified after deletion at begin",
				editor.isModified());
		checkEvents("beforeDelete(0, " + len + ")\n" +
			"afterDelete(0, " + len + ')');
	}

	/**
	 * Tests the deletion of the middle part of a REDEditor object.
	 */
	public void testDeleteMiddle() {
		REDEditor editor = getTestEditor();
		int from = TEXT_CONTENT.length() / 3;
		int to = TEXT_CONTENT.length() / 3 * 2;
		String result = TEXT_CONTENT.substring(0, from)
				+ TEXT_CONTENT.substring(to, TEXT_CONTENT.length());
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, from, to, null);
		assertEquals("Deletion in the middle failed", result, editor.asString());
		assertEquals("Wrong length after deletion in the middle",
				result.length(), editor.length());
		assertTrue("Text was not modified after deletion in the middle",
				editor.isModified());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" + 
			"afterDelete(" + from + ", " + to + ')');
	}

	/**
	 * Tests the deletion of the end of a REDEditor object.
	 */
	public void testDeleteEnd() {
		REDEditor editor = getTestEditor();
		int midIdx = TEXT_CONTENT.length() / 2;
		// cut second half		
		String result = TEXT_CONTENT.substring(0, midIdx);
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, midIdx, editor.length(), null);
		assertEquals("Deletion at the end failed", result, editor.asString());
		assertEquals("Wrong length after deletion at the end", result.length(),
				editor.length());
		assertTrue("Text was not modified after deletion at the end",
				editor.isModified());
		checkEvents("beforeDelete(" + midIdx + ", " + TEXT_CONTENT.length() + ")\n" +
			"afterDelete(" + midIdx + ", " + TEXT_CONTENT.length() + ')');
	}

	/**
	 * Tests the deletion of the last character of a REDEditor object.
	 */
	public void testDeleteLastChar() {
		REDEditor editor = getTestEditor();
		// cut last char
		String result = TEXT_CONTENT.substring(0, TEXT_CONTENT.length() - 1);
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, editor.length() - 1, editor.length(), null);
		assertEquals("Deletion of last character failed", result,
				editor.asString());
		assertEquals("Wrong length after deletion of last character",
				result.length(), editor.length());
		assertTrue("Text was not modified after deletion of last character",
				editor.isModified());
		int len = TEXT_CONTENT.length();
		checkEvents("beforeDelete(" + (len - 1) +", " + len + ")\n" +
			"afterDelete("  + (len - 1) + ", " + len + ')');
	}

	/**
	 * Tests the deletion of the whole text of a REDEditor object.
	 */
	public void testDeleteAll() {
		REDEditor editor = getTestEditor();
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, 0, editor.length(), null);
		assertEquals("Deletion of whole editor failed", "", editor.asString());
		assertEquals("Wrong length after deletion of whole editor", 0,
				editor.length());
		assertTrue("Text was not modified after deletion of whole editor",
				editor.isModified());
		checkEvents("beforeDelete(0, " + TEXT_CONTENT.length() + ")\n" +
			"afterDelete(0, " + TEXT_CONTENT.length() + ')');
	}

	/**
	 * Tests the deletion of the whole text of a REDEditor object in more replace
	 * operations. Further the length of the listener queues is tested, as it
	 * should grow after each operation.
	 */
	public void testDeleteAllStepwise() {
		REDEditor editor = getTestEditor();
		testDeleteBegin();		// first delete the begin (first half)
		// cut second half		
		editor.replace(null, 0, editor.length(), null);
		assertEquals("Deletion of second half failed", "", editor.asString());
		assertEquals("Wrong length after deletion of whole editor",0,
				editor.length());
		assertTrue("Text was not modified after second deletion",
				editor.isModified());
		int firstHalf = TEXT_CONTENT.length() / 2;
		int secondHalf = TEXT_CONTENT.length() - firstHalf;
		checkEvents("beforeDelete(0, " + firstHalf + ")\n" +
			"afterDelete(0, " + firstHalf + ")\n" +
			"beforeDelete(0, " + secondHalf + ")\n" +
			"afterDelete(0, " + secondHalf + ')');
	}

	/**
	 * Tests the deletion of text on an empty REDEditor object.
	 */
	public void testDeleteFromEmptyText() {
		REDEditor editor = getEmptyTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, 0, TEXT_CONTENT.length(), null);
		assertEquals("Deletion from empty editor changed editor", "",
				editor.asString());
		assertEquals("Deletion from empty editor changed length", 0,
				editor.length());
		assertTrue("Text was modified after deletion from empty editor",
				!editor.isModified());
		checkNoEvent();
	}

	/**
	 * Tests the deletion before the first character of a REDEditor object. So
	 * the object should not change.
	 */
	public void testDeleteZeroIdx() {
		REDEditor editor = getTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, 0, 0, null);
		assertEquals("Deletion before first character changed editor",
				TEXT_CONTENT,	editor.asString());
		assertEquals("Deletion before first character changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was modified after deletion before first character",
				!editor.isModified());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the begin of a REDEditor object, but with
	 * the first index out of range (i.e. from < 0).
	 */
	public void testDeleteBeginNegIdx() {
		REDEditor editor = getTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, -1, 0, null);
		assertEquals("Deletion before first character  (one neg. index) changed editor",
				TEXT_CONTENT,	editor.asString());
		assertEquals("Deletion before first character (one neg. index) changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was modified after deletion before first character (one neg. index)",
				!editor.isModified());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the begin of a REDEditor object, but with
	 * both indexes out of range (i.e. from < 0 && to < 0).
	 */
	public void testDeleteBeforeBegin() {
		REDEditor editor = getTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, -1, -1, null);
		assertEquals("Deletion before begin changed editor",
				TEXT_CONTENT, editor.asString());
		assertEquals("Deletion before begin changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was modified after deletion before begin",
				!editor.isModified());
		checkNoEvent();
	}
	
	/**
	 * Tests the deletion of text at the end of a REDEditor object, but with
	 * the both indexes equal length()).
	 */
	public void testDeleteEndLengthIdx() {
		REDEditor editor = getTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, editor.length(), editor.length(), null);
		assertEquals("Deletion at length() index changed editor", TEXT_CONTENT,
				editor.asString());
		assertEquals("Deletion at length() index changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was modified after deletion at length() index",
				!editor.isModified());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the end of a REDEditor object, but with
	 * the second index out of range (i.e. to > length()).
	 */
	public void testDeleteEndGreaterIdx() {
		REDEditor editor = getTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, editor.length(), editor.length() + 1, null);
		assertEquals("Deletion at length() + 1 index changed editor",
				TEXT_CONTENT, editor.asString());
		assertEquals("Deletion at length() + 1 index changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was modified after deletion at length() + 1 index",
				!editor.isModified());
		checkNoEvent();
	}

	/**
	 * Tests the deletion of text at the end of a REDEditor object, but with
	 * both indexes out of range (i.e. from > length() && to > length()).
	 */
	public void testDeleteAfterEnd() {
		REDEditor editor = getTestEditor();

		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(null, editor.length() + 1, editor.length() + 1, null);
		assertEquals("Deletion after end changed editor", TEXT_CONTENT,
				editor.asString());
		assertEquals("Deletion after end changed length", TEXT_CONTENT.length(),
				editor.length());
		assertTrue("Text was modified after deletion after end",
				!editor.isModified());
		checkNoEvent();
	}

	/**
	 * Tests the replacement of the first character in a REDEditor object.
	 */
	public void testReplaceFirstChar() {
		REDEditor editor = getTestEditor();
		
		String result = SHORT_TEXT + TEXT_CONTENT.substring(1);
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(SHORT_TEXT, 0, 1, null);
		assertEquals("Replacement of first character failed", result,
				editor.asString());
		assertEquals("Replacement of first character changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was not modified after replacement of first character",
				editor.isModified());
		checkEvents("beforeDelete(0, 1)\n" +
			"afterDelete(0, 1)\n" +
			"beforeInsert(0, " + SHORT_TEXT.length() + ")\n" +
			"afterInsert(0, " + SHORT_TEXT.length() + ')');
	}

	/**
	 * Tests the replacement of of the first character in a REDEditor object by a
	 * longer string.
	 */
	public void testReplaceBegin() {
		REDEditor editor = getTestEditor();
		String result = BEGIN_TEXT + TEXT_CONTENT.substring(1);
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(BEGIN_TEXT, 0, 1, null);
		assertEquals("Replacement of first character by longer string failed",
				result, editor.asString());
		assertEquals("Replacement of first character by longer string changed length",
				result.length(), editor.length());
		assertTrue("Text was not modified after replacement of first character by longer string",
				editor.isModified());
		checkEvents("beforeDelete(0, 1)\n" +
			"afterDelete(0, 1)\n" +
			"beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the replacement of the first character in a REDEditor object by a
	 * longer string.
	 */
	public void testReplaceBeginExactLength() {
		REDEditor editor = getTestEditor();
		String result = BEGIN_TEXT + TEXT_CONTENT.substring(BEGIN_TEXT.length());
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(BEGIN_TEXT, 0, BEGIN_TEXT.length(), null);
		assertEquals("Replacement of begin failed", result, editor.asString());
		assertEquals("Replacement of begin changed length", result.length(),
				editor.length());
		assertTrue("Text was not modified after replacement of begin",
				editor.isModified());
		checkEvents("beforeDelete(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterDelete(0, " + BEGIN_TEXT.length() + ")\n" +
			"beforeInsert(0, " + BEGIN_TEXT.length() + ")\n" +
			"afterInsert(0, " + BEGIN_TEXT.length() + ')');
	}

	/**
	 * Tests the replacement of a single character in the middle of a REDEditor
	 * object by a longer string.
	 */
	public void testReplaceMiddle() {
		REDEditor editor = getTestEditor();
		int from = TEXT_CONTENT.length() / 2;
		int to = TEXT_CONTENT.length() / 2 + 1;
		String result = TEXT_CONTENT.substring(0, from) + MIDDLE_TEXT
				+ TEXT_CONTENT.substring(to, TEXT_CONTENT.length());
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(MIDDLE_TEXT, from, to, null);
		assertEquals("Replacement of a single character by a longer string in the middle failed",
				result,	editor.asString());
		assertEquals("Replacement of a single character by a longer string  in the middle changed length",
				result.length(), editor.length());
		assertTrue("Text was not modified after replacement of a single character by a longer string  in the middle",
				editor.isModified());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ')');
	}

	/**
	 * Tests the replacement of a string in the middle a REDEditor object.
	 */
	public void testReplaceMiddleExactLength() {
		REDEditor editor = getTestEditor();
		int from = TEXT_CONTENT.length() / 2;
		int to = TEXT_CONTENT.length() / 2 + MIDDLE_TEXT.length();
		String result = TEXT_CONTENT.substring(0, from) + MIDDLE_TEXT
				+ TEXT_CONTENT.substring(to, TEXT_CONTENT.length());
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(MIDDLE_TEXT, from, to, null);
		assertEquals("Replacement of editor in the middle failed", result,
				editor.asString());
		assertEquals("Replacement of editor in the middle changed length",
				result.length(), editor.length());
		assertTrue("Text was not modified after replacement of editor in the middle",
				editor.isModified());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + MIDDLE_TEXT.length()) + ')');
	}

	/**
	 * Tests the replacement of a single character at the end of a REDEditor
	 * object by a longer string.
	 */
	public void testReplaceEnd() {
		REDEditor editor = getTestEditor();
		int from = TEXT_CONTENT.length() - 1;
		int to = TEXT_CONTENT.length();
		String result = TEXT_CONTENT.substring(0, from) + END_TEXT;
		
		assertTrue("Test was modified before any change.", !editor.isModified());
		editor.replace(END_TEXT, from, to, null);
		assertEquals("Replacement of the last character by a longer string in the middle failed",
				result,	editor.asString());
		assertEquals("Replacement of the last character by a longer string  in the middle changed length",
				result.length(), editor.length());
		assertTrue("Text was not modified after replacement of the last character by a longer string  in the middle",
				editor.isModified());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + END_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + END_TEXT.length()) + ')');
	}

	/**
	 * Tests the replacement of a string at the End a REDEditor object.
	 */
	public void testReplaceEndExactLength() {
		REDEditor editor = getTestEditor();
		int from = TEXT_CONTENT.length() - END_TEXT.length();
		int to = TEXT_CONTENT.length();
		String result = TEXT_CONTENT.substring(0, from) + END_TEXT;
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(END_TEXT, from, to, null);
		assertEquals("Replacement of editor at the end failed", result,
				editor.asString());
		assertEquals("Replacement of editor at the end changed length",
				result.length(), editor.length());
		assertTrue("Text was not modified after replacement of editor at the end",
				editor.isModified());
		checkEvents("beforeDelete(" + from + ", " + to + ")\n" +
			"afterDelete(" + from + ", " + to + ")\n" +
			"beforeInsert(" + from + ", " + (from + END_TEXT.length()) + ")\n" +
			"afterInsert(" + from + ", " + (from + END_TEXT.length()) + ')');
	}


	/**
	 * Tests the replacement of the last character in a REDEditor object.
	 */
	public void testReplaceLastChar() {
		REDEditor editor = getTestEditor();
		int len = TEXT_CONTENT.length();
		String result = TEXT_CONTENT.substring(0, len - 1) + SHORT_TEXT;
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(SHORT_TEXT, len - 1, len, null);
		assertEquals("Replacement of last character failed", result,
				editor.asString());
		assertEquals("Replacement of last character changed length",
				TEXT_CONTENT.length(), editor.length());
		assertTrue("Text was not modified after replacement of first character",
				editor.isModified());
		checkEvents("beforeDelete(" + (len-1) + ", " + len +")\n" +
			"afterDelete(" + (len-1) + ", " + len +")\n" +
			"beforeInsert(" + (len -1) + ", " + (len -1 + SHORT_TEXT.length()) + ")\n" +
			"afterInsert(" + (len -1) + ", " + (len -1 + SHORT_TEXT.length()) + ')');
	}
	
	/**
	 * Tests the replacement of the whole text in a REDEditor object by another
	 * string with the same length .
	 */
	public void testReplaceAll() {
		REDEditor editor = getTestEditor();
		
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.replace(LONG_TEXT, 0, editor.length(), null);
		assertEquals("Replacement of whole editor failed", LONG_TEXT,
				editor.asString());
		assertEquals("Replacement of the whole editor changed length",
				TEXT_CONTENT.length(), editor.asString().length());
		assertTrue("Text was not modified after replacement of the whole editor",
				editor.isModified());
		checkEvents("beforeDelete(0, " + TEXT_CONTENT.length() + ")\n" +
			"afterDelete(0, " + TEXT_CONTENT.length() + ")\n" +
			"beforeInsert(0, " + LONG_TEXT.length() + ")\n" +
			"afterInsert(0, " + LONG_TEXT.length() + ')');
	}
	
	/**
	 * Tests the replacement of the whole text of a REDEditor object in more
	 * replace operations. Further the length of the listener queues is tested,
	 * as it should grow after each operation.
	 */
	public void testReplaceAllStepwise() {
		REDEditor editor = getTestEditor();
		int halfLen = editor.length() / 2;
		int fullLen = editor.length();
		String firstHalf = LONG_TEXT.substring(0, halfLen);
		String secHalf = LONG_TEXT.substring(halfLen);
				
		assertTrue("Test was modified before any change", !editor.isModified());
		// replace first half
		editor.replace(firstHalf, 0, halfLen, null);
		assertEquals("Replacement of first half failed", firstHalf
				+ TEXT_CONTENT.substring(halfLen), editor.asString());
		assertTrue("Text was not modified after replacement of first half",
				editor.isModified());
		assertEquals("Wrong length after replacement of first half",
				LONG_TEXT.length(), editor.length());
		// cut second half		
		editor.replace(secHalf, halfLen, LONG_TEXT.length(), null);
		assertEquals("Replacement of whole editor stepwise failed",
				LONG_TEXT, editor.asString());
		assertTrue("Text was not modified after replacement of second half",
				editor.isModified());
		assertEquals("Wrong length after replacement of second half",
				LONG_TEXT.length(), editor.length());
		checkEvents("beforeDelete(0, " + halfLen + ")\n" +
			"afterDelete(0, " + halfLen + ")\n" +
			"beforeInsert(0, " + firstHalf.length() + ")\n" +
			"afterInsert(0, " + firstHalf.length() + ")\n" +
			"beforeDelete(" + halfLen + ", " + fullLen + ")\n" +
			"afterDelete(" + halfLen + ", " + fullLen + ")\n" +
			"beforeInsert(" + halfLen + ", " + editor.length() + ")\n" +
			"afterInsert(" + halfLen + ", " + editor.length() + ')');
	}
	
	/**
	 * Tests the replacement of the whole text of a REDEditor object character by
	 * character. Further the length of the listener queues is tested, as it
	 * should grow after each operation.
	 */
	public void testReplaceAllCharwise() {
		REDEditor editor = getTestEditor();
		String result;
		String expLog = "";
		
		assertTrue("Test was modified before any change", !editor.isModified());
		for (int i = 0; i < TEXT_CONTENT.length(); i++) {
			result = LONG_TEXT.substring(0, i + 1) + TEXT_CONTENT.substring(i + 1);
			editor.replace(LONG_TEXT.substring(i, i + 1), i, i + 1, null);
			assertEquals("Replacement of whole editor characterwise failed at char: " + i,
					result,	editor.asString());
			assertTrue("Text was not modified after replacement of second half",
					editor.isModified());
			assertEquals("Wrong length after replacement of second half",
					LONG_TEXT.length(), editor.length());
			expLog += "\nbeforeDelete(" + i + ", " + (i+1) + ")\n" +
				"afterDelete(" + i + ", " + (i+1) + ")\n" +
				"beforeInsert(" + i + ", " + (i+1) + ")\n" +
				"afterInsert(" + i + ", " + (i+1) + ')';
			checkEvents(expLog.substring(1));			
		}
	}
	
	/** Tests the setting of style. */
	public void testStyle() {
		REDEditor editor = getTestEditor();
		REDStyle style = new REDStyle(new Color(100, 100, 250), new Color(255, 255, 255), REDLining.DOUBLEUNDER, "Helvetica", "PLAIN", 18, null);
		String result = TEXT_CONTENT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		editor.setStyle(0, editor.length(), style);

		checkEvents("beforeStyleChange(0, " + editor.length() + ", " + style +")\n" +
			"afterStyleChange(0, " + editor.length() + ", " + style + ')');
	}
	
	/** Test idempotent style operations, which must not result in listener events. */
	public void testStyleIdempotent() {
		REDEditor editor = getTestEditor();
		REDStyle style = new REDStyle(new Color(100, 100, 250), new Color(255, 255, 255), REDLining.DOUBLEUNDER, "Helvetica", "PLAIN", 18, null);
		String result = TEXT_CONTENT;
		
		// set style on first few chars => events must happen
		editor.setStyle(0, 5, style);
		checkEvents("beforeStyleChange(0, 5, " + style + ")\n" + 
			"afterStyleChange(0, 5, " + style + ')');
		clearEvents(); checkNoEvent();	
		
		// set style again => events must not happen
		editor.setStyle(0, 5, style);
		checkNoEvent();
		
		// set inside => events must not happen
		editor.setStyle(1, 4, style);
		checkNoEvent();
		
		// enlarge style => events must happen
		editor.setStyle(1, 6, style);
		checkEvents("beforeStyleChange(1, 6, " + style + ")\n" + 
			"afterStyleChange(1, 6, " + style + ')');
		clearEvents(); checkNoEvent();
		
		editor.setStyle(6, 8, style);
		checkEvents("beforeStyleChange(6, 8, " + style + ")\n" + 
			"afterStyleChange(6, 8, " + style + ')');
		clearEvents(); checkNoEvent();
		
		// now set style for whole length to see if optimisation hasn't removed important stuff.
		editor.setStyle(0, editor.length(), style);
		checkEvents("beforeStyleChange(0, " + editor.length() + ", " + style +")\n" +
			"afterStyleChange(0, " + editor.length() + ", " + style + ')');
		clearEvents(); checkNoEvent();
		
		editor.setStyle(0, editor.length(), style);
		checkNoEvent();		
	}
	
	/** Tests the use of style batch mode. */
	public void testStyleBatch() {
		REDEditor editor = getTestEditor();
		REDStyle style = new REDStyle(new Color(100, 100, 250), new Color(255, 255, 255), REDLining.DOUBLEUNDER, "Helvetica", "PLAIN", 18, null);
		String result = TEXT_CONTENT;
			
		assertTrue("Test was modified before any change", !editor.isModified());
		assertTrue(!editor.hasStyleBatchNotification());
		editor.batchStyleNotificationStart();
		assertTrue(editor.hasStyleBatchNotification());
		editor.setStyle(0, editor.length(), style);
		editor.batchStyleNotificationEnd();
		assertTrue(!editor.hasStyleBatchNotification());

		checkEvents("beforeStyleBatchNotification()\n" +
			"afterStyleBatchNotification()");
	}
	
	public void setUp() throws Exception {
		super.setUp();
		logEventClass(REDTextEventListener.class);
		ignoreEventMethod("getListenerLevel");
	}	
}