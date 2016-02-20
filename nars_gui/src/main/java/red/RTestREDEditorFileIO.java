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

import java.io.*;
import junit.framework.*;
import red.file.*;

/** JUnit TestCase class for the file IO handling of red.REDEditor.
  *
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDEditorFileIO extends RTestREDEditor {
	/** Constant holding the name for a second temporary file. */
	private static final String TMP_FILE2 = "RTestREDEditor.2.tmp";

	/** Constant holding the name for a third temporary file. */
	private static final String TMP_FILE3 = "RTestREDEditor.3.tmp";
	
	/** Some text for insertion at the begin. */
	private static final String INS_TEXT = "But it is said: ";

	/** Constant for a long text for replace operations. */
	private static final String LONG_TEXT = "The Balrog reached the bridge."
			+ "\n\n" + " Gandalf stood in the middle of the span, leaning on the staff in hand."
			+ "\n";

	/**
	 * Static method to construct the TestSuite of RTestREDEditorFileIO.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDEditorFileIO.class);
	}
	
	/**
	 * Constructor.
	 * Constructs a new RTestREDEditorFileIO object.
	 */
	public RTestREDEditorFileIO(String name) {
		super(name);
	}
	
	/**
	 * Initializes test data. Here a REDEditorEventListener is attached to the
	 * test text and a second temporary file is created.
	 */
	public void setUp() throws Exception {
		REDFile tmpFile;
		REDFileRider rider;
		REDEditor editor;

		super.setUp();
		editor = getSharedTestEditor();
		// create a second tmp file
		tmpFile = new REDFile(TMP_FILE2);
		rider = new REDFileRider(tmpFile);
		tmpFile.purge();
		rider.writeBytes(LONG_TEXT.getBytes(), LONG_TEXT.length());
		assertTrue("Cannot write to current directory. Needed for testing.", rider.getRes() == 0);
		tmpFile.close();
	}
	
	/**
	 * Do some clean up. This is removing the REDEditorEventListener and
	 * deleting the second temporary file.
	 */
	public void tearDown() throws Exception {
		super.tearDown();
		File file = new File(TMP_FILE2);
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
	}

	/**
	 * Tests the automatic load, when an <CODE>REDEditor</CODE object is
	 * constructed and the <CODE>isModified</CODE> method of REDEditor. The
	 * object is constructed in the <CODE>setUp</CODE> method of
	 * <CODE>RTestREDEditor</CODE>.
	 */
	public void testAutoLoad() {
		REDEditor editor = getTestEditor();
		
		assertTrue("Text is modified after load.", !editor.isModified());
		assertEquals("Loaded file doesn't equal ref. data", TEXT_CONTENT,
				editor.asString()); 
	}
	
	/**
	 * Tests the <CODE>loadFile</CODE> and the <CODE>isModified</CODE> method
	 * of REDEditor. Also the REDEditorListener load events are tested.
	 */
	public void testLoadFile() {
		// BUG: The events after REDEditor.loadFile() don't occur.
		REDEditor editor = getTestEditor();

		assertTrue("Load of second file failed",
				editor.loadFile(TMP_FILE2, true));
		assertTrue("Text is modified after load", !editor.isModified());
		assertEquals("Loaded file doesn't equal ref. data", LONG_TEXT, editor.asString()); 
	}

	/**
	 * Tests the <CODE>loadFile</CODE> method of REDEditor for a non existant
	 * file. 
	 */
	public void testLoadFileEmpty() {
		REDEditor editor = new REDEditor("maynotexist.txt", false);
		assertTrue("Text is modified after loading empty file.",
				!editor.isModified());
		assertEquals("Not existant text is not empty", "", editor.asString());
		assertEquals("Text should have length 0", 0, editor.length());
	}
	
	/**
	 * Tests the sharing and unloading of unused shared texts with two editors
	 * and one shared text. 
	 *
	 * @param editor the first editor should have a loaded file, not as his
	 * private copy.
	 * @param supContent the supposed content of the text of the given editor.
	 */
	private static void test2EditorTextSharing(REDEditor editor, String content) {
		String filename = editor.getFilename();
		String result = INS_TEXT + content;
		REDEditor editor2 = new REDEditor();
		
		// change text in shared editor
		editor.replace(INS_TEXT, 0, 0, null);
		assertEquals("Insert text at beginning failed", result, editor.asString());
		assertTrue("Load of changed file failed",editor2.loadFile(filename, false));
		assertEquals("Changed text is not shared", result, editor2.asString());
		// delete editors
		editor2.close();
		editor = new REDEditor(filename, false);
		assertEquals("Reloaded shared text has wrong content", content,
				editor.asString());
	}
	
	/**
	 * Tests the sharing and unloading of unused shared texts.
	 */
	public void testTextSharing(){
		REDEditor editor = getSharedTestEditor();
		REDEditor editor2 = new REDEditor(TMP_FILE2, false);
//		test2EditorTextSharing(editor2, LONG_TEXT);
		// change text of shared editor to prepare testTextSharing2()
		editor2.close(); editor2 = null;
		editor.replace(INS_TEXT, 0, 0, null);
	}
	
	/**
	 * Test the unloading of a shared text. The text was changed in the
	 * previous testTextSharing() test but not saved. The editor was removed
	 * by RTestREDEditor.tearDown()
	 */
	public void testTextSharing2() {
		REDEditor editor = getSharedTestEditor();
		
		assertEquals("Unloaded shared test has wrong content", TEXT_CONTENT,
				editor.asString());
	}
	
	/**
	 * Tests the method <CODE>revert</CODE> of REDEditor.
	 */
	public void testRevert() {
		// configure logging
		logEventClass(REDTextEventListener.class);
		logEventClass(REDEditorEventListener.class);
		ignoreEventMethod("getListenerLevel");

		REDEditor editor = getTestEditor();

		insertText(editor);
		editor.revert();
		assertEquals("Revert failed", TEXT_CONTENT, editor.asString());
		
		checkEvents("beforeInsert(0, " + INS_TEXT.length() + ")\n" +
			"afterInsert(0, " + INS_TEXT.length() + ")\n" +
			"beforeLoad()\n" +
			"afterLoad()");
	}

	/**
	 * Tests the method <CODE>saveFile</CODE> of REDEditor and the
	 * REDEditorListener save events. Further the method
	 * <CODE>isModified</CODE> is tested.
	 */
	public void testSaveWithBackup() throws IOException {
		// configure logging
		logEventClass(REDTextEventListener.class);
		logEventClass(REDEditorEventListener.class);
		ignoreEventMethod("getListenerLevel");
		
		// supposed result string
		String result = INS_TEXT + TEXT_CONTENT;
		String actResult;
		REDEditor editor = getTestEditor();

		// insert some text at the beginning; adds insert events to protcol
		insertText(editor);				
		// save file
		assertTrue("SaveFile() failed", editor.saveFile("bak"));
		assertTrue("Text is still modified after save.", !editor.isModified());
		
		// check if saved file contains expected result
		actResult = getFileContent(TMP_FILE_NORMAL);
		assertEquals("Saved & Read bytes don't equal proposed result",
				result, actResult);
		
		// check if backup file contains old version of file		
		actResult = getFileContent(TMP_FILE_NORMAL + ".bak");
		assertEquals("Backup file does not equal original content", TEXT_CONTENT,
				actResult);
		checkEvents("beforeInsert(0, " + INS_TEXT.length() + ")\n" +
			"afterInsert(0, " + INS_TEXT.length() + ")\n" +
			"beforeFileSave(" + TMP_FILE_NORMAL +")\n" + 
			"beforeSave()\n" +
			"afterSave()\n" + 
			"afterFileSave(" + TMP_FILE_NORMAL + ')');

		// delete backup		
		File file = new File(TMP_FILE_NORMAL + ".bak");
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
	}		
	
		/**
	 * Tests the method <CODE>saveFile</CODE> of REDEditor and the
	 * REDEditorListener save events. Further the method
	 * <CODE>isModified</CODE> is tested.
	 */
	public void testSaveWithoutBackup() throws IOException {
		// configure logging
		logEventClass(REDTextEventListener.class);
		logEventClass(REDEditorEventListener.class);
		ignoreEventMethod("getListenerLevel");

		// supposed result string
		String result = INS_TEXT + TEXT_CONTENT;
		String actResult;
		REDEditor editor = getTestEditor();
		
		// insert some text at the beginning; adds insert events to protcol
		insertText(editor);				

		// Save again, this time without backup
		assertTrue("SaveFile() failed", editor.saveFile(null));
		checkEvents("beforeInsert(0, " + INS_TEXT.length() + ")\n" +
			"afterInsert(0, " + INS_TEXT.length() + ")\n" +
			"beforeFileSave(" + TMP_FILE_NORMAL +")\n" + 
			"beforeSave()\n" +
			"afterSave()\n" + 
			"afterFileSave(" + TMP_FILE_NORMAL + ')');
	}		

	/** 
	 * Returns the content of file for the given filename.
	 *
	 * @param filename the name of the file, whose content to get.
	 */
	private static String getFileContent(String filename) throws IOException {
		RandomAccessFile file = new RandomAccessFile(filename, "r");
		byte arr[] = new byte[(int) file.length()];
		file.read(arr);
		file.close();
		return new String(arr);
	}
	
	/**
	 * Tests the <CODE>saveFileAs</CODE> method of REDEditor and the
	 * REDEditor save events. Further the method <CODE>isModified</CODE> is
	 * tested.
	 */
	public void testSaveFileAs() throws IOException {
		// configure logging
		logEventClass(REDTextEventListener.class);
		logEventClass(REDEditorEventListener.class);
		ignoreEventMethod("getListenerLevel");

		String result = INS_TEXT + TEXT_CONTENT;
		String actResult;
		REDEditor editor = getTestEditor();	// test text from IN_FILE_NORMAL
		
		// save in a new tmp file
		assertTrue("saveFileAs in new file failed",
				editor.saveFileAs(TMP_FILE3, true));
		assertTrue("Text was modified after save in new file.",
				!editor.isModified());	
		checkEvents("beforeFileSave(" + TMP_FILE3 + ")\n" +
			"beforeSaveInto(" + TMP_FILE3 + ")\n" +
			"afterSaveInto(" + TMP_FILE3 + ")\n" + 
			"afterFileSave(" + TMP_FILE3 + ')');
				
		// change contents of text
		insertText(editor);
		// save file into an existing file with different contents
		assertTrue("saveFileAs in existing file failed",
				editor.saveFileAs(TMP_FILE2, true));
		assertTrue("Text is still modified after saveFileAs.", 
				!editor.isModified());

		actResult = getFileContent(TMP_FILE2);
		assertEquals("Saved & Read bytes don't equal proposed result",
				result, actResult);
		checkEvents("beforeFileSave(" + TMP_FILE3 + ")\n" +
			"beforeSaveInto(" + TMP_FILE3 + ")\n" +
			"afterSaveInto(" + TMP_FILE3 + ")\n" + 
			"afterFileSave(" + TMP_FILE3 + ")\n" +
			"beforeInsert(0, " + INS_TEXT.length() + ")\n" +
			"afterInsert(0, " + INS_TEXT.length() + ")\n" +			
			"beforeFileSave(" + TMP_FILE2 + ")\n" +
			"beforeSaveInto(" + TMP_FILE2 + ")\n" +
			"afterSaveInto(" + TMP_FILE2 + ")\n" + 
			"afterFileSave(" + TMP_FILE2 + ')');

		// delete second tmp file		
		File file = new File(TMP_FILE3);
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
	}
	
	/** Tests save as with a file that is already opened and has another content. */
	public void testSaveFileAsWithOpenTarget() throws IOException {
		REDEditor editor1 = getTestEditor();	// test text from IN_FILE_NORMAL	
		REDEditor editor2 = new REDEditor(TMP_FILE2, false);
		
		// setup
		editor2.replace("You cannot pass!", 0, editor2.length(), null);
		editor2.saveFile(null);
		assertEquals("Could not change file content", "You cannot pass!", editor2.asString());
		assertEquals("Wrong file content", "You cannot pass!", getFileContent(TMP_FILE2));
		assertEquals("Wrong editor content", TEXT_CONTENT, editor1.asString());

		// test
		editor1.saveFileAs(TMP_FILE2, false);
		assertEquals("Wrong editor content", TEXT_CONTENT, editor1.asString());
		assertEquals("Wrong file content", TEXT_CONTENT, getFileContent(TMP_FILE2));
		assertEquals("Save as did not change open editor", TEXT_CONTENT, editor2.asString());
	}
	
	/**
	 * Tests the <CODE>saveEmergency</CODE> method of REDEditor. Further the
	 * method <CODE>isModified</CODE> is tested.
	 */
	public void testSaveEmergency() throws IOException {
		// configure logging
		logEventClass(REDTextEventListener.class);
		logEventClass(REDEditorEventListener.class);
		ignoreEventMethod("getListenerLevel");

		String result = INS_TEXT + TEXT_CONTENT;
		String actResult;
		String emergencyFilename = TMP_FILE_NORMAL + ".auto";
		REDEditor editor = getTestEditor();	// test text from IN_FILE_NORMAL

		// change contents of text		
		insertText(editor);
		// save file into new tmp file
		assertTrue("Emergency save failed", editor.saveEmergency("auto"));
		assertTrue("Text is no longer modified after saveEmergency.", editor.isModified());
		actResult = getFileContent(emergencyFilename);
		assertEquals("Saved & Read bytes don't equal proposed result",
				result, actResult);
		checkEvents("beforeInsert(0, " + INS_TEXT.length() + ")\n" +
			"afterInsert(0, " + INS_TEXT.length() + ")\n" +
			"beforeFileSave(" + TMP_FILE_NORMAL + ".auto)\n" +
			"beforeSaveInto(" + TMP_FILE_NORMAL + ".auto)\n" +
			"afterSaveInto(" + TMP_FILE_NORMAL + ".auto)\n" + 
			"afterFileSave(" + TMP_FILE_NORMAL + ".auto)");
		// delete emergency saved file		
		File file = new File(TMP_FILE_NORMAL + ".auto");
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
	}
	
	private static void insertText(REDEditor editor) {
		String result = INS_TEXT + TEXT_CONTENT;
		
		// insert some text at the beginning
		assertTrue("Text was modified before change", !editor.isModified());	
		editor.replace(INS_TEXT, 0, 0, null);
		assertEquals("Insert text at beginning failed", result, editor.asString());
		assertTrue("Text was not modified after change", editor.isModified());
	}
	
	static class TestPlugin extends REDPlugin {
		public void afterInsert(int from, int to) { 
			fEditor.getLineForPosition(0);
		}
	}
	
	public void testClose() {
		REDEditor editor1 = new REDEditor("X", false);
		REDEditor editor2 = new REDEditor("X", false);
		editor2.addPlugin(new TestPlugin());
		editor2.replace("Some insertion.", 0, 0, null);
		assertTrue(editor2.isModified());
		int x = editor1.getSelectionStart();
		editor2.close();	// "discard" changes (but still kept in editor1)
		System.gc(); System.gc(); System.gc();	// ensure gc has swept away stuff
		
		// check editor1 is still working ok.
		assertEquals(editor1.getSelectionStart(), 0);
		editor1.setSelection(editor1.length(), editor1.length());
		assertEquals(editor1.getSelectionStart(), 15);		
		
		// now insert sth. => The plugin of the closed editor2 might try to react
		editor1.replace("Another insertion.", 0, 0, null);
		
		editor1.close();
	}
}